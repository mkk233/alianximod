专项 A：聚能输入底座（只读材料内的结论）

## 一、确定事实

**1. 能量字段的位置与默认值**
`ReleaseDefinition.java:120-128` 定义两个能量入口：`energyScale()` 表示本术式来源大小，默认 `1f`，注释说“所有属性共用，1 表示不改碰撞体积”；`chainEnergyScale()` 表示作为规则挂到后面攻击上的能量，默认 `1f`，“多条相乘”。`ReleaseDefinition.java:288-306` 另有 `energyScalesLength/Width/Height/Model()`，全默认 true。材料里没有显式单位、上限或下限，运行时兜底在 `CastFrame.volumeScale()`（`CastFrame.java:59-65`）：`energyScale * radiusMultiplier`，非有限或 ≤0 时回 1。`chainEnergyBefore` 另外跳过 `|值-1|≤0.001` 的项（`SkillChainPlan.java:508`）。因此实际下界是“>0”，0.001 内视为中性，无上界。

**2. 聚能走的是链能量，与 ChargeGate 无关**
`ChargeGate.java:8-40` 只有 `epoch/same/armed/arm/disarm/interrupt`，管蓄力时序与打断，没有任何能量字段。`SkillUsePacket` 的网络载荷只有 `slotIndex`（`SkillUsePacket.java:81-87`），没有“输入量”字段。聚能的实际路径是 `ReleaseDefinition.chainEnergyScale()` → `SkillChainPlan.chainEnergyBefore()` → `CastFrame.energyScale`。也就是说，“装配到 ChargeGate 的输入量”在现有材料里不存在。

**3. 数值公式（确定）**
`SkillSegmentRunner.java:458-461`：`ownEnergy = attackDefinition.energyScale()`；`chainEnergy = SkillChainPlan.chainEnergyBefore(techniques, actionIndex)`；`combined = ownEnergy * chainEnergy`；非法则 `1f`，写入 `frame.energyScale`。`SkillChainPlan.java:493-514` 从攻击前一位向前扫，遇到正伤非附魔术式即停（`sharesDualRelease` 为真则跳过），把每个 `chainEnergyScale` 连乘。普通（未聚能）：`energyScale=1`，`volumeScale=radiusMultiplier`，尺寸不变，与“基线不变”一致。一条聚能按文档 `底座功能点.md:50` 是乘 2，两条乘 4；该 2 来自文档，属推断，定义文件未给。

## 二、伤害、几何、寿命

伤害不乘能量：`SkillSegmentRunner.java:397` 仅 `finalDamage * influence.damageScale()`。几何走 `volumeScale()`，倍率 1 时不变（`底座功能点.md:58`）。寿命不乘：`CastFrame` 全类没有寿命或持续字段（1-290 行），长度由 `ReleaseDefinition.lifetimeTicks()` 提供，文档也明确“持续时间不乘”。所以“能量提高带动持续时间”当前没有承载字段，最早缺在 `CastFrame` 字段层，其次缺在读它的效果类。

## 三、复制、快照、延迟

能量不进 `buildSegmentRunner` 参数，也不进 `EnchantmentSnapshot`，而是在每次 `runSegment` 内按 `techniques + actionIndex` 重算。一分为三的每份（`SkillChainPlan.java:262-291`）、宿主组复制（`SkillSegmentRunner.java:229-243`）、双击序列（`SkillChainPlan.java:418-436`）都用同一份列表，故各份倍率相同，没有丢。蓄力延迟路径 `SkillSegmentRunner.java:317-342` 在延后 tick 递归 `runSegment`，列表仍为捕获的同一引用，重算结果不变。双重释放“几发共用一次加成”依赖 `sharesDualRelease`，该文件未给，无法确认成员二向前扫时是否会被成员一截断。

## 四、领域、护体、场地

这些消费者拿不到 frame：领域 `DomainService.tryWave(...domainCopies, snapshot)`（`SkillUsePacket.java:443`、`SkillChainPlan.java:312`）、护体 `BarrierService.tryPlace(...)`（`:461`、`:339`）、随身护体 `GuardStates.schedule + bindContact(snapshot)`（`:478-489`）、场地 `FieldSpawner.releaseFromChain/ releaseAttached`（`:605`、`SkillSegmentRunner.java:417`）。它们只收到 `EnchantmentSnapshot`、份数、锚点，没有 `energyScale`。若聚能要放大领域范围，当前结构不成立。

## 五、最早错用点与资格

最早、也是唯一的倍率落点是 `SkillSegmentRunner.java:458-461`，其上游 `SkillChainPlan.java:493-514` 不做类型判断。整条链没有 `TechniqueType` 或元素判定，`SWORD` 只在 `SkillUsePacket.java:125` 与挥臂处使用。因此“聚能只作用元素术式、剑术即使带元素也不允许”在现有代码中**没有实现**，表现为对让锋、缺月、幻剑等一并生效。修复入口应在这两处或 definition 资格层，具体位置留给裁决。

给 C 块对照的资格条件：攻击段 `baseDamage > 0` 且非附魔；定义与效果存在；倍率有限且 >0；向前扫至上一个攻击为止（双重例外待确认）；蓄力只延迟不取消；复制份共享同一倍率；领域、护体、屏障、场地不经过该路径。

## 六、猜测与缺失材料

猜测：聚能定义值 2、双重共用加成，均取自文档而非源码。缺失：`ReleaseDefinitions`、`Technique`/`TechniqueType`/`ElementReaction`（元素归属无法确认）、`EnchantBonus`（`radiusMultiplier` 与元素反应半径的实际计算）、`TechniqueHitSpec.spawn` 与 `TechniqueHitEntity`（体积消费端）、`WaterFormEffect`（水带宽度是否真读 `volumeScale`）、`SkillSegmentMath.sharesDualRelease`。根 README 不存在，未据此补造依据。建议优先核实上述六处，再判断改在链收集还是帧消费。