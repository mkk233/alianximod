package com.bslx.XueXiMod.shu.technique.release.water;

/**
 * 服务端窄弧和客户端水带共用的采样。宽包围盒只负责查询，不直接当命中。
 * 外环窄检在弦段半径上加上这段的真实弓高，宽盒按同一半径全轴包住，避免真实弧外缘先被漏掉。
 * 径向刃片沿用同一套角分段，把原点和两个外沿端点收成有限厚度三角片。
 * 一次求交最多 256 段，不再按段嵌套拆分。
 */
public final class ProgressArcBand {
    private static final int SEGMENT_LIMIT = 256;
    private static final double REACH_EPSILON = 1.0E-6;
    /** 整圈至少按这个角距布点，避免起终重合时背面没有弦段。 */
    private static final double COVER_SPAN = Math.PI / 6.0;
    /** 几何测试用来确认一次求交没有再乘出第二层循环。 */
    private static int walkedChords;

    private ProgressArcBand() {
    }

    public static ArcVec direction(ArcBandSnapshot snapshot, double progress) {
        double angle = snapshot.startAngle() + snapshot.sweepAngle() * progress;
        return snapshot.forwardAxis().scale(Math.cos(angle)).plus(snapshot.crossAxis().scale(Math.sin(angle)));
    }

    public static ArcVec point(ArcBandSnapshot snapshot, double progress) {
        return snapshot.origin().plus(direction(snapshot, progress).scale(snapshot.radius()));
    }

    public static int segmentCount(ArcBandSnapshot snapshot, double fromProgress, double toProgress) {
        double progressSpan = Math.abs(toProgress - fromProgress);
        if (progressSpan <= 1.0E-8) {
            return 0;
        }
        double angleSpan = Math.abs(snapshot.sweepAngle()) * progressSpan;
        double sagittaLimit = Math.max(snapshot.halfWidth() * 0.25, 1.0E-4);
        double estimated = angleSpan * Math.sqrt(snapshot.radius() / (8.0 * sagittaLimit));
        int count = (int) Math.ceil(estimated);
        if (count < 1) {
            count = 1;
        }
        int cover = (int) Math.ceil(angleSpan / COVER_SPAN - 1.0E-12);
        if (cover > count) {
            count = cover;
        }
        if (count > SEGMENT_LIMIT) {
            count = SEGMENT_LIMIT;
        }
        return count;
    }

    public static ArcBounds sliceBroadBounds(ArcBandSnapshot snapshot, double fromProgress, double toProgress) {
        if (snapshot == null) {
            return new ArcBounds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        if (snapshot.mode() == ArcBandMode.RADIAL_BLADE) {
            return radialBroadBounds(snapshot, fromProgress, toProgress);
        }
        double[] extent = new double[] {
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        boolean[] sawChord = new boolean[] {false};
        forEachChord(snapshot, fromProgress, toProgress, (startProgress, endProgress) -> {
            double reach = chordReach(snapshot, startProgress, endProgress);
            includeBall(extent, point(snapshot, startProgress), reach);
            includeBall(extent, point(snapshot, endProgress), reach);
            sawChord[0] = true;
            return false;
        });
        if (!sawChord[0]) {
            ArcVec lone = point(snapshot, fromProgress);
            return ballAround(lone, snapshot.halfWidth() + REACH_EPSILON);
        }
        return new ArcBounds(extent[0], extent[1], extent[2], extent[3], extent[4], extent[5]);
    }

    public static boolean intersectsSlice(ArcBandSnapshot snapshot, ArcBounds target, double fromProgress, double toProgress) {
        if (snapshot == null || target == null || !(toProgress > fromProgress + 1.0E-8)) {
            return false;
        }
        if (snapshot.mode() == ArcBandMode.RADIAL_BLADE) {
            return radialIntersects(snapshot, target, fromProgress, toProgress);
        }
        if (!sliceBroadBounds(snapshot, fromProgress, toProgress).overlaps(target)) {
            return false;
        }
        boolean[] hit = new boolean[] {false};
        forEachChord(snapshot, fromProgress, toProgress, (startProgress, endProgress) -> {
            if (hit[0]) {
                return true;
            }
            double reach = chordReach(snapshot, startProgress, endProgress);
            hit[0] = distanceSegmentToBounds(point(snapshot, startProgress), point(snapshot, endProgress), target) <= reach;
            return hit[0];
        });
        return hit[0];
    }

    /** 先补上这次年龄新扫到的弧，再按活跃拍数关闭后续伤害。没有进度增长则不产生命中。 */
    public static SweepCursor advance(double lastProgress, boolean damageClosed, long ageTicks, int activeTicks) {
        if (damageClosed) {
            return new SweepCursor(lastProgress, lastProgress, lastProgress, false, true);
        }
        int safeActive = Math.max(1, activeTicks);
        double current = ageTicks <= 0L ? 0.0 : Math.min(1.0, ageTicks / (double) safeActive);
        if (!(current > lastProgress + 1.0E-8)) {
            return new SweepCursor(lastProgress, lastProgress, lastProgress, false, ageTicks >= safeActive);
        }
        return new SweepCursor(current, lastProgress, current, true, ageTicks >= safeActive);
    }

    public static final class SweepCursor {
        private final double lastProgress;
        private final double fromProgress;
        private final double toProgress;
        private final boolean swept;
        private final boolean damageClosed;

        public SweepCursor(double lastProgress, double fromProgress, double toProgress, boolean swept, boolean damageClosed) {
            this.lastProgress = lastProgress;
            this.fromProgress = fromProgress;
            this.toProgress = toProgress;
            this.swept = swept;
            this.damageClosed = damageClosed;
        }

        public double lastProgress() {
            return lastProgress;
        }

        public double fromProgress() {
            return fromProgress;
        }

        public double toProgress() {
            return toProgress;
        }

        public boolean swept() {
            return swept;
        }

        public boolean damageClosed() {
            return damageClosed;
        }
    }

    static void resetWalkedChords() {
        walkedChords = 0;
    }

    static int walkedChords() {
        return walkedChords;
    }

    private interface ChordWalk {
        boolean accept(double startProgress, double endProgress);
    }

    private static void forEachChord(ArcBandSnapshot snapshot, double fromProgress, double toProgress, ChordWalk walk) {
        int pieces = segmentCount(snapshot, fromProgress, toProgress);
        if (pieces <= 0) {
            return;
        }
        double span = toProgress - fromProgress;
        for (int pieceIndex = 0; pieceIndex < pieces; pieceIndex++) {
            double pieceStart = fromProgress + span * pieceIndex / pieces;
            double pieceEnd = pieceIndex == pieces - 1 ? toProgress : fromProgress + span * (pieceIndex + 1) / pieces;
            walkedChords++;
            if (walk.accept(pieceStart, pieceEnd)) {
                return;
            }
        }
    }

    private static ArcBounds radialBroadBounds(ArcBandSnapshot snapshot, double fromProgress, double toProgress) {
        double[] extent = new double[] {
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        boolean[] sawChord = new boolean[] {false};
        forEachChord(snapshot, fromProgress, toProgress, (startProgress, endProgress) -> {
            double reach = chordReach(snapshot, startProgress, endProgress);
            includeBall(extent, snapshot.origin(), reach);
            includeBall(extent, point(snapshot, startProgress), reach);
            includeBall(extent, point(snapshot, endProgress), reach);
            sawChord[0] = true;
            return false;
        });
        if (!sawChord[0]) {
            double reach = snapshot.halfWidth() + REACH_EPSILON;
            includeBall(extent, snapshot.origin(), reach);
            includeBall(extent, point(snapshot, fromProgress), reach);
        }
        return new ArcBounds(extent[0], extent[1], extent[2], extent[3], extent[4], extent[5]);
    }

    private static boolean radialIntersects(ArcBandSnapshot snapshot, ArcBounds target, double fromProgress, double toProgress) {
        if (!sliceBroadBounds(snapshot, fromProgress, toProgress).overlaps(target)) {
            return false;
        }
        boolean[] hit = new boolean[] {false};
        forEachChord(snapshot, fromProgress, toProgress, (startProgress, endProgress) -> {
            if (hit[0]) {
                return true;
            }
            double reach = chordReach(snapshot, startProgress, endProgress);
            double gap = RadialBladeVolume.gapToBounds(
                    snapshot.origin(), point(snapshot, startProgress), point(snapshot, endProgress), target);
            hit[0] = gap <= reach;
            return hit[0];
        });
        return hit[0];
    }

    /** 半宽加这段弦的真实弓高。大于半圈时用真实弓高，不用半圈值假装盖住。 */
    private static double chordReach(ArcBandSnapshot snapshot, double startProgress, double endProgress) {
        double angleSpan = snapshot.sweepAngle() * (endProgress - startProgress);
        return snapshot.halfWidth() + bowError(snapshot.radius(), angleSpan) + REACH_EPSILON;
    }

    private static double bowError(double radius, double angleSpan) {
        double span = Math.abs(angleSpan);
        if (!(radius > 0.0) || !Double.isFinite(radius) || !(span > 0.0) || !Double.isFinite(span)) {
            return 0.0;
        }
        if (span > Math.PI * 2.0) {
            return radius * 2.0;
        }
        double quarterSine = Math.sin(span * 0.25);
        return radius * 2.0 * quarterSine * quarterSine;
    }

    private static void includeBall(double[] extent, ArcVec center, double reach) {
        extent[0] = Math.min(extent[0], center.east() - reach);
        extent[1] = Math.min(extent[1], center.above() - reach);
        extent[2] = Math.min(extent[2], center.south() - reach);
        extent[3] = Math.max(extent[3], center.east() + reach);
        extent[4] = Math.max(extent[4], center.above() + reach);
        extent[5] = Math.max(extent[5], center.south() + reach);
    }

    private static ArcBounds ballAround(ArcVec center, double reach) {
        return new ArcBounds(
                center.east() - reach, center.above() - reach, center.south() - reach,
                center.east() + reach, center.above() + reach, center.south() + reach);
    }

    static double distanceSegmentToBounds(ArcVec start, ArcVec end, ArcBounds target) {
        if (segmentHits(start, end, target)) {
            return 0.0;
        }
        double best = distancePointToBounds(start, target);
        best = Math.min(best, distancePointToBounds(end, target));
        best = Math.min(best, parallelFaceDistance(start, end, target));
        best = Math.min(best, edgeDistance(start, end, target));
        return best;
    }

    private static double distancePointToBounds(ArcVec point, ArcBounds target) {
        double east = clamp(point.east(), target.lowEast(), target.highEast());
        double above = clamp(point.above(), target.lowAbove(), target.highAbove());
        double south = clamp(point.south(), target.lowSouth(), target.highSouth());
        double deltaEast = point.east() - east;
        double deltaAbove = point.above() - above;
        double deltaSouth = point.south() - south;
        return Math.sqrt(deltaEast * deltaEast + deltaAbove * deltaAbove + deltaSouth * deltaSouth);
    }

    private static double parallelFaceDistance(ArcVec start, ArcVec end, ArcBounds target) {
        double best = Double.POSITIVE_INFINITY;
        double deltaEast = end.east() - start.east();
        double deltaAbove = end.above() - start.above();
        double deltaSouth = end.south() - start.south();
        if (Math.abs(deltaEast) <= 1.0E-10
                && overlapsPlane(start.above(), start.south(), end.above(), end.south(),
                target.lowAbove(), target.highAbove(), target.lowSouth(), target.highSouth())) {
            best = Math.min(best, outsideDistance(start.east(), target.lowEast(), target.highEast()));
        }
        if (Math.abs(deltaAbove) <= 1.0E-10
                && overlapsPlane(start.east(), start.south(), end.east(), end.south(),
                target.lowEast(), target.highEast(), target.lowSouth(), target.highSouth())) {
            best = Math.min(best, outsideDistance(start.above(), target.lowAbove(), target.highAbove()));
        }
        if (Math.abs(deltaSouth) <= 1.0E-10
                && overlapsPlane(start.east(), start.above(), end.east(), end.above(),
                target.lowEast(), target.highEast(), target.lowAbove(), target.highAbove())) {
            best = Math.min(best, outsideDistance(start.south(), target.lowSouth(), target.highSouth()));
        }
        return best;
    }

    private static double outsideDistance(double value, double lowBound, double highBound) {
        if (value < lowBound) {
            return lowBound - value;
        }
        if (value > highBound) {
            return value - highBound;
        }
        return 0.0;
    }

    private static boolean overlapsPlane(double startU, double startV, double endU, double endV,
                                         double lowU, double highU, double lowV, double highV) {
        double[] window = new double[] {0.0, 1.0};
        return clipSlab(startU, endU - startU, lowU, highU, window)
                && clipSlab(startV, endV - startV, lowV, highV, window);
    }

    private static double edgeDistance(ArcVec start, ArcVec end, ArcBounds target) {
        double lowEast = target.lowEast();
        double highEast = target.highEast();
        double lowAbove = target.lowAbove();
        double highAbove = target.highAbove();
        double lowSouth = target.lowSouth();
        double highSouth = target.highSouth();
        ArcVec[] corners = new ArcVec[] {
                new ArcVec(lowEast, lowAbove, lowSouth),
                new ArcVec(highEast, lowAbove, lowSouth),
                new ArcVec(lowEast, highAbove, lowSouth),
                new ArcVec(highEast, highAbove, lowSouth),
                new ArcVec(lowEast, lowAbove, highSouth),
                new ArcVec(highEast, lowAbove, highSouth),
                new ArcVec(lowEast, highAbove, highSouth),
                new ArcVec(highEast, highAbove, highSouth)
        };
        int[][] edges = new int[][] {
                {0, 1}, {2, 3}, {4, 5}, {6, 7},
                {0, 2}, {1, 3}, {4, 6}, {5, 7},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        double best = Double.POSITIVE_INFINITY;
        for (int[] edge : edges) {
            best = Math.min(best, segmentSegmentDistance(start, end, corners[edge[0]], corners[edge[1]]));
        }
        return best;
    }

    static double segmentSegmentDistance(ArcVec firstStart, ArcVec firstEnd, ArcVec secondStart, ArcVec secondEnd) {
        ArcVec firstDelta = firstEnd.minus(firstStart);
        ArcVec secondDelta = secondEnd.minus(secondStart);
        ArcVec between = firstStart.minus(secondStart);
        double firstLength = firstDelta.lengthSquared();
        double secondLength = secondDelta.lengthSquared();
        double mixed = firstDelta.dot(secondDelta);
        double firstSide = firstDelta.dot(between);
        double secondSide = secondDelta.dot(between);
        double denominator = firstLength * secondLength - mixed * mixed;
        double firstT = 0.0;
        double secondT = 0.0;
        if (denominator > 1.0E-12) {
            firstT = clamp((mixed * secondSide - secondLength * firstSide) / denominator, 0.0, 1.0);
        }
        double secondNumer = mixed * firstT + secondSide;
        if (secondLength <= 1.0E-12) {
            secondT = 0.0;
            firstT = clamp(-firstSide / Math.max(firstLength, 1.0E-12), 0.0, 1.0);
        } else {
            secondT = clamp(secondNumer / secondLength, 0.0, 1.0);
            if (secondNumer < 0.0 || secondNumer > secondLength) {
                firstT = clamp((mixed * secondT - firstSide) / Math.max(firstLength, 1.0E-12), 0.0, 1.0);
            }
        }
        ArcVec closest = between.plus(firstDelta.scale(firstT)).minus(secondDelta.scale(secondT));
        return closest.length();
    }

    static boolean segmentHits(ArcVec start, ArcVec end, ArcBounds target) {
        double[] window = new double[] {0.0, 1.0};
        return clipSlab(start.east(), end.east() - start.east(), target.lowEast(), target.highEast(), window)
                && clipSlab(start.above(), end.above() - start.above(), target.lowAbove(), target.highAbove(), window)
                && clipSlab(start.south(), end.south() - start.south(), target.lowSouth(), target.highSouth(), window);
    }

    private static boolean clipSlab(double start, double delta, double lowBound, double highBound, double[] window) {
        if (Math.abs(delta) <= 1.0E-12) {
            return start >= lowBound && start <= highBound;
        }
        double enter = (lowBound - start) / delta;
        double leave = (highBound - start) / delta;
        if (enter > leave) {
            double swap = enter;
            enter = leave;
            leave = swap;
        }
        if (enter > window[0]) {
            window[0] = enter;
        }
        if (leave < window[1]) {
            window[1] = leave;
        }
        return window[0] <= window[1];
    }

    private static double clamp(double value, double lowBound, double highBound) {
        return Math.max(lowBound, Math.min(highBound, value));
    }
}
