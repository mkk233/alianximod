# 协同回旋斩击 compile-final

状态：blocked。本单没有执行统一编译，也没有执行资源同步。不能称为完整可交付，也不能称为已经覆盖最终冻结字节的编译。

未游戏验证。未打包。未启动游戏。未新增正式复审。未改制作表。未改额度。未写 Java。

## 放行与作者收束

`记录.md` 的「最后返修交付与最终编译放行」已放行本单：命名门禁、`compileJava --offline --no-daemon`，成功后再 `processResources --offline --no-daemon`。该段写明 I/C/E/V 已最终收束，当前没有本批源码写作者。最近一份作者回执 `integration-repair-round3.final.md` 为 reported_done，writesSettled=true，并写明未编译、未游戏、本阶段额度已用完、返修结果未追加复审。本单只转述该记录，不另作业务审查。

## 并发检查

编译前没有本工作区的 `compileJava` 或 `processResources` 进程。

既有 Gradle daemon PID 50884（2026-11-02 20:25:37 启动）独占持有：

- `.gradle/buildOutputCleanup/buildOutputCleanup.lock`
- `.gradle/8.8/executionHistory/executionHistory.lock`
- `.gradle/8.8/checksums/checksums.lock`
- `.gradle/8.8/fileHashes/fileHashes.lock`

其子进程 PID 81132（2026-11-03 00:59:02 启动）仍在运行，命令特征为 runClient 游戏，不是编译任务。未打印完整命令行。未结束游戏，未结束 daemon。命名门禁和快照完成后再查，上述两把主锁仍被持有，两个进程仍在。因此 Gradle 命令没有启动。

## 环境

用 `C:/Program Files/Java/jdk-17/bin/java.exe -version` 得到：

```
java version "17.0.6" 2023-01-17 LTS
Java(TM) SE Runtime Environment (build 17.0.6+9-LTS-190)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.6+9-LTS-190, mixed mode, sharing)
```

这与本单要求的进程内 `JAVA_HOME=C:/Program Files/Java/jdk-17` 一致。Gradle 没有启动，所以没有把 `GRADLE_USER_HOME=C:/Users/baishang/.gradle` 和 JDK `bin` 的 PATH 前缀送进 Gradle 进程。现有 `gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G` 与 `org.gradle.daemon=false`。未改系统配置、用户配置或该文件，未下载 JDK 或缓存。旧水幻形报告未当作本批成功依据。

## 已做检查

命名门禁 `powershell -NoProfile -File tools/check-name-gate.ps1`：2026-11-02T21:07:49Z 至 2026-11-02T21:07:51Z，退出码 0。日志见 `compile-final-name-gate.log`。输出为「源码命名可读性检查通过：未发现单字母变量或禁止缩写」。未改源码。

编译前 `src` 快照：`compile-final-before.json`，capturedAtUtc=`2026-11-02T21:07:46.5618244Z`，1377 个文件，每项含相对路径、长度、UTC mtime、SHA256。无 BOM。编译未发生，没有 `compile-final-after.json`，没有前后比对。不能宣称这次结果覆盖最终冻结字节。

## 未执行

`.\gradlew.bat compileJava --offline --no-daemon` 未执行。没有退出码，没有 UP-TO-DATE，没有编译错误或警告。日志 `compile-final.log` 只记录未启动。

`.\gradlew.bat processResources --offline --no-daemon` 未执行。日志 `compile-final-resources.log` 只记录未启动。`build/resources/main` 未由本单刷新，动作 JSON、斩光 PNG、语言文件都没有做 src 与输出的字节对照。

## 源资源字节

只哈希了当前 `src`，没有输出侧。

| 相对路径 | 长度 | SHA256 |
| --- | --- | --- |
| `src/main/resources/assets/xueximod/textures/entity/xueximod/oblique_slash_v1.png` | 1293 | `58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7` |
| `src/main/resources/assets/xueximod/actions/cooperative_v1/oblique_slash.json` | 3867 | `47AC044586A5A936CD73F477278EC2CBEC11CC711669350C262C177C6204077E` |
| `src/main/resources/assets/xueximod/actions/cooperative_v1/spin.json` | 6889 | `9FFCC156AE8F334F9B71D78401E3DDDAE75FBDC1D096739955FC0E4EF2C93DE5` |
| `src/main/resources/assets/xueximod/models/effect/cooperative_v1/slash_plane_v1.json` | 722 | `099DBECE2DDCD712F46A48D3586017B4FECCBEC632C404D127416963EBCC3868` |
| `src/main/resources/assets/xueximod/lang/zh_cn.json` | 8934 | `E91CAC92A69FC09FBFD088B3A58F7D9D3CBEA842F16F95242D3CDD2D08C03C26` |
| `src/main/resources/assets/xueximod/lang/en_us.json` | 6852 | `2221B8AEF8FC1C18CA15F777CA47B3491EDE85F99BB655F3863F4C510507F524` |

正式斩光 PNG 的 src SHA256 等于已确认值 `58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7`。输出侧尚未核对。

动作来源按 `visual-delivery.md` 记录，未重做业务审查：`oblique_slash.json` 来自正式 v1 的 `animation.preview_solo`；`spin.json` 来自 `animation.preview_spin`。

## 下一步

主会话等 PID 81132 的游戏和 PID 50884 的 Gradle daemon 释放 `D:/alianximod/.gradle` 锁后，再启动本单。编译者不应结束该游戏或 daemon。源码未改。表无本任务匹配，不虚构迁页。
