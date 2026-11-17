我先读最终任务规格，按其中的授权范围收尾。规格允许改六份文档和本批清单。我先核对哪些文件还没有 pre-docs 副本，并只读已收束的交付报告。改之前先把还没有副本的三份现有文档存进 `pre-docs`。`AGENTS.md`、流程和规则的旧副本已经在，不会覆盖。副本已留好。接下来按 A2、A3、B2 和 C2 的实际交付改指南和入口，不把待编译写成已经通过。指南和入口已按 A2、A3、B2、C2 的交付对齐。未编译，未测游戏。本次允许写入的文件全部释放。

## 改动

`docs/配置平衡指南.md` 整篇改写（约 327 行）。文首写明：**源码已交回，待统一编译，游戏未测。**

- 第 16–33 行：模板在游戏目录 `defaultconfigs/xueximod-balance.json`，世界文件在世界根目录 `serverconfig/xueximod-balance.json`。已有世界文件只读，不覆盖旧值。
- 第 41–55 行：最小 JSON。`schemaVersion` 为 `1`，全局三项默认 `1`，示例术式是 `sword_thrust` 的 `damageMultiplier`。
- 第 89–119 行：标准键和已声明 `damageMultiplier` 的术式名单，与 `BalanceCatalog` 一致。`sword_come`、`sword_return_all` 没有伤害倍率。
- 第 121–190 行：专项键目录。B 的默认值和消费者链到 `docs/开发记录/配置平衡-20261101/本人键-消费者清单.md`。C 的 13 个键单独列表。
- 第 314–322 行：五步用户实测，标明尚未做过。

`docs/底座功能点.md`：第 83 行冷却改为先读 `cooldownTicks`；第 277 行改为平衡走 JSON，toml 仍不是伤害倍率；第 546 行 `/shu list` 显示已生效值；第 559–563 行新增「平衡配置」并链到指南；第 573–574 行补了目录入口。

`docs/README.md` 第 5 行：指南和底座「平衡配置」入口。

`AGENTS.md` 第 79 行：约束仍只在流程第 4、6 节，字段和示例改指指南。派发、额度和表格流程未改。

`docs/统一开发与术式批量制作流程.md` 第 81、112、149、195 行：去掉「接口尚未落地」，改指指南里的实际签名。额度、迁表和复审句子未改。

`.cursor/rules/balance-config.mdc` 第 8、17–19 行：入口改为指南，并写明文首若是待编译或未测，就不能当成已经验证。规则仍是 `alwaysApply: true`。

`docs/开发记录/配置平衡-20261101/author-docs-manifest.json`：本次读过的报告和写过的路径。

改前副本新增三份：`pre-docs/docs/配置平衡指南.md`、`底座功能点.md`、`README.md`。更早的 `AGENTS.md`、流程副本，以及规则文件的「新建」标记没有覆盖。

## 示例与真实接口

| 指南里的写法 | 对应实现 |
| --- | --- |
| `schemaVersion: 1` | `BalanceSchema.CURRENT_VERSION` |
| `global.damageMultiplier` / `manaMultiplier` / `cooldownMultiplier` | `BalanceKeys` 同名常量，默认 `1` |
| `techniques.sword_thrust.damageMultiplier` | `BalanceCatalog.DAMAGE_HOSTS` 含 `sword_thrust`；标准伤害键默认字面量 `1` |
| `lifetimeTicks` 默认 4 | B 的刺击登记寿命 |
| `declareSpecial` + `TechniqueBalanceSpec.builder` + `numberParameter` / `integerParameter` | `BalanceRegistry` 的公开方法 |
| `event.registerBalance(...)` | `RegisterXueXiContentEvent.registerBalance` |
| `BalanceService.number` / `intValue` / `damageContribution` / `manaCost` / `cooldownTicks` / `globalDamageContribution` | `BalanceService` 的公开静态方法；伤害入参用 `RawDamage.of` |
| `heat_beam` 的 `beamLength` 6、`hitIntervalTicks` 10 | `HeatBeamEffect` 的原常量，且已在 B 的清单里 |
| 封存顺序 | `XueXiMod.commonSetup` 投递外部事件后调用 `BalanceCatalog.sealAfterContent()`：先标准键，再 `ModTechniques.declareSpecialBalances()`，再 `EnchantmentDefinitions.declareBalance(BalanceRegistry.current())`，最后 `seal` |
| 同步包号 15、协议 `"2"` | `ModNetwork` 里 `BalanceSyncPacket.register(CHANNEL, 15)` |

没有新增配置键，也没有改 Java。

## 未完成、未验证

- 统一编译没跑。游戏没测。本文不是实现复审。
- `ContactCast` 仍把接触伤害小于等于 0 当成没有接触附加。倍率收成 0 会拒绝这次释放。
- `FireLayer` 仍拒绝小于等于 0 或大于 10000 的火伤。
- 垂丝的注水、泄水、坠落和客户端布局仍读静态常量，没有键。
- `fieldSpawnCost` 没有独立键，仍按原始值乘份数。
- `/shu` 判断载具是否匹配的两处仍看结构底伤是否大于 0。
- 目录未封存时，旧测试直接调用附魔解析或燃烧计划会抛出；这些测试没跑。
- 命名门禁仍只报未改过的 `OriginalHumanoid.java` 里原有两处短名字。
- 完整默认 JSON 和 `contractText()` 要等一次成功加载才看得到。这次没有加载。
