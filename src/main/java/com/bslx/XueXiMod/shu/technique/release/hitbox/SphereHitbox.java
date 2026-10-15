package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SphereHitbox implements TechniqueHitbox {
    @Override
    public AABB broadPhase(TechniqueHitEntity entity) {
        double radius = Math.max(entity.boxSize().x, 0.3) + 2.0;
        return new AABB(entity.previousPosition(), entity.position()).inflate(radius);
    }

    @Override
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        double radius = Math.max(entity.boxSize().x, 0.3);
        AABB box = target.getBoundingBox().inflate(radius);
        Vec3 from = entity.previousPosition();
        Vec3 to = entity.position();
        if (box.contains(to) || box.contains(from)) {
            return true;
        }
        return box.clip(from, to).isPresent();
    }
}
