package com.bslx.XueXiMod.shu.technique.release.prison.deflection;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 水牢壳的内向外出口。盒子就是服务端快照的 bounds，这里不再外扩。
 * 弹体半径把查询面收进体内，使箭身碰到壳时箭心仍在壳内。
 */
public final class PrisonShellGeometry {
    public static final double SURFACE_CLEARANCE = 0.08D;
    private static final double SLAB_EPSILON = 1.0E-6;

    private PrisonShellGeometry() {
    }

    public record ShellHit(double segmentT, Vec3 point, Vec3 inwardNormal) {
    }

    /** 从壳内穿出时的最早出口。起点不在收进后的壳内则没有接触。 */
    public static Optional<ShellHit> leaving(AABB shell, Vec3 from, Vec3 toward, double bodyRadius) {
        if (shell == null || !finite(from) || !finite(toward) || !Double.isFinite(bodyRadius) || bodyRadius < 0.0D) {
            return Optional.empty();
        }
        Vec3 travel = toward.subtract(from);
        if (travel.lengthSqr() <= 1.0E-12) {
            return Optional.empty();
        }
        AABB query = inset(shell, bodyRadius);
        if (!contains(query, from, SLAB_EPSILON)) {
            return Optional.empty();
        }
        double exitT = Double.POSITIVE_INFINITY;
        Vec3 outward = null;
        AxisPick east = pick(from.x, travel.x, query.minX, query.maxX, new Vec3(-1.0D, 0.0D, 0.0D), new Vec3(1.0D, 0.0D, 0.0D));
        if (east != null && east.exitT() < exitT) {
            exitT = east.exitT();
            outward = east.outward();
        }
        AxisPick above = pick(from.y, travel.y, query.minY, query.maxY, new Vec3(0.0D, -1.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D));
        if (above != null && above.exitT() < exitT) {
            exitT = above.exitT();
            outward = above.outward();
        }
        AxisPick south = pick(from.z, travel.z, query.minZ, query.maxZ, new Vec3(0.0D, 0.0D, -1.0D), new Vec3(0.0D, 0.0D, 1.0D));
        if (south != null && south.exitT() < exitT) {
            exitT = south.exitT();
            outward = south.outward();
        }
        if (outward == null || !Double.isFinite(exitT) || exitT > 1.0D + 1.0E-6) {
            return Optional.empty();
        }
        if (exitT < 0.0D) {
            exitT = 0.0D;
        }
        if (exitT > 1.0D) {
            exitT = 1.0D;
        }
        Vec3 point = from.add(travel.scale(exitT));
        return Optional.of(new ShellHit(exitT, point, outward.scale(-1.0D)));
    }

    /**
     * 箭心加上弹体半径后是否还挨着壳。完全退进壳内则否，整盒内部不算水体。
     */
    public static boolean touches(AABB shell, Vec3 point, double bodyRadius) {
        if (shell == null || !finite(point) || !Double.isFinite(bodyRadius) || bodyRadius < 0.0D) {
            return false;
        }
        double radius = Math.max(0.0D, bodyRadius);
        AABB reach = shell.inflate(radius + SLAB_EPSILON);
        if (!contains(reach, point, 0.0D)) {
            return false;
        }
        AABB interior = inset(shell, radius + SURFACE_CLEARANCE);
        return !contains(interior, point, 0.0D);
    }

    /**
     * 释放点在壳内，并再向内让出一截，使箭身离开壳面。
     * 推不开时落到盒子中心，仍不放到壳外。
     */
    public static Vec3 releaseInside(AABB shell, Vec3 contact, Vec3 inward, Vec3 reflectedUnit, double bodyRadius) {
        Vec3 fallback = shell == null ? (contact == null ? Vec3.ZERO : contact) : shell.getCenter();
        Vec3 origin = contact == null ? fallback : contact;
        Vec3 normal = inward == null || inward.lengthSqr() < 1.0E-16 ? new Vec3(0.0D, 1.0D, 0.0D) : inward.normalize();
        Vec3 unit = reflectedUnit == null || reflectedUnit.lengthSqr() < 1.0E-16 ? normal : reflectedUnit.normalize();
        Vec3 push = unit.dot(normal) > 0.0D ? unit : normal;
        Vec3 released = origin.add(push.scale(SURFACE_CLEARANCE));
        if (shell == null || !contains(shell, released, SLAB_EPSILON) || touches(shell, released, bodyRadius)) {
            return fallback;
        }
        return released;
    }

    /**
     * 法线朝壳内。来速已经朝内时保持原向量，避免再弹回壳外。长度与入射相同。
     */
    public static Vec3 reflectInward(Vec3 incoming, Vec3 inwardNormal) {
        Vec3 velocity = incoming == null ? Vec3.ZERO : incoming;
        Vec3 normal = inwardNormal == null || inwardNormal.lengthSqr() < 1.0E-16
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : inwardNormal.normalize();
        double along = velocity.dot(normal);
        if (along > 0.0D) {
            return velocity;
        }
        return velocity.subtract(normal.scale(2.0D * along));
    }

    private static AxisPick pick(double origin, double travel, double low, double high, Vec3 negative, Vec3 positive) {
        if (travel > 1.0E-12) {
            double exitT = (high - origin) / travel;
            if (exitT >= -1.0E-6) {
                return new AxisPick(Math.max(0.0D, exitT), positive);
            }
            return null;
        }
        if (travel < -1.0E-12) {
            double exitT = (low - origin) / travel;
            if (exitT >= -1.0E-6) {
                return new AxisPick(Math.max(0.0D, exitT), negative);
            }
        }
        return null;
    }

    private static AABB inset(AABB shell, double margin) {
        if (!Double.isFinite(margin) || margin <= 0.0D) {
            return shell;
        }
        double room = Math.min(shell.getXsize(), Math.min(shell.getYsize(), shell.getZsize())) * 0.5D;
        double used = Math.min(margin, Math.max(0.0D, room - 0.02D));
        if (used <= 0.0D) {
            return shell;
        }
        return shell.inflate(-used);
    }

    private static boolean contains(AABB box, Vec3 point, double slack) {
        return point.x >= box.minX - slack && point.x <= box.maxX + slack
                && point.y >= box.minY - slack && point.y <= box.maxY + slack
                && point.z >= box.minZ - slack && point.z <= box.maxZ + slack;
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private record AxisPick(double exitT, Vec3 outward) {
    }
}
