# 让锋与断潮：仅改名编译报告

2026-11-03。对已经停写的名称修改源码执行一次 `compileJava --offline --no-daemon`。没有改 Java、资源、配置或工作簿，没有打包，没有启动游戏，没有新增正式复审。编译通过不是用户游戏验收。

## 编译

- 命令：`.\gradlew.bat compileJava --offline --no-daemon`
- 工作目录：`D:\alianximod`
- 仅本次进程使用 `JAVA_HOME=C:\Program Files\Java\jdk-17`、`GRADLE_USER_HOME=C:\Users\baishang\.gradle`。`gradle.properties` 里原有的 `org.gradle.jvmargs=-Xmx3G` 和 `org.gradle.daemon=false` 没有改。
- 开始：2026-11-03T15:39:43.983+08:00
- 结束：2026-11-03T15:40:12.387+08:00
- 退出码：0
- 日志：`BUILD SUCCESSFUL in 28s`。4 个可执行任务里 2 个实际执行、2 个 UP-TO-DATE。`:compileJava` 实际执行，不是 UP-TO-DATE。`:downloadMcpConfig` 也执行了。`:extractSrg` 和 `:createMcpToSrg` 为 UP-TO-DATE。
- JVM：Java 17.0.6，Oracle 17.0.6+9-LTS-190，amd64。日志写明单次 Daemon 会在构建结束时停止。

编译前没有活动的 `runClient`、GradleWrapper 或其它 Java 编译进程。当时能看到的只有本次编译任务自己的启动器进程。空闲 Daemon 没有另关。

## 输入稳定性

最后一次源码写入是 `ModTechniques.java`，2026-11-03T15:33:34.665+08:00。编译开始晚于这次写入。

`src/main` 共 1238 个文件。编译前后清单 `compile-evidence/src-main-before.txt` 与 `src-main-after.txt` 字节和 SHA-256 都相同：`3d497b904911a587421384dddde3b6616f1d223d5886567a8a83d7aa39dc9724`。五份名称修改源文件的前后清单同样相同：`106b6bfafaeea17ae6ae955436c85b44a47dce11a2cb70cfe116045c0a57686f`。它们和实现回执里的哈希一致：

| 文件 | SHA-256 |
| --- | --- |
| `ModTechniques.java` | `0535c71309b2acced2d6520df7789443240a91e9e6a7ac0e3d496b500fd5178a` |
| `ActorCaptureTexts.java` | `9d674aa42f203cfffb8786b10a518da2706a3cb76fa850716a9822f3fb16eb0a` |
| `TideSeverActorAdapter.java` | `7e593e2cb16a9a671fddb000c1d95a202ea9d25ce3da275ab9860181d86d31eb` |
| `zh_cn.json` | `0de02d9b67c8756d6ab244ac92b48a9dfefd337d190b98066259e72e5eff0b7e` |
| `en_us.json` | `0fa028a84d54c1fc675864fdfa3855e9c946a6858065d3089a97df424dbaa98e` |

## 警告

`:compileJava` 打出 45 条过时 API 警告，随后是「某些输入文件使用或覆盖了已过时的 API」。内容是 `ResourceLocation` 构造器被标记为待删除，以及 `HitVolumes.java` 里 `ProjectileImpactEvent.setCanceled` 被标记为待删除。这些文件都不在本次改名的三个 Java 文件里。日志由控制台代码页原样保存，中文说明在 `compile-gradle.log` 里不是 UTF-8。没有把警告当成这次改名的失败，也没有边编译边改。

## 证据

| 文件 | 字节 | 修改时间 | SHA-256 |
| --- | ---: | --- | --- |
| `compile-gradle.log` | 17377 | 2026-11-03T15:40:12.339+08:00 | `0e538f684ab4643e258576f8094378a2eeddeec8c8cbf6d7b0c0a116f3aaa5b6` |
| `compile-evidence/compile-window.txt` | 218 | 2026-11-03T15:40:12.391+08:00 | `7d61eb3a28823241124a86ff1574e029ead52fe9f04f30398393c6f6825b7293` |
| `compile-evidence/src-main-before.txt` | 222780 | 2026-11-03T15:39:15.897+08:00 | `3d497b904911a587421384dddde3b6616f1d223d5886567a8a83d7aa39dc9724` |
| `compile-evidence/src-main-after.txt` | 222780 | 2026-11-03T15:40:25.053+08:00 | `3d497b904911a587421384dddde3b6616f1d223d5886567a8a83d7aa39dc9724` |
| `compile-evidence/name-files-before.txt` | 928 | 2026-11-03T15:39:15.915+08:00 | `106b6bfafaeea17ae6ae955436c85b44a47dce11a2cb70cfe116045c0a57686f` |
| `compile-evidence/name-files-after.txt` | 928 | 2026-11-03T15:40:25.104+08:00 | `106b6bfafaeea17ae6ae955436c85b44a47dce11a2cb70cfe116045c0a57686f` |

Gradle 还写了授权范围内的 `build` 和 `.gradle`。源码没有改。`writesSettled` 为 true。`blockers` 无。下一步交给唯一的 sheet-owner 做最终写回。
