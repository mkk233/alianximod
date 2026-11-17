我先读任务说明，确认授权范围后再动手。任务是 C2 完整接入。我先把合同、裁决和本人已确认事实读完，再按授权路径改代码。合同和 C1 事实已经对齐。接下来只读本人范围内的源码，定位每个键的真实消费点。基线先按文件落盘，同时确认 `tideMarks()` 能不能和现有字段并存。基线已按文件备份。接下来按合同把专项声明和各处唯一伤害转换接上。A 已经给出目录句柄。声明改走这个句柄，避免另建一份注册表。C2 已按合同接入。设计仍是 2/2，实现仍是 0/3。未编译，未跑测试，未启动游戏。写入已收束，C 的写所有权已释放。基线在 `docs/开发记录/配置平衡-20261101/pre-C/`，清单见 `author-C-manifest.json`。

## 1. 键、原始默认、真实消费

声明入口是 `EnchantmentDefinitions.declareBalance(BalanceRegistry)`。A2 在封存前调用 `EnchantmentDefinitions.declareBalance(BalanceRegistry.current())`。声明走句柄的 `acceptSpecial`，不另建目录，也不登记 `damageMultiplier`、`manaCost`、`cooldownTicks`。

| 键 | 原始默认 | 消费 |
| --- | --- | --- |
| `fire_liaoyuan.radius` | `GroundFireFields.BASE_RADIUS` = 10 | 只声明一次。B 读 `GroundFireFields.configuredRadius()`，再乘原来的 `volumeScale`，传入现有 `open`。`open` 用实参，不再读半径。 |
| `fire_scorch_field.lifetimeTicks` | `SurfaceFireHosts.FIELD_LIFE` = 100 | `configuredLifetimeTicks()`，宿主出生时写入到期时刻。`open` 签名不变。 |
| `fire_scorch_field.halfExtent` | `SurfaceContactField.HALF_EXTENT` = 2 | `configuredHalfExtent()`，`collect` 使用这一次读数。前进步长 1、竖直松弛 3 仍是常量。 |
| `fire_nether_gloom.seedCount` | `GLOOM_BASE` = 5 | `NetherContract.seedCount()`，只在 `NetherSeedArbiter.grant` 里、`floorProduction` 之前替换基数。炉心产种倍率仍乘一次。 |
| `fire_nether_gloom.periodTicks` | `GLOOM_PERIOD_TICKS` = 20 | `NetherContract.periodTicks()`。不乘全局冷却。 |
| `fire_seed_sow.seedCount` | `SOW_BASE` = 10 | `ResourceEnchantCatalog.collect` 只替换种火切片。本体基数和旧固定点数仍由调用方传入。 |
| `fire_kindle.segmentDamage` | 2 | `KindlePlan.specify` 先代入原公式，再 `settle("fire_kindle", …)`。 |
| `fire_kindle.intervalTicks` | 10 | `segmentCount` 与 `specify`。边界是账本可收下的 1–12000。 |
| `fire_kindle.flatTicks` | 40 | `durationTicks`。 |
| `fire_kindle.tierTicks` | 10 | `durationTicks`。 |
| `fire_cool.radius` | `SteamTuning.RADIUS` = 3 | `SteamBurst.release` 同一次读数用于搜索盒和距离判断。击退强度仍是常量。 |
| `vine_mark.controlTicks` | 40 | `EnchantBonus.resolve` 与 `EnchantmentSnapshot.read`。反应持续时间只乘这一次。 |
| `magic_ignite.burnSeconds` | 5 | 同上两处。只改着火秒数。公共单位没有秒，这项用 `COUNT`。 |

没有声明燎原寿命、焚天半径、引燃每段燃料 10、种火容量 100、档位上限 10、焚天段上限 10、引爆系数或潮印条数。`FIELD_LIFE = 200` 仍是燎原定义为空时的原始默认。有效值没有写入 `static final`。

## 2. 每条伤害的唯一转换

`SpecialBalance.settle` 调用一次 `damageContribution`。两个倍率都是 1 时保留原 double，燃料比例不变。已配置金额不再送回 `damageContribution`。

- 燎原本休：当时炉心算出 `8 * (1 + F)`，按 `fire_liaoyuan` 转换后 `strikeOnce(..., 已配置金额, 0, true)`。
- 燎原火种段、焚天段、引燃段：挂载时把整段原始金额转换一次，写入 `fullSegmentDamage`。脉冲和读档不再乘。
- 引爆：`damage()` 仍返回原公式；`strike` 在声明效果前按 `fire_detonate` 转换，炉心标记保持已结算。
- 燎里火种：`detonate` 签名不变，内部按 `fire_liaoli` 转换合入炉心后的金额。物理刺击由 B 缩放。
- 灼地接触：固定 1 按 `fire_scorch_field` 转换，炉心标记保持已结算。
- 蒸汽：固定 2 按 `fire_cool` 转换，仍走中性帧。
- 接触伤：`damageOf` 对每个大于 0 的 `magicDamage` 按该下标术式转换一次。原始加数是 0 时跳过，避免没有伤害倍率的资源附魔抛出。
- 链段：`carriedDamage` 仍是原始合计。A 使用 `rawHostAttack`、`rawAddedDamage`、`addedSourceId`、`rawTideDamage`、`tideMarks` 各自转换。
- 新武器印记形成时：共享附加和潮印按来源转换，`Attack` 只走 `globalDamageContribution`。消费和第二刀只加已保存金额。

`FireDamage.strikeOnce`、`FireRuntimeEvents.onHurt`、`ElementAdjustment` 没有配置倍率。原版点燃后的燃烧伤害没有进入这些入口。`spread` / `sky` 旧档案不是运行时挂载点，保持原公式。

共享附加仍是第一次从 0 设为 2 的那一笔。来源记在建立时，后续同属性修正不改来源。没有这笔贡献时来源为空字符串。

## 3. NBT

燃烧新挂载写版本 3，金额已经配置过。缺版本、版本 1 和版本 2 按旧最终金额读取，不再乘。其它版本整份丢弃。版本 3 的燃料模式与版本 2 相同。

武器新印记写 `Version = 1`、已配置的 `Attack`、`Added`、`TideDamage`，`Tide` 仍是条数。缺 `Version` 时主刀附加是 `Tide * 2 + Added`。有版本但缺 `TideDamage` 时，潮印金额回退为 `Tide * 2`。主刀只加这两笔附加，横扫不加。`PlayerAttackMixin` 未改。

## 4. 费用

`magicDamage` 和 `explicitManaPrice` 仍是原始单价。潮印费用固定 2。这里不乘全局魔力，也不读伤害配置。C 没有调用 `manaCost` 或 `cooldownTicks`。

## 5. 留给 A/B 的接线

B 的 `LiaoyuanEffect` 仍读 `BASE_RADIUS`，应改为 `configuredRadius()` 后再乘体积倍率。`NetherDomainCycle` 三处仍读静态 `PERIOD_TICKS`，应改为 `periodTicks()`。幽冥产种不要再读 `seedCount()`，`grant` 已经消费。灼地签名不变。

`ContactCast` 把 `damageOf <= 0` 和 `contactDamage <= 0` 当成没有接触附加。默认倍率下正加数与旧值相同。倍率收成 0 时，这道既有门会拒绝本次释放。该文件归 A，C 未改。条数、类型名单和幽冥标记在快照里仍然单独保存。

`FireLayer.acceptableDamage` 仍要求伤害大于 0 且不超过 10000。配置把金额收成 0 或把延迟段伤推过上限时，挂载或打击会被这道门拒绝。燎原本休现在先合成炉心再进入 `strikeOnce`，合计超过 10000 会被拒绝；正常炉心读数到不了这个上限。引燃平直刻和每档刻的上界是 1200000，段数仍受 32 段上限约束。

直接调用 `resolve`、`toTag`、`spreadSpec`、`skySpec`、`KindlePlan` 或种火 `collect` 的现有测试，在目录未封存时会抛出。测试不在可写范围，也没有运行。

## 6. 命名门禁与编译

命名门禁退出码 1。只有未改的 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。本次文件没有违规，这两处没有改名。

未运行 `gradlew`。待统一编译。游戏效果未测。
