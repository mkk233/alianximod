package com.bslx.XueXiMod.shu.technique.release.firecast;

import java.util.List;

/** 地表火层的纯几何。不读世界，也不改方块。 */
public final class GroundFireMath {
    private GroundFireMath() {
    }

    public static boolean finite(double sample) {
        return Double.isFinite(sample);
    }

    /**
     * 脚底落在地表以上、层高以内，并且地表顶与原点的落差不超过 slack。
     * 上界不含，刚好站在层顶外面不算。
     */
    public static boolean inLayer(double originY, double surfaceTop, double feetY, double slack, double layerHeight) {
        if (!finite(originY) || !finite(surfaceTop) || !finite(feetY) || !finite(slack) || !finite(layerHeight)) {
            return false;
        }
        if (slack < 0.0d || layerHeight <= 0.0d) {
            return false;
        }
        if (Math.abs(surfaceTop - originY) > slack) {
            return false;
        }
        return feetY >= surfaceTop && feetY < surfaceTop + layerHeight;
    }

    public static boolean insideDisc(double originX, double originZ, double feetX, double feetZ, double radius) {
        if (!finite(originX) || !finite(originZ) || !finite(feetX) || !finite(feetZ) || !finite(radius) || radius < 0.0d) {
            return false;
        }
        double east = feetX - originX;
        double north = feetZ - originZ;
        return east * east + north * north <= radius * radius;
    }

    /**
     * 在采样点的碰撞箱里取最高的、上方留有站立空隙、并且落差不超过 slack 的顶面。
     * 没有这样的顶面时返回非数字。
     */
    public static double openTop(double sampleX, double sampleZ, double originY, double slack, List<Span> spans) {
        if (!finite(sampleX) || !finite(sampleZ) || !finite(originY) || !finite(slack) || slack < 0.0d || spans == null) {
            return Double.NaN;
        }
        double chosen = Double.NaN;
        for (Span span : spans) {
            if (span == null || sampleX < span.lowX() || sampleX > span.highX() || sampleZ < span.lowZ() || sampleZ > span.highZ()) {
                continue;
            }
            double top = span.highY();
            if (!finite(top) || Math.abs(top - originY) > slack || covered(spans, sampleX, sampleZ, top)) {
                continue;
            }
            if (Double.isNaN(chosen) || top > chosen) {
                chosen = top;
            }
        }
        return chosen;
    }

    private static boolean covered(List<Span> spans, double sampleX, double sampleZ, double top) {
        double probe = top + 1.0e-4d;
        for (Span span : spans) {
            if (span == null || sampleX < span.lowX() || sampleX > span.highX() || sampleZ < span.lowZ() || sampleZ > span.highZ()) {
                continue;
            }
            if (probe > span.lowY() && probe < span.highY()) {
                return true;
            }
        }
        return false;
    }

    /** 世界坐标里的一块碰撞范围。 */
    public static final class Span {
        private final double lowX;
        private final double lowY;
        private final double lowZ;
        private final double highX;
        private final double highY;
        private final double highZ;

        public Span(double lowX, double lowY, double lowZ, double highX, double highY, double highZ) {
            this.lowX = lowX;
            this.lowY = lowY;
            this.lowZ = lowZ;
            this.highX = highX;
            this.highY = highY;
            this.highZ = highZ;
        }

        public double lowX() {
            return this.lowX;
        }

        public double lowY() {
            return this.lowY;
        }

        public double lowZ() {
            return this.lowZ;
        }

        public double highX() {
            return this.highX;
        }

        public double highY() {
            return this.highY;
        }

        public double highZ() {
            return this.highZ;
        }
    }
}
