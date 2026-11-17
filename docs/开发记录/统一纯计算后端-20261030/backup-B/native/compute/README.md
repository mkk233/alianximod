# xueximod_compute 离线构建

这个目录只准备 Windows x64 动态库和 Linux/macOS 共享库的离线构建入口。C 内核还没有写。`src/xueximod_compute.c` 要等作者 A 交回冻结的 Java/JNI ABI 之后再添加。现在不要放空文件，也不要先编一个没有签名的桩。

在那之前运行下面的脚本会以退出码 2 停止。直接跑 CMake 也会在进入编译器探测之前 `FATAL_ERROR`。这两种结果都不是构建成功。Linux 和 macOS 脚本写好了，但写脚本的这台 Windows 机器没有对应工具链，不能把它们标成已验证。真正执行 CMake 配置和编译需要另一次构建授权。

产物基名固定为 `xueximod_compute`。

| 系统 | 产物 | 本机是否验证 |
|---|---|---|
| Windows x64 | `xueximod_compute.dll` | 未编译，未验证 |
| Linux x64 | `libxueximod_compute.so` | 未验证 |
| macOS | `libxueximod_compute.dylib` | 未验证 |

默认构建目录是 `native/compute/build`。另一个允许的目录是 `native/compute/out`。多配置生成器的 DLL 也直接放在该目录，不另加 `Release/` 子目录。导入库、`.pdb`、CMake 缓存和临时的 `_xueximod_invoke.cmd` 都留在同一个构建目录里，并由 `.gitignore` 排除。

脚本不会把 DLL 复制到 `src/main/resources`、Gradle 的 `build/`、`run/` 或游戏目录，不会改用户或系统的 `PATH` 和 `JAVA_HOME`，不会接入 Gradle，也不会下载或安装编译器、CMake、Ninja、JDK。

## Windows 入口

在项目根执行：

```text
powershell -NoProfile -File native\compute\build-windows.ps1
```

同一目录下的 `build-windows.cmd` 会转发到这个脚本，并且不修改执行策略。

常用覆盖参数：

| 参数 | 作用 |
|---|---|
| `-JdkHome` | JDK 17 根目录，必须有 `include\jni.h`、`include\win32\jni_md.h` 和 `release` |
| `-VsInstall` | Visual Studio 安装根 |
| `-VsWhere` | `vswhere.exe` 路径 |
| `-Vcvars64` | 只能是名为 `vcvars64.bat` 的文件 |
| `-Cmake` | `cmake.exe` |
| `-Ninja` | `ninja.exe` |
| `-Generator` | `Ninja` 或 `Visual Studio 17 2022` |
| `-Configuration` | `Release`（默认）、`RelWithDebInfo`、`Debug` |
| `-BuildDir` | `build`、`out`，或解析后正好等于这两个目录的路径 |
| `-Clean` | 只删除上面允许的那一个构建目录 |

发现顺序：

1. 命令参数。
2. Visual Studio 用 `vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64`。`vswhere.exe` 先找参数，再找 `Program Files (x86)\Microsoft Visual Studio\Installer`，再找 `Program Files` 下的同名位置，最后才看当前进程 `PATH`。
3. `vcvars64.bat` 用 `-Vcvars64`，否则用安装根下的 `VC\Auxiliary\Build\vcvars64.bat`。`vcvars32.bat` 和其他宿主/目标组合会被拒绝。
4. JDK 用 `-JdkHome`，否则读当前进程、用户、系统里已经存在的 `JDK_HOME` 或 `JAVA_HOME`（只读，不写回）。还没有时，在 `Program Files` 下的 `Java`、`Eclipse Adoptium`、`Microsoft`、`Amazon Corretto` 里找 `release` 标明 17 且带 JNI 头的目录。
5. CMake 用参数，否则用该 Visual Studio 自带的 `Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe`，再否则用 `PATH` 上的 `cmake.exe`。
6. 找到 Ninja 时默认生成器是 Ninja。没找到且 vswhere 报的版本以 `17.` 开头时，用 `Visual Studio 17 2022 -A x64 -T host=x64`。

配置和编译都在同一次 `cmd /d /c` 里先 `call vcvars64.bat`，环境只留在这个子进程。脚本还会检查 `where cl` 的第一条 `cl.exe` 位于 `Hostx64\x64`，并在配置后核对 `CMAKE_SIZEOF_VOID_P=8`。JNI 只把 `include` 和 `include\win32` 加进头文件路径，不链接 `jvm.lib`。

本机可以供发现逻辑找到的位置写在本阶段事实报告里。那些路径不是脚本里的唯一入口。

## Linux / macOS 入口

```text
bash native/compute/build-unix.sh
```

参数是 `--jdk-home`、`--cc`、`--cmake`、`--build-dir build|out`、`--configuration`、`--clean`。JDK 先看参数，再只读 `JDK_HOME` / `JAVA_HOME`，最后才看发行版上常见的 JDK 17 目录。CMake 用参数、`CMAKE` 或 `PATH`。C 编译器默认交给 CMake 选择本机的 `cc`、`gcc` 或 `clang`。脚本拒绝在 Windows 上运行，并要求 64 位进程。共享库前缀保持各平台惯例：`libxueximod_compute.so` 或 `libxueximod_compute.dylib`。

写这份文件的机器没有 gcc/clang，也没有执行过这个脚本，所以这里没有 Linux 或 macOS 的成功记录。

## C 标准、浮点和产物路径

语言是 C11。`CMAKE_C_STANDARD` 为 11，并关闭 GNU 扩展。Windows 上编译选项另加 `/std:c11`。

Java 17 的浮点表达式按源码顺序做 IEEE 754 严格求值，普通的乘加不会收成 `Math.fma`。C 侧要能按同一顺序对照：

- MSVC：`/fp:precise`，再加该配置原有的优化级别（Release 为 CMake 默认的 `/O2`）。不使用 `/fp:fast`、`/Qfast_transcendentals`、`/GL`、`/LTCG`。`/fp:precise` 下不生成 FMA 收缩，后续 C 源也不要打开 `FP_CONTRACT`。
- GCC / Clang：`-ffp-contract=off -fno-fast-math -fno-unsafe-math-optimizations -fno-associative-math -fno-reciprocal-math`。GCC 另加 `-fexcess-precision=standard`。Release 把 CMake 默认的 `-O3` 换成 `-O2`，并去掉 `-Ofast` 和 `-flto`。
- 目标是 64 位，和本机 64 位 JVM 一致。x64 使用 SSE2 标量路径，避免 x87 的 80 位超额精度。
- 构建定义 `XUEXIMOD_COMPUTE_FP_PRECISE=1`。后续 C 源可以依赖它，但不要自行改成另一种浮点模式；只有 Java 参考调用了 `Math.fma` 时才显式调用 `fma`。

Windows 产物路径：

```text
native/compute/build/xueximod_compute.dll
```

使用 `-BuildDir out` 时，把其中的 `build` 换成 `out`。Linux 对应 `libxueximod_compute.so`，macOS 对应 `libxueximod_compute.dylib`。

## 退出码

| 码 | 含义 |
|---|---|
| 0 | 允许的构建目录里产生了动态库 |
| 2 | 还没有 C 源，ABI 未交接 |
| 3 | 没有带 JNI 头的 JDK 17 |
| 4 | 没有可用的 Visual Studio、`vcvars64.bat` 或 C 编译器 |
| 5 | 没有 CMake；要求 Ninja 时也没有 Ninja |
| 6 | 编译器或指针宽度不是 64 位 x64 |
| 7 | CMake 配置失败，或配置结果要求链接 JVM |
| 8 | 编译失败，或没有产生预期文件名 |
| 9 | 构建目录、参数或操作系统被拒绝 |

日志前缀是 `[xueximod_compute]`。失败时同一行带退出码。

## 下一步

作者 A 把稳定的 JNI 签名和数据布局交回之后，再按那份 ABI 写 `native/compute/src/xueximod_compute.c`。那次写入和随后的本地编译都要单独授权。这次准备没有运行 CMake，没有编译，没有 javac，没有 Gradle，也没有做加载验证。
