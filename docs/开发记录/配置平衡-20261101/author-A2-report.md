# 作者 A2 最终报告

模型：Cursor CLI grok-4.7-high-fast。设计额度 2/2 已用尽，本阶段没有做正式复审，也没有改架构。未编译、未启动游戏、未提交。

## 已完成

- 同一键再次声明直接抛 `BalanceConflictException`，即使默认值相同。不同键仍合并。标准键已存在时跳过，不覆盖 B/C 专项键。
- `BalanceRegistry.current()` 是唯一目录。`acceptSpecial` 与静态 `declareSpecial` 写同一份表。
- `BalanceService.globalDamageContribution(double)` 与 `globalDamageContribution(BalanceSide, double)` 只乘全局伤害一次。
- 内容事件之后：先补标准键，再 `ModTechniques.declareSpecialBalances()`，再 `EnchantmentDefinitions.declareBalance(BalanceRegistry.current())`，最后 `seal`。
- 世界配置：缺失时从 `defaultconfigs/xueximod-balance.json` 复制到世界 `serverconfig/xueximod-balance.json`。模板缺失则按封存默认生成。已有世界文件只读。坏 JSON 或未知 schema 保留文件并安装代码默认快照。单字段非法只警告并回退。
- 服务端启动安装并绑定 SERVER。玩家登录发送已接受的覆盖值（包号 15，协议仍是 `"2"`）。客户端安装 CLIENT。断线只清 CLIENT。服务端停止只清 SERVER。
- 客户端挂钩带 `Dist.CLIENT`，通用类不引用它们。
- 段伤害按宿主、共享附加来源、潮印各缩放一次。无附魔时整段宿主金额缩放一次。没有伤害键时保持原金额。
- 领域光环在 `DomainAuraResolver` 按光环附魔来源缩放一次后，再与已经配置的基础金额相加。
- 魔力：报价原始值为 0 时不读 `manaCost` 冻结值。领域开域费在多重份额里按 `manaCost` 缩放后乘份数。水幕、水环按 `spawnManaCost`。水牢成功单价仍排除在预扣之外。退款路径没有改。
- 冷却在作者 A 的网络和两处界面里先走 `cooldownTicks`，再做法杖 `scaleCooldown` 或反应倍率。
- `Technique.baseDamage`、`manaCost()`、`getBaseCooldown()` 仍是原始 getter。
- 外部 `registerBalance` 委托同一目录。

## 未覆盖

- `TechniqueChain.gapBefore` 仍用原始冷却，随后 `SkillUsePacket` 只做法杖缩放。该类不在 A2 写范围内，段间隔还没有全局冷却。
- 展示仍读原始冷却或原始伤害的文件：`Skill.getTotalBaseCooldown`、`SkillStoneData.imprint`、`SkillStoneItem`、`SkillBookItem`、`BoundSkillTooltip`、`ShuManagementScreen`、`ShuCommands`。战斗扣费和段伤害不走这些入口。
- `fieldSpawnCost` 默认 0，没有已批准的独立键。多重份额和 `SkillChainPlan.fieldSpawnExtra` 仍按原始值乘份数。
- 开域费、水幕水环生成费、水牢成功单价、自身状态、维护费、接触费的实际扣款在 B 的类里。A 只缩放自己文件里的报价和多重份额。
- 火伤、武器印记、剑实体物品伤害在 C/B 的文件里。A 提供读取入口，不改那些类。
- 内置术式基础伤害大于 0、却不在伤害表也不在排除表时，封存会打一条警告并且不发伤害键。这次没有启动，警告要等第一次加载才能看见。
- `CastFrame`、`ReleaseBatch`、`FlowRoundLedger`、`TechniqueHits`、`ReleaseInput`、`ReleaseRegistry`、`Technique`、`TechniqueManager`、`TechniqueIds` 和 `release/entity` 没有改。它们保存调用方已经给出的金额，getter 保持原始值。

## 配置键

标准键由 A 声明。原始默认来自计算器，封存时读一次，不调用 `BalanceService`。

| 键 | 原始默认 | A 的消费处 |
| --- | --- | --- |
| 全局 `damageMultiplier` / `manaMultiplier` / `cooldownMultiplier` | 字面量 1 | 只在 `damageContribution`、`globalDamageContribution`、`manaCost`、`cooldownTicks` 内各乘一次 |
| `cooldownTicks` | `Technique.getBaseCooldown()` | `SkillSegmentMath.configuredBaseCooldown`，然后才是反应或法杖缩放。界面：`SpellAssemblyScreen`、`ImprintTreeView` |
| `manaCost` | `Technique.manaCost()` | 报价原始值大于 0 时的 `quotedMana`；领域本体用 `initialCost` 读这一键。水牢预扣不含宿主这一笔 |
| `damageMultiplier` | 字面量 1 | `SkillSegmentMath.configuredSegmentDamage`、`DomainAuraResolver`、两处界面。剑来和万剑归宗不声明 |

声明了 `damageMultiplier` 的编号：三段斩击、冲刺、奥术飞弹、测试持续、热光束、空间斩、寄生种子、潮斩、石锤、金针、火球、火爆、三道水刃、陨星、冲击环、火花、幻剑、水牢、燎原、焚天、燎里、灼地、引燃、引爆、冷却、潮印、藤印、沉刃、锐印、点燃。

A 读取但由 B 声明的键：`spawnManaCost`。水幕和水环本体单价走它；键不存在时用 `spawnCost()` 原值。

A 不声明也不消费：`controlTicks`、`burnSeconds`、`periodTicks`、`intervalTicks`、`flatTicks`、`tierTicks`、`halfExtent`，以及寿命、速度、半径、卸伤、维护费等专项键。`BalanceKeys` 里的建议常量没有因此被登记。

## B/C 接入

已按合同调用，各一次，顺序是标准键、B、C、封存。句柄是 `BalanceRegistry.current()`。C 可调用静态 `declareSpecial` 或实例 `acceptSpecial`，都会进同一份目录。

统一编译还依赖这些尚未由 A 实现的方法：

- `ModTechniques.declareSpecialBalances()`
- `EnchantmentDefinitions.declareBalance(BalanceRegistry)`
- `EnchantBonus.rawHostAttack()`、`rawAddedDamage()`、`addedSourceId()`、`rawTideDamage()`

`carriedDamage(float)` 保留给旧公式。生产调用点已全部改走拆分，仓库里不再有 `.carriedDamage(` 的调用。`tideMarks()` 由 C 保存条数，A 不再用条数乘 2 回推伤害。

请 B 保持 `auraDamage()` 为原始光环金额。请 C 只在形成新的无潮印武器印记时调用 `globalDamageContribution`。

## 命名门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。只有 `OriginalHumanoid.java` 第 165 行 `i` 和第 202 行 `to`。本次改过的文件没有违规，没有改那个旧文件。

## 交付状态

待统一编译。游戏效果未测。写入已收束，所有权已释放。
