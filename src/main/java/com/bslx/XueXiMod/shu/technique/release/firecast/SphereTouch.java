package com.bslx.XueXiMod.shu.technique.release.firecast;

/** 球与轴对齐盒是否相交。不做视线检测。 */
public final class SphereTouch {
    private SphereTouch() {
    }

    public static boolean reaches(double originX, double originY, double originZ, double radius,
                                  double lowerX, double lowerY, double lowerZ,
                                  double upperX, double upperY, double upperZ) {
        if (!Double.isFinite(radius) || radius < 0.0d) {
            return false;
        }
        double closestX = nearest(originX, lowerX, upperX);
        double closestY = nearest(originY, lowerY, upperY);
        double closestZ = nearest(originZ, lowerZ, upperZ);
        double east = originX - closestX;
        double rise = originY - closestY;
        double north = originZ - closestZ;
        return east * east + rise * rise + north * north <= radius * radius;
    }

    private static double nearest(double sample, double lower, double upper) {
        if (sample < lower) {
            return lower;
        }
        if (sample > upper) {
            return upper;
        }
        return sample;
    }
}
