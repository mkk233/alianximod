package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 球形扫探。半径取 {@code boxSize.x}，并不小于 0.3。
 * 检查上一位置到当前位置的线段，不改无敌帧或账本。
 */
public final class SphereHitbox implements TechniqueHitbox {
    @Override
    /** 搜索盒覆盖两个端点，再外扩半径加 2 格。 */
    public AABB broadPhase(TechniqueHitEntity entity) {
        double radius = Math.max(entity.boxSize().x, 0.3) + 2.0;
        return new AABB(entity.previousPosition(), entity.position()).inflate(radius);
    }

    @Override
    /** 目标盒先按半径膨胀。端点在盒内，或线段碰到盒，都算命中。 */
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        double radius = Math.max(entity.boxSize().x, 0.3);
        AABB box = target.getBoundingBox().inflate(radius);
        Vec3 from = entity.previousPosition();
        Vec3 destination = entity.position();
        if (box.contains(destination) || box.contains(from)) {
            return true;
        }
        return box.clip(from, destination).isPresent();
    }
}
