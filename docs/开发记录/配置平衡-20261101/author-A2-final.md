我先读取 A2 任务说明和已有 API 合同，确认这阶段允许改哪些文件。A2 要按已定合同把配置接到注册、加载和结算上。我先读接口衔接和第 2 轮补充，再按互斥文件改。第 2 轮要求同键重复必须报错，并补上只乘全局伤害的入口。我先核对现有报价和段伤害调用点，再按合同改我这侧的文件。同一键改为冲突即拒绝，并接上目录封存、费用、伤害和世界配置。先把即将改动的文件按现有字节留底。目录句柄和全局伤害入口已经接上。接下来补封存、配置文件和各处实际读取。A2 已按授权范围接完。设计额度 2/2 已用尽，这次没有重做架构，也没有做正式复审。未编译、未启动游戏、未提交。写入已收束，所有权已释放。

## 已完成

同一键再次声明直接抛出 `BalanceConflictException`，默认值相同也拒绝。不同键仍合并。标准键如果已经存在就跳过，不覆盖 B/C 的专项键。

`BalanceRegistry.current()` 是唯一目录。实例方法 `acceptSpecial` 和静态 `declareSpecial` 写入同一份表。

新增：

```text
ConfiguredDamage BalanceService.globalDamageContribution(double rawAmount)
ConfiguredDamage BalanceService.globalDamageContribution(BalanceSide side, double rawAmount)
```

它只乘全局伤害一次，四舍五入、1e9 上限，以及倍率恰好为 1 时保留原 `float` 的规则，与 `damageContribution` 相同。它不读取术式自己的伤害倍率。

内容事件之后的顺序是：补标准键，调用 `ModTechniques.declareSpecialBalances()`，再调用 `EnchantmentDefinitions.declareBalance(BalanceRegistry.current())`，最后 `seal`。外部入口 `RegisterXueXiContentEvent.registerBalance` 委托同一目录。旧的 `registerCast` 和 `registerEnchantment` 仍可单独使用。

世界配置：

- 模板是游戏目录下的 `defaultconfigs/xueximod-balance.json`。缺失时按封存后的代码默认生成。
- 世界文件是世界根目录下的 `serverconfig/xueximod-balance.json`。缺失时复制模板。
- 已有世界文件只读，不回写玩家的值，也不删除未知键。
- JSON 损坏或 schema 不是 1 时，保留文件，记录日志，安装代码默认快照。
- 单个字段非法时警告并回退该字段。

服务端启动时装入并绑定服务端快照。玩家登录时发送已经接受的覆盖值，网络包号 15，通道协议仍是 `"2"`。同步的是覆盖值，不是乘完全局倍率的结果。客户端只安装客户端快照。断线只清客户端；服务端停止只清服务端。客户端挂钩带 `Dist.CLIENT`，通用类不引用它们。

段伤害按三笔分别缩放一次：宿主 `rawHostAttack()` 加武器、共享附加按 `addedSourceId()`、潮印按 `tide_mark`。没有附魔时，法杖调整后的整段宿主金额缩放一次。没有伤害键时保持原金额。`carriedDamage(float)` 仍留在 `EnchantBonus` 上，生产调用点已全部改走这次拆分。

领域光环在 `DomainAuraResolver` 里按光环附魔来源缩放一次，再加到已经配置过的基础金额上。

魔力按账单分开。报价原始值是 0 时，不读取 `manaCost` 的冻结默认，所以领域、水幕、水环的宿主预扣仍是 0。领域开域费在多重份额里用 `manaCost` 缩放单价，再乘份数。水幕和水环用 `spawnManaCost`。水牢因为单价大于 0，预扣仍只有附魔。退款仍退原收据，没有重新计算倍率。

冷却在作者 A 的网络代码和两处界面里先走 `cooldownTicks`，之后才是原来的反应倍率或法杖 `scaleCooldown`。`Technique.baseDamage`、`manaCost()`、`getBaseCooldown()` 仍返回原始值。

## 未覆盖

`TechniqueChain.gapBefore` 仍读取原始冷却，`SkillUsePacket` 只对它的返回值做法杖缩放。这个类不在 A2 的写入范围内，段间隔还没有进入全局冷却。

这些展示仍读原始冷却或原始伤害，战斗扣费和段伤害不走它们：`Skill.getTotalBaseCooldown`、`SkillStoneData.imprint`、`SkillStoneItem`、`SkillBookItem`、`BoundSkillTooltip`、`ShuManagementScreen`、`ShuCommands`。

`fieldSpawnCost` 默认是 0，没有已批准的独立键。多重份额和 `SkillChainPlan.fieldSpawnExtra` 仍按原始值乘份数。

开域费、水幕水环的实际生成扣款、水牢成功单价、自身状态、维护费和接触费在 B 的类里。A 只缩放自己文件中的报价和多重份额。火伤、武器印记和剑实体物品伤害在 B/C 的类里。

基础伤害大于 0、却不在下面的伤害表里、也不是剑来或万剑归宗的内置术式，封存时会打一条警告，并且不会获得伤害键。这次没有启动，这条警告要等第一次加载才能看见。

`CastFrame`、`ReleaseBatch`、`FlowRoundLedger`、`TechniqueHits`、`ReleaseInput`、`ReleaseRegistry`、`Technique`、`TechniqueManager`、`TechniqueIds` 和 `release/entity` 没有改。它们保存调用方已经给出的金额。

## 配置键

标准键由 A 声明。计算器在封存时读一次原始 getter，不调用 `BalanceService`。

| 键 | 原始默认 | 实际消费 |
| --- | --- | --- |
| 全局三项倍率 | 字面量 1 | 只在 `damageContribution`、`globalDamageContribution`、`manaCost`、`cooldownTicks` 内各乘一次 |
| `cooldownTicks` | `getBaseCooldown()` | `SkillSegmentMath.configuredBaseCooldown`，之后才是反应或法杖缩放；`SpellAssemblyScreen`、`ImprintTreeView` |
| `manaCost` | `manaCost()` | 报价大于 0 的 `quotedMana`；领域本体用 `initialCost` 读这一键。水牢预扣不含宿主这一笔 |
| `damageMultiplier` | 字面量 1 | `configuredSegmentDamage`、`DomainAuraResolver`、两处界面。剑来和万剑归宗不声明 |

有伤害倍率的编号：三段斩击、冲刺、奥术飞弹、测试持续、热光束、空间斩、寄生种子、潮斩、石锤、金针、火球、火爆、三道水刃、陨星、冲击环、火花、幻剑、水牢、燎原、焚天、燎里、灼地、引燃、引爆、冷却、潮印、藤印、沉刃、锐印、点燃。

A 读取但由 B 声明的键只有 `spawnManaCost`。水幕和水环本体单价走它；键不存在时用 `spawnCost()` 原值。

`controlTicks`、`burnSeconds`、`periodTicks`、`intervalTicks`、`flatTicks`、`tierTicks`、`halfExtent`，以及寿命、速度、半径、卸伤、维护费等专项键，A 不声明也不消费。`BalanceKeys` 里的建议常量没有因此被登记。

## B/C 接入

两个 bootstrap 各调用一次，顺序是标准键、B、C、封存。C 收到的句柄是 `BalanceRegistry.current()`。

统一编译还依赖这些由 B/C 实现的方法：

- `ModTechniques.declareSpecialBalances()`
- `EnchantmentDefinitions.declareBalance(BalanceRegistry)`
- `EnchantBonus.rawHostAttack()`、`rawAddedDamage()`、`addedSourceId()`、`rawTideDamage()`

`tideMarks()` 由 C 保存条数。A 不再用条数乘 2 回推伤害。

B 需要让 `auraDamage()` 保持原始光环金额，缩放只发生在 `DomainAuraResolver`。C 只在形成新的无潮印武器印记时调用 `globalDamageContribution`。

## 文件

A2 新建：`BalanceDisplay`、`BalanceCatalog`、`BalanceDocuments`、`BalanceLifecycle`、`BalanceSyncPacket`、`BalanceClientHooks`、`BalanceClientLogout`，以及 `docs/开发记录/配置平衡-20261101/author-A2-report.md`。

A2 修改：`BalanceRegistry`、`BalanceService`、`XueXiMod`、`RegisterXueXiContentEvent`、`ModNetwork`、`SkillSegmentMath`、`SkillUsePacket`、`BatchCastSession`、`CastExecutionSession`、`SkillChainPlan`、`AutomaticRound`、`SkillFollowCast`、`CompiledReleaseStep`、`BatchReleaseCompiler`、`ChainSteps`、`DomainAuraResolver`、`SpellAssemblyScreen`、`ImprintTreeView`、`API合同.md`、`author-A-manifest.json`。

改前的 18 个 Java 文件已复制到 `docs/开发记录/配置平衡-20261101/pre-A/` 下的对应相对路径。其中 `BalanceRegistry` 和 `BalanceService` 的副本是 A1 收束字节，不是仓库原有基线。`API合同.md` 第 6 节已改成同键必冲突，改前副本没有单独再存。

## 命名门禁与编译

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。只有 `OriginalHumanoid.java` 第 165 行的 `i` 和第 202 行的 `to`。本次改过的文件没有违规，没有改那个旧文件。

待统一编译。游戏效果未测。
