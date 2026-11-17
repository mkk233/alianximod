我先读取这份任务说明，按它划定的范围执行。任务是只读定位：先读已批准规格和相关流程，再定向查燎原的现有接点和资源。规格已读完。接下来只读燎原效果、场地、客户端渲染和火种结算这几处接点。燎原实施定位已完成。本单只读，没有改文件，没有编译。Cursor 会话 ID：`3ab5c23a-679e-456d-8c99-ab71cc0e5251`。同会话可按此 ID 续跑实现。

已读规格是 `docs/开发记录/燎原伤害定稿入表-20261031/任务.md` 与 `灰烬特效与范围任务.md`。其中上一轮“只写表、不改代码”的执行限制按本单忽略，设计语义保留。根目录 `AGENTS.md` 与 `docs/统一开发与术式批量制作流程.md` 已对照：本批复审额度已用完，这里不追加复审，也不改底座方案。相关 Java 路径下没有第二份 `AGENTS.md`。火场没有独立实体，是进程内的 `Zone` 记录。

## 现有接点

| 项 | 现况 | 位置 |
|---|---|---|
| 半径 | 常量 6；有施法帧且 `energyScalesLength()` 为真时再乘 `CastFrame.volumeScale()`（`energyScale * radiusMultiplier`，无效按 1） | `GroundFireFields.BASE_RADIUS` 第 31 行；`LiaoyuanEffect` 第 39–42 行；释放登记 `scaleLength=true`，`ReleaseDefinitions` 第 41 行、第 232–233 行 |
| 寿命 | 200 tick，从 `open` 当拍起算，含展开；定义缺失时用 `FIELD_LIFE` | `ReleaseDefinitions` 第 41 行 `lifetimeTicks=200`；`LiaoyuanEffect` 第 43 行；`GroundFireFields` 第 32、52 行 |
| 本体伤害 | 2 点，`strikeOnce(..., 2, 2, true)`。`furnaceSettled=true` 时受伤事件不再加炉心 | `GROUND_DAMAGE` 第 35 行；`pulse` 第 131 行；`ElementAdjustment.fireAmount` 第 11–17 行 |
| 本体周期 / 初始延迟 | 开场 `nextPulseTick = now + 20`，之后每 20 tick 一次。开场当拍不检测 | `GroundFireFields.open` 第 53 行；`onServerTick` 第 69–71 行。20 来自 `FireConsumerPlans.SPREAD_INTERVAL` |
| 燃烧足额伤害 | `2 × (1 + 0.1n + F)`，n、F 在挂载时锁进 `SharedSpec` | `FireConsumerPlans.spreadSpec` 第 31–38 行；公式 `FireLayer.combine` 第 8–9 行 |
| 燃烧间隔 / 请求燃料 / 段数上限 | 20 tick、每段请求 10、上限 `3+n` | `SPREAD_INTERVAL` 第 22 行；`FireLimits.STANDARD_SEGMENT_DEMAND` 第 14 行；`SPREAD_BASE_SEGMENTS` 第 23 行 |
| 燃烧初始等待 | 新共享层 `ticksUntilNext` 直接等于间隔 20。`pump` 只在 `ticksUntilNext <= 1` 时结算 | `BurnLedger.Track.shared` 第 299–304 行；`pump` 第 103–107、166–167 行 |
| 碰撞 | 先用方形粗查盒 `inflate(半径, 垂直余量+层高, 半径)`，再按脚点圆盘和脚底高度裁掉 | `GroundFireFields` 第 113–144 行；`GroundFireMath.insideDisc` 第 31–37 行；`inLayer` 第 18–28 行。层高 1，垂直余量 3 |
| 展开进度 | `coveredRadius(radius, age) = radius * min(1, age/12)`，`age<=0` 返回 0。只被客户端绘制使用 | `FireVisualLayout` 第 14、39–53 行；`FireVisualClient.drawGround` 第 304–327 行。服务端 `Zone` 不存出生拍，也不调用它 |

登记伤害 `Technique.baseDamage` 也是 `2f`（`ModTechniques` 第 192–195 行）。`LiaoyuanEffect` 不读这个字段，场地打击只用 `GROUND_DAMAGE`。施法冷却 600 与装配消耗 3、显式消耗 60 都不是这段本体/燃烧周期。

同一施法只铺一片：`ReleaseBatch.claimSurfaceFire()` 第 63–69 行。`LiaoyuanEffect.execute` 第 27–28 行失败即返回。不放原版火，不改方块。火场结束只 `FireVisualRelay.closeGround`，不清目标身上的燃烧存档。

## 应复用的结算入口

共享燃料的观察与挂载已经在扣费之前：

1. `FireConsumption.schedule`（第 23–52 行）先读 `FireSeedState.value` 和施法者炉心，再调用 `spreadSpec(balance, furnaceBonus)`，然后 `BurnLedger.admitShared`。这里不扣火种。
2. 周期结算只有 `FireRuntimeEvents.onLivingTick` → `BurnCarrier.tick` → `BurnDispatch.tick`（第 31–53 行）。
3. `BurnLedger.pump` 收集本拍到期的共享层，`FuelShare.divide` 按需求比例分同一余额，伤害为 `fullSegmentDamage * 实付 / 10`，再 `FireSeedState.draw` 扣实付。
4. 每一份伤害都是 `FireDamage.strikeOnce`。燃烧脉冲在 `BurnCarrier` 第 35–36 行，火场本体在 `GroundFireFields` 第 131 行。

`LivingTickEvent` 在实体 tick 期间发生，早于 `ServerTickEvent.END`。火场脉冲在 END。挂载发生在火场脉冲里时，本拍的 `BurnDispatch.tick` 已经过去。

同 tick 两份火伤的现有入口就是连续两次 `FireDamage.strikeOnce`。伤害类型是 `xueximod:fire_resource`。`FireResourceHurtMixin` 在来源匹配时跳过 else 分支对 `lastHurt` 的写入，以及 `hurt` 里唯一一处 `invulnerableTime` 写入（`FireCooldownPolicy.keepExistingHurt` 对这种来源返回真）。不要改这个 mixin，也不要改全局无敌时间。`hurt` 入口处对已有无敌时间的比较仍是原方法里的逻辑。

去重键是 `castId + 目标 UUID + "xueximod:fire_liaoyuan"`，只扫描账本里还活着的轨道（`BurnLedger.duplicate` 第 25–34 行）。轨道结束后这个判断为假。

`FireConsumerPlans.spread()`（第 58–77 行）仍是 B=2 的预付段计划，运行时火场不调用它，只有测试引用。挂载走 `spreadSpec`。

## 按现有结构做不到、需要补的缺口

- 立即首段：`schedule` 不结算。新层等待固定为 20，没有外部可写的等待字段。`pump` 每次还会把未到期的其他共享层等待减 1。没有“只结算刚挂上的这一层、且不推进其他层”的入口。把 `Track.shared` 的初始等待全局改成 1，会把焚天等共享层一起改成立即首段。燎原要当拍首段且仍走 `FuelShare.divide`，就要在 `BurnLedger` / `FireConsumption.schedule` 上增加默认行为不变的单层入口。不能在燎原里绕开账本再打一份。
- 本体与燃烧目前互斥：已挂载或本拍挂载成功就 `continue`，不打本体（第 120–130 行）。无火种才打 2 点本体。
- 没有每目标的本体计时，也没有“本火场已经给这个目标挂过燃烧”的记录。现有脉冲是整片火场每 20 tick 一次。轨道消失后，下一次脉冲会再次 `schedule`。反复进出因此会重新挂层；本体也没有可保留的 10 tick 计时。
- 服务端不用 `coveredRadius`。开场 12 tick 内打的是整片最终圆盘，而且要等 20 tick 才第一次检测。`coveredRadius` 在年龄小于等于 0 时返回 0，开场当拍若年龄记 0，中心也不在覆盖内。
- 接触不是实体碰撞盒相交。方形粗查之后用脚点圆盘裁掉角落。现成贴地低矮盒在 `SurfaceContactField`：`collect` 的半宽写死 `HALF_EXTENT=2`（边长 4），`Slice.meets(AABB)` 才是盒相交（第 284–286 行）。直接改这个常量会改变 `SurfaceFireHosts` 和 `SurfaceSnapshotAssembly`。半宽 10 不能调用现成 `collect`。
- 本体 8×(1+F) 不会随把常量改成 8 自动出现。当前调用把炉心标成已结算。`ElementAdjustment.fireAmount` 在未结算时是 `当前伤害 + 火种基数 × F`。基数和当前伤害都是 8、且未结算时，结果等于 8×(1+F)。燃烧侧的 F 已经在 `spreadSpec` 里乘进足额伤害，结算时 `fireBase` 传 0，不能再乘一次。
- 半径 10 若表示最终半径，只改 `BASE_RADIUS` 仍会被 `volumeScale()` 乘过。

离场后燃烧继续、火场结束不取消燃烧、公平分账、实付/10、n 与 F 在扣费前锁定，这些已经在上述账本路径上。段数上限仍是 `3+n`，间隔仍是 20，请求仍是 10。翻倍应改的是两处伤害基数：本体 2→8，`spreadSpec` 的基础 2→4。燃料请求不要改成 20。

## 最小可写文件

实现获准后，燎原侧需要写：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/GroundFireFields.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoyuanEffect.java`（半径还要乘能量倍率时才必须动；只改 `BASE_RADIUS` 则效果类可不动）
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/FireConsumerPlans.java`（只改 `spreadSpec` 的 2.0；不动 `skySpec`、`thrust`、间隔、需求 10、`3+n`）
- `src/main/java/com/bslx/XueXiMod/visual/fire/FireVisualLayout.java`（加灰烬路径常量；保留 `SPREAD_WAVE`、`WAVE_FRAMES`、现有 `GROUND_FLAME`）
- `src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java`（只改 `drawGround` 这一路）
- `src/main/resources/assets/xueximod/lang/zh_cn.json`
- `src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java` 第 192–195 行的中文说明（与 lang 各有一份）

公共文件里，立即首段必须改 `BurnLedger.java`，并给 `FireConsumption.schedule` 加一个默认仍为“等满间隔”的入口。不改 `FireDamage`、`FireResourceHurtMixin`、`FireCooldownPolicy`、`FuelShare`、`FireLayer`、`FireSeedState.draw`、`BurnDispatch.tick` 的周期语义、`FireVisualPacket`、`ReleaseBatch.claimSurfaceFire`。

`SurfaceContactField` 不是必须改。不改它，就在燎原侧自备半宽 10 的贴地收集，并调用已有的 `Slice.meets`。不要改 `HALF_EXTENT`。不要改 `SurfaceFireHosts`、灼地、焚天、燎里。

地面包 `FireVisualPacket.ground` 已带原点、半径、出生和结束时刻，没有贴图字段。灰烬换图不需要新包。`SPARK` 只由 `SurfaceFireHosts` 发出，燎原地面不发；那个包是飞向实体中心的 8 个 `ParticleTypes.FLAME`，不是地表升起的火星。

中文现文案：`zh_cn.json` 第 76–77 行，名称“燎原”，说明是“铺开地表火层。踩中后按段燃烧，每段消耗 10 点火种，燃料耗尽或到达期限就结束”。`en_us.json` 第 18–19 行是对应英文。伤害逻辑不依赖英文文件。

## 纹理与可接入方式

运行中的三张图，代码引用在 `FireVisualLayout` 第 24–26 行，没有 mcmeta，帧不走原版动画：

- `src/main/resources/assets/xueximod/textures/effect/liaoyuan_v2/spread_wave.png`
- `src/main/resources/assets/xueximod/textures/effect/liaoyuan_v2/ground_flame.png`
- `src/main/resources/assets/xueximod/textures/effect/liaoyuan_v2/ground_embers.png`

确认版说明在 `docs/资源库/特效/火焰/燎原_v2/README.md`：火浪单帧 128×128、12 帧、50ms；低矮火焰单帧 32×40、24 帧、50ms；炭火单帧 64×64、24 帧、50ms。代码里的时间是游戏拍：`stripFrame` 对年龄取整后对帧数取余，20 tick/秒时一帧 50ms。火浪只在年龄小于 12 时画，铺盖半径同步走完。24 帧循环是 1.2 秒，不控制 200 tick 寿命。结束前 20 tick，`seaOpacity` 从 255 收到 0。

`drawGround` 按方块列贴地：火浪用 `discPatch` 把一帧铺进整个圆盘；余烬是每列边长 1 的平片，UV 为整帧宽、纵向 `frame/24` 到 `(frame+1)/24`；火焰是十字片，宽 0.62、高 0.78。列中心还要落在圆盘内，方形角落没有画面。全部走 `RenderType.entityTranslucentEmissive`。身上的燎原附着（`drawSpread`）继续用 `GROUND_FLAME` 这张十字图，换地表图时不要覆盖这张，否则附着火焰跟着变。

过程草稿在 `docs/资源库/过程归档/燎原灰烬-20261031/`，尚未进入 `assets`，也未进正式资源库。可用的是 `ground_ash_strip.png`（64×1536，24 帧）或 `ground_ash_base.png`（64×64 静帧）加 `ground_embers_strip.png`（64×1536，透明余烬）。纵向 UV 与现有 `band(frame, 24)` 一致。静帧不能套进 24 帧切片。平铺应铺满已覆盖地表，余烬另减密度。底层用普通透明混合和最近邻；现有 emissive 类型会把炭灰打亮。外缘破碎、方形角、12 tick 展开、少量上升火星和结束淡出都要在 `drawGround` 里做，草稿图里没有烘焙圆罩。`spread_wave.png` 保留，不改 12 帧。不要复制 `素材库/burnt-1.10.5.3-forge-1.20.1.jar` 里的图。用户确认前不要把草稿拷进运行资源。

## 工作区

本次没有写入。实施时保留已有未提交改动，不要 reset、clean 或 checkout 覆盖。本单没有改制作表；表作者把 `fire_liaoyuan` 退回制作中的工作不在这里评价。源码仍未写。
