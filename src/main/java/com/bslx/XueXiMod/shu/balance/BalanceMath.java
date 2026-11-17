package com.bslx.XueXiMod.shu.balance;

/**
 * 伤害、费用和冷却的舍入。倍率恰好为 1 时不做第二轮舍入。
 */
final class BalanceMath {
    private BalanceMath() {
    }

    static boolean isOne(double multiplier) {
        return Double.compare(multiplier, 1.0d) == 0;
    }

    static boolean isIntegral(double amount) {
        if (!Double.isFinite(amount)) {
            return false;
        }
        double nearest = Math.rint(amount);
        return Math.abs(amount - nearest) <= 1.0e-6d
                && nearest >= Integer.MIN_VALUE
                && nearest <= Integer.MAX_VALUE;
    }

    static int toIntExact(double amount) {
        if (!isIntegral(amount)) {
            throw new BalanceValueException("需要整数参数，实际得到 " + amount);
        }
        return (int) Math.rint(amount);
    }

    static float preserveDamage(double rawAmount) {
        if (!Double.isFinite(rawAmount) || Double.compare(rawAmount, 0.0d) < 0) {
            throw new BalanceValueException("原始伤害贡献必须是有限非负数");
        }
        float narrowed = (float) rawAmount;
        if (!Float.isFinite(narrowed) || Float.compare(narrowed, 0.0f) < 0) {
            throw new BalanceValueException("原始伤害贡献超出浮点范围");
        }
        return narrowed;
    }

    static float scaleDamage(double rawAmount, double techniqueMultiplier, double globalMultiplier) {
        if (!Double.isFinite(techniqueMultiplier) || !Double.isFinite(globalMultiplier)
                || Double.compare(techniqueMultiplier, 0.0d) < 0
                || Double.compare(globalMultiplier, 0.0d) < 0) {
            throw new BalanceValueException("伤害倍率必须是有限非负数");
        }
        if (isOne(techniqueMultiplier) && isOne(globalMultiplier)) {
            return preserveDamage(rawAmount);
        }
        if (!Double.isFinite(rawAmount) || Double.compare(rawAmount, 0.0d) < 0) {
            throw new BalanceValueException("原始伤害贡献必须是有限非负数");
        }
        double scaled = rawAmount * techniqueMultiplier * globalMultiplier;
        if (!Double.isFinite(scaled) || scaled > 1_000_000_000.0d) {
            throw new BalanceValueException("伤害贡献缩放溢出");
        }
        double shifted = scaled * 10000.0d;
        if (!Double.isFinite(shifted)) {
            throw new BalanceValueException("伤害贡献缩放溢出");
        }
        double rounded = Math.floor(shifted + 0.5d);
        float narrowed = (float) (rounded / 10000.0d);
        if (!Float.isFinite(narrowed) || Float.compare(narrowed, 0.0f) < 0) {
            throw new BalanceValueException("伤害贡献缩放溢出");
        }
        return narrowed;
    }

    static int scaleMana(int rawMana, double multiplier) {
        if (rawMana < 0) {
            throw new BalanceValueException("原始魔力不能为负");
        }
        if (isOne(multiplier)) {
            return rawMana;
        }
        if (!Double.isFinite(multiplier) || Double.compare(multiplier, 0.0d) < 0) {
            throw new BalanceValueException("全局魔力倍率必须是有限非负数");
        }
        return scaleNonNegativeInt(rawMana * multiplier, "魔力");
    }

    static int scaleCooldown(int rawTicks, double multiplier) {
        if (rawTicks < 0) {
            throw new BalanceValueException("原始冷却不能为负");
        }
        if (rawTicks == 0 || isOne(multiplier)) {
            return rawTicks;
        }
        if (!Double.isFinite(multiplier) || Double.compare(multiplier, 0.0d) < 0) {
            throw new BalanceValueException("全局冷却倍率必须是有限非负数");
        }
        if (Double.compare(multiplier, 0.0d) == 0) {
            return 0;
        }
        int scaled = scaleNonNegativeInt(rawTicks * multiplier, "冷却");
        return scaled < 1 ? 1 : scaled;
    }

    private static int scaleNonNegativeInt(double scaled, String label) {
        if (!Double.isFinite(scaled) || Double.compare(scaled, 0.0d) < 0) {
            throw new BalanceValueException(label + "缩放溢出");
        }
        double rounded = Math.floor(scaled + 0.5d);
        if (rounded > Integer.MAX_VALUE) {
            throw new BalanceValueException(label + "缩放溢出");
        }
        return (int) rounded;
    }
}
