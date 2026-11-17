package com.bslx.XueXiMod.shu.technique.release.fire;

/** 蒸汽固定基础伤害和半径。击退强度只作为空旷地面的调参，不保证每个目标都飞出这么远。 */
public final class SteamTuning {
    public static final float BASE_DAMAGE = 2.0F;
    public static final double RADIUS = 3.0d;
    public static final double KNOCKBACK_STRENGTH = 1.16d;

    private SteamTuning() {
    }

    public static boolean reaches(double distanceSquared) {
        return distanceSquared <= RADIUS * RADIUS;
    }
}
