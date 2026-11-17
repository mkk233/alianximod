package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;

/** 一份出生体的水平半径和竖向半高。地表检查取整组覆盖后的较大值。 */
public final class BirthClearance {
    private final double horizontalRadius;
    private final double verticalHalf;

    public BirthClearance(double horizontalRadius, double verticalHalf) {
        this.horizontalRadius = horizontalRadius;
        this.verticalHalf = verticalHalf;
    }

    public static BirthClearance empty() {
        return new BirthClearance(0.0D, 0.0D);
    }

    public double horizontalRadius() {
        return horizontalRadius;
    }

    public double verticalHalf() {
        return verticalHalf;
    }

    /** 用这个释放定义的出生半径和半高扩大净空。倍率至少为 1。 */
    public BirthClearance cover(ReleaseDefinition definition) {
        if (definition == null) {
            return this;
        }
        double scale = Math.max(1.0D, definition.spanScale());
        return new BirthClearance(
                Math.max(this.horizontalRadius, definition.birthHorizontalRadius() * scale),
                Math.max(this.verticalHalf, definition.birthVerticalHalf() * scale));
    }
}
