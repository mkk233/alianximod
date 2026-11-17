package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * 垂丝瀑布分件。调用方传入已经结算的水帘宽和高，这里不再乘聚能。
 * 横向 24 像素对应 1 格：4 像素边缘各 1/6 格，中段按 4 像素周期重复，末段裁几何和 U。
 * 纵向同样保持 24 像素每格，每段只采样当前帧，不把 128 像素拉满整条水帘。
 */
public final class WaterCurtainLayout {
    public static final int FRAME_COUNT = 16;
    public static final double PIXELS_PER_BLOCK = 24.0;
    public static final double EDGE_BLOCKS = 4.0 / PIXELS_PER_BLOCK;
    public static final double FRAME_BLOCKS = 128.0 / PIXELS_PER_BLOCK;
    /** 半个纹素，避免最近邻踩到相邻动画帧。 */
    public static final double TEXEL_PAD = 0.5 / 128.0;
    /**
     * thin 画布宽 64。全部 16 帧的不透明像素都在 x=25..38，左右留白不参与宽度。
     * 固定取到 39，给水流在这 14 列里摆动，不按单帧再收紧。
     */
    public static final double THIN_CONTENT_U_START = 25.0 / 64.0;
    public static final double THIN_CONTENT_U_END = 39.0 / 64.0;
    private static final int TILE_GUARD = 512;
    private static final int BAND_GUARD = 64;
    private static final double FIT_SLOP = 1.0E-4;

    public enum Piece {
        LEFT,
        MIDDLE,
        RIGHT,
        THIN
    }

    public static final class Panel {
        public final Piece piece;
        public final double alongStart;
        public final double alongEnd;
        public final double topOffset;
        public final double bottomOffset;
        public final double textureUStart;
        public final double textureUEnd;
        public final double textureVStart;
        public final double textureVEnd;

        private Panel(Piece piece, double alongStart, double alongEnd,
                      double topOffset, double bottomOffset,
                      double textureUStart, double textureUEnd,
                      double textureVStart, double textureVEnd) {
            this.piece = piece;
            this.alongStart = alongStart;
            this.alongEnd = alongEnd;
            this.topOffset = topOffset;
            this.bottomOffset = bottomOffset;
            this.textureUStart = textureUStart;
            this.textureUEnd = textureUEnd;
            this.textureVStart = textureVStart;
            this.textureVEnd = textureVEnd;
        }
    }

    private WaterCurtainLayout() {
    }

    /** 与服务端水体同一排空起点。贴图仍由 {@link #flowFrame} 每 2 tick 推进。 */
    public static int drainStart(int lifeTicks) {
        return WeepingSettings.drainStartFor(lifeTicks);
    }

    /**
     * 高度已经是服务端按同一寿命算出的水段。这里只拒绝没有排空窗口的哨兵，不改分件。
     */
    public static List<Panel> panels(double curtainWidth, double curtainHeight, int frameIndex, int drainStart) {
        if (drainStart > Integer.MAX_VALUE - WeepingSettings.FILL_TICKS) {
            return List.of();
        }
        return panels(curtainWidth, curtainHeight, frameIndex);
    }

    /** 16 帧，每 2 tick 一帧。相位只平移时间，不读取宽度，也不随寿命拉长。 */
    public static int flowFrame(int livedTicks, float partialTicks, int phaseTicks) {
        double partial = partialTicks;
        if (!Float.isFinite(partialTicks)) {
            partial = 0.0;
        }
        double elapsed = livedTicks + partial + phaseTicks;
        if (!Double.isFinite(elapsed)) {
            return 0;
        }
        int stepped = (int) Math.floor(elapsed / 2.0);
        return Math.floorMod(stepped, FRAME_COUNT);
    }

    public static List<Panel> panels(double curtainWidth, double curtainHeight, int frameIndex) {
        if (!Double.isFinite(curtainWidth) || !Double.isFinite(curtainHeight)) {
            return List.of();
        }
        if (curtainWidth <= FIT_SLOP || curtainHeight <= FIT_SLOP) {
            return List.of();
        }
        List<Span> spans = horizontalSpans(curtainWidth);
        List<Band> bands = verticalBands(curtainHeight, frameIndex);
        ArrayList<Panel> panels = new ArrayList<>(spans.size() * bands.size());
        for (Band band : bands) {
            for (Span span : spans) {
                panels.add(new Panel(
                        span.piece, span.alongStart, span.alongEnd,
                        band.topOffset, band.bottomOffset,
                        span.textureUStart, span.textureUEnd,
                        band.textureVStart, band.textureVEnd));
            }
        }
        return panels;
    }

    private static List<Span> horizontalSpans(double curtainWidth) {
        double halfAcross = curtainWidth * 0.5;
        double middleSpan = curtainWidth - 2.0 * EDGE_BLOCKS;
        if (!(middleSpan >= -FIT_SLOP)) {
            return thinFallback(halfAcross);
        }
        double periods = middleSpan / EDGE_BLOCKS;
        int wholeTiles = (int) Math.floor(periods + FIT_SLOP);
        if (wholeTiles < 0 || wholeTiles > TILE_GUARD) {
            return thinFallback(halfAcross);
        }
        double leftover = middleSpan - wholeTiles * EDGE_BLOCKS;
        if (leftover < FIT_SLOP) {
            leftover = 0.0;
        }
        double leftInner = -halfAcross + EDGE_BLOCKS;
        double rightInner = halfAcross - EDGE_BLOCKS;
        if (rightInner < leftInner) {
            double meet = (leftInner + rightInner) * 0.5;
            leftInner = meet;
            rightInner = meet;
        }
        ArrayList<Span> spans = new ArrayList<>();
        spans.add(new Span(Piece.LEFT, -halfAcross, leftInner, 0.0, 1.0));
        double cursor = leftInner;
        for (int tileIndex = 0; tileIndex < wholeTiles; tileIndex++) {
            double tileEnd = cursor + EDGE_BLOCKS;
            boolean closing = tileIndex == wholeTiles - 1 && leftover == 0.0;
            if (closing || tileEnd > rightInner) {
                tileEnd = rightInner;
            }
            spans.add(new Span(Piece.MIDDLE, cursor, tileEnd, 0.0, 1.0));
            cursor = tileEnd;
        }
        if (leftover > 0.0) {
            double textureEnd = leftover / EDGE_BLOCKS;
            if (textureEnd > 1.0) {
                textureEnd = 1.0;
            }
            spans.add(new Span(Piece.MIDDLE, cursor, rightInner, 0.0, textureEnd));
        }
        spans.add(new Span(Piece.RIGHT, rightInner, halfAcross, 0.0, 1.0));
        return spans;
    }

    private static List<Span> thinFallback(double halfAcross) {
        return List.of(new Span(Piece.THIN, -halfAcross, halfAcross, THIN_CONTENT_U_START, THIN_CONTENT_U_END));
    }

    private static List<Band> verticalBands(double curtainHeight, int frameIndex) {
        int frameSlot = Math.floorMod(frameIndex, FRAME_COUNT);
        double halfTall = curtainHeight * 0.5;
        double lowerEdge = -halfTall;
        double cursor = halfTall;
        ArrayList<Band> bands = new ArrayList<>();
        int placed = 0;
        while (cursor - lowerEdge > FIT_SLOP && placed < BAND_GUARD) {
            double room = cursor - lowerEdge;
            double bandSpan;
            double localCover;
            if (room >= FRAME_BLOCKS - FIT_SLOP) {
                if (Math.abs(room - FRAME_BLOCKS) <= FIT_SLOP) {
                    bandSpan = room;
                } else {
                    bandSpan = FRAME_BLOCKS;
                }
                localCover = 1.0;
            } else {
                bandSpan = room;
                localCover = bandSpan / FRAME_BLOCKS;
            }
            double localTop = TEXEL_PAD;
            double localBottom = localCover - TEXEL_PAD;
            if (localBottom <= localTop) {
                localTop = TEXEL_PAD;
                localBottom = Math.min(1.0 - TEXEL_PAD, TEXEL_PAD + Math.max(localCover, TEXEL_PAD));
            }
            double textureVStart = (frameSlot + localTop) / FRAME_COUNT;
            double textureVEnd = (frameSlot + localBottom) / FRAME_COUNT;
            double frameCeiling = (frameSlot + 1.0) / FRAME_COUNT;
            if (textureVEnd > frameCeiling) {
                textureVEnd = frameCeiling - TEXEL_PAD / FRAME_COUNT;
            }
            bands.add(new Band(cursor, cursor - bandSpan, textureVStart, textureVEnd));
            cursor -= bandSpan;
            placed++;
        }
        return bands;
    }

    private static final class Span {
        private final Piece piece;
        private final double alongStart;
        private final double alongEnd;
        private final double textureUStart;
        private final double textureUEnd;

        private Span(Piece piece, double alongStart, double alongEnd, double textureUStart, double textureUEnd) {
            this.piece = piece;
            this.alongStart = alongStart;
            this.alongEnd = alongEnd;
            this.textureUStart = textureUStart;
            this.textureUEnd = textureUEnd;
        }
    }

    private static final class Band {
        private final double topOffset;
        private final double bottomOffset;
        private final double textureVStart;
        private final double textureVEnd;

        private Band(double topOffset, double bottomOffset, double textureVStart, double textureVEnd) {
            this.topOffset = topOffset;
            this.bottomOffset = bottomOffset;
            this.textureVStart = textureVStart;
            this.textureVEnd = textureVEnd;
        }
    }
}
