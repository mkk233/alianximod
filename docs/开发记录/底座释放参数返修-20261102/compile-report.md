# 底座释放参数返修编译

status：`compile_successful`

`:compileJava` 实际执行，不是 UP-TO-DATE。exit 0，BUILD SUCCESSFUL。没有打包，没有启动游戏。这次编译不是追加正确性复审。本阶段额度已用完，返修结果未追加复审。游戏待用户实测。

## 冻结证据

`implementation-result.json` 与 `implementation-ownership.json` 都是 `writes_stopped=true`、`blockers=[]`。最终源码是这 4 个文件，编译前后哈希和写入时间都没变：

| 文件 | SHA256 | mtime |
| --- | --- | --- |
| `BatchCastSession.java` | `e958c35c223e37fab2256f65df3c87585ebc041ac26bb15dd0d5f9529e83d44f` | 2026-11-02T03:50:53.9570425+08:00 |
| `SkillSegmentRunner.java` | `665f4bcbdaef562731d6d804c865bcc9935b7360cef2f9d0307864c9c85a335d` | 2026-11-02T03:51:20.4871488+08:00 |
| `BatchShockRingEffect.java` | `48af3e6b3d44222af44b4def51dfd3ff2e5e9f97f0866354387cdfdea87d71a2` | 2026-11-02T03:51:00.3990663+08:00 |
| `TechniqueHitSpec.java` | `fd70ab2637ca0b33091f0771740eeb49c85985571045a14ea7f17e57d534fd8e` | 2026-11-02T03:51:01.9955679+08:00 |

`src/main` 没有比 `SkillSegmentRunner.java` 这次最终写入更新的文件。编译开始 `2026-11-02T03:55:41.3293566+08:00`，晚于该写入。

编译前只有空闲 Gradle daemon PID 66116，父进程 57868，无子进程，命令是 `GradleDaemon 8.8`，不是 `GradleWrapperMain`、`runClient` 或 `compileJava`。没有关闭它。编译结束后它仍在，仍无子进程，也没有活动包装器或游戏进程。

`src/main` 编译前清单 `compile-source-before.txt` 与编译后清单 `compile-source-after.txt` 都是 1019 行，逐行一致。

## 本次编译

命令：`.\gradlew.bat compileJava --offline --no-daemon`

工作目录：`D:\alianximod`

仅当前进程：`JAVA_HOME=C:/Program Files/Java/jdk-17`，`GRADLE_USER_HOME=C:/Users/baishang/.gradle`。没有改 Gradle 配置。`org.gradle.jvmargs=-Xmx3G`，`org.gradle.daemon=false`。

startedAt：`2026-11-02T03:55:41.3293566+08:00`

endedAt：`2026-11-02T03:56:10.1974142+08:00`

exitCode：`0`

compileJavaOutcome：`EXECUTED`

`:downloadMcpConfig` 执行了。`:extractSrg` 和 `:createMcpToSrg` 是 UP-TO-DATE。`:compileJava` 实际编译。4 个任务里 2 个执行、2 个 UP-TO-DATE。Java 17.0.6。日志末行是 40 个警告，都是既有弃用：`ResourceLocation` 构造，以及 `HitVolumes.java:208` 的 `ProjectileImpactEvent.setCanceled`。原始日志：`compile-gradle.log`。

```
> Task :downloadMcpConfig
> Task :extractSrg UP-TO-DATE
> Task :createMcpToSrg UP-TO-DATE

> Task :compileJava
BUILD SUCCESSFUL in 28s
4 actionable tasks: 2 executed, 2 up-to-date
```

`writes_stopped=true`。`blockers=[]`。没有改 Java、表或 `调度记录.md`。
