# 水幻形用户实测返修 03：诊断编译第二次

结果：**诊断版 compileJava 通过**。退出码 **0**，`BUILD SUCCESSFUL in 30s`。`:compileJava` 实际执行，不是 UP-TO-DATE。

这只说明 03c 改过导入之后的诊断源码能编译。缺臂尚未修复。不能把诊断编译通过写成缺臂已修，也不能称为游戏效果已经一致。

第一次失败证据仍在 `compile-game-visual-03-*`，这次没有覆盖。本批此前没有 `compile-game-visual-03-r2` 编译报告，所以没有另建 attempt 目录。

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
- 编译前没有 `compileJava`、`processResources` 或 `GradleWrapperMain`。有一个由 IDE 拉起的空闲 `GradleDaemon`（pid 50884），命令里没有本仓库路径。没有结束它，本次用 `--no-daemon`
- 没有读取或输出游戏命令行、accessToken。没有热替换，没有向客户端发重载，没有启动游戏

## 时间

| 项 | 时间（+08:00） |
| --- | --- |
| `src` 最后写入 | 2026-11-03T00:18:12.4726404，`src/main/java/com/bslx/XueXiMod/client/WaterPhantomVisualDiagnostics.java` |
| 编译前快照 | 2026-11-03T00:20:19.9478265 |
| 命名门禁 | 2026-11-03T00:20:27.9831672 至 2026-11-03T00:20:31.1971544 |
| compileJava | 2026-11-03T00:20:39.4194456 至 2026-11-03T00:21:10.8274907 |
| 编译后快照 | 2026-11-03T00:21:35.9830337 |

编译开始晚于最后一次 `src` 写入。03c 报告写明当时没有编译。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 **0**。

输出：源码命名可读性检查通过：未发现单字母变量或禁止缩写。

日志：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-r2-name-gate.log`。没有为门禁改源码。

## 源码身份

- 前：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-r2-before.json`，1264 个文件，相对路径 / 大小 / SHA256 / 最后修改时间
- 后：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-r2-after.json`，1264 个文件
- 对比：新增 0，删除 0，大小或 SHA256 变化 0
- 源码未变，这次成功覆盖该冻结字节。没有再编译

## 编译状态

日志原文（代码页 936）：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-r2.log`（14641 字节）。同一字节的 UTF-8 副本：`docs/开发记录/水幻形术式-20261102/compile-game-visual-03-r2-log-utf8.txt`。

- `:downloadMcpConfig` 执行
- `:extractSrg` UP-TO-DATE
- `:createMcpToSrg` UP-TO-DATE
- `:compileJava` 实际执行，该行没有 UP-TO-DATE
- `4 actionable tasks: 2 executed, 2 up-to-date`
- `BUILD SUCCESSFUL in 30s`
- 退出码 0
- javac 汇总：**41 个警告**。没有错误行

`build/classes/java/main/com/bslx/XueXiMod/client/WaterPhantomVisualDiagnostics.class` 已生成，22418 字节，时间 2026-11-03T00:21:03。

## 资源输出

03b/03c 没有改确认贴图和网格。`build/resources/main` 与 `src` 哈希相同，输出时间不落后：

| 文件 | 字节 | SHA256 | src 时间 | 输出时间 |
| --- | ---: | --- | --- | --- |
| `textures/entity/xueximod/steve_water_lines.png` | 2687 | `9e7e5b01a7e09c393d3ffa1800468bd61ae919f68cf628152b00d9c347793da5` | 15:56:44 | 2026-11-02T23:39:49 |
| `textures/entity/xueximod/water_flakes.png` | 608 | `cefdee9e3e3fb7f701e19fc9cb3438c586c4841d3a50e3f8c9399a635bd4d70c` | 15:56:44 | 2026-11-02T23:39:49 |
| `models/effect/water_phantom_steve_v21/lines.bin` | 60666 | `26ef5093f3fc9cad03272f7bdfad06df771953e161b69310b2ff7e37b89f9025` | 15:56:44 | 2026-11-02T23:39:48 |

没有执行 `processResources`，也没有手改构建产物。

## 怎么采集诊断

这次改的是 Java。完全重启当前开发客户端，不要用 F3+T，也不要只重进世界。进入世界后释放一具水幻形，让它在画面里停留大约 4 秒。到 `D:\alianximod\run\logs\latest.log` 搜索 `WATER_PHANTOM_VISUAL_DIAG_03B`。该日志文件已经存在；重启前里面不会有这次诊断类写出的标记。若进世界时已经有一具水幻形，第一笔绘制就会开始这 4 次采集。

只做资源重载换不上这次诊断类。贴图和 `lines.bin` 字节没变。

## 未做

未 `processResources`、`build`、`jar`、`reobf`、发布、`runClient`，未启动或操控游戏。未改源码、资源、构建配置或制作表。未追加正式复审。设计 2/2、实现 3/3 沿用。缺臂未修，游戏效果仍待用户复测。
