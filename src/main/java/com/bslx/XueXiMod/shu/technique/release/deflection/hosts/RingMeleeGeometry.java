package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 水环近战护体包络。水平至少盖住环的外半径，竖直只用主人当前包围盒。
 * 这不是渲染出来的环带，也不给箭用。
 */
public final class RingMeleeGeometry {
    private static final double SKIN = 1.0E-8;

    private RingMeleeGeometry() {
    }

    public record MeleeHit(double segmentT, Vec3 point, Vec3 unitNormal) {
    }

    /**
     * 只护这一代主人。线段必须同时碰到包络和主人身体；贴在身体上的零长度也算。
     */
    public static Optional<MeleeHit> protect(boolean generationLive, UUID ownerId, UUID victimId, Vec3 center,
                                              double majorRadius, double tubeRadius, AABB ownerBody,
                                              Vec3 from, Vec3 toward) {
        if (!generationLive || ownerId == null || victimId == null || !ownerId.equals(victimId)) {
            return Optional.empty();
        }
        if (!finite(center) || !finite(from) || !finite(toward) || !finiteBox(ownerBody)) {
            return Optional.empty();
        }
        if (!Double.isFinite(majorRadius) || majorRadius < 0.0 || !Double.isFinite(tubeRadius) || tubeRadius < 0.0) {
            return Optional.empty();
        }
        double outerReach = majorRadius + tubeRadius;
        if (!Double.isFinite(outerReach)) {
            return Optional.empty();
        }
        AABB envelope = envelope(center, outerReach, ownerBody);
        Optional<Double> shieldEntry = earliest(envelope, from, toward);
        if (shieldEntry.isEmpty()) {
            return Optional.empty();
        }
        if (earliest(ownerBody, from, toward).isEmpty()) {
            return Optional.empty();
        }
        double segmentT = shieldEntry.get();
        Vec3 point = pointAt(from, toward, segmentT);
        Vec3 unitNormal = facing(center, point, from, toward);
        if (!finite(point) || !finite(unitNormal)) {
            return Optional.empty();
        }
        return Optional.of(new MeleeHit(segmentT, point, unitNormal));
    }

    private static AABB envelope(Vec3 center, double outerReach, AABB ownerBody) {
        double west = Math.min(ownerBody.minX, center.x - outerReach);
        double east = Math.max(ownerBody.maxX, center.x + outerReach);
        double north = Math.min(ownerBody.minZ, center.z - outerReach);
        double south = Math.max(ownerBody.maxZ, center.z + outerReach);
        return new AABB(west, ownerBody.minY, north, east, ownerBody.maxY, south);
    }

    private static Optional<Double> earliest(AABB box, Vec3 from, Vec3 toward) {
        double[] window = clipAxis(from.x, toward.x - from.x, box.minX, box.maxX, 0.0, 1.0);
        if (window == null) {
            return Optional.empty();
        }
        window = clipAxis(from.y, toward.y - from.y, box.minY, box.maxY, window[0], window[1]);
        if (window == null) {
            return Optional.empty();
        }
        window = clipAxis(from.z, toward.z - from.z, box.minZ, box.maxZ, window[0], window[1]);
        if (window == null) {
            return Optional.empty();
        }
        if (window[1] < -SKIN || window[0] > 1.0) {
            return Optional.empty();
        }
        double segmentT = window[0] < 0.0 ? 0.0 : window[0];
        if (segmentT > 1.0) {
            return Optional.empty();
        }
        return Optional.of(segmentT);
    }

    private static double[] clipAxis(double origin, double travel, double lower, double upper, double enter, double leave) {
        if (Math.abs(travel) <= SKIN) {
            if (origin < lower - SKIN || origin > upper + SKIN) {
                return null;
            }
            return new double[]{enter, leave};
        }
        double near = (lower - origin) / travel;
        double far = (upper - origin) / travel;
        if (near > far) {
            double swap = near;
            near = far;
            far = swap;
        }
        double nextEnter = enter > near ? enter : near;
        double nextLeave = leave < far ? leave : far;
        if (nextEnter > nextLeave + SKIN || nextLeave < -SKIN || nextEnter > 1.0 + SKIN) {
            return null;
        }
        return new double[]{nextEnter, nextLeave};
    }

    private static Vec3 pointAt(Vec3 from, Vec3 toward, double segmentT) {
        return new Vec3(
                from.x + (toward.x - from.x) * segmentT,
                from.y + (toward.y - from.y) * segmentT,
                from.z + (toward.z - from.z) * segmentT);
    }

    private static Vec3 facing(Vec3 center, Vec3 point, Vec3 from, Vec3 toward) {
        double offsetX = point.x - center.x;
        double offsetZ = point.z - center.z;
        double flat = offsetX * offsetX + offsetZ * offsetZ;
        double normalX;
        double normalY;
        double normalZ;
        if (flat > 1.0E-12) {
            double scale = 1.0 / Math.sqrt(flat);
            normalX = offsetX * scale;
            normalY = 0.0;
            normalZ = offsetZ * scale;
        } else {
            normalX = 0.0;
            normalY = 1.0;
            normalZ = 0.0;
        }
        double travelX = toward.x - from.x;
        double travelY = toward.y - from.y;
        double travelZ = toward.z - from.z;
        double alignment = travelX * normalX + travelY * normalY + travelZ * normalZ;
        if (alignment > 0.0) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }
        return new Vec3(normalX, normalY, normalZ);
    }

    private static boolean finiteBox(AABB box) {
        if (box == null) {
            return false;
        }
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
                && box.minX <= box.maxX && box.minY <= box.maxY && box.minZ <= box.maxZ;
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
