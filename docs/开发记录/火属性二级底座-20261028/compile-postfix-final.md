我先读这份收尾编译任务，确认授权范围后再执行。返修交回新增了 `ElementHurtLifecycleCheck` 和 `FireHurtBytecodeCheck`。我先看作者脚本和这两个入口实际怎么跑，再对最新源码做 `compileJava`。作者脚本会把检查编进 `build/fire-repair-check`，并且不走这次的 Gradle main classes。我只抽出它的入口和依赖，先对最新源码跑 `compileJava`。`compileJava` 对当前源码重新编译通过，退出码 0。全量 `compileTestJava` 本单没有再跑。12 个检查入口各跑一轮，失败都是 0。没有打包，没有启动游戏。返修结果未追加复审。

环境是 JDK 17.0.6 和 `GRADLE_USER_HOME=C:\Users\baishang\.gradle`，离线，没有下载。以往 `verification/` 和 `verification/final/` 日志保留。本轮日志在 `docs/开发记录/火属性二级底座-20261028/verification/postfix/`。作者的 `build/run-fire-hurt-check.ps1` 只读过，没有执行；它会把产物写到 `build/fire-repair-check`，并自己重编一部分主源码。本轮检查类在 `verification/postfix/check-classes`，用的是这次 Gradle 的 `build/classes/java/main`（`FireHurtBoundary.class` 等写于 2026-10-29 00:45:17）。

## 命令

| 命令 | 退出码 | 结果 |
|---|---:|---|
| `compileJava --offline --no-daemon` | 0 | 32 秒，任务实际执行。0 个错误，25 个警告 |
| `exportFireCheckClasspath --offline --no-daemon` | 0 | 重新导出当前 `test` runtimeClasspath |
| 检查 `javac --release 17` | 0 | 原 10 个入口、2 个新入口，以及字节码夹具 `HurtBoundaryFixture`、`HurtBoundaryHook` |

`compileJava` 的 25 个警告仍是待删除 API。本批里有 `FireDamage.java:23`、`SteamBurst.java:29`、`FireSeedState.java:17` 的 `ResourceLocation(String,String)`。没有 `build`、`jar`、`reobf`、`runClient` 或 GameTestServer。

`compileTestJava` 未运行。上一轮已确认它被未修改的 `SkinAttachmentChecks.java:336` 挡住：`Matrix4f.m30` 不是 public，同文件共 20 个错误。这个限制还在。

本会话没有修改 Java、JSON、语言文件或构建配置。

## 本轮入口

每个入口只跑一次。数字只属于这一轮，不把此前的 508 或 583 加进来。

| 检查 | 退出码 | 断言 | 失败 | 层次 |
|---|---:|---:|---:|---|
| `FireSeedCheck` | 0 | 244 | 0 | 纯逻辑 |
| `FurnaceMathCheck` | 0 | 50 | 0 | 纯逻辑 |
| `FurnaceStructureCheck` | 0 | 19 | 0 | 源码结构 |
| `FireLayerCheck` | 0 | 45 | 0 | 纯逻辑 |
| `BurnLedgerCheck` | 0 | 121 | 0 | 纯逻辑 |
| `FireReleaseStructureCheck` | 0 | 41 | 0 | 源码与标签文本 |
| `FireCooldownCheck` | 0 | 16 | 0 | 纯逻辑模拟，加钩子源码文本 |
| `BurnDispatchCheck` | 0 | 16 | 0 | 纯逻辑 |
| `FurnaceRepairCheck` | 0 | 15 | 0 | 布尔门是纯逻辑，其余是源码文本 |
| `FireDeathMessageCheck` | 0 | 18 | 0 | 读语言文件并自行格式化 |
| `ElementHurtLifecycleCheck` | 0 | 16 | 0 | 纯逻辑 |
| `FireHurtBytecodeCheck` | 0 | 26 | 0 | 本地字节码 |

`FireCooldownCheck` 这一轮是 16 条，不是上一轮的 14 条。它仍用 `FireCooldownPolicy` 模拟保留受击记录，并读取 Mixin 与冷却标签源码。没有织入。

`ElementHurtLifecycleCheck` 直接调用 `enterHurt`、`exitHurt(令牌)` 和 `restore`。覆盖正常返回、早退、嵌套异常向外传、回调捕获、进入前抛出、非 `RuntimeException` 的 `Throwable`，以及边界恢复。没有 Mixin 织入，也没有真实 `hurt`。

`FireHurtBytecodeCheck` 读的是本机 `recomp.jar` 里的 `LivingEntity.hurt(DamageSource,float)`。它核对 `lastHurt` 两次 `PUTFIELD`、`invulnerableTime` 一次 `PUTFIELD`，owner 都是 `LivingEntity`，再在内存里套上 `FireHurtBoundary`。夹具段把 `HurtBoundaryFixture` 变换后加载并调用，覆盖正常、早退、直接异常和内层异常。这是本地字节码变换和夹具执行，不是游戏进程里的 Mixin 织入，也不是真实伤害结算。

## 未跑

没有游戏内织入，没有真实伤害、护甲、抗性、抗火、火焰保护、牛奶或存档。全量测试编译仍被旧的 `SkinAttachmentChecks` 挡住。返修结果未追加复审。
