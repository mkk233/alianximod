# 专项C报告：聚能资格与其他消费者

**结论**

- 已给材料中没有“元素术式、剑术”的资格判定。`TechniqueHitSpec.spawnReported` 151-170 行按 `volumeScale` 放大所有开启 scale 开关的实体载体；`bakeArc` 417、428-435 行对弧带同样放大。结果就是：缺月、断潮、岩槌、让锋这类带元素剑术会被聚能放大；奥术飞弹、持续试射这类无属性法术也会被放大。除非缺失的 `CastFrame.volumeScale()` 自己按载体拦截，否则资格在消费者处已经丢失。
- 放大只覆盖实体命中体：`length/width/height/modelScale`，外加 `ObstacleBodies.track` 的挡路体积（`TechniqueHitSpec` 393 行）和弧带快照 scale。持续时间没有倍率字段；`TechniqueHitEntity` 367 行原样抄 `spec.lifetime`，垂丝用常量 `WeepingSettings.LIFETIME`（`WeepingCurtain` 127-129 行）。
- 水幕、雾雨、深海重压、水环、水牢这类零伤害载体未见能量消费者：`WaterReleaseDefinition.barrier/domain/bodyGuard/conditional` 94-131 行都不调用 `.energy()`，`thickness` 为 0，`fieldRadius` 未设；范围与持续时间都取定义固定值。材料内可确认“只有实体消费者处理了放大”，非实体载体高度疑似漏掉。

**确定事实**

- 资格来源只有 `TechniqueType` 与 `Technique.getAttribute()`。`canUseForSwordSkill()` 为 SWORD|UNIVERSAL，`canUseForMagicSkill()` 为 MAGIC|UNIVERSAL；`TechniqueManager` 两个兼容列表只按类型过滤。`SkillAssemblyPacket.put` 117-131 行只拦“剑术进法杖”和“法术进剑技”，UNIVERSAL 两边都能装，没有元素或聚能限制。
- 聚能：`ReleaseDefinitions` 26 行注册为 `ReleaseKind.RULE`、`ChainRole.ATTACK`、`energyScale=1`、`chainEnergyScale=2`；`ModTechniques` 112-115 行为 UNIVERSAL、无属性、0 伤害、装配费 0、魔力 0。
- `ReleaseDefinition` 没有“元素”字段，也没有“持续时间倍率”字段；只有 `energyScale/chainEnergyScale` 和四个 `energyScalesLength/Width/Height/Model` 开关。水系额外有 `spanScale/lengthScale/fieldRadius/guard/barrier/domain`，这些都不读能量。
- 剑术命中体走 `cohortSword`，四个 scale 开关全为 true（`ReleaseDefinitions` 38-47 行）；缺月走 `WaterReleaseDefinition.shot(...).swordForm()`，`arcBand=true`、自身 `energy=1.35`（`WaterTechniques` 23-25 行）。两条路都没有按 `TechniqueType.SWORD` 排除。
- 未聚能时 `TechniqueHitSpec` 157 行只在 `volumeScale != 1` 时改尺寸，基线本应不变；但缺月基线已是 1.35 而非 1，这是现有定义的特例，需确认是否算“未聚能基线”。

**资格链与最早丢失点**

- 装配阶段无资格检查，聚能可进剑技链，也可进法杖链。元素剑术不能进法杖，但能在剑技链里与聚能同链。
- 注册阶段，聚能占 `ChainRole.ATTACK` 是最可疑的早期点。如果收集器把 ATTACK 当载体端点，`chainEnergyScale` 可能挂不到后面的攻击上；需 `SkillSegmentMath`、`ReleaseBatch`、`CastFrame` 证实。
- 实体生成阶段，`TechniqueHitSpec` 151-170 行对非弧带载体无条件乘 `volumeScale`，`bakeArc` 417/428-435 行对弧带无条件乘。这是材料内最早可确认的“错误放大”位置。
- 零伤害载体在 `WaterReleaseDefinition.barrier/domain/bodyGuard` 94-120 行就没有能量入口，也不生成 `TechniqueHitSpec`；这是水幕、领域、护体“最早丢失倍率”的位置。
- 持续时间在契约层就缺字段，`TechniqueHitEntity` 367 行与 `WeepingCurtain` 127-129 行都用固定值，后面没有可丢的倍率。

**飞弹、水幕、垂丝**

- 飞弹：判定用 `spec.length/width/height`，视觉用 `modelScale`（同步到 `DATA_MODEL_SCALE`），挡路由 `ObstacleBodies` 用同一组尺寸；持续仍是定义里的 40 tick。
- 水幕：不走实体命中体，墙体与减伤由 `BarrierDefinition.curtain()` 提供；定义 `thickness=0`、`fieldRadius` 未设、无 energy，材料内看不到判定或视觉放大入口，持续取 `barrier.lifetimeTicks()` 固定。
- 垂丝：宽度随实体 `width` 放大，厚度按宽度比例派生，长度、高度由地形裁切；持续固定。它只算实体消费者里的宽度分支。
- 水环、雾雨、深海重压、水牢：定义未接能量，范围、持续、视觉固定，疑似漏掉。

**适用载体与排除依据**

- 适用：MAGIC 且属性非 NONE 的命中体，如热光束、金针、寄生种子、火球、爆燃、垂丝。元素领域、护体要等补上消费者后才成立。
- 排除：`Technique.getType() == SWORD` 的全部术式，包括带元素的缺月、断潮、岩槌、让锋，以及幻剑术、剑来、万剑归宗。依据只能是 `TechniqueType`；`requiresHeldSword()` 只覆盖缺月，`role()` 全为 ATTACK，`manipulatesSwordEntities()` 只覆盖剑来、万剑归宗，都不能代替类型判断。
- 奥术飞弹、持续试射、生成水的属性为 NONE，按“只作用元素术式”也应排除；当前实体消费者没有属性判断。

**猜测与缺失材料**

- `CastFrame.volumeScale()` 是否已含载体类型或属性判断，材料未给；这是“资格是否真丢”的关键。
- 领域、护体、屏障的运行时类可能自行读 frame 或 definition，需查。
- 双重释放共享一次聚能加成（`docs/底座功能点.md` 50 行）以及附魔半径并入体积倍率（同文件 58 行），需 `ReleaseBatch`、`EnchantmentResolution` 证实；附魔半径可能让非元素载体也吃到元素放大。
- 按文档，体积倍率是“释放定义 `energyScale` × 链上 `chainEnergyScale` × 附魔半径”，代码只确认消费者读取 `volumeScale`，完整公式在缺失的 `CastFrame`。

**建议核实**

`CastFrame.java`、`SkillSegmentMath.java`、`ReleaseBatch.java`、`TechniqueHitSettle.java`、`TechniqueHitEntity.tryHit`、`SizedBoxHitbox`、`ArcBandSnapshot.freezeLook`、`BarrierDefinition`/`DomainDefinition`/`GuardDefinition` 及其实体、`WaterFormEffect`、`WeepingSettings`、`EnchantmentResolution`、`ChainRole.java`、`ReleaseDefinition.java` 完整接口。根目录 README 不存在，未补造依据。