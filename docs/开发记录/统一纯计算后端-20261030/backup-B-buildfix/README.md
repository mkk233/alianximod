# xueximod_compute

ABI 1 的 C 内核在 `src/xueximod_compute.c`。它实现 `JavaReference` 里的同一批公式，并通过下面 7 个 JNI 导出交给 `com.bslx.XueXiMod.shu.compute.XuexiComputeNative`。类名里的 `XueXiMod` 保持大小写。x64 上这些名字不加 stdcall 修饰：

- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_handshake`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trajectoryBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_turnBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trackingStepBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorOverlapsBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorContactBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorBlockedBatch`

握手成功时写入版本 `1` 和能力字 `63`（轨迹 1、转向 2、追踪步长 4、相交 8、接触 16、阻挡 32）。六项都有真实计算。状态 7 仍保留给无法按同一公式实现的情况；当前库不返回它。`sin`、`cos`、`atan2`、`acos`、`sqrt` 用 `math.h`。这和 HotSpot 不保证逐位相同，也没有改成多项式。

库基名是 `xueximod_compute`。Java 只用 `System.load(绝对路径)`。这里的脚本不把产物复制到资源目录、Gradle 输出、`run/` 或游戏目录，也不改 `PATH` / `JAVA_HOME`，不下载工具，不链接 JVM。

## 构建入口

Windows，在项目根执行：

```text
powershell -NoProfile -File D:\alianximod\native\compute\build-windows.ps1
```

`build-windows.cmd` 只转发到这个脚本，不改执行策略。脚本用 vswhere 找带 `VC.Tools.x86.x64` 的 Visual Studio，再 `call vcvars64.bat`，并要求 `cl.exe` 位于 `Hostx64\x64`。JDK 必须是带 `include/jni.h` 和 `include/win32/jni_md.h` 的 17。CMake 和 Ninja 可以用参数覆盖。

Linux / macOS：

```text
bash native/compute/build-unix.sh
```

这条入口只在源码层面支持。写这份说明的 Windows 机器没有把它拿去编译，不能当成 Linux 或 macOS 已经验证。

常用 Windows 参数：`-JdkHome`、`-VsInstall`、`-VsWhere`、`-Vcvars64`、`-Cmake`、`-Ninja`、`-Generator`、`-Configuration`、`-BuildDir`。`-Vcvars64` 的文件名必须是 `vcvars64.bat`。`-BuildDir` 只能是 `build`、`out`，或解析后正好等于这两个目录的路径。`-Clean` 也只删除那一个目录。

默认生成器是已发现的 Ninja。没有 Ninja 且 Visual Studio 主版本是 17 时，用 `Visual Studio 17 2022 -A x64 -T host=x64`。

## 语言、浮点和产物

C11。JDK 17 头文件只用于声明，不链接 `jvm.lib`、`libjvm.so` 或 `libjvm.dylib`。

MSVC：`/std:c11 /fp:precise /W3`，Release 另有 CMake 默认的 `/O2`。源文件关闭 `fp_contract`。不用 `/fp:fast`、`/Qfast_transcendentals`、`/GL`、`/LTCG`。

GCC / Clang：`-std=c11 -ffp-contract=off -fno-fast-math -fno-unsafe-math-optimizations -fno-associative-math -fno-reciprocal-math`。GCC 另加 `-fexcess-precision=standard`。Release 使用 `-O2`。源文件有 `#pragma STDC FP_CONTRACT OFF`。

12 度转角按 `(12.0 / 180.0) * PI` 计算。`PI` 的位型是 `0x400921FB54442D18`。构建定义 `XUEXIMOD_COMPUTE_FP_PRECISE=1`。指针宽度必须是 8 字节。

| 系统 | 产物 | 本机状态 |
|---|---|---|
| Windows x64 | `native/compute/build/xueximod_compute.dll` | 源码已写，本次没有配置或编译 |
| Linux | `native/compute/build/libxueximod_compute.so` | 仅有构建入口，未编译 |
| macOS | `native/compute/build/libxueximod_compute.dylib` | 仅有构建入口，未编译 |

使用 `-BuildDir out` 时，把路径中的 `build` 换成 `out`。

## 退出码

| 码 | 含义 |
|---|---|
| 0 | 允许的构建目录里产生了动态库 |
| 2 | 没有 `src/xueximod_compute.c` |
| 3 | 没有带 JNI 头的 JDK 17 |
| 4 | 没有 Visual Studio、`vcvars64.bat` 或 C 编译器 |
| 5 | 没有 CMake；要求 Ninja 时也没有 Ninja |
| 6 | 编译器或指针宽度不是 64 位 |
| 7 | CMake 配置失败，或配置结果要求链接 JVM |
| 8 | 编译失败，或没有产生预期文件名 |
| 9 | 构建目录、参数或操作系统被拒绝 |

这些是构建脚本的退出码，不是 JNI 返回的计算状态。计算状态 0 到 10 的含义在 ABI 契约里。

## 限制

进程崩溃、访问违例和挂起的 Java 严重错误不会被这个库恢复。失败的批量调用在写出之前返回非 0，并用 `JNI_ABORT` 丢掉输出缓冲；只有整批写完才返回 0。库不保存 `JNIEnv`、数组或指针，也不创建线程。
