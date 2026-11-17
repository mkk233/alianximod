package com.bslx.XueXiMod.shu.technique.release.firecast;

import com.bslx.XueXiMod.shu.compute.ComputeCalls;

/** 剑前走廊的纯判定。前方从起点沿朝向伸出，不向后。公式在 JavaReference。 */
public final class MeleeCorridorMath {
    private MeleeCorridorMath() {
    }

    public static boolean blocked(double contactDistance, double obstacleDistance) {
        return ComputeCalls.blocked(contactDistance, obstacleDistance);
    }

    /**
     * 朝向必须是单位向量。盒子用八个角的包围来近似相交：
     * 沿朝向的投影区间碰到 [0, length]，横向和竖向落在半宽半高内。
     */
    public static boolean overlaps(double startX, double startY, double startZ,
                                   double forwardX, double forwardY, double forwardZ,
                                   double length, double width, double height,
                                   double lowerX, double lowerY, double lowerZ,
                                   double upperX, double upperY, double upperZ) {
        return ComputeCalls.overlaps(startX, startY, startZ, forwardX, forwardY, forwardZ,
                length, width, height, lowerX, lowerY, lowerZ, upperX, upperY, upperZ);
    }

    /** 目标靠近射线的一端。单位朝向。负值表示在起点后方。 */
    public static double contactDistance(double startX, double startY, double startZ,
                                         double unitX, double unitY, double unitZ,
                                         double centerX, double centerY, double centerZ,
                                         double alongPad) {
        return ComputeCalls.contactDistance(startX, startY, startZ, unitX, unitY, unitZ,
                centerX, centerY, centerZ, alongPad);
    }
}
