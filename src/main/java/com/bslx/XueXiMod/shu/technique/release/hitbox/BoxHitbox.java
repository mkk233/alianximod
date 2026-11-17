package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 轴对齐盒。只看当前位置和 {@code boxSize}，不随朝向旋转，也不扫上一拍。
 * 不改无敌帧或账本。
 */
public final class BoxHitbox implements TechniqueHitbox {
    @Override
    /** 以对角线一半再加 1 格作搜索范围。 */
    public AABB broadPhase(TechniqueHitEntity entity) {
        double half = entity.boxSize().length() * 0.5 + 1.0;
        return new AABB(entity.position(), entity.position()).inflate(half);
    }

    @Override
    /** 目标原版包围盒与这个轴对齐盒相交才算命中。 */
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 half = entity.boxSize().scale(0.5);
        Vec3 center = entity.position();
        AABB box = new AABB(center.x - half.x, center.y - half.y, center.z - half.z,
                center.x + half.x, center.y + half.y, center.z + half.z);
        return box.intersects(target.getBoundingBox());
    }
}
