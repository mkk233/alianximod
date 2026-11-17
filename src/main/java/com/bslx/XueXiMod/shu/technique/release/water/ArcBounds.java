package com.bslx.XueXiMod.shu.technique.release.water;

/** 轴对齐包围盒。宽阶段用它缩小查询，本身不算命中。 */
public final class ArcBounds {
    private final double lowEast;
    private final double lowAbove;
    private final double lowSouth;
    private final double highEast;
    private final double highAbove;
    private final double highSouth;

    public ArcBounds(double lowEast, double lowAbove, double lowSouth, double highEast, double highAbove, double highSouth) {
        this.lowEast = Math.min(lowEast, highEast);
        this.lowAbove = Math.min(lowAbove, highAbove);
        this.lowSouth = Math.min(lowSouth, highSouth);
        this.highEast = Math.max(lowEast, highEast);
        this.highAbove = Math.max(lowAbove, highAbove);
        this.highSouth = Math.max(lowSouth, highSouth);
    }

    public double lowEast() {
        return lowEast;
    }

    public double lowAbove() {
        return lowAbove;
    }

    public double lowSouth() {
        return lowSouth;
    }

    public double highEast() {
        return highEast;
    }

    public double highAbove() {
        return highAbove;
    }

    public double highSouth() {
        return highSouth;
    }

    public boolean overlaps(ArcBounds other) {
        return lowEast <= other.highEast && highEast >= other.lowEast
                && lowAbove <= other.highAbove && highAbove >= other.lowAbove
                && lowSouth <= other.highSouth && highSouth >= other.lowSouth;
    }

    public ArcBounds expand(double padding) {
        return new ArcBounds(
                lowEast - padding, lowAbove - padding, lowSouth - padding,
                highEast + padding, highAbove + padding, highSouth + padding);
    }
}
