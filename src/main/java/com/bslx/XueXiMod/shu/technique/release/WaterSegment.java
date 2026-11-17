package com.bslx.XueXiMod.shu.technique.release;

import net.minecraft.world.phys.Vec3;

/**
 * 一拍垂丝的不可变水段。宽和厚都是整段，不是半径。
 * 唇口和朝向在这一拍里不动，中心只随上下沿改变高度。
 */
public record WaterSegment(Vec3 lip, Vec3 center, Vec3 ahead,
                           double upperDrop, double lowerDrop, double footDrop,
                           double across, double thick, boolean present, boolean floorFoot) {
    /** 开度不大于这个值就不当成水体，避免留下一条最低高度的假体积。 */
    public static final double PRESENT_GAP = 0.05;

    public static final WaterSegment EMPTY = new WaterSegment(
            Vec3.ZERO, Vec3.ZERO, new Vec3(0.0, 0.0, 1.0),
            0.0, 0.0, 0.0, 0.0, 0.0, false, false);

    public WaterSegment {
        if (!finitePoint(lip)) {
            lip = Vec3.ZERO;
        }
        if (!finitePoint(center)) {
            center = lip;
        }
        ahead = horizontalUnit(ahead);
        upperDrop = finiteDrop(upperDrop);
        lowerDrop = finiteDrop(lowerDrop);
        footDrop = finiteDrop(footDrop);
        across = finiteSpan(across);
        thick = finiteSpan(thick);
    }

    /** 这一拍没有水。唇口和朝向仍保留，高度是 0。 */
    public static WaterSegment absent(Vec3 lip, Vec3 ahead, double across, double thick) {
        Vec3 mouth = finitePoint(lip) ? lip : Vec3.ZERO;
        return new WaterSegment(mouth, mouth, ahead, 0.0, 0.0, 0.0, across, thick, false, false);
    }

    /** 朝向压到水平单位向量。垂直输入落到正南。 */
    public static Vec3 horizontalAhead(Vec3 look) {
        return horizontalUnit(look);
    }

    /** 实际水体高度。没有水时是 0，不保底。 */
    public double activeHeight() {
        if (!this.present) {
            return 0.0;
        }
        double span = this.lowerDrop - this.upperDrop;
        if (!Double.isFinite(span) || span <= PRESENT_GAP) {
            return 0.0;
        }
        return span;
    }

    private static boolean finitePoint(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static Vec3 horizontalUnit(Vec3 look) {
        if (!finitePoint(look)) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double horizontalX = look.x;
        double horizontalZ = look.z;
        double horizontal = horizontalX * horizontalX + horizontalZ * horizontalZ;
        if (horizontal < 1.0E-8) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double inverse = 1.0 / Math.sqrt(horizontal);
        return new Vec3(horizontalX * inverse, 0.0, horizontalZ * inverse);
    }

    private static double finiteDrop(double drop) {
        if (!Double.isFinite(drop) || drop < 0.0) {
            return 0.0;
        }
        return drop;
    }

    private static double finiteSpan(double span) {
        if (!Double.isFinite(span) || span < 0.0) {
            return 0.0;
        }
        return span;
    }
}
