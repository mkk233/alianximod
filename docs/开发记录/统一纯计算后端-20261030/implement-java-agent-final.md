我先读取任务说明和执行约束，确认这次授权范围后再动手。这是已授权的实现任务，额度已用完，不再另开复审。我先读本目录记录和四份既有纯计算类，再按冻结范围接线。范围已明确：只接四份已定位的纯数值类，并新增 `shu/compute`。我先把现行公式和性能计数门控读完。现行公式和性能门控已经对齐。我先备份要改的既有文件，再写入纯计算接口并把这四处调用接到 Java 参考实现。备份已落在 `backup-A`。接下来写入纯计算契约、Java 参考实现和 JNI 桥，再把四处现有调用改成转调。命名检查里，本次改过的文件没有新违规。接下来写冻结 ABI、使用说明和交付记录。额度已用完，未追加复审；待统一编译。写入权释放。本阶段没有跑模组 `compileJava`、没有编 C、没有进游戏。

Java 是默认纯计算后端。同一套数字布局留给 C。不选 native、没有动态库时，现有轨迹、转向、追踪步长和近战走廊仍按原公式计算。

## 接线

标量入口直接调用 `JavaReference`，不分配批量数组，也不进 JNI。当前每个实体、每个攻击体的一次调用都在这条路径上。

- `TrajectorySampler.sample`：已注册 FUNCTION 仍在原方法里走回调；直线、垂落、跟随视线、涡旋，以及未注册函数的直线退回进入参考实现。
- `TrackingSupport.turnToward` 转调。`turnLimitRadians` 仍是同一个 12 度。索敌查询未动。
- `SwordTrackingStep.plan` 转调。`retainedMark` 未动。
- `MeleeCorridorMath` 的相交、接触距离、阻挡转调。方块射线仍在 `MeleeCorridor`。

批量入口是 `ComputeCalls` 的六个 `*Batch` 方法。条数小于 8 时，即使动态库已经握手，也整批用 Java。这个 8 不是实测加速值。现有循环不会为了凑满 8 条而推迟或重排。单次 JNI 不是优化。

native 只在同时满足这些条件时调用：`-Dxueximod.compute.backend=native`，`-Dxueximod.compute.library` 是绝对路径且文件名符合平台产物名，握手版本为 1，该操作能力位存在，条数在 8 到 4096 之间。失败或链接错误时，同一输入整批改由 Java 写完。链接失败后停用后续 native。校验失败不写输出。进程级 native 崩溃不捕获、不续跑。

性能枚举只在末尾追加了 `COMPUTE_BACKEND` 和四项计数。原有序号和 `ticks.csv` 列未改。Java 执行与回退互斥。关闭采样时不额外读 `nanoTime`。所选后端或回退原因每个进程记一行。

## 文件

新增 `shu/compute/` 下的 `ComputeCodes`、`ComputeOutput`、`JavaReference`、`XuexiComputeNative`、`ComputeSelection`、`ComputeCalls`。改动前备份在 `docs/开发记录/统一纯计算后端-20261030/backup-A/`。使用说明是 `docs/纯计算后端使用说明.md`。路径、字节数和 UTC 时间在 `evidence-A.json`，`released=true`。

## ABI

冻结正文是 `docs/开发记录/统一纯计算后端-20261030/abi-contract.md`，公式以 `JavaReference` 为准。

- 类：`com.bslx.XueXiMod.shu.compute.XuexiComputeNative`
- 版本 1。能力位：轨迹 1，转向 2，追踪步长 4，相交 8，接触 16，阻挡 32。
- JNI 前缀：`Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_`
- 函数：`handshake`、`trajectoryBatch`、`turnBatch`、`trackingStepBatch`、`corridorOverlapsBatch`、`corridorContactBatch`、`corridorBlockedBatch`
- Windows 产物名 `xueximod_compute.dll`。只按绝对路径 `System.load`，不搜索目录。
- 12 度是 `(12.0 / 180.0) * Math.PI`。`Math.PI` 位型 `0x400921FB54442D18`。
- 使用 precise 浮点，不用 fast-math，不用 FMA 收缩。libm 与 HotSpot 不保证逐位相同，不能改成近似公式。
- 状态非 0 不得修改输出。返回 0 才表示整批写完。Java 不把半批交给业务。

## 未完成

作者 B 的 C 源码和动态库还没有，握手成功并没有发生。Linux、macOS 二进制没有编译。三维交错、一分为三和 `CastFrame` 未接。已注册轨迹回调、体积贴合、实体查询和方块射线未进纯计算。

命名门禁对本次改过的文件没有新违规。`OriginalHumanoid.java` 里原有的两处短名不在本次范围，没有改。
