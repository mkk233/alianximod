package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 有厚度的环带。主半径是环心到管心，管半径是带宽的一半再加弹体半径。
 * 环心圆盘和球都不算盾。线段用距离到圆的 Lipschitz 细分找最早接触，两端都在管外仍能命中。
 * 平面基底在捕获时记下，之后只随当前法线做最小旋转，避免倾角一变角度就跳。
 */
public final class RingBandGeometry {
    public static final int ORBIT_TICKS = 6;
    private static final double TURN = Math.PI * 2.0;
    private static final double GOLDEN = 0.6180339887498949;

    private RingBandGeometry() {
    }

    public record RingHit(double segmentT, Vec3 point, Vec3 unitNormal) {
    }

    /** 环平面上的稳定基底。法线变化时只做最小旋转。 */
    public static final class PlaneBasis {
        private final Vec3 eastAxis;
        private final Vec3 northAxis;
        private final Vec3 unitNormal;

        private PlaneBasis(Vec3 eastAxis, Vec3 northAxis, Vec3 unitNormal) {
            this.eastAxis = eastAxis;
            this.northAxis = northAxis;
            this.unitNormal = unitNormal;
        }

        public Vec3 eastAxis() {
            return eastAxis;
        }

        public Vec3 northAxis() {
            return northAxis;
        }

        public Vec3 unitNormal() {
            return unitNormal;
        }

        /** 由当前法线建立基底。 */
        public static PlaneBasis of(Vec3 normal) {
            Vec3 unit = unitOrUp(normal);
            Vec3 reference = Math.abs(unit.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            Vec3 east = reference.cross(unit);
            if (east.lengthSqr() < 1.0E-16) {
                east = new Vec3(0.0, 0.0, 1.0);
            }
            east = east.normalize();
            Vec3 north = unit.cross(east).normalize();
            return new PlaneBasis(east, north, unit);
        }

        /** 把已有基底旋到新法线，不重新抽角度。 */
        public PlaneBasis rotatedTo(Vec3 nextNormal) {
            Vec3 target = unitOrUp(nextNormal);
            Vec3 spunEast = spin(eastAxis, unitNormal, target);
            Vec3 spunNorth = spin(northAxis, unitNormal, target);
            if (spunEast.lengthSqr() < 1.0E-16) {
                return of(target);
            }
            Vec3 east = spunEast.normalize();
            Vec3 north = target.cross(east);
            if (north.lengthSqr() < 1.0E-16) {
                return of(target);
            }
            north = north.normalize();
            if (north.dot(spunNorth) < 0.0) {
                north = north.scale(-1.0);
            }
            return new PlaneBasis(east, north, target);
        }

        /** 平面上这个角度的径向点。 */
        public Vec3 radial(double angle, double radius) {
            return eastAxis.scale(Math.cos(angle) * radius).add(northAxis.scale(Math.sin(angle) * radius));
        }
    }

    /** 线段碰到环管的最早点。环心圆盘不算盾。 */
    public static Optional<RingHit> earliest(Vec3 center, Vec3 planeNormal, double majorRadius, double tubeRadius,
                                              Vec3 from, Vec3 toward, double projectileRadius) {
        if (!finite(center) || !finite(from) || !finite(toward)) {
            return Optional.empty();
        }
        if (!Double.isFinite(majorRadius) || majorRadius <= 1.0E-6
                || !Double.isFinite(tubeRadius) || tubeRadius < 0.0
                || !Double.isFinite(projectileRadius) || projectileRadius < 0.0) {
            return Optional.empty();
        }
        double minor = tubeRadius + projectileRadius;
        PlaneBasis basis = PlaneBasis.of(planeNormal);
        Vec3 travel = toward.subtract(from);
        double segmentT = earliestTube(from, travel, center, basis.unitNormal, majorRadius, minor);
        if (!Double.isFinite(segmentT) || segmentT < -1.0E-7 || segmentT > 1.0 + 1.0E-7) {
            return Optional.empty();
        }
        if (segmentT < 0.0) {
            segmentT = 0.0;
        }
        if (segmentT > 1.0) {
            segmentT = 1.0;
        }
        Vec3 point = from.add(travel.scale(segmentT));
        Vec3 unitNormal = tubeNormal(point, center, basis.unitNormal, majorRadius);
        if (travel.lengthSqr() > 1.0E-16 && travel.dot(unitNormal) > 0.0) {
            unitNormal = unitNormal.scale(-1.0);
        }
        return Optional.of(new RingHit(segmentT, point, unitNormal));
    }

    /**
     * 点到环管中心线的距离不超过管半径加弹体半径。环心圆盘不算在内。
     */
    public static boolean covers(Vec3 center, Vec3 planeNormal, double majorRadius, double tubeRadius,
                                 Vec3 point, double projectileRadius) {
        if (!finite(center) || !finite(point)) {
            return false;
        }
        if (!Double.isFinite(majorRadius) || majorRadius <= 1.0E-6
                || !Double.isFinite(tubeRadius) || tubeRadius < 0.0
                || !Double.isFinite(projectileRadius) || projectileRadius < 0.0) {
            return false;
        }
        PlaneBasis basis = PlaneBasis.of(planeNormal);
        double minor = tubeRadius + projectileRadius;
        return tubeGap(point, center, basis.unitNormal, majorRadius) <= minor + 1.0E-6;
    }

    /**
     * 只抽一次随机角。若径向朝向主人，再用固定黄金角试有限次，取最不朝向主人的那个。
     * 之后不再抽。
     */
    public static double chooseExitAngle(double entryAngle, double randomUnit, Vec3 center, PlaneBasis basis,
                                         double radius, Vec3 ownerAnchor) {
        double seed = Double.isFinite(randomUnit) ? randomUnit : 0.0;
        double first = wrap(seed * TURN);
        double bestAngle = first;
        double bestScore = aimScore(first, center, basis, radius, ownerAnchor);
        if (bestScore <= 0.0) {
            return first;
        }
        for (int attempt = 1; attempt <= 8; attempt++) {
            double candidate = wrap(first + attempt * GOLDEN * TURN);
            double score = aimScore(candidate, center, basis, radius, ownerAnchor);
            if (score < bestScore) {
                bestScore = score;
                bestAngle = candidate;
            }
            if (score <= 0.0) {
                return candidate;
            }
        }
        return bestAngle;
    }

    /** 按入射切向决定顺时针或逆时针绕行。 */
    public static int winding(double entryAngle, Vec3 incoming, PlaneBasis basis) {
        Vec3 tangent = basis.eastAxis.scale(-Math.sin(entryAngle)).add(basis.northAxis.scale(Math.cos(entryAngle)));
        Vec3 velocity = incoming == null ? Vec3.ZERO : incoming;
        return velocity.dot(tangent) >= 0.0 ? 1 : -1;
    }

    /** 按绕行方向计算入口到出口的有符号路程。 */
    public static double signedTravel(double entryAngle, double exitAngle, int windingSign) {
        double delta = wrap(exitAngle) - wrap(entryAngle);
        if (windingSign >= 0) {
            if (delta < 0.0) {
                delta += TURN;
            }
            if (delta < 1.0E-6) {
                delta = TURN;
            }
            return delta;
        }
        if (delta > 0.0) {
            delta -= TURN;
        }
        if (delta > -1.0E-6) {
            delta = -TURN;
        }
        return delta;
    }

    /** 在两段法线之间按时间插值。 */
    public static Vec3 blendNormal(Vec3 fromNormal, Vec3 towardNormal, int phaseStart, int phaseEnd, int now) {
        Vec3 from = unitOrUp(fromNormal);
        Vec3 toward = towardNormal == null || towardNormal.lengthSqr() < 1.0E-16 ? from : towardNormal;
        int span = phaseEnd - phaseStart;
        Vec3 mixed = from;
        if (span > 0 && now >= phaseEnd) {
            mixed = toward;
        } else if (span > 0 && now > phaseStart) {
            double alpha = (now - phaseStart) / (double) span;
            mixed = new Vec3(
                    from.x + (toward.x - from.x) * alpha,
                    from.y + (toward.y - from.y) * alpha,
                    from.z + (toward.z - from.z) * alpha);
        }
        return unitOrUp(mixed);
    }

    /** 接触点在捕获基底上的环向角。 */
    public static double entryAngle(Vec3 contact, Vec3 center, PlaneBasis basis) {
        Vec3 offset = contact.subtract(center);
        Vec3 planar = offset.subtract(basis.unitNormal.scale(offset.dot(basis.unitNormal)));
        if (planar.lengthSqr() < 1.0E-12) {
            return 0.0;
        }
        return Math.atan2(planar.dot(basis.northAxis), planar.dot(basis.eastAxis));
    }

    private static double aimScore(double angle, Vec3 center, PlaneBasis basis, double radius, Vec3 ownerAnchor) {
        Vec3 exit = center.add(basis.radial(angle, radius));
        Vec3 outward = basis.radial(angle, 1.0);
        Vec3 anchor = ownerAnchor == null ? center : ownerAnchor;
        return outward.dot(anchor.subtract(exit));
    }

    private static double earliestTube(Vec3 from, Vec3 travel, Vec3 center, Vec3 normal, double major, double minor) {
        double speed = travel.length();
        double startGap = tubeGap(from, center, normal, major);
        if (startGap <= minor) {
            return 0.0;
        }
        double[] lowEdge = new double[80];
        double[] highEdge = new double[80];
        int stackSize = 1;
        lowEdge[0] = 0.0;
        highEdge[0] = 1.0;
        double best = Double.NaN;
        int steps = 0;
        while (stackSize > 0 && steps < 4096) {
            steps++;
            stackSize--;
            double low = lowEdge[stackSize];
            double high = highEdge[stackSize];
            if (Double.isFinite(best) && low >= best) {
                continue;
            }
            Vec3 lowPoint = from.add(travel.scale(low));
            Vec3 highPoint = from.add(travel.scale(high));
            double lowGap = tubeGap(lowPoint, center, normal, major);
            double highGap = tubeGap(highPoint, center, normal, major);
            double room = (high - low) * speed;
            if (lowGap - room > minor && highGap - room > minor) {
                continue;
            }
            if (room < 1.0E-5 || high - low < 1.0E-8 || stackSize >= lowEdge.length - 2) {
                double candidate = lowGap <= highGap ? low : high;
                double nearestGap = Math.min(lowGap, highGap);
                if (nearestGap <= minor + 1.0E-5 && (!Double.isFinite(best) || candidate < best)) {
                    best = candidate;
                }
                continue;
            }
            double mid = (low + high) * 0.5;
            lowEdge[stackSize] = mid;
            highEdge[stackSize] = high;
            stackSize++;
            lowEdge[stackSize] = low;
            highEdge[stackSize] = mid;
            stackSize++;
        }
        if (!Double.isFinite(best)) {
            return Double.NaN;
        }
        if (tubeGap(from.add(travel.scale(best)), center, normal, major) > minor + 1.0E-5) {
            return best;
        }
        double outside = 0.0;
        double inside = best;
        for (int refine = 0; refine < 40; refine++) {
            double mid = (outside + inside) * 0.5;
            double gap = tubeGap(from.add(travel.scale(mid)), center, normal, major);
            if (gap <= minor + 1.0E-8) {
                inside = mid;
            } else {
                outside = mid;
            }
        }
        return inside;
    }

    private static double tubeGap(Vec3 point, Vec3 center, Vec3 normal, double major) {
        Vec3 offset = point.subtract(center);
        double along = offset.dot(normal);
        Vec3 planar = offset.subtract(normal.scale(along));
        double radial = planar.length();
        double ringGap = radial - major;
        return Math.sqrt(ringGap * ringGap + along * along);
    }

    private static Vec3 tubeNormal(Vec3 point, Vec3 center, Vec3 normal, double major) {
        Vec3 offset = point.subtract(center);
        double along = offset.dot(normal);
        Vec3 planar = offset.subtract(normal.scale(along));
        double radial = planar.length();
        Vec3 circle = radial < 1.0E-8 ? center : center.add(planar.scale(major / radial));
        Vec3 outward = point.subtract(circle);
        if (outward.lengthSqr() < 1.0E-16) {
            outward = radial < 1.0E-8 ? normal : planar;
        }
        if (outward.lengthSqr() < 1.0E-16) {
            return normal;
        }
        return outward.normalize();
    }

    private static Vec3 spin(Vec3 vector, Vec3 fromNormal, Vec3 target) {
        Vec3 axis = fromNormal.cross(target);
        double sine = axis.length();
        double cosine = fromNormal.dot(target);
        if (sine < 1.0E-8) {
            if (cosine >= 0.0) {
                return vector;
            }
            Vec3 pivot = Math.abs(fromNormal.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            Vec3 spinAxis = fromNormal.cross(pivot);
            if (spinAxis.lengthSqr() < 1.0E-16) {
                return vector.scale(-1.0);
            }
            return spinAround(vector, spinAxis.normalize(), Math.PI);
        }
        return spinAround(vector, axis.scale(1.0 / sine), Math.atan2(sine, cosine));
    }

    private static Vec3 spinAround(Vec3 vector, Vec3 axisUnit, double radians) {
        double cosAngle = Math.cos(radians);
        double sinAngle = Math.sin(radians);
        return vector.scale(cosAngle)
                .add(axisUnit.cross(vector).scale(sinAngle))
                .add(axisUnit.scale(axisUnit.dot(vector) * (1.0 - cosAngle)));
    }

    private static double wrap(double angle) {
        double wrapped = angle % TURN;
        if (wrapped < 0.0) {
            wrapped += TURN;
        }
        return wrapped;
    }

    private static Vec3 unitOrUp(Vec3 vector) {
        if (vector == null || vector.lengthSqr() < 1.0E-16) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return vector.normalize();
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
