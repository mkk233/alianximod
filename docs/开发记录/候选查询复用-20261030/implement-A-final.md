我先读任务说明和相关执行规范，确认这次授权范围后再动手。任务范围已明确：我是作者 A，只做步骤 3 的候选查询复用。先读本目录记录和两份查询报告，再定位可改文件。两份报告已对齐。接下来核对可改入口、计数接口和原版失效点，再备份现有文件。失效点已经对上：同 section 位移、只改包围盒、加入移除和区块可访问性都要单独挂钩。先备份现有文件并核对哈希，然后再改。备份已核对。开始接入候选查询服务、失效挂钩，并替换已授权的查询入口。入口已经接上。正在写说明和最终报告，并记下每个文件的哈希。步骤 3 的候选查询复用已经写入并收束。同一服务端 tick、同一世界、Living 空间结构没变时，附近攻击体和飞剑可以复用一份按原版顺序保存的候选；每次请求仍用自己的精确包围盒和原谓词过滤。没有编译，没有游戏实测，没有正式复审。

完整记录在 `docs/开发记录/候选查询复用-20261030/implementation-A-final.md`，证据在同目录 `evidence-A.json`（`startedUtc=2026-10-30T04:11:24.0392218Z`，`endedUtc=2026-10-30T04:18:49.0922529Z`，`released=true`）。行为说明在 `docs/候选查询复用说明.md`。

## 实际接入

实体查询走 `LivingCandidateQueries.entityQuery`，目标查询走 `targetQuery`。

| 入口 | 位置 | 账本 |
| --- | --- | --- |
| 陷阱踏板 | `TechniqueHitEntity.java` 333–334 | `ENTITY_QUERY` |
| 普通命中 | 同文件 341–342 | `ENTITY_QUERY` |
| 弧带 | 同文件 919–920 | `ENTITY_QUERY` |
| 引爆 | `TechniqueHitSettle.java` 220–221 | `ENTITY_QUERY` |
| 剑扫掠 | `SwordEntityMotion.java` 86 | `ENTITY_QUERY` |
| 体积盒 | `HitVolumes.java` 108–110 | `ENTITY_QUERY` |
| 光束 | `HitVolumes.java` 173–174 | `ENTITY_QUERY` |
| 追踪选敌 | `TrackingSupport.java` 38 | `ENTITY_QUERY`（原先不计数，口径扩大） |
| 锁定 | `LockTargetService.java` 60–61 | `TARGET_QUERY` |
| 准星中心 | `ReleaseBatch.java` 112–113 | `TARGET_QUERY` |
| 冻结锚点 | `ReleaseBatch.java` 138–139 | `TARGET_QUERY` |
| 接触附魔 | `ContactEnchantState.java` 97 | `ENTITY_QUERY` |

`UniversalLockTargetEffect` 仍调用 `LockTargetService.resolve`，因此跟着锁定入口走。领域载体在 `ContactEnchantState` 里仍于查询前返回。陷阱、引爆、弧带、普通命中的后续循环，以及剑的 5 个命中上限，都留在查询之后。锚点一旦写入仍直接返回。锁定每次重新选择，相等距离留更早的实体；准星相等距离仍换成更晚的实体。

## 没有接入

这些调用仍直接使用原版查询：`FieldZones` 268 和 354、`DomainContactBilling` 37、`GroundFireFields` 114、`FentianEffect` 46、`SteamBurst` 43、`BarrierService` 191、`DomainPulses` 78 和 92、`FireBurstEffect` 78、`ObstacleBodies` 81、`MagicIgniteEffect` 23、`EnchantedProjectileHandler` 34、`MagicArcaneMissileEffect` 51、`MovementDashEffect` 46、90 和 140。它们不在授权文件里，谓词和副作用没有逐个确认。

`SwordEntity` 的 `getAllEntities` 所属扫描和风暴 `LAUNCHING` 没改。术式链结构缓存、世界伤害、无敌帧、附魔、5 tick 吸血和 6 tick 发射间隔没改。`PerfCounter.java`、`PerfMetric.java`、`PerformanceRecorder.java` 没改。

## 缓存上界、键和失效

常量在 `LivingCandidateQueries.java` 33–36 行：每个世界最多 4 块覆盖，每块最多 256 个候选，查询盒任一轴超过 48 格走原查询，覆盖盒默认外扩 8 格，外扩后超过 48 格则覆盖盒等于查询盒本身。

键是 `ServerLevel` 的对象身份加上覆盖包围盒。查询盒必须六面都落在覆盖盒内才复用，小结果不会拿去回答更大的盒。过滤在 `select` 290–310 行：按缓存顺序先做与原版相同的 section 窗口（X/Z 两侧 2 格，Y 向下 4 格、向上 0 格），再做包围盒相交，相交之后才执行原谓词。不排序、不去重。`PartEntity` 只做相交和谓词，对应原版在 section 扫描之后追加的部件。

失效只针对 `LivingEntity`。飞剑和攻击体不是 Living，普通移动不会冲掉复用。

| 挂点 | 本仓 | 映射源码 |
| --- | --- | --- |
| 坐标变化，含同一 section | `LivingCandidateEntityMixin` 19–26 行；变化在 `onMove` 前和方法返回后各失效一次 | `Entity.setPosRaw` 3593–3608 行。`Callback.onMove` 375–392 行在 section 键相同时直接返回 |
| 只改包围盒 | 同 mixin 29–37 行，盒子相等则不失效 | `Entity.setBoundingBox` 3076–3078 行 |
| Living 加入 section | `LivingCandidateSectionMixin` 24–31 行，写入前后各失效一次 | `EntitySection.add` 22–24 行。Join 事件在 `addEntity` 78–80 行，实际加入在 89 行 |
| Living 离开 section | 同 mixin 34–41 行 | `EntitySection.remove` 26–28 行。`onRemove` 429 行先移出 section，离开事件在 `ServerLevel` 1599 行 |
| section 可访问性变化 | 同 mixin 44–48 行；section 里没有 Living 则不失效 | `EntitySection.updateChunkStatus` 68–72 行。范围查询看 `isAccessible()` |

`TRACKED` 和 `TICKING` 都可访问，两者互换不失效。服务端启动绑定服务器和线程；停服与世界卸载丢掉引用。tick 开始和 tick 结束会清空候选，查找时若 tick 计数变了也会先清空。这两种过期不计 `CANDIDATE_QUERY_INVALIDATIONS`。该计数只在结构变化并且当时确实清掉了已有缓存时加一。两类钩子的静态初始化都登记之后才允许复用，否则全部走原查询。

## 顺序、副作用和回退

构建覆盖时只用无副作用的恒真谓词。调用方谓词在小盒相交之后执行一次，不在扩大区域外的实体上执行。谓词抛出时不吞掉、不重试。每次返回新的列表，缓存列表不可变。

回退包括：钩子未齐、未绑定、线程或服务器不符、盒子非法或过大、覆盖条目已满、落在“候选曾超过 256”的拒绝区。构建途中世界结构变了，就丢掉覆盖结果并按原包围盒再查一次，两次真实查询都计数。超过 256 个候选时不入库，用那次真实查询滤回小盒，终态记 bypass。

## 计数

一次查找记一次 `CANDIDATE_QUERY_REQUESTS`，并只记 reuse、build、bypass 中的一个终态。`TARGET_QUERY_CALLS` 和 `ENTITY_QUERY_CALLS` 只在 `getEntitiesOfClass` 真正返回后加一，复用不加。候选数是交给调用方的过滤后列表长度，含复用。`CANDIDATE_QUERY_LOOKUP` 包住整次请求，含其中的世界查询，不能与内部的 `TARGET_QUERY`、`ENTITY_QUERY` 相加。

调用方不再外包一层查询计时。锁定的距离循环、准星方块射线和锚点选择不再计入 `TARGET_QUERY`。作者 B 的枚举注释仍写射线留在该项，那个文件这次没改。窄判定、更新和伤害计数没改。

## 文件与备份

备份在改文件前完成，备份哈希与原文件一致。原记录在 `backup-A/original-metadata.json`。

| 路径 | 最终 SHA-256 | 最终 UTC mtime |
| --- | --- | --- |
| `LivingCandidateQueries.java` | `32ee646d862256fb5a4e783d4d825b892df564282ab1da1605135330623d9e04` | `2026-10-30T04:14:59.3937361Z` |
| `LivingCandidateEvents.java` | `c287bef00aec6da8c9d65a07b8cd3660e4a761b04b4fcd0cc90efe678c88cb76` | `2026-10-30T04:13:08.7413533Z` |
| `LivingCandidateEntityMixin.java` | `c773a1168ed71f3a7d53713111ca9c084faaef23e73762bbbaf01cae506a4e35` | `2026-10-30T04:13:11.2409523Z` |
| `LivingCandidateSectionMixin.java` | `9fb1c35e6e443322a10d448399cce4d12672fa8ddb15e645f5d989ce0948a60d` | `2026-10-30T04:13:13.9720032Z` |
| `TechniqueHitEntity.java` | `293a627ab57f28d7e206e0ce3c06d0976b27ec096fcb5528374a12a580286ea9` | `2026-10-30T04:13:50.6800673Z` |
| `TechniqueHitSettle.java` | `ba5d383ac31170faffd66563d86abdbb34b1559c1a3c028ce75b468cca6390df` | `2026-10-30T04:13:51.5787177Z` |
| `SwordEntityMotion.java` | `54acfe88d65e721d2ed7614749c0ebf7f0f5edc976aac74a697601684fc445b3` | `2026-10-30T04:13:53.1503288Z` |
| `HitVolumes.java` | `c1fd38f98cd8401e25c569e521a0b9516a2c32c7ac2222093c02df65293f23bb` | `2026-10-30T04:14:02.0380519Z` |
| `TrackingSupport.java` | `a5f3b263885128e44a0f8888e46a670de4406d4343ef0b5cfe3ea1d24934622c` | `2026-10-30T04:14:03.4656993Z` |
| `LockTargetService.java` | `4e3faefbd41c289fbd12d7a7a9c9d494a29c659d3917fc4317a108966a20f58c` | `2026-10-30T04:14:06.3410643Z` |
| `ReleaseBatch.java` | `3845e798a6025b99842da91b7eb927cad7cbf743f337983a2f1f36c0295eaa02` | `2026-10-30T04:16:07.4078621Z` |
| `ContactEnchantState.java` | `4955d6f74031317eddeb2520b7ef4c6b0c830baf2110b12e4aefa8ae679a73cf` | `2026-10-30T04:14:14.1601668Z` |
| `xueximod.mixins.json` | `353ba6fde87ca81652e2006542f2d42f6eefc3f5e307df8cf5aea527c28bba9c` | `2026-10-30T04:13:54.0495718Z` |

Java 路径前缀是 `src/main/java/com/bslx/XueXiMod/`。mixin 配置只在公共列表末尾追加了两个新项。名字门禁里，这次改过的文件没有违规；未改的 `OriginalHumanoid.java` 仍有 2 处旧违规，没有顺手改名。

## 未验证和风险

mixin 能否在当前映射下注入，没有经过编译。命中顺序、同距选择和复用命中率没有进游戏看过。钩子类若没完成静态初始化，全部请求会走原查询。谓词若在同一次过滤里搬走尚未访问的实体，这次用的是查询开始时的快照，和原版边扫边看可能不同；当前已接入谓词在已读源码里不写位置。绕过 `setPosRaw`、`setBoundingBox` 和 section 加入移除的直接字段写入不会失效。候选超过 256 的第一次请求会先扫外扩区域。准星射线不再计入 `TARGET_QUERY`，与作者 B 的注释不一致。
