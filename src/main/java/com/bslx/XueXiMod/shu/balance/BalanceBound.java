package com.bslx.XueXiMod.shu.balance;

/**
 * 含端点的有限闭区间。个别尺寸区间把下端排除，用来表示必须为正。
 * 冷却额外要求：只有原始默认值本身为 0 时，配置里的 0 才合法。
 */
public final class BalanceBound {
    public static final double MULTIPLIER_UPPER = 1000.0d;
    public static final double MANA_UPPER = 1_000_000.0d;
    public static final double TICK_UPPER = 1_200_000.0d;
    public static final double SIZE_UPPER = 512.0d;
    public static final double SPEED_UPPER = 64.0d;
    public static final double COUNT_UPPER = 10_000.0d;
    public static final double DAMAGE_AMOUNT_UPPER = 1_000_000.0d;
    /** 与护体、场地卸力生效处的 {@code 0.6f} 封顶相同。 */
    public static final double DAMAGE_REDUCTION_UPPER = (double) 0.6f;

    private final double lowerInclusive;
    private final double upperInclusive;
    private final boolean lowerExclusive;
    private final boolean zeroRequiresMatchingDefault;

    private BalanceBound(double lowerInclusive, double upperInclusive,
                         boolean lowerExclusive, boolean zeroRequiresMatchingDefault) {
        if (!Double.isFinite(lowerInclusive) || !Double.isFinite(upperInclusive)) {
            throw new BalanceDeclarationException("参数边界必须是有限数");
        }
        int compared = Double.compare(lowerInclusive, upperInclusive);
        if (lowerExclusive ? compared >= 0 : compared > 0) {
            throw new BalanceDeclarationException("参数边界的下端不能高于上端");
        }
        this.lowerInclusive = lowerInclusive;
        this.upperInclusive = upperInclusive;
        this.lowerExclusive = lowerExclusive;
        this.zeroRequiresMatchingDefault = zeroRequiresMatchingDefault;
    }

    public static BalanceBound inclusive(double lowerInclusive, double upperInclusive) {
        return new BalanceBound(lowerInclusive, upperInclusive, false, false);
    }

    public static BalanceBound multiplier() {
        return inclusive(0.0d, MULTIPLIER_UPPER);
    }

    public static BalanceBound mana() {
        return inclusive(0.0d, MANA_UPPER);
    }

    public static BalanceBound cooldownTicks() {
        return new BalanceBound(0.0d, TICK_UPPER, false, true);
    }

    public static BalanceBound positiveTicks() {
        return inclusive(1.0d, TICK_UPPER);
    }

    public static BalanceBound nonNegativeSize() {
        return inclusive(0.0d, SIZE_UPPER);
    }

    public static BalanceBound positiveSize() {
        return new BalanceBound(0.0d, SIZE_UPPER, true, false);
    }

    public static BalanceBound nonNegativeSpeed() {
        return inclusive(0.0d, SPEED_UPPER);
    }

    public static BalanceBound positiveSpeed() {
        return new BalanceBound(0.0d, SPEED_UPPER, true, false);
    }

    /** 下端排除 0，上端含入。用于必须为正、又超过正尺寸 512 的量。 */
    public static BalanceBound strictlyPositive(double upperInclusive) {
        return new BalanceBound(0.0d, upperInclusive, true, false);
    }

    public static BalanceBound damageReduction() {
        return inclusive(0.0d, DAMAGE_REDUCTION_UPPER);
    }

    public static BalanceBound unitRatio() {
        return inclusive(0.0d, 1.0d);
    }

    public static BalanceBound count(double lowerInclusive, double upperInclusive) {
        return inclusive(lowerInclusive, upperInclusive);
    }

    public static BalanceBound flatDamage() {
        return inclusive(0.0d, DAMAGE_AMOUNT_UPPER);
    }

    public double lowerInclusive() {
        return lowerInclusive;
    }

    public double upperInclusive() {
        return upperInclusive;
    }

    public boolean lowerExclusive() {
        return lowerExclusive;
    }

    public boolean zeroRequiresMatchingDefault() {
        return zeroRequiresMatchingDefault;
    }

    public boolean containsRange(double amount) {
        if (!Double.isFinite(amount)) {
            return false;
        }
        int comparedLower = Double.compare(amount, lowerInclusive);
        if (lowerExclusive) {
            if (comparedLower <= 0) {
                return false;
            }
        } else if (comparedLower < 0) {
            return false;
        }
        return Double.compare(amount, upperInclusive) <= 0;
    }

    public boolean accepts(double amount, double rawDefaultAmount) {
        if (!containsRange(amount) || !Double.isFinite(rawDefaultAmount)) {
            return false;
        }
        return !zeroRequiresMatchingDefault
                || Double.compare(amount, 0.0d) != 0
                || Double.compare(rawDefaultAmount, 0.0d) == 0;
    }

    public boolean sameRange(BalanceBound other) {
        return Double.compare(lowerInclusive, other.lowerInclusive) == 0
                && Double.compare(upperInclusive, other.upperInclusive) == 0
                && lowerExclusive == other.lowerExclusive
                && zeroRequiresMatchingDefault == other.zeroRequiresMatchingDefault;
    }

    @Override
    public String toString() {
        String lowerText = (lowerExclusive ? "(" : "[") + lowerInclusive;
        return lowerText + ", " + upperInclusive + "]";
    }
}
