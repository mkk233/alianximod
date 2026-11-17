package com.bslx.XueXiMod.shu.technique.release.prison;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** 创建时把目标真实包围盒外扩 0.20 格作为壳。接触只算这层壳，不算盒子内部。 */
public final class PrisonShell {
    public static final double PADDING = 0.20D;

    private PrisonShell() {
    }

    public static AABB expand(AABB body) {
        if (body == null) {
            return null;
        }
        return body.inflate(PADDING);
    }

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
