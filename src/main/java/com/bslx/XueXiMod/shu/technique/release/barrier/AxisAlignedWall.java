package com.bslx.XueXiMod.shu.technique.release.barrier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 吸附世界四轴的薄墙。渲染、阻挡和接触都读这一份盒子。 */
public final class AxisAlignedWall {
    public static final double CROSS_EPSILON = 1.0E-6;
    private final AABB box;
    private final Vec3 center;

    public AxisAlignedWall(AABB box) {
        this.box = box;
        this.center = new Vec3((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
    }

    /** 墙的真实盒子。 */
    public AABB box() {
        return box;
    }

    /** 盒子中心。 */
    public Vec3 center() {
        return center;
    }

    /** 目标身体或这一拍的扫掠盒是否碰到墙。 */
    public boolean touches(LivingEntity target, double slop) {
        if (target == null) {
            return false;
        }
        AABB body = target.getBoundingBox();
        AABB swept = body;
        double shiftX = target.xOld - target.getX();
        double shiftY = target.yOld - target.getY();
        double shiftZ = target.zOld - target.getZ();
        if (shiftX * shiftX + shiftY * shiftY + shiftZ * shiftZ > CROSS_EPSILON) {
            swept = body.minmax(body.move(shiftX, shiftY, shiftZ));
        }
        return swept.inflate(slop).intersects(box) && distance(body, box) <= slop;
    }

    /** 运动段在墙内的长度。外表面擦过接近 0，不算穿越。 */
    public double interiorLength(Vec3 from, Vec3 finish) {
        if (from == null || finish == null) {
            return 0.0;
        }
        double spanX = finish.x - from.x;
        double spanY = finish.y - from.y;
        double spanZ = finish.z - from.z;
        double travel = Math.sqrt(spanX * spanX + spanY * spanY + spanZ * spanZ);
        if (travel <= CROSS_EPSILON) {
            return 0.0;
        }
        double[] window = new double[] {0.0, 1.0};
        if (!clip(from.x, spanX, box.minX, box.maxX, window)
                || !clip(from.y, spanY, box.minY, box.maxY, window)
                || !clip(from.z, spanZ, box.minZ, box.maxZ, window)) {
            return 0.0;
        }
        return travel * Math.max(0.0, window[1] - window[0]);
    }

    /** 运动段进入墙的比例。擦过或不相交时返回极大值。 */
    public double entryFraction(Vec3 from, Vec3 finish) {
        if (interiorLength(from, finish) <= CROSS_EPSILON) {
            return Double.MAX_VALUE;
        }
        double spanX = finish.x - from.x;
        double spanY = finish.y - from.y;
        double spanZ = finish.z - from.z;
        double[] window = new double[] {0.0, 1.0};
        if (!clip(from.x, spanX, box.minX, box.maxX, window)
                || !clip(from.y, spanY, box.minY, box.maxY, window)
                || !clip(from.z, spanZ, box.minZ, box.maxZ, window)) {
            return Double.MAX_VALUE;
        }
        return window[0];
    }

    private static boolean clip(double origin, double span, double lower, double upper, double[] window) {
        if (Math.abs(span) <= CROSS_EPSILON) {
            return origin >= lower && origin <= upper;
        }
        double enter = (lower - origin) / span;
        double leave = (upper - origin) / span;
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
        return window[1] > window[0];
    }

    private static double distance(AABB body, AABB wall) {
        double deltaX = gap(body.minX, body.maxX, wall.minX, wall.maxX);
        double deltaY = gap(body.minY, body.maxY, wall.minY, wall.maxY);
        double deltaZ = gap(body.minZ, body.maxZ, wall.minZ, wall.maxZ);
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    private static double gap(double bodyLow, double bodyHigh, double wallLow, double wallHigh) {
        if (bodyHigh < wallLow) {
            return wallLow - bodyHigh;
        }
        if (wallHigh < bodyLow) {
            return bodyLow - wallHigh;
        }
        return 0.0;
    }
}
