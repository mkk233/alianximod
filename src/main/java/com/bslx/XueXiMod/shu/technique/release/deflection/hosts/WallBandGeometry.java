package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 有限轴对齐墙的线段最早接触。盒子就是墙的真实范围，不是无限平面。
 * 弹体半径只把盒子向外扩一圈。反射只看入射和法线，不看射手位置。
 */
public final class WallBandGeometry {
    public static final double SURFACE_NUDGE = 0.05;
    private static final double SLAB_EPSILON = 1.0E-8;

    private WallBandGeometry() {
    }

    public record WallHit(double segmentT, Vec3 point, Vec3 unitNormal) {
    }

    /** 线段进入胀开墙盒的最早点。不相交时为空。 */
    public static Optional<WallHit> earliest(AABB box, Vec3 from, Vec3 toward, double projectileRadius) {
        if (box == null || !finite(from) || !finite(toward) || !Double.isFinite(projectileRadius) || projectileRadius < 0.0) {
            return Optional.empty();
        }
        AABB grown = projectileRadius > 0.0 ? box.inflate(projectileRadius) : box;
        Vec3 travel = toward.subtract(from);
        double[] window = new double[] {0.0, 1.0};
        Vec3[] chosen = new Vec3[] {null};
        if (!slab(from.x, travel.x, grown.minX, grown.maxX, window, new Vec3(-1.0, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0), chosen)
                || !slab(from.y, travel.y, grown.minY, grown.maxY, window, new Vec3(0.0, -1.0, 0.0), new Vec3(0.0, 1.0, 0.0), chosen)
                || !slab(from.z, travel.z, grown.minZ, grown.maxZ, window, new Vec3(0.0, 0.0, -1.0), new Vec3(0.0, 0.0, 1.0), chosen)) {
            return Optional.empty();
        }
        double segmentT = window[0];
        if (segmentT < -1.0E-7 || segmentT > 1.0 + 1.0E-7) {
            return Optional.empty();
        }
        if (segmentT < 0.0) {
            segmentT = 0.0;
        }
        if (segmentT > 1.0) {
            segmentT = 1.0;
        }
        Vec3 point = from.add(travel.scale(segmentT));
        Vec3 unitNormal = chosen[0] == null ? nearestFaceNormal(grown, point) : chosen[0];
        if (travel.lengthSqr() > 1.0E-16 && travel.dot(unitNormal) > 0.0) {
            unitNormal = unitNormal.scale(-1.0);
        }
        return Optional.of(new WallHit(segmentT, point, unitNormal));
    }

    /**
     * 箭心是否落在按弹体半径胀开的墙盒里，含表面。
     * 这和零跨度 slab 的内点判断是同一体积，不通过再跑一遍线段来代替。
     */
    public static boolean covers(AABB box, Vec3 point, double projectileRadius) {
        if (box == null || !finite(point) || !Double.isFinite(projectileRadius) || projectileRadius < 0.0) {
            return false;
        }
        AABB grown = projectileRadius > 0.0 ? box.inflate(projectileRadius) : box;
        return point.x >= grown.minX - SLAB_EPSILON && point.x <= grown.maxX + SLAB_EPSILON
                && point.y >= grown.minY - SLAB_EPSILON && point.y <= grown.maxY + SLAB_EPSILON
                && point.z >= grown.minZ - SLAB_EPSILON && point.z <= grown.maxZ + SLAB_EPSILON;
    }

    /**
     * 法线正负号先拨到朝向来弹的一侧，再做 v' = v - 2 (v·n) n。长度与入射相同。
     */
    public static Vec3 reflect(Vec3 incoming, Vec3 unitNormal) {
        Vec3 velocity = incoming == null ? Vec3.ZERO : incoming;
        Vec3 normal = unitNormal == null || unitNormal.lengthSqr() < 1.0E-16
                ? new Vec3(0.0, 1.0, 0.0)
                : unitNormal.normalize();
        if (velocity.dot(normal) > 0.0) {
            normal = normal.scale(-1.0);
        }
        double along = velocity.dot(normal);
        return velocity.subtract(normal.scale(2.0 * along));
    }

    /**
     * 出口只把弹体沿反射方向挪离表面。墙盒本身不移动。挡住去路的固体把这段截短。
     */
    public static Vec3 nudgeClear(Vec3 contact, Vec3 unitDirection, double distance, AABB... blockers) {
        Vec3 origin = contact == null ? Vec3.ZERO : contact;
        Vec3 direction = unitDirection == null || unitDirection.lengthSqr() < 1.0E-16
                ? new Vec3(0.0, 1.0, 0.0)
                : unitDirection.normalize();
        double travel = Double.isFinite(distance) && distance > 0.0 ? distance : 0.0;
        double limit = 1.0;
        if (blockers != null) {
            Vec3 goal = origin.add(direction.scale(travel));
            for (AABB blocker : blockers) {
                double blocked = blockerEntry(origin, goal, blocker);
                if (blocked >= 0.0 && blocked < limit) {
                    limit = blocked;
                }
            }
        }
        double used = limit < 1.0 ? Math.max(0.0, limit * travel - 0.01) : travel;
        return origin.add(direction.scale(used));
    }

    private static boolean slab(double origin, double span, double lower, double upper, double[] window,
                                Vec3 lowerNormal, Vec3 upperNormal, Vec3[] chosen) {
        if (Math.abs(span) <= SLAB_EPSILON) {
            return origin >= lower - SLAB_EPSILON && origin <= upper + SLAB_EPSILON;
        }
        double enter = (lower - origin) / span;
        double leave = (upper - origin) / span;
        Vec3 enterNormal = lowerNormal;
        if (enter > leave) {
            double swap = enter;
            enter = leave;
            leave = swap;
            enterNormal = upperNormal;
        }
        if (enter > window[0]) {
            window[0] = enter;
            chosen[0] = enterNormal;
        } else if (chosen[0] == null && enter >= window[0] - SLAB_EPSILON) {
            chosen[0] = enterNormal;
        }
        if (leave < window[1]) {
            window[1] = leave;
        }
        return window[1] >= window[0] - SLAB_EPSILON;
    }

    private static double blockerEntry(Vec3 from, Vec3 toward, AABB blocker) {
        if (blocker == null || !finite(from) || !finite(toward)) {
            return -1.0;
        }
        Vec3 travel = toward.subtract(from);
        double[] window = new double[] {0.0, 1.0};
        Vec3[] ignored = new Vec3[] {null};
        if (!slab(from.x, travel.x, blocker.minX, blocker.maxX, window, new Vec3(-1.0, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0), ignored)
                || !slab(from.y, travel.y, blocker.minY, blocker.maxY, window, new Vec3(0.0, -1.0, 0.0), new Vec3(0.0, 1.0, 0.0), ignored)
                || !slab(from.z, travel.z, blocker.minZ, blocker.maxZ, window, new Vec3(0.0, 0.0, -1.0), new Vec3(0.0, 0.0, 1.0), ignored)) {
            return -1.0;
        }
        if (window[0] > 1.0 || window[1] < 0.0) {
            return -1.0;
        }
        return window[0] < 0.0 ? 0.0 : window[0];
    }

    private static Vec3 nearestFaceNormal(AABB box, Vec3 point) {
        double[] gaps = new double[] {
                Math.abs(point.x - box.minX), Math.abs(box.maxX - point.x),
                Math.abs(point.y - box.minY), Math.abs(box.maxY - point.y),
                Math.abs(point.z - box.minZ), Math.abs(box.maxZ - point.z)
        };
        Vec3[] faces = new Vec3[] {
                new Vec3(-1.0, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0),
                new Vec3(0.0, -1.0, 0.0), new Vec3(0.0, 1.0, 0.0),
                new Vec3(0.0, 0.0, -1.0), new Vec3(0.0, 0.0, 1.0)
        };
        int chosen = 0;
        for (int faceIndex = 1; faceIndex < gaps.length; faceIndex++) {
            if (gaps[faceIndex] < gaps[chosen]) {
                chosen = faceIndex;
            }
        }
        return faces[chosen];
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
