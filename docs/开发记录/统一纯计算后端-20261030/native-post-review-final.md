# 返修后的 Windows native 脚本

结论：修复后的官方脚本退出码 0。Ninja 显示 `no work to do`。DLL 仍是 `2026-10-30T08:40:59.0017107Z` 那次实际编译并链接的文件，这次没有重新编译或链接。

## 命令

工作目录 `D:\alianximod`。没有 `-Clean`，沿用 `native/compute/build`。

```text
powershell -NoProfile -File "D:\alianximod\native\compute\build-windows.ps1" -JdkHome "C:\Program Files\Java\jdk-17"
```

开始 `2026-10-30T09:30:49.6977346Z`，结束 `2026-10-30T09:30:58.4170380Z`，退出码 0。配置已执行。CMake 参数是 `-S D:\alianximod\native\compute -B D:\alianximod\native\compute\build -G Ninja`，并带上 Ninja 与 JDK 17 的路径。随后 `--build D:\alianximod\native\compute\build`。指针宽度记录为 `CMakeCCompiler.cmake=8`。编译器路径是 `Hostx64\x64\cl.exe`。

更早有一次启动把 `C:\Program Files\Java\jdk-17` 拆开，脚本把 `Files\Java\jdk-17` 当成 `VsInstall`，退出码 4。那不是这条带引号的命令。记录在 `native-post-review-launch-split.txt`。那次没有改 DLL。

## 产物

`D:\alianximod\native\compute\build\xueximod_compute.dll` 仍是 26112 字节，写入时间仍是 `2026-10-30T08:40:59.0017107Z`。来源是 `native-attempt-2` 的实际编译和链接。C 源、`CMakeLists.txt` 和 `XuexiComputeNative.java` 的冻结长度与 UTC ticks 没有相对那次构建改变；改变的是 `build-windows.ps1`。因此 Ninja 无工作是允许的增量结果。

dumpbin 仍是 `8664 machine (x64)`，7 个 JNI 导出。没有执行 DLL。

仓库、JDK 和 Visual Studio 路径都是 ASCII。这次不能当成中文路径构建已经验证。

冻结前后 native 12 项和总输入 712 项都一致。没有重做 SHA256。
