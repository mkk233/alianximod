# 协同回旋斩击 compile-final 重编

状态：failed。`compileJava` 已实际执行，不是 UP-TO-DATE。Shell 退出码 1，Gradle 打印 `BUILD FAILED in 22s`。自动审批没有拦截。不能称为编译通过，也不能当作游戏验收。

未游戏验证。未打包。未启动游戏。后续返修未追加正式复审。未改 Java。未改制作表。未改 `compile-failed-01/` 归档。

## 编译错误

javac 汇总为 2 个错误、45 个警告。控制台中文是乱码，错误位置和符号是清楚的。

1. `src/main/java/com/bslx/XueXiMod/network/CooperativeCastRouter.java` 第 63 行：`FamilyExecutionService.FORCED_ANCHOR.set(origin);`
2. 同文件第 81 行：`FamilyExecutionService.FORCED_ANCHOR.remove();`

`FamilyExecutionService.java` 第 83 行是包内字段 `static final ThreadLocal<Vec3> FORCED_ANCHOR`，没有 `public`。调用方在 `com.bslx.XueXiMod.network`，字段在 `com.bslx.XueXiMod.shu.technique.release.cooperative`。诊断碎片是 `FORCED_ANCHOR��FamilyExecutionService�в��ǹ�����; �޷����ⲿ������ж�����з���`。

这次输出里没有再出现 `CooperativePoseApplier` 或 `MatrixFrames`。

45 条警告都带 `[removal]`，主要是 `ResourceLocation` 构造器，另有 `HitVolumes.java:208` 的 `ProjectileImpactEvent.setCanceled(boolean)`。完整列表在 `compile-final.log`。编译者没有改源码，也没有对同一源码再编一次。

## 执行范围

工作目录 `D:/alianximod`。进程环境为 `JAVA_HOME=C:/Program Files/Java/jdk-17`、`GRADLE_USER_HOME=C:/Users/baishang/.gradle`，`PATH` 前缀是该 JDK 的 `bin`。Gradle 打印 `Java: 17.0.6`。`gradle.properties` 仍是 `org.gradle.jvmargs=-Xmx3G` 与 `org.gradle.daemon=false`。本次单次 Daemon 在构建结束时停止。

编译前五把工作区 Gradle 锁都可独占打开，没有另一条编译。命名门禁退出码 0。

编译前快照 `compile-final-before.json` 为 2026-11-03T02:52:32.4401424Z，1377 个文件。编译后快照 `compile-final-after.json` 为 2026-11-03T02:53:24.9182063Z，同样 1377 个文件。新增 0、删除 0、内容变化 0。源码在这次失败编译期间没有其它写入。编译失败，不能当作最终编译通过。

`processResources` 未执行。没有做 `build/resources/main` 与 src 的字节对照，也没有核对输出侧斩光 PNG。未做业务审查。

## 下一步

主会话把上述两处 `FORCED_ANCHOR` 访问错误交原作者返修。编译者不改 Java。表无本任务匹配，不虚构迁页。
