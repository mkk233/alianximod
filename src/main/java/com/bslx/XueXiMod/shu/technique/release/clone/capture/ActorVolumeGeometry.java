package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 断潮静止斩的正交基和有向包围盒。
 * 视线接近竖直时，临时右轴用世界 X；粗查询只用外接 AABB，命中必须经过分离轴。
 */
public final class ActorVolumeGeometry {
    private static final double VERTICAL_CUTOFF = 0.999d;
    private static final double UNIT_LOWER = 0.98d;
    private static final double UNIT_UPPER = 1.02d;
    private static final double AXIS_EPSILON = 1.0E-8d;
    private static final double SEPARATION_SLOP = 1.0E-4d;

    private ActorVolumeGeometry() {
    }

    public static @Nullable Vec3[] basis(@Nullable Vec3 lookUnit) {
        if (!unitAxis(lookUnit)) {
            return null;
        }
        Vec3 forwardUnit = lookUnit.normalize();
        Vec3 rightUnit;
        if (Math.abs(forwardUnit.y) > VERTICAL_CUTOFF) {
            rightUnit = new Vec3(1.0d, 0.0d, 0.0d);
        } else {
            Vec3 crossed = forwardUnit.cross(new Vec3(0.0d, 1.0d, 0.0d));
            if (crossed.lengthSqr() < AXIS_EPSILON) {
                return null;
            }
            rightUnit = crossed.normalize();
        }
        Vec3 upUnit = rightUnit.cross(forwardUnit);
        if (upUnit.lengthSqr() < AXIS_EPSILON) {
            return null;
        }
        upUnit = upUnit.normalize();
        if (!unitAxis(forwardUnit) || !unitAxis(upUnit) || !unitAxis(rightUnit)) {
            return null;
        }
        return new Vec3[] {forwardUnit, upUnit, rightUnit};
    }

    public static @Nullable AABB worldBounds(@Nullable ActorVolumeSpec spec) {
        Vec3[] corners = corners(spec);
        if (corners == null) {
            return null;
        }
        double lowerX = Double.POSITIVE_INFINITY;
        double lowerY = Double.POSITIVE_INFINITY;
        double lowerZ = Double.POSITIVE_INFINITY;
        double upperX = Double.NEGATIVE_INFINITY;
        double upperY = Double.NEGATIVE_INFINITY;
        double upperZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            lowerX = Math.min(lowerX, corner.x);
            lowerY = Math.min(lowerY, corner.y);
            lowerZ = Math.min(lowerZ, corner.z);
            upperX = Math.max(upperX, corner.x);
            upperY = Math.max(upperY, corner.y);
            upperZ = Math.max(upperZ, corner.z);
        }
        if (!Double.isFinite(lowerX) || !Double.isFinite(lowerY) || !Double.isFinite(lowerZ)
                || !Double.isFinite(upperX) || !Double.isFinite(upperY) || !Double.isFinite(upperZ)) {
            return null;
        }
        return new AABB(lowerX, lowerY, lowerZ, upperX, upperY, upperZ);
    }

    public static boolean intersectsObb(@Nullable ActorVolumeSpec spec, @Nullable AABB targetBounds) {
        if (!shapeReady(spec) || !finiteBounds(targetBounds)) {
            return false;
        }
        Vec3 center = centerOf(spec);
        if (center == null) {
            return false;
        }
        Vec3 targetCenter = new Vec3(
                (targetBounds.minX + targetBounds.maxX) * 0.5d,
                (targetBounds.minY + targetBounds.maxY) * 0.5d,
                (targetBounds.minZ + targetBounds.maxZ) * 0.5d);
        Vec3 delta = targetCenter.subtract(center);
        Vec3 rightUnit = spec.rightUnit();
        Vec3 upUnit = spec.upUnit();
        Vec3 forwardUnit = spec.forwardUnit();
        Vec3 worldX = new Vec3(1.0d, 0.0d, 0.0d);
        Vec3 worldY = new Vec3(0.0d, 1.0d, 0.0d);
        Vec3 worldZ = new Vec3(0.0d, 0.0d, 1.0d);
        Vec3[] primary = {rightUnit, upUnit, forwardUnit, worldX, worldY, worldZ};
        for (Vec3 axis : primary) {
            if (separated(delta, axis, spec, targetBounds)) {
                return false;
            }
        }
        Vec3[] boxAxes = {rightUnit, upUnit, forwardUnit};
        Vec3[] worldAxes = {worldX, worldY, worldZ};
        for (Vec3 boxAxis : boxAxes) {
            for (Vec3 worldAxis : worldAxes) {
                if (separated(delta, boxAxis.cross(worldAxis), spec, targetBounds)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean usable(@Nullable ActorVolumeSpec spec) {
        if (!shapeReady(spec) || spec.configuredDamage() == null || spec.lifetimeTicks() <= 0
                || spec.volumeVisual() == null) {
            return false;
        }
        float amount = spec.configuredDamage().amount();
        return Float.isFinite(amount) && amount >= 0.0f;
    }

    static boolean finitePositive(double sample) {
        return Double.isFinite(sample) && sample > 0.0d;
    }

    static @Nullable Vec3 overlapPoint(@Nullable ActorVolumeSpec spec, @Nullable AABB targetBounds) {
        if (!intersectsObb(spec, targetBounds)) {
            return null;
        }
        Vec3 eyeOrigin = spec.eyeOrigin();
        if (insideObb(spec, eyeOrigin) && containsInclusive(targetBounds, eyeOrigin)) {
            return eyeOrigin;
        }
        Vec3 point = clampAabb(targetBounds, eyeOrigin);
        for (int step = 0; step < 8; step++) {
            point = clampObb(spec, point);
            point = clampAabb(targetBounds, point);
        }
        Witness chosen = null;
        if (insideObb(spec, point) && containsInclusive(targetBounds, point)) {
            chosen = nearer(null, eyeOrigin, point, spec, targetBounds);
        }
        chosen = nearerCorners(chosen, eyeOrigin, spec, targetBounds, aabbCorners(targetBounds));
        chosen = nearerCorners(chosen, eyeOrigin, spec, targetBounds, corners(spec));
        return chosen == null ? null : chosen.point;
    }

    private static boolean shapeReady(@Nullable ActorVolumeSpec spec) {
        if (spec == null || !finitePositive(spec.depthBlocks()) || !finitePositive(spec.heightBlocks())
                || !finitePositive(spec.widthBlocks()) || !finitePoint(spec.eyeOrigin())
                || !unitAxis(spec.forwardUnit()) || !unitAxis(spec.upUnit()) || !unitAxis(spec.rightUnit())) {
            return false;
        }
        double basisDet = determinant(spec.rightUnit(), spec.upUnit(), spec.forwardUnit());
        return Double.isFinite(basisDet) && Math.abs(basisDet) >= AXIS_EPSILON;
    }

    private static @Nullable Vec3 centerOf(@Nullable ActorVolumeSpec spec) {
        if (!shapeReady(spec)) {
            return null;
        }
        return spec.eyeOrigin().add(spec.forwardUnit().scale(spec.depthBlocks() * 0.5d));
    }

    private static @Nullable Vec3[] corners(@Nullable ActorVolumeSpec spec) {
        Vec3 center = centerOf(spec);
        if (center == null) {
            return null;
        }
        double halfWidth = spec.widthBlocks() * 0.5d;
        double halfHeight = spec.heightBlocks() * 0.5d;
        double halfDepth = spec.depthBlocks() * 0.5d;
        Vec3[] corners = new Vec3[8];
        int cursor = 0;
        for (int corner = 0; corner < 8; corner++) {
            double rightSign = (corner & 1) == 0 ? -halfWidth : halfWidth;
            double upSign = (corner & 2) == 0 ? -halfHeight : halfHeight;
            double forwardSign = (corner & 4) == 0 ? -halfDepth : halfDepth;
            Vec3 point = center
                    .add(spec.rightUnit().scale(rightSign))
                    .add(spec.upUnit().scale(upSign))
                    .add(spec.forwardUnit().scale(forwardSign));
            if (!finitePoint(point)) {
                return null;
            }
            corners[cursor] = point;
            cursor++;
        }
        return corners;
    }

    private static Vec3[] aabbCorners(AABB bounds) {
        Vec3[] corners = new Vec3[8];
        int cursor = 0;
        for (int corner = 0; corner < 8; corner++) {
            double sampleX = (corner & 1) == 0 ? bounds.minX : bounds.maxX;
            double sampleY = (corner & 2) == 0 ? bounds.minY : bounds.maxY;
            double sampleZ = (corner & 4) == 0 ? bounds.minZ : bounds.maxZ;
            corners[cursor] = new Vec3(sampleX, sampleY, sampleZ);
            cursor++;
        }
        return corners;
    }

    private static Witness nearerCorners(@Nullable Witness chosen, Vec3 eyeOrigin, ActorVolumeSpec spec,
                                         AABB targetBounds, @Nullable Vec3[] corners) {
        if (corners == null) {
            return chosen;
        }
        Witness current = chosen;
        for (Vec3 corner : corners) {
            current = nearer(current, eyeOrigin, corner, spec, targetBounds);
        }
        return current;
    }

    private static @Nullable Witness nearer(@Nullable Witness chosen, Vec3 eyeOrigin, @Nullable Vec3 candidate,
                                            ActorVolumeSpec spec, AABB targetBounds) {
        if (candidate == null || !insideObb(spec, candidate) || !containsInclusive(targetBounds, candidate)) {
            return chosen;
        }
        double distanceSquared = eyeOrigin.distanceToSqr(candidate);
        if (!Double.isFinite(distanceSquared)) {
            return chosen;
        }
        if (chosen == null || distanceSquared < chosen.distanceSquared) {
            return new Witness(candidate, distanceSquared);
        }
        return chosen;
    }

    private static boolean insideObb(ActorVolumeSpec spec, Vec3 point) {
        double[] local = localOffset(spec, point);
        if (local == null) {
            return false;
        }
        return Math.abs(local[0]) <= spec.widthBlocks() * 0.5d + SEPARATION_SLOP
                && Math.abs(local[1]) <= spec.heightBlocks() * 0.5d + SEPARATION_SLOP
                && Math.abs(local[2]) <= spec.depthBlocks() * 0.5d + SEPARATION_SLOP;
    }

    private static Vec3 clampObb(ActorVolumeSpec spec, Vec3 point) {
        double[] local = localOffset(spec, point);
        Vec3 center = centerOf(spec);
        if (local == null || center == null) {
            return point;
        }
        double clampedRight = clamp(local[0], -spec.widthBlocks() * 0.5d, spec.widthBlocks() * 0.5d);
        double clampedUp = clamp(local[1], -spec.heightBlocks() * 0.5d, spec.heightBlocks() * 0.5d);
        double clampedForward = clamp(local[2], -spec.depthBlocks() * 0.5d, spec.depthBlocks() * 0.5d);
        return center
                .add(spec.rightUnit().scale(clampedRight))
                .add(spec.upUnit().scale(clampedUp))
                .add(spec.forwardUnit().scale(clampedForward));
    }

    private static @Nullable double[] localOffset(ActorVolumeSpec spec, Vec3 point) {
        Vec3 center = centerOf(spec);
        if (center == null || !finitePoint(point)) {
            return null;
        }
        Vec3 offset = point.subtract(center);
        double basisDet = determinant(spec.rightUnit(), spec.upUnit(), spec.forwardUnit());
        if (!Double.isFinite(basisDet) || Math.abs(basisDet) < AXIS_EPSILON) {
            return null;
        }
        double alongRight = determinant(offset, spec.upUnit(), spec.forwardUnit()) / basisDet;
        double alongUp = determinant(spec.rightUnit(), offset, spec.forwardUnit()) / basisDet;
        double alongForward = determinant(spec.rightUnit(), spec.upUnit(), offset) / basisDet;
        if (!Double.isFinite(alongRight) || !Double.isFinite(alongUp) || !Double.isFinite(alongForward)) {
            return null;
        }
        return new double[] {alongRight, alongUp, alongForward};
    }

    private static double determinant(Vec3 first, Vec3 second, Vec3 third) {
        return first.dot(second.cross(third));
    }

    private static boolean separated(Vec3 delta, Vec3 axis, ActorVolumeSpec spec, AABB targetBounds) {
        if (axis == null) {
            return false;
        }
        double axisLengthSqr = axis.lengthSqr();
        if (!Double.isFinite(axisLengthSqr) || axisLengthSqr < AXIS_EPSILON) {
            return false;
        }
        Vec3 unitAxis = axis.normalize();
        double distance = Math.abs(delta.dot(unitAxis));
        double boxReach = Math.abs(spec.rightUnit().dot(unitAxis)) * spec.widthBlocks() * 0.5d
                + Math.abs(spec.upUnit().dot(unitAxis)) * spec.heightBlocks() * 0.5d
                + Math.abs(spec.forwardUnit().dot(unitAxis)) * spec.depthBlocks() * 0.5d;
        double boundsReach = Math.abs(unitAxis.x) * (targetBounds.maxX - targetBounds.minX) * 0.5d
                + Math.abs(unitAxis.y) * (targetBounds.maxY - targetBounds.minY) * 0.5d
                + Math.abs(unitAxis.z) * (targetBounds.maxZ - targetBounds.minZ) * 0.5d;
        return distance > boxReach + boundsReach + SEPARATION_SLOP;
    }

    private static Vec3 clampAabb(AABB bounds, Vec3 point) {
        return new Vec3(
                clamp(point.x, bounds.minX, bounds.maxX),
                clamp(point.y, bounds.minY, bounds.maxY),
                clamp(point.z, bounds.minZ, bounds.maxZ));
    }

    private static boolean containsInclusive(AABB bounds, Vec3 point) {
        return point.x >= bounds.minX - SEPARATION_SLOP && point.x <= bounds.maxX + SEPARATION_SLOP
                && point.y >= bounds.minY - SEPARATION_SLOP && point.y <= bounds.maxY + SEPARATION_SLOP
                && point.z >= bounds.minZ - SEPARATION_SLOP && point.z <= bounds.maxZ + SEPARATION_SLOP;
    }

    private static boolean finiteBounds(@Nullable AABB bounds) {
        return bounds != null
                && Double.isFinite(bounds.minX) && Double.isFinite(bounds.minY) && Double.isFinite(bounds.minZ)
                && Double.isFinite(bounds.maxX) && Double.isFinite(bounds.maxY) && Double.isFinite(bounds.maxZ)
                && bounds.minX <= bounds.maxX
                && bounds.minY <= bounds.maxY
                && bounds.minZ <= bounds.maxZ;
    }

    private static boolean unitAxis(@Nullable Vec3 axis) {
        if (!finitePoint(axis)) {
            return false;
        }
        double lengthSqr = axis.lengthSqr();
        return lengthSqr > UNIT_LOWER && lengthSqr < UNIT_UPPER;
    }

    private static boolean finitePoint(@Nullable Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static double clamp(double sample, double lower, double upper) {
        if (sample < lower) {
            return lower;
        }
        if (sample > upper) {
            return upper;
        }
        return sample;
    }

    private record Witness(Vec3 point, double distanceSquared) {
    }
}
