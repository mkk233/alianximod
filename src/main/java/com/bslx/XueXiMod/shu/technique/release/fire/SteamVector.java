package com.bslx.XueXiMod.shu.technique.release.fire;

/** 击退用的水平方向。重合或正上方时改用施法者水平朝向的反向量，让原版击退顺着朝向推。 */
public final class SteamVector {
    private static final double OVERLAP_LIMIT = 1.0e-4d;

    private final double towardX;
    private final double towardZ;

    private SteamVector(double towardX, double towardZ) {
        this.towardX = towardX;
        this.towardZ = towardZ;
    }

    public static SteamVector horizontal(double casterX, double casterZ, double targetX, double targetZ,
                                        double facingYawDegrees) {
        double towardX = casterX - targetX;
        double towardZ = casterZ - targetZ;
        double horizontal = towardX * towardX + towardZ * towardZ;
        if (horizontal < OVERLAP_LIMIT) {
            double yawRadians = Math.toRadians(facingYawDegrees);
            double lookX = -Math.sin(yawRadians);
            double lookZ = Math.cos(yawRadians);
            towardX = -lookX;
            towardZ = -lookZ;
        }
        return new SteamVector(towardX, towardZ);
    }

    public double towardX() {
        return this.towardX;
    }

    public double towardZ() {
        return this.towardZ;
    }
}
