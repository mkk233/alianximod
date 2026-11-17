# B 接口说明：新火种资源与六个附魔

本文件是第一阶段可调用签名和接线清单。公共链、法杖、命中和领域宿主仍由对应作者接入。这里不表示那些接线已经完成。

## 1. 稳定签名

魔力与直接伤害分开。六个新附魔的 `magicDamage` 固定返回 0。显式魔力用：

- `ResourceEnchantCatalog.explicitManaPrice(String enchantmentId)`：本批附魔返回 2，其它返回 -1。
- `ResourceEnchantCatalog.collect(Skill skill, List<Integer> indexes)`：相同效果只留一份，每个原装配格仍各加 2 点魔力。
- `EnchantBonus.resourceLoad`：`resolve` 会带上这份载荷。旧五元素的伤害、燃烧秒数、击退、潮印和 `seedPoints` 仍按原效果键计算。

新效果键是 `fire_flame_trail`、`fire_seed_sow`、`fire_kindle`、`fire_detonate`、`fire_nether_gloom`、`earth_break_ground`。它们不是 `fire`，因此不会得到旧火附魔的固定 2 点火种、原版着火或 2 点附加伤害。

已接受伤害的资源入口：

```text
HitResourceResult ResourceHitCommit.commit(
    LivingEntity target,
    FireCastContext cast,
    LivingEntity caster,
    ResourceEnchantLoad load,
    int bodyScaledBase,
    int legacyFixedPoints,
    boolean damageAccepted)
```

`damageAccepted == false` 时返回 `NOT_ACCEPTED`，不改余额、不挂层、不引爆。参数无效或目标当时不能入账时返回 `REJECTED`。只有 `APPLIED` 表示这条命中走过了资源顺序。引燃被拒绝、引爆没有打中，都不会被写成另一条成功。

产种合并：

```text
SeedMergeResult SeedMerge.commit(
    LivingEntity target,
    FireCastContext cast,
    LivingEntity caster,
    int scaledBase,
    int legacyPoints)
int FireSeedMath.mergedRequest(int scaledBase, int legacyPoints, double multiplier)
```

`scaledBase` 先乘 `FurnaceService.seedMultiplier` 并向下取整一次，`legacyPoints` 再按原固定点数相加，最后由余额钳到 100。调用方必须传入这一次命中已经收齐的两部分，并且使用根 `FireCastContext`。本方法不新建上下文。请求大于 0 时，即使增加量为 0 也会占掉该目标这一次产种机会。已经产过则不再补差额。

`bodyScaledBase` 加载荷里的种火基数后才传入 `scaledBase`。火苗本体的 2 和种火的 10 都属于新基数。旧点火的 2、热光束的 1、火球的 3 属于 `legacyPoints`，保持原来 `FireSeedGrant.deposit` 的倍率 1，不再乘产种倍率。

引燃：`FireConsumption.scheduleKindle(LivingEntity target, FireCastContext cast)`。

引爆：`DetonatePlan.strike(LivingEntity target, FireCastContext cast)`。伤害公式是 `DetonatePlan.damage(balance, furnaceBonus)`，即 `0.2 * 余额 * (1 + 当时 fireBonus)`。炉心热量用 `FireConsumption.frozenFurnaceBonus`。

效果去重：`FireCastContext.claimEffect(UUID targetId, String effectKey)`。它不写入旧的 `hasProduced` 或 `hasConsumed`。

幽冥仲裁：

```text
NetherWindowResult NetherSeedArbiter.grant(
    LivingEntity target,
    long windowId,
    List<NetherOffer> offers)
```

`NetherOffer` 是领域标识和领域主人。一次调用必须带上该窗口覆盖这个目标的全部领域。

留焰只读 `TrailContract`。崩地形状只读 `BreakGroundShape.offsets()`。幽冥表现和限制只读 `NetherContract`。

## 2. 既有入口

以下行为没有改公式：

- `FireSeedAccount.grant` 仍在 `hasConsumed` 或 `hasProduced` 时直接返回 0。取整结果不大于 0 时仍不占生产机会。
- `FireSeedAccount.consume` 仍会 `markConsumed`。`draw` 仍不占引爆机会。
- `FireSeedGrant.deposit` 和 `FireSeedProduction.produce` 仍每次新建上下文。同一命中不要对 2 和 10 各调用一次。
- `FireConsumption.schedule`、`scheduleOpening` 和旧 `detonate` 仍走原来的挂载和消费。
- `FuelShare.divide` 未改。`BurnLedger.pump` 只有在本拍所有到期共享层都带引燃轮换标记时才改走 `RemainderRotation`。只要有一条旧共享层同时到期，这一拍仍调用 `FuelShare.divide`。`settleOpening` 仍调用 `FuelShare.divide`。
- 旧五元素附魔的折叠会把伤害和速度倍率乘进共享结算。六个新定义折叠时这两个倍率保持 1，不改变旧乘区。
- `FurnaceService.fireBonus` 与 `FurnaceMath.seedMultiplier` 的数值没有改。

## 3. 作者 A 需要接线

1. 在 `ModTechniques` 把六个标识登记为附魔。魔力 2、冷却 5 tick、等级上限 20、战斗成长 0。复杂度：留焰 2、种火 1、引燃 2、引爆 2、幽冥 3、崩地 3。装配消耗沿用附魔的 2。不要把它们登记成攻击，也不要为了收费把 `magicDamage` 写成 2。
2. 现有 `Technique.manaCost` 对附魔使用 `max(1, magicDamage)`。新定义的 `magicDamage` 是 0，若不改报价，登记后会被收成 1。报价应在 `explicitManaPrice` 非负时用那个返回值。相同附魔占两格就收两份 2，效果不叠加。
3. 命中被接受后只调用一次 `ResourceHitCommit.commit`，并传入根施法上下文。不要再对同一份 2 或 10 调用 `deposit` 或 `produce`。取消、未接受伤害、免疫或最终伤害为 0 时不要把 `damageAccepted` 传成 true。
4. `EnchantmentSnapshot` 仍只保存旧火的 `seedPoints == 2`。新载荷在 `EnchantBonus.resourceLoad`。释放帧需要把这份载荷带到结算，不能只抄 `seedPoints`。
5. 火苗本体基数作为 `bodyScaledBase` 传入。旧点火、热光束、火球的既有固定点数作为 `legacyFixedPoints` 传入。两部分都要进同一次调用。
6. 普通武器沿用现有 `WeaponEnchant` 账，不要另建一套。现在的 `imprint` 在燃烧、附加伤害、潮印和旧火种都为 0 时会直接返回，纯新载荷会被丢掉。需要在那次写入里保留 `resourceLoad`，并在下一次合法普通攻击被接受后调用一次 `commit`。攻击没有被接受就不要当成资源成功，也不要再收一次魔力。本阶段没有改 `WeaponEnchant`。
7. 附魔费用仍由现有魔力收据在该附魔所属的第一次攻击提交时收取一次。`commit` 不扣魔力，也不退款。

## 4. 作者 C 需要接线

1. 留焰按 `TrailContract`：寿命 40 tick，直径 0.5，表现采样不超过 0.25 格，每载体最多 256 个活跃表现点。碰撞用连续扫掠，采样限额不扩大判定。共用载体命中钟。不产种、不耗种、不放原版火、不破坏地形。第一版宿主只声明 `fire_spark`、`fire_orb`、`fire_meteorite`。
2. 幽冥不要走要求 `contactDamage > 0` 的接触采样。每个目标在同一个 `windowId` 上只调用一次 `NetherSeedArbiter.grant`，并传入当时全部覆盖领域。基础 5 乘主人当时的产种倍率后取整，同值用领域标识字典序较小者。周期 20 tick、第一笔在完整周期之后，由领域宿主控制。蓝火只读 `NetherContract.VISUAL_TINT`。不减速、不自建领域、不放原版火、不刷新已有燃烧时长。
3. 崩地使用 `BreakGroundShape.offsets()`。相对撞击块按先近后远排列，当前形状是 23 个方块，上限 32。水平半径 2、深度 2 的浅碗，不随攻击体积放大。世界边界、保护、掉落、统一破坏开关和实际改块都由地形作者执行。这里没有第二套开关，也没有第二个坑形。
4. 引燃、引爆和种火的资源语义已在本阶段实现。领域、轨迹和地形作者不要再写一份产种、挂层或引爆。

## 5. 资源实现与待宿主集成

已经实现的是：类型载荷、旧五元素兼容的结算字段、一次产种合并、引燃计划与轮换分账、引爆的独立消费和炉心已结算火伤、幽冥窗口仲裁，以及留焰和崩地的宿主合同。

尚未接入的是：术式注册与魔力报价、命中结果到 `commit` 的调用、武器印记保存、留焰几何、领域周期遍历、蓝火表现、崩地的权限和改块。这些都不是本阶段已经生效的游戏行为。
