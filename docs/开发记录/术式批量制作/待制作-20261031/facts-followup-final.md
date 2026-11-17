我先读跟进任务说明，只按其中写明的范围采集。五项问题已经对齐。接下来只查链长、炉心、结算回调、方块保护和魔力收费这几处接缝。五项接缝已经对上现有类。接下来只把事实写进本目录的跟进报告。设计后五项最小事实补充已经写入 `docs/开发记录/术式批量制作/待制作-20261031/facts-followup-report.md` 与同目录 `facts-followup.json`。源码、资源、制作表和其他文档未改。未编译、未测试、未打包、未启动游戏。写入到此停止。

这是只读信息采集，不是复审。设计累计仍是 1/2，实现 0/3。没有把提案写成代码，没有正式正确性结论，也没有一项可验收。

## 1. 链长 8 与 12 槽法杖

玩法上的 8 段只有一个常数：`Skill.CHAIN_LIMIT = 8`（`skill/Skill.java` 第 20 行）。它管的是一条剑技里的术式个数，不是法杖槽数。

服务端装配：`SkillAssemblyPacket.put` 第 127–148 行。剑技槽必须是 0 到 3；该槽已有段数达到 8 就拒绝，提示「一条剑技最多 8 段」。法杖不走这 8 段，只看手上这件物品的 `StaffData.slotCount()`（第 122–124 行）。

服务端菜单：`SpellAssemblyMenu`。剑技格子是 4 条乘 8 段，`gearSlots` 在非法杖时写成 32（第 67 行）。读档时若某条超过 8 段，`preservedChain[slot] = true`（第 228–230 行），这次打开不改写该条（第 266–267 行）。法杖的格子数等于物品上的 `slotCount()`（第 67 行）。`rejectReason` 第 298–311 行只检查法杖是否装满，不把剑技的 8 段套到法杖上。

客户端界面有两处。`SkillAssemblyScreen.computeSlotCount` 第 119–124 行：法杖读物品槽数，剑读 4。槽数大于 4 时加高并按槽拉宽（第 86–90 行）。`SpellAssemblyMenu` 是 4×8 的剑技格。两边都没有单独的 12 这个常数。

存档：剑技在玩家持久数据 `xueximod_bound_skills` 的 `BoundSkill_0` 到 `BoundSkill_3`。`PlayerBoundSkills.get/set`（第 10–32 行）整份读写 `Skill.toNbt/fromNbt`。`Skill.fromNbt` 第 123–144 行不按 8 截断。所以长于 8 的旧剑技能留在存档里；只有再次经装配写入才会被 8 格界面限制。法杖存在物品 NBT `xueximod_staff_skills/Techniques`。`StaffSkills.add` 第 68 行在 `list.size() >= slotLimit` 时拒绝。`readTechniques` 第 124–132 行不截断已经写成列表的条目。旧的 `SlotN` 键只在迁移时按当时的 `slotCount` 截断（`flattenOldSlots` 第 143–157 行），并删掉旧键。

网络：`BoundSkillSyncPacket` 固定 4 个剑技槽（第 16–38 行），每个槽写完整 NBT，不按 8 段裁。法杖链在物品 NBT 里，随物品同步。本仓库没有为链长另设字节上限。`CastPlanCache.CHAIN_LIMIT = 64`（`CastPlanCache.java` 第 25、72 行）只是超过 64 段就不进计划缓存，不是释放拒绝，也不是 8。`SkillUsePacket.offerNow` 第 156–165 行：法杖只接受槽 0，剑技槽 0 到 3；这里不再检查 8 段。

12 槽法杖：`StaffData` 构造只要求 `slotCount >= 0`（`StaffData.java` 第 10–14 行）。`StaffItem` 保存这份数据（第 17–22 行）。界面、装配包和 `StaffSkills` 都读 `data().slotCount()`，没有把 6 写死在这些路径里。现有注册只有 `ModItems.basic_staff`：`new StaffData(6, 0.20, 0.10, 100, null)`（`ModItems.java` 第 24–25 行）。创造栏只放入这件（`ModCreativeModTabs` 第 51 行）。资源里没有第二件法杖，也没有给它的配方或战利品。因此构造函数能做出 12 槽物品，当前取得路径不能给出 12 槽法杖。

旧物品：槽数在物品类型上，不在每个堆叠的 NBT 里。保持 `basic_staff` 的 `StaffData(6, …)` 不变，现有基础法杖仍是 6 槽。另注册一件 `StaffData(12, …)` 的新物品，不会改旧堆叠的槽数。若直接把 `basic_staff` 改成 12，所有已存在的基础法杖都会变成 12 槽，因为 `slotCount()` 读的是物品实例。剑技四槽和 `Skill.CHAIN_LIMIT = 8` 是另一套常数；不加新法杖、也不改这两个常数，旧剑槽不会扩成 12。

## 2. 炉心 F 与种火倍率

热量 F 的查询是 `FurnaceService.fireBonus`（`FurnaceService.java` 第 17–19 行）。没有档位时返回 0；有档位时返回存档里的 `snapshot.bonus()`。这是热量，不是倍率。

数值范围在 `FurnaceMath`：`HEAT_UNIT = 0.001`（第 8 行）。普通档上限 300 单位，即 0.300；过载上限 600 单位，即 0.600（第 11–12、143–145、151–153 行）。每拍普通加 0.002，过载加 0.003（第 9–10、147–149 行）。无炉心、损坏版本或负数在 `sanitize` 里归零（第 166–181 行）。

现有火伤公式把这个热量当作 F：

- `FireLayer.combine`（`FireLayer.java` 第 3–9 行）：`D = B × (1 + A + F)`。F 是已冻结的炉心热量。A 是资源相对 1 的增量。A 为 0 时才是 `B × (1 + F)`。
- 调用处 `FireConsumption.casterBonus` 第 121–130 行把 `FurnaceService.fireBonus` 传进这个 F。施法者不在线或数值非法时 F 为 0。
- 尚未在这一帧结算过的显式火伤，走 `ElementAdjustment.fireAmount`（第 11–22 行）：在当前伤害上加 `fireBase × F`，F 同样来自 `fireBonus`（`FireRuntimeEvents` 第 35–39、83–88 行）。结算后 `settleFurnace()`，同一帧不再加。

种火倍率是另一个方法：`FurnaceService.seedMultiplier`（第 22–24 行）调用 `FurnaceMath.seedMultiplier`（第 211–225 行）。无档位返回 1.0；过载固定 2.2；普通档按热量在 1.12 到 1.60。`FireSeedProduction.produce` 只用这个倍率做 `floor(基数 × 倍率)`。它不读 `fireBonus`，也不使用 `(1 + F)`。

因此「基础火伤 × (1 + F)」里的 F 应取 `FurnaceService.fireBonus` 的热量：无炉心 0，普通最多 0.300，过载最多 0.600。不能把 `seedMultiplier` 的 1.12–2.2 代进去。现有层公式在 F 之外还有资源项 A；若新效果要写成严格的 `B × (1 + F)`，需要自己把 A 固定为 0，现有 `FireLayer.combine` 不会自动那样做。

## 3. Tally 的结算、取消与免疫

`ReleaseBatch.Tally` 没有结算完成回调。`record` 是公开的（约第 238–283 行）。`flush` 是私有的（第 293 行）。第一次入账后 `FlowRoundLedger.FLUSH_DELAY = 6`（`FlowRoundLedger.java` 第 17 行），用 `ChainScheduler` 排到 `now + 6`。对外只能读 `flushTick()`（第 285–287 行）。冲刷结束没有监听器。

冲刷里的分支（第 300–329 行）：

- 目标已死、找不到或没有伤害：`retireSeeds(..., false)`，不打伤害。
- 带 `FlowSuccessPayload`：主人不在则种子不发。否则 `TechniqueHits.hurtConfirmed`，落地方式 `Landing.RESPECT_INVULNERABILITY`。只有返回真才施加流量载荷，并按该布尔值决定种子是否入账。
- 没有流量载荷：`TechniqueHits.hurtRoot`。种子是否入账等于这次 `hurt` 的布尔值。

`hurtRoot`（`TechniqueHits.java` 第 37–40 行）走 `Landing.BYPASS_INVULNERABILITY`：先把无敌时间写成 0 再 `target.hurt`（第 59–64 行）。`amount <= 0` 时直接返回 false，不调用 `hurt`（第 50–51 行）。

`hurtConfirmed`（第 86–106 行）只接受 `RESPECT_INVULNERABILITY`。它先看 `hurt` 是否成功，再看 `TideMarkDamageScope.finish`（`TideMarkDamageScope.java` 第 54–74 行）。事件已取消或最终伤害不大于 0 时返回 false。这是「必须打出正伤且事件没取消」，不是「免疫也算命中成立」。

几何命中和伤害结算是分开的。`TechniqueHitSettle.tryHit` 先记下 `hitOnce`（第 42–45 行），再 `settle`，然后才调用 `HitListener.onHit`（第 74–82 行）。`onHit` 不看伤害有没有被接受。`burstOnImpact` 在调用 `onHit` 后直接丢弃，不进 `settle`（第 58–72 行）。`onExpire`（第 247 行附近）是到期或着地爆散的位置回调，不表示伤害已入账。

没有「伤害被免疫或取消，但命中仍然成立」的 Tally 事件。零伤害不会进入 `hurt`。无敌帧被根结算绕过；接触确认则把取消和非正伤都当成未成功。Tally 冲刷没有完成回调可以把子段挂上去。

## 4. 方块破坏、放置与保护

本仓库 Java 没有订阅 `BlockEvent`、破坏事件或放置事件，也没有游戏规则包装、领地或冒险模式检查。`build.gradle` 第 179 行编译依赖是 Forge `1.20.1-47.3.33`。Jade 只在第 200 行 `runtimeOnly`，不参与编译，也不是保护接口。没有下载或改配置。

现有改方块只有两处，都直接 `Level.setBlock`，标志为 3：

- `MagicCreateWaterEffect` 第 22–24 行：目标格是空才放水。不先发保护事件。
- `WaterPartingService.writeWater` 第 297 行把水换成空气，失败或结果不是空气就停并回滚已写格子（第 307 行附近）。这是分海自己的租约，不是统一地形开关。

没有术式地形破坏开关，也没有「破坏前询问保护模组」的接缝。统一开关如果要接权限，现有代码里没有可挂的单点；水放置和分海也不会自动跟着新开关走。

## 5. 魔力报价、复制、附魔收费、装配消耗、退款

魔力报价和装配消耗是两套数。

单件魔力：`Technique.manaCost` 第 97–116 行。附魔用 `EnchantmentDefinition.magicDamage`，原胚固定 `NONE` 对 `NONE`，至少为 1；附魔定义缺失时返回 1。`ElementReaction.damageMultiplier` 在非相同时是 1（`ElementReaction.java` 第 43–44 行），所以普通火附魔这条约是 `ceil(2 × 1) = 2`。潮印的 `magicDamage` 固定返回 2。规则种类且没有显式魔力时返回 0。领域用开域费。其余是 `ceil(基础伤害 × 2)`。`ReleaseDefinition.explicitManaCost() >= 0` 时，`SkillSegmentMath.manaCostOf` 第 235–240 行改用这个显式值。

段报价：`segmentManaCost` 第 193–196 行等于宿主 `manaCostOf` 加上该段 `enchantIndexes` 里每件附魔的 `manaCostOf`。作用域是这一段挂上的附魔，不是整条链，也不使用原胚相生倍率。`segmentMagicDamage` 第 178–189 行才按原胚反应累加附魔魔法伤害，那是携带伤害，不是扣款。

复制：`CompiledReleaseStep.memberShareExact` 第 444–475 行。普通宿主是段费用乘份数。条件释放单价大于 0 时只留附魔费，不乘份数（第 449–450 行）。零伤领域、屏障、护体是段费用一次，另加开域或生成费乘份数（第 452–459 行）。声明了复制合并的条件释放不乘份数（第 461–462 行）。场地生成费大于 0 时再乘份数。`exactExpansionRuleShare` 第 423–440 行把不属于任何成员的已计费下标各收一次。`int` 相乘或相加溢出则整份报价拒绝。

只含附魔、且切出来的段列表为空时，`imprintEnchantChain` 第 1184–1198 行把列表里每件 `manaCost()` 加总后立刻 `MagicPool.trySpend`。`legalEnchantChain` 第 1202–1215 行要求全是附魔且没有场地半径。普通 `TechniqueChain.split` 会给链尾附魔各建一段，段列表不是空的，所以这条立刻扣款不是普通附魔链的路径。普通路径仍是段报价。

装配消耗：`Technique.assemblyCost` 第 86–95 行。显式装配消耗优先；否则附魔 2，动作 0，基础伤害大于 0 为 1，其余 0。它不扣魔力。剑技在一条槽内把这些数相加，容量是 `10 + (max(剑术等级, 1) - 1) × 5`（`SkillAssemblyPacket` 第 137–141 行，`SpellAssemblyMenu.rejectReason` 第 316–331 行）。法杖不累加装配消耗，只受 `slotCount` 限制。

预约和退款存在。`GroupBudget.reserve` 第 17–27 行先 `ManaPaymentReceipt.charge`。`commit` 用 `retainShare` 留下已用份额（第 30–38 行）。`settle` 把剩余 `openShare` 经 `refundShare` 退回（第 41–49 行）。收据退回原来的玩家池和原来那根法杖（`ManaPaymentReceipt` 第 12–14、73–79 行）。`CastExecutionSession` 第 248 行按组账单预约，第 626 行附近有 `actionReceipt.refund`。`imprintEnchantChain` 的 `trySpend` 不经过这张收据。水势 `WaterMomentum.reserve` 是另一本账，不是魔力退款。

源码未改。本目录以外没有写入。采集到此停止。
