package com.bslx.XueXiMod.shu.technique.release.deflection.melee;

/** 攻击起点到身体最近点。点落在身体内部时没有可检验的攻击线。 */
public final class MeleeLine {
    public static final double SHORTEST_SPAN = 1.0E-8;

    private MeleeLine() {
    }

    /** 点到包围盒的最近点。 */
    public static double[] closest(double pointX, double pointY, double pointZ,
                                   double lowerX, double lowerY, double lowerZ,
                                   double upperX, double upperY, double upperZ) {
        return new double[]{
                clamp(pointX, lowerX, upperX),
                clamp(pointY, lowerY, upperY),
                clamp(pointZ, lowerZ, upperZ)
        };
    }

    /** 线段长度是否足够形成可检验的攻击线。 */
    public static boolean usable(double spanSquared) {
        return Double.isFinite(spanSquared) && spanSquared > SHORTEST_SPAN;
    }

    private static double clamp(double reading, double lower, double upper) {
        if (reading < lower) {
            return lower;
        }
        if (reading > upper) {
            return upper;
        }
        return reading;
    }
}
