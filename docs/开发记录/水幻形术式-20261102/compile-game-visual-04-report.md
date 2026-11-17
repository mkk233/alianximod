# 水幻形缺臂实际修复 04：最终统一编译

结果：**compileJava 通过**。退出码 **0**，`BUILD SUCCESSFUL in 28s`。`:compileJava` 实际执行，不是 UP-TO-DATE。

这是 04b 报告里的绘制顺序修复，不是 03 诊断版。编译通过不是游戏实测通过。返修未追加正式复审。

03 的诊断编译和第一次失败记录都没有覆盖。本批此前没有这组同名编译报告，所以没有另建 attempt 目录。

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
- 编译前没有 `compileJava`、`processResources` 或 `GradleWrapperMain`。有一个由 IDE 拉起的空闲 `GradleDaemon`（pid 50884），命令里没有本仓库路径。没有结束它，本次用 `--no-daemon`，没有和它并发编译
- 没有读取或输出游戏命令行。没有热替换，没有向客户端发重载，没有启动游戏

## 时间

| 项 | 时间（+08:00） |
| --- | --- |
| `src` 最后写入 | 2026-11-03T00:48:19.2719413，`src/main/java/com/bslx/XueXiMod/client/SteveWaterLineVisual.java` |
| 编译前快照 | 2026-11-03T00:52:05.5619736 |
| 命名门禁 | 2026-11-03T00:52:12.8524502 至 2026-11-03T00:52:15.9499176 |
| compileJava | 2026-11-03T00:52:25.0659116 至 2026-11-03T00:52:54.6570789 |
| 编译后快照 | 2026-11-03T00:53:16.0698434 |

编译开始晚于最后一次 `src` 写入。04b 报告写明当时没有编译。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 **0**。

输出：源码命名可读性检查通过：未发现单字母变量或禁止缩写。

日志：`docs/开发记录/水幻形术式-20261102/compile-game-visual-04-name-gate.log`。没有为门禁改源码。

## 源码身份

- 前：`docs/开发记录/水幻形术式-20261102/compile-game-visual-04-before.json`，1263 个文件，相对路径 / 大小 / SHA256 / 最后修改时间
- 后：`docs/开发记录/水幻形术式-20261102/compile-game-visual-04-after.json`，1263 个文件
- 对比：新增 0，删除 0，大小或 SHA256 变化 0
- `WaterPhantomVisualDiagnostics.java` 在前后快照里都不存在
- 源码未变，这次成功覆盖该冻结字节。没有再编译

## 编译状态

日志原文（代码页 936）：`docs/开发记录/水幻形术式-20261102/compile-game-visual-04.log`（14641 字节）。同一字节的 UTF-8 副本：`docs/开发记录/水幻形术式-20261102/compile-game-visual-04-log-utf8.txt`。

- `:downloadMcpConfig` 执行
- `:extractSrg` UP-TO-DATE
- `:createMcpToSrg` UP-TO-DATE
- `:compileJava` 实际执行，该行没有 UP-TO-DATE
- `4 actionable tasks: 2 executed, 2 up-to-date`
- `BUILD SUCCESSFUL in 28s`
- 退出码 0
- javac 汇总：**41 个警告**。没有错误行

相关类输出时间落在这次编译窗口内：

| 类 | 字节 | 时间 |
| --- | ---: | --- |
| `SteveWaterLineVisual.class` | 7717 | 2026-11-03T00:52:48.522 |
| `SteveWaterLineMesh.class` | 15049 | 2026-11-03T00:52:48.512 |
| `CloneRenderer.class` | 6610 | 2026-11-03T00:52:47.997 |

`build` 下没有 `WaterPhantomVisualDiagnostics` 开头的输出文件。这次没有手删构建产物。

## 资源输出

04b 没有改确认贴图和网格。`build/resources/main` 与 `src` 哈希相同，输出时间不落后：

| 文件 | 字节 | SHA256 | src 时间 | 输出时间 |
| --- | ---: | --- | --- | --- |
| `textures/entity/xueximod/steve_water_lines.png` | 2687 | `9e7e5b01a7e09c393d3ffa1800468bd61ae919f68cf628152b00d9c347793da5` | 15:56:44 | 2026-11-02T23:39:49 |
| `textures/entity/xueximod/water_flakes.png` | 608 | `cefdee9e3e3fb7f701e19fc9cb3438c586c4841d3a50e3f8c9399a635bd4d70c` | 15:56:44 | 2026-11-02T23:39:49 |
| `models/effect/water_phantom_steve_v21/lines.bin` | 60666 | `26ef5093f3fc9cad03272f7bdfad06df771953e161b69310b2ff7e37b89f9025` | 15:56:44 | 2026-11-02T23:39:48 |

没有执行 `processResources`。

## 重启后看什么

这次改的是 Java。完整重启当前开发客户端。F3+T 或只重进世界不能替换这些类。

重启后释放一具水幻形，看双臂是否都在、双腿颜色是否都是水线而不是一侧发白，并再触发一次散水。作者报告里缺的是存储坐标 `+X` 臂，浅的是同侧 `+X` 腿；画面左右会随身体朝向换边。这仍待你在游戏里看一次。

## 未做

未 `processResources`、`build`、`jar`、`reobf`、发布、`runClient`，未启动或操控游戏。未改源码、资源、构建配置或制作表。未追加正式复审。设计 2/2、实现 3/3 沿用。制作表本项仍无匹配行，没有新增或迁移。
