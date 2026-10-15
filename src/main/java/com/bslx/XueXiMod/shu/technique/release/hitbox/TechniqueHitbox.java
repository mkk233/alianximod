package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public interface TechniqueHitbox {
    AABB broadPhase(TechniqueHitEntity entity);

    boolean contains(TechniqueHitEntity entity, LivingEntity target);
}
