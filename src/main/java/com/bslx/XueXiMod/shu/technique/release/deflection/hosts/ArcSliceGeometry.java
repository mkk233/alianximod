package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 缺月只查本拍活动径向三角片。进度由出生 tick 和年龄推出，不读实体上的伤害游标。
 * 窄相是有厚度的三角片，不用宽包围盒当命中。拖尾年龄直接空。
 */
public final class ArcSliceGeometry {
    private ArcSliceGeometry() {
    }

    public record ActiveSpan(double fromProgress, double toProgress) {
    }

    public record ArcHit(double segmentT, Vec3 point, Vec3 unitNormal, double progress) {
    }

    /** 由出生时刻和年龄推出本拍活动进度。拖尾年龄为空。 */
    public static ActiveSpan activeSpan(long gameTick, long birthTick, int activeTicks) {
        if (activeTicks <= 0 || birthTick < 0L) {
            return null;
        }
        long ageTicks = gameTick - birthTick;
        if (ageTicks <= 0L || ageTicks > activeTicks) {
            return null;
        }
        double previous = (ageTicks - 1L) / (double) activeTicks;
        ProgressArcBand.SweepCursor cursor = ProgressArcBand.advance(previous, false, ageTicks, activeTicks);
        if (!cursor.swept() || !(cursor.toProgress() > cursor.fromProgress() + 1.0E-8)) {
            return null;
        }
        return new ActiveSpan(cursor.fromProgress(), cursor.toProgress());
    }

    /** 线段碰到本拍径向三角片的最早点。 */
    public static Optional<ArcHit> earliest(ArcBandSnapshot snapshot, Vec3 from, Vec3 toward,
                                            double projectileRadius, long gameTick) {
        if (snapshot == null || snapshot.mode() != ArcBandMode.RADIAL_BLADE || !finite(from) || !finite(toward)) {
            return Optional.empty();
        }
        if (!Double.isFinite(projectileRadius) || projectileRadius < 0.0 || Math.abs(snapshot.sweepAngle()) <= 1.0E-6) {
            return Optional.empty();
        }
        ActiveSpan span = activeSpan(gameTick, snapshot.birthTick(), snapshot.activeTicks());
        if (span == null) {
            return Optional.empty();
        }
        Vec3 travel = toward.subtract(from);
        Vec3 bladeNormal = orient(vec(snapshot.normalAxis()), travel);
        int pieces = ProgressArcBand.segmentCount(snapshot, span.fromProgress(), span.toProgress());
        if (pieces < 1) {
            pieces = 1;
        }
        double bestT = Double.NaN;
        Vec3 bestPoint = null;
        double bestProgress = span.fromProgress();
        double progressSpan = span.toProgress() - span.fromProgress();
        for (int pieceIndex = 0; pieceIndex < pieces; pieceIndex++) {
            double pieceStart = span.fromProgress() + progressSpan * pieceIndex / pieces;
            double pieceEnd = pieceIndex == pieces - 1
                    ? span.toProgress()
                    : span.fromProgress() + progressSpan * (pieceIndex + 1) / pieces;
            double reach = snapshot.halfWidth()
                    + bowError(snapshot.radius(), snapshot.sweepAngle() * (pieceEnd - pieceStart))
                    + projectileRadius;
            Vec3 first = vec(snapshot.origin());
            Vec3 second = vec(ProgressArcBand.point(snapshot, pieceStart));
            Vec3 third = vec(ProgressArcBand.point(snapshot, pieceEnd));
            double segmentT = earliestTriangle(from, travel, first, second, third, bladeNormal, reach);
            if (!Double.isFinite(segmentT)) {
                continue;
            }
            if (!Double.isFinite(bestT) || segmentT < bestT - 1.0E-9) {
                bestT = segmentT;
                bestPoint = from.add(travel.scale(segmentT));
                bestProgress = progressOf(snapshot, bestPoint, pieceStart, pieceEnd);
            }
        }
        if (!Double.isFinite(bestT) || bestPoint == null) {
            return Optional.empty();
        }
        return Optional.of(new ArcHit(bestT, bestPoint, bladeNormal, bestProgress));
    }

    /**
     * 点到本拍径向三角片的距离不超过片厚加弹体半径。
     * 与线段起点已在片内时返回参数 0 的判断相同，拖尾年龄直接为假。
     */
    public static boolean covers(ArcBandSnapshot snapshot, Vec3 point, double projectileRadius, long gameTick) {
        if (snapshot == null || snapshot.mode() != ArcBandMode.RADIAL_BLADE || !finite(point)) {
            return false;
        }
        if (!Double.isFinite(projectileRadius) || projectileRadius < 0.0 || Math.abs(snapshot.sweepAngle()) <= 1.0E-6) {
            return false;
        }
        ActiveSpan span = activeSpan(gameTick, snapshot.birthTick(), snapshot.activeTicks());
        if (span == null) {
            return false;
        }
        Vec3 bladeNormal = orient(vec(snapshot.normalAxis()), new Vec3(0.0, 1.0, 0.0));
        int pieces = ProgressArcBand.segmentCount(snapshot, span.fromProgress(), span.toProgress());
        if (pieces < 1) {
            pieces = 1;
        }
        double progressSpan = span.toProgress() - span.fromProgress();
        for (int pieceIndex = 0; pieceIndex < pieces; pieceIndex++) {
            double pieceStart = span.fromProgress() + progressSpan * pieceIndex / pieces;
            double pieceEnd = pieceIndex == pieces - 1
                    ? span.toProgress()
                    : span.fromProgress() + progressSpan * (pieceIndex + 1) / pieces;
            double reach = snapshot.halfWidth()
                    + bowError(snapshot.radius(), snapshot.sweepAngle() * (pieceEnd - pieceStart))
                    + projectileRadius;
            Vec3 first = vec(snapshot.origin());
            Vec3 second = vec(ProgressArcBand.point(snapshot, pieceStart));
            Vec3 third = vec(ProgressArcBand.point(snapshot, pieceEnd));
            if (distancePointToTriangle(point, first, second, third, bladeNormal) <= reach + 1.0E-8) {
                return true;
            }
        }
        return false;
    }

    /** 弧终点的切线方向。 */
    public static Vec3 endTangent(ArcBandSnapshot snapshot) {
        if (snapshot == null || Math.abs(snapshot.sweepAngle()) <= 1.0E-8) {
            return null;
        }
        double endAngle = snapshot.startAngle() + snapshot.sweepAngle();
        double sweepSign = snapshot.sweepAngle() >= 0.0 ? 1.0 : -1.0;
        ArcVec forward = snapshot.forwardAxis();
        ArcVec cross = snapshot.crossAxis();
        ArcVec tangent = forward.scale(-Math.sin(endAngle)).plus(cross.scale(Math.cos(endAngle))).scale(sweepSign);
        if (tangent.lengthSquared() < 1.0E-16) {
            return null;
        }
        ArcVec unit = tangent.normalize();
        return new Vec3(unit.east(), unit.above(), unit.south());
    }

    /** 指定进度上的外沿点。 */
    public static Vec3 outerPoint(ArcBandSnapshot snapshot, double progress) {
        double clamped = progress < 0.0 ? 0.0 : (progress > 1.0 ? 1.0 : progress);
        return vec(ProgressArcBand.point(snapshot, clamped));
    }

    /**
     * 接触点按剩余进度滑向外沿。alpha 为 0 时停在接触点，为 1 时到达进度 1 的外沿。
     */
    public static Vec3 transportPosition(ArcBandSnapshot snapshot, Vec3 contact, double contactProgress, double alpha) {
        double clampedAlpha = alpha < 0.0 ? 0.0 : (alpha > 1.0 ? 1.0 : alpha);
        double clampedProgress = contactProgress;
        if (!Double.isFinite(clampedProgress)) {
            clampedProgress = 0.0;
        }
        if (clampedProgress < 0.0) {
            clampedProgress = 0.0;
        }
        if (clampedProgress > 1.0) {
            clampedProgress = 1.0;
        }
        double progress = clampedProgress + (1.0 - clampedProgress) * clampedAlpha;
        Vec3 outer = outerPoint(snapshot, progress);
        Vec3 start = contact == null ? outer : contact;
        return start.add(outer.subtract(start).scale(clampedAlpha));
    }

    /** 捕获后到活动终点还剩多少拍。 */
    public static long remainingTicks(long captureTick, long birthTick, int activeTicks) {
        if (activeTicks <= 0) {
            return 0L;
        }
        long ageTicks = captureTick - birthTick;
        if (ageTicks >= activeTicks) {
            return 0L;
        }
        if (ageTicks < 0L) {
            return activeTicks;
        }
        return activeTicks - ageTicks;
    }

    private static double earliestTriangle(Vec3 from, Vec3 travel, Vec3 first, Vec3 second, Vec3 third,
                                           Vec3 unitNormal, double reach) {
        if (distancePointToTriangle(from, first, second, third, unitNormal) <= reach + 1.0E-8) {
            return 0.0;
        }
        double best = Double.NaN;
        best = earlier(best, planeHit(from, travel, first, unitNormal, reach, first, second, third));
        best = earlier(best, planeHit(from, travel, first, unitNormal.scale(-1.0), reach, first, second, third));
        best = earlier(best, edgeHit(from, travel, first, second, reach, first, second, third, unitNormal));
        best = earlier(best, edgeHit(from, travel, second, third, reach, first, second, third, unitNormal));
        best = earlier(best, edgeHit(from, travel, third, first, reach, first, second, third, unitNormal));
        best = earlier(best, sphereHit(from, travel, first, reach, first, second, third, unitNormal));
        best = earlier(best, sphereHit(from, travel, second, reach, first, second, third, unitNormal));
        best = earlier(best, sphereHit(from, travel, third, reach, first, second, third, unitNormal));
        return best;
    }

    private static double planeHit(Vec3 from, Vec3 travel, Vec3 planePoint, Vec3 planeNormal, double reach,
                                   Vec3 first, Vec3 second, Vec3 third) {
        double gapDelta = planeNormal.dot(travel);
        double startGap = planeNormal.dot(from.subtract(planePoint));
        if (Math.abs(gapDelta) <= 1.0E-12) {
            return Double.NaN;
        }
        double segmentT = (reach - startGap) / gapDelta;
        if (segmentT < -1.0E-7 || segmentT > 1.0 + 1.0E-7) {
            return Double.NaN;
        }
        if (segmentT < 0.0) {
            segmentT = 0.0;
        }
        if (segmentT > 1.0) {
            segmentT = 1.0;
        }
        Vec3 point = from.add(travel.scale(segmentT));
        Vec3 projected = point.subtract(planeNormal.scale(planeNormal.dot(point.subtract(planePoint))));
        if (!insideTriangle(projected, first, second, third)) {
            return Double.NaN;
        }
        return segmentT;
    }

    private static double edgeHit(Vec3 from, Vec3 travel, Vec3 start, Vec3 end, double reach,
                                  Vec3 first, Vec3 second, Vec3 third, Vec3 unitNormal) {
        Vec3 edge = end.subtract(start);
        double edgeLengthSquared = edge.lengthSqr();
        if (edgeLengthSquared < 1.0E-16) {
            return Double.NaN;
        }
        Vec3 offset = from.subtract(start);
        Vec3 travelCross = travel.cross(edge);
        Vec3 offsetCross = offset.cross(edge);
        double coeffA = travelCross.lengthSqr();
        double coeffB = 2.0 * offsetCross.dot(travelCross);
        double coeffC = offsetCross.lengthSqr() - reach * reach * edgeLengthSquared;
        double[] roots = rootsOf(coeffA, coeffB, coeffC);
        double best = Double.NaN;
        for (int rootIndex = 0; rootIndex < roots.length; rootIndex++) {
            double segmentT = roots[rootIndex];
            if (!Double.isFinite(segmentT) || segmentT < -1.0E-7 || segmentT > 1.0 + 1.0E-7) {
                continue;
            }
            if (segmentT < 0.0) {
                segmentT = 0.0;
            }
            if (segmentT > 1.0) {
                segmentT = 1.0;
            }
            Vec3 point = from.add(travel.scale(segmentT));
            double edgeParam = edge.dot(point.subtract(start)) / edgeLengthSquared;
            if (edgeParam < -1.0E-6 || edgeParam > 1.0 + 1.0E-6) {
                continue;
            }
            if (distancePointToTriangle(point, first, second, third, unitNormal) <= reach + 1.0E-4) {
                best = earlier(best, segmentT);
            }
        }
        return best;
    }

    private static double sphereHit(Vec3 from, Vec3 travel, Vec3 center, double reach,
                                    Vec3 first, Vec3 second, Vec3 third, Vec3 unitNormal) {
        Vec3 offset = from.subtract(center);
        double coeffA = travel.lengthSqr();
        double coeffB = 2.0 * offset.dot(travel);
        double coeffC = offset.lengthSqr() - reach * reach;
        double[] roots = rootsOf(coeffA, coeffB, coeffC);
        double best = Double.NaN;
        for (int rootIndex = 0; rootIndex < roots.length; rootIndex++) {
            double segmentT = roots[rootIndex];
            if (!Double.isFinite(segmentT) || segmentT < 0.0 || segmentT > 1.0) {
                continue;
            }
            Vec3 point = from.add(travel.scale(segmentT));
            if (distancePointToTriangle(point, first, second, third, unitNormal) <= reach + 1.0E-4) {
                best = earlier(best, segmentT);
            }
        }
        return best;
    }

    private static double[] rootsOf(double coeffA, double coeffB, double coeffC) {
        double[] roots = new double[] {Double.NaN, Double.NaN};
        if (Math.abs(coeffA) < 1.0E-14) {
            if (Math.abs(coeffB) >= 1.0E-14) {
                roots[0] = -coeffC / coeffB;
            }
            return roots;
        }
        double discriminant = coeffB * coeffB - 4.0 * coeffA * coeffC;
        if (discriminant < 0.0) {
            return roots;
        }
        double root = Math.sqrt(discriminant);
        roots[0] = (-coeffB - root) / (2.0 * coeffA);
        roots[1] = (-coeffB + root) / (2.0 * coeffA);
        return roots;
    }

    private static double distancePointToTriangle(Vec3 point, Vec3 first, Vec3 second, Vec3 third, Vec3 unitNormal) {
        double gap = unitNormal.dot(point.subtract(first));
        Vec3 projected = point.subtract(unitNormal.scale(gap));
        if (insideTriangle(projected, first, second, third)) {
            return Math.abs(gap);
        }
        double best = distancePointToSegment(point, first, second);
        best = Math.min(best, distancePointToSegment(point, second, third));
        best = Math.min(best, distancePointToSegment(point, third, first));
        return best;
    }

    private static double distancePointToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 edge = end.subtract(start);
        double lengthSquared = edge.lengthSqr();
        if (lengthSquared < 1.0E-18) {
            return point.distanceTo(start);
        }
        double param = point.subtract(start).dot(edge) / lengthSquared;
        if (param < 0.0) {
            param = 0.0;
        }
        if (param > 1.0) {
            param = 1.0;
        }
        return point.distanceTo(start.add(edge.scale(param)));
    }

    private static boolean insideTriangle(Vec3 point, Vec3 first, Vec3 second, Vec3 third) {
        Vec3 edgeFirst = second.subtract(first);
        Vec3 edgeSecond = third.subtract(first);
        Vec3 offset = point.subtract(first);
        double firstFirst = edgeFirst.dot(edgeFirst);
        double firstSecond = edgeFirst.dot(edgeSecond);
        double secondSecond = edgeSecond.dot(edgeSecond);
        double determinant = firstFirst * secondSecond - firstSecond * firstSecond;
        if (Math.abs(determinant) < 1.0E-18) {
            return false;
        }
        double inverse = 1.0 / determinant;
        double alongFirst = (secondSecond * edgeFirst.dot(offset) - firstSecond * edgeSecond.dot(offset)) * inverse;
        double alongSecond = (firstFirst * edgeSecond.dot(offset) - firstSecond * edgeFirst.dot(offset)) * inverse;
        double slack = 1.0E-6;
        return alongFirst >= -slack && alongSecond >= -slack && alongFirst + alongSecond <= 1.0 + slack;
    }

    private static double progressOf(ArcBandSnapshot snapshot, Vec3 point, double pieceStart, double pieceEnd) {
        Vec3 origin = vec(snapshot.origin());
        Vec3 offset = point.subtract(origin);
        Vec3 forward = vec(snapshot.forwardAxis());
        Vec3 cross = vec(snapshot.crossAxis());
        double planar = offset.dot(forward) * offset.dot(forward) + offset.dot(cross) * offset.dot(cross);
        if (planar < 1.0E-8 || Math.abs(snapshot.sweepAngle()) < 1.0E-8) {
            return pieceStart;
        }
        double angle = Math.atan2(offset.dot(cross), offset.dot(forward));
        double turned = Math.atan2(Math.sin(angle - snapshot.startAngle()), Math.cos(angle - snapshot.startAngle()));
        double progress = turned / snapshot.sweepAngle();
        if (progress < pieceStart - 0.05 || progress > pieceEnd + 0.05) {
            return (pieceStart + pieceEnd) * 0.5;
        }
        if (progress < pieceStart) {
            return pieceStart;
        }
        if (progress > pieceEnd) {
            return pieceEnd;
        }
        return progress;
    }

    private static double bowError(double radius, double angleSpan) {
        double span = Math.abs(angleSpan);
        if (!(radius > 0.0) || !Double.isFinite(span) || !(span > 0.0)) {
            return 0.0;
        }
        if (span > Math.PI * 2.0) {
            return radius * 2.0;
        }
        double quarterSine = Math.sin(span * 0.25);
        return radius * 2.0 * quarterSine * quarterSine;
    }

    private static Vec3 orient(Vec3 unitNormal, Vec3 travel) {
        if (unitNormal.lengthSqr() < 1.0E-16) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        Vec3 unit = unitNormal.normalize();
        if (travel.lengthSqr() > 1.0E-16 && travel.dot(unit) > 0.0) {
            return unit.scale(-1.0);
        }
        return unit;
    }

    private static double earlier(double current, double candidate) {
        if (!Double.isFinite(candidate)) {
            return current;
        }
        if (!Double.isFinite(current) || candidate < current) {
            return candidate;
        }
        return current;
    }

    static Vec3 vec(ArcVec arc) {
        return new Vec3(arc.east(), arc.above(), arc.south());
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
