package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

import net.minecraft.util.Mth;

import java.util.function.IntUnaryOperator;

/**
 * 按原版 ceil 和暴击公式抽一次。领域加值是浮点，之后的永久削弱不再截成整数。
 */
public final class ArrowDamageFreeze {
    private ArrowDamageFreeze() {
    }

    /** 按原版速度和基础伤害取向上取整后的基数。 */
    public static int ceilBase(double speed, double baseDamage) {
        double product = speed * baseDamage;
        double clamped = Mth.clamp(product, 0.0D, (double) Integer.MAX_VALUE);
        return Mth.ceil(clamped);
    }

    /** 把暴击加值加到基数上，并停在整数上界。 */
    public static int applyCrit(int base, int extra) {
        return (int) Math.min((long) extra + (long) base, 2147483647L);
    }

    /** 抽一次暴击并加上领域浮点加值。 */
    public static float freeze(double speed, double baseDamage, boolean crit, IntUnaryOperator roller, float auraBonus) {
        int base = ceilBase(speed, baseDamage);
        int rolled = base;
        if (crit) {
            int bound = base / 2 + 2;
            int extra = roller.applyAsInt(bound);
            rolled = applyCrit(base, extra);
        }
        float bonus = Float.isFinite(auraBonus) ? auraBonus : 0f;
        return rolled + bonus;
    }
}
