我先读最终编译任务说明，确认这次授权范围后再执行。任务是返修收束后的统一编译。我先读两份返修交回，确认新增回归检查，再串行编译。新增回归是 `FireCooldownCheck`、`BurnDispatchCheck`、`FireDeathMessageCheck`、`FurnaceRepairCheck`。我按上次的 JDK 17.0.6 和本机 Gradle 缓存开始编译，同时核对每项检查实际覆盖哪一层。`compileJava` 已通过。`compileTestJava` 退出码是 1，我先定位首个错误，再看这次有没有产出可运行的检查类。`compileTestJava` 的 20 个错误都在旧的 `SkinAttachmentChecks`，这次没有产出测试 class。接下来只用这次 `compileJava` 的 main class 来跑检查，不拿上次的 class 目录充数。`compileJava` 通过，退出码 0。`compileTestJava` 失败，退出码 1。10 个 main 检查各跑了一轮，失败都是 0。没有改源码，没有进游戏。

环境是 JDK 17.0.6（`C:\Program Files\Java\jdk-17`）和 `GRADLE_USER_HOME=C:\Users\baishang\.gradle`。离线，没有下载。上一轮 `verification/` 日志还在。新日志在 `docs/开发记录/火属性二级底座-20261028/verification/final/`。

## 命令

| 命令 | 退出码 | 结果 |
|---|---:|---|
| `compileJava --offline --no-daemon` | 0 | 34 秒，`BUILD SUCCESSFUL`。0 个错误，25 个警告 |
| `compileTestJava --offline --no-daemon` | 1 | 22 秒。20 个错误，7 个警告 |
| `exportFireCheckClasspath --offline --no-daemon` | 0 | 导出 121 条 `test` runtimeClasspath |
| 检查用 `javac --release 17` | 0 | 只编 10 个检查源码到 `verification/final/check-classes` |

`compileJava` 的 25 个警告都是待删除 API。本批相关的是 `FireDamage.java:23`、`SteamBurst.java:29`、`FireSeedState.java:17` 的 `ResourceLocation(String,String)`。其余在旧的客户端、水牢和潮湿代码里；`HitVolumes.java:206` 仍是 `setCanceled`。

`compileTestJava` 的 20 个错误全在 `src/test/java/com/bslx/XueXiMod/client/action/skinned/attachments/SkinAttachmentChecks.java`。首个是第 336 行：`Matrix4f.m30` 不是 public。同文件还有 `m31`、`m03`、`m01`，以及第 634、809 行匿名类没有实现 `Model.renderToBuffer`。7 个警告是 `PrisonQaPacketCheck` 和 `StatusQueryCheck` 里的 `ResourceLocation`。火属性检查源码没有编译错误。Gradle 因此没有把这 10 个检查写进 `build/classes/java/test`。

这次 `compileTestJava` 的任务图还执行了它依赖的 `copyMixinRefmap`、`processResources` 和 `classes`。没有 `build`、`jar`、`reobf`、`runClient` 或 GameTestServer。

检查 classpath 的第三项是这次编译出的 `build/classes/java/main`（`FurnaceEvents.class` 等写于 23:37:57–23:37:58）。检查类放在 `verification/final/check-classes`，排在 classpath 最前。没有使用上一轮 `verification/check-classes`。这不等于 `compileTestJava` 通过。

## 本轮检查

每个入口只跑一次。下面是这一轮各自的断言，不把上一轮 508 条加进来。

| 检查 | 退出码 | 断言 | 失败 |
|---|---:|---:|---:|
| `FireSeedCheck` | 0 | 244 | 0 |
| `FurnaceMathCheck` | 0 | 50 | 0 |
| `FurnaceStructureCheck` | 0 | 19 | 0 |
| `FireLayerCheck` | 0 | 45 | 0 |
| `BurnLedgerCheck` | 0 | 121 | 0 |
| `FireReleaseStructureCheck` | 0 | 41 | 0 |
| `FireCooldownCheck` | 0 | 14 | 0 |
| `BurnDispatchCheck` | 0 | 16 | 0 |
| `FurnaceRepairCheck` | 0 | 15 | 0 |
| `FireDeathMessageCheck` | 0 | 18 | 0 |

前 6 个是原检查，后 4 个是返修回归。`FireDeathMessageCheck` 以 R6 补完后的 18 条为准：中英文、两种伤害，各自覆盖基础句单参、基础句多传攻击者且与单参相同、`.player` 两参、`.item` 三参，代入后不留 `%`。

## 回归覆盖到哪一层

- **R2** `FireCooldownCheck`：纯逻辑用 `FireCooldownPolicy` 模拟资源火伤保留 `lastHurt` 和无敌时间，以及更小普通攻击被拒绝、更大普通攻击只改差量。结构层只读 `FireResourceHurtMixin`、`xueximod.mixins.json` 和 `bypasses_cooldown.json`。没有织入 Mixin，没有真实 `hurt`、护甲、抗性或火焰保护。
- **R3** 在 `FireLayerCheck` 的「范围」里：纯逻辑直接调用 `armFire`、`armWaterIce`、`armNeutral`、`enterHurt`、`exitHurt`。覆盖嵌套 `hurt` 身份不同、未标记的嵌套不继承外层、显式中性只领走自己的空帧。检查自己调用了 `enterHurt`，不能证明 Mixin 在真实 `hurt` 出入口调用了它们。
- **R4** `BurnDispatchCheck`：纯逻辑内存账本。回调里 `schedule` 的新轨道会留下且当次不推进；`discard` 和死亡清账后剩余段不把旧计划写回，丢弃会推进世代；首段仍等满间隔。没有实体 NBT、死亡事件或真实 `hurt`。
- **R5** `FurnaceRepairCheck`：纯逻辑只测 `FurnaceMath.allowsServerWork` 的四个布尔组合，以及 `REJECT_THREAD` 不是成功。查询先看线程再 `reconcile`、写存档前拒绝、不把异步排队当成成功，都是源码文本顺序。没有真实服务端线程，也没有效果或属性写入。同一检查还用文本确认 tick、登录、克隆直接取 `Player`。
- **R6** `FireDeathMessageCheck`：读两个语言文件，按 `%n$s` 自己格式化。没有加载游戏语言，也没有选择死亡句。

## 未跑

没有游戏、没有 Mixin 织入、没有真实伤害、护甲、抗性、抗火、火焰保护、牛奶、存档或击退。`compileTestJava` 被既有 `SkinAttachmentChecks` 挡住，本单没有改那个文件。本会话没有修改 Java、JSON、语言文件或构建脚本。
