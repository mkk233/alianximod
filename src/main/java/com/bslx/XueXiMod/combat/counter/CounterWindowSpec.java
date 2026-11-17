package com.bslx.XueXiMod.combat.counter;

/**
 * 一次迎击的已结算参数。窗口时长为正，减伤比例落在 [0, 1)，反击伤害为有限非负数。
 */
public final class CounterWindowSpec {
    private final int windowTicks;
    private final float reductionRatio;
    private final float counterDamage;
    private final CounterAdmission admission;

    private CounterWindowSpec(int windowTicks, float reductionRatio, float counterDamage, CounterAdmission admission) {
        this.windowTicks = windowTicks;
        this.reductionRatio = reductionRatio;
        this.counterDamage = counterDamage;
        this.admission = admission;
    }

    public static CounterWindowSpec tryOf(int windowTicks, float reductionRatio, float counterDamage,
                                           CounterAdmission admission) {
        if (admission == null || windowTicks <= 0) {
            return null;
        }
        if (!Float.isFinite(reductionRatio) || reductionRatio < 0.0f || reductionRatio >= 1.0f) {
            return null;
        }
        if (!Float.isFinite(counterDamage) || counterDamage < 0.0f) {
            return null;
        }
        return new CounterWindowSpec(windowTicks, reductionRatio, counterDamage, admission);
    }

    public int windowTicks() {
        return this.windowTicks;
    }

    public float reductionRatio() {
        return this.reductionRatio;
    }

    public float counterDamage() {
        return this.counterDamage;
    }

    public CounterAdmission admission() {
        return this.admission;
    }
}
