我先读取这次返修任务的完整说明，确认授权范围后再改。复核指出临时批处理用 ASCII 写路径，中文会被换成问号。我先确认 cmd 能否用 UTF-16 把环境变量原样交出来，再改脚本，避免把路径写进有损批处理。事实检查已经对上：中文路径留在子进程环境里，不再写进批处理。我现在把改动和未验证项写进报告。# 作者 B：R3 Windows 路径不再经过有损批处理

`released=true`。写入权已释放。这是用户追加的唯一一轮实现修复，累计实现 4 轮。修完后没有再审，也没有新的正式复审。本单没有跑官方构建。

## 改动

`build-windows.ps1` 不再生成或调用 `_xueximod_invoke.cmd`，两处 `-Encoding Ascii` 已去掉。`vcvars64.bat` 只在子进程里 `call`。路径放在该子进程的环境变量中，不写进批处理字节。子进程成功返回后，脚本用 `cmd /d /u` 按 UTF-16 读回环境，再直接启动 `cmake.exe`。配置和编译各自一个进程，参数按 Unicode 传入，环境只用刚才读回的那一份。父进程的 `PATH` 和 `JAVA_HOME` 不改。

`cmd` 会把 `if errorlevel` 后面的整条 `&` 链当成条件体。条件不成立时，后面的输出会被跳过。脚本改为只有 `call` 成功才继续读环境。`vcvars64.bat` 失败时使用它自己的退出码，脚本仍退出 4。CMake 配置失败退出 7，编译失败退出 8。不再把 CMake 自己的退出码 20 记成 vcvars 失败。

vswhere、JDK 17、`vcvars64.bat`、`Hostx64\x64\cl.exe`、构建目录只允许 `build` 或 `out`、离线、以及配置后的指针宽度检查都还在。`cl.exe` 只按路径确认，不执行。指针宽度仍同时看 `CMakeCache.txt` 的 `CMAKE_SIZEOF_VOID_P` 和 `CMakeCCompiler.cmake` 的 `CMAKE_C_SIZEOF_DATA_PTR`。缺字段或不是 8 仍退出 6。

`build-windows.cmd` 没有改。它只有 `%~dp0`，运行时由 cmd 展开，文件里没有写死的仓库路径。

## 没有改的部分

C 源、`CMakeLists.txt`、ABI、JNI 导出和浮点参数都没有改。`/fp:precise`、`fp_contract`、以及禁止 fast-math 的原有设置仍在 CMake 里，本单没有动它们。没有改 Java、Gradle 或制作表，也没有改作者 A 的文件。

`native/compute/build` 没有清理。已有的 `xueximod_compute.dll` 仍是此前官方构建留下的 26112 字节，时间 `2026-10-30T08:40:59.0017107Z`。同目录里的旧 `_xueximod_invoke.cmd` 仍是 353 字节，时间 `2026-10-30T08:40:51.4620113Z`。新脚本不调用它，本单也没有删除它。

已有 `D:\alianximod` 编译成功记录仍然有效。这次修复不推翻那次结果。

## 变更前

| 路径 | 字节 | UTC |
|---|---|---|
| `D:/alianximod/native/compute/build-windows.ps1` | 16585 | 2026-10-30T08:36:15.9459820Z |
| `D:/alianximod/native/compute/README.md` | 5316 | 2026-10-30T08:56:37.1089887Z |

副本在 `docs/开发记录/统一纯计算后端-20261030/backup-B-review-fix/`。没有计算哈希。

## 变更后

| 路径 | 字节 | UTC |
|---|---|---|
| `D:/alianximod/native/compute/build-windows.ps1` | 21632 | 2026-10-30T09:25:31.7987271Z |
| `D:/alianximod/native/compute/README.md` | 5839 | 2026-10-30T09:19:30.9790228Z |

README 只加了路径传递说明：不再写临时批处理，子进程环境按 UTF-16 读回，父进程环境不变，以及这次没有用中文路径实际构建。

未改文件的当前时间：`build-windows.cmd` 172 字节，`2026-10-30T07:51:43.2419211Z`；`CMakeLists.txt` 5578 字节，`2026-10-30T08:19:17.3762850Z`；`src/xueximod_compute.c` 44883 字节，`2026-10-30T08:20:44.3571677Z`。

## 事实检查

这些检查只确认脚本文本和编码传递，不是中文路径下的构建，也不是新的复审。

- `build-windows.ps1` 解析错误数为 0。
- 脚本里已经没有 `Encoding Ascii`、`Set-Content` 和 `_xueximod_invoke.cmd`。
- 临时目录里的 ASCII 批处理只执行 `exit /b 0`。它的目录名含空格和 U+672F。样本值 `D:\术开发\check-✓`（U+672F 与 U+2713）放在子进程环境里，经 `cmd /d /u` 读回后与原值相同。
- 用读回的环境启动另一个进程，该进程写出的 UTF-16 文本与原值相同。检查用的父进程变量随后已删除。
- 不存在的 `vcvars64.bat` 路径使这次读取退出 1。脚本会把任何非 0 的 vcvars 退出码转成 4。
- 没有执行 `vcvars64.bat`、`cmake`、`cl.exe` 或 `build-windows.ps1`。

## 未验证

没有用含中文的仓库、JDK 或工具路径运行构建。没有 CMake 配置、C 编译、Java 编译、DLL 加载、数值对照、游戏或性能测试。Linux / macOS 构建也没有跑。最终仍要由唯一编译者用修复后的官方入口完整构建：

```text
powershell -NoProfile -File D:\alianximod\native\compute\build-windows.ps1
```

本会话没有审批拒绝。
