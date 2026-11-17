# 协同回旋斩击 compile-final 重编

状态：reported_done。`compileJava` 与 `processResources` 都已实际执行并通过。这不是游戏验收。

未游戏验证。未打包。未启动游戏。后续返修未追加正式复审。未改 Java。未改制作表。未改 `compile-failed-02/` 归档。

## 编译

`.\gradlew.bat compileJava --offline --no-daemon` 已执行，不是 UP-TO-DATE。Shell 退出码 0，Gradle 打印 `BUILD SUCCESSFUL in 26s`。自动审批没有拦截。Gradle 打印 `Java: 17.0.6`。4 个任务里 2 个执行、2 个 UP-TO-DATE，其中 `:compileJava` 是执行的。

javac 没有错误。有 45 条 `[removal]` 警告，汇总行是 `45 个警告`。内容主要是 `ResourceLocation` 构造器，另有 `HitVolumes.java:208` 的 `ProjectileImpactEvent.setCanceled(boolean)`。完整列表在 `compile-final.log`。这些警告没有让构建失败，也没有在本单里改源码。

## 资源同步

`.\gradlew.bat processResources --offline --no-daemon` 随后单独执行，Shell 退出码 0，`BUILD SUCCESSFUL in 11s`。`:processResources` 实际执行，不是 UP-TO-DATE。日志见 `compile-final-resources.log`。

## 快照与字节

编译前快照 `compile-final-before.json`：2026-11-03T02:58:03.9400074Z，1377 个文件。编译和资源同步后的快照 `compile-final-after.json`：2026-11-03T02:59:19.7626075Z，同样 1377 个文件。新增 0、删除 0、内容变化 0。这次结果覆盖的是该冻结窗口里的 src 字节。

下列文件的 src 与 `build/resources/main` SHA256 相同：

| 路径 | 长度 | SHA256 |
| --- | --- | --- |
| `assets/xueximod/textures/entity/xueximod/oblique_slash_v1.png` | 1293 | `58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7` |
| `assets/xueximod/actions/cooperative_v1/oblique_slash.json` | 3867 | `47AC044586A5A936CD73F477278EC2CBEC11CC711669350C262C177C6204077E` |
| `assets/xueximod/actions/cooperative_v1/spin.json` | 6889 | `9FFCC156AE8F334F9B71D78401E3DDDAE75FBDC1D096739955FC0E4EF2C93DE5` |
| `assets/xueximod/models/effect/cooperative_v1/slash_plane_v1.json` | 722 | `099DBECE2DDCD712F46A48D3586017B4FECCBEC632C404D127416963EBCC3868` |
| `assets/xueximod/lang/zh_cn.json` | 8934 | `E91CAC92A69FC09FBFD088B3A58F7D9D3CBEA842F16F95242D3CDD2D08C03C26` |
| `assets/xueximod/lang/en_us.json` | 6852 | `2221B8AEF8FC1C18CA15F777CA47B3491EDE85F99BB655F3863F4C510507F524` |

正式斩光 PNG 的 src 与输出都等于已确认值 `58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7`。

动作来源只转述既有交付，未重做业务审查：`oblique_slash.json` 来自正式 v1 的 `animation.preview_solo`；`spin.json` 来自 `animation.preview_spin`。

## 环境

工作目录 `D:/alianximod`。进程环境为 `JAVA_HOME=C:/Program Files/Java/jdk-17`、`GRADLE_USER_HOME=C:/Users/baishang/.gradle`，`PATH` 前缀是该 JDK 的 `bin`。`java -version` 为 17.0.6。`gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G` 与 `org.gradle.daemon=false`。未改全局配置。编译前五把工作区 Gradle 锁都可独占打开，没有另一条编译。命名门禁退出码 0。

## 下一步

主会话汇总这次实际编译和资源同步结果。表无本任务匹配，不虚构迁页。交用户游戏实测。45 条 removal 警告仍在日志里，本单没有把它们改掉。
