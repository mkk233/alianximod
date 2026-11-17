# 水幻形用户实测返修 01：最终编译

结果：**compileJava 通过**。退出码 **0**，`BUILD SUCCESSFUL in 31s`。`:compileJava` 实际执行，不是 UP-TO-DATE。

这只说明返修后的冻结源码能编译。返修未追加正式复审。不能把编译通过称为游戏效果已经和确认版一致。当前正在运行的开发客户端仍是旧类，需要用户自己正常重启并重新进入世界。

本批此前没有这组同名编译报告，所以没有另建 attempt 目录。16:16 的集成编译和更早的非视觉编译都不覆盖这次 Java 改动。

## 命令与环境

- 工作目录：`D:/alianximod`
- 命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 实际进程：`cmd.exe /d /c .\gradlew.bat compileJava --offline --no-daemon`
- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- 进程 PATH 前缀：`C:\Program Files\Java\jdk-17\bin`
- 启动前 `java -version`：17.0.6+9-LTS-190
- 构建日志 `Configure project`：Java 17.0.6，JVM 17.0.6+9-LTS-190（Oracle），amd64
- `gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false`，本次未改
- 日志说明按该 JVM 设置派生一次性 Daemon，构建结束停止
- 编译前没有 `compileJava`、`processResources` 或 `GradleWrapperMain`。有一个由 `idea64.exe` 拉起的空闲 `GradleDaemon`（pid 50884，20:25 启动），命令里没有本仓库路径，也没有编译任务。没有结束它，本次用 `--no-daemon`，没有和它并发编译
- 没有读取或输出游戏命令行、accessToken。没有热替换，没有向客户端发重载

## 时间

| 项 | 时间（+08:00） |
| --- | --- |
| `src` 最后写入 | 2026-11-02T21:44:48.3278412，`src/main/java/com/bslx/XueXiMod/client/SteveWaterLineMesh.java` |
| 编译前快照 | 2026-11-02T21:49:13.2168051 |
| 命名门禁 | 2026-11-02T21:49:19.7497463 至 2026-11-02T21:49:22.2485594 |
| compileJava | 2026-11-02T21:49:31.0233708 至 2026-11-02T21:50:03.3132870 |
| 编译后快照 | 2026-11-02T21:50:32.6300470 |

编译开始晚于最后一次 `src` 写入。作者报告写明返修当时没有编译。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 **0**。

输出：源码命名可读性检查通过：未发现单字母变量或禁止缩写。

日志：`docs/开发记录/水幻形术式-20261102/compile-game-visual-01-name-gate.log`。没有为门禁改源码。

## 源码身份

- 前：`docs/开发记录/水幻形术式-20261102/compile-game-visual-01-before.json`，1262 个文件，相对路径 / 大小 / SHA256 / 最后修改时间
- 后：`docs/开发记录/水幻形术式-20261102/compile-game-visual-01-after.json`，1262 个文件
- 对比：新增 0，删除 0，大小或 SHA256 变化 0
- 源码未变，这次成功覆盖该冻结字节。没有再编译

## 编译状态

日志原文（代码页 936）：`docs/开发记录/水幻形术式-20261102/compile-game-visual-01.log`（14641 字节）。同一字节的 UTF-8 副本：`docs/开发记录/水幻形术式-20261102/compile-game-visual-01-log-utf8.txt`。

- `:downloadMcpConfig` 执行
- `:extractSrg` UP-TO-DATE
- `:createMcpToSrg` UP-TO-DATE
- `:compileJava` 实际执行，该行没有 UP-TO-DATE
- `4 actionable tasks: 2 executed, 2 up-to-date`
- `BUILD SUCCESSFUL in 31s`
- 退出码 0
- javac 汇总：**41 个警告**。没有错误行

两个类的输出时间落在这次编译窗口内：

- `build/classes/java/main/com/bslx/XueXiMod/client/SteveWaterLineMesh.class`，14820 字节，2026-11-02T21:49:56
- `build/classes/java/main/com/bslx/XueXiMod/client/SteveWaterLineVisual.class`，7833 字节，2026-11-02T21:49:56

## 资源输出

`compileJava` 不处理资源。对照 `src/main/resources` 与 `build/resources/main`，返修报告写明未改的三份水幻形运行资源哈希一致，输出时间晚于源文件，没有落后：

| 文件 | 字节 | SHA256 | src 时间 | 输出时间 |
| --- | ---: | --- | --- | --- |
| `textures/entity/xueximod/steve_water_lines.png` | 2687 | `9e7e5b01a7e09c393d3ffa1800468bd61ae919f68cf628152b00d9c347793da5` | 15:56:44 | 20:25:50 |
| `textures/entity/xueximod/water_flakes.png` | 608 | `cefdee9e3e3fb7f701e19fc9cb3438c586c4841d3a50e3f8c9399a635bd4d70c` | 15:56:44 | 20:25:50 |
| `models/effect/water_phantom_steve_v21/lines.bin` | 60666 | `26ef5093f3fc9cad03272f7bdfad06df771953e161b69310b2ff7e37b89f9025` | 15:56:44 | 20:25:50 |

`zh_cn.json`、`en_us.json` 也与输出哈希相同。因此没有执行 `processResources`，也没有手改构建产物。资源对比之后的源码快照就是上面的 after 快照，差异仍是 0。

## 怎么进到当前客户端

这次改的是 Java。正常重启当前开发客户端，再重新进入世界，才会载入 21:49:56 的两个类。只做资源重载不会换上这次绘制。贴图和 `lines.bin` 字节没变，重载也没有新的资源要换。

## 未做

未 `build`、`jar`、`reobf`、发布、`runClient`，未启动或操控游戏。未改源码、资源、构建配置或制作表。未修编译错误，未追加正式复审。设计 2/2、实现 3/3 沿用。游戏效果仍待用户复测。
