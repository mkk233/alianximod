package com.bslx.XueXiMod.shu.technique.release.prison;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** 创建时把目标真实包围盒外扩 0.20 格作为壳。接触只算这层壳，不算盒子内部。 */
public final class PrisonShell {
    public static final double PADDING = 0.20D;

    private PrisonShell() {
    }

    /** 按固定填充把身体盒扩成外壳。 */
    public static AABB expand(AABB body) {
        return expand(body, 1.0D);
    }

    /** 以目标包围盒中心放大原外壳。倍率不是有限正数时保持原外壳。 */
    public static AABB expand(AABB body, double sizeMultiplier) {
        if (body == null) {
            return null;
        }
        AABB outer = body.inflate(PADDING);
        return scaleAbout(body, outer, sizeMultiplier);
    }

    /** 从外壳反推固定填充后的内盒。太薄时为空。 */
    public static AABB innerOf(AABB outer) {
        if (outer == null) {
            return null;
        }
        AABB inner = outer.inflate(-PADDING);
        if (inner.maxX <= inner.minX || inner.maxY <= inner.minY || inner.maxZ <= inner.minZ) {
            return null;
        }
        return inner;
    }

    /** 原内盒按同一倍率从目标中心放大后冻结。不用固定填充从放大后的外壳反推。 */
    public static AABB innerFor(AABB body, double sizeMultiplier) {
        AABB inner = innerOf(expand(body, 1.0D));
        if (body == null || inner == null) {
            return null;
        }
        return scaleAbout(body, inner, sizeMultiplier);
    }

    private static AABB scaleAbout(AABB centerBox, AABB targetBox, double sizeMultiplier) {
        double factor = sizeMultiplier;
        if (!Double.isFinite(factor) || factor <= 0.0D) {
            factor = 1.0D;
        }
        double centerX = (centerBox.minX + centerBox.maxX) * 0.5D;
        double centerY = (centerBox.minY + centerBox.maxY) * 0.5D;
        double centerZ = (centerBox.minZ + centerBox.maxZ) * 0.5D;
        return new AABB(
                centerX + (targetBox.minX - centerX) * factor,
                centerY + (targetBox.minY - centerY) * factor,
                centerZ + (targetBox.minZ - centerZ) * factor,
                centerX + (targetBox.maxX - centerX) * factor,
                centerY + (targetBox.maxY - centerY) * factor,
                centerZ + (targetBox.maxZ - centerZ) * factor);
    }

    /** 候选身体是否碰到外壳。被困者自己和完全留在内部的不算。 */
    public static boolean touches(AABB outer, AABB inner, LivingEntity candidate, UUID prisonerId) {
        if (outer == null || candidate == null || prisonerId == null || prisonerId.equals(candidate.getUUID())) {
            return false;
        }
        AABB body = candidate.getBoundingBox();
        if (body == null || !body.intersects(outer)) {
            return false;
        }
        if (inner == null) {
            return true;
        }
        return body.minX < inner.minX || body.maxX > inner.maxX
                || body.minY < inner.minY || body.maxY > inner.maxY
                || body.minZ < inner.minZ || body.maxZ > inner.maxZ;
    }
}
