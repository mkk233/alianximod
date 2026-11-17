我先读任务文件和这次的补充提示，只按它们写明的范围补事实。补查只对着账本、动作付费和横劈全链。我按这三个符号读直接调用，不重做上一轮事实。横劈的普通释放仍走旧段循环。账本只按目标、轮次和复制序号去重；宿主组能共用调用方传入的同一份 `Tally`，但没有显式角色几何入口。动作窗口的接触点也没有写进后续斩击。

## 1. 账本、直接伤害、宿主组

`FlowRoundLedger`（`D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\FlowRoundLedger.java`）没有取消方法。去重在 `TideMarkCopies.accept`：同一账本内，目标 UUID、`roundIndex`、`copyIndex` 三者相同则拒绝；另一份账本不共享这套记录。

```24:44:D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\FlowRoundLedger.java
public static int roundIndex(int hitInterval, int originTick, int nowTick)
public boolean accept(UUID targetId, int roundIndex, int copyIndex, float damage, int marks, FlowSuccessPayload flowPayload)
```

`hitInterval <= 0` 时轮次恒为 0。收下后伤害被覆盖成最后一份，`aggregatedDamage` 再乘份数。`clearSettled()` 只清本轮计数、伤害和载荷，已见过的轮次与复制序号保留。`FLUSH_DELAY = 6`。

`ReleaseBatch.Tally` 是非静态内部类，构造私有。公开入口是 `ReleaseBatch.openGroupTally()`（第 221–223 行）。记录重载：

```250:264:D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseBatch.java
public void record(LivingEntity target, float damage, int copyIndex, int hitInterval)
public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide)
public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide, FlowSuccessPayload flowPayload)
public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide, FlowSuccessPayload flowPayload, int seedPoints)
```

另外有 `respectLanding()`、`whenComplete(Completion)`、`armResource(UUID, ResourceEnchantLoad, int, int)`、`settledVerdict(UUID)`、`flushTick()`。`flush()` 是私有的。第一次收下且服务器存在时，排到 `now + 6`；已有未到期刷新则不再改期。刷新时目标缺失、已死或没有伤害则不打，记 `INVALID_OR_CANCELLED`；主人不在且带载荷时也不打。没有单独的账本取消方法，已排上的刷新仍会跑。

`TechniqueHitSettle.tryHit(TechniqueHitEntity, ServerLevel, LivingEntity)`（第 38 行）先做存活、观众、施法者自身、分区和命中间隔过滤。`settle`（第 99 行）里：

- `settleTally != null`：只 `record(...)`，返回 `PENDING`，这一下不 `hurt`。
- `settleTally == null` 且施法者还在：`respectLanding` 走 `TechniqueHits.classifyRespecting`，否则 `classifyRoot`，当场结算。
- `persistentFlow` 且有账本：带载荷 `record` 后返回；没有账本则 `classifyConfirmed` 当场结算。

`sweepEarliest`（第 197 行）直接 `classifyRespecting`，不读 `settleTally`。横劈 `prefersEarliestContact()` 默认 false，不走这条。

宿主组能共用调用方传入的同一份 `Tally`，自己不保存账本，也没有角色几何参数。`HostGroupContext.open(HostCast, UUID, int requiredSpawns, int spawnCount, HostGroupListener)` 只冻结视线 `Vec3`、治疗数和动作 ID。`commitHostGroup` 的最长签名在 `SkillSegmentRunner.java` 第 336–344 行，把同一 `tally` 传给组内每次 `runSegment`。`requiredSpawns = copyCount * max(1, SkillEntityCount.entityCount())`，`spawnCount` 是该实体倍率（第 374–388 行）。生成数不符、主人离开或父释放已关时 `seal` 丢弃已生成实体（`HostGroupContext.java` 第 167–182 行），不冲账。`ActorVolumeGeometry` 是断潮用的静态有向盒，没有接到宿主组。

结算序号是 `CohortVisual.settleEncoding(int copyIndex, int spawnIndex)`，即 `copyIndex + spawnIndex * 1024`（`CohortVisual.java` 第 11–13 行）。

## 2. 定时动作与冻结几何

这些都是包内 `static`，不是 `public`。`UncommittedShare` 的预约和取消是 `public`。

- `SkillChainPlan.collectModifiers(Skill, List<ChainSegment>, List<Technique>, int from, CastPlanContext)`，第 196–207 行。
- `firePlan(..., ManaPaymentReceipt, CastSource, CastPlanContext)`，第 271–284 行。内部 `UncommittedShare.reserve(receipt, attackShare, volleys * copies)` 再调用 `firePaidPlan`。
- `firePaidPlan(..., UncommittedShare.Pool, CastSource, CastPlanContext)`，第 292–305 行。
- `frozenChainInput(Skill, List<Technique>, int actionIndex, CastPlanContext)`，第 719–735 行。剑术返回 `ReleaseInput.BASE`。
- `AttackBudgetWindow.open(ServerPlayer, ServerLevel, int castEpoch, UncommittedShare.Pool, Skill, List<Technique>, List<ChainSegment>, ModifierPlan, float weaponBonus, CastSource, CastPlanContext)`，第 54–58 行。`onContact` 调 `firePaidPlan`；`onEnded`/`onFinished` 调 `Pool.close`，只退未取走的份额。
- `UncommittedShare.reserve(ManaPaymentReceipt, int shareEach, int memberCount)`、`cancel(ServerPlayer)`、`Pool.take()`、`Pool.close(ServerPlayer)`。

`ModifierPlan` 只保留落点、锁定、垂落、追踪、交错、`copies`、`volleys`、双重下标和攻击下标（第 1100–1138 行）。`scanModifiers` 的 `default` 不记录错时、命中接续、领域绑定。收集从动作的下一段开始（`SkillUsePacket.java` 第 629–630 行），不回头看动作前面的规则。

正底伤动作会挡住后面的攻击：`scanRoles` 和 `copiesBefore` 遇到前一个正底伤非附魔术就停（`SkillSegmentRunner.java` 第 833–839 行，`SkillChainPlan.java` 第 669–679 行）。冲刺底伤是 2，所以它前面的一分为三和索敌不会记到后面的横劈上。底伤为 0 的动作不会形成这道边界。双重加动作不走 `AttackBudgetWindow`，而在 `onFirstTrigger` 里才 `firePlan`（`SkillUsePacket.java` 第 657–668 行）；触发没发生就不会预约攻击份额。

没有一条已经同时服务新斩击和现有横劈、且不读主人身体的冻结几何入口。横劈效果固定写 `player.getEyePosition()` 和当次 `cast.getLook()`，不读 `CastFrame`（`SwordHorizontalSlashEffect.java` 第 38–74 行）。`TechniqueHitSpec.spawnReported` 只有 `frame.overrideGeometry` 为真才改静止锚点。`firePaidPlan` 和领域自动轮调用 `runSegment` 时接触点都是 null（`SkillChainPlan.java` 第 400–404 行，`AutomaticRound.java` 第 375–377 行）。`BirthLayout.apply(TechniqueHitSpec)` 能写入 `forcedOrigin`/`forcedDirection`，但只在新批次安装了槽时存在；普通横劈不安装它，而且会把 `centered` 设为 false。

## 3. 横劈全链的份数与绕过

普通横劈链没有新批次角色时，`BatchReleaseCompiler.compile` 返回 null（第 16–28 行），`SkillUsePacket.executeSkill` 进入旧段循环（第 339–346 行）。这就是绕过新执行计划的分支。链上一旦出现 `batchLifecycle()`（错时、命中接续、地表、喷涌）、抛物线、喷涌轨迹或新报价附魔，整条技能改走新编译；拒绝时直接失败，不退回旧循环。`routeCapture` 在两者之前，捕获链会更早返回。

份数来源：

- 旧 `ModifierPlan.copies` 只累加本次收集窗口里的 `RULE_TRIPLE`，`volleys` 保持 1。`firePaidPlan` 用这个数，不用 `copiesBefore`。
- `copiesBefore` 从攻击往前乘一分为三，直到上一个正底伤。领域自动轮的 `plannedCopies` 用它（`AutomaticRound.java` 第 302–304 行）。
- `SkillEntityCount.entityCount()` 默认 1，命令上限 4096。无宿主组时，`TechniqueHitSpec.spawnReported` 每个逻辑副本再生成这么多个（第 315–317、341 行）。横劈不在宿主组白名单，因此是「计划副本 × 全局实体倍率」。
- 领域绑定由 `BindingStructureCompiler.compile` 把标记到绑定攻击整段从手动链拿掉（第 64–78、180–206 行）。横劈 `qualifiedAttack` 为真。被拿走的子链稍后由 `AutomaticRound` 直接 `runSegment`，`planContext` 为 null。`BoundMember.create` 的短重载把 `copyCount` 写成 0，自动轮不读这个字段。
- 错时间隔只在新批次：`BatchReleasePlan.STAGGER_GAP = 10`，`BatchCastSession` 第 239 行按子序号乘这格。旧 `scanModifiers` 不实现错时，但错时不会留在旧路径。
- 命中接续同样只在新编译器。横劈可以作为正底伤命中体宿主，没有单独的旧路径实现。

## 缺失

没有账本取消 API，没有宿主组角色几何参数，没有按姿态时刻放出后续攻击的入口，也没有动作接触点到横劈锚点的传参。`ModifierPlan` 不保存错时、命中接续和领域绑定。动作前面的规则不会进入该动作发起的那次收集。

未编译，未做游戏验证，未改文件。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"补查：Tally 按目标+轮次+copyIndex 去重，6 tick 后 flush，无取消 API；settleTally 非空只 record。宿主组可共用调用方 Tally，无显式 actor 几何。动作收集不保留 ACTION 前规则，接触点未传入横劈。普通横劈绕过新批次计划；错时/命中接续走新编译，领域绑定另由自动轮 runSegment。无待写入。","changedFiles":[],"checks":["FlowRoundLedger、TideMarkCopies、ReleaseBatch.Tally、TechniqueHitSettle、CohortVisual","HostGroupContext/commitHostGroup 与 ActorVolumeGeometry","SkillChainPlan collect/fire/frozenInput、AttackBudgetWindow、UncommittedShare","BatchReleaseCompiler、BindingStructureCompiler、AutomaticRound、BirthLayout、横劈效果锚点"],"notRun":["未实现","未编译","未游戏验证","未改表","未正式复审"],"writesSettled":true,"blocker":null,"nextAction":"把这份补查交给正在进行的 Astra 设计第1轮，不另开复审轮次"}
<<<END_CURSOR_TASK_REPORT>>>
