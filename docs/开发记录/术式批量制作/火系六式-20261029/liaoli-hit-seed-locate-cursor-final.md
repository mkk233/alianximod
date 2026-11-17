先读定位任务的授权范围，确认它允许读什么、能不能写文件。这是只读定位。按引用查燎里命中、火环、火种同步和跟随插值，不改文件。# 燎里命中、火环与跟随定位

只读定位，没有改文件，没有编译，没有审查结论。现行需求仍是共享燃料：命中时花掉当时剩余火种，物理 6 点另加 `0.36×S×(1+F)`。不恢复私有预付火种。设计 2/2、实现 3/3 已用完。下面三项是下一步授权，这一份只指出接线，不写成已经做完。

## 1. 三道后环只在有效命中且扣前 S>50 时出现

现在的环不看命中，也不看火种。`LiaoliV6Renderer.render` 每次绘制都先画环，寿命来自命中体的 `tickCount`。`drawRings` 用 `rear_ring.png`，三道环按 `ringProgress` 每 24 拍循环，半径变大，中心沿负深度后移。空挥也会播。

有效命中的服务端入口是 `TechniqueHitSettle.tryHit`：`hitInterval <= 0` 时每个目标只进一次 `hitOnce`，然后调用 `listener.onHit`。燎里的监听在 `LiaoliEffect` 第 51–62 行：先 `DamagePermission.allows`，不通过就直接返回；通过后若 `FireSeedState.value(target) > 0` 才 `FireConsumption.detonate(..., ALL_SEEDS, thrust)`，随后无论火种多少都打物理伤害。

扣前余额就是这次 `value` 读到的数。`detonate` 再进 `FireSeedState.consumePrepared`：用同一次读出的账本做 `preview`，校验通过后才 `consume` 并 `persist`。`ALL_SEEDS` 是 `Integer.MAX_VALUE`，`FireSeedMath.invested` 会把它收成当时全部余额。火伤在扣款之后由 `thrust` 按 `0.36×amount` 计算。环要看的 S>50 必须用这次扣款前的余额；50 本身不播。空挥没有 `onHit`。伤害和扣种公式不用改。

环画在施法者身后的命中体上，不是画在被击中的目标上。命中体同步数据里没有“允许出环”的字段，客户端也读不到 S。所以只改 `LiaoliV6Renderer` 无法知道该不该画。最小接线是：在 `LiaoliEffect.onHit` 里、`detonate` 之前记下扣前 S，仅当权限通过且 S>50 时给这个命中体一个客户端能看见的触发；`drawRings` 没有触发就不画。几何、贴图、后移方向留在 `LiaoliV6Layout.ringCenter` / `ringRadius`。

现有环是整段施法都在循环。新要求只说了触发条件，没说命中之后是继续循环到收招，还是只放一轮 24 拍。这不是火种规则，实施时要按“保留现有后扩外观”沿用现有循环，不要另做一套环。

## 2. 火种朵数：客户端现在没有余额

上限是 `FireSeedMath.UPPER_BOUND = 100`。余额在服务端持久数据 `xueximod_attribute_states` 的 `fire_seed` 里，经 `AttributeStateData` 读写。`FireSeedState` 写明不注册药水。`value()` 在客户端或非主线程直接返回 0。死亡时 `discard` 把该键写成 0。没有把这个整数同步到客户端的包或实体数据。

因此 `floor(S/10)` 现在只能在服务端算：0–9 为 0 朵，100 为 10 朵。所有带火种的生物都要显示，不能只挂在燎里目标上。现成的 `FireVisualClient` 是 `RenderLevelStageEvent.AFTER_ENTITIES` 上的地面火和燃烧轨道，数据来自 `FireVisualPacket`，不是火种余额，也不能在实体离开或余额变 0 时表示“这朵火苗该拆”。

新火苗图还没确认，不能接进游戏。现有火焰条是竖条、按帧高切开、用 `RenderType.entityTranslucentEmissive` 画。`rear_ring.png` 为 96×2304，`RING_FRAMES = 24`，一帧 96×96。新图若沿用这种方式，客户端按帧高切片即可。帧尺寸等确认图，不要先定。

## 3. 高速跟随闪到身后

服务端 `FollowCasterMotion.tick` 每拍把命中体 `placeAt` 到施法者当前眼睛，朝向用 `getLookAngle()`。`placeAt` 立刻 `setPos`，并用水平方向写 `yRot`、`xRot`。

客户端非生物不会自己消化移动包。`TechniqueHitEntity.lerpTo` / `moveTo` 都进 `ClientTrack`：包到达时先记住旧坐标和旧转角，再把实体放到新点；本拍 `tick` 之后把 `xo/yo/zo` 和 `yRotO/xRotO` 设回旧值，渲染再在两点之间插值。人跑得快时，这一拍的起点会落在身后。

剑阵还不跟这个实体走到底。`casterUuid` 只在服务端 `applySpec` 里赋值。`defineSynchedData` 没有它，存档只写寿命。`getCaster()` 在客户端直接返回 null。`LiaoliV6Renderer.matchedPlayer` 用插值后的命中体位置和在场玩家眼睛比距离，0.75 格内最近的才算施法者。对上时，背后八剑用该玩家的身体偏航和俯仰，但顶点仍画在命中体自己的位置上。对不上时退回命中体视线偏航，俯仰为 0，站立剑阵。快移时眼睛距离很容易超过 0.75，姿态会在“跟着身体”和“站立、落在落后的命中体上”之间跳，看起来就是闪到身后。

不要把 0.75 放宽。那会在两人靠近时认错施法者。平滑跟随要先把施法者身份同步到这个命中体上，渲染再用该玩家本帧插值眼睛和转角，而不是用落后一拍的实体坐标去猜。

v7 站立尖朝下、横置到腿部并前移 `[0,-12,0]`，以及六张 `liaoli_v6` 贴图，都还在 `LiaoliV6Layout` / `LiaoliV6Renderer`，这次没有改。

## 预期会碰到的文件

| 范围 | 文件 | 原因 |
| --- | --- | --- |
| 本次燎里视觉作者 | `LiaoliV6Renderer`、`LiaoliV6Layout` | 环的开关、剑阵改跟已绑定玩家的插值 |
| 命中，不改公式 | `LiaoliEffect` | 扣前读 S，仅 S>50 且权限通过时发环 |
| 共享命中体 | `TechniqueHitEntity` 的同步数据，`ClientTrack`，`FollowCasterMotion` | 现在没有客户端施法者；位置每拍跳到眼睛 |
| 火种账 | `FireSeedState`、`AttributeStateData` | 只有服务端整数；朵数要另有客户端可见副本 |
| 现有火焰绘制入口 | `FireVisualClient`、`FireVisualPacket` | 可参考绘制时机，但它不是火种余额 |
| 素材 | `rear_ring.png` 继续用 | 新火苗图未确认，不能接 |

`TechniqueHitEntity` 和火种账是共享路径，不是这两份姿态文件能单独完成的。没有需要改玩法才能做的分叉。唯一还不能落地的是：火苗图未确认，以及客户端根本没有 S。环的循环是播到收招还是只播一轮，新句子没有写死，实施时保持现在的后扩循环即可。
