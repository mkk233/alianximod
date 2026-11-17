package com.bslx.XueXiMod.shu.technique.release;

/**
 * 链上聚能的冻结输入。只保存链聚能倍率，不含术式自身能量、附魔半径或操作身份。
 */
public record ReleaseInput(float chainEnergyMultiplier) {
    public static final ReleaseInput BASE = new ReleaseInput(1f);

    /** 有效链聚能。非有限或小于等于 0 时返回 1。 */
    public float sizeMultiplier() {
        float multiplier = this.chainEnergyMultiplier;
        if (!Float.isFinite(multiplier) || multiplier <= 0f) {
            return 1f;
        }
        return multiplier;
    }

    /**
     * 有限正寿命按有效倍率做 double 乘法后向上取整，再钳到 1 与 Integer.MAX_VALUE。
     * 小于等于 0 的原值保持不变。
     */
    public int scaleDuration(int baseTicks) {
        if (baseTicks <= 0) {
            return baseTicks;
        }
        double scaledTicks = Math.ceil((double) baseTicks * (double) this.sizeMultiplier());
        if (Double.isNaN(scaledTicks)) {
            return 1;
        }
        if (scaledTicks >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (scaledTicks <= 1d) {
            return 1;
        }
        return (int) scaledTicks;
    }

    /** 有限正寿命的 int 到期刻。按 Java int 环绕相加，不钳制。 */
    public static int expiryTick(int nowTick, int durationTicks) {
        return nowTick + durationTicks;
    }

    /** 当前刻是否已到到期刻。用 int 差值，避免直接比较两个可能环绕的刻。 */
    public static boolean hasExpired(int nowTick, int expiryTick) {
        return nowTick - expiryTick >= 0;
    }

    /**
     * 长游戏时间的到期点。非负寿命相加会超过 long 上界时返回 Long.MAX_VALUE，否则相加。
     */
    public static long expiryGameTime(long now, long durationTicks) {
        if (durationTicks >= 0L && now > Long.MAX_VALUE - durationTicks) {
            return Long.MAX_VALUE;
        }
        return now + durationTicks;
    }
}
