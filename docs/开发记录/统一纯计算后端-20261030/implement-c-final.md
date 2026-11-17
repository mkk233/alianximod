我先读取 C 实现任务说明，确认这次授权范围后再动手。任务已生效：按已冻结 ABI 写 C 内核和 JNI 桥，仍不编译。我先读契约和作者 A 的最终交回。契约和 `JavaReference` 已经对齐。我先把现有 `native/compute` 备份到 `backup-B`，再按冻结公式写 C 内核，这次不编译。# 作者 B 最终报告：ABI 1 的 C 内核

`released=true`。写入权已释放。设计 2/2、实现 3/3 已用完，未追加复审。本报告不是编译通过，也不是运行验证。

检查完成时间：2026-10-30 16:21:01 +08:00。

## 结论

`native/compute/src/xueximod_compute.c` 按冻结 ABI 1 实现了六个批量数值内核和握手。能力字是 `63`。Windows 产物名仍是 `xueximod_compute.dll`，但这次没有配置 CMake，也没有编出 DLL。Linux / macOS 只有源码和 `build-unix.sh`，没有编译。

## 实际能力

握手写入 `sink[0] = 1`、`sink[1] = 63`，返回 0。`63` 是下面六位的并：

| 操作 | JNI 函数 | 能力位 |
|---|---|---|
| 封闭轨迹 | `trajectoryBatch` | 1 |
| 有界转向 | `turnBatch` | 2 |
| 追踪步长 | `trackingStepBatch` | 4 |
| 走廊相交 | `corridorOverlapsBatch` | 8 |
| 走廊接触距离 | `corridorContactBatch` | 16 |
| 走廊阻挡 | `corridorBlockedBatch` | 32 |

公式来自 `JavaReference`：轨迹归一、直线、垂落、跟随视线、涡旋、转向、追踪步长、走廊相交、接触距离、阻挡。代码 3、`-1` 和其它未知轨迹代码走无目标直线。`followCurrentLook` 从头部读出后丢弃。寿命的 `(lifetimeTicks * 2) / 3` 按有符号 32 位补码截断，再做向零除法，然后与 8 取较大值。

`sin`、`cos`、`atan2`、`acos`、`sqrt` 使用 `math.h`。12 度是 `(12.0 / 180.0) * PI`，`PI` 位型 `0x400921FB54442D18`。源文件关闭 FMA 收缩。这不表示与 HotSpot 逐位相同。六项公式都已实现，所以当前库不返回状态 7；状态 7 仍表示“做不到同一公式”，返回时不写输出。

## C 导出与 ABI

类：`com.bslx.XueXiMod.shu.compute.XuexiComputeNative`。x64 导出名不加修饰：

- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_handshake`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trajectoryBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_turnBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trackingStepBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorOverlapsBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorContactBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorBlockedBatch`

调用前检查条数、空指针、double 数组身份别名、头长度、输入长度、输出容量和 64 位 `count * stride`。条数 0 返回 0，不要求数组非空，不写。非 0 状态不提交输出，`Get*ArrayElements` 得到的输出用 `JNI_ABORT` 释放。只有整批写完才返回 0。不保存 `JNIEnv`、数组或指针，不回调 Java，不建线程。`Get*ArrayElements` 返回空或已有挂起异常时返回 8，不调用 `ExceptionClear`。不安装信号处理器，也不声明能从访问违例或进程崩溃里恢复。

状态码与契约一致：0 成功，2 条数，3 输入长度，4 输出容量，5 溢出，6 别名，8 原生失败，9 空缓冲，10 头长度。握手在 `sink` 为空时返回 9，长度小于 2 时返回 3 且不写。状态 1 由 Java 在版本字不是 1 时停用整库，本库握手写的版本字就是 1。

## 供最终构建使用的命令

这次没有执行它。获得编译授权后，在项目根执行：

```text
powershell -NoProfile -File D:\alianximod\native\compute\build-windows.ps1
```

`D:\alianximod\native\compute\build-windows.cmd` 只转发到同一脚本。成功时 DLL 应在：

```text
D:\alianximod\native\compute\build\xueximod_compute.dll
```

脚本自己发现 vswhere、`vcvars64.bat`、JDK 17 JNI 头、CMake 和 Ninja。需要覆盖时使用 `-JdkHome`、`-VsInstall`、`-VsWhere`、`-Vcvars64`、`-Cmake`、`-Ninja`。不要把 DLL 拷进游戏目录，也不要靠这次源码自动打开 native。

Linux / macOS 的源码入口是 `bash native/compute/build-unix.sh`。它没有被用来编译。

## 改动文件

变更前副本在 `docs/开发记录/统一纯计算后端-20261030/backup-B/native/compute/`。那里没有 C 源，因为当时还没有这个文件。

本次写入或修改：

| 文件 | 字节 |
|---|---|
| `native/compute/src/xueximod_compute.c` | 44883，新建 |
| `native/compute/CMakeLists.txt` | 5578 |
| `native/compute/build-windows.ps1` | 15452 |
| `native/compute/build-unix.sh` | 6825 |
| `native/compute/README.md` | 4597 |
| `docs/开发记录/统一纯计算后端-20261030/implementation-B-final.md` | 6902 |
| `docs/开发记录/统一纯计算后端-20261030/evidence-B.json` | 元数据，`released=true` |

`build-windows.cmd` 与 `.gitignore` 沿用准备阶段的内容，没有再改。没有改 Java、Gradle、配置、制作表、总用户说明或 ABI 契约。

## 供总说明同步的字段

总说明仍由作者 A 维护。需要同步的事实是：

- C 源是 `native/compute/src/xueximod_compute.c`，ABI 版本 1，能力字 63，上面 7 个 JNI 符号都已实现。
- Windows 动态库名 `xueximod_compute.dll`。授权构建后的默认位置是 `native/compute/build/xueximod_compute.dll`。本次磁盘上还没有这个 DLL。
- Linux 文件名 `libxueximod_compute.so`，macOS 文件名 `libxueximod_compute.dylib`。只有构建入口，没有二进制。
- 加载方式仍是 Java 侧的 `System.load(绝对路径)`。C 侧不搜索路径，也不自行启用。
- 超越函数来自 `math.h`，不保证与 HotSpot 逐位相同；没有用近似多项式。
- 构建命令是上面的 `build-windows.ps1`。浮点是 C11、`/fp:precise`、关闭 FMA 收缩。

## 必须保留的限制

- 未追加复审。待编译，待加载，待游戏内验证。不能把源码存在写成 DLL 已通过或数值已经逐位一致。
- `math.h` 与 Java 17 `Math` 的 `sin`、`cos`、`atan2`、`acos`、`sqrt` 允许有舍入差异。不能靠“误差很小”把它们改成别的公式。
- 世界、实体、NBT、随机数、文件、网络和线程调度不在这个库里。
- 条数小于 8 仍由 Java 决定走参考实现。C 按契约接受 0 到 4096，不在库内另设门槛。
- 成功时只写 `[0, count * stride)`，不清理输出尾部。
- 非 0 状态、挂起异常和原生失败都不能被当成半批成功。
- 访问违例和进程崩溃不能恢复。
- 本机只准备了 Windows x64 工具链。Linux / macOS 未验证。

## 本会话检查

- PowerShell 解析 `build-windows.ps1`：0 个语法错误。没有执行该脚本，因此没有进入 CMake。
- `D:\Program Files\Git\bin\bash.exe -n build-unix.sh` 退出码 0。这只是语法检查，不是 Unix 编译。
- 七个 JNI 导出名在 C 源里各出现一次。没有 TODO，也没有空实现返回值。
- `native/compute/build` 不存在。没有 cl、cmake、ninja、javac、Gradle、JNI 加载、游戏或打包。
- 本会话没有审批拒绝。没有降模型，没有加 force，没有改安全设置。
