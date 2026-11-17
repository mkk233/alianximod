我先读这份设计事实任务，只按它写明的范围做。这是设计 1/2 的补充事实，不计新轮次。我只核对注册默认、领域载体、地表、火视觉和炉心倍率这几处现有接口。这是设计 1/2 的补充事实，不另开一轮，也不给灼地填数值。下面都是现有接口的实际语义。

## 1. 注册默认与零值

`Technique` 没有「未定」构造。四个构造最终都写入伤害、冷却、等级上限、描述、复杂度、属性、弹速、是否附魔。短构造把没写出的项固定成复杂度 `0`、属性 `NONE`、弹速 `1.0`、非附魔。

```29:44:src/main/java/com/bslx/XueXiMod/shu/Technique.java
    public Technique(String techniqueId, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int levelCap, String description) {
        this(techniqueId, name, type, baseDamage, baseCooldown, levelCap, description, 0, ShuAttribute.NONE, 1.0f);
    }
    // ...
    public Technique(..., int complexity, ShuAttribute attribute, float projectileSpeed) {
        this(..., complexity, attribute, projectileSpeed, false);
    }
```

等级上限是必填整数，读口是 `getMaxLevel()`。`src/main` 里没有其他调用。登记时这个参数普遍写成 `20`，代码里没有把 `20` 或 `0` 解释成未定。

复杂度 `0` 的含义在类注释和 `PlayerShuData.isUnlocked`：剑术、法术用「类型等级 ≥ 复杂度」；`0` 就是类型等级 0 也能解锁。通用和动作忽略复杂度，始终解锁。

冷却 `0` 在 `SkillSegmentMath.scaleCooldown` 里原样返回，不抬成 1。大于 0 时缩放后至少为 1。

费用不在 `Technique` 上。`ReleaseDefinition.explicitManaCost()` 和 `explicitAssemblyCost()` 默认都是 `-1`。短 `define(...)` 也传入 `-1, -1`。接口注释写明：魔力小于 0 仍按伤害算；装配消耗小于 0 仍按伤害和类型推。`0` 是写明的 0，不是未定。

```132:140:src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinition.java
    /** 小于 0 时仍按伤害计算魔力。防御和治疗填自己的消耗。 */
    default int explicitManaCost() {
        return -1;
    }
    /** 小于 0 时仍按伤害和类型推装配消耗。 */
    default int explicitAssemblyCost() {
        return -1;
    }
```

`Technique.manaCost()` 的顺序是：附魔走附魔伤害；条件释放且显式魔力 ≥ 0 用显式值；`domain() != null` 用 `domain().initialCost()`；释放种类 `RULE` 为 0；再否则显式魔力 ≥ 0 用显式值；最后是 `ceil(基础伤害 × 2)`，下限 0。`assemblyCost()` 是：显式 ≥ 0 用显式值；附魔为 2；动作为 0；基础伤害大于 0 为 1；其余为 0。

最接近的火系法术攻击，而且没有另写费用的，是火球。术式登记是 `TechniqueType.MAGIC`、伤害 5、冷却 20、等级上限 20、复杂度 0、属性火、弹速 1.2、非附魔（`ModTechniques.java` 171–174 行）。释放登记是 `HIT_VOLUME`、`TRIGGER`、`ChainRole.ATTACK`，走短 `define`，因此显式魔力与装配消耗都是 `-1`（`ReleaseDefinitions.java` 23 行）。按上面的公式，这类正伤害、无领域、非规则的登记会得到装配消耗 1，魔力为伤害向上取整后的两倍。这是火球这条现成登记的算法，不是灼地的数值。

基础伤害 `0` 会让魔力公式得到 0、装配消耗落到 0，并且进不了后面的「可绑定攻击」；领域宿主反而要求伤害不大于 0。弹速 `0` 只表示术式上的弹速字段是 0，释放定义里的速度是另一项。`fieldRadius()` 默认 0，表示这发不是场地。

## 2. 领域绑定怎样对待攻击

`rule_domain_bind` 自身是通用、伤害 0、冷却 0、等级上限 20。释放种类是 `RULE`，标签 `TRIGGER`，角色 `RULE_DOMAIN_BIND`（`ModTechniques.java` 115–118 行，`ReleaseDefinitions.java` 30 行）。效果注册是空实现。

它不把后面的攻击当成领域载体。载体必须是标记正前方那一格，而且要同时满足：不是附魔、基础伤害不大于 0、`domain() != null`。

```429:448:src/main/java/com/bslx/XueXiMod/network/BindingStructureCompiler.java
    private static boolean domainHost(Technique technique) {
        if (technique == null || technique.isEnchantment() || technique.getBaseDamage() > 0f) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.domain() != null;
    }
    private static boolean qualifiedAttack(Technique technique) {
        // 基础伤害 > 0，角色 ATTACK，不能再带 domain/guard/barrier，不能是条件释放或 SELF_STATUS
    }
```

标记必须紧贴载体。通过后，标记和它后面的攻击段从投影链里摘掉，冻成 `BoundReleasePlan`。`qualifiedAttack` 不要求种类一定是 `HIT_VOLUME`，只排除 `SELF_STATUS`。`fieldRadius() > 0` 不能代替 `domain()`，也挡不住一条攻击被绑走。

运行时宿主是 `DomainInstance`。`DomainDefinition` 要有 `Placer`、初始消耗、寿命、原生策略、光环、是否达级学会；放置口是 `place(ServerPlayer)`。现成盒子是 `DomainDefinition.feetBox` 和雾雨用的 `AxisBoxGeometry.atFeet`。`SkillUsePacket` 599 行同样只在 `domain() != null` 且伤害不大于 0 时调用 `DomainService.tryWave`。

绑定后的攻击由 `DomainBindingRuntime` 放。首发是开域后 20 刻，之后每 20 刻一次，常量 `PERIOD_TICKS = 20`。原点是领域几何里的采样点，方向朝领域内最近目标，否则随机球面方向。开域费由领域服务收，每一轮子计划的费用由自动执行器收。

持续攻击的现成接法是另一条：热光束在释放定义里用 `ReleaseTag.SUSTAINED`，效果 `HeatBeamEffect.execute` 自己组 `TechniqueHitSpec`（跟随、寿命读定义、命中间隔 10、视觉号 2），再按编号注册。跟随施法的间隔来自该段冷却，`SkillFollowCast` 160 行会把冷却先抬到至少 1。燎原则是 `TRIGGER` 加 `LiaoyuanEffect`，不走这套持续命中体。

以「灼地仍是领域载体」去对现有接口：载体门只认 `domain()` 且伤害不大于 0。`fieldRadius()`、命中体和正伤害都过不了 `domainHost`。正伤害的 `ATTACK` 会被绑定标记当成被周期施放的攻击，而不是载体。

## 3. 地表搜索与脚前距离

`StandSurface.columnTop(BlockGetter level, double sampleX, double sampleZ, double originY, double slack)` 把同一列、从 `floor(originY + slack)` 到 `floor(originY - slack) - 1` 的碰撞箱收进 `GroundFireMath.openTop`。成功时返回该柱里最高的、上方没有别的碰撞挡住、并且与 `originY` 落差不超过 `slack` 的顶面。参数非法、没有碰撞，或顶面都被挡住时返回 `NaN`。它不改方块，也没有水平距离参数。

唯一调用在 `GroundFireFields`，传入的 `slack` 是常量 `VERTICAL_SLACK = 3.0`。那是燎原的垂直搜索，不是通用脚前格数。

现有释放没有「脚前若干格」的公共距离。燎原原点是施法者脚点 `player.position()`，有 `CastFrame.overrideGeometry` 时改用帧上的 `origin`。场地锚点在身体中点。落点指定的视线搜索是眼睛向前 8 格、找不到实体时退回 4 格（`ReleaseBatch` 的 `ANCHOR_RANGE = 8.0`、`ANCHOR_FALLBACK = 4.0`），取的是实体中心或视线点，不是可站立顶面。

## 4. 火视觉

`FireVisualPacket` 没有样式或来源字段。地面包是 `ground(zoneId, dimensionId, originX, originY, originZ, radius, birthTick, expireTick)`，半径和出生、结束时刻都在包里。种类只有地面、地面结束、燃烧、燃烧结束、炉心。燃烧只用 `FireVisualLayout.SPREAD = 1` 和 `SKY = 2` 区分燎原与焚天。地面贴图写死为燎原的 `liaoyuan_v2/ground_flame.png` 和 `ground_embers.png`。`FireVisualRelay.openGround` 按区号、维度、原点、半径和寿命转发，不接受贴图或颜色。

火种视觉不走粒子。`FireSeedVisualPacket` 只带实体和余额，`FireSeedFlameClient` 用自己的透明图集画四边形，朵数来自 `floor(余额 / 10)`。没有「把火种视觉改播成某种粒子」的参数。

`src/main` 里没有 `SOUL_FIRE_FLAME`。`ParticleTypes.FLAME` 只出现在这些位置：命中实体视觉号为 4 时（`TechniqueHitEntity.java` 410 行）、光束分段（`HitVolumes.java` 199 行）、爆燃（`FireBurstEffect.java` 85 行）、竖劈（`SwordVerticalSlashEffect.java` 55 行）、带火附魔的冲刺（`MovementDashEffect.java` 63 行）。它们都不读火种余额，也不区分蓝火。

## 5. 炉心产种倍率

```22:25:src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceService.java
    public static double seedMultiplier(LivingEntity entity) {
        FurnaceStore.Snapshot snapshot = serverSnapshot(entity);
        return FurnaceMath.seedMultiplier(snapshot.tier(), snapshot.bonus());
    }
```

参数是传入的那个生物。`serverSnapshot` 只接受服务端线程上的 `Player`；否则返回无炉心快照。`FurnaceMath.seedMultiplier` 在档位为 null 时返回 `1.0`，超频返回 `2.2`，普通档按热量在 1.12 到 1.60 之间。非玩家、错线程或没有炉心，因此都是 `1.0`。全库没有调用 `seedMultiplier`。

已有的快照惯例是燃烧用的 `fireBonus`，不是这个产种倍率。`FireConsumption.casterBonus` 用施法上下文里的施法者 UUID 找到在线施法者，读的是施法者的 `fireBonus`，不是目标身上的炉心。挂燃烧时把这个数写入燃烧记录，之后各段沿用那次写入的值。施法者不在线时记 0。
