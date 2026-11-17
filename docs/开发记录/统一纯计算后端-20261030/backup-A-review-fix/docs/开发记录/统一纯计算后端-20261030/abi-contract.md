# 纯计算 ABI 1

本文是作者 B 实现 `xueximod_compute` 的冻结契约。规范公式在 `com.bslx.XueXiMod.shu.compute.JavaReference`。东、上、南就是世界 X、Y、Z。不要读世界、实体、NBT 或随机数来补这些数字。

## 库与 JNI

- 产物名：Windows `xueximod_compute.dll`；Linux `libxueximod_compute.so`；macOS `libxueximod_compute.dylib`。
- Java 只用 `System.load(绝对路径)`。不调用 `System.loadLibrary`，不搜索 `java.library.path`、工作目录或 `native/compute`。
- 类：`com.bslx.XueXiMod.shu.compute.XuexiComputeNative`。方法是包内静态 native。导出名大小写必须一致，包名中的 `XueXiMod` 不能写成小写。名字里没有下划线，所以没有 `_1`。
- 调用约定：包含 JDK 17 的 `jni.h`。Windows 上 `JNIEXPORT` 是 `__declspec(dllexport)`，`JNICALL` 按 `jni_md.h` 写。方法都是静态的，第二参数是 `jclass`。
- 本次环境只确认能编 Windows x64。Linux / macOS 文件名只是加载器接受的名字，本阶段没有这些二进制，也不要假定已经有源码。

```c
JNIEXPORT jint JNICALL Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_handshake
  (JNIEnv *, jclass, jintArray sink);
JNIEXPORT jint JNICALL Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trajectoryBatch
  (JNIEnv *, jclass, jint count, jintArray header, jdoubleArray input, jdoubleArray output);
JNIEXPORT jint JNICALL Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_turnBatch
  (JNIEnv *, jclass, jint count, jintArray header, jdoubleArray input, jdoubleArray output);
JNIEXPORT jint JNICALL Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trackingStepBatch
  (JNIEnv *, jclass, jint count, jintArray header, jdoubleArray input, jdoubleArray output);
JNIEXPORT jint JNICALL Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorOverlapsBatch
  (JNIEnv *, jclass, jint count, jdoubleArray input, jintArray output);
JNIEXPORT jint JNICALL Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorContactBatch
  (JNIEnv *, jclass, jint count, jdoubleArray input, jdoubleArray output);
JNIEXPORT jint JNICALL Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorBlockedBatch
  (JNIEnv *, jclass, jint count, jdoubleArray input, jintArray output);
```

操作编号只用于文档，不放进缓冲区。v1 一种操作一个函数。

| 编号 | 操作 | 能力位 |
|---|---|---|
| 1 | 封闭轨迹 | 1 |
| 2 | 有界转向 | 2 |
| 3 | 追踪步长 | 4 |
| 4 | 走廊相交 | 8 |
| 5 | 走廊接触距离 | 16 |
| 6 | 走廊阻挡 | 32 |

ABI 版本是 `1`。握手成功时 `sink[0] = 1`，`sink[1]` 是上面的能力按位或。未知高位 Java 忽略。缺某一位时，该操作的批量改走 Java，不调用对应 native。

## 握手

`handshake` 的 `sink` 至少 2 个 `jint`。成功时只写下标 0 和 1，返回 0。`sink` 为 null 返回 9，不抛 Java 异常。长度小于 2 返回 3，不写。版本不是 1 时 Java 整库停用，已加载的库留在进程里，但不再调用。

## 状态码

| 码 | 名字 | 输出 |
|---|---|---|
| 0 | OK | 全部 `count` 条都已写完 |
| 1 | BAD_VERSION | 不写 |
| 2 | BAD_COUNT | 不写 |
| 3 | BAD_LENGTH | 不写 |
| 4 | BAD_CAPACITY | 不写 |
| 5 | OVERFLOW | 不写 |
| 6 | ALIAS | 不写 |
| 7 | UNSUPPORTED | 不写 |
| 8 | NATIVE_FAILURE | 不写 |
| 9 | NULL_BUFFER | 不写 |
| 10 | BAD_HEADER | 不写 |

Java 在调用 native 前先检查。顺序是：条数、空指针、同一数组别名、头长度、输入长度、输出容量。第一个失败就返回，不调用 native，不改输出。

- 条数小于 0 或大于 4096：状态 2。
- 条数等于 0：状态 0，不要求数组非空，不写。
- 输入或输出是同一个 Java 数组对象：状态 6。不同类型的数组在 Java 里不能是同一对象。
- 头不够：状态 10。double 输入不够：状态 3。输出不够：状态 4。
- `count * stride` 用 64 位乘法。超过 `Integer.MAX_VALUE`：状态 5。以 4096 为上限时，现有 stride 不会溢出。
- 只定义输出区间 `[0, count * stride)`。成功调用也不清理尾部。
- 非 0 状态必须让输出数组保持进入调用前的内容。用 `Get*ArrayElements` 时，失败路径用 `JNI_ABORT` 释放。成功路径要拷回。不要保存指针，不要回调 Java，不要自建线程。
- native 返回非 0，或抛出 `UnsatisfiedLinkError` 时，Java 用同一批输入把整个输出区间重写为参考结果，再向业务返回 0。业务看不到半批。链接错误会关掉后续 native 调用。
- 返回 0 表示 native 结果被接受，Java 不再重算，也不逐项比对。只有整批写完才能返回 0。
- 不要抛受检异常代替状态码。运行时异常会原样传出，此时输出视为未定义，业务不得读取。
- 不要捕获 `Error` 来掩盖虚拟机错误。访问违例一类的进程崩溃不能恢复，也不要安装续跑用的信号处理器。

## 浮点

- 全程 `double`。MSVC x64 使用 `/fp:precise`，不要 `/fp:fast`、`/fp:contract` 或会收缩成 FMA 的选项。不要 `-ffast-math`。
- `+ - * /` 和比较按 IEEE 与 Java 的左结合。NaN 比较为假。
- `sin`、`cos`、`atan2`、`acos`、`sqrt` 用 `math.h` 的同名函数。它们与 HotSpot 不保证逐位相同。不要改成多项式或快近似。做不到同一公式就返回状态 7，并保持输出不动。
- Java `Math.PI` 的位型是 `0x400921FB54442D18`。12 度转角必须是 `(12.0 / 180.0) * PI`，不能写成 `(12.0 * PI) / 180.0`。
- 寿命乘 2 再除 3 是 Java 有符号 32 位整数，允许溢出后按二进制补码截断，再做向零的整数除法。

## 存在标记

整数 0 表示向量不存在。任何非 0 都表示存在，不要求恰好是 1。不存在时忽略对应分量，按 0 处理。`followCurrentLook` 必须从头部读出，然后丢弃，任何整数值都不参与公式。

## 轨迹

参考：`JavaReference.sampleClosed` 与 `JavaReference.writeTrajectory`。

已注册的 FUNCTION 回调、`progress - 0.04` 和 `fitVolume` 不进入本函数。代码 3 只表示未注册时的无目标直线。代码 -1 以及其它未知代码同样走无目标直线。

头部 stride 8：

| 偏移 | 字段 |
|---|---|
| 0 | 轨迹代码 |
| 1 | 起点存在 |
| 2 | 前向存在 |
| 3 | 目标点存在 |
| 4 | followCurrentLook，读取后忽略 |
| 5 | 当前视线存在 |
| 6 | lifetimeTicks，有符号 32 位 |
| 7 | ageTicks，有符号 32 位 |

输入 stride 13：起点东、上、南，前向东、上、南，速度，目标东、上、南，视线东、上、南。

输出 stride 7：位置存在（恰好 0.0 或 1.0），位置东、上、南，方向东、上、南。位置不存在时三个位置分量写 0。

轨迹代码：0 直线，1 垂落，2 跟随视线，3 未注册函数退回，4 涡旋。

前向先做轨迹归一。失败，或前向不存在，则用 `(0,0,1)`。

轨迹归一，对应原 `normalized` 再加 `Vec3.normalize`：

1. `lengthSquared = east*east + upward*upward + south*south`。小于 `1.0e-8` 则失败。不先判断有限性。
2. 否则 `length = sqrt(east*east + upward*upward + south*south)`。`length < 1.0e-4` 时结果是 `(0,0,0)`，这是成功，不是失败。
3. 否则分量除以 `length`。

直线：目标存在时，方向是 `normalize(目标 - 起点)`，失败则用上面的前向。位置是 `起点 + 方向 * (speed * ageTicks)`。目标不存在时方向就是前向，位置公式相同。代码 3 和未知代码强制按目标不存在处理。

垂落：不使用前向、速度、目标、视线。`fall = 0.5 * 0.08 * ageTicks * ageTicks`，从左往右，全程 double。位置 `(起点东, 起点上 - fall, 起点南)`，方向 `(0,-1,0)`。

跟随视线：先归一当前视线，失败则用前向。目标存在时再归一 `目标 - 起点`；成功就改用它，失败且视线存在则改用未归一的原始视线，否则用前向。位置就是起点。起点不存在时位置存在标记为 0。游戏适配层只在「跟随视线且目标点也为空」时允许空起点；其它空起点在适配层抛出空指针，与原先读取 `origin.x` 相同。批量数字协议本身没有对象，空起点按分量 0。

涡旋：`spiralTicks = max(8, (lifetimeTicks * 2) / 3)`，`max` 和乘法、除法都是 32 位整数。`ageTicks >= spiralTicks` 时停在起点，方向是水平前向 `(前向东, 0, 前向南)` 的轨迹归一，失败则为 `(0,0,1)`。否则：

```text
spiralProgress = ageTicks / (double) spiralTicks
phase = atan2(前向南, 前向东)
angle = phase + spiralProgress * 3.0 * PI * 2.0
radius = 5.0 * (1.0 - spiralProgress)
height = 12.0 * (1.0 - spiralProgress)
spinRate = 3.0 * PI * 2.0 / spiralTicks
shrinkRate = -5.0 / spiralTicks
dropRate = -12.0 / spiralTicks
位置 = (起点东 + cos(angle) * radius, 起点上 + height, 起点南 + sin(angle) * radius)
切向东 = -sin(angle) * radius * spinRate + cos(angle) * shrinkRate
切向上 = dropRate
切向南 = cos(angle) * radius * spinRate + sin(angle) * shrinkRate
```

切向轨迹归一失败时方向为 `(0,-1,0)`。速度不参与。

## 转向

参考：`JavaReference.turnToward` 与 `writeTurn`。输出永远是三个具体分量，没有空方向。

头部 stride 2：当前存在、期望存在。输入 stride 6：当前东、上、南，期望东、上、南。输出 stride 3：东、上、南。

转向单位化与轨迹归一不同：

1. 不存在，或任一分量不是有限数，则失败。判断顺序是东、上、南。
2. `length = sqrt(东*东 + 上*上 + 南*南)`。长度不是有限数，或 `length < 1.0e-8`，则失败。
3. 否则每个分量乘 `1.0 / length`。

当前失败时，期望成功就返回期望单位，否则 `(0,0,1)`。期望失败则返回当前单位。两者都成功时：

```text
dot = max(-1.0, min(1.0, 点积))
angle = acos(dot)
```

`angle <= 转角` 时返回期望单位。转角是 `(12.0 / 180.0) * PI`。否则切向原向量是 `期望单位 - 当前单位 * dot`，再做转向单位化。失败时：`abs(当前单位上) < 0.9` 用辅助向量 `(0,1,0)`，否则 `(1,0,0)`。叉乘是 `cross(当前单位, 辅助)`，再做上面的轨迹用 `Vec3.normalize`（`1.0e-4`），这里失败也得到 `(0,0,0)` 并继续使用。最后：

```text
mixed = 当前单位 * cos(转角) + 切向 * sin(转角)
方向 = Vec3.normalize(mixed)
```

## 追踪步长

参考：`JavaReference.trackingStep` 与 `writeTracking`。内部调用上面的转向，不要另写一份角度。

头部 stride 2：当前方向存在、目标向量存在。输入 stride 7：当前东、上、南，目标东、上、南，基础速度。输出 stride 4：方向东、上、南，步长。

`unitOrForward`：不存在、任一分量非有限、长度非有限或长度小于 `1.0e-8` 时得到 `(0,0,1)`，否则乘 `1.0 / length`。它不使用 `1.0e-4` 那条归一。

```text
travelSpeed = 基础速度有限且 > 0 时取本身，否则 0
held = unitOrForward(当前)
```

目标不存在、目标任一分量非有限，或 `lengthSquared < 1.0e-8`：方向为 held，步长 0。然后重新计算 `distance = sqrt(目标点积)`，不是复用上一步的平方根变量。距离非有限或小于 `1.0e-8`：同样步长 0。

否则 `turned = turnToward(held, 原始目标)`，`next = unitOrForward(turned)`。`targetUnit = 目标 * (1.0 / distance)`，不做有限性再检查。`alignment = dot(next, targetUnit)`。alignment 非有限则步长 0。夹取时先判断 `> 1`，再判断 `< -1`，不要用会把 NaN 收成边界的写法。`remaining = acos(夹取后)`。remaining 非有限或 `>= PI/2` 时步长 0。`remaining <= 1.0e-4` 时步长是 `min(travelSpeed, distance)`。否则：

```text
turnSine = sin(转角)
speedCap = min(travelSpeed, 0.8 * distance * turnSine)
cosine = cos(remaining)
cosine 非有限或 < 0 时改为 0
step = speedCap * cosine
```

两条分支之后，步长非有限或小于 0 则改为 0，大于 travelSpeed 则改为 travelSpeed。

## 走廊

相交参考 `JavaReference.overlaps` / `writeOverlaps`。输入 stride 15：起点东、上、南，前向东、上、南，长度、宽度、高度，盒子低角东、上、南，高角东、上、南。输出是 `jint` stride 1，恰好 0 或 1。

长度非有限或 `<= 0`，或宽度 `< 0`，或高度 `< 0`，则不相交。前向长度小于 `1.0e-8` 也不相交。先把前向除以自身长度。`abs(单位上) > 0.9` 时提示向量是 `(1,0,0)`，否则 `(0,1,0)`。右轴是 `cross(单位前向, 提示)`，长度小于 `1.0e-8` 则不相交，否则右轴再归一。上轴是 `cross(右轴, 单位前向)`。盒子中心是两端平均，半轴是两端差的绝对值乘 `0.5`。沿前向、横向、竖向的投影余量是半轴与对应轴分量绝对值的点积。

```text
沿前向命中 = along + alongPad >= 0 且 along - alongPad <= length
横向命中 = abs(lateral) <= width * 0.5 + lateralPad
竖向命中 = abs(vertical) <= height * 0.5 + verticalPad
```

三个都成立才相交。边界是闭的。

接触距离参考 `JavaReference.contactDistance` / `writeContact`。输入 stride 10：起点东、上、南，单位朝向东、上、南，中心东、上、南，alongPad。输出 stride 1，一个 double。

```text
along = dot(中心 - 起点, 单位朝向)
near = along - max(0.0, alongPad)
结果 = near < 0 时为 0，否则为 near
```

这里不检查有限性。输入和输出不能是同一个 double 数组。

阻挡参考 `JavaReference.blocked` / `writeBlocked`。输入 stride 2：接触距离、障碍距离。输出 `jint` 0 或 1。

```text
接触非有限或 < 0 则阻挡
否则障碍非有限则不阻挡
否则障碍 + 1.0e-4 < 接触 才阻挡
```

最后这个比较是严格小于。`level.clip` 不在本函数里。

## 游戏侧已经接上的入口

这些入口保持原签名。默认 Java 时，标量直接走参考公式，不因为没选 native 就做 JNI：

- `TrajectorySampler.sample`
- `TrackingSupport.turnToward`
- `SwordTrackingStep.plan`
- `MeleeCorridorMath.overlaps`、`contactDistance`、`blocked`

显式设置 native、库有效加载且对应能力支持时，上面已接入的六类单次调用按现有批量布局以 `count=1` 进入对应 JNI，不受公开批量下限 8 约束。这里只记录分发已经接到这些入口。运行加载、数值、游戏和性能都还没有验证，不能写成已经成功调用 C。

批量入口是 `ComputeCalls` 的 `trajectoryBatch`、`turnBatch`、`trackingStepBatch`、`corridorOverlapsBatch`、`corridorContactBatch`、`corridorBlockedBatch`。条数小于 8 时即使 native 已握手也走 Java。这个 8 不是测出来的加速点，现有每实体调用也不等待凑满 8 条。
