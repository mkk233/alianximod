我先读任务说明，按里面写明的范围执行，再交完整报告。任务是只读定位链计划与当次释放数据。我按指定文件和插桩点做定向查找，不改代码。文件已定位。接下来只读解析入口、计划结构和插桩点，不展开效果实现。每次释放都会把整条链重新切开再分类。现有的 `CompiledReleaseStep` 和 `ModifierPlan` 只活在这一次按下里，而且已经把可复用的链结构和当次的冷却、武器、施法者混在一起。下面按五个问题给事实，供设计裁决使用。

## 1. 重复解析的入口、产物和消费路径

唯一会切开链条的地方是 `SkillUsePacket.executeSkill`。`TechniqueChain.split` 与 `ChainSteps.list` 在 `src` 里没有第二个调用点。

按下入口是 `D:/alianximod/src/main/java/com/bslx/XueXiMod/network/SkillUsePacket.java` 的 `handle`（102–124）包住 `offer`。`HoldRepeat.tick`（`HoldRepeat.java` 50–74）在冷却结束且不要求重新点按时直接再调 `offer`，不经过 `handle`。

`offerNow`（146–199）读取主手、法杖冷却缩减和属性伤害加成、槽位或法杖上的 `Skill`，然后进入 `executeSkill`（228）。顺序是：

1. `ChainSteps.list`（249）按术式顺序分成若干「一发」。非空时用 `ChainProgress.current` 取游标，`ChainSteps.cooldown` 乘上法杖缩减，再 `ChainSteps.slice`（76–83）做成只含这一发的新 `Skill`。后面的下标都是切片后的局部下标。
2. `TechniqueChain.split`（265；实现在 `TechniqueChain.java` 20–45）把前置附魔并进宿主，等待宿主的规则单独成段，链尾附魔各自成段。
3. `CompiledReleaseStep.compile`（306；`CompiledReleaseStep.java` 50–97）。
4. 编译结果非空就进 `CastExecutionSession.open`（312），不再走下面的段循环。
5. 否则 `pressCommitCost`（323）预扣，然后 `while`（372）按段分发。

`compile` 只有两种非空结果，注释写明双重只收两个终结成员：

- 第一个 `RULE_DUAL`（且它前面没有 `ACTION`）：`releasedSegments` 里最多两名成员，费用、整链冷却、动作冷却、`ReleaseInput` 一并写入。
- 没有双重、动作前没有别的可释放段、动作后恰好一个零伤害条件释放：`actionConditional`（104–155），`dualSegment = -1`，`actionLed = true`。

其余返回 `null`，包括普通攻击、纯修饰加攻击、领域、屏障、护卫、力场、剑实体、自身状态、触发链。

| 路径 | 分类发生处 | 产物交给谁 |
| --- | --- | --- |
| 普通段、宿主组、剑操纵 | 段循环 423–433、771–833 | `ChainScheduler` 上的 `buildSegmentRunner` / `commitHostGroup`；`frozenChainInput` 在按下时算好 |
| 动作后有攻击、无双重 | 437–459 的 `collectModifiers` | `AttackBudgetWindow`；接触时 `firePaidPlan` |
| 动作后有双重 | 460–472 | 命中回调里 `firePlan` |
| 修饰开头 | 483–522 再 `collectModifiers` | 无攻击的双重走 `fireDualSequence`；有攻击走 `firePlan`；否则 `imprintEnchant` |
| 双重 / 动作加单一条件 | `compile` 后 `CastExecutionSession` | 接触或立刻 `commitMember`（269–368）；第二名成员再隔 5 tick |
| 条件释放 | 585–679，会话里 287–295 | 按下或到点 `commitConditional` |
| 领域、屏障、护卫、力场 | 526–710；双重序列里 320–396 | 按下或调度点调用对应服务 |
| 持续按住 | `HoldRepeat.tick` | 每次重新 `offer` → 整段 `executeSkill` |
| 纯附魔 | 循环 394–404 的 `imprintEnchant` | 读当前攻击力并写主手 |

`segments.isEmpty()` 且 `legalEnchantChain` 才会进 `imprintEnchantChain`（276–277，1107）。`split` 会把附魔都做成段（`TechniqueChain.java` 29–41），空术式列表又会被 `legalEnchantChain`（1125）拒绝。按这两段代码，这条捷径到不了。非空纯附魔走循环里的 `imprintEnchant`。带 `fieldRadius` 的附魔不算合法纯附魔链。

`SkillChainPlan.ModifierPlan`（774–812）是每次 `collectModifiers` 新建的可变对象。`pressCommitCost`（64、80）用 `skill == null` 再扫一遍，冻结输入因此是 `ReleaseInput.BASE`；真正发射前还会再 `collectModifiers(skill, …)`。`volleys` 只有初值 1，仓库里没有赋值。

`SkillFollowCast.buildRuleTrigger` 会再取 `releasedSegments` 并组 runner，但 `src` 内没有调用点，不是现网入口。它用的 runner 重载会调用 `frozenChainInput(null, …)`（`SkillSegmentRunner.java` 90），`skill == null` 时永远是基准倍率。

`CompiledReleaseStep` 能复用的是下标、成员、份数费用、拒蓄力和 `ReleaseInput`。混进当次数据的是 `fullCooldown` / `actionCooldown`（已乘 `cooldownReduction`）、以及会话里另存的施法者、载体 `ItemStack`、`weaponBonus`、`CastSource`、纪元。`members` 是这一次 `split` 造出来的 `ChainSegment` 实例；`memberShare`（201）和 `commitMember`（284）用 `segments.indexOf(member)`，换一次 `split` 身份就对不上。

## 2. 候选字段依赖

只取决于术式顺序、id、是否附魔、术式类型与属性、技能类型、原胚，以及登记表上的角色、拉取、能量、弹道、费用和冷却的：

- `ChainSteps.list` / `signature`（签名只有 id，不含原胚和技能类型）、`slice` 的下标分组
- `ChainSegment` 的 `actionIndex` 与 `enchantIndexes`
- `ModifierPlan` 的锁定、垂落、追踪、交错、`copies`、`dualRuleSegment`、`attackSegment`、`resume`
- `copiesBefore`、`releasedSegments`、`lockThisCast`、`dropThisCast`、`sameReleaseGroup`
- `frozenChainInput` / `consumesElementMagic`（545–573）：元素法术且种类为命中体积才乘前面的 `chainEnergyScale`
- 未缩放的冷却原料、`gapBefore` 里的原胚反应、段魔力配方、`chargeRefusal`
- 段上 `EnchantBonus.resolve` 用的是术式底伤和原胚，层数在这些调用里是 1 或链上 `copies`

必须留在当次、不能跨次共享的：

- 法杖 `cooldownReduction`、`attributeDamageBonus`（`offerNow` 156–157）；属性加成只进普通循环和触发链的 `segmentDamage`（719、745）。`firePaidPlan`（183–185）和 `commitMember`（323）把这项传成 `null`
- `weaponBonus`（230，来自当前攻击力）
- 主手 `ItemStack`、持剑门、世界剑是否存在
- `ChainProgress` 游标
- `WaterBeatBridge.capture` 的 `CastSource`
- 魔力回执、领域是否已学、冷却账本
- 当前 tick、`ChargeGate` 纪元、所在世界
- 视线、位置、眼睛、接触点与方向
- `FieldZones.influence`、`LockTargetService.resolve`、`ReleaseBatch` 的准星和锚点
- 随机种子（`runSegment` 531–534：UUID、游戏时间、术式 id、volley）
- `ContactCast.snapshot` → `EnchantmentSnapshot.from(..., player)`，快照之后不再读主手
- `imprintSelection`（641–643）当时的攻击力属性与主手
- `ReleaseBatch`、`Tally`、`HostCast`、份额与退款

`ReleaseInput` 只有链聚能倍率（`ReleaseInput.java` 6–15）。`CastFrame` 上的伤害、附魔、账本、几何、场地和潮印都是执行期写入。

## 3. 动态值的读取时点与不能共享的闭包

按下时读取：武器与攻击力、法杖两项、游标、切片、`CastSource`、魔力、领域解锁、纪元、tick、普通段伤害数字、普通路径的 `lockThisCast`/`dropThisCast`、领域/屏障/条件/力场的接触快照、`ReleaseInput`。自身状态在循环里直接返回（408–421），但 list、split、compile 已经做过。

调度到点或接触/蓄力后再读：`HeldSwordGate` 再看主手；`AttackBudgetWindow.onContact`（67–72）才 `firePaidPlan`，并在那时再次 `getMainHandItem` 计算伤害。动作加双重要等命中回调（464–470）才 `firePlan`。`commitMember` 在接触或第二段 tick 重新算份数、锁定、垂落、快照和伤害，载体用的是打开会话时的那份 `ItemStack`，`weaponBonus` 也是打开时冻结的。力场在 `chargeTicks` 之后提交动量并读视线（689–702）。`runSegment` 若未武装蓄力，会再排一拍然后重入自己（392–416）。

段执行时读取：`player.getLookAngle()` / `position()`（runner 107–108）、场地影响（477）、锁定目标（492）、准星或锚点（521–527）、种子、`CastFrame`。命中之后的效果体本次没有展开。

这些闭包持有施法者、术式列表引用、按下或发射时的 `ItemStack`、`ReleaseBatch.current()`、回执和触发器，不能放进跨次计划，也不能提前把武器或视线快照成共享状态：

- `buildSegmentRunner`（100–120）和 `buildSegmentRunnerAt`（158）
- `firePaidPlan`（233–298）里的武器、伤害、份额、`chainInput`
- `AttackBudgetWindow` 一直握着 `ModifierPlan`、列表和份额
- `CastExecutionSession` 握着载体、列表、`segments`、计划、`weaponBonus`、`castSource`
- 条件、力场、护卫、触发链（750–763 会在命中时再扣魔力）、`refundIfBlocked`（948）
- 蓄力重入（402）带着同一把武器和当时的视线、位置

`ReleaseInput` 与角色布尔值可以按装配共享。玩家、武器栈、批次、账本、回执、接触几何、附魔快照不行。

## 4. 最小接入边界（候选，不是裁决）

不可变计划以「整链术式 id 顺序 + 原胚 + 技能类型」为失效条件。只放：分发下标、每个切片的段、角色标志、份数、双重成员下标、锁定/垂落、`ReleaseInput`、未缩放冷却原料、魔力配方、蓄力拒绝。用下标，不要缓存 `ChainSegment` 实例。`ChainSteps.signature` 只有 id，不够当键。

当次对象在按下创建，接触和执行时再填：游标、缩放后的冷却、武器加成、法杖属性加成、`CastSource`、纪元、世界、槽位与载体、回执、接触点、视线、场地、锁定目标、种子、批次与宿主。

接入候选位置：

- `executeSkill` 249、265、306、323：list、split、compile、预扣改为读计划
- 438 与 484 的 `collectModifiers`：改为按切片取已分类的修饰组
- `runSegment` 446–470：若改读计划上的标志，这是唯一角色扫描点
- `commitMember` 284–353 与 `firePaidPlan` 181–192：伤害和快照仍在执行时算，留在当次对象

`CastFrame`、`ReleaseBatch` 继续只表示这一次的几何和账本。

## 5. 插桩口径

`PerfMetric`（3–13）写明各项是 inclusive，可嵌套，禁止相加当总耗时。

- `CHAIN_SPLIT` 只包 `TechniqueChain.split` 的 try（21–44）。每次 `executeSkill` 在切片之后打一次，双重会话接触时不再 split。经 `handle` 时它嵌在 `RELEASE_REQUEST`（104–122）里。`HoldRepeat` 直接 `offer`，没有 `RELEASE_REQUEST`，但仍有 `CHAIN_SPLIT`。
- `CHAIN_ROLE_SCAN` 只包 `runSegment` 446–470 的向前角色循环，嵌在 `SEGMENT_EXECUTE`（373–615）里。每个复制各打一次；蓄力重入会再打。`collectModifiers`、`compile`、`pressCommitCost`、`copiesBefore`、`lockThisCast` 都不在这项里。`sharesDualRelease`（244–276）会在扫描中重建可释放列表，时间算在扫描里。
- `list`、`compile`、两次 `collectModifiers` 和预扣目前都混在按下处理里，没有单独指标。

缓存后要保持的事实：跳过 `split` 就不要再记 `CHAIN_SPLIT`，否则它不再表示切开链条。角色标志若改由按下计划提供，`CHAIN_ROLE_SCAN` 的区间会变短或消失；它现在量的是执行期那一次回扫，不是按下分类。两项不要合并，按下与段执行仍是两个时点。

## 文件所有权候选

- 计划结构：`TechniqueChain.java`、`ChainSegment.java`、`ChainSteps.java`、`CompiledReleaseStep.java`、`SkillChainPlan.java`
- 按下分发：`SkillUsePacket.java`。`HoldRepeat.java` 只是重复调用方
- 延后双重与动作：`CastExecutionSession.java`、`AttackBudgetWindow.java`
- 执行期扫描和闭包：`SkillSegmentRunner.java`
- 当次几何与批次：`CastFrame.java`、`ReleaseBatch.java`、`ReleaseInput.java`
- 指标注释：`PerfMetric.java`；计时点仍在 `TechniqueChain` 与 `SkillSegmentRunner`
- `SkillSegmentMath.java` 被上述各方共用，适合只改调用处
- `SkillFollowCast.java` 无调用点，先不要把它当成入口

## 未确定

登记表在运行期会不会改，这次没核对。`imprintEnchantChain` 不可达只来自上述两段代码，没有跑过。`SkillFollowCast` 未查反射。命中之后的效果、魔力账本和冷却存储没有展开。技能列表是就地修改还是整份替换，只看到闭包和会话保存的是按下时的列表引用。

本次只读，没有改文件、编译、运行游戏、测试、改表或提交。
