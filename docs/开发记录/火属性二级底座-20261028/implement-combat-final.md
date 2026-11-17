我先读任务说明和相关流程，确认这次授权范围后再动手。任务归属是作者 C：火伤消费、燃烧调度和蒸汽爆发。我先读完实现合同和主设计，再按 C 的写权限落地。合同把火伤、燃烧调度和蒸汽爆发划给作者 C。我先对现有伤害类型、关系策略和 A/B 接口做定位，再只在 C 的包里实现。本地映射源里有 `DamageTypeTags` 和 `LivingEntity`。我对照无敌帧和冷却标签，确认 4 tick 结算该走哪条定向机制。本地 1.20.1 映射里确认有 `DamageTypeTags.BYPASSES_COOLDOWN`：`LivingEntity.hurt` 在无敌帧后半段会用它决定是否仍结算全额。接下来只在火伤包、伤害类型和对应标签里实现。冷却标签已经对上：只有带 `bypasses_cooldown` 的来源能在无敌帧后半段仍按全额进入 `actuallyHurt`，护甲和抗性仍在后面的吸收里。开始按这个机制写火伤包。A 和 B 的接口已经落在仓库里。我对照它们的真实方法名，避免火伤这边按合同猜错调用。`Frame` 里的字段和方法重名了，检查编译不过。命名门禁里剩下的两处在没改过的旧文件上，这次不动它们。火伤消费、定时燃烧和蒸汽爆发已经落在作者 C 的包里。专用火伤走 `xueximod:fire_resource`，只进入冷却标签和火焰标签；蒸汽是独立的 `xueximod:steam_burst`。没有改术式、表格、无敌帧字段，也没有跑 Gradle。

## 4 tick 依据

本地 Forge 1.20.1 parchment 映射里，`DamageTypeTags.BYPASSES_COOLDOWN` 存在。`LivingEntity.hurt` 在无敌帧大于 10 且来源没有这个标签时，伤害不高于上次就直接返回；有这个标签则进入另一支，按本次全额调用 `actuallyHurt`。护甲、抗性、保护附魔仍在 `actuallyHurt` 之后。抗火在更前面：来源带 `is_fire` 且目标有抗火时，`hurt` 直接返回。

因此 `fire_resource` 放进 `bypasses_cooldown` 和 `is_fire`。前者只让这一条专用火伤在 4 tick 间隔上仍按全额进入结算；后者让抗火和火焰保护继续生效。没有放进水牢那四份防御绕过标签，代码里也不写 `invulnerableTime`。

原版那一支在放行全额的同时会把 `invulnerableTime` 设为 20。这是 `hurt` 自己的赋值，会刷新其他伤害的无敌帧计时。要避开它就得改 `hurt`，而 mixin 清单不在这次写入范围。`hurt` 返回 false（抗火、无敌、事件取消、已死）只消耗当前这一段，不会补打。

## 行为

同层公式是 `D = B × (1 + A + F)`。燃烧和引爆在挂载时用快照里的炉心算一次，并把作用域标成已结算，伤害事件不再加。没有打开 `ElementDamageScope` 的伤害，包括现有 `playerAttack`，事件直接返回，不按术式 id 或伤害量猜测火、水、冰。以后的术式要在自己的 `hurt` 外包一层显式范围：火伤交出基础值 B，未结算时事件只加一次 `B × F`；水或冰只乘一次 `FurnaceService.waterIceMultiplier`，两种标签同时存在也只乘一次。

消费在服务端主线程上按这个顺序进行：目标与冲突、预览、纯函数生成计划、校验、`consume`、挂载。计划不合法、冲突或封存伤害超限时，不会调用 `consume`。燃烧记在目标持久数据 `xueximod_fire_burn`。挂载当拍不打，等满间隔再打第一段；每拍最多一段；失败也推进。卸载后实体不再 tick，加载只继续剩余等待和剩余段数。施法者离线时伤害来源不带实体，段伤害仍用已封存的快照，不会改记到别的玩家。目标死亡清除；非死亡克隆抄这份数据，死亡克隆不抄。新火种不能改已经封上的段。

`SteamBurst.release(Player)` 使用固定基础伤害 2、身体中心半径 3。先排除自己、记分板同队、自己或在线队友驯服的生物，再用 `RelationPolicy.hostileCapture()`。不读炉心、火种，也不打开元素范围。击退走原版 `knockback`，强度 1.16，是按普通地面、无击退抗性、并叠上原版自带 0.4 击退之后大约 3 格来调的。障碍、固定实体和击退抗性仍会缩短距离。重合或正上方时用退出者的水平朝向。`hurt` 没有落下就不推，也不重试。

燎原、燎里、焚天和旧档案的 0.04、每档 10 tick 只出现在检查输入里。运行模块只接收效果键、排他、间隔、基础值 B 和资源增量 A。

## 安全上限

| 项 | 上限 |
|---|---|
| 每个目标的计划 | 8 |
| 每条计划的段数 | 32 |
| 效果键 | 1–48，字符集 `a-z0-9_.:-` |
| 间隔 | 1–12000 tick |
| 单段封存伤害 | 大于 0 且不超过 10000 |
| 快照投入 | 1–100，且档数、余数与点数一致 |
| 存档版本 | 1；版本不对整份丢弃 |

读档最多解析 8 条。非有限伤害、段数或等待超出上述范围的条目丢弃并打一条警告。等待若为负或大于间隔，钳回一个间隔，不会在一拍里把错过的段补完。

## 接口

- `FireConsumption.schedule(目标, FireCastContext, 请求点数, 效果键, 是否目标全局排他, BurnPlanner)`
- `FireConsumption.detonate(目标, FireCastContext, 请求点数, StrikePlanner)`
- `BurnPlanner.plan(点数, 档, 余数)` 与 `StrikePlanner.component(...)` 必须是纯函数，不能把炉心烘进基础值
- `FireLayer.combine(B, A, F)`
- `ElementDamageScope.openFire / openWater / openIce / openWaterIce`
- `FireDamage.strikeOnce(...)`，类型 `xueximod:fire_resource`
- `SteamBurst.release(Player)`，类型 `xueximod:steam_burst`
- `FireRuntimeEvents` 用 `@Mod.EventBusSubscriber` 自行注册

`requested == Integer.MAX_VALUE` 表示全部，其余非负请求原样交给火种预览和消费。炉心查询用的是施法者 UUID 对应的在线玩家；找不到则为 0，并写入快照。

## 文件

运行代码在 `src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/`：`FireConsumption`、`FireDamage`、`FireRuntimeEvents`、`BurnCarrier`、`BurnLedger`、`SteamBurst`，以及公式、计划、元素范围和在线施法者查找。

数据文件：

- `data/xueximod/damage_type/fire_resource.json`
- `data/xueximod/damage_type/steam_burst.json`
- `data/minecraft/tags/damage_type/bypasses_cooldown.json`
- `data/minecraft/tags/damage_type/is_fire.json`

检查：`FireLayerCheck`、`BurnLedgerCheck`、`FireReleaseStructureCheck`。

## 已跑和未跑

纯 Java 检查已通过：同层与蒸汽 37 项、燃烧状态机和三式输入 121 项、源码与标签 37 项，失败都是 0。这只说明公式、冲突、首段时机、4 tick 段数、尾段、快照冻结和不补打离线。没有编译 Forge，没有加载数据包，没有对真实实体做 NBT、抗火、护甲、队伍、驯服或击退距离验证。

命名门禁仍因未改动的 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to` 退出码为 1。本次火伤文件没有报出违规。

死亡文案还没进语言文件。需要的键是 `death.attack.fire_resource`、`death.attack.fire_resource.player` 和 `death.attack.steam_burst`。这两份语言文件不在这次写入范围。没有这些键时，死亡消息会显示原始翻译键，伤害本身仍然结算。
