# native 编译诊断（交回作者 B）

本次 Windows x64 入口退出码 **6**。没有产生 `xueximod_compute.dll`。源码、构建脚本、README、Gradle 和制作表都没有改。`native/compute/build` 保留，没有使用 `-Clean`，也没有删除。

## 实际原因

`build-windows.ps1` 在 CMake 配置成功后读取 `CMakeCache.txt`，用正则 `CMAKE_SIZEOF_VOID_P:INTERNAL=8` 判断指针宽度。匹配不到就退出 6，文案是 “CMake pointer size is not 8 bytes”。

这次缓存里没有 `CMAKE_SIZEOF_VOID_P` 这一项，不是该项等于 4。随 Visual Studio 2022 自带的 CMake 3.25.1-msvc1 把宽度写在：

`native/compute/build/CMakeFiles/3.25.1-msvc1/CMakeCCompiler.cmake`

```text
set(CMAKE_C_SIZEOF_DATA_PTR "8")
set(CMAKE_SIZEOF_VOID_P "${CMAKE_C_SIZEOF_DATA_PTR}")
```

`CMAKE_C_COMPILER_ARCHITECTURE_ID` 与 `MSVC_C_ARCHITECTURE_ID` 都是 `x64`。`CMakeLists.txt` 自己的 8 字节检查通过了：日志有 `Configuring done` 和 `Generating done`，没有走到脚本的配置失败出口 7。

脚本在覆盖 `_xueximod_invoke.cmd` 为构建命令之前就退出了，所以没有执行 `cmake --build`，也没有用 `cl.exe` 编译 `src/xueximod_compute.c`。`build.ninja` 里已有未执行的 `/machine:x64` 链接规则。

因此这不是 32 位编译器，也不是 JDK 或 C 源缺失。要改的是作者 B 的 `native/compute/build-windows.ps1` 对 CMake 3.25 缓存格式的检查。本单没有改这个文件，也没有重试。

## 已执行的命令

工作目录 `D:\alianximod`。开始 `2026-10-30T08:29:36.5513823Z`，结束 `2026-10-30T08:29:55.3119704Z`。

```text
powershell -NoProfile -File D:\alianximod\native\compute\build-windows.ps1 -JdkHome 'C:\Program Files\Java\jdk-17'
```

配置辅助脚本 `native/compute/build/_xueximod_invoke.cmd` 的内容是：

```text
call "D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
"D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe" -S "D:\alianximod\native\compute" -B "D:\alianximod\native\compute\build" -G Ninja -DCMAKE_MAKE_PROGRAM="D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja\ninja.exe" -DCMAKE_BUILD_TYPE=Release -DJDK_HOME="C:\Program Files\Java\jdk-17"
```

## 工具

- Visual Studio 2022 Community 17.5.3：`D:\Program Files\Microsoft Visual Studio\2022\Community`
- cl：`D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.35.32215\bin\Hostx64\x64\cl.exe`，识别为 MSVC 19.35.32216.1
- JDK：`C:\Program Files\Java\jdk-17`，`JAVA_VERSION="17.0.6"`
- 生成器 Ninja，配置 Release

完整输出在 `native-compile.log`。
