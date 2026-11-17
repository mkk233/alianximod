package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 水环弹体用的身体外壳。包住主人当前碰撞盒，并比原版 0.3 的拾取外扩再早一截。
 * 这样穿环心、打头或打脚的箭会在打到人之前被接住。环带几何本身仍不把圆盘当盾。
 */
public final class RingBodyShell {
    /** 原版 {@code ProjectileUtil.getEntityHitResult} 把目标盒外扩的距离。 */
    public static final double PICK_INFLATE = 0.3;
    /** 再往外的提前量，保证接触参数严格早于实体命中。 */
    public static final double OUTER_LEAD = 0.2;
    private static final double SKIN = 1.0E-8;

    private RingBodyShell() {
    }

    public record ShellHit(double segmentT, Vec3 point, Vec3 unitNormal) {
    }

    /** 线段进入主人身体外壳的最早点。外壳比原版拾取范围再早一截。 */
    public static Optional<ShellHit> earliest(AABB ownerBody, Vec3 from, Vec3 toward) {
        if (!finiteBox(ownerBody) || !finite(from) || !finite(toward)) {
            return Optional.empty();
        }
        AABB shell = ownerBody.inflate(PICK_INFLATE + OUTER_LEAD);
        double[] window = clipAxis(from.x, toward.x - from.x, shell.minX, shell.maxX, 0.0, 1.0);
        if (window == null) {
            return Optional.empty();
        }
        window = clipAxis(from.y, toward.y - from.y, shell.minY, shell.maxY, window[0], window[1]);
        if (window == null) {
            return Optional.empty();
        }
        window = clipAxis(from.z, toward.z - from.z, shell.minZ, shell.maxZ, window[0], window[1]);
        if (window == null || window[1] < -SKIN || window[0] > 1.0) {
            return Optional.empty();
        }
        double segmentT = window[0] < 0.0 ? 0.0 : window[0];
        if (segmentT > 1.0) {
            return Optional.empty();
        }
        Vec3 point = new Vec3(
                from.x + (toward.x - from.x) * segmentT,
                from.y + (toward.y - from.y) * segmentT,
                from.z + (toward.z - from.z) * segmentT);
        Vec3 unitNormal = facing(shell, point, from, toward);
        if (!finite(point) || !finite(unitNormal)) {
            return Optional.empty();
        }
        return Optional.of(new ShellHit(segmentT, point, unitNormal));
    }

    private static Vec3 facing(AABB shell, Vec3 point, Vec3 from, Vec3 toward) {
        double westGap = Math.abs(point.x - shell.minX);
        double eastGap = Math.abs(shell.maxX - point.x);
        double downGap = Math.abs(point.y - shell.minY);
        double upGap = Math.abs(shell.maxY - point.y);
        double northGap = Math.abs(point.z - shell.minZ);
        double southGap = Math.abs(shell.maxZ - point.z);
        double nearest = westGap;
        double normalX = -1.0;
        double normalY = 0.0;
        double normalZ = 0.0;
        if (eastGap < nearest) {
            nearest = eastGap;
            normalX = 1.0;
            normalY = 0.0;
            normalZ = 0.0;
        }
        if (downGap < nearest) {
            nearest = downGap;
            normalX = 0.0;
            normalY = -1.0;
            normalZ = 0.0;
        }
        if (upGap < nearest) {
            nearest = upGap;
            normalX = 0.0;
            normalY = 1.0;
            normalZ = 0.0;
        }
        if (northGap < nearest) {
            nearest = northGap;
            normalX = 0.0;
            normalY = 0.0;
            normalZ = -1.0;
        }
        if (southGap < nearest) {
            normalX = 0.0;
            normalY = 0.0;
            normalZ = 1.0;
        }
        double travelX = toward.x - from.x;
        double travelY = toward.y - from.y;
        double travelZ = toward.z - from.z;
        if (travelX * normalX + travelY * normalY + travelZ * normalZ > 0.0) {
            normalX = -normalX;
            normalY = -normalY;
            normalZ = -normalZ;
        }
        return new Vec3(normalX, normalY, normalZ);
    }

    private static double[] clipAxis(double origin, double travel, double lower, double upper, double enter, double leave) {
        if (Math.abs(travel) <= SKIN) {
            if (origin < lower - SKIN || origin > upper + SKIN) {
                return null;
            }
            return new double[] {enter, leave};
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
        return new double[] {nextEnter, nextLeave};
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
