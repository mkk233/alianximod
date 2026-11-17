package com.bslx.XueXiMod.shu.technique.release.barrier;

import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;

/** 固定薄墙的声明。水幕用它，不再走球形场地。费用 getter 保持原始单价。 */
public final class BarrierDefinition {
    public static final String CURTAIN_ID = "water_curtain";
    public static final double CURTAIN_WIDTH = 3.2D;
    public static final double CURTAIN_HEIGHT = 2.8D;
    public static final double CURTAIN_THICKNESS = 0.25D;
    public static final double CURTAIN_FORWARD = 2.0D;
    public static final int CURTAIN_LIFE = 80;
    public static final int CURTAIN_SPAWN = 8;
    public static final int CURTAIN_UPKEEP = 2;
    public static final int CURTAIN_PERIOD = 20;
    public static final float CURTAIN_ATTENUATION = 0.5F;

    private final double width;
    private final double height;
    private final double thickness;
    private final double forward;
    private final int lifetimeTicks;
    private final int spawnCost;
    private final int upkeepCost;
    private final int upkeepPeriod;
    private final float attenuation;
    private final boolean deflectsProjectiles;
    private final String balanceId;

    public BarrierDefinition(double width, double height, double thickness, double forward,
                             int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod, float attenuation) {
        this(width, height, thickness, forward, lifetimeTicks, spawnCost, upkeepCost, upkeepPeriod, attenuation, false, "");
    }

    /** 普通薄墙声明。偏转默认关闭。 */
    public BarrierDefinition(double width, double height, double thickness, double forward,
                             int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod, float attenuation,
                             boolean deflectsProjectiles) {
        this(width, height, thickness, forward, lifetimeTicks, spawnCost, upkeepCost, upkeepPeriod, attenuation,
                deflectsProjectiles, "");
    }

    private BarrierDefinition(double width, double height, double thickness, double forward,
                              int lifetimeTicks, int spawnCost, int upkeepCost, int upkeepPeriod, float attenuation,
                              boolean deflectsProjectiles, String balanceId) {
        this.width = width;
        this.height = height;
        this.thickness = thickness;
        this.forward = forward;
        this.lifetimeTicks = lifetimeTicks;
        this.spawnCost = spawnCost;
        this.upkeepCost = upkeepCost;
        this.upkeepPeriod = upkeepPeriod;
        this.attenuation = attenuation;
        this.deflectsProjectiles = deflectsProjectiles;
        this.balanceId = balanceId == null ? "" : balanceId;
    }

    /** 水幕显式承接来袭弹。其它薄墙构造保持关闭。 */
    public static BarrierDefinition curtain() {
        return new BarrierDefinition(CURTAIN_WIDTH, CURTAIN_HEIGHT, CURTAIN_THICKNESS, CURTAIN_FORWARD,
                CURTAIN_LIFE, CURTAIN_SPAWN, CURTAIN_UPKEEP, CURTAIN_PERIOD, CURTAIN_ATTENUATION, true, CURTAIN_ID);
    }

    /** 登记水幕的尺寸、寿命和费用配置。不创建墙。 */
    public static void declareSpecialBalances() {
        SpecialBalance.publish(SpecialBalance.manaFee(
                SpecialBalance.manaFee(
                        SpecialBalance.ticks(
                                SpecialBalance.life(
                                        SpecialBalance.blocks(
                                                SpecialBalance.blocks(
                                                        SpecialBalance.blocks(SpecialBalance.begin(CURTAIN_ID),
                                                                BalanceKeys.BOX_WIDTH, CURTAIN_WIDTH, "水幕宽度"),
                                                        BalanceKeys.BOX_HEIGHT, CURTAIN_HEIGHT, "水幕高度"),
                                                BalanceKeys.THICKNESS, CURTAIN_THICKNESS, "水幕厚度"),
                                        CURTAIN_LIFE, "水幕寿命"),
                                BalanceKeys.UPKEEP_INTERVAL_TICKS, CURTAIN_PERIOD, "水幕维护周期"),
                        BalanceKeys.SPAWN_MANA_COST, CURTAIN_SPAWN, "水幕生成费"),
                BalanceKeys.UPKEEP_MANA_COST, CURTAIN_UPKEEP, "水幕维护费")
                .numberParameter(BalanceKeys.SPEED_DECAY, RawDefault.literal(CURTAIN_ATTENUATION),
                        BalanceBound.unitRatio(), BalanceUnit.RATIO, "水幕来袭弹削去比例"));
    }

    /** 当前宽度。有平衡编号时读配置。 */
    public double width() {
        return sized(BalanceKeys.BOX_WIDTH, width);
    }

    /** 当前高度。有平衡编号时读配置。 */
    public double height() {
        return sized(BalanceKeys.BOX_HEIGHT, height);
    }

    /** 当前厚度。有平衡编号时读配置。 */
    public double thickness() {
        return sized(BalanceKeys.THICKNESS, thickness);
    }

    /** 墙放在身前的固定距离。链聚能不改它。 */
    public double forward() {
        return forward;
    }

    /** 墙寿命。有平衡编号时读配置。 */
    public int lifetimeTicks() {
        if (balanceId.isEmpty()) {
            return lifetimeTicks;
        }
        return SpecialBalance.integer(balanceId, BalanceKeys.LIFETIME_TICKS, lifetimeTicks);
    }

    /** 原始生成单价。调用方另按份数计算总账。 */
    public int spawnCost() {
        return spawnCost;
    }

    /** 原始维护单价。 */
    public int upkeepCost() {
        return upkeepCost;
    }

    /** 维护周期。有平衡编号时读配置。 */
    public int upkeepPeriod() {
        if (balanceId.isEmpty()) {
            return upkeepPeriod;
        }
        return SpecialBalance.integer(balanceId, BalanceKeys.UPKEEP_INTERVAL_TICKS, upkeepPeriod);
    }

    /** 穿过墙时削去的速度比例。 */
    public float attenuation() {
        if (balanceId.isEmpty()) {
            return attenuation;
        }
        return SpecialBalance.decimal(balanceId, BalanceKeys.SPEED_DECAY, attenuation);
    }

    private double sized(String parameterKey, double rawSize) {
        if (balanceId.isEmpty()) {
            return rawSize;
        }
        return SpecialBalance.number(balanceId, parameterKey, rawSize);
    }

    /** 这面墙是否承接来袭弹。 */
    public boolean deflectsProjectiles() {
        return deflectsProjectiles;
    }

    /** 平衡配置编号。普通薄墙为空。 */
    public String balanceId() {
        return balanceId;
    }

    /** 新的一份墙。宽、高、厚和有限正寿命随链聚能放大，前向距离和费用不改。原对象不改。 */
    public BarrierDefinition withInput(ReleaseInput input) {
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        double factor = frozen.sizeMultiplier();
        return new BarrierDefinition(width() * factor, height() * factor, thickness() * factor, forward,
                frozen.scaleDuration(lifetimeTicks()), spawnCost, upkeepCost, upkeepPeriod(), attenuation(),
                deflectsProjectiles, "");
    }
}
