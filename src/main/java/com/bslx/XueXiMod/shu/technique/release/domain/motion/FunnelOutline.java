package com.bslx.XueXiMod.shu.technique.release.domain.motion;

/**
 * 组合漏斗轮廓。{@code rF(t)=R×(0.30+0.65×t^1.15)}，t 是柱底到组合高度的比例。
 * 只给主水带和优先轨道使用，不是平衡键，也不替代圆柱容纳。
 * 这是无状态几何服务；球形场地和护体都不使用这条漏斗轮廓。
 */
public final class FunnelOutline {
    public static final double NARROW = 0.30D;
    public static final double FLARE = 0.65D;
    public static final double CURVE = 1.15D;

    private FunnelOutline() {
    }

    /** 按采样高度计算漏斗半径。柱高过小退回 1，比例被夹到 0 到 1。 */
    public static double radiusAt(double bodyRadius, double bottom, double columnHeight, double sampleY) {
        double span = columnHeight;
        if (!(span > 1.0E-4D)) {
            span = 1.0D;
        }
        double fraction = (sampleY - bottom) / span;
        if (fraction < 0.0D) {
            fraction = 0.0D;
        }
        if (fraction > 1.0D) {
            fraction = 1.0D;
        }
        return bodyRadius * (NARROW + FLARE * Math.pow(fraction, CURVE));
    }
}
