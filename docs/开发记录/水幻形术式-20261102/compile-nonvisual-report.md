# 水幻形非视觉阶段统一编译

结果：**非视觉阶段 compileJava 通过**。退出码 **0**，`BUILD SUCCESSFUL in 25s`。`:compileJava` 实际执行，不是 UP-TO-DATE。

这只覆盖当前冻结的非视觉源码。用户尚未确认 Steve v2 水线外观，正式 `PLAYER` 注册、专项声明和具体外观仍未接线。不能称为完整术式交付，也没有游戏实测。

首轮失败证据留在 `compile-attempt-1/`。本次是修复停写后的重新编译，不用那次失败或旧底座结果代替。

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
| `src` 最后写入 | 2026-11-02T12:49:05.3564081，`src/main/java/com/bslx/XueXiMod/shu/technique/release/capture/ActorCaptureDispatcher.java` |
| 编译前快照 | 2026-11-02T12:51:19.2200418 |
| 命名门禁 | 2026-11-02T12:51:28.9290575 至 2026-11-02T12:51:31.5932941 |
| compileJava | 2026-11-02T12:51:39.8725608 至 2026-11-02T12:52:06.4423286 |
| 编译后快照 | 2026-11-02T12:52:17.0807700 |

编译开始晚于最后一次 `src` 写入。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 **0**。

输出：源码命名可读性检查通过：未发现单字母变量或禁止缩写。

日志：`docs/开发记录/水幻形术式-20261102/compile-nonvisual-name-gate.log`。没有因门禁改源码。

## 源码身份

- 前：`docs/开发记录/水幻形术式-20261102/compile-nonvisual-before.json`，1256 个文件，路径 / 大小 / SHA256
- 后：`docs/开发记录/水幻形术式-20261102/compile-nonvisual-after.json`，1256 个文件
- 对比：新增 0，删除 0，大小或 SHA256 变化 0
- 最后写入路径在前后快照中相同

派发说明相关作者已停写。批次记录写明 A/C 编译修复已交回并停写。本次编译者没有改这些文件。源码未变，这次成功覆盖该冻结字节。

## 编译状态

日志原文（代码页 936）：`docs/开发记录/水幻形术式-20261102/compile-nonvisual.log`（14641 字节）。同一字节的 UTF-8 副本：`docs/开发记录/水幻形术式-20261102/compile-nonvisual-log-utf8.txt`。

- `:downloadMcpConfig` 执行
- `:extractSrg` UP-TO-DATE
- `:createMcpToSrg` UP-TO-DATE
- `:compileJava` 实际执行，日志中出现 javac 注记与警告，没有 UP-TO-DATE
- `4 actionable tasks: 2 executed, 2 up-to-date`
- `BUILD SUCCESSFUL in 25s`
- 退出码 0
- javac 汇总：**41 个警告**。日志中没有错误行

41 条都是既有过时 API 的 removal 警告。没有失败文件行号。

## 未做

未 `build`、`jar`、`reobf`、发布、`runClient`，未启动游戏。未改源码、资源、构建配置或制作表。未做额外复审。正式 `PLAYER` 注册、专项声明和具体外观仍未接线。
