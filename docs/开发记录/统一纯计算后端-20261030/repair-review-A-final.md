先读返修任务说明，只按它划定的范围改。R1 只接非法路径，R2 只在活适配里保留涡旋停驻的空位置。先留下修改前副本。此为按追加轮结论修复，修后未再审，待最终编译。用户追加的一轮已用完，累计实现 4 轮；修复后未授权再审。写入所有权已释放（`released=true`）。没有编译，没有跑测试，没有加载 DLL，没有启动游戏，没有打包。下面的输入走读只核对这次改动是否对着已知路径，不是正式审查结论。

## R1

`ComputeSelection.resolve` 在 `Path.of` 处只捕获 `InvalidPathException`。原因记为 `library-invalid-path`，并带上该异常自己的说明，然后 `acceptJava` 返回。`ensureResolved` 在 `resolve` 正常返回后把 `resolved` 设为 true，后续入口不再解析这条路径，也不会重复抛同一配置异常。没有捕获其它运行异常或 `Error`。加载、握手和计算里的业务异常仍按原路径传播。

## R2

`ComputeCalls.sampleClosed` 只对「涡旋、起点对象为 null、且 `ageTicks >= max(8, (lifetimeTicks * 2) / 3)`」放行。停驻刻数仍用原来的 32 位乘法和除法，没有改阈值。放行后 Java 或 count=1 JNI 照旧计算；两条活路径在把结果交给适配之后，只把 `positionPresent` 清成 0。`sampledPosition` 因此返回 null，方向保留数字结果里的水平前向。已知输入 `VORTEX`、`origin=null`、`forward=(0,0,1)`、`lifetime=12`、`age=8` 时，`spiralTicks=8`，走停驻：位置为 null，方向为 `(0,0,1)`，不会变成原点。`age` 还小于停驻刻数时仍调用原来的 `absentOrigin`，读取 `origin.x` 抛出空指针，不进入会把空起点写成 `(0,0,0)` 的数字路径。

`TrajectorySampler` 没有改。它本来就用 `sampledPosition` / `sampledDirection` 组装 `Sample`。`ComputeLiveDispatch`、`ComputeOutput`、`JavaReference` 没有改。

## ABI 与 C

native ABI 的签名、字段、stride、错误码和公式常量都没有改。C 源和 Windows 构建脚本没有改，那是作者 B 的范围。数字协议里空起点仍是分量 0，涡旋停驻仍写出位置存在 1。null 位置只存在于活适配的对象层。

## 文档

`docs/纯计算后端使用说明.md` 写了非法路径的一次回退，以及涡旋停驻空起点的活调用语义，并写明这不是已经成功调用 C。`abi-contract.md` 补了同样的加载说明和涡旋对象语义；契约数值没有改。

## 未完成

- 没有编译 Java 或 C，没有运行该输入，没有加载 DLL，没有数值、游戏或性能验证。
- R3 的 Windows 构建脚本编码不在本单。
- 变量名检查仍只报未改动的 `OriginalHumanoid.java` 两处旧名（`i`、`to`）。本次改过的文件没有新增违规。检查脚本自己的解析编译不是模组 `compileJava`。

## 备份与证据

修改前副本在 `docs/开发记录/统一纯计算后端-20261030/backup-A-review-fix/`：

| 路径 | 字节 | UTC |
| --- | ---: | --- |
| `ComputeSelection.java` | 8783 | 2026-10-30T08:15:39.391Z |
| `ComputeCalls.java` | 22909 | 2026-10-30T08:15:59.284Z |
| `docs/纯计算后端使用说明.md` | 4635 | 2026-10-30T08:56:35.771Z |
| `abi-contract.md` | 14859 | 2026-10-30T08:24:08.245Z |

当前文件：`ComputeSelection.java` 9122 字节（2026-10-30T09:16:07.011Z），`ComputeCalls.java` 23770 字节（2026-10-30T09:16:08.428Z），使用说明 5217 字节（2026-10-30T09:16:29.041Z），`abi-contract.md` 15536 字节（2026-10-30T09:16:33.823Z），`repair-review-A-final.md` 2963 字节（2026-10-30T09:16:56.941Z）。证据 `evidence-A-review-fix.json` 为 2083 字节，`released=true`，无哈希；自身时间记为对齐前读到的 2026-10-30T09:17:25.846Z，这次对齐不改变长度。
