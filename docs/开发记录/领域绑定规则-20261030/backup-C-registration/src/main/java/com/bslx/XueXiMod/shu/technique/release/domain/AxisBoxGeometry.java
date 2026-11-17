package com.bslx.XueXiMod.shu.technique.release.domain;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 轴对齐盒子。生成时写下世界坐标，之后不跟着施法者走。 */
public final class AxisBoxGeometry implements DomainGeometry {
    private static final double EDGE = 1.0E-4;
    private static final double INSIDE_SLACK = 1.0E-3;
    private final AABB bounds;

    public AxisBoxGeometry(AABB bounds) {
        this.bounds = bounds;
    }

    public static AxisBoxGeometry atFeet(Vec3 feet, double width, double depth, double height) {
        double halfWidth = width * 0.5;
        double halfDepth = depth * 0.5;
        return new AxisBoxGeometry(new AABB(
                feet.x - halfWidth, feet.y, feet.z - halfDepth,
                feet.x + halfWidth, feet.y + height, feet.z + halfDepth));
    }

    /** 绕原盒的水平中心放大，底面停在原来的地面。倍率 1 时范围不变。 */
    public static AxisBoxGeometry scaledFromGround(AABB source, double factor) {
        double widthSpan = source.maxX - source.minX;
        double heightSpan = source.maxY - source.minY;
        double depthSpan = source.maxZ - source.minZ;
        double midpointAcross = (source.minX + source.maxX) * 0.5;
        double midpointDepth = (source.minZ + source.maxZ) * 0.5;
        double ground = source.minY;
        double halfAcross = widthSpan * factor * 0.5;
        double halfDepth = depthSpan * factor * 0.5;
        double risen = heightSpan * factor;
        return new AxisBoxGeometry(new AABB(
                midpointAcross - halfAcross, ground, midpointDepth - halfDepth,
                midpointAcross + halfAcross, ground + risen, midpointDepth + halfDepth));
    }

    @Override
    public AABB bounds() {
        return bounds;
    }

    @Override
    public boolean fits(AABB body) {
        if (body == null) {
            return false;
        }
        return body.maxX - body.minX <= bounds.maxX - bounds.minX + EDGE
                && body.maxY - body.minY <= bounds.maxY - bounds.minY + EDGE
                && body.maxZ - body.minZ <= bounds.maxZ - bounds.minZ + EDGE;
    }

    @Override
    public boolean fullyContains(AABB body) {
        return body != null
                && body.minX >= bounds.minX - EDGE
                && body.minY >= bounds.minY - EDGE
                && body.minZ >= bounds.minZ - EDGE
                && body.maxX <= bounds.maxX + EDGE
                && body.maxY <= bounds.maxY + EDGE
                && body.maxZ <= bounds.maxZ + EDGE;
    }

    @Override
    public boolean intersects(AABB body) {
        return body != null && bounds.intersects(body);
    }

    @Override
    public double[] fullInterval(AABB body, Vec3 displacement) {
        if (body == null || displacement == null || !fits(body)) {
            return null;
        }
        double[] window = new double[] {0.0, 1.0};
        if (!axis(body.minX, body.maxX, bounds.minX, bounds.maxX, displacement.x, window)
                || !axis(body.minY, body.maxY, bounds.minY, bounds.maxY, displacement.y, window)
                || !axis(body.minZ, body.maxZ, bounds.minZ, bounds.maxZ, displacement.z, window)) {
            return null;
        }
        if (window[0] > window[1]) {
            return null;
        }
        return window;
    }

    @Override
    public Vec3 clipInside(AABB body, Vec3 movement) {
        if (body == null || movement == null) {
            return movement;
        }
        return new Vec3(
                stay(body.minX, body.maxX, bounds.minX, bounds.maxX, movement.x),
                stay(body.minY, body.maxY, bounds.minY, bounds.maxY, movement.y),
                stay(body.minZ, body.maxZ, bounds.minZ, bounds.maxZ, movement.z));
    }

    private static double stay(double bodyLow, double bodyHigh, double boxLow, double boxHigh, double delta) {
        if (delta > 0.0) {
            double room = boxHigh - bodyHigh - INSIDE_SLACK;
            if (delta > room) {
                return room > 0.0 ? room : 0.0;
            }
        } else if (delta < 0.0) {
            double room = bodyLow - boxLow - INSIDE_SLACK;
            if (-delta > room) {
                return room > 0.0 ? -room : 0.0;
            }
        }
        return delta;
    }

    private static boolean axis(double bodyLow, double bodyHigh, double boxLow, double boxHigh,
                                double delta, double[] window) {
        if (Math.abs(delta) <= EDGE) {
            return bodyLow >= boxLow - EDGE && bodyHigh <= boxHigh + EDGE;
        }
        double towardLow = (boxLow - bodyLow) / delta;
        double towardHigh = (boxHigh - bodyHigh) / delta;
        double enter = Math.min(towardLow, towardHigh);
        double leave = Math.max(towardLow, towardHigh);
        if (enter > window[0]) {
            window[0] = enter;
        }
        if (leave < window[1]) {
            window[1] = leave;
        }
        return window[0] <= window[1];
    }
}
