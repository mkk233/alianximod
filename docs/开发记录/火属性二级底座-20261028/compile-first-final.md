我先读任务说明，确认授权范围后再按其中的步骤做。任务范围是统一编译和本批新增的 main 检查。我先读三份最终交回，确认检查入口，再串行编译。三份交回里的新检查是 `FireSeedCheck`、`FurnaceMathCheck`、`FurnaceStructureCheck`、`FireLayerCheck`、`BurnLedgerCheck`、`FireReleaseStructureCheck`。我先看现有运行方式和终端占用，再串行编译。检查类本身是纯逻辑和源码结构。接下来在项目根串行跑 `compileJava`，日志写到本批验证目录。编译没进到 Java：离线模式下 Gradle 插件仓库解析失败。我改用本机已有的 Gradle 缓存重跑，不再下载。`compileJava` 已失败，按任务不接着跑 `compileTestJava`。失败点在炉心事件，纯逻辑类仍可单独编进验证目录并执行这 6 个 main 检查。`compileJava` 失败，退出码 1。`compileTestJava` 没有跑。6 个新增 main 检查都已执行，合计 508 条断言、失败 0。没有进游戏。

## 编译

权威命令在 `D:\alianximod`，`JAVA_HOME` 为 JDK 17.0.6，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`：

`.\gradlew.bat compileJava --offline --no-daemon`

退出码 **1**。日志是 `docs/开发记录/火属性二级底座-20261028/verification/compileJava.log` 和 `compileJava.err.log`。编译器报告 **3 个错误、25 个警告**。

首个错误：

`src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceEvents.java:27`

`TickEvent.PlayerTickEvent.player` 的类型已经是 `Player`，`instanceof Player player` 被拒绝。同一种错误还有第 41 行 `PlayerLoggedInEvent.getEntity()` 和第 50 行 `PlayerEvent.Clone.getEntity()`。本单没有改 Java。

这之前有一次环境误跑：进程里的 `GRADLE_USER_HOME` 指向沙箱缓存，wrapper 把 Gradle 8.8 下到了那个缓存，离线解析找不到 Foojay 0.7.0。退出码也是 1，但还没进入 Java 编译。日志是 `compileJava-sandbox-home.log`。上面的结果才是对着本机已有缓存的源码编译。

`compileTestJava` 按任务要等 `compileJava` 成功才跑，因此未执行。没有 `build`、`jar`、`reobf`、`runClient`，也没有启动游戏。

## 检查

classpath 由验证目录里的 `export-classpath.gradle` 导出，任务 `exportFireCheckClasspath --offline --no-daemon`，退出码 0，121 条 `test` runtimeClasspath，文件是 `test-runtime-classpath.txt`。检查类和火种纯运算源码用 JDK 17 `javac` 编到 `verification/check-classes`，`check-javac` 退出码 0。没有改 `build.gradle`。

| 检查 | 退出码 | 断言 | 失败 |
|---|---:|---:|---:|
| `FireSeedCheck` | 0 | 244 | 0 |
| `FurnaceMathCheck` | 0 | 50 | 0 |
| `FurnaceStructureCheck` | 0 | 19 | 0 |
| `FireLayerCheck` | 0 | 37 | 0 |
| `BurnLedgerCheck` | 0 | 121 | 0 |
| `FireReleaseStructureCheck` | 0 | 37 | 0 |

6 个都跑了，没有跳过失败项。它们不覆盖 `FurnaceEvents` 的编译错误。`compileJava` 失败后，`build/classes/java/main` 里没有 `FurnaceEvents.class`，也没有 `FireSeedMath`、`FireSeedAccount`、`FireSeedArchive` 的 class；火种检查调用的这几个纯类是在验证目录里另编的。

各检查自己打印的未覆盖范围：实体存档、死亡和克隆、生物转化、牛奶、服务端主线程拒绝、属性修饰、治疗、命令清除、抗性与保护、Mixin、数据包加载、标签合并、真实无敌帧和 `hurt`。没有游戏世界。
