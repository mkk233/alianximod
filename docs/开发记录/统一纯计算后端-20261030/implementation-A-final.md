# 作者 A 最终报告

额度已用完，未追加复审；待统一编译。本阶段没有 Gradle、没有模组 compileJava、没有 C 编译、没有游戏。写入权释放。

## 完成了什么

Java 是默认纯计算后端。同一套数字布局同时留给 C。现有四个类保持原签名，只把已经定位的公式转调到 `JavaReference`。不选 native、没有动态库时，游戏仍走这些公式。

C 侧只完成 Java 声明、绝对路径加载、版本握手和批量分发。动态库由作者 B 按 `abi-contract.md` 实现。默认不加载、不搜索目录、不下载。

## 文件

新增：

- `src/main/java/com/bslx/XueXiMod/shu/compute/ComputeCodes.java`
- `src/main/java/com/bslx/XueXiMod/shu/compute/ComputeOutput.java`
- `src/main/java/com/bslx/XueXiMod/shu/compute/JavaReference.java`
- `src/main/java/com/bslx/XueXiMod/shu/compute/XuexiComputeNative.java`
- `src/main/java/com/bslx/XueXiMod/shu/compute/ComputeSelection.java`
- `src/main/java/com/bslx/XueXiMod/shu/compute/ComputeCalls.java`

转调：

- `TrajectorySampler.java`：已注册 FUNCTION 仍在原方法里调用回调；其余封闭轨迹进入参考实现。
- `TrackingSupport.java`：`turnToward` 转调；`turnLimitRadians` 仍返回同一个 12 度。索敌查询未动。
- `SwordTrackingStep.java`：`plan` 转调。`retainedMark` 未动。
- `MeleeCorridorMath.java`：三个纯函数转调。方块射线仍在 `MeleeCorridor`。

性能枚举只在末尾追加，原有序号未改，CSV 列未改：

- `PerfMetric.COMPUTE_BACKEND`
- `PerfCounter.COMPUTE_LOGICAL_REQUESTS`
- `PerfCounter.COMPUTE_JAVA_EXECUTIONS`
- `PerfCounter.COMPUTE_NATIVE_EXECUTIONS`
- `PerfCounter.COMPUTE_JAVA_FALLBACKS`

文档：

- `docs/纯计算后端使用说明.md`
- `docs/开发记录/统一纯计算后端-20261030/abi-contract.md`
- 本报告
- `evidence-A.json`
- `backup-A/` 下六个改动前的 Java 文件

## 调用边界

标量入口每次直接调用 `JavaReference`，不分配批量数组，也不进入 JNI。当前每实体、每攻击体的一次调用都在这条路径上。

批量入口是 `ComputeCalls` 的六个 `*Batch` 方法。条数小于 8 时，即使动态库已经握手，也整批用 Java。8 不是实测加速值。没有任何现有循环会为了凑满 8 条而推迟或重排。

native 只在同时满足这些条件时调用：启动参数精确为 `native`，库是绝对路径且文件名符合平台产物名，握手版本为 1，该操作能力位存在，条数大于等于 8 且不超过 4096。返回非 0 或链接失败时，同一输入整批改由 Java 写完输出。链接失败后停用后续 native 调用。校验失败不写输出，也不调用 native。

`COMPUTE_JAVA_EXECUTIONS` 与 `COMPUTE_JAVA_FALLBACKS` 互斥。逻辑请求数对上其中实际发生的一项。关闭采样时不额外读取 `nanoTime`。选择后端的那一行日志每个进程一次。

## 未完成

- 作者 B 的 C 源码、CMake 和动态库都还没有。本报告不把握手成功写成已经发生。
- Linux、macOS 二进制没有编译。加载器只接受约定文件名。工具链事实仍只覆盖 Windows x64。
- 三维交错、一分为三、`CastFrame` 未接。
- 已注册轨迹回调、体积贴合、实体查询、方块射线未进纯计算。
- 没有统一编译，也没有游戏实测。不能据此声称数值逐位一致或帧数变化。
- 命名门禁仍报告 `OriginalHumanoid.java` 原有两处短名。该文件不在本次写入范围，没有改。

## ABI

冻结正文是同目录 `abi-contract.md`。实现时以它和 `JavaReference` 为准。

- 包与类：`com.bslx.XueXiMod.shu.compute.XuexiComputeNative`
- 版本 1。能力位：轨迹 1，转向 2，追踪步长 4，相交 8，接触 16，阻挡 32。
- JNI 前缀：`Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_`
- 函数：`handshake`、`trajectoryBatch`、`turnBatch`、`trackingStepBatch`、`corridorOverlapsBatch`、`corridorContactBatch`、`corridorBlockedBatch`
- 状态 0 到 10 的含义、输出是否可写、别名和半批规则见契约。
- 12 度必须是 `(12.0 / 180.0) * Math.PI`。`Math.PI` 位型 `0x400921FB54442D18`。
- 编译使用 precise 浮点，不用 fast-math，不用 FMA 收缩。libm 与 HotSpot 不保证逐位相同；不能改用近似公式。
- 启动参数：`-Dxueximod.compute.backend=java|native` 与 `-Dxueximod.compute.library=绝对路径`。
- Windows 产物名 `xueximod_compute.dll`。
