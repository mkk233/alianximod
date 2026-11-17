我先读取这份任务说明，确认授权范围后再按它执行。任务是把显式 native 接到现有数学调用上。我先读当前分发和四个适配类，确认不改冻结 ABI。现有批量入口仍保留 8 条阈值。显式 native 且握手成功时，已接入的单次计算会按现有 ABI 以 `count=1` 进入 JNI，默认 Java 仍直接走参考公式。单次 JNI 不能每次新建 lambda。我改成直接调用，并去掉错误的状态上报。分发已经接到现有调用上。正在写最终报告和证据，并核对文件长度与时间。额度已用完，未追加复审；待统一编译。写入所有权已释放（`released=true`）。本单没有改 ABI，没有改公式，没有编译，没有启动游戏。

## 显式 native 时现有调用怎么走

用户设置 `-Dxueximod.compute.backend=native`，并给出绝对库路径，握手成功且对应能力位打开时，`ComputeSelection.liveRoute` 返回 native。这条路径不看批量下限 8。

已经接入的六个入口各自把一条数字装进现有批量布局，以 `count=1` 调现有 JNI：

| 现有入口 | JNI | 能力位 |
| --- | --- | --- |
| `ComputeCalls.sampleClosed` | `trajectoryBatch` | 轨迹 |
| `ComputeCalls.turnToward` | `turnBatch` | 转向 |
| `ComputeCalls.trackingStep` | `trackingStepBatch` | 步长 |
| `ComputeCalls.overlaps` | `corridorOverlapsBatch` | 走廊重叠 |
| `ComputeCalls.contactDistance` | `corridorContactBatch` | 走廊接触 |
| `ComputeCalls.blocked` | `corridorBlockedBatch` | 走廊阻挡 |

步长只进一次 `trackingStepBatch`，不再另调转向 JNI。没有新开关，也没有把一条记录复制成 8 条。成功后轨迹、转向、步长写入原有 `ComputeOutput`，再由原来的适配读取；走廊的布尔和距离在返回前先抄进局部变量。四个适配类本单没有改：`TrajectorySampler`、`TrackingSupport`、`SwordTrackingStep`、`MeleeCorridorMath` 本来就调用 `ComputeCalls`。已登记的 FUNCTION 回调仍留在 `TrajectorySampler`，不进这条分发。

单条 JNI 可能更慢。这个模式只表示后端已经接上，可以对比，不表示帧数或 tick 变快。

缓冲在 `ComputeLiveDispatch`。每条线程最多 4 层，每层的数组按现有 stride 固定，第一次用到该层才分配，之后复用。每次调用重写全部输入，并在 JNI 前把输出清零，避免上一拍残留。默认 Java 不取这些缓冲。超过 4 层时本层不调用 JNI，改走 Java，避免和外层共用一块输出。不同线程各有自己的缓冲。

## 默认 Java

未请求 native、库路径无效、握手失败或之后链接失败被关掉时，`liveRoute` 返回 Java。轨迹、转向、步长直接调用 `JavaReference` 的标量写入；走廊直接调用 `JavaReference` 的对应方法。这条路径不分配批量临时数组，也不进入 JNI。公式和阈值仍以 `JavaReference` 为准，本单没有改它。

公开的六个 `*Batch` 方法仍保留条数下限 8。少于 8 条的批量调用继续用 Java。游戏里的现有适配不走这六个方法，所以这个下限不会把显式 native 模式下的轨迹、转向、步长和走廊留在 Java。单次入口也不会再进入 `*Batch`，因此不会把同一次请求记成两笔。

## 回退

能力位缺失、native 状态非 0、`UnsatisfiedLinkError`：同一组输入改由 `JavaReference` 完成，记一次回退，不把 native 输出交回适配。链接错误会关掉后续 native。其它运行时异常照常抛出，不吞业务异常。深度用尽时记一次 Java 执行，不记回退，因为没有发起 JNI。一次逻辑请求只记 Java、native、回退中的一项。性能枚举没有新增。

## 文档

`docs/纯计算后端使用说明.md` 已改成：默认直达 Java；显式 native 且握手成功时，已接入的单次计算以 `count=1` 进入对应 JNI；单条 JNI 可能更慢，只用于接入和对比；公开批量仍保留下限 8；回退和四层缓冲与上面一致。

`abi-contract.md` 末尾仍写着标量游戏入口始终留在 Java、条数小于 8 不进 JNI。这句和现在的单次分发不一致，待总文档收尾时同步。本单没有改这份契约，因为作者 B 正在读它。`ComputeCodes` 里关于下限 8 的注释同样仍把该下限说成单次 JNI 边界；该文件本单冻结，没有改。批量入口的实际行为仍是少于 8 条走 Java。

## 未完成

- 没有运行 `compileJava`，没有编译 C，没有测试，没有启动游戏，没有打包。
- C 源码和动态库仍由作者 B 编写。本机尚未出现一次成功握手，所以这条 JNI 路径还没有在进程里跑过。
- Linux、macOS 库文件本单不制作。
- 变量名检查只扫到未改动的 `OriginalHumanoid.java` 两处旧名（`i`、`to`）。本次改过的文件没有新增违规。检查脚本自己的解析编译不是模组 `compileJava`。

## 备份与证据

修改前副本在 `docs/开发记录/统一纯计算后端-20261030/backup-A-live-native/`：`ComputeCalls.java`（14733 字节，2026-10-30T08:04:41.591Z）、`ComputeSelection.java`（8194 字节，2026-10-30T08:04:07.080Z）、`docs/纯计算后端使用说明.md`（2821 字节，2026-10-30T08:06:08.120Z）。`ComputeLiveDispatch.java` 是本单新增，没有修改前副本。

证据在 `docs/开发记录/统一纯计算后端-20261030/evidence-A-live-native.json`（1851 字节，记录时间 2026-10-30T08:19:12.335Z），`released=true`，无哈希。同目录报告 `implementation-A-live-native-final.md` 为 4688 字节，2026-10-30T08:17:43.662Z。当前源码：`ComputeCalls.java` 22909 字节（2026-10-30T08:15:59.284Z），`ComputeSelection.java` 8783 字节（2026-10-30T08:15:39.391Z），`ComputeLiveDispatch.java` 16481 字节（2026-10-30T08:15:20.843Z），`docs/纯计算后端使用说明.md` 3412 字节（2026-10-30T08:16:43.325Z）。
