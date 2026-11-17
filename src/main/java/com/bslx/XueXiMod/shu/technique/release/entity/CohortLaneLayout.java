package com.bslx.XueXiMod.shu.technique.release.entity;

/**
 * 同锚点多份的纯视觉分槽。每份只画自己的 60% 槽，槽间留 40% 空隙。
 * 序号固定，不看相机，也不按 3 取余。份数很多时槽会窄到像素里分不清，但不丢份、不改实体倍率。
 * 断潮这类斩面的可分离轴只有命中厚度那么窄，间隙在几何上为正，远处可能糊成一道。
 * 旧外环水弧只在原来的半径厚度里分槽，近处不如径向刃片清楚。
 */
public final class CohortLaneLayout {
    public static final double DRAW_FRACTION = 0.6;

    private CohortLaneLayout() {
    }

    /** 面内较长的一边当刀刃。更高则左右分，更宽或相等则上下分。 */
    public static SeparableAxis separableAxis(double acrossSpan, double riseSpan) {
        if (riseSpan > acrossSpan + 1.0E-6) {
            return SeparableAxis.ACROSS;
        }
        return SeparableAxis.RISE;
    }

    /** 贴图半宽就是命中宽度的一半。 */
    public static float sheetHalfAcross(double width) {
        return (float) width * 0.5F;
    }

    /** 高度几乎为 0 时改用长度；过短则用 1.5。 */
    public static float sheetHalfRise(double length, double height) {
        float halfRise = height <= 0.05D ? (float) length * 0.5F : (float) height * 0.5F;
        if (halfRise < 0.05F) {
            halfRise = 1.5F;
        }
        return halfRise;
    }

    /** 单份用整张面。多份沿较长边分成自己的槽。 */
    public static FaceWindow sheetWindow(double width, double length, double height, CohortVisual visual) {
        float halfAcross = sheetHalfAcross(width);
        float halfRise = sheetHalfRise(length, height);
        float acrossLow = -halfAcross;
        float acrossHigh = halfAcross;
        float riseLow = -halfRise;
        float riseHigh = halfRise;
        CohortVisual drawing = visual == null ? CohortVisual.SINGLE : visual.forDrawing();
        if (drawing.laneSplit()) {
            SeparableAxis axis = separableAxis(halfAcross * 2.0F, halfRise * 2.0F);
            LaneBand laneBand = LaneBand.of(drawing.displayIndex(), drawing.displayCount(),
                    axis == SeparableAxis.ACROSS ? acrossLow : riseLow,
                    axis == SeparableAxis.ACROSS ? acrossHigh : riseHigh);
            if (axis == SeparableAxis.ACROSS) {
                acrossLow = (float) laneBand.lowEdge();
                acrossHigh = (float) laneBand.highEdge();
            } else {
                riseLow = (float) laneBand.lowEdge();
                riseHigh = (float) laneBand.highEdge();
            }
        }
        return new FaceWindow(acrossLow, acrossHigh, riseLow, riseHigh);
    }

    /**
     * 让锋的斩光。单份保持调用方传入的半宽。
     * 多份时沿命中体更薄的那条轴分槽；左右分时辉光占满槽，刃芯收在槽中间，避免辉光跨进邻槽。
     */
    public static SlashInterval partingSlash(double hitWidth, double hitHeight, double sideHalf, double riseHalf,
                                              CohortVisual visual, boolean coreInset) {
        float sideLow = (float) -sideHalf;
        float sideHigh = (float) sideHalf;
        float riseLow = (float) -riseHalf;
        float riseHigh = (float) riseHalf;
        CohortVisual drawing = visual == null ? CohortVisual.SINGLE : visual.forDrawing();
        if (!drawing.laneSplit()) {
            return new SlashInterval(sideLow, sideHigh, riseLow, riseHigh);
        }
        SeparableAxis axis = separableAxis(hitWidth, hitHeight);
        if (axis == SeparableAxis.ACROSS) {
            LaneBand laneBand = LaneBand.of(drawing.displayIndex(), drawing.displayCount(), -hitWidth * 0.5, hitWidth * 0.5);
            if (coreInset) {
                double center = (laneBand.lowEdge() + laneBand.highEdge()) * 0.5;
                double insetHalf = (laneBand.highEdge() - laneBand.lowEdge()) * 0.25;
                laneBand = new LaneBand(center - insetHalf, center + insetHalf);
            }
            sideLow = (float) laneBand.lowEdge();
            sideHigh = (float) laneBand.highEdge();
        } else {
            LaneBand laneBand = LaneBand.of(drawing.displayIndex(), drawing.displayCount(), -riseHalf, riseHalf);
            riseLow = (float) laneBand.lowEdge();
            riseHigh = (float) laneBand.highEdge();
        }
        return new SlashInterval(sideLow, sideHigh, riseLow, riseHigh);
    }

    /** 单份半宽保持原水带。多份沿命中横截面分槽，只返回绘制用的中心和半宽。 */
    public static RibbonStrip ribbonStrip(double hitWidth, double singleHalf, CohortVisual visual) {
        CohortVisual drawing = visual == null ? CohortVisual.SINGLE : visual.forDrawing();
        if (!drawing.laneSplit()) {
            return new RibbonStrip(0.0, singleHalf, false);
        }
        double domain = hitWidth;
        if (!(domain > 1.0E-4) || !Double.isFinite(domain)) {
            domain = singleHalf * 2.0;
        }
        LaneBand laneBand = LaneBand.of(drawing.displayIndex(), drawing.displayCount(), -domain * 0.5, domain * 0.5);
        double center = (laneBand.lowEdge() + laneBand.highEdge()) * 0.5;
        double halfSpan = (laneBand.highEdge() - laneBand.lowEdge()) * 0.5;
        return new RibbonStrip(center, halfSpan, true);
    }

    /**
     * 切向近竖直时改用世界东向作参考，并锁在东向非负的一侧，避免水带翻面。
     * 只给多份分道用。单份仍走原来的世界上叉乘。
     */
    public static LateralAxis stableLateral(double east, double above, double south) {
        double tangentLength = Math.sqrt(east * east + above * above + south * south);
        if (!(tangentLength > 1.0E-8) || !Double.isFinite(tangentLength)) {
            return new LateralAxis(1.0, 0.0, 0.0);
        }
        double unitEast = east / tangentLength;
        double unitAbove = above / tangentLength;
        double unitSouth = south / tangentLength;
        double referenceEast = 0.0;
        double referenceAbove = 1.0;
        double referenceSouth = 0.0;
        if (Math.abs(unitAbove) > 0.85) {
            referenceEast = 1.0;
            referenceAbove = 0.0;
            referenceSouth = 0.0;
        }
        double sideEast = unitAbove * referenceSouth - unitSouth * referenceAbove;
        double sideAbove = unitSouth * referenceEast - unitEast * referenceSouth;
        double sideSouth = unitEast * referenceAbove - unitAbove * referenceEast;
        double sideLength = Math.sqrt(sideEast * sideEast + sideAbove * sideAbove + sideSouth * sideSouth);
        if (!(sideLength > 1.0E-8) || !Double.isFinite(sideLength)) {
            sideEast = unitAbove;
            sideAbove = -unitEast;
            sideSouth = 0.0;
            sideLength = Math.sqrt(sideEast * sideEast + sideAbove * sideAbove + sideSouth * sideSouth);
            if (!(sideLength > 1.0E-8) || !Double.isFinite(sideLength)) {
                return new LateralAxis(1.0, 0.0, 0.0);
            }
        }
        sideEast /= sideLength;
        sideAbove /= sideLength;
        sideSouth /= sideLength;
        if (sideEast < 0.0 || (Math.abs(sideEast) < 1.0E-8 && sideSouth < 0.0)) {
            sideEast = -sideEast;
            sideAbove = -sideAbove;
            sideSouth = -sideSouth;
        }
        return new LateralAxis(sideEast, sideAbove, sideSouth);
    }

    public enum SeparableAxis {
        ACROSS,
        RISE
    }

    public static final class LaneBand {
        private final double lowEdge;
        private final double highEdge;

        public LaneBand(double lowEdge, double highEdge) {
            this.lowEdge = lowEdge;
            this.highEdge = highEdge;
        }

        /** 份数不足两份时用整段。否则在槽中间留出 40% 空隙。 */
        public static LaneBand of(int displayIndex, int displayCount, double domainLow, double domainHigh) {
            if (displayCount <= 1) {
                return new LaneBand(domainLow, domainHigh);
            }
            double span = domainHigh - domainLow;
            double slotSpan = span / displayCount;
            double drawSpan = slotSpan * DRAW_FRACTION;
            double inset = (slotSpan - drawSpan) * 0.5;
            double drawnLow = domainLow + slotSpan * displayIndex + inset;
            return new LaneBand(drawnLow, drawnLow + drawSpan);
        }

        public double lowEdge() {
            return this.lowEdge;
        }

        public double highEdge() {
            return this.highEdge;
        }

        public double clearanceBefore(LaneBand later) {
            return later.lowEdge - this.highEdge;
        }
    }

    public static final class FaceWindow {
        private final float acrossLow;
        private final float acrossHigh;
        private final float riseLow;
        private final float riseHigh;

        public FaceWindow(float acrossLow, float acrossHigh, float riseLow, float riseHigh) {
            this.acrossLow = acrossLow;
            this.acrossHigh = acrossHigh;
            this.riseLow = riseLow;
            this.riseHigh = riseHigh;
        }

        public float acrossLow() {
            return this.acrossLow;
        }

        public float acrossHigh() {
            return this.acrossHigh;
        }

        public float riseLow() {
            return this.riseLow;
        }

        public float riseHigh() {
            return this.riseHigh;
        }
    }

    public static final class SlashInterval {
        private final float sideLow;
        private final float sideHigh;
        private final float riseLow;
        private final float riseHigh;

        public SlashInterval(float sideLow, float sideHigh, float riseLow, float riseHigh) {
            this.sideLow = sideLow;
            this.sideHigh = sideHigh;
            this.riseLow = riseLow;
            this.riseHigh = riseHigh;
        }

        public float sideLow() {
            return this.sideLow;
        }

        public float sideHigh() {
            return this.sideHigh;
        }

        public float riseLow() {
            return this.riseLow;
        }

        public float riseHigh() {
            return this.riseHigh;
        }
    }

    public static final class RibbonStrip {
        private final double center;
        private final double halfSpan;
        private final boolean separated;

        public RibbonStrip(double center, double halfSpan, boolean separated) {
            this.center = center;
            this.halfSpan = halfSpan;
            this.separated = separated;
        }

        public double center() {
            return this.center;
        }

        public double halfSpan() {
            return this.halfSpan;
        }

        public boolean separated() {
            return this.separated;
        }
    }

    public static final class LateralAxis {
        private final double east;
        private final double above;
        private final double south;

        public LateralAxis(double east, double above, double south) {
            this.east = east;
            this.above = above;
            this.south = south;
        }

        public double east() {
            return this.east;
        }

        public double above() {
            return this.above;
        }

        public double south() {
            return this.south;
        }
    }
}
