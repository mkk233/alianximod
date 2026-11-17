我先读取这份任务说明，按其中授权范围执行，并在结束时给出完整报告。任务是 B1 只读采集：不写文件、不编译、不派代理。我先读本目录的架构裁决、实现合同和三份终稿，再只针对归我负责的源码核对接口事实。架构合同已对齐。接下来只核对我名下的定义工厂、费用扣点和对 C 的现有调用签名。B1 只读采集完成。未写文件，未编译，未派代理，未读 `shu/balance` 的中间代码，也未做正式复审。下面是后续接入要用的外部接口事实。

## 1. 专项参数工厂，以及给 A2 的一个入口

标准费用、冷却、适用伤害仍由 A 从已登记的 `Technique` 和释放定义导出。B 只声明这些导出覆盖不到、而且已有消费者的专项参数。声明必须用工厂里的同一份原始默认值，注册和静态初始化只保留原始值，生效值等本次世界快照。

就近工厂：

| 工厂 | 现在持有的专项原始值 | 登记时机 |
| --- | --- | --- |
| `ReleaseDefinitions.define` / `cohortSword` | 寿命、速度、厚度；垂丝改读 `WeepingSettings` | `ReleaseDefinitions.registerAll`，由 `ModTechniques.registerAll` 第 383 行调用 |
| `WeepingSettings` | 底伤 2、潮湿 3、命中间隔 10、寿命 40、填充 8、排空起点 32、下落上限 6、帘宽 0.5、显式魔力 10 | 被术式字段和 `define` 同时引用 |
| `ListedRelease.meteorite/shockRing/spark` | 寿命、速度、厚度、出生半径、火苗产种基数 2 | `ReleaseDefinitions.registerAll` 第 45–47 行 |
| `ListedRelease` 四条规则与 `enchant` | 无独立玩法尺寸；附魔魔力不在这里 | 同文件第 48–58 行 |
| `DomainDefinition.mistRain/deepPressure/gloom` | 盒子、寿命、开域费；原生策略参数。`upkeepCost()` 已废弃，不声明维护费 | 雾雨、深压在 `WaterTechniques.register`；幽冥在 `ListedRelease.gloomDomain` |
| `BarrierDefinition.curtain` | 3.2×2.8×0.25、前向 2、寿命 80、生成 8、维护 2/20、衰减 0.5 | `WaterTechniques` 的 `barrier(...)` |
| `GuardDefinition.personalRing` | 寿命 200、生成 8、维护 2/20、卸伤 0.20、半径 0.9、带宽 0.12、身高 0.55 | `WaterTechniques` 的 `guard(...)` |
| `WaterReleaseDefinition.shot/domain/barrier/bodyGuard/conditional` | 把上面的对象挂到释放定义。`domain/barrier/bodyGuard` 把 `explicitCost` 写成 0 | `WaterTechniques.register` 第 21–39 行 |
| `WaterPrisonEffect` + `PrisonRuntime.prisonTicks` + `PrisonPressure` | 扫描半径 12、成功单价 40、一层 20 刻、二层 100 刻、压强 4、间隔 20。四层门槛保持原结构 | 水牢定义在 `WaterTechniques.prison` |
| 效果与载体里真正消费、登记副本盖不住的常量 | 热光束长度 6 与间隔 10、寄生种子寿命 160 与散布 1.2、焚天半径 8、爆燃半径 3、空间切割寿命 10、柔带寿命 32、剑来搜索盒 `inflate(20)`、`BatchCarrierIds` 的陨岩/热波/火苗命中尺寸、`MotionProfile` 的抛物线/喷涌首版、`DomainContactCharge.MANA_PER_BASIC_ENCHANTMENT = 1` | 各效果执行时 |

`WaterReleaseDefinition.zone(...)` 没有调用方。`SelfStatusDefinition` 的 20/20/0 是显式魔力，归标准费用，不另造专项伤害键。

统一触发：`ModTechniques.registerAll` 在第 382–384 行依次调用 `EnchantmentDefinitions.registerAll()`、`ReleaseDefinitions.registerAll()`、`WaterTechniques.register()`。外部内容在这之后才由 `XueXiMod.commonSetup` 投递 `RegisterXueXiContentEvent`。`ReleaseRegistry` 只有 `register` 和 `get`，没有遍历接口，而且该类归 A。

建议 A2 在外部登记之后、参数目录封存之前调用一次：

`com.bslx.XueXiMod.shu.ModTechniques.declareSpecialBalances()`

这个方法归 B，实现后再由 A2 接上。它内部再调用 `ReleaseDefinitions`、`ListedRelease`、`DomainDefinition`、`BarrierDefinition`、`GuardDefinition`、`WaterTechniques.declareSpecials()`，以及上面那些效果工厂的声明方法。不要放进静态字段，也不要放进 `registerAll()`，因为 `XueXiMod.java` 不归 B。附魔 id 即使由 `registerAll` 调到 C 的 `EnchantmentDefinitions.registerAll()`，也不在这个方法里声明。

## 2. B 与 C 的直接交界

B 不能改 C 的文件。现有调用如下。

**燎里。** `LiaoliEffect` 第 122 行：

`FireConsumption.detonate(LivingEntity, FireCastContext, int requested, StrikePlanner)`

实参是 `FireConsumerPlans.ALL_SEEDS`（`Integer.MAX_VALUE`）和 `FireConsumerPlans::thrust`。`thrust(int amount, int tiers, int remainder)` 返回 `Optional<BurnComponent>`。`StrikePlanner.component(int, int, int)` 同样返回这个类型。物理刺击在同文件第 124–126 行走 `TechniqueHits.hurtRoot`，金额是底伤加迎面速度，不进入这次 `detonate`。`detonate` 内部自己算火种伤并调用 `FireDamage.strikeOnce`，参数里没有术式 id，也没有伤害倍率。

需要 C 提供的合同：按 `fire_liaoli` 只缩放这次火种贡献，并保持炉心只合入一次。B 继续只缩放 `hurtRoot` 的物理贡献。

**火范围与持续时间。**

- 燎原：`LiaoyuanEffect` 第 39–45 行先取 `GroundFireFields.BASE_RADIUS`（10）和定义寿命，再调用已有的 `GroundFireFields.open(ServerLevel, FireCastContext, Vec3, double radius, int lifeTicks)`。半径和寿命能由 B 在调用前换成快照值。本体伤 `GROUND_DAMAGE = 8` 仍在 C。
- 焚天：半径是 `FentianEffect.BASE_RADIUS`（8），归 B。段参数走第 59–60 行 `FireConsumption.schedule(LivingEntity, FireCastContext, String, SpecSource)`，键是 `FireConsumerPlans.SKY_KEY`，来源是 `FireConsumerPlans::skySpec`。`skySpec(int balance, double furnaceBonus)` 不接收间隔、段数或基数。间隔 4、上限 10 段和 `2×(1.2^层+F)` 都在 C。
- 灼地：`ScorchEffect` 第 45 行调用 `SurfaceFireHosts.open(ServerLevel, ServerPlayer, ReleaseBatch, BoundReleasePlan, EnchantmentSnapshot, ResourceEnchantLoad, UUID)`。该签名不接收半径或寿命。C 内部寿命是私有 `FIELD_LIFE = 100`，范围在 `SurfaceContactField`。释放定义上的寿命 100 不会进入这个 `open`。

需要 C 提供的合同：焚天的 `SpecSource` 要能吃已配置的间隔、段上限和原始段基数，或者 C 自己按 `fire_fentian` 读取这些键。灼地的 `open` 要增加半径和寿命，或由 C 按 `fire_scorch_field` 读取。燎原的 `open` 可以保持不变。

**幽冥产种。** `NetherDomainCycle` 第 124–125 行和第 399 行用 `NetherContract.PERIOD_TICKS` 做窗口。第 444 行调用：

`NetherSeedArbiter.grant(LivingEntity target, long windowId, List<NetherOffer> offers)`

`NetherOffer` 只有 `UUID domainId, LivingEntity owner`。产种量在 C 内部写成 `ResourceEnchantNumbers.GLOOM_BASE`（5），再乘炉心倍率。`NetherContract.PERIOD_TICKS` 和 `BASE_POINTS` 都是指向 `GLOOM_PERIOD_TICKS`（20）和 `GLOOM_BASE` 的 `static final`。领域盒子、寿命 200、开域费 0 在 B 的 `DomainDefinition.gloom()`。

需要 C 提供的合同：`grant` 使用配置后的产种基数，周期改成方法或由 B 能读到的快照值。B 不复制 5 和 20。

**附魔费用。** B 的消费点只有接触单价：

`DomainContactCharge.quote(EnchantmentSnapshot)`，金额是 `distinctBasicTypeCount() * MANA_PER_BASIC_ENCHANTMENT`。

种类数来自 C 的 `EnchantmentSnapshot.distinctBasicTypeCount()`。资源附魔单价不在 B：

`ResourceEnchantCatalog.explicitManaPrice(String) -> int`，负数表示不是资源附魔；`ResourceEnchantDefinition.manaPrice()` 返回切片里的 `ResourceEnchantNumbers.MANA_PRICE`（2）。

A 的 `Technique.manaCost()` 和 `SkillSegmentMath.manaCostOf` 已经调用它。`ModTechniques.registerAll` 第 382 行会调用 `EnchantmentDefinitions.registerAll()`，B 只保留这行调用。

需要 C 提供的合同：`explicitManaPrice` 和旧附魔 `magicDamage` 继续返回未乘全局魔力倍率的原始单价。接触种类数接口保持不变。倍率只由 A 的 `manaCost(id, key, rawDefault)` 乘一次。

## 3. B 的真实扣费点，以及和 A 报价的交界

钱包仍是 `MagicPool`。退款必须退当次收据或同一个局部变量，不能按新配置重算。全局魔力倍率在单价形成时乘一次，随后的份数、成功目标数和维护次数沿用原账单。

B 侧真正扣款：

| 位置 | 原始单价 | 与 A 报价的关系 |
| --- | --- | --- |
| `DomainService.releaseWave` 第 151、166 行，`!reserved` 时 `initialCost * count` | 雾雨/深压 40，幽冥 0 | 普通链先由 `SkillUsePacket` 第 409 行扣 `pressCommitCost`。领域定义的 `explicitCost` 是 0，所以这次预扣不含开域费，开域费只在这里扣。多重走 `CompiledReleaseStep.memberShare` 把 `initialCost * copies` 加进账单，再调 `tryWaveReserved`，这里不再扣 |
| `BarrierService.releasePlace` 第 79–80 行，`!reserved` 时 `spawnCost * count`；失败时第 96、105 行 `MagicPool.restore` 同一笔 | 水幕 8 | 与开域同一套分流。维护不在预扣里 |
| `BarrierBody.tick` 第 186 行，实体上保存的 `upkeepCost` | 水幕 2，周期在第 190 行重新调用 `BarrierDefinition.curtain().upkeepPeriod()` | 只在这里按周期扣。周期必须从快照读，不能只改放置时复制的那份 |
| `GuardStates.activate` 第 90 行，`!reserved` 时 `spawnCost` | 水环 8 | 普通链在这里扣；多重由 A 报价后走 `activateReserved` |
| `GuardStates.tick` 第 221 行 `upkeepCost` | 水环 2 / 20 刻 | 预扣不含维护 |
| `SelfStatusCommit.attempt` 第 42–51 行 `explicitManaCost` | 升温 20、超频 20、冷却 0 | `pressCommitCost` 遇到自身状态直接跳过，所以只在这里扣 |
| `WaterPrisonEffect.attempt` 第 88、108、123 行，每个成功目标 `unitManaCost()` | `SUCCESS_MANA = 40`，定义上的 `explicitCost` 也是 40 | `settledSegmentCost` 在单价大于 0 时只预扣附魔。成功单价不能再进预扣 |
| `DomainContactBilling.apply` 第 85–92 行 | `quote(snapshot)` | 不进按键预扣 |
| `FieldZones.tick` 第 229 行，场地对象上的 `upkeepCost` | 现有水系成品没有调用 `zone()`，`fieldRadius` 默认 0 | 生成费由 A 的 `fieldSpawnCost()` 报价，默认 0。一旦半径大于 0，维护在这里扣 |

A 已经读取、并由 A 扣款的原始值，B 的 getter 必须继续返回未缩放的数：

- `SkillSegmentMath.manaCostOf`：先 `explicitManaPrice`，再 `explicitManaCost()`，否则 `Technique.manaCost()`。
- `Technique.manaCost()`：领域在显式魔力之前返回 `domain.initialCost()`。界面走这条。
- `CompiledReleaseStep.memberShare` 第 404–411 行：零伤领域、屏障、护体在宿主价之外再加 `initialCost`、`spawnCost`。
- `SkillChainPlan.fieldSpawnExtra` 与 `pressCommitCost`。
- 领域自动子计划：`DomainBindingRuntime` 第 162 行只调用 `BoundReleases.executeAutomatic`。计价和 `GroupBudget.reserve` 在 A 的 `AutomaticRound`。B 这里不再扣开域费。`DomainService` 已写明周期释放不在开域时预扣。

会重复乘倍率的点，必须共用一个原始单价：

- `initialCost`、`spawnCost` 同时被普通链的 B 扣款和多重链的 A 报价读取。倍率放在单价形成处各走一次，不能写进 getter。
- 水牢的 `SUCCESS_MANA` 与 `explicitCost` 现在是两份 40。成功扣款和界面报价要合成一个键。
- 护体、屏障、场地的维护费只在 tick 形成单价。放置时复制进实体的是原始值，tick 再乘一次就会和放置时的缩放叠成两次。
- 幽冥开域费是 0，真正的附魔费在 C 的 `explicitManaPrice`。B 不能把这份附魔费再加成领域开域费。
- 垂丝、燎原、燎里、焚天的显式魔力只通过 `manaCostOf` 进入预扣，效果里没有第二次扣款。

`BalanceService.manaCost(id, key, rawDefault)` 是 B 与 A 的单价边界。B 在上面这些真正形成单价的位置调用它，并传入原始默认。A 的 `manaCostOf`、`memberShare` 和界面也调用同一入口。B 不改 `Technique.java`、`SkillSegmentMath.java` 和 `network/**`。

## 4. 本所有权内的具体阻塞

已批准分工里，下面几项 B 无法独自接通：

- 燎里火种伤没有可传入术式或倍率的参数，缩放点在 C 的 `detonate`。
- 灼地半径和寿命没有进入 `SurfaceFireHosts.open`，C 使用私有 `FIELD_LIFE = 100`。
- 焚天段间隔、段上限和段基数封在 `skySpec(int, double)` 里。
- 幽冥产种量封在 `NetherSeedArbiter.grant` 里，周期是 C 的 `static final`。
- 资源附魔和旧附魔的原始单价在 C。若 C 先乘全局魔力倍率，A 的 `manaCost` 会再乘一次。

`ReleaseRegistry` 不能枚举，所以专项声明用第 1 节那个显式 bootstrap，不依赖 A 新增遍历接口。领域维护费键保持不生成。A 的最终 API 合同到达之前不实现。
