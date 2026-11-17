我先读完这份返修说明，确认这一轮允许改什么，再按那个范围改。返修结论和编译记录是这次的依据。我先把这两份读完，再按 R1 到 R6 的最小修法改。六项返修按已定范围落地：先改编译错误和伤害隔离，再收束元素帧、燃烧提交和炉心线程。R1 到 R6 都已落地。本次源码写入已经全部结束。没有跑 Gradle，没有打包，也没有启动游戏。

## R1 玩家取值

`FurnaceEvents.java` 三处静态类型已经是 `Player` 的模式匹配改成直接取值，空玩家和客户端仍直接返回：

- `onPlayerTick` 第 27 行：`Player player = event.player`
- `onLogin` 第 42 行：`Player player = event.getEntity()`
- `onClone` 第 52 行：`Player successor = event.getEntity()`

死亡、治疗这些事件的实体静态类型仍是 `LivingEntity`，所以 `instanceof Player` 保留。

## R2 资源火伤不改冷却记录

`bypasses_cooldown` 仍只加入 `xueximod:fire_resource`，`replace` 仍是 false。没有清 `invulnerableTime`，没有改差量分支，也没有改 `handleDamageEvent`。

新钩子是 `src/main/java/com/bslx/XueXiMod/mixin/FireResourceHurtMixin.java`，已登记在 `xueximod.mixins.json` 第 31 行。它只挂 `LivingEntity.hurt(DamageSource, float)`：

- 第 37–46 行：`lastHurt` 的 `PUTFIELD` ordinal 1（映射源码里 else 那一次写入），`require = 1`
- 第 54–63 行：`Entity.invulnerableTime` 的 `PUTFIELD` ordinal 0（`hurt` 里唯一一次写入），`require = 1`
- 来源是 `xueximod:fire_resource` 时两处都不写，原值保留；其他来源照常写入

工程锁定的 Mixin 是 0.8.5，没有 `@Local`。所以第 27–34 行在每次 `hurt` 入口把这次的 `DamageSource` 压进调用栈，重定向只读栈顶，返回时弹出。嵌套 `hurt` 有自己的一层。这不是一个会被覆盖的单个缓存槽。ordinal 按映射源码里的写入次数确定，没有用 javap 核对字节码，也没有织入。ordinal 对不上时，`require = 1` 会让 Mixin 应用失败，而不是改到别的字段。

## R3 元素帧绑定这一次 hurt

删掉了线程栈上的 `openFire`、`openWater`、`openIce`、`openWaterIce`、`current()` 和 `AutoCloseable.close()`。

`ElementDamageScope` 现在的公开签名是：

- `ElementArm armFire(UUID casterId, double fireBase, boolean furnaceSettled)`
- `ElementArm armWater()`
- `ElementArm armIce()`
- `ElementArm armWaterIce()`
- `ElementArm armNeutral()`
- `void disarm(ElementArm arm)`
- `HurtCall enterHurt(Object target, Object source)`
- `void exitHurt()`
- `HurtCall currentHurt()`
- `HurtCall.identity()`、`target()`、`source()`；`frame()` 仍是包内可见

`FireDamage.strikeOnce` 第 53 行在 `hurt` 前 `armFire`。`SteamBurst` 第 56 行对每个目标 `armNeutral`，不读炉心、火种、水、冰或火。`FireRuntimeEvents.onHurt` 第 23–28 行只读栈顶这次调用，并且目标和 `DamageSource` 必须就是这次事件的对象。未标记的嵌套 `hurt` 领不到外层帧。同一目标和同一来源对象再进一次 `hurt`，身份不同，也不会借外层帧。显式中性只领走自己的空帧。

`enterHurt` / `exitHurt` 由上面的 Mixin 在 `hurt` 入口和每个返回点调用。纯检查只模拟这对调用，不能证明 Mixin 已织入。

## R4 先提交再打击

`BurnDispatch.tick` 第 39 行在分发伤害前写入已推进的账本，循环里不再写回这份旧账。世代变了或目标已死，剩余段直接停止。

`BurnCarrier` 第 33 行走这个顺序。空账本只删计划键，不推进世代。`clear` 第 94–95 行删除计划并把 `xueximod_fire_burn_gen` 加一。回调里 `schedule` 写上的新轨道会留下，而且这次 tick 不推进它。回调里 `discard` 或死亡清账后，剩余段不能把旧计划写回去。首段仍等满间隔，失败段也已经在提交时前进，加载后仍不补打离线段。

## R5 炉心只在服务端主线程改状态

`FurnaceMath.Outcome.REJECT_THREAD` 在第 42 行，`allowsServerWork(boolean serverPresent, boolean sameThread)` 在第 53 行。`FurnaceThreadGate.onServerThread` 要求 `server != null && server.isSameThread()`。

`FurnaceService.gate` 第 114–115 行让 `enterNormal`、`enterOverdrive`、`endPassive`、`cool` 在不安全线程返回 `REJECT_THREAD`，并且不会接着放蒸汽。查询走 `serverSnapshot` 第 121 行：不安全线程直接返回无炉心快照（火伤加成 0、火种倍率 1、未激活、水冰倍率 1），不调用 `reconcile`。`FurnaceRuntime.reconcile` 第 12 行和 `advance` 第 43 行在写存档、效果或热量前同样拒绝。没有把工作排回主线程再报成功。`suppressesOwnResistance` 只读效果，不写状态。

## R6 死亡句

`zh_cn.json` 第 62–67 行、`en_us.json` 第 3–8 行各加了 6 个键。原有水牢句保留。

- 基础键带 `%1$s` 和 `%2$s`：有造成者时，原版会把受害者和攻击者一起传入
- `.player` 带受害者和击杀归属
- `.item` 带受害者、攻击者和具名武器

没有造成者、也没有击杀归属时，原版基础键只传一个参数。基础句里的 `%2$s` 这时会格式化失败，并显示原文模板。有造成者的路径会带上攻击者。

## 检查

纯 Java 检查均已跑过，失败都是 0：

| 入口 | 断言 | 失败 |
| --- | --- | --- |
| `FireLayerCheck` | 45 | 0 |
| `BurnLedgerCheck` | 121 | 0 |
| `FireReleaseStructureCheck` | 41 | 0 |
| `FireCooldownCheck` | 14 | 0 |
| `BurnDispatchCheck` | 16 | 0 |
| `FireDeathMessageCheck` | 14 | 0 |
| `FurnaceRepairCheck` | 15 | 0 |

变量名门禁仍只报未改动的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`。这次没有改那个文件。

没有在游戏里验证：Mixin 织入、4 tick 资源火伤和普通攻击交错后的 `lastHurt`、护甲、抗性、抗火、火焰保护、牛奶、卸载存档，以及蒸汽击退距离。`compileJava` 留给下一会话，使用 JDK 17 和 `GRADLE_USER_HOME=C:/Users/baishang/.gradle`。
