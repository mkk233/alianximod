package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDamage;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;

/**
 * B 的专项读取。未封存或该键尚未声明时退回调用点传入的原始默认，注册和封存计算器因此不会读到有效快照。
 * 已声明的键在玩法读取时走当前侧快照。魔力读取会乘一次全局魔力倍率，伤害读取走一次 damageContribution。
 */
public final class SpecialBalance {
    private SpecialBalance() {
    }

    /** 未封存或键未声明时退回原始整数。 */
    public static int integer(String techniqueId, String parameterKey, int rawInteger) {
        if (!active(techniqueId, parameterKey)) {
            return rawInteger;
        }
        BalanceSide side = readableSide();
        if (side == null) {
            return BalanceService.intValue(techniqueId, parameterKey);
        }
        return BalanceService.intValue(side, techniqueId, parameterKey);
    }

    /** 未封存或键未声明时退回原始小数。 */
    public static double number(String techniqueId, String parameterKey, double rawNumber) {
        if (!active(techniqueId, parameterKey)) {
            return rawNumber;
        }
        BalanceSide side = readableSide();
        if (side == null) {
            return BalanceService.number(techniqueId, parameterKey);
        }
        return BalanceService.number(side, techniqueId, parameterKey);
    }

    /** 与 number 相同，结果收成 float。 */
    public static float decimal(String techniqueId, String parameterKey, float rawNumber) {
        return (float) number(techniqueId, parameterKey, rawNumber);
    }

    /** 单价。调用方再按份数、成功目标数或维护次数去乘。退款退这次算出的金额，不要再读一次。 */
    public static int mana(String techniqueId, String parameterKey, int rawMana) {
        if (!active(techniqueId, parameterKey)) {
            return rawMana;
        }
        BalanceSide side = readableSide();
        if (side == null) {
            return BalanceService.manaCost(techniqueId, parameterKey, rawMana);
        }
        return BalanceService.manaCost(side, techniqueId, parameterKey, rawMana);
    }

    /**
     * 原公式已经算完的一笔贡献。没有伤害倍率声明时保持原 float 收窄，避免水牢或潮印在 A 补标准键前直接抛掉。
     * 已声明时只转换这一次，调用方保存或结算的是返回值。
     */
    public static float contributed(String techniqueId, double rawAmount) {
        if (!Double.isFinite(rawAmount) || rawAmount < 0.0d) {
            return 0.0f;
        }
        if (!active(techniqueId, BalanceKeys.DAMAGE_MULTIPLIER)) {
            return (float) rawAmount;
        }
        BalanceSide side = readableSide();
        if (side == null) {
            return BalanceService.damageContribution(techniqueId, RawDamage.of(rawAmount)).amount();
        }
        return BalanceService.damageContribution(side, techniqueId, RawDamage.of(rawAmount)).amount();
    }

    /** 开始一条专项声明。 */
    public static TechniqueBalanceSpec.Builder begin(String techniqueId) {
        return TechniqueBalanceSpec.builder(techniqueId);
    }

    /** 把这条专项声明交给平衡注册表。 */
    public static void publish(TechniqueBalanceSpec.Builder builder) {
        BalanceRegistry.declareSpecial(builder.build());
    }

    /** 给声明加上存活刻数。 */
    public static TechniqueBalanceSpec.Builder life(TechniqueBalanceSpec.Builder builder, int rawLife, String description) {
        return builder.integerParameter(BalanceKeys.LIFETIME_TICKS, RawDefault.literal(rawLife),
                BalanceBound.positiveTicks(), BalanceUnit.TICKS, description);
    }

    /** 给声明加上运动速度。 */
    public static TechniqueBalanceSpec.Builder speed(TechniqueBalanceSpec.Builder builder, float rawSpeed, String description) {
        return builder.numberParameter(BalanceKeys.MOTION_SPEED, RawDefault.literal(rawSpeed),
                BalanceBound.positiveSpeed(), BalanceUnit.BLOCKS_PER_TICK, description);
    }

    /** 给声明加上厚度。 */
    public static TechniqueBalanceSpec.Builder thickness(TechniqueBalanceSpec.Builder builder, float rawThickness, String description) {
        return builder.numberParameter(BalanceKeys.THICKNESS, RawDefault.literal(rawThickness),
                BalanceBound.positiveSize(), BalanceUnit.BLOCKS, description);
    }

    /** 给声明加上一个正尺寸。 */
    public static TechniqueBalanceSpec.Builder blocks(TechniqueBalanceSpec.Builder builder, String parameterKey,
                                                       double rawSize, String description) {
        return builder.numberParameter(parameterKey, RawDefault.literal(rawSize),
                BalanceBound.positiveSize(), BalanceUnit.BLOCKS, description);
    }

    /** 给声明加上一个正刻数。 */
    public static TechniqueBalanceSpec.Builder ticks(TechniqueBalanceSpec.Builder builder, String parameterKey,
                                                      int rawTicks, String description) {
        return builder.integerParameter(parameterKey, RawDefault.literal(rawTicks),
                BalanceBound.positiveTicks(), BalanceUnit.TICKS, description);
    }

    /** 给声明加上一项魔力费用。 */
    public static TechniqueBalanceSpec.Builder manaFee(TechniqueBalanceSpec.Builder builder, String parameterKey,
                                                        int rawMana, String description) {
        return builder.manaFee(parameterKey, RawDefault.literal(rawMana), description);
    }

    private static boolean active(String techniqueId, String parameterKey) {
        return techniqueId != null && !techniqueId.isEmpty()
                && BalanceRegistry.isSealed()
                && BalanceRegistry.isDeclared(techniqueId, parameterKey);
    }

    /**
     * 刻线程已绑定时用绑定侧。两侧都未安装时让无侧读取返回代码默认。
     * 只有一侧安装时用那一侧。集成服务器两侧都在、当前线程未绑定时用服务端快照，避免渲染线程抛未绑定。
     */
    private static BalanceSide readableSide() {
        BalanceSide bound = BalanceService.boundSide();
        if (bound != null) {
            return bound;
        }
        boolean serverReady = BalanceService.installed(BalanceSide.SERVER) != null;
        boolean clientReady = BalanceService.installed(BalanceSide.CLIENT) != null;
        if (serverReady && !clientReady) {
            return BalanceSide.SERVER;
        }
        if (clientReady && !serverReady) {
            return BalanceSide.CLIENT;
        }
        if (serverReady) {
            return BalanceSide.SERVER;
        }
        return null;
    }
}
