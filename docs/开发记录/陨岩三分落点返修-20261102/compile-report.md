# 陨岩三分落点底座修复编译

status：`compile_successful`

`:compileJava` 实际执行，不是 UP-TO-DATE。exit 0，BUILD SUCCESSFUL。没有打包，没有启动游戏。本阶段额度已用完，返修结果未追加复审。游戏待用户实测。

## 冻结证据

`implementation-ownership.json` 声明 `source_writes_closed` 和 `writes_stopped` 都是 true。唯一生产改动是 `TechniqueHitSpec.java`。编译前和编译后 SHA256 都是 `827a767db942ce2e58dd9fade5dc77694e217e37df55cb63a6e594d4b4fb488c`，最后写入 `2026-11-02T02:52:38.1607953+08:00`，33040 字节。`src/main` 在这次作者写入之后没有更新的文件。

runClient PID 43376 已不存在。Gradle daemon PID 66116 仍在，没有子进程，命令是空闲 daemon，不是 `GradleWrapperMain`、`runClient` 或 `compileJava`。没有杀进程。

`src/main` 编译前清单 `compile-before.txt` 与编译后清单 `compile-after.txt` 都是 1019 行，内容一致。编译覆盖的是这份最终源码。

## 本次编译

命令：`.\gradlew.bat compileJava --offline --no-daemon`

工作目录：`D:\alianximod`

仅当前进程：`JAVA_HOME=C:/Program Files/Java/jdk-17`，`GRADLE_USER_HOME=C:/Users/baishang/.gradle`。没有改 Gradle 配置。`org.gradle.jvmargs=-Xmx3G`，`org.gradle.daemon=false`。

startedAt：`2026-11-02T02:56:32.9068567+08:00`

endedAt：`2026-11-02T02:57:01.8956800+08:00`

exitCode：`0`

compileJavaOutcome：`EXECUTED`

`:downloadMcpConfig` 执行了。`:extractSrg` 和 `:createMcpToSrg` 是 UP-TO-DATE。`:compileJava` 实际编译。4 个任务里 2 个执行、2 个 UP-TO-DATE。Java 17.0.6。40 条既有弃用警告，来自 `ResourceLocation` 构造和 `ProjectileImpactEvent.setCanceled`。日志里的中文注释在捕获里是乱码，原文没有改写。原始日志：`compile-gradle.log`。

```
> Task :downloadMcpConfig
> Task :extractSrg UP-TO-DATE
> Task :createMcpToSrg UP-TO-DATE

> Task :compileJava
BUILD SUCCESSFUL in 28s
4 actionable tasks: 2 executed, 2 up-to-date
```

packaged：false

gameStarted：false

blockers：[]

本报告、`compile-result.json` 和 `compile-ownership.json` 写完后停止写入。制作表不由这次编译改。
