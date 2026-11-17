# 陨岩熔岩岩壳 v5 编译

status：`compile_successful`

`:compileJava` 的结果是 UP-TO-DATE。exit 0，BUILD SUCCESSFUL。没有重新运行 javac。已接入，这次编译成功，游戏待用户实测。本阶段额度已用完，返修结果未追加复审。

## 编译前证据

文件所有权在 `调度记录.md`：制作表 writer `5d70c782-b8d9-4205-b81e-a6328287e56e` 已停写；素材作者 `c944df97-5698-4cb3-ae72-8adf3edc9b0a` 已停写。`implementation-ownership.json` 的 `source_writes_closed` 和 `writes_stopped` 都是 true。本阶段唯一编译者是同一会话。

作者最后写入是 `implementation-ownership.json`，时间 `2026-11-02T02:19:51.7637356+08:00`。运行贴图最后写入 `2026-11-02T02:06:55.8786421+08:00`。编译开始 `2026-11-02T02:22:26.8186215+08:00`，晚于这些写入。

runClient PID 68932 已不存在。Gradle daemon PID 66116 仍在，父进程是 IntelliJ，命令是 `GradleDaemon 8.8`，没有子进程，日志 `daemon-66116.out.log` 为 0 字节。没有 `GradleWrapperMain`、`compileJava` 或 `runClient` 进程。daemon 空闲，本次用 `--no-daemon` 单独编译，没有杀进程。

编译前运行贴图 SHA256 为 `a0f7027b8b39c4f9f3aaabb9e05713c42e5b0e86697c89d9294848a9b228e1d8`。编译后仍是这个哈希，1324 字节。没有改 Java、模型或运行 PNG。没有跑命名门禁。

## 本次编译

命令：`.\gradlew.bat compileJava --offline --no-daemon`

工作目录：`D:\alianximod`

仅当前进程：`JAVA_HOME=C:/Program Files/Java/jdk-17`，`GRADLE_USER_HOME=C:/Users/baishang/.gradle`。没有改 `gradle.properties`。其中仍是 `org.gradle.jvmargs=-Xmx3G`、`org.gradle.daemon=false`。

startedAt：`2026-11-02T02:22:26.8186215+08:00`

endedAt：`2026-11-02T02:22:41.5564331+08:00`

exitCode：`0`

compileJavaOutcome：`UP-TO-DATE`

javac 没有执行。`:downloadMcpConfig` 执行了；`:extractSrg`、`:createMcpToSrg` 和 `:compileJava` 都是 UP-TO-DATE。4 个任务里 1 个执行、3 个 UP-TO-DATE。原始日志：`compile-gradle.log`。

```
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.8/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 

> Configure project :
Java: 17.0.6, JVM: 17.0.6+9-LTS-190 (Oracle Corporation), Arch: amd64

> Task :downloadMcpConfig
> Task :extractSrg UP-TO-DATE
> Task :createMcpToSrg UP-TO-DATE
> Task :compileJava UP-TO-DATE

BUILD SUCCESSFUL in 14s
4 actionable tasks: 1 executed, 3 up-to-date
```

productionWritesFrozen：true。编译期间没有改 Java、模型或运行 PNG。编译后只更新了正式目录 README、manifest、visual-handoff 和资源库索引里本项的编译状态。

packaged：false

gameStarted：false

blockers：[]

本报告和 `compile-result.json` 写完后停止写入。制作表由唯一 writer 回写。
