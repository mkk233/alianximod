package com.bslx.XueXiMod.shu.technique.release.water;

import java.util.Locale;

/**
 * 冻结后的有限水弧。半径和半宽已经含最终倍率，调用方不要再乘。
 * 本批只从外部传入拍号，不推进拍，也不播放姿态。
 */
public final class ArcBandSnapshot {
    public static final int ACTIVE_TICKS = 6;
    public static final int TAIL_TICKS = 4;
    public static final double BASE_RADIUS = 2.4;
    public static final double BASE_HALF_WIDTH = 0.16;
    /** 一圈。超过这个扫角直接拒绝，不截成一圈，也不按周长取余。 */
    private static final double FULL_TURN = Math.PI * 2.0;
    private static final long UNBORN = -1L;

    private final ArcVec origin;
    private final ArcVec forwardAxis;
    private final ArcVec crossAxis;
    private final ArcVec normalAxis;
    private final int beat;
    private final double radius;
    private final double halfWidth;
    private final double startAngle;
    private final double sweepAngle;
    private final long birthTick;
    private final int activeTicks;
    private final int tailTicks;
    private final ArcBandMode bladeMode;

    private ArcBandSnapshot(ArcVec origin, ArcVec forwardAxis, ArcVec crossAxis, ArcVec normalAxis, int beat,
                            double radius, double halfWidth, double startAngle, double sweepAngle,
                            long birthTick, int activeTicks, int tailTicks, ArcBandMode bladeMode) {
        this.origin = origin;
        this.forwardAxis = forwardAxis;
        this.crossAxis = crossAxis;
        this.normalAxis = normalAxis;
        this.beat = beat;
        this.radius = radius;
        this.halfWidth = halfWidth;
        this.startAngle = startAngle;
        this.sweepAngle = sweepAngle;
        this.birthTick = birthTick;
        this.activeTicks = activeTicks;
        this.tailTicks = tailTicks;
        this.bladeMode = bladeMode;
    }

    public static ArcBandSnapshot of(ArcVec origin, ArcVec forwardAxis, ArcVec crossAxis, ArcVec normalAxis, int beat,
                                     double radius, double halfWidth, double startAngle, double sweepAngle,
                                     long birthTick, int activeTicks, int tailTicks) {
        return of(origin, forwardAxis, crossAxis, normalAxis, beat, radius, halfWidth, startAngle, sweepAngle,
                birthTick, activeTicks, tailTicks, ArcBandMode.OUTER_ARC);
    }

    public static ArcBandSnapshot of(ArcVec origin, ArcVec forwardAxis, ArcVec crossAxis, ArcVec normalAxis, int beat,
                                     double radius, double halfWidth, double startAngle, double sweepAngle,
                                     long birthTick, int activeTicks, int tailTicks, ArcBandMode bladeMode) {
        if (bladeMode == null) {
            return null;
        }
        if (!acceptable(origin, forwardAxis, crossAxis, normalAxis, radius, halfWidth, startAngle, sweepAngle, activeTicks, tailTicks)) {
            return null;
        }
        return new ArcBandSnapshot(origin, forwardAxis, crossAxis, normalAxis, beat, radius, halfWidth,
                startAngle, sweepAngle, birthTick, activeTicks, tailTicks, bladeMode);
    }

    /**
     * 唯一缩放入口。{@code scale} 应是定义能量、链聚能和附魔半径已经乘好的那一个数。
     * 面向提示只在交错明确改过斩面时使用；平行于瞄准的提示不会改冻结朝向。
     */
    public static ArcBandSnapshot freezeLook(ArcVec origin, ArcVec forward, ArcVec facingHint, boolean useFacingHint,
                                              double extraRollDegrees, int beat, double baseRadius, double baseHalfWidth,
                                              double scale) {
        return freezeLook(origin, forward, facingHint, useFacingHint, extraRollDegrees, beat, baseRadius, baseHalfWidth,
                scale, ArcBandMode.OUTER_ARC);
    }

    public static ArcBandSnapshot freezeLook(ArcVec origin, ArcVec forward, ArcVec facingHint, boolean useFacingHint,
                                              double extraRollDegrees, int beat, double baseRadius, double baseHalfWidth,
                                              double scale, ArcBandMode bladeMode) {
        return freezeLook(origin, forward, facingHint, useFacingHint, extraRollDegrees, beat, baseRadius, baseHalfWidth,
                scale, bladeMode, startDegrees(beat), sweepDegrees(beat));
    }

    /**
     * 滚转只加一次：宿主拍的滚转加上调用方传入的附加角。
     * 扇区由调用方最终指定，同一拍上的副本可以换起止，但拍号仍写宿主拍。
     */
    public static ArcBandSnapshot freezeLook(ArcVec origin, ArcVec forward, ArcVec facingHint, boolean useFacingHint,
                                              double extraRollDegrees, int beat, double baseRadius, double baseHalfWidth,
                                              double scale, ArcBandMode bladeMode,
                                              double sectorStartDegrees, double sectorSweepDegrees) {
        if (bladeMode == null || origin == null || !origin.finite()) {
            return null;
        }
        ArcVec facing = forward;
        if (useFacingHint && facingHint != null && facingHint.lengthSquared() > 1.0E-8) {
            facing = facingHint;
        }
        if (facing == null || facing.lengthSquared() < 1.0E-8) {
            return null;
        }
        double appliedScale = Double.isFinite(scale) && scale > 0.0 ? scale : 1.0;
        ArcVec unitForward = facing.normalize();
        ArcVec right = new ArcVec(0.0, 1.0, 0.0).cross(unitForward);
        if (right.lengthSquared() < 1.0E-10) {
            right = new ArcVec(1.0, 0.0, 0.0).cross(unitForward);
        }
        if (right.lengthSquared() < 1.0E-10) {
            return null;
        }
        right = right.normalize();
        double rollRadians = Math.toRadians(rollDegrees(beat) + extraRollDegrees);
        ArcVec rolledRight = rotateAround(right, unitForward, rollRadians).normalize();
        ArcVec normal = unitForward.cross(rolledRight).normalize();
        if (normal.lengthSquared() < 1.0E-10 || rolledRight.lengthSquared() < 1.0E-10) {
            return null;
        }
        return of(origin, unitForward, rolledRight, normal, beat,
                baseRadius * appliedScale, baseHalfWidth * appliedScale,
                Math.toRadians(sectorStartDegrees), Math.toRadians(sectorSweepDegrees),
                UNBORN, ACTIVE_TICKS, TAIL_TICKS, bladeMode);
    }

    public ArcBandSnapshot withBirth(long bornTick) {
        if (bornTick == birthTick) {
            return this;
        }
        return new ArcBandSnapshot(origin, forwardAxis, crossAxis, normalAxis, beat, radius, halfWidth,
                startAngle, sweepAngle, bornTick, activeTicks, tailTicks, bladeMode);
    }

    public String pack() {
        return String.format(Locale.ROOT,
                "%.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %.6f %d %.6f %.6f %.8f %s %d %d %d %s",
                origin.east(), origin.above(), origin.south(),
                forwardAxis.east(), forwardAxis.above(), forwardAxis.south(),
                crossAxis.east(), crossAxis.above(), crossAxis.south(),
                normalAxis.east(), normalAxis.above(), normalAxis.south(),
                beat, radius, halfWidth, startAngle, Double.toString(sweepAngle), activeTicks, tailTicks, birthTick,
                bladeMode.name());
    }

    public static ArcBandSnapshot unpack(String packed) {
        if (packed == null || packed.isBlank()) {
            return null;
        }
        String[] tokens = packed.trim().split(" ");
        if (tokens.length != 20 && tokens.length != 21) {
            return null;
        }
        ArcBandMode unpackedMode = ArcBandMode.OUTER_ARC;
        if (tokens.length == 21) {
            unpackedMode = ArcBandMode.fromPacked(tokens[20]);
            if (unpackedMode == null) {
                return null;
            }
        }
        try {
            ArcVec unpackedOrigin = new ArcVec(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]));
            ArcVec unpackedForward = new ArcVec(Double.parseDouble(tokens[3]), Double.parseDouble(tokens[4]), Double.parseDouble(tokens[5]));
            ArcVec unpackedCross = new ArcVec(Double.parseDouble(tokens[6]), Double.parseDouble(tokens[7]), Double.parseDouble(tokens[8]));
            ArcVec unpackedNormal = new ArcVec(Double.parseDouble(tokens[9]), Double.parseDouble(tokens[10]), Double.parseDouble(tokens[11]));
            int unpackedBeat = Integer.parseInt(tokens[12]);
            double unpackedRadius = Double.parseDouble(tokens[13]);
            double unpackedHalf = Double.parseDouble(tokens[14]);
            double unpackedStart = Double.parseDouble(tokens[15]);
            double unpackedSweep = Double.parseDouble(tokens[16]);
            int unpackedActive = Integer.parseInt(tokens[17]);
            int unpackedTail = Integer.parseInt(tokens[18]);
            long unpackedBirth = Long.parseLong(tokens[19]);
            return of(unpackedOrigin, unpackedForward, unpackedCross, unpackedNormal, unpackedBeat,
                    unpackedRadius, unpackedHalf, unpackedStart, unpackedSweep, unpackedBirth, unpackedActive, unpackedTail,
                    unpackedMode);
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    public ArcVec origin() {
        return origin;
    }

    public ArcVec forwardAxis() {
        return forwardAxis;
    }

    public ArcVec crossAxis() {
        return crossAxis;
    }

    public ArcVec normalAxis() {
        return normalAxis;
    }

    public int beat() {
        return beat;
    }

    public double radius() {
        return radius;
    }

    public double halfWidth() {
        return halfWidth;
    }

    public double startAngle() {
        return startAngle;
    }

    public double sweepAngle() {
        return sweepAngle;
    }

    public long birthTick() {
        return birthTick;
    }

    public int activeTicks() {
        return activeTicks;
    }

    public int tailTicks() {
        return tailTicks;
    }

    public ArcBandMode mode() {
        return bladeMode;
    }

    public int lifeTicks() {
        return activeTicks + tailTicks;
    }

    static double rollDegrees(int beat) {
        return switch (Math.floorMod(beat, 3)) {
            case 1 -> -40.0;
            case 2 -> 0.0;
            default -> 40.0;
        };
    }

    static double startDegrees(int beat) {
        return switch (Math.floorMod(beat, 3)) {
            case 1 -> 50.0;
            case 2 -> -70.0;
            default -> -50.0;
        };
    }

    static double sweepDegrees(int beat) {
        return switch (Math.floorMod(beat, 3)) {
            case 1 -> -90.0;
            case 2 -> 140.0;
            default -> 90.0;
        };
    }

    private static ArcVec rotateAround(ArcVec vector, ArcVec axisUnit, double radians) {
        double cosAngle = Math.cos(radians);
        double sinAngle = Math.sin(radians);
        return vector.scale(cosAngle)
                .plus(axisUnit.cross(vector).scale(sinAngle))
                .plus(axisUnit.scale(axisUnit.dot(vector) * (1.0 - cosAngle)));
    }

    private static boolean acceptable(ArcVec origin, ArcVec forwardAxis, ArcVec crossAxis, ArcVec normalAxis,
                                      double radius, double halfWidth, double startAngle, double sweepAngle,
                                      int activeTicks, int tailTicks) {
        if (origin == null || forwardAxis == null || crossAxis == null || normalAxis == null) {
            return false;
        }
        if (!origin.finite() || !forwardAxis.finite() || !crossAxis.finite() || !normalAxis.finite()) {
            return false;
        }
        if (!Double.isFinite(radius) || !Double.isFinite(halfWidth) || !Double.isFinite(startAngle) || !Double.isFinite(sweepAngle)) {
            return false;
        }
        if (radius <= 1.0E-6 || halfWidth <= 1.0E-6 || Math.abs(sweepAngle) <= 1.0E-6) {
            return false;
        }
        if (Math.abs(sweepAngle) > FULL_TURN) {
            return false;
        }
        if (activeTicks <= 0 || tailTicks < 0) {
            return false;
        }
        if (Math.abs(forwardAxis.lengthSquared() - 1.0) > 1.0E-3
                || Math.abs(crossAxis.lengthSquared() - 1.0) > 1.0E-3
                || Math.abs(normalAxis.lengthSquared() - 1.0) > 1.0E-3) {
            return false;
        }
        if (Math.abs(forwardAxis.dot(crossAxis)) > 1.0E-3
                || Math.abs(forwardAxis.dot(normalAxis)) > 1.0E-3
                || Math.abs(crossAxis.dot(normalAxis)) > 1.0E-3) {
            return false;
        }
        return forwardAxis.cross(crossAxis).dot(normalAxis) > 0.5;
    }
}
