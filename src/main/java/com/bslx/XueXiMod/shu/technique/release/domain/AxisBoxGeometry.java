package com.bslx.XueXiMod.shu.technique.release.domain;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 轴对齐盒子几何实例。生成时写下世界坐标，调用方持有它，之后不跟着施法者走。
 * 球形场地用半径覆盖，护体跟着身体移动，都不是这种固定盒子。
 */
public final class AxisBoxGeometry implements DomainGeometry {
    private static final double EDGE = 1.0E-4;
    private static final double INSIDE_SLACK = 1.0E-3;
    private final AABB bounds;

    /** 直接持有传入的世界包围盒。不复制、不扣资源，空盒也原样保存。 */
    public AxisBoxGeometry(AABB bounds) {
        this.bounds = bounds;
    }

    /** 以脚底为底面中心生成盒子。尺寸原样使用，不扣资源，也不跟随之后的移动。 */
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
    /** 身体三轴都能放进盒子才算装得下。空身体返回假，不扣资源。 */
    public boolean fits(AABB body) {
        if (body == null) {
            return false;
        }
        return body.maxX - body.minX <= bounds.maxX - bounds.minX + EDGE
                && body.maxY - body.minY <= bounds.maxY - bounds.minY + EDGE
                && body.maxZ - body.minZ <= bounds.maxZ - bounds.minZ + EDGE;
    }

    @Override
    /** 身体六面都在盒内才算完整包含。空身体返回假，边缘只留固定浮点余量。 */
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
    /** 只判断身体和盒子是否相交。空身体返回假，不做完整包含收窄。 */
    public boolean intersects(AABB body) {
        return body != null && bounds.intersects(body);
    }

    @Override
    /** 求身体完整位于盒内的位移参数区间。装不下或三轴没有公共区间时返回空。 */
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

    @Override
    /** 在真实包围盒内按轴独立均匀取点，并离开盒的浮点边缘。随机源为空时直接拒绝。 */
    public Vec3 samplePoint(RandomSource random) {
        if (random == null) {
            throw new IllegalArgumentException("domain sample needs a random source");
        }
        double[] across = innerSpan(bounds.minX, bounds.maxX);
        double[] rise = innerSpan(bounds.minY, bounds.maxY);
        double[] depth = innerSpan(bounds.minZ, bounds.maxZ);
        return new Vec3(
                uniform(random, across[0], across[1]),
                uniform(random, rise[0], rise[1]),
                uniform(random, depth[0], depth[1]));
    }

    @Override
    /** 把点收到真实包围盒的内缩范围。空点仍返回空。已经落在范围内的坐标保持不变。 */
    public Vec3 clampPoint(Vec3 point) {
        if (point == null) {
            return null;
        }
        double[] across = innerSpan(bounds.minX, bounds.maxX);
        double[] rise = innerSpan(bounds.minY, bounds.maxY);
        double[] depth = innerSpan(bounds.minZ, bounds.maxZ);
        return new Vec3(
                constrain(point.x, across[0], across[1]),
                constrain(point.y, rise[0], rise[1]),
                constrain(point.z, depth[0], depth[1]));
    }

    /** 单轴可采样区间。宽度够时两端各让出 EDGE，否则只保留中点。 */
    private double[] innerSpan(double boxLow, double boxHigh) {
        double span = boxHigh - boxLow;
        if (Double.isFinite(span) && span > EDGE * 2.0) {
            return new double[] { boxLow + EDGE, boxHigh - EDGE };
        }
        double middle = (boxLow + boxHigh) * 0.5;
        if (!Double.isFinite(middle)) {
            middle = Double.isFinite(boxLow) ? boxLow : (Double.isFinite(boxHigh) ? boxHigh : 0.0);
        }
        return new double[] { middle, middle };
    }

    private static double uniform(RandomSource random, double sampleLow, double sampleHigh) {
        double drawn = random.nextDouble();
        if (!(sampleHigh > sampleLow)) {
            return sampleLow;
        }
        return sampleLow + drawn * (sampleHigh - sampleLow);
    }

    private static double constrain(double value, double sampleLow, double sampleHigh) {
        if (!(sampleHigh > sampleLow)) {
            return sampleLow;
        }
        if (!Double.isFinite(value)) {
            return (sampleLow + sampleHigh) * 0.5;
        }
        if (value < sampleLow) {
            return sampleLow;
        }
        if (value > sampleHigh) {
            return sampleHigh;
        }
        return value;
    }
}
