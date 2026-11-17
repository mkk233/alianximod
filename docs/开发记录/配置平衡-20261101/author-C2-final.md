# C2 最终报告

模型 grok-4.7-high-fast。设计 2/2 已用尽，实现仍 0/3，本单不追加复审。未编译，未跑测试，未启动游戏，未改制作表，未改 B/A 源码。全部写入已收束，C 的写所有权释放。

基线在 `docs/开发记录/配置平衡-20261101/pre-C/`，清单见 `author-C-manifest.json`。新文件 `SpecialBalance.java` 没有旧字节。

## 1. 键 → 原始默认来源 → 真实消费

声明入口：`EnchantmentDefinitions.declareBalance(BalanceRegistry)`。A2 在封存前调用 `EnchantmentDefinitions.declareBalance(BalanceRegistry.current())`。声明走句柄的 `acceptSpecial`，不另建目录，不登记 `damageMultiplier`、`manaCost`、`cooldownTicks`。

| 键 | 原始默认 | 消费 |
| --- | --- | --- |
| `fire_liaoyuan.radius` | `GroundFireFields.BASE_RADIUS` = 10 | 只声明一次。B 读 `GroundFireFields.configuredRadius()`，再乘原来的 `volumeScale`，传入现有 `open`。`open` 用实参，不再读半径。 |
| `fire_scorch_field.lifetimeTicks` | `SurfaceFireHosts.FIELD_LIFE` = 100 | `configuredLifetimeTicks()`，宿主出生时写入到期时刻。`open` 签名不变。 |
| `fire_scorch_field.halfExtent` | `SurfaceContactField.HALF_EXTENT` = 2 | `configuredHalfExtent()`，`collect` 每次用这一次读数。前进步长 1、竖直松弛 3 仍是常量。 |
| `fire_nether_gloom.seedCount` | `ResourceEnchantNumbers.GLOOM_BASE` = 5 | `NetherContract.seedCount()`，只在 `NetherSeedArbiter.grant` 里、`FireSeedMath.floorProduction` 之前替换基数。炉心产种倍率仍乘一次。 |
| `fire_nether_gloom.periodTicks` | `ResourceEnchantNumbers.GLOOM_PERIOD_TICKS` = 20 | `NetherContract.periodTicks()`。不乘全局冷却。 |
| `fire_seed_sow.seedCount` | `ResourceEnchantNumbers.SOW_BASE` = 10 | `ResourceEnchantCatalog.collect` 只替换种火切片的基数。本体基数和旧固定点数仍由调用方传入。 |
| `fire_kindle.segmentDamage` | `KINDLE_SEGMENT_DAMAGE` = 2 | `KindlePlan.specify` 先读出，再 `FireLayer.combine`，再 `SpecialBalance.settle("fire_kindle", …)`。 |
| `fire_kindle.intervalTicks` | `KINDLE_INTERVAL_TICKS` = 10 | `segmentCount` 与 `specify`。边界是账本能收下的 1–12000。 |
| `fire_kindle.flatTicks` | `KINDLE_FLAT_TICKS` = 40 | `durationTicks`。 |
| `fire_kindle.tierTicks` | `KINDLE_TIER_TICKS` = 10 | `durationTicks`。 |
| `fire_cool.radius` | `SteamTuning.RADIUS` = 3 | `SteamBurst.release` 同一次读数用于搜索盒和 `reaches`。击退强度仍是常量。 |
| `vine_mark.controlTicks` | `SpecialBalance.VINE_CONTROL_TICKS` = 40 | `EnchantBonus.resolve` 与 `EnchantmentSnapshot.read`，经 `SpecialBalance.controlTicks`。反应持续时间只乘这一次。 |
| `magic_ignite.burnSeconds` | `SpecialBalance.IGNITE_BURN_SECONDS` = 5 | 同上两处的 `SpecialBalance.burnSeconds`。只改着火秒数。单位用 `COUNT`，因为公共单位里没有秒。 |

没有声明的项：燎原寿命、焚天半径、引燃每段燃料 10、种火容量 100、档位上限 10、焚天段上限 10、引爆系数 0.2、潮印条数、炉心公式。`fire_liaoyuan.lifetimeTicks` 仍由 B 声明；C 的 `FIELD_LIFE = 200` 继续作为定义为空时的同一原始默认。

有效值没有写入 `static final`。上述常量仍是代码默认。玩法在施放、挂载、出生或结算时读快照。

## 2. 每条伤害的唯一转换

公共转换是 `SpecialBalance.settle`。它调用一次 `damageContribution(术式, RawDamage)`。两个倍率都是 1 时，结果 float 与 `(float) raw` 相同，方法把原 double 交回去，燃料比例保持原公式。倍率不是 1 时，保存已配置的 float。已配置金额不再送回 `damageContribution`。

| 贡献 | 原公式收束 | 唯一转换 | 之后 |
| --- | --- | --- | --- |
| 燎原本休 | 当时炉心 F，与 `FireRuntimeEvents` 相同的在线读数；原始金额 `8 * (1 + F)` | `payBodies` 里按 `fire_liaoyuan` | `strikeOnce(目标, 施法者, 已配置金额, 0, true)` |
| 燎原火种段 | `spreadSpec` 的 `4 * (1 + 0.1 * 档 + F)` | 写入 `fullSegmentDamage` 之前按 `fire_liaoyuan` | 燃料比例只切分这份金额。脉冲仍是 `strikeOnce(..., damage, 0, true)` |
| 焚天段 | `skySpec` 的 `2 * (1.2^档 + F)`，档上限 10，间隔 4，最多 10 段 | 写入前按 `fire_fentian` | 同上，脉冲不再乘 |
| 引燃段 | 先替换段伤、间隔、平直刻、每档刻，再 `段伤 * (1 + F)` | `specify` 里按 `fire_kindle` | 每段燃料仍是 10 |
| 引爆 | `damage()` 仍返回 `0.2 * 余额 * (1 + F)` | `strike` 里、声明效果之前按 `fire_detonate` | `furnaceSettled` 仍为 true，火基数仍是 `0.2 * 余额` |
| 燎里火种 | `detonate` 内 `FireLayer.combine` | 按 `fire_liaoli` 一次 | 签名不变。B 缩放物理刺击 |
| 灼地接触 | 固定 1，炉心已结算 | 按 `fire_scorch_field` | `strikeOnce(..., 已配置金额, 1, true)` |
| 主动冷却蒸汽 | 固定 2 | 按 `fire_cool` | 仍走中性帧 `hurt` |
| 接触附魔 | 每个下标的 `magicDamage` | `damageOf` 对大于 0 的加数按该下标术式各一次 | 写入 `contactDamage`。原始加数是 0 时直接跳过，避免没有伤害倍率的资源附魔抛出 |
| 链段宿主 / 共享附加 / 潮印 | `EnchantBonus` 保持旧公式 | C 不缩放 `carriedDamage` | A 用下面的只读接口各缩放一次 |
| 新武器印记 | 共享附加与潮印用上面的原始值；`Attack` 是印记武器数 | 形成时：`damageContribution(addedSourceId, rawAddedDamage)`、`damageContribution("tide_mark", rawTideDamage)`、`globalDamageContribution(rawHostAttack)` | 消费和无潮印第二刀只加已保存金额 |

`FireDamage.strikeOnce`、`FireRuntimeEvents.onHurt`、`ElementAdjustment`、`FireHurtBoundary` 没有配置倍率。原版点燃后的燃烧伤害没有进入这些入口。

`FireConsumerPlans.spread` 与 `sky` 仍是旧预付档案，运行时挂载不走它们，因此没有接入配置。

## 3. 附魔金额与武器 NBT

`EnchantBonus` 保留首次把共享附加额从 0 设为 2 的循环顺序、同属性 bump 和层数。`addedSourceId` 记这次建立时的正式编号，后续 bump 不改来源。没有这笔贡献时来源是空字符串，金额是 0。

给 A 的接口：

- `float rawHostAttack()`
- `float rawAddedDamage()`
- `String addedSourceId()`
- `float rawTideDamage()`，即原来的 `tideMarks * 2`
- `int tideMarks()`
- `float carriedDamage(float)` 仍是四项原始合计

接触公式仍按下标累加，没有改成第一来源独占。

武器根键仍是 `xueximod_weapon_enchant`。新印记写 `Version = 1`、已配置的 `Attack`、`Added`、`TideDamage`，以及来源 `AddedSource`。`Tide` 仍是条数。

- 缺 `Version`：主刀附加是 `Tide * 2 + Added`，两笔都按旧最终金额使用。
- 有 `Version` 但缺 `TideDamage`：潮印金额回退为 `Tide * 2`，不再缩放。
- 并进原版主刀只加潮印金额和 `Added`。横扫注入没有加上这笔。
- 无潮印第二刀仍是保存的 `Attack + Added`。形成新印记时 `Attack` 已走 `globalDamageContribution`。消费时不再缩放，也不对原版第一刀调用它。

`PlayerAttackMixin` 未改。主刀已经调用 `includeTideFlat`，横扫只合并领域供给。

## 4. 燃烧 NBT

`FireLimits.CONFIGURED_VERSION = 3`。新挂载写出的 `full` 和段伤已经是配置后的金额。

- 缺版本（读出 0）、版本 1、版本 2：按旧最终金额解释，读取不乘。
- 版本 3：接受，读取不乘。
- 其它版本整份丢弃。
- 版本 3 的燃料模式与版本 2 相同：只有明确写成共享的轨道按段扣余额。缺版本和版本 1 视为预付。

读档后再写出会标成版本 3，金额不再乘一次。

## 5. 费用与冷却

`EnchantmentDefinitions.magicDamage` 和 `ResourceEnchantCatalog.explicitManaPrice` 仍返回原始单价。潮印费用固定 2，其它旧附魔仍是 `ceil(2 * 反应伤害倍率)`。这里不乘全局魔力，也不读伤害配置。C 没有调用 `manaCost` 或 `cooldownTicks`，也没有自己的扣款点。

## 6. 跨作者接口

A2：

```java
EnchantmentDefinitions.declareBalance(BalanceRegistry.current());
```

封存前调用一次。真实火伤和进入金额公式的旧五附魔（含 `tide_mark`）需要 A 已声明的标准 `damageMultiplier`，否则 `damageContribution` 抛出。`globalDamageContribution(double)` 与分侧重载已经按合同存在；C 只在新印记形成时使用。

A 的链段组装仍是：宿主 `damageContribution(hostId, rawHostAttack() + weaponBonus)`，共享附加 `damageContribution(addedSourceId, rawAddedDamage())`，潮印 `damageContribution("tide_mark", rawTideDamage())`。没有贡献的项为 0。合计之后不要再调用 `damageContribution`。

B：

- `LiaoyuanEffect` 改读 `GroundFireFields.configuredRadius()`，能量缩放仍只乘一次，然后调用现有 `open`。当前该效果仍读 `BASE_RADIUS`，这次没有改 B 的文件。
- `NetherDomainCycle` 的开窗取模、窗口号、首轮等待改读 `NetherContract.periodTicks()`。当前三处仍读静态 `PERIOD_TICKS`。
- 幽冥产种不要再读 `seedCount()`。`grant` 已经消费。
- 灼地 `open` 签名不变。展示若要有效寿命或半幅，读 `SurfaceFireHosts.configuredLifetimeTicks()` 与 `SurfaceContactField.configuredHalfExtent()`，不要再声明同键。
- 燎里物理伤由 B 按 `fire_liaoli` 缩放后送入 `hurtRoot`。火种伤已在 `detonate` 内缩放。

`ContactCast` 用 `damageOf <= 0` 和 `contactDamage <= 0` 拒绝接触释放。默认倍率下正加数与旧值相同。伤害倍率收成 0 时，这一道既有门会把本次释放判成没有附加伤害。该文件归 A，C 未改。条数、类型名单和幽冥标记在快照对象里仍然独立保存；门本身在快照交给接触结算之前。

## 7. 默认行为与既有门

未改配置时，字面默认等于改前常量。倍率 1 的火伤转换保留原 double，再由原来的 `(float)` 收窄进入 `hurt`。控制 40 与着火 5 仍先乘反应持续时间。

既有正伤害门仍在：`FireLayer.acceptableDamage` 要求大于 0 且不超过 10000，`SharedSpec` 和 `strikeOnce` 使用它。配置把金额收成 0，或把延迟段伤推过 10000 时，这笔挂载或打击会被这道门拒绝。默认倍率不触发。

燎原本休现在先合成 `8 * (1 + F)` 再进入 `strikeOnce`。合计超过 10000 时会被拒绝。旧路径先放行 8，炉心在受击事件里追加。正常炉心读数到不了这个上限。

`flatTicks` / `tierTicks` 的上界是 1200000。段数仍受 `FireLimits.SEGMENT_CAP = 32` 约束，过大的持续会让 `SharedSpec` 拒绝挂载。

直接调用 `EnchantBonus.resolve`、`WeaponEnchant.toTag`、`spreadSpec`、`skySpec`、`KindlePlan` 或种火 `collect` 的现有测试，在目录未封存、对应键未声明时会抛出。测试不在本次可写范围，也没有运行。

## 8. 命名门禁、编译、所有权

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。仅有既有两处，都在未改的 `animation/runtime/OriginalHumanoid.java`：165 行 `i`，202 行 `to`。本次文件没有违规，这两处没有顺手改名。

未运行 `gradlew`、测试和游戏。待统一编译。游戏效果未测。

写入已收束。C 不再持有这些文件的写所有权。
