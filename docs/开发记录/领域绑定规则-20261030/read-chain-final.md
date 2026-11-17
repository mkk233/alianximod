我先读取这份任务说明，确认授权范围后再按其中要求整理完整报告。任务限定为只读采集。我先读调度说明和本目录记录，再只在指定的链解析与规则术式范围内核对代码事实。链路类在 `network` 和 `shu` 两边。我按题目点名的解析、分段和规则注册往下对行号，不扩到领域运行时。本次只做链路事实采集，未改文件，未编译，也未做正确性审查。领域运行时、几何和生命周期留给另一只读任务。链实现实际在 `network`，不在 `shu`：下面的行号来自 `SkillUsePacket`、`SkillChainPlan`、`ChainSteps`、`SkillSegmentMath`、`TechniqueChain`、`CompiledReleaseStep`、`CastPlanContext`、`CompiledCastPlan`、`SkillSegmentRunner`、`ContactCast`、`UncommittedShare`、`CastExecutionSession`，以及 `shu` 里的登记、分段和 `CastFrame`。

## 1. 现行链怎么切、领域是什么角色

一次按键只带槽位和手势。`SkillUsePacket.handle`（102–126）在服务端取发送者，`offerNow`（146–170）读主手：法杖只接受槽 0，并从法杖读冷却与属性加成；否则槽必须是 0–3，技能来自 `PlayerBoundSkills`。然后 `executeSkill`（228–311）用 `ChainSteps.list` 把整条链切成若干步，只留下当前步再 `TechniqueChain.split`。

`TechniqueChain.split`（20–46）把连续附魔挂到下一个不「等宿主」的段上。等宿主的段附魔列表为空；链尾剩下的附魔各自成段。`waitsForHost`（48–62）与 `ChainSteps.waitsForHost`（163–178）是同一份角色表：`SEEK_SPACE`、`SEEK_ENTITY`、`TRAJECTORY_TRACKING`、`TRAJECTORY_DROP`、`TRAJECTORY_SCATTER`、`RULE_TRIPLE`、`RULE_DUAL`，或者 `chainEnergyScale` 偏离 1。

`ChainSteps.list`（21–62）遇到真正的宿主就结束这一步。双重释放的 `pullAfterA` 会再多吞宿主（130–138）。`rule_dual_release` 的 `pullAfterA` 是 1（`ReleaseDefinitions` 17），所以房间是后面 2 个非修饰段（`releasedSegments`，883–899）。

领域没有单独的 `ChainRole`。`ChainRole`（6–15）只有攻击、动作、两种索敌、三种弹道、一分为三、双重。`water_mist`、`water_deep_pressure` 由 `WaterTechniques.domain`（77–83）登记，`WaterReleaseDefinition.role`（349–350）固定返回 `ATTACK`，`kind` 是 `HIT_VOLUME`，基础伤害是 0，`chainEnergyScale` 用接口默认值 1。因此领域不等宿主，会自己结束一步。它后面的修饰和攻击进下一步，要再按一次键才走普通释放。

现行「这一发攻击」的判定是基础伤害大于 0 且不是附魔，不是 `role == ATTACK`：

| 对象 | 现行判定 |
|---|---|
| 修饰扫描停点 | `SkillChainPlan.scanModifiers` 155–161：伤害大于 0 且非附魔则记下 `attackSegment` 并停止；否则不是链修饰就直接停，且不记攻击 |
| 向后收集规则 | `SkillSegmentRunner.scanRoles` 713–720：同样遇到伤害大于 0 的非附魔才停；双重释放共享的成员会跳过 |
| 聚能边界 | `isReleaseBoundary` 597–602：非附魔、`HIT_VOLUME`、`ATTACK`。注释写明聚能和动作宿主不是边界 |
| 一分为三的旧查找 | `findTripleAttack` 734–746：先遇到任意 `ReleaseKind.RULE` 就返回 -1。全仓没有别的调用 |

因此这些东西都是 `ATTACK` 或伤害为 0，但不会被当成被绑定攻击：

- 领域、护体、屏障：`domain()` / `guard()` / `barrier()` 非空且伤害不大于 0。按键路径分别在 `SkillUsePacket` 572–628；双重序列在 `SkillChainPlan.fireDualSequence` 369–445。
- 动作：`movement_dash` 是 `ChainRole.ACTION`。自身状态是 `ReleaseKind.SELF_STATUS`，`SelfStatusDefinition` 的角色也是 `ACTION`，按键时直接提交并返回（454–467）。
- 条件释放：伤害不大于 0 且 `conditionalEffect()` 非空（631）。
- 聚能 `rule_gather_energy`：`ReleaseKind.RULE`，角色却是 `ATTACK`，`chainEnergyScale` 为 2（`ReleaseDefinitions` 28）。它因倍率成为链修饰并等宿主，`scanModifiers` 的 `default` 不记几何（175–176）。
- 落点、锁定、交错、垂落、追踪、风暴轨迹的 `kind` 是 `HIT_VOLUME`，角色才是索敌或弹道。真正 `ReleaseKind.RULE` 的只有双重、一分为三、聚能。

`scanModifiers`（145–192）从起点向后走：空间锁、实体锁、垂落路径、追踪、交错、一分为三把 `copies` 乘 3、双重记下规则段。`volleys` 只有初始值 1（967），没有别的赋值。双重的间隔是常量 `DUAL_STEP_TICKS = 5`（1000）。术式说明文字写的是「隔 10 tick」（`ModTechniques` 97）。这里只并列两处原文。

聚能沿术式下标向前乘（574–594），遇到释放边界就停。领域是 `HIT_VOLUME + ATTACK`，所以是边界。`consumesElementMagic`（636–652）还要求技能和该成员都是法术、有元素、且是命中体积；剑术得到 `ReleaseInput.BASE`。`geometryEnergy`（656–662）只把定义自身倍率乘这份链输入。

同一列表里，领域伤害为 0，挡不住 `scanRoles`。落点或锁定若留在领域前面，向后扫描仍会记到后面的攻击上。按键切步之后，攻击那一步的列表里已经没有领域。

## 2. 计价、退款，以及服务端能复用什么

普通链在 `executeSkill` 369–377 用 `pressCommitCost` 算整步费用，`MagicPool.trySpend` 一次扣下，收据是 `MagicPool.lastReceipt`。费用规则在 `SkillChainPlan.pressCommitCost`（55–119）：

- 已移除规则 `rule_interleave`、`rule_damage_tally` 跳过（`SkillSegmentMath` 116–117）。
- 自身状态跳过。
- 动作先加自己的段费用；后面有攻击则再加攻击单价乘份数乘波次，场地另加 `fieldSpawnCost * copies`（541–546）。
- 链修饰没有自己的伤害价。`ReleaseKind.RULE` 在 `Technique.manaCost`（111）返回 0，除非显式魔力非负。修饰最终把后面攻击的 `segmentManaCost` 乘份数。
- 双重走 `dualSequenceCost`（510–529）。领域、护体、屏障、合并份数的条件、单价大于 0 的条件，按段结算一次，不按份数乘。
- 一分为三对会合并的条件把计价份数收成 1（533–538）。

预约和确认：

- `UncommittedShare.reserve`（28–34）按份切开收据。`commit`（37–42）只标记，不退。`cancel`（61–68）调用 `refundShare`。池关闭时只退还没取走的份（94–104）。
- 即时失败：`stepFailed` 且不是条件链时，整张收据 `refund`（888–892）。条件链只退失败下标之后的 `pressCommitCost`（894–901）。条件段当场失败还会退后续费用（643–647）。
- 延迟任务由 `ChainScheduler.schedule`（20–30）登记；时刻已到就立刻跑，没有周期重放。
- 普通段的延迟取消在 `refundIfBlocked`（988–1010）：施法者死亡、换了维度、`ChargeGate` 代际变了，或当时主手不再允许，就 `cancel`；否则执行后 `commit`。
- 宿主组在 `SkillChainPlan.firePaidPlan`（276–305）和 `SkillUsePacket`（836–862）：中断或 `ReleaseOutcome` 未提交则取消份额，提交成功才 `commit`。非宿主组的延迟路径在 `runner.run()` 之后直接 `commit`（333–342），不看结果。
- 段里带双重，或「动作后面只有一个条件」时，`CompiledReleaseStep.compile`（55–80，92–131）非空，`executeSkill` 352–359 改走 `CastExecutionSession.open`，不再走上面的整步预扣。会话里有 `budget.commit`（247、272、510）、第二段延迟（258）、中断结果（280）和 `refund`（423、526）。动作接触点会读当前位置和准星（163、186）。

包能复用的只有 `slotIndex` 和 `holdGesture`（85–99）。技能编号、朝向、落点都不在包里。服务端硬读主手（147，以及延迟时的 `getMainHandItem`）、槽位上的已装配技能、执行当时的 `getLookAngle()`。`buildSegmentRunner` 在执行瞬间取准星和脚点（119–120）。`buildSegmentRunnerAt`（135–160）可以传入朝向和位置，但实体锁仍会 `LockTargetService.resolve`（543），那条射线是眼睛沿当前准星 20 格、侧向 8 格（`LockTargetService` 19–20、58–62）。`ReleaseBatch.crosshairCenter` 沿准星 32 格（104–129）；`resolveAnchor` 在要锚点时扫 8 格，否则眼睛前方 4 格（34–35、135–164）。打开批次时只要链上有 `SEEK_SPACE` 就把 `wantAnchor` 设上（73–80）。

伪造按键包只能打到该玩家已装配的槽，不能塞进一条新链，也不能自带几何。直接生成命中体会绕过 `trySpend`、解锁检查、冷却、持剑门和这段预约。

## 3. 新规则要登记的身份和显示

现有规则的名字来自 Java 字符串，不是语言文件。`zh_cn.json` / `en_us.json` 没有双重、一分为三、聚能、落点、交错的条目。装配界面画的是 `Technique.getName()`（`SkillAssemblyScreen` 275–282，`SpellAssemblyScreen` 241）。

图标不按术式编号。`techniqueIcon`（224–228）用类型加属性拼 `textures/gui/technique/{attack|other}_{属性}.png`。通用且无属性会用到已经存在的 `other_none.png`。剑术用 `attack_*`。没有规则专用的物品模型 JSON；`models/item/skill_stone*.json` 是通用石头。

因此一条无专用图标的规则，只要 `Technique` 已注册，装配列表就能用现有类型图标和构造时的中文名。不必新做贴图或 JSON。语言键现在不参与这条显示。

要出现在可装配目录，现有规则实际写了这些位置：

- `ModTechniques`：常量、`TechniqueManager.register`、`TechniqueEffectRegistry.register`。双重、垂落、聚能的效果是空实现（274–279）。
- `ReleaseDefinitions.registerAll`：`ReleaseRegistry.register`。注册会调用 `ChainStructureEpoch.changed()`（`ReleaseRegistry` 17–18）。
- 装配消耗：`Technique.assemblyCost`（86–94）。显式值非负时用它；附魔 2，动作 0，伤害大于 0 为 1，其余 0。规则目前是 0。
- 魔力：规则种类为 0；领域走 `domain().initialCost()`（108–109）。
- 新角色若要被当成修饰并等宿主，还要同时改 `SkillSegmentMath.isChainModifier`（92–98）和两处 `waitsForHost`。只改 `ChainRole` 枚举不会自动进入扫描。

领域术式本身在 `WaterTechniques`，不在 `ReleaseDefinitions`。代码里没有「必须排在领域后面」的装配校验。

## 4. 冻住子链之后，每次执行最少要有什么

计划能冻住、且与世界无关的是结构，不是几何。

`ChainSegment` 只有 `actionIndex` 和附魔下标（5–19）。`ModifierTemplate` / `ModifierPlan`（902–997）有：空间锁、实体锁、垂落、路径覆盖、追踪、交错、份数、波次、双重段、攻击段、恢复下标，以及每名成员一份 `ReleaseInput`。`ReleaseInput`（6–7）只有链聚能倍率，`BASE` 是 1。`CompiledCastPlan.Slice`（176–207）另存分段、每段份数、双重拉走的成员、同组、锁定、垂落和 `RoleScan`。`RoleScan`（754–767）是空间锁、实体锁、交错、追踪、路径。`CastPlanContext`（13–98）只指向这次技能和这份切片，代际不符就不再安装新图。

`CastFrame` 是执行时现做的，挂在 `ThreadLocal` 上，不进计划。`forAttack`（90–119）的调用方必须给出伤害、附魔、汇总、复制序号与份数、中心、基础朝向、空间锁、交错、实体锁、路径、弹道是否可变、种子和地面高度。没有空间锁、交错或替换路径时，`overrideGeometry` 为假，生成点不改。

没有鼠标时，这些语义现在都从玩家当时的准星或眼睛来，调用方必须改供：

- 中心：可变弹道且空间锁或龙卷风时用准星落点；可变、交错、且没有实体锁时用身体中点，地面用玩家脚高；只有空间锁时用锚点；否则眼睛（570–580）。
- 朝向：执行瞬间的准星。垂落改成 `(0,-1,0)`，出生点再抬 8 格（86、222–223）。可变弹道的交错在没有实体锁时用种子随机三维方向（225–226、234–239）。斩击交错仍朝基础朝向，只散落点和刀面（122–158）。
- 实体锁：仍向当前准星解析。没有目标就清标记。专一初瞄会把朝向改到目标中心（557–565）。
- 空间抖动半幅是 1.5（88、179），斩击在非空间锁时半幅是 1.0（125）。这不是领域半径内取样。
- 种子现在混的是玩家 UUID、游戏时间、术式编号和波次（582–585）。
- 主手在执行时再读一次。操纵剑和持剑门失败会取消份额。
- 接触原点会盖掉帧的几何（620–631）。

一分为三保留的是 `copies` 乘 3 和宿主组 `Tally`。双重保留的是 `releasedSegments` 与每段间隔 5 tick，并且会把领域、护体、屏障当序列成员。交错、垂落、追踪、落点、锁定保留在 `RoleScan` 和 `ModifierPlan` 的标志里，但落点中心仍要调用方给。聚能保留在该成员的 `ReleaseInput` 里，只对元素法术命中体积生效。

## 5. 事实之外必须裁决的点

下面不是方案定案。

1. 随机点和随机方向的具体含义仍未确认。现成几何只有上面的 1.5 方盒、准星射线、锚点 8/4、锁定 20/8、垂落向下和交错随机方向，没有「领域体积内取样」。
2. 「第一个可用攻击」用哪条现成谓词。活路径是伤害大于 0 且非附魔。那会把热光束、持续术式、燎原这类也算进去。`findTripleAttack` 会在任意 `RULE` 前停下，和活路径不一致。领域、护体、屏障、动作、自身状态、条件释放都不应算。
3. 捕获段怎样从普通按键步里拿掉。现在领域会结束当前步，后面的修饰和攻击属于下一次按键，并仍从准星释放。这和「开域时不再作为普通攻击、改由领域持有」直接冲突。要不要新角色、要不要让领域向前吞段、吞到之后那一步是否还留着，都还没定。
4. 若绑定规则本身做成 `HIT_VOLUME + ATTACK`，它会变成聚能边界，也会让 `scanModifiers` 在它身上停下，后面的攻击绑不上。做成现有修饰角色，则会跟攻击走下一次按键。
5. 每 20 tick 固定再放、没有目标也放。`ChainScheduler` 只跑一次。双重间隔是 5 tick。定义上的 `fieldUpkeepPeriod` 默认 20，不是重放攻击。无目标时，锁定会清标记，准星射线会退回落点；领域 `tryWave` 失败会中断这一步（576–580）。周期释放不能把这些失败当成「没有目标就跳过」。
6. 费用发生在开域一次，还是每 20 tick 一次，以及延迟取消是否沿用 `ChargeGate`、主手和收据份额。这次不审查旧的接触附魔计费。
7. 附魔挂在领域上还是挂在被绑定攻击上。现行分段把附魔交给下一个不等宿主的宿主。领域能承载接触附魔；伤害为 0 又不是领域、屏障、场地、护体或条件的宿主会被 `ContactCast.refusal` 拒绝（19–36）。
8. 捕获段里再遇到双重时，是否仍拉走第一个攻击之后的段。用户要求停在第一个可用攻击；双重现在的房间是两个非修饰段。
9. 周期执行时持剑、冷却、链进度和主手以哪一次为准。延迟路径会重读当时主手。
10. 装配约束现在不存在：多个领域、绑定不在领域后、中间夹屏障或附魔、后面两个攻击、已移除规则。编号、类型、属性和消耗也还是暂名。
