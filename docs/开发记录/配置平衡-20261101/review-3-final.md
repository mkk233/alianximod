先读本轮复核任务说明，并核对记录里本轮写入是否已经收束。记录确认返修 2 的 A、B、收据衔接和文档都已交回。接着按终稿和命中裁决接口核对当前代码。作者报告和接口合同已经对齐。接下来核对裁决入口、报价和接触收费的当前实现。**需返修。** 实现复核第 3/3 轮，也是最后一轮。记录确认返修 2 的 A、B、收据衔接和文档都已交回并释放写入。类型化裁决的 0 与非法边界、接触安装和领域 0 收据已经按合同分开；索敌和链上规则的标准耗魔仍进不了原来的预扣，普通报价的整数求和仍可能绕成免费。编译未跑，游戏未测。本轮之后不自动再开复核。

## 已收口

`hurt`、`hurtRoot`、`hurtConfirmed`、`confirmTracked` 的布尔值只等于 `grantsSeeds()`。有限且恰好为 0、权限通过时，`classifyHit`、`classifyRoot`、`classifyConfirmed`、`classifyRespecting` 都是 `ESTABLISHED_NO_DAMAGE`，不调用原版 `hurt`，不改无敌帧。负数、`NaN`、正无穷、负无穷是无效。`classifyRoot` 只在进入 `classifyHit` 前合并一次领域供给；已经合并过的 flow 结算走 `classifyConfirmed` 或 `classifyRespecting`，不会再合并一次，也不会把同一次正伤打两次。

普通命中、扫掠、带 flow 和不带 flow 的汇总，产种和 flow 载荷都看 `grantsSeeds()`。命中节拍和完成回调看裁决本身，不把可接续写成 `DAMAGE_ACCEPTED`。`HitVolumes` 第 186–187 行只用 `continues()` 记命中和回调，这条光束仍没有生产调用方。接触扫描在 `continues()` 时下发效果，只在 `grantsSeeds()` 时产种；权限不成立则两条都不做。领域有限 0 在成立后扣一次并留下收据，同一次停留只补状态、不产种；权限不成立不扣、不退、不下发。正伤仍先扣款，只有 `grantsSeeds()` 留下收据并产种；抛出时若已经扣血或削减吸收，收据留下。

`quotedMana` 对原始 0 会读标准 `manaCost`。领域、屏障、护环在基础伤害不是正数且定义上另有本体时仍返回占位 0，生成费和开域费不进宿主预扣。自身状态仍被预扣循环跳过。条件成功单价仍只在单价大于 0 时把段预扣收成附魔一次。资源附魔原始标价大于 0，仍走原来的读取。跳跃、造水、剑来不是链上修饰，会进入 `settledSegmentCost` 这一次报价；默认 0 仍是 0。

接触单价乘种类用 `long`，超出 int 时收成 `Integer.MAX_VALUE`，不会绕成负数。批量编译仍用精确加法和乘法，溢出直接拒绝。火伤、JSON 和同步本轮没有相关改动，不重复审查。尺寸和寿命的默认路径仍保持第 2 轮的限制：没有逐行对照全部 `pre-B`。

指南第 3 行写明待第 3 轮、待编译、游戏未测。第 9 行仍写火伤大于 10000 在扣种和挂载之前拒绝。示例和键名没有改坏。

## 仍须返修

### 1. 高。作者 A

索敌和链上规则把标准 `manaCost` 配成正数后，按键预扣仍是 0。

`SkillChainPlan.pressCommitCost` 第 91–100 行遇到链上修饰就只加后面攻击段或双重成员的费用，然后把下标跳到 `plan.resume`。修饰段自己的 `segmentManaCost` 没有加进去。单独一个索敌时，`attackSegment` 仍是 -1，两个分支都不加钱。

`SkillSegmentMath.isChainModifier` 第 94–104 行包含落点指定、锁定实体、地表定位、垂落、追踪、喷涌、交错、一分为三、双重释放、错时和命中接续。聚能的角色不是这些，但 `chainEnergyScale` 不是 1，第 107–118 行同样把它当成修饰。

触发：把 `universal_lock_target` 或 `rule_cast_anchor` 的 `manaCost` 写成正数，单独释放，或放在一记攻击前面。影响：`quotedMana` 已经会读这笔配置，预扣循环却不调用它。默认 0 的总价不变，配成正数也不扣。跳跃、造水、剑来会走到第 118 行，不受这一处影响。指南第 106 行已经写成锁定会进入原来那一次报价。

最小修复：修饰段被吃掉时，把该段的 `segmentManaCost` 加一次，不乘份数，也不要在出手时再扣一次。默认 0 的合计保持 0。领域、屏障、护环继续走占位 0。

### 2. 高。作者 A

这次能变成大额的报价，按 int 相加或乘份数后会绕成免费。

单笔标准耗魔上限是 1000000，全局魔力倍率上限是 1000，`BalanceMath.scaleMana` 得到 1000000000，仍放得进 int。`pressCommitCost` 第 77、83、97、113、118 行，`enchantOnlyCost` 第 245 行，`SkillFollowCast` 第 149–153 行，`SkillUsePacket.imprintEnchantChain` 第 1209 行，以及 `dualSequenceCost` 第 552 行的乘份数，都是普通 int 运算。三笔 1000000000 相加，或一笔 1000000000 乘 3 份，会变成负数。

`SkillUsePacket` 第 409–410 行把这个合计交给 `MagicPool.trySpend`。`MagicPool` 第 54–56 行对小于等于 0 的费用直接成功，并记下 0 元收据。`AutomaticRound` 第 129 行还有 `Math.max(0, pressCommitCost(...))`，负数会被收成 0。批量编译的 `exactAdd` / `exactMultiply` 不会这样。

触发：全局魔力倍率 1000，跳跃、造水、剑来各把 `manaCost` 写成 1000000，放在同一次按键预扣里。影响：应付约 30 亿魔力，实际不扣。接触单价乘种类不是这条路径。

最小修复：这些合计和乘份数改成与批量编译相同的精确运算，溢出就拒绝这一次释放，不要把负数交给魔力池。退款仍退当时收据上的金额。

### 3. 中。作者 B

幻剑在权限不成立时仍占用命中并上载荷。

`SwordEntityMotion` 第 114 行在裁决前就把目标写入 `sword.hit`。第 118 行的 `classifyRoot` 对有限 0 会给出无效。第 119 行仍无条件 `HitPayloads.apply`。第 120–121 行只有 `grantsSeeds()` 才产种，产种这一侧是对的。

触发：幻剑伤害倍率配成 0，命中自身、无敌或 `canHarmPlayer` 拒绝的目标。影响：裁决已经是无效，潮湿、治疗、牵引、灭火和抬升仍会套上，该目标也不会再被这把剑打一次。接触扫描第 121–130 行已经用 `continues()` 挡住这些效果。

最小修复：只有 `continues()` 才写入命中占位并 `HitPayloads.apply`。产种继续只看 `grantsSeeds()`。非主人的原版魔法伤害分支不用改。

## 记录与限制

命名门禁本轮没有重跑。仍只记录未改文件 `animation/runtime/OriginalHumanoid.java` 第 165 行的 `i`、第 202 行的 `to`。

实现复核累计 3/3 已经用尽。这份终稿之后不自动另开正确性复核，也不因为额度用尽而改判通过。统一编译仍由后续指定的执行者来跑。游戏未测。
