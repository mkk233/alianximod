package com.bslx.XueXiMod.shu.technique.release.guard;

import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;

/**
 * 随身护体的参数。没有声明护体的术式保持原样。
 * 类型键用来认同一类护体，公共入口不按术式编号分支。
 * 这是参数合同，实例寿命由护体状态管理；领域用固定盒子，球形场地用锚点球。
 */
public final class GuardDefinition {
    public static final String RING_ID = "water_ring";
    public static final int RING_LIFE = 200;
    public static final int RING_SPAWN = 8;
    public static final int RING_UPKEEP = 2;
    public static final int RING_PERIOD = 20;
    public static final float RING_UNLOAD = 0.20F;
    public static final double RING_RADIUS = 0.9D;
    public static final double RING_BAND = 0.12D;
    public static final double RING_HEIGHT = 0.55D;

    private final String typeKey;
    private final int lifetimeTicks;
    private final int spawnCost;
    private final int upkeepCost;
    private final int upkeepPeriod;
    private final float unloadRatio;
    private final double radius;
    private final double band;
    private final double bodyHeight;
    private final float baseTilt;
    private final boolean deflectsProjectiles;
    private final String balanceId;

    /** 建不偏转弹体的护体参数。数值原样保存，不扣生成费。 */
    public GuardDefinition(String typeKey, int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod,
                           float unloadRatio, double radius, double band, double bodyHeight, float baseTilt) {
        this(typeKey, lifetimeTicks, spawnCost, upkeepCost, upkeepPeriod, unloadRatio, radius, band, bodyHeight, baseTilt, false, "");
    }

    /** 建可选择是否偏转弹体的护体参数。不绑定平衡编号，不扣资源。 */
    public GuardDefinition(String typeKey, int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod,
                           float unloadRatio, double radius, double band, double bodyHeight, float baseTilt,
                           boolean deflectsProjectiles) {
        this(typeKey, lifetimeTicks, spawnCost, upkeepCost, upkeepPeriod, unloadRatio, radius, band, bodyHeight, baseTilt,
                deflectsProjectiles, "");
    }

    private GuardDefinition(String typeKey, int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod,
                            float unloadRatio, double radius, double band, double bodyHeight, float baseTilt,
                            boolean deflectsProjectiles, String balanceId) {
        this.typeKey = typeKey;
        this.lifetimeTicks = lifetimeTicks;
        this.spawnCost = spawnCost;
        this.upkeepCost = upkeepCost;
        this.upkeepPeriod = upkeepPeriod;
        this.unloadRatio = unloadRatio;
        this.radius = radius;
        this.band = band;
        this.bodyHeight = bodyHeight;
        this.baseTilt = baseTilt;
        this.deflectsProjectiles = deflectsProjectiles;
        this.balanceId = balanceId == null ? "" : balanceId;
    }

    /** 水环显式承接来袭弹。其它护体构造保持关闭。 */
    public static GuardDefinition personalRing() {
        return new GuardDefinition("personal_ring", RING_LIFE, RING_SPAWN, RING_UPKEEP, RING_PERIOD, RING_UNLOAD,
                RING_RADIUS, RING_BAND, RING_HEIGHT, 0f, true, RING_ID);
    }

    /** 登记水环的尺寸、寿命、周期和两份法力参数。只发布声明，不生成护体。 */
    public static void declareSpecialBalances() {
        SpecialBalance.publish(SpecialBalance.manaFee(
                SpecialBalance.manaFee(
                        SpecialBalance.ticks(
                                SpecialBalance.life(
                                        SpecialBalance.blocks(
                                                SpecialBalance.blocks(
                                                        SpecialBalance.blocks(SpecialBalance.begin(RING_ID),
                                                                BalanceKeys.RADIUS, RING_RADIUS, "水环半径"),
                                                        BalanceKeys.THICKNESS, RING_BAND, "水环带宽"),
                                                BalanceKeys.BOX_HEIGHT, RING_HEIGHT, "水环身高"),
                                        RING_LIFE, "水环寿命"),
                                BalanceKeys.UPKEEP_INTERVAL_TICKS, RING_PERIOD, "水环维护周期"),
                        BalanceKeys.SPAWN_MANA_COST, RING_SPAWN, "水环生成费"),
                BalanceKeys.UPKEEP_MANA_COST, RING_UPKEEP, "水环维护费")
                .numberParameter(BalanceKeys.DAMAGE_REDUCTION, RawDefault.literal(RING_UNLOAD),
                        BalanceBound.damageReduction(), BalanceUnit.RATIO, "水环卸伤比例"));
    }

    public String typeKey() {
        return typeKey;
    }

    /** 读取寿命。绑定平衡时改读配置，否则用原值。 */
    public int lifetimeTicks() {
        return balanceId.isEmpty() ? lifetimeTicks : SpecialBalance.integer(balanceId, BalanceKeys.LIFETIME_TICKS, lifetimeTicks);
    }

    public int spawnCost() {
        return spawnCost;
    }

    public int upkeepCost() {
        return upkeepCost;
    }

    /** 读取维护周期。绑定平衡时改读配置，否则用原值。 */
    public int upkeepPeriod() {
        return balanceId.isEmpty()
                ? upkeepPeriod
                : SpecialBalance.integer(balanceId, BalanceKeys.UPKEEP_INTERVAL_TICKS, upkeepPeriod);
    }

    /** 读取卸伤比例。绑定平衡时改读配置，否则用原值。 */
    public float unloadRatio() {
        return balanceId.isEmpty()
                ? unloadRatio
                : SpecialBalance.decimal(balanceId, BalanceKeys.DAMAGE_REDUCTION, unloadRatio);
    }

    /** 读取环半径。绑定平衡时改读配置，否则用原值。 */
    public double radius() {
        return sized(BalanceKeys.RADIUS, radius);
    }

    /** 读取环带宽。绑定平衡时改读配置，否则用原值。 */
    public double band() {
        return sized(BalanceKeys.THICKNESS, band);
    }

    /** 读取环身高。绑定平衡时改读配置，否则用原值。 */
    public double bodyHeight() {
        return sized(BalanceKeys.BOX_HEIGHT, bodyHeight);
    }

    public String balanceId() {
        return balanceId;
    }

    private double sized(String parameterKey, double rawSize) {
        return balanceId.isEmpty() ? rawSize : SpecialBalance.number(balanceId, parameterKey, rawSize);
    }

    public float baseTilt() {
        return baseTilt;
    }

    public boolean deflectsProjectiles() {
        return deflectsProjectiles;
    }

    /** 新的一份参数。半径和带宽随链聚能放大，寿命只延长有限正值。原对象不改。 */
    public GuardDefinition withInput(ReleaseInput input) {
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        double factor = frozen.sizeMultiplier();
        return new GuardDefinition(typeKey, frozen.scaleDuration(lifetimeTicks()), spawnCost, upkeepCost, upkeepPeriod(),
                unloadRatio(), radius() * factor, band() * factor, bodyHeight(), baseTilt, deflectsProjectiles);
    }
}
