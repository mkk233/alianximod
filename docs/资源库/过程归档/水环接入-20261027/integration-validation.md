# 水环接入核验（2026-10-27）

`compileJava` 已通过。未打包，未启动游戏，未做游戏内实测。

## 本次验证阶段的源码改动

接入代码清单见 `render-report.md` 与 `wire-report.md`。本次编译验证阶段没有新增主代码改动，主源码在编译前已经是最新，`compileJava` 为 UP-TO-DATE。没有为测试失败去改 `WaterRing`、`GuardSyncPacket`、`GuardClientLifecycle`、`GuardStates`、`ModNetwork`。

## 命令与结果

工作目录 `D:\alianximod`。`GRADLE_USER_HOME=C:/Users/baishang/.gradle`。单进程，`--offline --no-daemon`。

| 命令 | 结果 |
| --- | --- |
| `.\gradlew.bat compileJava --offline --no-daemon` | 成功。日志 `compile.log`：`compileJava UP-TO-DATE`，`BUILD SUCCESSFUL in 12s`，4 个任务（1 执行，3 UP-TO-DATE）。Java 17.0.6。 |
| `.\gradlew.bat compileTestJava processResources --offline --no-daemon` | `processResources` 已产出运行资源。整份测试源集编译失败，日志 `compile-test.log`。失败在既有的 `SkinAttachmentChecks`（JOML 私有字段、`Model.renderToBuffer`）；`PrisonQaPacketCheck` 的过时构造只有警告。不是水环检查，未改这些文件。 |
| `javac` 只编译 `GuardSyncWireCheck`、`GuardSnapshotPoseCheck` | 退出码 0。类在 `build/guard-checks`。classpath 用已有 `build/tide-classpath.txt`，没有启动 Minecraft。 |
| `java ... GuardSyncWireCheck` | 最终 9 组核心、55 条断言通过，退出码 0；协议运行时检查单独 `UNEXECUTED`。见 `guard-check-final.log`。 |
| `java ... GuardSnapshotPoseCheck` | `EXECUTED guard-snapshot-pose`，`断言 14 失败 0`，退出码 0。 |
| `WaterRingRenderCheck` | 本轮未改 `WaterRingV8Math`，按任务不重复跑。先前同目录记录为 `java-render-check-ok`。 |

自定义 Gradle `JavaExec`（`guard-check-init.gradle`）被自动审查拦住，批准重试也被拒绝。上面的 `javac`/`java` 是替代，没有再跑那条 Gradle 命令。

## 断言计数

- `GuardSnapshotPoseCheck`：已执行，14 条断言，0 失败。覆盖年龄 50、零与负年龄夹到 0、起步剩余 10、中途剩余 5 且起点为 `(√0.5, √0.5, 0)`、结束剩余 0 且起点等于终点、静止与空法线。
- `GuardSyncWireCheck`：首次运行只执行 2 条包往返断言，随后被协议类初始化打断。经测例隔离修正，最终 9 组核心（包往返、刷新相位、乱序门控、重复姿态、新激活、晚加入、旧删除、世界清理策略和非负年龄）全部执行，55 条断言、0 失败。核心退出码为 0；协议初始化检查单独输出 `PROTOCOL UNEXECUTED`，不表示整体协议运行时测试通过。生产代码未因此修改，详见 `test-final-report.md`。
- 协议项未执行原因：首次使用 `ModNetwork` 时，`NetworkRegistry.newSimpleChannel` 在 Forge 事件总线里查找 `NetworkEvent` 的无参构造，抛出 `NoSuchMethodException`。这是没有引导 Forge 时的类初始化失败，不是护体断言失败。没有为它启动 Minecraft，也没有改网络代码。

## 运行资源

`processResources` 输出与源文件、归档 runtime 三份字节相同：

| 文件 | 字节 | SHA-256 |
| --- | ---: | --- |
| `water_ring_v8.png` | 46970 | `67a0c18fb9048a8597807015ced446908a9787559f001c1a48ad50e0b6e19708` |
| `water_ring_v8.png.mcmeta` | 61 | `b7e0a427205bfd66824f6fc35ab059aebf29c58517515251ae534e283546ab61` |
| `water_ring_v8_mesh.json` | 230848 | `0943752fc3d5704a5cd54d656104abae0d1ec288416a6b837129559d718f2ea9` |

源路径在 `src/main/resources/assets/xueximod/`，输出在 `build/resources/main/assets/xueximod/`。贴图是 `textures/entity/xueximod/`，网格是 `waterring/`。

## 未做

未运行 `build`、`jar`、`reobfJar`、`runClient`、`runServer`。未做游戏内实测。未跑变量名门禁。

最后仅修测试入口并独立 javac/java 验证，没有再次运行全量测试或主源码编译。临时 build/water-ring-task junction 已非递归移除，docs 中全部过程资料保留。
