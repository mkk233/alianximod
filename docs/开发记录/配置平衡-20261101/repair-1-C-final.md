我先读返修任务说明，确认这次允许改的范围后再动手。复核写明两件事：火伤要先结算再决定是否扣种，接触资格要另给一个未配置的原始合计。我先把这些消费点对上。先把这次要改的文件按当前字节存进返修前快照，再改结算顺序和 0 贡献的接受路径。命名门禁只剩未改过的 `OriginalHumanoid`。正在写清单和返修报告。# 配置平衡 repair-1 C 最终报告

作者 C，模型 grok-4.7-high-fast。本单只落实 review-1 问题 2，以及问题 1 里交给 C 的 `rawDamageOf`。设计额度仍是 2/2，未再做正式复审。未编译、未测试、未启动游戏、未改表、未提交。A、B 的源文件未读中间改动、未写入。最初 `pre-C/` 保留。本次改动前的字节在 `pre-repair-1/C/`。写入已全部释放。

## 校验在扣种前

正数原公式先走 `SpecialBalance.settleConfigured`，得到已配置金额后，再用 `FireLayer.acceptableSettled` 判断。通过之后才做不可逆提交。

- 引爆：余额大于 0 时，原公式非有限或小于等于 0 直接未就绪。合法正数原公式先结算；结果非有限、为负或大于 10000 时，在 `claimEffect` 和 `FireSeedState.draw` 之前返回未就绪。已接受的金额（含配置产生的 0）才占效果并扣完全部余额。余额小于等于 0 仍先占效果再返回空余额，不结算、不扣种。
- 燎里：`holdStrike` 在 `consumePrepared` 提交前结算。原公式非有限或小于等于 0、以及已配置金额不能接受时，谓词返回 false，种子不扣。接受后把已配置金额记在 `ChosenStrike`，引爆只打这一次，不再结算第二次。
- 引燃、燎原附着、焚天：仍在 `KindlePlan.specify`、`FireConsumerPlans.spreadSpec`、`skySpec` 里先结算，再交给 `SharedSpec.of`。超限或非法金额得到 empty，`FireConsumption` 不挂载，段燃料尚未支付。配置产生的 0 可以挂上。
- 灼地初见：接触策略和伤害许可仍先检查。已配置金额不能接受时，不写入 `seenTargets`，不 `claimFirst`，不产初见火种。金额可接受后才占初见并产种。周期产种仍只发生在已经记过初见的目标上。
- 燎原本体：炉心仍在 `payBodies` 当时采样，公式仍是 `8 * (1 + F)`，`furnaceSettled` 仍为 true，火种基数仍是 0。不能接受时不推进本体间隔，下一拍按原间隔条件重试。这里没有火种扣款。
- 蒸汽：伤害类型解析失败则整次释放返回。已配置金额超限、非有限或为负时，在击退前返回。敌对和距离条件仍在击退前检查。

`BurnComponent` 的基础值和预付 `seal` 仍要求原分量大于 0。原公式本身不是一笔正数分量时，不会被配置 0 放行。

## 0 贡献贯穿

`FireLayer.acceptableSettled` 接受有限的 0，并拒绝负数、非有限数和大于 10000 的金额。`acceptableDamage` 仍只接受大于 0 的原分量。

- `SharedSpec.of` 接受已配置 0，挂载可以成立。
- 共享脉冲按已锁定的足额段分摊。分摊结果为 0 时仍生成脉冲。
- `FireDamage.strikeOnce` 在目标仍存活、位于服务端且伤害类型能解析后，对 float 金额 0 返回 true，不调用 `hurt`。正数金额仍走原来的一次 `hurt`，并保持 `furnaceSettled`。
- 引爆因此把已接受的 0 记为 `STRUCK`。燎原本体间隔推进一次。灼地初见照常占位并产种。蒸汽在敌对条件通过且伤害类型存在时仍击退，正数路径才进入中性元素帧并 `hurt`。
- 预付恢复和共享层恢复接受存档里的 0，负数、非有限数和超过 10000 的记录仍整轨丢弃。读档不乘倍率。

两个倍率都是 1 时，`settle` 仍在配置浮点与原 double 的 float 相等时保留原 double。已配置金额不会再送回 `damageContribution`。

## 上限拒绝与存档兼容

超过 `FireLimits.DAMAGE_CAP`（10000）的已配置金额按未就绪拒绝，发生在扣种、占效果、占初见和挂载之前。换算抛出时 `settleConfigured` 返回 empty，同样按未就绪处理，不扣种。

燃烧版本没有改。缺失版本以及版本 1、2 仍是旧的最终金额，版本 3 仍表示挂载时已经缩放过。四种版本的读取都不追乘。未知其他版本仍丢弃。武器附魔的 `Version` 本单没有改。

## 公共 raw 方法

```java
public static float rawDamageOf(Skill skill, List<Integer> indexes)
```

定义在 `EnchantmentSnapshot`。参数类型和顺序与 `damageOf` 相同。返回各下标 `magicDamage` 原值之和，不调用 `BalanceService`。`damageOf` 仍返回逐槽已配置金额。费用公式未改。A 的 `ContactCast` 不在本单写入范围，资格门要由 A 改读这个方法；命中仍用已配置金额。

## 改动路径

- `shu/technique/enchant/SpecialBalance.java`
- `shu/technique/enchant/EnchantmentSnapshot.java`
- `shu/technique/release/fire/FireLayer.java`
- `shu/technique/release/fire/FireDamage.java`
- `shu/technique/release/fire/SharedSpec.java`
- `shu/technique/release/fire/BurnLedger.java`
- `shu/technique/release/fire/DetonatePlan.java`
- `shu/technique/release/fire/FireConsumption.java`
- `shu/technique/release/fire/SteamBurst.java`
- `shu/technique/release/firecast/GroundFireFields.java`
- `shu/technique/release/firecast/SurfaceFireHosts.java`

以上路径均相对 `src/main/java/com/bslx/XueXiMod/`。`PlayerAttackMixin` 未改。

## 未解决与阻塞

- A 的接触施法结构门仍要改读 `rawDamageOf`。该方法已经公开，本单不能写 A 的文件。
- 旧预付档案 `FireConsumerPlans.spread` / `sky` 仍不缩放，`BurnLedger.seal` 仍要求合出来的原分量为正。运行时燎原和焚天挂载走 `spreadSpec` / `skySpec`。
- 命名门禁退出码 1，只有未改文件 `animation/runtime/OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。这两处没有改名。
- 依赖已密封配置目录的测试未改、未跑。

本单没有剩余的 C 路径阻塞。统一编译待后续对收束后的源码执行 `compileJava --offline --no-daemon`。本单没有跑这条命令，也没有启动游戏。

全部 C 路径写入已关闭，所有权已释放。
