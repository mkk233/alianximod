我先读这份任务说明，确认授权范围后再按它执行。任务是只读采集：现有构建、配置注册和本机 JDK/C 工具链事实。我先读指定文档和 Gradle 配置，再定位注册入口。构建文件里没有 JNI 依赖。接下来只读配置、命令注册和启动入口，并检查本机 JDK 与 C 工具链。源码里没有 JNI、JNA 或 FFM。接下来只查本机 JDK 17、JNI 头文件和 C 工具链的实际路径。当前 PATH 上的 Java 是 17.0.6，C 编译器不在 PATH 里。我继续解析 JDK 真实目录，并核对标准安装位置和本机 Gradle 缓存。步骤4/5只读事实报告。设计 2/2、实现 3/3 已用完，本报告只采集事实，不是新的正确性判断。未写文件，未跑 Gradle，未编译 DLL，未打包，未改制作表。不能据此承诺已经编出动态库，也不能承诺 C 后端的性能收益。

## 结论

本机可以编 Windows x64 DLL：Oracle JDK 17.0.6 自带 JNI 头，Visual Studio 2022 里有 x64 `cl`、`link`、CMake 3.25.1、Ninja 1.11.0、MSBuild，以及 Windows SDK 10.0.22000.0 的头和 x64 库。这些工具都不在 PATH 上，调用入口是下面的绝对路径；链接环境要先进入 `vcvars64.bat`。项目里没有 JNI、JNA、FFM，也没有 native 目录或加载代码。Java 17 仍是现成默认路径。native 缺失或架构不符时，现有 Java 运行链不会被 native 加载挡住，因为根本没有加载点。

## 必要文件与共享注册器

语言与坐标：Java 17（`build.gradle` 第 17 行），Forge `1.20.1-47.3.33`，模组 `xueximod`，包名 `com.bslx.XueXiMod`，版本 `0.0.1-1.20.1`。编译编码 UTF-8（`build.gradle` 第 260–262 行）。`org.gradle.jvmargs=-Xmx3G`，`org.gradle.daemon=false`。

| 文件 | 事实 |
|---|---|
| `D:\alianximod\build.gradle` | 唯一构建脚本。依赖只有 Forge、Mixin 注解处理器 `org.spongepowered:mixin:0.8.5:processor`、运行时 Jade `11.13.3+forge`。没有 JNA、JNI 包装库、FFM、CMake 或 native 插件 |
| `D:\alianximod\settings.gradle` | 插件仓库含 Forge 与 Parchment；`org.gradle.toolchains.foojay-resolver-convention` `0.7.0` |
| `D:\alianximod\gradle.properties` | 版本坐标与内存、daemon。没有 `org.gradle.java.home` |
| `D:\alianximod\gradle\wrapper\gradle-wrapper.properties` | `gradle-8.8-bin.zip`，`distributionBase=GRADLE_USER_HOME` |
| `D:\alianximod\gradle\wrapper\gradle-wrapper.jar`、`D:\alianximod\gradlew.bat` | Wrapper 入口。`JAVA_HOME` 有值时用它，否则用 PATH 上的 `java` |
| `D:\alianximod\src\main\resources\META-INF\mods.toml` | `modLoader=javafml`，Forge 与 Minecraft 依赖均为 `side=BOTH`。没有 native 库声明 |
| `src\main\java\com\bslx\XueXiMod\XueXiMod.java` | 唯一 `@Mod` 入口 |
| `src\main\java\com\bslx\XueXiMod\Config.java` | 唯一 `ForgeConfigSpec` |
| `src\main\java\com\bslx\XueXiMod\command\ModCommands.java` | 唯一 `RegisterCommandsEvent` 登记处 |

以下目录和文件都不存在：`src\main\java\com\bslx\XueXiMod\shu\compute`、`native`、`native\compute`、`src\main\native`、`CMakeLists.txt`、`src` 下的 `natives`、项目内 log4j 配置。`src` 中没有 `System.load`、`loadLibrary`、`java.lang.foreign`、JNA。记录里预留的 `shu/compute` 与 `native/compute` 尚未创建。

共享注册器的事实：

- 内容登记集中在 `XueXiMod.registerContent`（第 77–91 行）：物品、方块、方块实体、菜单、创造标签、释放实体、效果。纯计算后端若不是 `DeferredRegister` 对象，不必进这个方法。
- 构造函数（第 63–75 行）顺序是：取模组总线、`registerContent`、挂 `commonSetup`、把本实例注册到 `MinecraftForge.EVENT_BUS`、挂创造栏、`context.registerConfig(ModConfig.Type.COMMON, Config.SPEC)`。
- `commonSetup`（第 93–100 行）调用 `ModNetwork.register()`、`ActionBootstrap.load()`、`ModTechniques.registerAll()`，再投递 `RegisterXueXiContentEvent`。该事件是术式/附魔登记，不是 native 加载钩子。本任务没有继续读 network、链、领域和发布运行源码。
- `onServerStarting`（第 111–113 行）是空的 `ServerStartingEvent`。
- 配置只有一份 COMMON `ForgeConfigSpec`（`Config.java` 第 32–53 行）。键是 `logDirtBlock`、`magicNumber`、`magicNumberIntroduction`、`items`，以及 `wetness.flightExemptEntityIds`。`ModConfigEvent` 时写入静态字段（第 79–94 行）。`SPEC` 是包内可见，只在主类构造函数登记。要加开关就得改 `Config.java`。没有 CLIENT/SERVER 第二份规范。
- 命令只有 `ModCommands`（第 19–27 行）监听 `RegisterCommandsEvent`，再调用 `ShuCommands`、`ActionCommands`、`PerformanceCommands`。客户端 `DebugLogCommands` 另行监听 `RegisterClientCommandsEvent`，登记 `/shulog`，不经过 `ModCommands`。
- 独立发现方式已经在用：类上加 `@Mod.EventBusSubscriber(modid = xueximod)`。`Config` 用 MOD 总线。`PerformanceEvents` 显式用 FORGE 总线且不限 `Dist`，集成服和专用服都会加载，并监听 `ServerStartedEvent` / `ServerStoppingEvent`。
- `ModEvents.ServerEvents` 也监听 `ServerStartedEvent` / `ServerStoppingEvent`，方法体进入 network 与 domain 包。本任务没有往下读这些包。把 native 加载写进 `ModEvents.java` 或 `XueXiMod.java` 会占用共享入口；写进 `commonSetup` 同样会占用 `XueXiMod.java`。
- 因此：不加命令、不加配置项、不登记游戏内容时，不必占用 `ModCommands`、`Config` 和 `registerContent`。与现有非注册表监听器相同的独立类注解即可被 Forge 扫到。这是现有机制，不是新的方案裁决。

日志与专用服入口：主类日志是 `LogUtils.getLogger()`（SLF4J）。没有项目自己的 log4j 配置。Gradle 的 client、server、gameTestServer、data 都把 `forge.logging.markers` 设为 `REGISTRIES`，控制台级别 `debug`。server 运行配置工作目录是 `run`，参数 `--nogui`（`build.gradle` 第 116–119 行）。本次没有启动游戏。

## 本机工具链

当前进程：Windows 11，版本号 10.0.26100，`PROCESSOR_ARCHITECTURE=AMD64`，64 位操作系统和 64 位进程。`java -XshowSettings` 给出 `os.arch=amd64`、`sun.arch.data.model=64`。

JDK：

- `JAVA_HOME` 未设置。用户目录没有 `C:\Users\baishang\.gradle\gradle.properties`。
- PATH 上的 `java` / `javac` 是 `C:\Program Files\Common Files\Oracle\Java\javapath\java.exe` 与同目录 `javac.exe`。
- 实际 `java.home` 是 `C:\Program Files\Java\jdk-17`。`java` 与 `javac` 都是 17.0.6（2023-01-17 LTS），HotSpot 64-Bit Server VM。`release` 文件写明 `OS_ARCH=x86_64`、`OS_NAME=Windows`、实现方 Oracle。
- 直接入口：`C:\Program Files\Java\jdk-17\bin\java.exe`、`...\bin\javac.exe`。
- 同盘还有 `C:\Program Files\Java\jre1.8.0_291`。当前 PATH 选中的是 JDK 17，不是这份 JRE 8。
- JNI 头可用：`C:\Program Files\Java\jdk-17\include\jni.h`（74681 字节），`C:\Program Files\Java\jdk-17\include\win32\jni_md.h`。后者把 `JNIEXPORT` 定义为 `__declspec(dllexport)`，`JNICALL` 定义为 `__stdcall`。`jni.h` 的版本宏到 `JNI_VERSION_10`（第 1975–1981 行），没有名为 `JNI_VERSION_17` 的宏；头文件本身属于这份 JDK 17。

C 工具链：PATH 上没有 `cl`、`clang`、`clang++`、`gcc`、`g++`、`cmake`、`ninja`、`msbuild`。已查的 LLVM、MinGW、msys2、Strawberry、TDM 标准目录没有 gcc/clang。VS 自带 LLVM 的 `VC\Tools\Llvm\x64\bin\clang.exe` 也不存在。可用的 C 编译器是 MSVC，不是 gcc/clang。

Visual Studio Community 2022 17.5.3 安装在 `D:\Program Files\Microsoft Visual Studio\2022\Community`，组件 `VC.Tools.x86.x64` 和 `VC.CMake.Project` 都在。定位器是 `C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe`。

| 用途 | 绝对入口 |
|---|---|
| 注入 x64 包含路径和库路径 | `D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat` |
| 与当前 64 位 JVM 匹配的编译器 | `D:\Program Files\Microsoft Visual Studio\2022\Community\VC\Tools\MSVC\14.35.32215\bin\Hostx64\x64\cl.exe` |
| 同目录链接器 | 同级 `link.exe` |
| CMake | `D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe` |
| Ninja | `D:\Program Files\Microsoft Visual Studio\2022\Community\Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja\ninja.exe` |
| MSBuild | `D:\Program Files\Microsoft Visual Studio\2022\Community\MSBuild\Current\Bin\amd64\MSBuild.exe` |

版本：cl 自报「用于 x64 的 Microsoft (R) C/C++ 优化编译器 19.35.32216.1」；CMake `3.25.1-msvc1`；Ninja `1.11.0`；MSBuild `17.5.1+f6fdcf537`。同一次直接执行 `cl.exe` 没有先跑 `vcvars64.bat`，没有生成 DLL。同一套 MSVC 里还有 Hostx86 和 x86 目标，它们与当前 64 位 JVM 的架构不一致。

链接所需文件已在盘上：`C:\Program Files (x86)\Windows Kits\10\Include\10.0.22000.0\um\windows.h`，`...\Lib\10.0.22000.0\um\x64\kernel32.lib`，`...\Lib\10.0.22000.0\ucrt\x64\ucrt.lib`，以及 `...\MSVC\14.35.32215\lib\x64\libcmt.lib`。

Gradle 真实位置：

- 项目入口：`D:\alianximod\gradlew.bat`
- 已解压的 8.8：`C:\Users\baishang\.gradle\wrapper\dists\gradle-8.8-bin\dl7vupf4psengwqhwktix4v1\gradle-8.8\bin\gradle.bat`
- 同目录有 `gradle-8.8-bin.zip.ok`
- 用户缓存里已有 `caches\modules-2\files-2.1\net.minecraftforge.gradle`、`caches\8.8`、`caches\forge_gradle` 下的 Forge `1.20.1-47.3.33` 映射产物
- `C:\Users\baishang\.gradle\jdks` 只有 `CACHEDIR.TAG`，没有另一份已解压的 JDK
- 项目本地 `D:\alianximod\.gradle\8.8` 存在
- 本采集进程的 `GRADLE_USER_HOME` 指向一个不存在的 sandbox 目录。用户平时未设置该变量时，Wrapper 使用 `C:\Users\baishang\.gradle`。本次没有改全局环境

## 离线、产物路径与缺口

C 动态库：编译器、链接器、Windows SDK、MSVC x64 库、JDK 17 JNI 头、CMake、Ninja 都已在本机。按上述绝对路径使用时，编一个 Windows x64 DLL 不需要再下载编译器。本次没有实际编译，所以这只说明工具在盘上。`cl` 不经过 `vcvars64.bat` 时，INCLUDE/LIB 不会自动配好。

Java：`gradlew.bat` 在当前 PATH 下会落到 JDK 17.0.6。Gradle 8.8 与 ForgeGradle 1.20.1-47.3.33 缓存已在用户目录。`settings.gradle` 仍启用 Foojay 工具链自动解析，而 `.gradle\jdks` 里没有已准备的 JDK。本次没有执行 `compileJava --offline`，因此不能把缓存存在写成离线编译已经通过。若在本采集进程原样继承那个空的 `GRADLE_USER_HOME`，Gradle 会看不到上述用户缓存。

现有构建没有按操作系统拆源集，没有 native 任务，也没有把 DLL 打进 jar 的规则。jar 名由 `archivesName=xueximod` 和 `mod_version` 决定。资源来自 `src\main\resources` 与 `src\generated\resources`。仓库里出现的 `.dll` 只在若干 `docs\开发记录\**\node_modules\@img\sharp-win32-x64` 下，是文档脚本依赖，不是模组 native。

因此项目没有 C 动态库产物路径惯例。MSVC 直接 `/LD` 时，DLL 默认落在当时的工作目录；VS 自带的 CMake 多配置生成器通常把 DLL 放进构建树的配置子目录，Ninja 生成器通常放在构建目录。游戏开发时能从 classpath 看见的是 `build\resources\main` 和最终 jar。现在没有任何代码选择其中一条路径。

真实缺口：

- 没有 JNI、JNA、FFM 依赖，也没有 native 加载模式。Java 路径是唯一现成实现。
- 没有 C 源码、JNI 头的项目内副本、CMake/Ninja 脚本、Gradle native 任务。
- `cl`、CMake、Ninja、MSBuild 都不在 PATH 上；没有 gcc/clang。
- `JAVA_HOME` 未设置。Wrapper 依赖 PATH，当前 PATH 指向 JDK 17。
- 没有与 64 位 JVM 配套的现成 DLL，所以也不存在一份架构不符的库。
- Foojay 仍可能在工具链未命中时访问网络；本次未验证 `--offline`。
- 服务端启动方法 `onServerStarting` 为空；模组初始化发生在 `commonSetup`，服务端已启动回调在 `ModEvents` 与 `PerformanceEvents`。native 加载尚未挂到任一处。
- 未编译、未做加载实验、未测性能。DLL 即使以后同进程加载，仍在调用它的那条 Java 线程上，本报告不把它写成脱离服务端线程或已有 TPS 收益。
