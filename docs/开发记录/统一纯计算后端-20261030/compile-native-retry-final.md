我先读这次重试的完整任务说明，再按其中的授权范围执行。这是第二次 native 构建。我先核对修复报告、新的冻结清单和所有权，确认可以沿用现有构建目录再重跑官方脚本。两份冻结清单都对得上，构建目录还在，没有别的编译在跑。开始完整重跑官方 Windows 脚本，不清理缓存。脚本退出码是 0，并且已经链出 DLL。接下来用 dumpbin 核对架构和 7 个 JNI 导出，并复查冻结元数据。本次 Windows x64 构建成功，退出码 0。新 DLL 已生成，dumpbin 确认为 x64，并导出 ABI 要求的 7 个 JNI 符号。编译前后两份冻结的路径、长度和 UTC ticks 都一致，记为本版本构建结果。native 编译权已释放。

设计 2/2、实现 3/3 已用完。未追加正式复审。未运行 DLL，未做 Java 加载、数值对照、游戏或性能测试。Linux / macOS 未验证。未运行 Gradle。Java `compileJava` 仍留给后续独立授权。

## 输出文件

- `docs/开发记录/统一纯计算后端-20261030/native-attempt-2.log`
- `docs/开发记录/统一纯计算后端-20261030/native-attempt-2-results.json`
- `docs/开发记录/统一纯计算后端-20261030/native-attempt-2-final.md`
- `docs/开发记录/统一纯计算后端-20261030/native-attempt-2-dumpbin.txt`

第一次失败的 `native-compile.log`、`native-compile-results.json`、`native-compile-final.md`、`native-compile-diagnosis.md` 以及 08:26 快照均未覆盖。构建目录没有删除，也没有使用 `-Clean`。

## 执行

沿用已有 `native/compute/build`。工作目录 `D:\alianximod`。开始 `2026-10-30T08:40:37.4330131Z`，结束 `2026-10-30T08:40:59.0574180Z`，退出码 0。审批没有拒绝。

```text
powershell -NoProfile -File D:\alianximod\native\compute\build-windows.ps1 -JdkHome 'C:\Program Files\Java\jdk-17'
```

编译前没有 cmake、ninja、cl、link 或 msbuild 进程。作者 B 的构建脚本修复已释放。`native-source-freeze.json` 12 个文件、`compile-source-freeze.json` 712 个文件，在编译前都与 `2026-10-30T08:38:32.6782275Z` 的冻结一致。编译后再次比对，两份仍全部一致。没有另做 SHA256。

## 实际执行的命令

配置已执行，日志有 `Configuring done` 和 `Generating done`。脚本随后记录指针宽度 `CMakeCCompiler.cmake=8`，然后执行构建。Ninja 输出是 `[1/2] Building C object` 和 `[2/2] Linking C shared library`。

配置命令：

```text
call "D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
"D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe" -S "D:\alianximod\native\compute" -B "D:\alianximod\native\compute\build" -G Ninja -DCMAKE_MAKE_PROGRAM="D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja\ninja.exe" -DCMAKE_BUILD_TYPE=Release -DJDK_HOME="C:\Program Files\Java\jdk-17"
```

构建命令留在 `native/compute/build/_xueximod_invoke.cmd`：

```text
call "D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
"D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe" --build "D:\alianximod\native\compute\build"
```

Ninja 编译使用 `Hostx64\x64\cl.exe`，定义 `XUEXIMOD_COMPUTE_FP_PRECISE=1`，标志含 `/std:c11 /fp:precise /W3 /O2`。链接使用同目录 `link.exe`，`/machine:x64 /INCREMENTAL:NO`，链接库里没有 `jvm.lib`。

| 项 | 值 |
|---|---|
| Visual Studio | 2022 Community 17.5.3 |
| cl / dumpbin | `...\MSVC\14.35.32215\bin\Hostx64\x64\`，dumpbin 14.35.32216.1 |
| CMake | 3.25.1-msvc1，生成器 Ninja，配置 Release |
| JDK | `C:\Program Files\Java\jdk-17` |
| 架构 | dumpbin：`8664 machine (x64)` |

## 产物

编译前该路径没有 DLL。本次命令写出：

`D:\alianximod\native\compute\build\xueximod_compute.dll`

长度 26112 字节。写入时间 `2026-10-30T08:40:59.0017107Z`，落在本次开始和结束之间。脚本没有把它复制到资源目录、`run/` 或游戏目录。

dumpbin `/headers` 与 `/exports` 只读取该文件，没有执行它。导出共 7 个函数、7 个名字，均无 stdcall 修饰：

- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_handshake`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trajectoryBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_turnBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_trackingStepBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorOverlapsBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorContactBatch`
- `Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_corridorBlockedBatch`

## 没有做的检查

没有 JNI 运行加载，没有数值对照，没有游戏，没有性能测试。非 Windows 平台未验证。未追加复审。未改源码、README、Gradle 或制作表。
