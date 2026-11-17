## 结论

- 水环在客户端是“按主人玩家实体、半径、带宽、体高比例和姿态法线重建”的视图，服务端只发送 **FULL/POSE/REMOVE** 三种包，位置坐标逐拍交给客户端用主人实体推算（`GuardSyncPacket.java:16` 注释明确“坐标不逐拍发送”）。
- 服务端命中/偏转所用的几何与包里的 `radius / band / bodyHeight / baseTilt / from→toward` 同源，美术替换必须让画面与这套数值保持一致，不能另立一套显示半径或环心高度。
- 客户端 `ClientRings.View` **没有** `expiresAt`、`upkeepAt` 字段，消失完全依赖服务端 `REMOVE`（或 `clear()`），客户端不做本地到期判断。
- `animationStart` 是服务端 `getTickCount()` 的绝对刻度，包内没有同时携带“服务端当前 tick / 已存活时长”，客户端也没有任何换算代码；现阶段只能以收包时刻作为年龄原点，真实年龄无法在纯客户端还原。
- 本次材料未包含 `docs/水环重设设计与实现指导-2026-10-22.md` 的正文，也未包含 `GuardPose`、`GuardDefinition`、`RingBandGeometry`、`RingMeleeGeometry`、`GuardPreview`、客户端渲染类、`WaterFormEffect`，相关内容一律列为待查。

## 0. 材料边界

已提供：`WaterTechniques.java`、`GuardStates.java`、`GuardSyncPacket.java`、`RingDeflectionHost.java`、`RingFollow.java`、`RingOrbitRoute.java`。

路径缩写：

- `WaterTechniques.java` = `src/main/java/com/bslx/XueXiMod/shu/WaterTechniques.java`
- `GuardStates.java` = `src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardStates.java`
- `GuardSyncPacket.java` = `src/main/java/com/bslx/XueXiMod/network/GuardSyncPacket.java`
- `RingDeflectionHost.java` / `RingFollow.java` / `RingOrbitRoute.java` = `src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/hosts/` 下同名文件

## 1. 注册链

- `WaterTechniques.java:35-36`：`guard("water_ring", "水环", 40, 0, "环绕自身的水环，受到的伤害削去两成", GuardDefinition.personalRing())`。
- `WaterTechniques.java:95-102`：注册 `Technique`（`MAGIC`、伤害 `0f`、冷却 `40`、复杂度 `0`）→ `ReleaseRegistry.register(WaterReleaseDefinition.bodyGuard(releaseId, guard))` → `TechniqueEffectRegistry.register(releaseId, FORM)`。
- `personalRing()` 的具体 `radius / band / bodyHeight / baseTilt / lifetimeTicks / upkeepPeriod / upkeepCost / spawnCost / deflectsProjectiles / unloadRatio` 数值在 `GuardDefinition`，材料未提供 → 待查。
- `WaterReleaseDefinition.bodyGuard` 如何走到 `GuardStates.schedule/activate`，材料未提供 → 待查。

## 2. 服务端存活状态机

| 事件 | 位置 | 行为 |
|---|---|---|
| 排期/立即生成 | `GuardStates.java:37-56` | 比较 `server.getTickCount()`；未来 tick 走 `ChainScheduler`，并校验 `ChargeGate` 与 `isAlive` |
| 新建 | `GuardStates.java:93-109` | `expiresAt = now + lifetimeTicks`、`nextUpkeepAt = now + upkeepPeriod`、`animationStart = now`、`revision = 1`、`fromNormal = GuardPose.baseNormal(baseTilt)`、`towardNormal = fromNormal`、`phaseStart = phaseEnd = now` |
| 刷新（同 owner 同 `typeKey` 已存在） | `GuardStates.java:82-91` | 先卸偏转 → 延长 `expiresAt` → `revision += 1` → 更新维度/`serverLevel` → 重新挂偏转 → `broadcastFull` |
| 每 tick | `GuardStates.java:136-171` | 主人离线/死亡/换维 → 掉落并 `REMOVE`；`now >= expiresAt` → 掉落并 `REMOVE`；维护费失败 → 掉落并 `REMOVE`；否则推进姿态 |
| 关闭 | `GuardStates.java:173-189` | `closeOwned` 清掉该玩家全部护体并移除波次记录；调用方待查 |

- “每人每类最多一个”由 `find(playerId, typeKey)` 保证（`GuardStates.java:82`、`:254-264`，类注释 `:28`）。
- 波次去重：`claimWave` / `forgetWave` / `forgetOldWaves`（`GuardStates.java:266-283`），保留 400 tick。
- **刷新路径不重置 `animationStart`，也不重置 `fromNormal/towardNormal/phaseStart/phaseEnd`**（`GuardStates.java:84-89`）。重复释放会让旧动画年龄继续，同时 `revision` 变成新代。
- 卸载比上限：`unloadRatio()` 截到 `0.6`（`GuardStates.java:112-122`）。

## 3. 何时消失

服务端移除并发 `REMOVE` 的路径：

- 主人不存在、死亡、维度不一致：`GuardStates.java:141-149`。
- 到期：`GuardStates.java:150-155`（`expiresAt` 为服务端绝对 tick）。
- 维护费不足：`GuardStates.java:157-165`。
- 主动关闭：`GuardStates.java:178-187`。
- `REMOVE` 包构造：`GuardStates.java:402-405`（`kind=REMOVE`，其余数值全 0）。

客户端移除：

- `GuardSyncPacket.java:130-133`：收到 `REMOVE` 时按 `ownerId` 直接删视图，`revision` 不参与判断。
- `GuardSyncPacket.java:162-164`：`clear()` 清空全部视图，调用方未在材料中 → 待查。
- `ClientRings.View` 字段（`GuardSyncPacket.java:190-207`）没有 `expiresAt/upkeepAt`，客户端无法自行到期。

## 4. 客户端可见范围与同步节奏

- `GuardSyncPacket.java:106-115`：发给 `owner.serverLevel().players()` 中 `watcher == owner` 或 `watcher.distanceTo(owner) <= 128f` 的玩家。主人自己永远收到；其他玩家按 128 格三维距离过滤。
- 只在该维度内广播；换维时 `broadcastRemove` 走的是主人**当前** `serverLevel` 的玩家列表（`GuardStates.java:145-147`），旧维度观察者不会收到这条 `REMOVE`。是否有其他清理（例如 `clear()`、实体追踪重置）材料未给出 → 待查，属于“幽灵环”风险点。
- 触发 `POSE` 的时机：受击 `strike`（`GuardStates.java:124-134`）、每两 tick 的预览 `GuardPreview.look`（`:166-168`）、回正（`:225`）、统一由 `beginMove` 发包（`:230-238`）。
- 触发 `FULL` 的时机：新建、刷新、`snapshot`（`GuardStates.java:285-287`、`:191-198`、`:407-416`）。
- 新观察者补发：`GuardSyncPacket.sendSnapshot(watcher, owner)`（`GuardSyncPacket.java:117-122`）已存在，调用点材料未提供 → 待查。
- 材料中**不存在** `syncToTracking` 这个符号；按名字找不到对应实现，需要另行定位。

## 5. 包字段与客户端门控

`GuardSyncPacket.java:41-85` 字段：

`kind`、`ownerId`、`revision`、`sequence`、`expiresAt`、`upkeepAt`、`animationStart`、`baseTilt`、`radius`、`band`、`bodyHeight`、`fromX/Y/Z`、`towardX/Y/Z`、`poseTicks`。

- 全字段走 `writeVarInt`/`writeFloat`，没有当前位置、没有服务端当前 tick、没有“已存活 tick”。
- 客户端 `FULL`（`GuardSyncPacket.java:136-153`）：`packet.revision < view.revision` 丢弃；同代或更高代覆盖写入 `revision/sequence/baseTilt/radius/band/bodyHeight/animationStart`，再 `applyPose`。`sequence` 在 `FULL` 上不比较。
- 客户端 `POSE`（`GuardSyncPacket.java:155-159`）：无视图、`revision` 不等、`sequence` 更小则丢弃；`sequence` 相等会重新应用并重置姿态起点。
- `applyPose`（`GuardSyncPacket.java:179-188`）：写入 `from/toward`，`poseStartMillis = System.currentTimeMillis()`，`poseTicks = max(1, packet.poseTicks)`。姿态插值使用**客户端墙钟**。
- `View` 默认值（`GuardSyncPacket.java:194-206`）：`radius 0.9`、`band 0.12`、`bodyHeight 0.55`、`from/toward = (0,1,0)`、`poseTicks 1`、`animationStart 0`。这些默认值只在收到 `FULL` 前生效。
- `expiresAt`、`upkeepAt` 在 `encode/decode` 里存在，`ClientRings` 未保存。

## 6. 几何约定（服务端）

- 环心：`owner.position() + (0, owner.getBbHeight() * bodyHeight, 0)`（`GuardStates.java:368-374`）。
- 主人锚点：`owner.position()`，即脚底（`GuardStates.java:386-392`）。
- 平面法线：`RingBandGeometry.blendNormal(fromNormal, towardNormal, phaseStart, phaseEnd, now)`，`now` 来自服务端 tick（`GuardStates.java:377-383`）。
- 偏转宿主参数：`majorRadius = guard.radius()`，`tubeRadius = guard.band() * 0.5`（`GuardStates.java:319-326`）。
- 宿主键：`HostKey(hostId, generation = guard.revision)`（`RingDeflectionHost.java:32-40`）；`hostId` 在 `BodyGuard` 创建时随机（`GuardStates.java:446`），刷新时沿用。
- 宿主存活：`serverLevel != null && follow.live()`（`RingDeflectionHost.java:57-60`）。`LiveRing.live()` 判定 `retired`、`revision` 绑定、`serverLevel`、是否过期、主人是否在本 level 且存活（`GuardStates.java:351-365`）。
- 注释明确：法线与 `GuardStates` 登记时同一公式（`RingDeflectionHost.java:15-22`）；管半径是带宽一半，不加附魔容差；圆盘内部不算盾（`:19`）。
- 客户端是否使用同一套“体高比例求环心”的公式，取决于渲染类，材料未提供 → 待查。`ClientRings` 只保存 `bodyHeight`，不计算中心。

## 7. 姿态机制

- 受击：`strike` 把目标设为 `GuardPose.worldLevel()`（近战）或 `leanToward(incoming)`，`cause = CAUSE_HIT`，`holdUntil = now + ENTER_TICKS + HOLD_TICKS`（`GuardStates.java:124-134`）。
- 预览：`preview` 依据来袭方向 `leanToward`，`cause = CAUSE_PREVIEW`，`holdUntil = now + ENTER_TICKS + 4`，相同方向且 `distanceToSqr < 0.04` 时跳过（`GuardStates.java:200-213`）。
- 回正：相位结束且未处于保持期且不在回正中时，目标回到 `GuardPose.baseNormal(baseTilt)`，`cause` 沿用，时长 `RETURN_TICKS`（`GuardStates.java:215-228`）。
- 每次换姿态 `sequence += 1` 并立即发 `POSE`（`GuardStates.java:230-238`）。
- 包内 `poseTicks`：`FULL` 用 `max(1, phaseEnd - phaseStart)`（`:415`），`POSE` 用传入的 `ticks`（`:425`）。
- `ENTER_TICKS / HOLD_TICKS / RETURN_TICKS` 数值、`worldLevel()` 与 `baseNormal()` 的公式 → 待查（`GuardPose` 未提供）。

## 8. 偏转与绕行

- 弹道：`RingBandGeometry.earliest(center, normal, majorRadius, tubeRadius, from, toward, projectileRadius)`（`RingDeflectionHost.java:63-74`）。
- 覆盖判定：`RingBandGeometry.covers(...)`（`RingDeflectionHost.java:97-104`）。
- 近战：仅接受 `Kind.MELEE` 且有 `victim`，走 `RingMeleeGeometry.protect(generationLive, owner, victimId, center, majorRadius, tubeRadius, ownerBody, from, toward)`（`RingDeflectionHost.java:76-95`）；注释说明“只护当前这一代的主人，不把环心圆盘当成无敌”（`:20`）。
- 绕行：`RingOrbitRoute` 在构造时定死入口角、出口角、绕行方向与随机量（`RingOrbitRoute.java:30-52`）；随机数每路线只取一次（`RingDeflectionHost.java:107-113`）。
- 采样：每次读取当前 `follow.center()` 与 `planeNormal()`，基底用 `captureBasis.rotatedTo(...)` 做最小旋转，角度不重抽（`RingOrbitRoute.java:95-115`）；`ownerAnchor` 参与出口方向翻转（`:117-123`）。
- 生命期：`ORBIT_TICKS`（注释称“绕行六拍”，`RingOrbitRoute.java:9`）后封存为完成；跟随失效时按当时位置封存为 `lost`（`:75-93`、`:146-156`）。常量数值在 `RingBandGeometry`，材料未提供 → 待查。
- `emergencyRelease` 返回 `ReleaseSample`，同 tick 重复询问不推进时钟、不重抽随机（`RingOrbitRoute.java:60-68`，注释 `:9-12`）。
- `active()` 为 `!lost`（`:70-73`）。

## 9. 时钟问题：已确认事实与缺口

事实：

- `animationStart` 来自 `player.getServer().getTickCount()`（`GuardStates.java:100`），随 `FULL`/`POSE` 原样发送（`:411`、`:422`），客户端原样存储（`GuardSyncPacket.java:151`）。
- 同一份 `expiresAt`、`upkeepAt` 也是服务端绝对 tick，客户端根本不保存（`ClientRings.View` 无对应字段，`GuardSyncPacket.java:190-207`）。
- 姿态动画的时间基准是客户端 `System.currentTimeMillis()`（`GuardSyncPacket.java:186`），与服务端 tick 无关。
- 包内没有“服务端当前 tick”、没有“已存活 tick”、没有相对年龄字段（`GuardSyncPacket.java:66-93`）。
- 材料里没有任何把服务端 tick 映射到 `level.getGameTime()` 或本地时间的代码。

因此：

- 渲染若用 `level.getGameTime() - animationStart` 求年龄，等于把客户端游戏时间与服务端 tick 计数相减，代码层面没有任何校正项；两者在同一时刻是否相等，本次材料无法证明。
- `snapshot`（`GuardStates.java:191-198`）给后加入者发的是**原始** `animationStart`，客户端手里没有服务端当前刻度，无法换算已存活时长。
- 刷新路径保留旧 `animationStart`（`GuardStates.java:84-89`），所以“以收包时刻为 0 岁”在首次生成时准确，在刷新和补发快照时不准确。

只改展示时可选的同步口径（供脑子裁决，均基于上述事实，未读渲染类）：

- 收包原点：首次 `FULL` 时记 `age = 0`，用本地帧时间推进。实现最小，刷新与中途加入会从 0 重新起播。
- 收包原点 + 记录 `receivedAnimationStart`：可判断“是否同一代动画”，但无法得到收包前已经过去的时长。
- 精确年龄：需要 `FULL` 额外携带服务端当前 tick 或 `serverNow - animationStart`（或剩余寿命），这属于协议/服务端改动，超出“只改展示”。

## 10. 美术替换红线

- 环心高度公式必须与 `owner.getBbHeight() * bodyHeight` 一致（`GuardStates.java:373`），`bodyHeight` 只能用包内值。
- 主半径必须是 `radius`，管/带厚度必须是 `band`，且服务端偏转用 `band * 0.5`（`GuardStates.java:325-326`、`RingDeflectionHost.java:38-39`）。画面环带与 `band` 脱节会造成“看着挡住、实际穿过”。
- 法线必须来自 `from → toward` 的插值，插值时长必须等于包内 `poseTicks`，起点用收包时刻（`GuardSyncPacket.java:179-188`）。不要改用 `baseTilt` 直接算固定朝向。
- 姿态触发时机保持：受击进入 `ENTER_TICKS`、保持 `HOLD_TICKS`、回正 `RETURN_TICKS`，预览只由每两 tick 的 `GuardPreview.look` 驱动（`GuardStates.java:131-132`、`:166-168`、`:211`）。
- 世代规则保持：`FULL` 只在 `revision` 不低于当前时覆盖；`POSE` 要求同 `revision` 且 `sequence` 不倒退（`GuardSyncPacket.java:136-159`）。
- 消失只认 `REMOVE` 与 `clear()`；不要新增客户端到期隐藏，否则会与服务端寿命/维护费脱节（`GuardStates.java:150-165`）。
- 刷新语义保持：重复释放延长寿命、`revision+1`、`animationStart` 不变（`GuardStates.java:82-91`）。若美术需要“重新起播”，要在渲染层处理代切换，不能靠改 `animationStart`。
- 偏转行为保持：只挡环带、圆盘内部不挡、近战只护当前代主人（`RingDeflectionHost.java:19-20`、`:63-104`）。
- 绕行行为保持：六拍、出口角与绕行方向在捕获时定死、随机量每条路线一次（`RingOrbitRoute.java:9-12`、`:30-52`）。
- 接触附魔的几何独立：使用 `towardNormal`、环心公式与 `band * 0.5 + SURFACE_SLOP`（`GuardStates.java:494-512`），美术改动不要牵动这段。

## 11. 待查清单

- `docs/水环重设设计与实现指导-2026-10-22.md` 正文（本次未提供）。
- `GuardDefinition.personalRing()` 的全部数值。
- `GuardPose.worldLevel()`、`baseNormal(baseTilt)`、`ENTER_TICKS/HOLD_TICKS/RETURN_TICKS`。
- `RingBandGeometry.blendNormal/earliest/covers/PlaneBasis/chooseExitAngle/winding/signedTravel/ORBIT_TICKS`。
- `RingMeleeGeometry.protect`。
- `GuardPreview.look` 及其调用来源。
- 客户端水环渲染类（消费 `ClientRings.all()` 的地方）与 `WaterFormEffect`。
- `GuardSyncPacket.sendSnapshot` 的调用点（追踪开始/登录补发）。
- `ClientRings.clear()` 的调用点（断线、换维、资源重载）。
- `GuardStates.tick` 的驱动点、`GuardStates.closeOwned` 的调用点。
- `WaterReleaseDefinition.bodyGuard` 到 `GuardStates.schedule/activateReserved` 的路径。
- `syncToTracking` 符号在仓库中的实际名称与位置（材料中不存在）。