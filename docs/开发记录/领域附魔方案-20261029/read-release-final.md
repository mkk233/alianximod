我先读取任务说明，按其中授权范围完成只读工作并给出完整报告。任务是只读接点采集。我按指定范围定位计费、释放链和垂丝的真实调用。当前接点是只读事实。领域存在期间仍在扣维护魔力；接触附魔和领域附加伤害都不按次再扣。垂丝是 40 拍的持续命中实体，冷却 20 拍，再次释放不会停掉旧水帘。没有可复用的、脱离按键与准星的服务端子释放入口。下面按路径说明。

## 1. 魔力、先后与伤害失败

来源在 `MagicPool.trySpendTracked`（`player/MagicPool.java` 50–86 行）：先扣主手法杖库存，不够再扣 `PlayerShuData`。付不起则整笔不动，并可选提示「魔力不足」。`canAfford`（35–46 行）用同一口径预检。回复在 `tickRegen`（181–210 行），每 20 拍 5 点，法杖有回复效率时加乘，先填玩家池再填法杖。`restore`（159–170 行）只加玩家池。

收据是 `ManaPaymentReceipt`。`charge`（39–46 行）成功后取 `MagicPool.lastReceipt`。预扣记在 `leftStaff` / `leftPlayer`，事后追加进 `dynamicStaff` / `dynamicPlayer`。`refundShare`（54–86 行）先退追加口袋，再按预扣比例退回原玩家池和当初那根法杖（`findStaff` 按身份找，199–207 行），不退给后来换上的法杖。`retainShare`（93–101 行）只划走可退余额。`spendInto`（`MagicPool.java` 93–151 行）往父收据追加；付不起或法杖身份冲突时池和收据都不改。

按键链在 `SkillUsePacket.executeSkill`（316–324 行）用 `SkillChainPlan.pressCommitCost` 一次预扣。领域术式 `WaterReleaseDefinition.domain` 把显式魔力写成 0（`WaterReleaseDefinition.java` 104–107 行），`SkillSegmentMath.manaCostOf`（236–241 行）因此预扣里只有该段附魔费，不含领域本体。本体在 `DomainService.releaseWave`（85–105 行）：未预留时 `initialCost * 份数`，雾雨与深海重压都是 40（`DomainDefinition.java` 40–46、90–96 行）。同一次按键若后面失败，非条件链 `pressReceipt.refund` 退预扣（`SkillUsePacket.java` 835–838 行）；`tryWave` 已经成功的那笔是另一张收据，这里不会退。条件链只退失败下标起的尾款（841–848 行）。延迟段用 `UncommittedShare`：打断则 `cancel` 退份额，跑完则 `commit`（`SkillUsePacket.java` 935–956 行）。双重编译会话走 `tryWaveReserved`（`CastExecutionSession.java` 297–299 行），父收据已含 `initialCost * 份数`（`CompiledReleaseStep.java` 209–210 行），领域方法内不再扣。

领域活着以后，`DomainService.tick`（134–139 行）到 `nextUpkeepAt` 就 `MagicPool.trySpend(owner, upkeepCost, false)`。雾雨、深海重压都是每 20 拍 8 点，寿命 200 拍，第一次维护点在放置后一个周期（`DomainInstance.java` 42 行）。扣失败、主人不在、死亡、换维或到期，`closeAt` 关掉这一份，不退开域费。死亡、登出、换维还会 `closeOwned` 关掉该玩家全部领域（`ReleaseBaseEvents.java` 151–168、194–202 行）。接触伤害和领域附加都不在命中时扣魔。

根伤害与附魔的顺序：

- 有底伤的命中体在 `TechniqueHitSettle.tryHit`（35–68 行）。间隔内重复命中直接返回。非持续流会立刻 `HitPayloads.apply`；垂丝是持续流，这里跳过。
- 无账本的普通根伤走 `hurtRoot`（137 行）：先 `DomainAuraResolver.merge(..., ROOT_TECHNIQUE)`，再 `TechniqueHits.hurt` 且 `BYPASS_INVULNERABILITY`（`TechniqueHits.java` 29–32、46–58 行）。`hurt` 为真才 `FireSeedGrant.deposit`。
- 垂丝单份无账本走 `settleFlow`（146–165 行）：同样并一次领域附加，但用 `hurtConfirmed` + `RESPECT_INVULNERABILITY`（71–85 行）。只有确认打出正伤才 `FlowSuccessPayload.apply`（潮湿、燃烧、缓慢、击退）并发火种。零伤、取消或无敌帧挡住则这些都不发。
- 一分为三且份数大于 1 时，`firePaidPlan` 打开 `ReleaseBatch.Tally`（`SkillChainPlan.java` 275 行）。命中只 `record`。`FlowRoundLedger` 同一目标、同一轮、同一复制序号只收一次，伤害留最后一份再乘份数（`FlowRoundLedger.java` 32–64 行）。约 6 拍后 `flush`（`ReleaseBatch.java` 204、217–251 行）：持续流用 `hurtConfirmed`，其它用 `hurtRoot`；施法者已无效或目标已死则不发载荷、不发火种。
- 零伤领域的基础附魔不走命中体。`ContactCast.snapshot`（`ContactCast.java` 55–70 行）在释放时冻结 `EnchantmentSnapshot`：接触伤害只累加 `magicDamage`（`EnchantmentSnapshot.java` 16–18、47–64 行）。`ContactEnchantState.scan` 每 2 拍一次（48–50、64–94 行），`TechniqueHits.hurt(..., RESPECT_INVULNERABILITY)`，金额是快照接触伤害，不并领域附加。`hurt` 失败则不点火种、不 `applyEffects`。`AttackEnchantmentContext.grantsAura` 只给近战、箭、术式根伤（`AttackEnchantmentContext.java` 17–18 行）。`DomainAuraResolver` 在攻击者完全处于友方领域内时取一份最大附加（`DomainAuraResolver.java` 12–20、23–46 行）。领域原生潮湿、减速、水势在 `DomainPulses.fire`，不是伤害，也不另扣魔（`DomainNativePolicy.java` 3 行）。

## 2. 链组合与子释放入口

链按装配顺序走，不是先把全链重排成四段再执行。`collectModifiers`（`SkillChainPlan.java` 113–161 行）从修饰段往后收，直到第一个底伤大于 0 且不是附魔的攻击：`SEEK_SPACE`、`SEEK_ENTITY`、`TRAJECTORY_DROP`（含风暴的路径覆盖）、`TRAJECTORY_TRACKING`、`TRAJECTORY_SCATTER`、`RULE_TRIPLE`（份数乘 3）、`RULE_DUAL`。`copiesBefore`（495–506 行）只乘攻击段之前、上一个攻击之后的一分为三。

真正出手在 `SkillSegmentRunner.runSegment`（363–605 行）。它再向前扫描到上一个攻击，写下索敌、追踪、交错、路径覆盖。实体锁调用 `LockTargetService.resolve`：准星 20 格、离视线 8 格（`LockTargetService.java` 16–17、24–31 行），找到则发光 200 拍。然后 `CastFrame.forAttack`（`CastFrame.java` 88–112 行）。`trajectoryMutable == false` 时，只有落点或交错才 `overrideGeometry`，并只改生成位置（`slashGeometry`，116–121 行）。可变弹道才改成垂落或龙卷（`TechniqueHitSpec.spawn` 207–242 行）。不可改的 `FUNCTION` 仍可把 `spec.start` 挪到帧上的 `origin`（222–227 行）。效果在 `CastFrame.bind` 期间执行。

这些调用都绑在玩家按键链上：

- 入口是 `SkillUsePacket.handle`（100–117 行）：槽位、点按或按住。法杖只允许 0 号槽，并读法杖冷却缩减（`offerNow` 139–163 行）；非法杖用绑定槽 0–3 和主手能否释放。
- `CastCooldown.busy` 在链条未结束时拒绝新手（184–188 行）。分步链用 `ChainProgress.current` 记住游标（`executeSkill` 247–252 行）。
- 施法原点默认眼睛；落点用 `ReleaseBatch` 的准星或锚点（`runSegment` 510–520 行）。接触点只有 `contactOrigin != null` 时盖过几何（376–380、560–571 行）。
- 主手在扣费、法杖身份、`HeldSwordGate` 和延迟执行时再读一次。垂丝不要求持剑。
- 领域零伤段不进 `WaterFormEffect`（`WaterFormEffect.java` 31–32 行），而在 `SkillUsePacket` 519–527 行、`fireDualSequence` 320–340 行或 `CastExecutionSession.commitMember` 297–299 行调用 `DomainService.tryWave` / `tryWaveReserved`。

`SkillSegmentRunner` 的 `runSegment`、`commitHostGroup`、`buildSegmentRunner` 都是 `network` 包内可见，要玩家、武器、视线、伤害、账本和冷却门。没有一个公开方法能在领域周期里按绑定术式重放整条链，同时跳过按键、槽位冷却和当时准星。公开且可被领域侧直接调用的是 `DomainService.tryWave`、`TechniqueHitSpec.spawn` 和 `CastFrame.bind`。`spawn` 只有当前绑着 `CastFrame` 时才写入底伤、附魔和账本（`TechniqueHitSpec.java` 163–170 行）。

## 3. 垂丝

垂丝不是瞬发，也不是领域。登记为 `water_weeping_arc`：底伤 2，冷却 20，显式魔力 10，寿命 40，`Trajectory.FUNCTION`，`trajectoryMutable == false`，`persistentFlowSettlement == true`，潮湿 3（`ModTechniques.java` 211–213 行；`WeepingSettings.java` 8–16 行；`ReleaseDefinitions.java` 24、249–260 行）。效果是 `WeepingCurtainEffect`，不是 `WaterFormEffect`（`ModTechniques.java` 296 行）。

每次 `TechniqueHitSpec.spawn` 一个 `TechniqueHitEntity`。无几何覆盖时，起点在眼睛沿水平视线前方 2.2 格，多份按 `copyIndex` 横向错 0.72 格（`WeepingCurtainEffect.java` 42–57 行）。唇口在源点上方 4 格，每拍 `FunctionMotion` 调用 `WeepingCurtain.fit`：充水 8 拍，32 拍起排空，最大落差 6，源被堵住或足迹超预算则本拍无水体（`WeepingCurtain.java` 49–72、125–171 行）。命中间隔 10（`WeepingSettings.HIT_INTERVAL`）。伤害与附魔按上一节持续流：确认正伤后才给潮湿和附魔效果；领域附加并一次。寿命到点 `onExpire` 丢弃实体，`forget` 清掉水段缓存（`TechniqueHitEntity.java` 337–347、640–645 行）。`endsOnRelease` 为假，松键不停。

20 拍后再放：槽位冷却约 20，实体寿命 40，第二发开始时第一发还在。代码里没有按玩家或术式停掉旧垂丝的登记。旧实体只在自己寿命结束、被移除或施法结束路径碰到它时消失；垂丝不在 `DomainService.closeOwned` 那条里。一分为三是同一发里并排的多份，各自寿命和命中，不是替换。落点和交错能搬生成位置；垂落和龙卷不改它的函数路径。实体锁会在 `runSegment` 里解析并发光，但垂丝 `dedicatedInitialAim()` 为假（它同时接受目标覆盖），`WeepingCurtainEffect` 不用锁定目标改方向，仍用玩家当时水平视线。

方案里需要写清的现况：重复释放会重叠到旧帘自然结束；没有「停旧流」；间隔 10 的同一目标会在两帘上各算各的；费用在释放时收 10，存续期间不再为这条水帘扣魔。

## 4. 已有防护与缺失

已有：接触快照和攻击帧在释放时冻结，之后不读主手（`EnchantmentSnapshot` 16–18 行；`runSegment` 526–539 行）。领域几何在放置时冻结，聚能只缩放寿命和地面锚点尺寸（`DomainService.frozenLife`、`placedGeometry`，161–184 行）。同 tick 同主人、同原生种类、同初始费和寿命的领域声明只放一份，第二次 `tryWave` 直接返回成功且不再扣（`DomainService.java` 72–84、114 行）。跨 tick 会再放一份，互不替换。复制波次间隔 5 拍（`SkillChainPlan.DUAL_STEP_TICKS`，815 行）。账本延迟 6 拍。接触每 2 拍。伤害来源是 `playerAttack(施法者)`。垂丝 `canHit` 排除施法者自己（`TechniqueHitSettle.java` 32 行）。领域接触和原生脉冲排除主人、盟友和旁观，并要求身体完全在盒内（`ContactPolicy.java` 45–51 行；`DomainNative.java` 11–21 行）。根术式命中不套用这套盟友过滤。

没有：施法深度或「领域触发放术式」的防递归。没有领域绑定术式、触发器、单目标队列。没有缺魔只跳过这一发却留着领域的分支；维护失败会关掉领域。没有同名按释放组替换。这些在方案里应写成拟新增，不能写成已有能力。

## 5. 最小接入与待确认

最小接入应落在 `DomainService.tick` 里、现有维护扣款和 `DomainPulses.fire` 附近，并且只挂到声明了特殊形式的 `DomainDefinition` 上。雾雨的 `DomainNativePolicy` 仍只做潮湿和水势，不要让所有领域自动获得绑定术式。绑定当时应冻结主人、几何、附魔快照和一份释放输入；周期里若要放出垂丝，应新写服务端入口，显式给出落点和方向，再 `CastFrame.bind` 后 `TechniqueHitSpec.spawn`。不要调用 `SkillUsePacket.handle`，否则会重新吃槽位、冷却、准星和主手。

下面几项当前实现与口头方向不一致，必须由你确认后才能当授权：

- 现况是开域收 `initialCost`，之后每 20 拍收 8 点维护，扣失败就关域。若存在期间不耗维护魔力，要改 `DomainService.tick` 的维护分支。
- 基础附魔接触伤害和领域附加现在都不再按次扣魔。若只有「打出附加伤害」或「发动绑定术式」才额外耗魔，需要新的扣款点，以及失败时关域还是跳过这一发。
- 单目标、缺魔跳过但保留领域、同名按释放组替换，都还没有实现，也还不是已确认规则。
- 垂丝每 20 拍放一次只是例子。真实间隔、是否停旧帘、索敌失败是否仍放、弹道和规则是沿用链上冻结还是每拍重算，都要单独写明。
