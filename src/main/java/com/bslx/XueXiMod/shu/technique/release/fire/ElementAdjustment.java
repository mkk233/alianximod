package com.bslx.XueXiMod.shu.technique.release.fire;

/**
 * 事件里补一次未结算的炉心，或给水/冰来源乘一次承伤。
 * 已结算的火伤原样返回。水冰倍率只使用调用方给出的那一个数。
 */
public final class ElementAdjustment {
    private ElementAdjustment() {
    }

    public static double fireAmount(double currentAmount, double fireBase, double furnaceBonus, boolean furnaceSettled) {
        if (furnaceSettled || !Double.isFinite(currentAmount)) {
            return currentAmount;
        }
        if (!Double.isFinite(furnaceBonus) || furnaceBonus <= 0.0d || !Double.isFinite(fireBase) || fireBase <= 0.0d) {
            return currentAmount;
        }
        double adjusted = currentAmount + fireBase * furnaceBonus;
        if (!Double.isFinite(adjusted)) {
            return currentAmount;
        }
        return adjusted;
    }

    public static double vulnerableAmount(double currentAmount, double multiplier, boolean alreadyApplied) {
        if (alreadyApplied || !Double.isFinite(currentAmount)) {
            return currentAmount;
        }
        if (!Double.isFinite(multiplier) || multiplier <= 0.0d || multiplier == 1.0d) {
            return currentAmount;
        }
        double adjusted = currentAmount * multiplier;
        if (!Double.isFinite(adjusted)) {
            return currentAmount;
        }
        return adjusted;
    }
}
