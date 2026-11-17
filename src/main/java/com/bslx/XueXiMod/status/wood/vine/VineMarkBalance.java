package com.bslx.XueXiMod.status.wood.vine;

import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.shu.balance.RawDamage;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;
import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;

/**
 * 缠枝专项键。伤害只在创建状态时乘一次术式与全局倍率。
 */
public final class VineMarkBalance {
    public static final String TECHNIQUE_ID = SpecialBalance.VINE_MARK;

    public static final String CONTROL_TICKS = SpecialBalance.CONTROL_TICKS;
    public static final String BUD_CONTROL_TICKS = "budControlTicks";
    public static final String BRANCH_LIFETIME_TICKS = "branchLifetimeTicks";
    public static final String BUD_LIFETIME_TICKS = "budLifetimeTicks";
    public static final String RESIDUAL_PROTECTION_TICKS = "residualProtectionTicks";
    public static final String MOVEMENT_INTERVAL_TICKS = "movementIntervalTicks";
    public static final String MOVEMENT_DAMAGE_PER_BRANCH_PER_BLOCK = "movementDamagePerBranchPerBlock";
    public static final String BLOOM_FLAT_DAMAGE = "bloomFlatDamage";
    public static final String BLEED_DURATION_TICKS = "bleedDurationTicks";
    public static final String BLEED_FLAT_DAMAGE = "bleedFlatDamage";
    public static final String BLEED_INTERVAL_TICKS = "bleedIntervalTicks";
    public static final String BLEED_ENHANCED_INTERVAL_TICKS = "bleedEnhancedIntervalTicks";
    public static final String SPECTRUM_DURATION_TICKS = "spectrumDurationTicks";
    public static final String FALLEN_LIFETIME_TICKS = "fallenLifetimeTicks";
    public static final String FALLEN_UNITS = "fallenUnits";
    public static final String FALLEN_CONSUME_UNITS = "fallenConsumeUnits";
    public static final String FALLEN_SEARCH_RADIUS = "fallenSearchRadius";
    public static final String FALLEN_TARGET_RANGE = "fallenTargetRange";
    public static final String FALLEN_SPEED = "fallenSpeed";
    public static final String FALLEN_FLIGHT_TICKS = "fallenFlightTicks";

    public static final int DEFAULT_CONTROL_TICKS = 10;
    public static final int DEFAULT_BUD_CONTROL_TICKS = 60;
    public static final int DEFAULT_BRANCH_LIFETIME_TICKS = 120;
    public static final int DEFAULT_BUD_LIFETIME_TICKS = 120;
    public static final int DEFAULT_RESIDUAL_PROTECTION_TICKS = 80;
    public static final int DEFAULT_MOVEMENT_INTERVAL_TICKS = 20;
    public static final double DEFAULT_MOVEMENT_DAMAGE_PER_BRANCH_PER_BLOCK = 0.5d;
    public static final double DEFAULT_BLOOM_FLAT_DAMAGE = 4.0d;
    public static final int DEFAULT_BLEED_DURATION_TICKS = 300;
    public static final double DEFAULT_BLEED_FLAT_DAMAGE = 1.0d;
    public static final int DEFAULT_BLEED_INTERVAL_TICKS = 20;
    public static final int DEFAULT_BLEED_ENHANCED_INTERVAL_TICKS = 10;
    public static final int DEFAULT_SPECTRUM_DURATION_TICKS = 600;
    public static final int DEFAULT_FALLEN_LIFETIME_TICKS = 120;
    public static final int DEFAULT_FALLEN_UNITS = 3;
    public static final int DEFAULT_FALLEN_CONSUME_UNITS = 1;
    public static final double DEFAULT_FALLEN_SEARCH_RADIUS = 8.0d;
    public static final double DEFAULT_FALLEN_TARGET_RANGE = 12.0d;
    public static final double DEFAULT_FALLEN_SPEED = 0.8d;
    public static final int DEFAULT_FALLEN_FLIGHT_TICKS = 30;
    public static final int FALLEN_POTENTIAL_PER_UNIT = 20000;

    private VineMarkBalance() {
    }

    public static void declare(BalanceRegistry registry) {
        registry.acceptSpecial(TechniqueBalanceSpec.builder(TECHNIQUE_ID)
                .integerParameter(CONTROL_TICKS, RawDefault.literal(DEFAULT_CONTROL_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS,
                        "第一枝束缚持续刻数。不再乘旧木元素持续反应。")
                .integerParameter(BUD_CONTROL_TICKS, RawDefault.literal(DEFAULT_BUD_CONTROL_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "含苞束缚持续刻数。")
                .integerParameter(BRANCH_LIFETIME_TICKS, RawDefault.literal(DEFAULT_BRANCH_LIFETIME_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "一二枝自最后成功插枝起的时限。")
                .integerParameter(BUD_LIFETIME_TICKS, RawDefault.literal(DEFAULT_BUD_LIFETIME_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "含苞独立时限。")
                .integerParameter(RESIDUAL_PROTECTION_TICKS, RawDefault.literal(DEFAULT_RESIDUAL_PROTECTION_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "催花后残枝保护时限。")
                .integerParameter(MOVEMENT_INTERVAL_TICKS, RawDefault.literal(DEFAULT_MOVEMENT_INTERVAL_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "移动刺伤结算间隔。")
                .numberParameter(MOVEMENT_DAMAGE_PER_BRANCH_PER_BLOCK,
                        RawDefault.literal(DEFAULT_MOVEMENT_DAMAGE_PER_BRANCH_PER_BLOCK),
                        BalanceBound.flatDamage(), BalanceUnit.DAMAGE, "每格每枝的移动刺伤原始金额。")
                .numberParameter(BLOOM_FLAT_DAMAGE, RawDefault.literal(DEFAULT_BLOOM_FLAT_DAMAGE),
                        BalanceBound.flatDamage(), BalanceUnit.DAMAGE, "催花抽枝的固定伤害。")
                .integerParameter(BLEED_DURATION_TICKS, RawDefault.literal(DEFAULT_BLEED_DURATION_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "流血持续刻数。")
                .numberParameter(BLEED_FLAT_DAMAGE, RawDefault.literal(DEFAULT_BLEED_FLAT_DAMAGE),
                        BalanceBound.flatDamage(), BalanceUnit.DAMAGE, "流血每次扣血。")
                .integerParameter(BLEED_INTERVAL_TICKS, RawDefault.literal(DEFAULT_BLEED_INTERVAL_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "一级流血扣血间隔。")
                .integerParameter(BLEED_ENHANCED_INTERVAL_TICKS,
                        RawDefault.literal(DEFAULT_BLEED_ENHANCED_INTERVAL_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "二级流血扣血间隔。")
                .integerParameter(SPECTRUM_DURATION_TICKS, RawDefault.literal(DEFAULT_SPECTRUM_DURATION_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "野蔷薇花谱持续刻数。")
                .integerParameter(FALLEN_LIFETIME_TICKS, RawDefault.literal(DEFAULT_FALLEN_LIFETIME_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "落英地面寿命。")
                .integerParameter(FALLEN_UNITS, RawDefault.literal(DEFAULT_FALLEN_UNITS),
                        BalanceBound.count(1.0d, 256.0d), BalanceUnit.COUNT, "一批落英的有效单位数。")
                .integerParameter(FALLEN_CONSUME_UNITS, RawDefault.literal(DEFAULT_FALLEN_CONSUME_UNITS),
                        BalanceBound.count(1.0d, 256.0d), BalanceUnit.COUNT, "一次主动调动消费的单位数。")
                .numberParameter(FALLEN_SEARCH_RADIUS, RawDefault.literal(DEFAULT_FALLEN_SEARCH_RADIUS),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "选取本人落英的半径。")
                .numberParameter(FALLEN_TARGET_RANGE, RawDefault.literal(DEFAULT_FALLEN_TARGET_RANGE),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "准星选取流血目标的距离。")
                .numberParameter(FALLEN_SPEED, RawDefault.literal(DEFAULT_FALLEN_SPEED),
                        BalanceBound.inclusive(0.0d, 64.0d), BalanceUnit.BLOCKS_PER_TICK, "枝条飞行速度。")
                .integerParameter(FALLEN_FLIGHT_TICKS, RawDefault.literal(DEFAULT_FALLEN_FLIGHT_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "枝条飞行寿命。")
                .build());
    }

    public static int controlTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, CONTROL_TICKS);
    }

    public static int budControlTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, BUD_CONTROL_TICKS);
    }

    public static int branchLifetimeTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, BRANCH_LIFETIME_TICKS);
    }

    public static int budLifetimeTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, BUD_LIFETIME_TICKS);
    }

    public static int residualProtectionTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, RESIDUAL_PROTECTION_TICKS);
    }

    public static int movementIntervalTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, MOVEMENT_INTERVAL_TICKS);
    }

    public static float movementDamagePerBranchPerBlock() {
        return scaledDamage(BalanceService.number(TECHNIQUE_ID, MOVEMENT_DAMAGE_PER_BRANCH_PER_BLOCK));
    }

    public static float bloomFlatDamage() {
        return scaledDamage(BalanceService.number(TECHNIQUE_ID, BLOOM_FLAT_DAMAGE));
    }

    public static int bleedDurationTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, BLEED_DURATION_TICKS);
    }

    public static float bleedFlatDamage() {
        return scaledDamage(BalanceService.number(TECHNIQUE_ID, BLEED_FLAT_DAMAGE));
    }

    public static int bleedIntervalTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, BLEED_INTERVAL_TICKS);
    }

    public static int bleedEnhancedIntervalTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, BLEED_ENHANCED_INTERVAL_TICKS);
    }

    public static int spectrumDurationTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, SPECTRUM_DURATION_TICKS);
    }

    public static int fallenLifetimeTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, FALLEN_LIFETIME_TICKS);
    }

    public static int fallenUnits() {
        return BalanceService.intValue(TECHNIQUE_ID, FALLEN_UNITS);
    }

    public static int fallenConsumeUnits() {
        return BalanceService.intValue(TECHNIQUE_ID, FALLEN_CONSUME_UNITS);
    }

    public static double fallenSearchRadius() {
        return BalanceService.number(TECHNIQUE_ID, FALLEN_SEARCH_RADIUS);
    }

    public static double fallenTargetRange() {
        return BalanceService.number(TECHNIQUE_ID, FALLEN_TARGET_RANGE);
    }

    public static double fallenSpeed() {
        return BalanceService.number(TECHNIQUE_ID, FALLEN_SPEED);
    }

    public static int fallenFlightTicks() {
        return BalanceService.intValue(TECHNIQUE_ID, FALLEN_FLIGHT_TICKS);
    }

    private static float scaledDamage(double rawAmount) {
        ConfiguredDamage configured = BalanceService.damageContribution(TECHNIQUE_ID, RawDamage.of(rawAmount));
        return configured.amount();
    }
}
