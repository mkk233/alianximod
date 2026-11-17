package com.bslx.XueXiMod.shu.technique.release.water;

/** 尚未乘最终倍率、也尚未冻结原点的弧带请求。默认不启用。旧工厂保持外环。 */
public final class ArcBandDraft {
    private final int beat;
    private final double baseRadius;
    private final double baseHalfWidth;
    private final ArcBandMode bladeMode;

    private ArcBandDraft(int beat, double baseRadius, double baseHalfWidth, ArcBandMode bladeMode) {
        this.beat = beat;
        this.baseRadius = baseRadius;
        this.baseHalfWidth = baseHalfWidth;
        this.bladeMode = bladeMode;
    }

    public static ArcBandDraft of(int beat, double baseRadius, double baseHalfWidth) {
        return of(beat, baseRadius, baseHalfWidth, ArcBandMode.OUTER_ARC);
    }

    /** 剑形弧带用的径向刃片。不表示其它水形态。 */
    public static ArcBandDraft radialBlade(int beat, double baseRadius, double baseHalfWidth) {
        return of(beat, baseRadius, baseHalfWidth, ArcBandMode.RADIAL_BLADE);
    }

    public static ArcBandDraft of(int beat, double baseRadius, double baseHalfWidth, ArcBandMode bladeMode) {
        if (bladeMode == null || !Double.isFinite(baseRadius) || !Double.isFinite(baseHalfWidth)) {
            return null;
        }
        if (baseRadius <= 1.0E-6 || baseHalfWidth <= 1.0E-6) {
            return null;
        }
        return new ArcBandDraft(beat, baseRadius, baseHalfWidth, bladeMode);
    }

    public int beat() {
        return beat;
    }

    public double baseRadius() {
        return baseRadius;
    }

    public double baseHalfWidth() {
        return baseHalfWidth;
    }

    public ArcBandMode mode() {
        return bladeMode;
    }
}
