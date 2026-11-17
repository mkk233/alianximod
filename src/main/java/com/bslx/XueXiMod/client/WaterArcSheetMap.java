package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortVisual;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;

/**
 * 把弧形水斩竖条按素材极坐标采样到已冻结的弧带上。
 * 世界顶点只用快照的轴向、扫角、半径和半宽；不读相机，也不把原图弧再弯一次。
 */
public final class WaterArcSheetMap {
    public static final int FRAME_COUNT = 16;
    public static final int FRAME_EDGE = 64;
    public static final double SOURCE_START_DEGREES = -213.0;
    public static final double PEAK_END_DEGREES = 4.0;
    /** 前 7 帧浪头。活动阶段用当前帧的终点，消退阶段固定停在峰值 4 度。 */
    public static final double[] HEAD_END_DEGREES = {-199.0, -187.0, -171.0, -142.0, -96.0, -43.0, 4.0};
    public static final double ELLIPSE_CENTER_COLUMN = 32.0;
    public static final double ELLIPSE_CENTER_ROW = 34.0;
    public static final double ELLIPSE_HALF_COLUMN = 25.5;
    public static final double ELLIPSE_HALF_ROW = 24.0;
    /** 极径窗覆盖月牙主体，并给浪钩、近处水滴留出一点外溢。 */
    public static final double RHO_LOW = 0.42;
    public static final double RHO_HIGH = 1.28;
    public static final double RHO_MID = 0.85;
    public static final double RHO_BODY_HALF = 0.22;
    public static final int RADIAL_STEPS = 8;
    private static final double TEXEL_INSET = 0.5;

    private WaterArcSheetMap() {
    }

    public static int frameIndex(double age, int activeTicks, int tailTicks) {
        if (!Double.isFinite(age) || age < 0.0) {
            return -1;
        }
        int safeActive = activeTicks < 1 ? 1 : activeTicks;
        int safeTail = tailTicks < 0 ? 0 : tailTicks;
        if (age >= safeActive + safeTail) {
            return -1;
        }
        if (age <= safeActive) {
            return (int) Math.floor(6.0 * age / safeActive);
        }
        int faded = 6 + (int) Math.floor(10.0 * (age - safeActive) / safeTail);
        if (faded > FRAME_COUNT - 1) {
            return FRAME_COUNT - 1;
        }
        return faded;
    }

    public static double reveal(double age, int activeTicks) {
        int safeActive = activeTicks < 1 ? 1 : activeTicks;
        if (!Double.isFinite(age) || age <= 0.0) {
            return 0.0;
        }
        if (age >= safeActive) {
            return 1.0;
        }
        return age / safeActive;
    }

    public static double fadeAlpha(double age, int activeTicks, int tailTicks) {
        if (!Double.isFinite(age) || age < activeTicks || tailTicks <= 0) {
            return age < activeTicks ? 1.0 : 0.0;
        }
        double fade = 1.0 - (age - activeTicks) / tailTicks;
        if (fade < 0.0) {
            return 0.0;
        }
        if (fade > 1.0) {
            return 1.0;
        }
        return fade;
    }

    public static double sourceDegrees(int frameIndex, double age, int activeTicks, double progress) {
        if (age <= activeTicks) {
            int bounded = frameIndex;
            if (bounded < 0) {
                bounded = 0;
            }
            if (bounded >= HEAD_END_DEGREES.length) {
                bounded = HEAD_END_DEGREES.length - 1;
            }
            double revealed = reveal(age, activeTicks);
            double covered = 0.0;
            if (revealed > 1.0E-8) {
                covered = progress / revealed;
            }
            if (covered < 0.0) {
                covered = 0.0;
            }
            if (covered > 1.0) {
                covered = 1.0;
            }
            double head = HEAD_END_DEGREES[bounded];
            return SOURCE_START_DEGREES + (head - SOURCE_START_DEGREES) * covered;
        }
        double clampedProgress = progress;
        if (clampedProgress < 0.0) {
            clampedProgress = 0.0;
        }
        if (clampedProgress > 1.0) {
            clampedProgress = 1.0;
        }
        return SOURCE_START_DEGREES + (PEAK_END_DEGREES - SOURCE_START_DEGREES) * clampedProgress;
    }

    public static double worldDistance(ArcBandSnapshot snapshot, double rho) {
        double lateral = (rho - RHO_MID) / RHO_BODY_HALF;
        return snapshot.radius() + snapshot.halfWidth() * lateral;
    }

    public static ArcVec offsetAt(ArcBandSnapshot snapshot, double progress, double rho) {
        return offsetAt(snapshot, progress, rho, CohortVisual.SINGLE);
    }

    public static ArcVec offsetAt(ArcBandSnapshot snapshot, double progress, double rho, CohortVisual visual) {
        return ProgressArcBand.direction(snapshot, progress).scale(placedDistance(snapshot, rho, visual));
    }

    /**
     * 单份仍用原来的极径到半径。多份把整段素材压进自己的径向槽。
     * 径向刃片的槽在 0.35 倍半径到半径之间；旧外环只占用原来的厚度带。
     */
    public static double placedDistance(ArcBandSnapshot snapshot, double rho, CohortVisual visual) {
        if (snapshot == null) {
            return 0.0;
        }
        CohortVisual drawing = visual == null ? CohortVisual.SINGLE : visual.forDrawing();
        if (!drawing.laneSplit()) {
            return worldDistance(snapshot, rho);
        }
        double domainLow;
        double domainHigh;
        if (snapshot.mode() == ArcBandMode.RADIAL_BLADE) {
            domainHigh = snapshot.radius();
            domainLow = domainHigh * 0.35;
        } else {
            double nearDistance = worldDistance(snapshot, RHO_LOW);
            double farDistance = worldDistance(snapshot, RHO_HIGH);
            domainLow = Math.min(nearDistance, farDistance);
            domainHigh = Math.max(nearDistance, farDistance);
        }
        if (!(domainHigh > domainLow + 1.0E-8)) {
            return worldDistance(snapshot, rho);
        }
        CohortLaneLayout.LaneBand laneBand = CohortLaneLayout.LaneBand.of(
                drawing.displayIndex(), drawing.displayCount(), domainLow, domainHigh);
        double rhoSpan = RHO_HIGH - RHO_LOW;
        double unit = rhoSpan == 0.0 ? 0.0 : (rho - RHO_LOW) / rhoSpan;
        if (unit < 0.0) {
            unit = 0.0;
        }
        if (unit > 1.0) {
            unit = 1.0;
        }
        return laneBand.lowEdge() + (laneBand.highEdge() - laneBand.lowEdge()) * unit;
    }

    public static double rhoAt(int bandIndex, int bandSteps) {
        if (bandSteps < 1) {
            return RHO_MID;
        }
        double fraction = bandIndex / (double) bandSteps;
        if (fraction < 0.0) {
            fraction = 0.0;
        }
        if (fraction > 1.0) {
            fraction = 1.0;
        }
        return RHO_LOW + (RHO_HIGH - RHO_LOW) * fraction;
    }

    public static int angleSteps(ArcBandSnapshot snapshot, double revealed) {
        double degrees = Math.toDegrees(Math.abs(snapshot.sweepAngle()) * revealed);
        int steps = (int) Math.ceil(degrees / 3.0);
        if (steps < 4) {
            steps = 4;
        }
        if (steps > 64) {
            steps = 64;
        }
        return steps;
    }

    public static SheetCorner corner(ArcBandSnapshot snapshot, int frameIndex, double age, int activeTicks,
                                     double progress, double rho) {
        return corner(snapshot, frameIndex, age, activeTicks, progress, rho, CohortVisual.SINGLE);
    }

    public static SheetCorner corner(ArcBandSnapshot snapshot, int frameIndex, double age, int activeTicks,
                                     double progress, double rho, CohortVisual visual) {
        ArcVec offset = offsetAt(snapshot, progress, rho, visual);
        double degrees = sourceDegrees(frameIndex, age, activeTicks, progress);
        double radians = Math.toRadians(degrees);
        double column = ELLIPSE_CENTER_COLUMN + ELLIPSE_HALF_COLUMN * rho * Math.cos(radians);
        double row = ELLIPSE_CENTER_ROW + ELLIPSE_HALF_ROW * rho * Math.sin(radians);
        double safeColumn = clampRange(column, TEXEL_INSET, FRAME_EDGE - TEXEL_INSET);
        double safeRow = clampRange(row, TEXEL_INSET, FRAME_EDGE - TEXEL_INSET);
        double textureU = safeColumn / FRAME_EDGE;
        double textureV = (frameIndex + safeRow / FRAME_EDGE) / FRAME_COUNT;
        return new SheetCorner(offset.east(), offset.above(), offset.south(), textureU, textureV);
    }

    public static boolean emit(ArcBandSnapshot snapshot, double age, SheetVisitor visitor) {
        return emit(snapshot, age, visitor, CohortVisual.SINGLE);
    }

    public static boolean emit(ArcBandSnapshot snapshot, double age, SheetVisitor visitor, CohortVisual visual) {
        if (snapshot == null || visitor == null) {
            return false;
        }
        int frame = frameIndex(age, snapshot.activeTicks(), snapshot.tailTicks());
        if (frame < 0) {
            return false;
        }
        double revealed = reveal(age, snapshot.activeTicks());
        if (revealed <= 1.0E-8) {
            return false;
        }
        int arcSteps = angleSteps(snapshot, revealed);
        boolean emitted = false;
        for (int arcIndex = 0; arcIndex < arcSteps; arcIndex++) {
            double fromProgress = revealed * arcIndex / arcSteps;
            double toProgress = revealed * (arcIndex + 1) / arcSteps;
            for (int bandIndex = 0; bandIndex < RADIAL_STEPS; bandIndex++) {
                double innerRho = rhoAt(bandIndex, RADIAL_STEPS);
                double outerRho = rhoAt(bandIndex + 1, RADIAL_STEPS);
                visitor.quad(
                        corner(snapshot, frame, age, snapshot.activeTicks(), fromProgress, innerRho, visual),
                        corner(snapshot, frame, age, snapshot.activeTicks(), fromProgress, outerRho, visual),
                        corner(snapshot, frame, age, snapshot.activeTicks(), toProgress, outerRho, visual),
                        corner(snapshot, frame, age, snapshot.activeTicks(), toProgress, innerRho, visual));
                emitted = true;
            }
        }
        return emitted;
    }

    private static double clampRange(double sample, double lower, double upper) {
        if (sample < lower) {
            return lower;
        }
        if (sample > upper) {
            return upper;
        }
        return sample;
    }

    @FunctionalInterface
    public interface SheetVisitor {
        void quad(SheetCorner startInner, SheetCorner startOuter, SheetCorner endOuter, SheetCorner endInner);
    }

    public static final class SheetCorner {
        private final double eastOffset;
        private final double aboveOffset;
        private final double southOffset;
        private final double textureU;
        private final double textureV;

        public SheetCorner(double eastOffset, double aboveOffset, double southOffset, double textureU, double textureV) {
            this.eastOffset = eastOffset;
            this.aboveOffset = aboveOffset;
            this.southOffset = southOffset;
            this.textureU = textureU;
            this.textureV = textureV;
        }

        public double eastOffset() {
            return eastOffset;
        }

        public double aboveOffset() {
            return aboveOffset;
        }

        public double southOffset() {
            return southOffset;
        }

        public double textureU() {
            return textureU;
        }

        public double textureV() {
            return textureV;
        }
    }
}
