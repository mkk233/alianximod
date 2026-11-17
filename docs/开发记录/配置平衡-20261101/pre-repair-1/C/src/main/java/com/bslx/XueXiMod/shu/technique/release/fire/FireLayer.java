package com.bslx.XueXiMod.shu.technique.release.fire;

/** 同层火伤：D = B × (1 + A + F)。A 是资源相对 1 的增量，F 是已冻结的炉心。 */
public final class FireLayer {
    private FireLayer() {
    }

    public static double combine(double base, double resourceAboveOne, double furnaceBonus) {
        return base * (1.0d + resourceAboveOne + furnaceBonus);
    }

    public static boolean acceptableDamage(double damage) {
        return Double.isFinite(damage) && damage > 0.0d && damage <= FireLimits.DAMAGE_CAP;
    }
}
