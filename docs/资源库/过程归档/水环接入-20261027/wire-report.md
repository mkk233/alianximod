# 水环同步 / 生命周期

本轮只做网络与客户端世界卸载。未改渲染、资源、`ModEvents`、玩法状态机。未启动 Gradle，未跑测试，未进游戏。

## 路径

- `src/main/java/com/bslx/XueXiMod/network/GuardSyncPacket.java`
- `src/main/java/com/bslx/XueXiMod/network/ModNetwork.java`
- `src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardStates.java`（只构包和删除目的地）
- `src/main/java/com/bslx/XueXiMod/client/GuardClientLifecycle.java`（新）
- `src/test/java/com/bslx/XueXiMod/network/GuardSyncWireCheck.java`
- `src/test/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardSnapshotPoseCheck.java`

未改 `GuardRingRenderer`、其他渲染文件、`ReleaseBaseEvents`、`ModEvents`。死亡、登出、换维仍走原来的 `endCast → closeOwned`。开始追踪仍走原来的 `sendSnapshot`。

## 做了什么

1. 包只多了 `animationAgeTicks`。`FULL` / `POSE` / `REMOVE` 构包时写入 `max(0, serverTick - animationStart)`。`animationStart` 仍是激活身份，刷新不改它。`encode` / `decode` 对称。`ModNetwork` 协议由 `"1"` 改为 `"2"`，两端仍用同一字符串严格相等。
2. 客户端年龄是「收到的年龄 + 本地游戏 tick 增量 + partial」。锚点是收包时的 `clientGameTick`，不用服务器 tick 减 `gameTime`，旋转也不用墙钟。同一次 `animationStart` 的重复 `FULL` / `POSE` 不会把年龄打回 0；只有收到的年龄比本地已经推进的更大时才向前改锚。新观察者从收到的年龄起播。`revision` 更旧，或同 `revision` 且 `sequence` 更旧，不能覆盖。更大的 `animationStart` 是新激活，不因旧环 revision 很高而被丢掉。更小的 `animationStart` 的 `FULL` / `POSE` 不能重建或摆动当前环；没有对应快照的 `POSE` 也不会新建环。
3. `FULL` 的 `from` 改为服务端当前插值法线，`toward` 仍是当前目标，`poseTicks` 改为剩余过渡 tick。过渡已结束或本来没有过渡时，`from` 与 `toward` 用同一个向量，剩余为 0。不写服务器上的 `fromNormal` / `towardNormal` / `phaseStart` / `phaseEnd`。`POSE` 仍在动作开始时带上这次过渡的 tick 数。
4. `GuardClientLifecycle` 在客户端 `LevelEvent.Unload` 且 `isClientSide` 时清空 `ClientRings`。原来的登出清空还在 `ModEvents`，没有动。
5. `broadcastRemove` 先按原样发给主人当前维度 128 格内的观察者。若 `BodyGuard.serverLevel` 是另一份关卡，再把同一条 `REMOVE` 发给那份关卡里的玩家。`REMOVE` 带真实 `animationStart`。客户端只删相同激活，且 revision 不落后、同 revision 时 sequence 不落后的环。

渲染将调用的接口：

`GuardSyncPacket.ClientRings.View.animationAge(long clientGameTick, float partialTick)`

返回值不小于 0。`from` / `toward` / `poseStartMillis` / `poseTicks` 仍是原来的姿态字段，姿态时钟没有改成 tick。`applyPose` 仍把 `poseTicks` 存成至少 1，所以线上剩余为 0 时，视图里的姿态时长是 1，两端法线已经相同。旋转年龄不要读 `poseStartMillis`。

另外两处是测试和卸载缝，不是渲染契约：`ClientRings.accept(packet, clientGameTick)` 是包内可见的纯入口；游戏收包仍调用原来的 `accept(packet)`，再由 `GuardClientLifecycle.clientGameTick()` 读本地关卡 `gameTime`。`clearIfClientSide(boolean)` 是卸载策略，事件方法先判断客户端关卡再调用它。

## 已静态核对

- 刷新分支仍只增加 `revision` 和到期时间，不改 `animationStart`，也不改持续时间、维护费、减伤、偏转、接触附魔。
- 同身份旧包在更新锚点之前就被 `revision` / `sequence` 丢掉，因此乱序包上更大的年龄也不会把相位拉回去。
- 新身份会换锚点，不继承旧环已经走过的年龄。
- 旧 `REMOVE` 对不上 `animationStart`，或 revision 落后，不会删当前环。对得上且 revision 不落后才会删。
- 主人还在保存环的那份关卡时，不重复发送删除。离开之后不再用跨维度距离套 128 格，否则旧维度里的人收不到。
- 姿态插值沿用 `RingBandGeometry.blendNormal`。中途 `from` 是当前混合结果，`toward` 是目标；结束后两个字段相同，剩余 tick 为 0。
- 定向检查写的是：刷新不断相、重复和乱序 `FULL` / `POSE`、新身份不被旧高 revision 拒绝、晚加入年龄、旧删除不拆新环、负年龄与倒退 tick、`FULL` 当前法线和剩余 tick、只在客户端卸载时清空、包往返、协议 `"2"`。这些检查没有执行。

命名门禁扫过整个 `src`。本次改过的文件没有新的短名或禁止缩写。脚本仍因未改动的 `OriginalHumanoid.java` 里原有 `i`、`to` 退出码为 1，按门禁约定没有顺手改那个文件。

## 待编译

主控合并渲染改动后统一编译，并运行：

- `com.bslx.XueXiMod.network.GuardSyncWireCheck`
- `com.bslx.XueXiMod.shu.technique.release.guard.GuardSnapshotPoseCheck`

本轮没有 `gradlew`，没有客户端运行，也没有打包。同一 tick 内先拆再开、且删除包晚于新的 `FULL` 到达时，两边 `animationStart` 相同，新的 revision 1 仍会被当成旧包丢掉。身份按已裁决的 `animationStart` 区分，没有另加字段。

## 姿态时钟修正

同一次激活、同一 `revision`、同一 `sequence` 的 `FULL` / `POSE` 仍可向前校正年龄；`FULL` 仍更新半径等尺寸。覆盖这些字段之前先算出 `freshActivation`、`sameRevision`、`sameSequence`。只有新激活，或 revision / sequence 确实前进的姿态，才调用 `applyPose`，因此重复包不再改 `poseStartMillis`。

`POSE` 必须 `packet.revision == view.revision`。更高 revision 的姿态包整份丢弃，不改视图。新 revision 仍只由 `FULL` 建立。`packetLags` 仍用小于判断，同 sequence 的 `REMOVE` 可以删除。

`GuardSyncWireCheck.duplicatePoseClockStays` 覆盖：重复 `FULL` / `POSE` 保留姿态时钟、合法新 `POSE` 更新姿态、高 revision `POSE` 不改变视图。该检查尚未执行。没有新增墓碑，也没有改 `animationStart` 的类型或身份协议。
