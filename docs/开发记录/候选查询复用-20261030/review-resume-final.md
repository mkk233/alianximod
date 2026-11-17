上次复核还没交最终报告。我先读续做说明，按它的范围把缺的核对补完。领域绑定已经写过这些文件，旧行号不能再用。我按当前源码重读候选查询服务、mixin 和实际调用点。静态复核未发现会改变命中、顺序、伤害或失效语义的阻塞缺陷。这不等于已经编译通过，也不等于游戏里已经测过。交付说明里有一处过时句子，只需要改文字，不要把射线计时加回去。

## 范围

本轮是同一次实现复核 3/3 的收束，没有另开第四轮。对照 `backup-A`、首轮完整 `backup-B`，以及说明修复前保存的 `PerformanceRecorder` 第 576 行原文。领域绑定写进 `ReleaseBatch`、`TrackingSupport`、`TechniqueHitEntity` 的分支不计入本次候选复用。`SwordEntity` 仍继承 `Entity`，`TechniqueHitEntity` 仍继承 `Entity`。

作者 B 的最终哈希和独立结束时钟没有测到。`evidence-B-stat-notes.json` 里这两项是空和占位时间，本轮没有补哈希。

## 接入

12 处授权查询都进入 `LivingCandidateQueries`，调用方在查询之后仍做原来的窄判定、引爆、距离选择和剑的 5 个目标限制。

| 入口 | 现行位置 | 账本 |
| --- | --- | --- |
| 陷阱 | `TechniqueHitEntity.java` 341–342 | `ENTITY_QUERY` |
| 普通命中 | 同文件 349–350 | `ENTITY_QUERY` |
| 弧带 | 同文件 971–972 | `ENTITY_QUERY` |
| 引爆 | `TechniqueHitSettle.java` 220–221 | `ENTITY_QUERY` |
| 剑扫掠 | `SwordEntityMotion.java` 86–88 | `ENTITY_QUERY` |
| 体积盒 | `HitVolumes.java` 108–110 | `ENTITY_QUERY` |
| 光束 | 同文件 173–174 | `ENTITY_QUERY` |
| 追踪 | `TrackingSupport.java` 61 | `ENTITY_QUERY` |
| 锁定 | `LockTargetService.java` 60–61 | `TARGET_QUERY` |
| 准星中心 | `ReleaseBatch.java` 118–119 | `TARGET_QUERY` |
| 冻结锚点 | `ReleaseBatch.java` 148–149 | `TARGET_QUERY` |
| 接触附魔 | `ContactEnchantState.java` 97 | `ENTITY_QUERY` |

同一服务端 tick、同一 `ServerLevel`、Living 空间未变时，后一次落在已有覆盖盒内的查询走复用。钩子未齐、线程或服务器不对、单轴超过 48、该世界已有 4 块覆盖且不被包含、或落在拒绝区时，调用原来的 `getEntitiesOfClass`。没有用全世界扫描核对缓存。

飞剑和攻击体的移动不失效。`LivingCandidateEntityMixin` 第 32–34 行只对 `LivingEntity` 处理包围盒；位移入口同样先判断 Living。

未接入的 `getEntitiesOfClass(LivingEntity.class, …)` 与作者 A 的清单一致，仍直接走原版：`FieldZones` 268、354；`DomainContactBilling` 37；`GroundFireFields` 114；`FentianEffect` 46；`SteamBurst` 43；`BarrierService` 191；`DomainPulses` 78、92；`FireBurstEffect` 78；`ObstacleBodies` 81；`MagicIgniteEffect` 23；`EnchantedProjectileHandler` 34；`MagicArcaneMissileEffect` 51；`MovementDashEffect` 46、90、140。`SwordEntity` 的 `getAllEntities` 扫描仍独立。

`ReleaseBatch.crosshairCenter` 和 `resolveAnchor` 在查询前有 `automaticOrigin()` 早退，`TrackingSupport.resolveTarget` 在查询前有自动瞄准早退。这两段不在 `backup-A` 里，不属于本次复用差异，本轮不验收。早退不成立时，下面的查询和同距比较仍按原规则。

## 顺序、谓词和列表

过滤在 `LivingCandidateQueries.select` 290–310 行：先按缓存顺序做 section 窗口和包围盒相交，相交之后才执行原谓词。窗口用 `SectionPos.posToSectionCoord`，边界是 `minX/minZ - 2`、`maxX/maxZ + 2`、`minY - 4`、`maxY`，与已提供的引擎事实一致。代码没有自己排序。负坐标的 section 顺序留在原版那次覆盖查询的列表里，过滤只删掉窗口外的项，相对顺序不变。

小盒被覆盖盒包含时，相交小盒的实体也相交覆盖盒，所以过滤不会漏掉新进小盒、但本就在覆盖结果里的对象。新加入的 Living 靠 section 加入失效，而不是靠这次过滤补进去。

锁定仍用 `distSqr < bestDistSqr`（`LockTargetService.java` 70 行），同距留下更早的实体。准星仍用 `distance <= bestDistance`（`ReleaseBatch.java` 124 行），同距换成更晚的实体。锚点仍用 `distance < best`（154 行），同距留下更早的实体。`anchor != null` 时仍直接返回。

已接入谓词在查询当时读取存活、旁观、主人、已命中集合。`TechniqueHitSettle.canHit` 和剑的 `canHit` 只读这些状态。`volume.hit` 也在谓词里当时判断。返回值是新的 `ArrayList`，缓存是 `List.copyOf`。嵌套查询不共用可变候选表。谓词抛出时原样抛出，不再执行一次。

`PartEntity` 分支对本次结果不可达：查询类型是 `LivingEntity`，原版末影龙部件展开在引擎事实里是关闭的，部件不会进入这个列表。

## 失效和回退

| 变化 | 现行挂点 |
| --- | --- |
| 坐标变化，含同 section | `LivingCandidateEntityMixin` 19–26 行，`setPosRaw` 的 HEAD 与 RETURN |
| 只改包围盒 | 同文件 29–39 行，`setBoundingBox` 的 HEAD；盒子相等则跳过 |
| Living 加入、离开 section | `LivingCandidateSectionMixin` 24–41 行，`add`/`remove` 的 HEAD 与 RETURN |
| 可访问性翻转 | 同文件 44–49 行；`TRACKED` 与 `TICKING` 都可访问时不失效；section 内没有 Living 则不失效 |

`PrisonEntityMixin` 把 `setPos` 里的 `setPosRaw` 重定向到锚点后仍调用 `setPosRaw`，所以上面的注入仍会跑。`DomainMotion.finish` 拉回时调用 `setPos`，Living 会失效。客户端维度在 `invalidate` 164–166 行直接返回，不写服务端缓存。两个 mixin 都在公共 `mixins` 数组末尾（`xueximod.mixins.json` 35–36 行），类里没有客户端类型。`FireHurtMixinPlugin.shouldApplyMixin` 对全部 mixin 返回真。

绑定、停服、世界卸载和 tick 清理在 `LivingCandidateEvents` 18–46 行。tick 开始是最高优先级，tick 结束是最低优先级。`HitVolumes` 的 tick 结束订阅没有写优先级，排在这次最低优先级清理之前，同一拍末尾的体积查询还能看见本拍缓存。停服和卸载会丢掉世界条目。tick 清理直接清空表，不记失效次数。失效计数只在缓存里已经有条目时增加。

上界在 `LivingCandidateQueries` 33–36 行：每世界 4 块覆盖，每块 256 个候选，单轴 48，外扩 8。超过 256 不入库，用已经取到的覆盖结果滤回小盒，终态是 bypass，不再查第二次。代数在构建期间变化时丢掉覆盖结果，再按原 AABB 查一次，两次底层查询都计数。条目满、钩子不齐或盒子过大时走原 API，不截断结果。

## 计数

`readWorld` 323–333 行只在 `getEntitiesOfClass` 返回之后增加 `TARGET_QUERY_CALLS` 或 `ENTITY_QUERY_CALLS`。复用不进入这个方法。`finish` 336–340 行把交给调用方的过滤后 `size` 记入候选数，含复用。`PerformanceRecorder.add` 在采样关闭、线程不对或增量不大于 0 时直接返回。调用方已经去掉查询外包的次数和计时。窄判定、更新和伤害仍用原来的计数。

`TARGET_QUERY` 只包这次真实空间查询。锁定的距离循环、准星方块射线和锚点选择在 helper 外面。`PerfMetric.java` 21–26 行和 `docs/性能记录使用说明.md` 77–79 行已经写成这个收窄口径。`TrackingSupport` 接入后的实体查询扩大口径也写在这两处。

作者 B 只在枚举末尾追加了 5 个计数和 `CANDIDATE_QUERY_LOOKUP`。`CAST_PLAN_BYPASSES` 仍在 `PerfCounter` 53 行，`CAST_PLAN_COMPILE` 仍在 `PerfMetric` 60 行。数组长度仍取 `values().length`。逐 tick 存储和 `ticks.csv` 表头仍是原来的查询、候选、更新列。`/shuperf` 的启停代码没有改。说明修复相对首轮 `backup-B`，再把过时的“射线留在该项”从 `PerfMetric` 注释和 `note_candidate_query` 里改掉。现行第 576 行在修复前那一行之后追加了口径收窄的句子。

## 需要同步的说明

`docs/候选查询复用说明.md` 第 65 行后半仍写着：`PerfMetric.TARGET_QUERY` 的注释还把射线留在该项，而且该文件这次没有改。现行 `PerfMetric.java` 21–26 行已经写明不含查后距离选择、准星方块射线和锚点选择。这句会让后续修改按过时状态把射线计时加回去。

最小修复：改第 65 行这句说明，使它和现行注释一致。不要改 `LivingCandidateQueries` 的计时范围，也不要把射线放回 `TARGET_QUERY`。归属作者 A 的说明文档。作者 B 的返修范围不包括这个文件。它不改变当前命中结果。

## 未验证

- 没有运行 `compileJava`，没有启动游戏。mixin 是否能在当前映射下注入，没有编译或启动结果。
- 本轮没有打开 sources jar。方法名与已提供的引擎事实报告一致；`setPosRaw(DDD)V`、`setBoundingBox`、`EntitySection.add/remove` 的擦除描述符、bridge，以及 `@Shadow chunkStatus` 是否与本机 parchment 字节码完全一致，没有在本轮重新对照字节码。不能把这一点写成运行时注入已经成功。
- `PrisonMotion.acceptConnection` 使用的 `absMoveTo` 是否进入 `setPosRaw`，jar 未重新打开，未验证。
- `EntitySection.getEntities` 在 `updateChunkStatus` 的 HEAD 是否仍能看到 section 里的 Living，jar 未重新打开，未验证。现行逻辑是：可访问性发生变化且流里有 Living 才失效。
- 作者 B 说明修复后的 SHA-256 和独立 UTC mtime 未测。`PerformanceRecorder.java` 没有修复前的整文件备份，只核对了保存的原第 576 行和现行该行。
- 停服、卸载、同距选择和复用命中率没有运行。

绕过 `setPosRaw`、`setBoundingBox` 和 `EntitySection.add/remove` 的直接字段写入不会失效。这是既有边界，当前已读的监狱重定向和领域拉回都走 `setPos` / `setPosRaw`。

本轮只读，没有写文件，没有编译，没有后台作业。实现复核 3/3 到此结束，不自动开第四轮。
