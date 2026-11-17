# 作者 A 最终报告：步骤 3 候选查询复用

实现已写入并收束。没有编译，没有游戏实测，没有正式复审。设计额度 2/2 已用完，本次没有另开设计审查。

## 结论

同一服务端 tick、同一世界、Living 空间结构未变时，附近的攻击体和飞剑可以复用一份按原版顺序保存的 LivingEntity 候选。每次请求仍用自己的精确 AABB 和原谓词过滤，命中和结算循环没有改。钩子不齐、线程不对、查询过大或候选过多时走原来的 `getEntitiesOfClass`，不截断结果。

## 实际接入

全部经 `LivingCandidateQueries`。实体查询走 `entityQuery`，目标查询走 `targetQuery`。

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

`UniversalLockTargetEffect` 仍调用 `LockTargetService.resolve`，因此跟着锁定入口复用。`ContactEnchantState` 在领域载体上仍于查询前返回，`DomainContactBilling` 没有接入。

陷阱、引爆、弧带、普通命中的遍历，以及剑的 `hit.size() >= 5`，都留在查询之后。`ReleaseBatch.anchor` 非空时仍直接返回。锁定仍每次重新扫描返回列表，相等距离保留更早的实体；准星相等距离仍换成更晚的实体。

## 没有接入

这些 `getEntitiesOfClass(LivingEntity.class, …)` 仍直接调用原版，因为不在授权文件里，谓词、顺序和副作用没有逐个确认，不能把 helper 扩成全局查询：

- `FieldZones.java` 268、354
- `DomainContactBilling.java` 37
- `GroundFireFields.java` 114
- `FentianEffect.java` 46
- `SteamBurst.java` 43
- `BarrierService.java` 191
- `DomainPulses.java` 78、92
- `FireBurstEffect.java` 78
- `ObstacleBodies.java` 81
- `MagicIgniteEffect.java` 23
- `EnchantedProjectileHandler.java` 34
- `MagicArcaneMissileEffect.java` 51
- `MovementDashEffect.java` 46、90、140

`SwordEntity.anyOwnedBy` / `ownedBy` 的 `getAllEntities` 和风暴 `LAUNCHING` 没有改。术式链结构缓存没有改。世界伤害、无敌帧、附魔、5 tick 吸血和 6 tick 发射间隔没有改。`PerfCounter.java`、`PerfMetric.java`、`PerformanceRecorder.java` 没有改。

## 缓存上界、键、失效

常量在 `LivingCandidateQueries.java` 33–36 行：

- 每个 `ServerLevel` 最多 `ENTRY_LIMIT = 4` 块覆盖
- 每块最多 `CANDIDATE_LIMIT = 256` 个候选
- 查询盒任一轴超过 `EDGE_LIMIT = 48` 直接原查询
- 覆盖盒默认外扩 `MARGIN_BLOCKS = 8`；外扩后超 48 则覆盖盒等于查询盒

键是 `IdentityHashMap` 里的 `ServerLevel` 身份，加上覆盖 AABB。复用要求查询盒六面都落在覆盖盒内（285–287 行）。小结果不回答大盒。查找在 201–210 行，构建在 216–236 行。

过滤在 `select` 290–310 行：按缓存顺序，section 窗口、AABB 相交、原谓词。窗口与 `EntitySectionStorage.forEachAccessibleNonEmptySection`（映射源码 35–64 行）一致：`minX/minZ - 2`、`maxX/maxZ + 2`、`minY - 4`、`maxY + 0`，经 `SectionPos.posToSectionCoord`。实体 section 用 `blockPosition()` 再 `blockToSectionCoord`（313–320 行）。不排序、不去重。`PartEntity` 跳过 section 窗口，只做相交和谓词，对应 `Level.getEntities` 732–766 行末尾的部件循环。

失效只针对 `LivingEntity`。飞剑和攻击体继承 `Entity`，移动不失效。

| 挂点 | 本仓 mixin | 映射源码 |
| --- | --- | --- |
| 坐标变化，含同 section | `LivingCandidateEntityMixin` 19–26 行，`beginPositionChange` / `endPositionChange` 122–138 行。变化在 `onMove` 前失效，返回后再失效 | `Entity.setPosRaw` 3593–3608 行，3607 行调用 `onMove`。`Callback.onMove` 375–392 行在 section 键相同时直接返回 |
| 只改包围盒 | 同 mixin 29–37 行 | `Entity.setBoundingBox` 3076–3078 行，只赋值 |
| Living 加入 section | `LivingCandidateSectionMixin` 24–31 行 | `EntitySection.add` 22–24 行。`addEntity` 78–80 行的 Join 事件在 89 行 `section.add` 之前 |
| Living 离开 section | 同 mixin 34–41 行 | `EntitySection.remove` 26–28 行。`Callback.onRemove` 429 行先移除，离开事件在 `ServerLevel` 1599 行、可见表删除前 |
| section 可访问性 | 同 mixin 44–48 行；section 内没有 Living 则不失效 | `EntitySection.updateChunkStatus` 68–72 行。范围查询在 `EntitySectionStorage` 58 行看 `isAccessible()` |

`TRACKED` 与 `TICKING` 都可访问，互换不失效。加入和移除在写入前后各失效一次，避免回调里用加入前建好的缓存漏掉新实体。位置变化用线程内栈配对，避免无位移的嵌套 `setPosRaw` 误清外层。

代数在 `invalidate` 164–176 行递增。缓存非空才记 `CANDIDATE_QUERY_INVALIDATIONS`。tick 过期（`expireTick` 87–95 行，`alignTick` 97–105 行）和空缓存不加这项。`LivingCandidateEvents`：启动 18–20 行绑定，停服 23–25 行释放，tick 开始最高优先级 28–33 行，tick 结束最低优先级 36–41 行，世界卸载 44–46 行。`HitVolumes` 是默认优先级的 tick 结束，排在本次最低优先级清理之前。

两类 mixin 的静态初始化分别登记。`hookMask` 未齐时 `reusable` 251–258 行返回假，请求走原查询。

## 顺序和副作用

构建覆盖时用无副作用的 `living -> true`，只取空间候选。调用方谓词在小盒相交之后执行一次，不在扩大区域外的实体上执行。谓词抛出时不捕获后重试：尚未选定终态记 bypass；已经决定复用或已经入库则保持该终态（237–245 行）。

返回列表是新的 `ArrayList`。缓存用 `List.copyOf`。嵌套查询不共用可变列表。

已接入谓词在现有源码里不写位置。若谓词在同一次过滤中移动尚未访问的实体，本次仍用查询开始时的快照；原版 section 扫描会看到中途状态。这次差异只出现在这种谓词上。失效后的下一次请求会重建。

## 原 API 回退

`readWorld` 323–333 行调用原来的 `getEntitiesOfClass`。回退条件：

- 钩子未齐、未绑定、线程或服务器身份不符、盒子非有限或任一轴大于 48
- 该世界已有 4 块覆盖且查询不被包含
- 查询落在拒绝区（候选曾超过 256）
- 构建过程中代数变化：丢掉覆盖结果，再按原 AABB 查一次，两次都计入底层查询
- 候选超过 256 或入库时条目已满：不入库，用已取得的覆盖结果滤回小盒，终态是 bypass，不再查第二次

不截断候选，不用全世界扫描验证缓存。

## 计数边界

`lookup` 188–248 行包住 `CANDIDATE_QUERY_LOOKUP`。开头加 `CANDIDATE_QUERY_REQUESTS`。成功路径恰好一个终态：reuse 210 行、build 236 行、bypass 196/204/213/220/224/231 行。

底层 `TARGET_QUERY_CALLS` 或 `ENTITY_QUERY_CALLS` 只在 `getEntitiesOfClass` 返回之后加一。复用不进入 `readWorld`。候选计数是过滤后列表的 `size`，含复用；`delta <= 0` 仍由记录器丢掉。代数变化后的第二次原查询单独再记一次调用和它自己的耗时。

调用方已去掉查询外包的计时和 `ENTITY_QUERY_CALLS` / `TARGET_QUERY_CALLS`，避免双重记账。陷阱、普通命中和剑扫掠不再用方法开头的 `recordingNow` 决定查询次数；查询计数改由记录器在查询当时门控。窄判定和伤害仍用原来的 `recordingNow`。

锁定的距离循环、准星 `clip` 和锚点选择不再计入 `TARGET_QUERY`。作者 B 的枚举注释仍写射线留在 `TARGET_QUERY`，该文件本次未改。窄判定、更新、伤害计数未改。采样关闭时不另写日志。

## 文件、哈希、备份

备份在写入前完成，源文件与备份 SHA-256 一致。开始时间 `2026-10-30T04:11:24.0392218Z`。原哈希和原 mtime 在 `backup-A/original-metadata.json`。

| 路径 | 最终 SHA-256 | 最终 UTC mtime | 备份 |
| --- | --- | --- | --- |
| `shu/technique/query/LivingCandidateQueries.java` | `32ee646d862256fb5a4e783d4d825b892df564282ab1da1605135330623d9e04` | `2026-10-30T04:14:59.3937361Z` | 新增 |
| `shu/technique/query/LivingCandidateEvents.java` | `c287bef00aec6da8c9d65a07b8cd3660e4a761b04b4fcd0cc90efe678c88cb76` | `2026-10-30T04:13:08.7413533Z` | 新增 |
| `mixin/LivingCandidateEntityMixin.java` | `c773a1168ed71f3a7d53713111ca9c084faaef23e73762bbbaf01cae506a4e35` | `2026-10-30T04:13:11.2409523Z` | 新增 |
| `mixin/LivingCandidateSectionMixin.java` | `9fb1c35e6e443322a10d448399cce4d12672fa8ddb15e645f5d989ce0948a60d` | `2026-10-30T04:13:13.9720032Z` | 新增 |
| `TechniqueHitEntity.java` | `293a627ab57f28d7e206e0ce3c06d0976b27ec096fcb5528374a12a580286ea9` | `2026-10-30T04:13:50.6800673Z` | 有 |
| `TechniqueHitSettle.java` | `ba5d383ac31170faffd66563d86abdbb34b1559c1a3c028ce75b468cca6390df` | `2026-10-30T04:13:51.5787177Z` | 有 |
| `SwordEntityMotion.java` | `54acfe88d65e721d2ed7614749c0ebf7f0f5edc976aac74a697601684fc445b3` | `2026-10-30T04:13:53.1503288Z` | 有 |
| `HitVolumes.java` | `c1fd38f98cd8401e25c569e521a0b9516a2c32c7ac2222093c02df65293f23bb` | `2026-10-30T04:14:02.0380519Z` | 有 |
| `TrackingSupport.java` | `a5f3b263885128e44a0f8888e46a670de4406d4343ef0b5cfe3ea1d24934622c` | `2026-10-30T04:14:03.4656993Z` | 有 |
| `LockTargetService.java` | `4e3faefbd41c289fbd12d7a7a9c9d494a29c659d3917fc4317a108966a20f58c` | `2026-10-30T04:14:06.3410643Z` | 有 |
| `ReleaseBatch.java` | `3845e798a6025b99842da91b7eb927cad7cbf743f337983a2f1f36c0295eaa02` | `2026-10-30T04:16:07.4078621Z` | 有 |
| `ContactEnchantState.java` | `4955d6f74031317eddeb2520b7ef4c6b0c830baf2110b12e4aefa8ae679a73cf` | `2026-10-30T04:14:14.1601668Z` | 有 |
| `xueximod.mixins.json` | `353ba6fde87ca81652e2006542f2d42f6eefc3f5e307df8cf5aea527c28bba9c` | `2026-10-30T04:13:54.0495718Z` | 有 |

Java 路径前缀是 `src/main/java/com/bslx/XueXiMod/`。mixin 配置是 `src/main/resources/xueximod.mixins.json`，只在公共 `mixins` 数组末尾追加两项，原有项顺序未改。

另有说明 `docs/候选查询复用说明.md`。备份目录 `docs/开发记录/候选查询复用-20261030/backup-A/`。

名字门禁扫过整个 `src`。本次改过的文件没有违规。未改的 `animation/runtime/OriginalHumanoid.java` 仍有 2 处旧违规，没有顺手改名。

## 未验证和风险

- 未运行 `gradlew`，未编译。mixin 目标是否能在当前映射下注入，没有经过编译器或启动确认。
- 未启动游戏，命中顺序、同距选择和复用命中率都没有实测。
- 若 mixin 类没有完成静态初始化，钩子标记不齐，全部请求走原查询。这是保守结果，不是复用成功。
- 谓词若在同一次过滤中改写尚未访问实体的位置，快照与原版边扫边看可能不同。当前已接入谓词在已读源码里不写位置。
- 绕过 `setPosRaw` / `setBoundingBox` / `EntitySection.add|remove` 的直接字段写入不会失效。
- 既是 `PartEntity` 又被放进 section 的 Living 会跳过 section 窗口。原版末影龙部件不是 Living，这条只影响这类模组部件。
- 候选超过 256 的第一次请求会扫外扩区域再滤回，比单次小盒查询更贵；随后同一覆盖内的请求改为小盒原查询。
- 复用命中不再增加分析器的 `getEntities` 计数。
- 准星射线不再计入 `TARGET_QUERY`，与作者 B 注释不一致；注释文件不在本次写入范围。
- 停服、卸载和跨 tick 只在源码路径上核对，没有运行。

没有完成编译和游戏实测，不能把这次写成已经验证通过。
