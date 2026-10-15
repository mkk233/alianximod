package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ObbHitbox implements TechniqueHitbox {
    @Override
    public AABB broadPhase(TechniqueHitEntity entity) {
        double half = entity.boxSize().length() * 0.5 + 1.0;
        return new AABB(entity.position(), entity.position()).inflate(half);
    }

    @Override
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 rel = target.getBoundingBox().getCenter().subtract(entity.position());
        double yawRad = Math.toRadians(entity.getYRot());
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double localX = rel.x * cos + rel.z * sin;
        double localY = rel.y;
        double localZ = -rel.x * sin + rel.z * cos;
        return Math.abs(localX) <= entity.boxSize().x * 0.5
                && Math.abs(localY) <= entity.boxSize().y * 0.5
                && Math.abs(localZ) <= entity.boxSize().z * 0.5;
    }
}
