# 步骤4/5作者B：离线C构建准备最终报告

`released=true`。写入权已释放。设计 2/2、实现 3/3 已用完，本阶段没有另开代理、复审或独立测试。本报告是准备结果，不是新的正确性裁决，也不是编译通过。

报告时间：2026-10-30 15:56:11 +08:00。

## 结论

离线构建入口已经写在 `D:/alianximod/native/compute/`。Windows 产物名固定为 `xueximod_compute.dll`，库基名固定为 `xueximod_compute`。C 源和冻结 ABI 仍然没有。`D:/alianximod/native/compute/src/xueximod_compute.c` 没有创建，没有空实现。Windows、Linux、macOS 都还没有编出动态库，不能标成已验证。

## 脚本入口

Windows：

```text
powershell -NoProfile -File D:\alianximod\native\compute\build-windows.ps1
```

`D:\alianximod\native\compute\build-windows.cmd` 只转发到上述脚本，不改执行策略。

Linux / macOS：

```text
bash native/compute/build-unix.sh
```

参数、发现顺序、C11、浮点选项和退出码写在 `D:/alianximod/native/compute/README.md`。JDK、Visual Studio、CMake、Ninja 都可以用参数覆盖。脚本先用 vswhere 和参数找工具，不把本机个人目录写成唯一入口。

允许的构建目录只有 `native/compute/build`（默认）和 `native/compute/out`。`-Clean` 也只删除这两个目录之一。产物留在构建目录，不复制到资源目录、Gradle `build/`、`run/` 或游戏目录。脚本不改用户或系统的 `PATH` / `JAVA_HOME`，不下载依赖，不链接 `jvm.lib` / `libjvm`。

## 本次实际写过的文件

- `D:/alianximod/native/compute/CMakeLists.txt`
- `D:/alianximod/native/compute/build-windows.ps1`
- `D:/alianximod/native/compute/build-windows.cmd`
- `D:/alianximod/native/compute/build-unix.sh`
- `D:/alianximod/native/compute/.gitignore`
- `D:/alianximod/native/compute/README.md`
- `D:/alianximod/docs/开发记录/统一纯计算后端-20261030/native-build-prep-final.md`
- `D:/alianximod/docs/开发记录/统一纯计算后端-20261030/evidence-B-prep.json`

仓库根 `.gitignore` 里已有不带斜杠的 `build` 和 `out`，构建目录因此也会被忽略。本次没有改那个文件。目录内 `.gitignore` 继续排除生成物并保留源文件与脚本。

## 仍然缺少

- `D:/alianximod/native/compute/src/xueximod_compute.c`
- 作者 A 的冻结 Java/JNI 签名和数据布局
- 任何一次 CMake 配置
- 任何一次 C 编译和 DLL/so/dylib
- Linux、macOS 工具链验证
- Windows 成功路径（退出码 0）验证。当前没有 C 源，脚本按设计在配置前停止

主会话拿到 A 的稳定 ABI 之后，需要再单独授权，才能由本作者续写 C 内核；编译也要另一次授权。

## 本机工具位置

这些是 2026-10-30 15:48 用 `Test-Path` 和 `vswhere -format json` 复核、并用文件版本资源读取的位置。除 vswhere 和缺源脚本路径外，没有启动 cl、link、cmake、ninja。版本资源读不到的 Ninja，沿用同日 `read-native-env-final.md` 里对该文件执行 `ninja --version` 的记录，不把它写成这次重新执行的结果。

| 用途 | 路径 | 版本事实 |
|---|---|---|
| vswhere | `C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe` | 本次执行过定位。安装版本 `17.5.33516.290`，产品 `17.5.3` |
| Visual Studio | `D:\Program Files\Microsoft Visual Studio\2022\Community` | Community 2022 |
| vcvars64 | `D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat` | 文件在。本次没有 call |
| x64 cl | `D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.35.32215\bin\Hostx64\x64\cl.exe` | 文件版本 `19.35.32216.1`。本次没有运行 |
| link | 同目录 `link.exe` | 文件版本 `14.35.32216.1`。本次没有运行 |
| CMake | `D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe` | 文件版本 `3.25.1-msvc1`。本次没有运行 |
| Ninja | `D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja\ninja.exe` | 文件在，版本资源为空。同日环境报告记录 `1.11.0` |
| JDK 17 | `C:\Program Files\Java\jdk-17` | `release` 为 `JAVA_VERSION="17.0.6"`，`OS_ARCH="x86_64"`。`java.exe` 产品版本 `17.0.6.0` |
| JNI 头 | `C:\Program Files\Java\jdk-17\include\jni.h` 与 `include\win32\jni_md.h` | 两个文件都在 |
| Windows SDK 头/库 | `C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\windows.h`，以及 `Lib\10.0.22000.0\um\x64\kernel32.lib` | 文件在 |

`JAVA_HOME` 当时为空。进程架构 `AMD64`。这些路径只说明本机现在找得到工具，脚本仍按 vswhere、参数和环境重新发现。

## 本会话检查

- PowerShell 解析 `build-windows.ps1`：0 个语法错误。
- 执行了一次 `build-windows.ps1`。日志是：`ERROR 2: C source is not present: D:\alianximod\native\compute\src\xueximod_compute.c`。退出码 2。没有创建 `build/`，没有创建 C 源，没有进入 CMake。
- `D:\Program Files\Git\bin\bash.exe -n build-unix.sh` 退出码 0。这只检查脚本语法。该 bash 不是 C 编译器，Linux/macOS 构建没有执行。
- 没有 CMake 配置，没有 cl/link/ninja 编译，没有 javac，没有 Gradle，没有加载验证，没有生成测试。
- 没有读作者 A 正在写的 `shu/compute`、Java 桥、四个数值适配类或 ABI。
- 没有改 Java、Gradle、共享配置、制作表和总说明。
- 没有 git reset、clean、checkout 或提交。
- 本会话没有审批拒绝。更早的 Ask 模式阻塞仍留在 `prepare-native-final.md`，这次没有改那份文件，也没有降模型、加 force 或改安全设置。

## 浮点与架构

C11。MSVC 使用 `/fp:precise` 和该配置原有优化级别，不用 `/fp:fast`、FMA 收缩、`/GL` 或 `/LTCG`。GCC/Clang 使用 `-ffp-contract=off` 和一组关闭 fast-math 的选项，Release 用 `-O2` 而不是 `-O3`/`-Ofast`。构建定义 `XUEXIMOD_COMPUTE_FP_PRECISE=1`。生成器限定 x64，并核对 8 字节指针，用来对齐本机 64 位 JVM。后续 C 源必须按 Java 17 参考的求值顺序写；这次没有函数签名。

## 交接

作者 B 对 `native/compute/` 的本次写入已经结束。`released=true`。在主会话交回 A 的稳定 ABI 并再次授权之前，不再改这些文件。
