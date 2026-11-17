我先读取返修任务说明，确认授权范围后再改构建脚本。诊断写明配置已成功，失败出在脚本把未写入缓存的指针宽度当成了 32 位。我先核对日志和两份构建脚本里的检查。缓存里确实没有 `CMAKE_SIZEOF_VOID_P`，宽度写在 `CMakeCCompiler.cmake` 的 `CMAKE_C_SIZEOF_DATA_PTR "8"`。我先备份要改的脚本，再只改这一处判断。# 作者 B：指针宽度检查修复

`released=true`。写入权已释放。设计 2/2、实现 3/3 已用完，未追加正式复审。这次只改构建脚本对指针宽度的读取，没有重跑 CMake 或编译，也没有产生 DLL。

## 原因

2026-10-30 的 Windows 构建退出码 6。CMake 3.25.1-msvc1 已经 `Configuring done` / `Generating done`，编译器是 `Hostx64\x64\cl.exe`。`native/compute/build/CMakeFiles/3.25.1-msvc1/CMakeCCompiler.cmake` 有 `set(CMAKE_C_SIZEOF_DATA_PTR "8")`。`CMakeCache.txt` 没有 `CMAKE_SIZEOF_VOID_P`。`build-windows.ps1` 却要求缓存里有字面行 `CMAKE_SIZEOF_VOID_P:INTERNAL=8`，于是在 `cmake --build` 之前退出。这不是 32 位配置。`build-unix.sh` 用了同一条缓存假定。

## 改动

`build-windows.ps1` 和 `build-unix.sh` 在配置成功后同时查看：

- `CMakeCache.txt` 的 `CMAKE_SIZEOF_VOID_P:INTERNAL=<数字>`，有则采用
- `CMakeFiles/**/CMakeCCompiler.cmake` 的 `set(CMAKE_C_SIZEOF_DATA_PTR ...)`

任一处记成不是 8，退出码仍是 6，并写出实际数字。两处都没有数字时也退出 6，文案是缺字段，不把它当成 64 位。缓存缺项而编译器文件是 `"8"` 时继续，不再误报。`Hostx64\x64` 的 `cl.exe` 检查、`vcvars64.bat`、JDK/vswhere 发现、构建目录限制和退出码都保留。

`CMakeLists.txt` 没有改。它在配置期对 `CMAKE_SIZEOF_VOID_P` 等于 8 的拒绝仍然在。浮点参数、C 源、JNI 和协议没有改。没有 `-Clean`，没有删除 `native/compute/build`。

README 只同步了检查说明、退出码 6 和这次已知失败：配置已成功，DLL 仍未生成。

## 变更前

| 路径 | 字节 | UTC |
|---|---|---|
| `D:/alianximod/native/compute/build-windows.ps1` | 15452 | 2026-10-30T08:19:19.1203653Z |
| `D:/alianximod/native/compute/build-unix.sh` | 6825 | 2026-10-30T08:19:21.0927202Z |
| `D:/alianximod/native/compute/README.md` | 4597 | 2026-10-30T08:20:09.8842647Z |

副本在 `docs/开发记录/统一纯计算后端-20261030/backup-B-buildfix/`。

## 变更后

| 路径 | 字节 | UTC |
|---|---|---|
| `D:/alianximod/native/compute/build-windows.ps1` | 16585 | 2026-10-30T08:36:15.9459820Z |
| `D:/alianximod/native/compute/build-unix.sh` | 7988 | 2026-10-30T08:36:18.6624943Z |
| `D:/alianximod/native/compute/README.md` | 5176 | 2026-10-30T08:36:22.1808221Z |

## 本会话检查

对照保留的配置产物，PowerShell 正则从 `CMakeCCompiler.cmake` 取出 `8`，`CMakeCache.txt` 仍没有 `CMAKE_SIZEOF_VOID_P`。Unix 脚本使用的 `sed` 也对同一文件取出 `8`。`build-windows.ps1` 解析错误数为 0。`bash -n build-unix.sh` 退出码 0。两条脚本里都不再有只认 `CMAKE_SIZEOF_VOID_P:INTERNAL=8` 的旧判断。

没有执行 `build-windows.ps1`，没有 CMake 配置或 `cmake --build`，没有 cl、Gradle、加载、测试或游戏。`xueximod_compute.dll` 仍然不存在。原编译者需要重跑完整构建后，才能把退出码 0 和 DLL 记成构建结果。

本会话没有审批拒绝。
