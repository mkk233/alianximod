package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BoxHitbox implements TechniqueHitbox {
    @Override
    public AABB broadPhase(TechniqueHitEntity entity) {
        double half = entity.boxSize().length() * 0.5 + 1.0;
        return new AABB(entity.position(), entity.position()).inflate(half);
    }

    @Override
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 half = entity.boxSize().scale(0.5);
        Vec3 c = entity.position();
        AABB box = new AABB(c.x - half.x, c.y - half.y, c.z - half.z,
                c.x + half.x, c.y + half.y, c.z + half.z);
        return box.intersects(target.getBoundingBox());
    }
}
