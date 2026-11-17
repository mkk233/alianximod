# 水幻形 v2.1 最终集成编译

结果：**compileJava 通过**。退出码 **0**，`BUILD SUCCESSFUL in 27s`。`:compileJava` 实际执行，不是 UP-TO-DATE。

这次覆盖的是接入后的 `src`（1262 个文件）。早先非视觉编译是 1256 个文件、12:51–12:52 那一次，不能当作本次最终覆盖。成功只表示这批冻结源码能编译。未打包，未启动游戏，游戏效果待用户实测。

本批此前没有 `compile-integrated-report.md` 或其他集成编译产物，所以没有另建 attempt 目录。

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
- 编译前没有 `java.exe` / `gradle.exe`。离线配置进入了 `:compileJava`，没有依赖下载阻塞

## 时间

| 项 | 时间（+08:00） |
| --- | --- |
| `src` 最后写入 | 2026-11-02T16:00:44.4292576，`src/main/java/com/bslx/XueXiMod/client/CloneAfterimagePlayback.java` |
| 编译前快照 | 2026-11-02T16:16:11.5574725 |
| 命名门禁 | 2026-11-02T16:16:17.8793115 至 2026-11-02T16:16:20.5249165 |
| compileJava | 2026-11-02T16:16:28.5322731 至 2026-11-02T16:16:56.9238805 |
| 编译后快照 | 2026-11-02T16:17:06.2115605 |

编译开始晚于最后一次 `src` 写入。C、D 接入报告和集成第 3 轮复核都写明接入后还没跑过这次 `compileJava`。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 **0**。

输出：源码命名可读性检查通过：未发现单字母变量或禁止缩写。

日志：`docs/开发记录/水幻形术式-20261102/compile-integrated-name-gate.log`。没有为门禁改源码。

## 源码身份

- 前：`docs/开发记录/水幻形术式-20261102/compile-integrated-before.json`，1262 个文件，相对路径 / 大小 / SHA256
- 后：`docs/开发记录/水幻形术式-20261102/compile-integrated-after.json`，1262 个文件
- 对比：新增 0，删除 0，大小或 SHA256 变化 0
- 最后写入路径在前后快照中相同

源码未变，这次成功覆盖该冻结字节。没有自行再编译。

## 编译状态

日志原文（代码页 936）：`docs/开发记录/水幻形术式-20261102/compile-integrated.log`（14641 字节）。同一字节的 UTF-8 副本：`docs/开发记录/水幻形术式-20261102/compile-integrated-log-utf8.txt`。

- `:downloadMcpConfig` 执行
- `:extractSrg` UP-TO-DATE
- `:createMcpToSrg` UP-TO-DATE
- `:compileJava` 实际执行，日志里有 javac 注记和警告，该行没有 UP-TO-DATE
- `4 actionable tasks: 2 executed, 2 up-to-date`
- `BUILD SUCCESSFUL in 27s`
- 退出码 0
- javac 汇总：**41 个警告**。日志中没有错误行，没有失败文件行号

41 条都是既有过时 API 的 removal 警告。

## 未做

未 `build`、`jar`、`reobf`、发布、`runClient`，未启动游戏，未提交。未改源码、资源、构建配置或制作表。未修编译错误，也没有把这次编译当成新的正确性复审。
