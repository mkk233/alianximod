package com.bslx.XueXiMod.status.fire.seed;

/**
 * 火种纯运算。余额钳在 0–100，档位是 floor(S/10)。
 * 旧档案默认只花整档，余数留给实体；全部消费由调用方另传请求点数。
 */
public final class FireSeedMath {
    public static final int UPPER_BOUND = 100;
    public static final int POINTS_PER_TIER = 10;

    private FireSeedMath() {
    }

    /** 夹到 0 以上、{@link #UPPER_BOUND} 以下。 */
    public static int clamp(int amount) {
        if (amount <= 0) {
            return 0;
        }
        if (amount >= UPPER_BOUND) {
            return UPPER_BOUND;
        }
        return amount;
    }

    /** 完整档数。 */
    public static int tiers(int amount) {
        return clamp(amount) / POINTS_PER_TIER;
    }

    /** 不满一整档的余数。 */
    public static int remainder(int amount) {
        return clamp(amount) % POINTS_PER_TIER;
    }

    /** 旧档案应扣除的点数。没有完整档时为 0，调用方不得据此标记消费。 */
    public static int wholeTierSpend(int amount) {
        return tiers(amount) * POINTS_PER_TIER;
    }

    /** 持续档换成刻。一档 {@link #POINTS_PER_TIER} 刻。 */
    public static int sustainTicks(int tiers) {
        if (tiers <= 0) {
            return 0;
        }
        long ticks = (long) tiers * (long) POINTS_PER_TIER;
        if (ticks > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) ticks;
    }

    /** 是不是有限且不小于 0。 */
    public static boolean finiteNonNegative(double sample) {
        return Double.isFinite(sample) && sample >= 0.0D;
    }

    /**
     * floor(P×M)。P 为 0 时结果为 0。非有限或非正倍率视为不能生产。
     * 超过上限的请求压到上限，真正入账仍由余额再钳一次。
     */
    public static int floorProduction(int baseAmount, double productionMultiplier) {
        if (baseAmount <= 0 || !finiteNonNegative(productionMultiplier) || productionMultiplier == 0.0D) {
            return 0;
        }
        double product = (double) baseAmount * productionMultiplier;
        if (!Double.isFinite(product) || product < 1.0D) {
            return 0;
        }
        if (product >= UPPER_BOUND) {
            return UPPER_BOUND;
        }
        return (int) Math.floor(product);
    }

    /**
     * 本次可投入的点数。{@link Integer#MAX_VALUE} 表示全部余额；
     * 其他非负请求取与余额的较小值。负数不是全部消费。
     */
    public static int invested(int balance, int requested) {
        int clamped = clamp(balance);
        if (requested < 0) {
            return 0;
        }
        if (requested == Integer.MAX_VALUE || requested > clamped) {
            return clamped;
        }
        return requested;
    }

    /**
     * 新基数先 floor(基数 × 倍率)，旧固定点数再加到取整结果上。
     * 余额上限不在这里钳，由入账时统一钳到 100。负数输入视为不能合并。
     */
    public static int mergedRequest(int scaledBase, int legacyPoints, double multiplier) {
        if (scaledBase < 0 || legacyPoints < 0) {
            return 0;
        }
        int scaled = floorProduction(scaledBase, multiplier);
        long combined = (long) scaled + (long) legacyPoints;
        if (combined <= 0L) {
            return 0;
        }
        if (combined > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) combined;
    }
}
