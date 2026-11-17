我先读第二轮复核任务说明，确认授权范围和前置状态后再审。记录确认返修 A、B、C 和文档都已收束。这一轮核对第 1 轮问题是否闭环，并补上 B 的参数消费和 0 费用、0 冷却。返修报告已齐。接下来核对 0 贡献在命中、火伤和费用上的上下游，避免只看入口就判通过。**需返修。** 实现复核第 2/3 轮。记录已确认本轮源码和文档写入收束，可以复核。第 1 轮的接触资格、火伤先结算再扣资源、领域/屏障溢出拒绝，以及指南里的自相矛盾，已经收口。合法 0 贡献仍会在普通命中和接触上下游被当成正伤产种，或被当成没有接触而整段丢掉。原本免费、单价就是标准 `manaCost` 的规则和动作，显式配成正数后报价仍不扣。编译未跑，游戏未测。

## 已收口

接触释放资格已改看 `EnchantmentSnapshot.rawDamageOf`。原始加数大于 0 时，配置金额为 0 不再在扣费前拒绝。快照里打出去的仍是已配置的 `contactDamage`。`rawDamageOf` 不参与实际伤害。见 `network/ContactCast.java` 第 36–37、45–46、112–118 行。

火伤先做 `settleConfigured`，再用 `FireLayer.acceptableSettled`。有限的 0 可以留下。负数、非有限数、大于 10000 的已配置金额，在 `claimEffect`、扣种和挂载之前返回。引爆在此之后才 `draw`。`FireDamage.strikeOnce` 对恰好为 0 的有限值，在存活、服务端、伤害类型成立后直接返回 true，不调用原版 `hurt`。`Outcome.struck()` 没有调用方，这条 true 不会再产一笔种。灼地先过接触策略和 `DamagePermission`。贴地火场收集目标时已经滤过权限。蒸汽击退仍先过敌对条件。第 1 轮的「先扣种、后被 0 或上限丢掉」没有留下。

`DomainService.exactBill` 与 `BarrierService.exactBill` 使用 `Math.multiplyExact`。单价或份数为负、或乘积超出 int 时返回空账单，并提示没有释放。领域在几何、扣款和激活列表之前拒绝。屏障的 `planShifts` 只填写局部列表；溢出返回发生在 `trySpend`、生成实体和 `setPos` 之前。真实 0 仍是 0。预扣路径不在这里再扣。随后生成失败仍按这次账单原额退回。

指南第 3 行写明待第二轮复核、待编译、游戏未测。第 8–10 行和第 100 行写明 `hurt` 的 true 只是可接续，并点名下游仍可能当成正伤。不再把「0 不会拆掉领域」写成已经成立。

基础冷却为 0 的术式，显式配成正数会生效。`BalanceDisplay.cooldown` 只在原始刻数小于 0 时直接返回；原始 0 仍读快照。`SkillSegmentMath.configuredBaseCooldown` 被 `ChainSteps`、`SkillUsePacket`、`SkillFollowCast`、`BatchReleaseCompiler` 用作这一发的冷却。冷却范围允许在默认值为 0 时写成正数。`scaleCooldown` 只在合计仍不大于 0 时原样返回。段间空隙对底伤为 0 的前一段仍返回 0，这是原有空隙条件，不是这条标准键的施放冷却。

本批 B 的读取没有把已接受的配置 0 换回正伤。`SpecialBalance` 在键已声明时返回快照值，未封存或未声明时才退回调用点原始值。速度、寿命、尺寸的正数下界会拒绝「默认不是 0、配置写成 0」，回退的是该键自己的默认，不是再乘一次。`ListedRelease` 只在原始速度不大于 0、厚度不大于 0 或寿命不大于 1 时跳过读取；清单里已声明的这些默认都是正数，会读到。水系、领域、水幕、水环的定义 getter 各读一次。幻剑在生成时 `contributed` 一次并保存，读档抄 `Damage`，不再乘。领域光环加的是已经配置过的附加；配置结果为 0 时不加回原始附加。燎里物理刺击只在原始刺击为正时贡献一次，倍率 0 得到 0，返回值被忽略，这一处本身不产种。未配置时行为保持调用点原始值。没有发现声明了却没有消费者的新增键。本轮没有逐行对照全部 `pre-B`，也没有重审第 1 轮已核对且本轮未改的 JSON 与同步。

## 仍须返修

### 1. 高。作者 A

合法 0，以及负数和负无穷，在权限通过时被记成正伤并产种。

`TechniqueHits.hurt` 第 52–53 行：`amount <= 0f` 时返回 `classifyNonPositive(...).continues()`。`classifyRespecting` 第 99–100 行把同一笔交给 `classifyNonPositive`。后者第 148–152 行只区分权限：不允许则无效，允许则 `ESTABLISHED_NO_DAMAGE`。这个结论的 `continues()` 为 true，`grantsSeeds()` 为 false。负数和负无穷走这条。`NaN` 和正无穷不满足 `<= 0`，仍进原版 `hurt`，没有被收成合法 0。

`hurtConfirmed` 第 125 行对 `amount <= 0f` 直接返回 false。因此同一笔 0，在不同账本上结果相反。

普通命中体的 `respectLanding` 默认 false，只有批量槽会写成 true。`TechniqueHitSettle` 第 186–192 行在这条默认路径上把 `hurtRoot` 的 true 写成 `DAMAGE_ACCEPTED`，再交给 `ResourceSettlement` 第 27–30 行。`grantsSeeds()` 为 true 时 `FireSeedGrant.deposit`。触发：任意带伤害倍率的普通命中，倍率配成 0，目标合法。影响：没有正伤，仍然产种，接续链把这一拍当成伤害已被接受。

`ReleaseBatch.respectLanding()` 第 298–299 行没有调用方，字段保持 false。没有 flow 载荷时，`flush` 第 428–433 行同样把 true 写成 `DAMAGE_ACCEPTED`，并用这个布尔调用 `retireSeeds`。有 flow 载荷的分支走 `hurtConfirmed`，0 不产种。触发：一分为三这类汇总、且没有 flow 载荷，合计已是配置 0。影响：汇总结算产种。

`HitVolumes.beam` 第 185–194 行也会把 true 当成命中并跑 `onHit`，但仓库里没有生产调用方。不单列必须修改；修布尔合同时不要只改有调用方的两处、留下这个方法的含义继续分叉。

最小修复：只有有限且恰好为 0 的金额，在权限通过时记 `ESTABLISHED_NO_DAMAGE`。负数和非有限值记无效，`hurt` / `hurtRoot` 返回 false。`TechniqueHitSettle` 与 `ReleaseBatch.flush` 的非 `classifyRespecting` 分支按 `grantsSeeds()` 产种，不要把 `continues()` 写成 `DAMAGE_ACCEPTED`。权限失败、自身、旁观、无敌和 `canHarmPlayer` 拒绝保持现状。

### 2. 高。作者 B

幻剑把可接续当成产种。

`SwordEntity` 第 140 行把 `SpecialBalance.contributed("phantom_sword", weaponDamage)` 存进 `copiedDamage`。倍率 0 时这里是 0，不会退回武器原伤。`SwordEntityMotion` 第 117–121 行对这个值调用 `hurtRoot`，返回 true 就 `FireSeedGrant.deposit`。`HitPayloads.apply` 写在 `landed` 判断之前，失败命中原来也会走，不是这次 0 新加的。

触发：幻剑伤害倍率配成 0，剑命中合法目标。影响：不打出正伤，仍按剑上的火种点数产种。

最小修复：只在裁决 `grantsSeeds()` 时 deposit。有限 0 可以保留命中占位和载荷接续，不产种。

### 3. 高。作者 B

合法的配置 0 接触被当成没有接触。

`ContactEnchantState.open` 第 44–45 行在 `contactDamage <= 0f` 时返回 null，扫描器不安装。领域收集 `DomainContactBilling.collect` 第 34 行、`eligible` 第 167 行同样直接返回。非领域宿主（水幕、水环、水牢、场地、灼地打开的接触）因此没有效果。领域即使放开收集，`apply` 第 87–90 行和第 101–120 行仍要 `hurtConfirmed` 或 `confirmTracked` 为 true 才 `deliverPayload`；这两条对 0 都返回 false，所以潮湿等接触效果不会下发，费用路径会退款。

`scan` 第 118–126 行在 `hurt` 为 true 时既 `applyEffects`，又 `FireSeedGrant.deposit`。当前因为 `open` 直接返回，配置 0 到不了这里。只放开 `open`、不改产种条件，屏障和守卫接触会开始为 0 伤产种。

触发：接触附魔原始加数大于 0，接触伤害倍率配成 0，释放已经按第 1 轮修复放行。影响：接触状态和附魔效果整段丢失；修复安装处而不分产种，则会变成问题 1 的接触版。

最小修复：有限的配置 0，在原始接触资格已经成立时仍安装接触，并下发接续效果。产种只跟随 `grantsSeeds()`。权限失败不安装、不产种。领域的 0 金额不要先扣再因为正伤确认失败而退回。

### 4. 中。作者 A

原本免费、实际单价就是标准 `manaCost` 的术式，配成正数后不扣款。

`SkillSegmentMath.quotedMana` 第 254–262 行在 `manaCostOf <= 0` 时直接返回 0，不调用 `BalanceDisplay.mana`。后者第 46–53 行对原始 0 是会读覆盖值的。

这个提前返回对领域、水幕、水环是对的。它们的 `explicitManaCost` 为 0，宿主预扣必须保持 0。真正的开域费和生成费走 `configuredBodyUnit` 第 273–280 行，以及 `DomainService` / `BarrierService` 里对应的 `SpecialBalance.mana`。同一笔不会扣两次。

规则的 `Technique.manaCost()` 因 `ReleaseKind.RULE` 返回 0，没有另一把费用键。`movement_leap`、`magic_create_water`、`universal_lock_target`、`sword_come` 的结构耗魔也是 0，报价就是这笔 `manaCost`。把标准键写成正数后，`segmentManaCost`、批量宿主报价和附魔报价仍是 0。收费次数没有增加，金额也没有变成配置值。

最小修复：只对领域、屏障、护环这三类占位 0 跳过读取。其余原始单价为 0 的术式照走 `BalanceDisplay.mana`。扣款仍是原来那一次报价，不新加扣费点。指南第 96 行现在写的是「报价大于 0 的单价」，与这处代码一致，但与已拍板的「原本为 0 也允许显式配置」不一致；代码改完后这句要改成同一含义。

## 记录与限制

命名门禁本轮没有重跑。仍只记录未改文件 `animation/runtime/OriginalHumanoid.java` 第 165 行的 `i`、第 202 行的 `to`。本批文件没有新的命名问题要扩修。

`compileJava` 未执行。游戏未测。设计额度 2/2 已用尽，本轮没有做新的架构裁决，也没有把首版范围扩成「每个内部常量都可配」。JSON 与客户端同步沿用第 1 轮结论，本轮返修没有改这些文件。
