package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;

/** 蒸汽基础伤害和半径。击退强度只作为空旷地面的调参，不保证每个目标都飞出这么远。 */
public final class SteamTuning {
    public static final float BASE_DAMAGE = 2.0F;
    public static final double RADIUS = 3.0d;
    public static final double KNOCKBACK_STRENGTH = 1.16d;

    private SteamTuning() {
    }

    public static double configuredRadius() {
        return BalanceService.number(SpecialBalance.COOL, BalanceKeys.RADIUS);
    }

    public static boolean reaches(double distanceSquared) {
        return reaches(distanceSquared, configuredRadius());
    }

    public static boolean reaches(double distanceSquared, double radius) {
        if (!Double.isFinite(distanceSquared) || !Double.isFinite(radius) || radius < 0.0d) {
            return false;
        }
        return distanceSquared <= radius * radius;
    }
}
