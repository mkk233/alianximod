package com.bslx.XueXiMod.shu.balance;

/**
 * 三个全局倍率。默认都是 1，并且只在对应的读取入口各用一次。
 */
public final class GlobalBalance {
    public static final double IDENTITY_MULTIPLIER = 1.0d;

    private static final GlobalBalance IDENTITY = new GlobalBalance(
            IDENTITY_MULTIPLIER, IDENTITY_MULTIPLIER, IDENTITY_MULTIPLIER);

    private final double damageMultiplier;
    private final double manaMultiplier;
    private final double cooldownMultiplier;

    private GlobalBalance(double damageMultiplier, double manaMultiplier, double cooldownMultiplier) {
        this.damageMultiplier = damageMultiplier;
        this.manaMultiplier = manaMultiplier;
        this.cooldownMultiplier = cooldownMultiplier;
    }

    /** 伤害、魔力和冷却都是 1。 */
    public static GlobalBalance identity() {
        return IDENTITY;
    }

    /** 三个倍率都在允许范围内才收下。 */
    public static GlobalBalance checked(double damageMultiplier, double manaMultiplier, double cooldownMultiplier) {
        requireMultiplier(damageMultiplier, BalanceKeys.DAMAGE_MULTIPLIER);
        requireMultiplier(manaMultiplier, BalanceKeys.MANA_MULTIPLIER);
        requireMultiplier(cooldownMultiplier, BalanceKeys.COOLDOWN_MULTIPLIER);
        return new GlobalBalance(damageMultiplier, manaMultiplier, cooldownMultiplier);
    }

    private static void requireMultiplier(double multiplier, String parameterKey) {
        if (!BalanceBound.multiplier().containsRange(multiplier)) {
            throw new BalanceValueException("全局倍率超出范围: " + parameterKey);
        }
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double manaMultiplier() {
        return manaMultiplier;
    }

    public double cooldownMultiplier() {
        return cooldownMultiplier;
    }
}
