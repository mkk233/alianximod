package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 距离用脚底到碰撞箱最近点。遮挡从眼睛打到同一点，只挡方块，不挡实体和流体。 */
public final class BlockOcclusion {
    private BlockOcclusion() {
    }

    public static Vec3 closestPoint(Vec3 origin, AABB bounds) {
        if (origin == null || bounds == null) {
            throw new IllegalArgumentException("bounds");
        }
        double clampedX = clamp(origin.x, bounds.minX, bounds.maxX);
        double clampedY = clamp(origin.y, bounds.minY, bounds.maxY);
        double clampedZ = clamp(origin.z, bounds.minZ, bounds.maxZ);
        return new Vec3(clampedX, clampedY, clampedZ);
    }

    public static boolean withinRadius(double distanceSquared, double radius) {
        if (Double.isNaN(distanceSquared) || distanceSquared < 0.0D || Double.isInfinite(distanceSquared)) {
            return false;
        }
        if (!(radius > 0.0D) || Double.isNaN(radius) || Double.isInfinite(radius)) {
            return false;
        }
        return distanceSquared <= radius * radius;
    }

    /** 偏移取眼睛到最近点，与视线点积必须为正。零向量不算在前方。 */
    public static boolean inFront(Vec3 eye, Vec3 closest, Vec3 look) {
        if (eye == null || closest == null || look == null) {
            return false;
        }
        double offsetX = closest.x - eye.x;
        double offsetY = closest.y - eye.y;
        double offsetZ = closest.z - eye.z;
        double dot = offsetX * look.x + offsetY * look.y + offsetZ * look.z;
        return dot > 0.0D;
    }

    public static boolean clearOfBlocks(ServerLevel level, ServerPlayer caster, Vec3 closest) {
        if (level == null || caster == null || closest == null) {
            return false;
        }
        if (!Double.isFinite(closest.x) || !Double.isFinite(closest.y) || !Double.isFinite(closest.z)) {
            return false;
        }
        Vec3 eye = caster.getEyePosition();
        if (eye.distanceToSqr(closest) <= 1.0E-8D) {
            return true;
        }
        BlockHitResult hit = level.clip(new ClipContext(
                eye, closest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return hit.getType() == HitResult.Type.MISS;
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
}
