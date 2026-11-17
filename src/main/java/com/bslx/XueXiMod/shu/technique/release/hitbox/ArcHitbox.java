package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 施法者脚下的水平扇形。先用锚点，没有锚点才用施法者脚位。
 * 不看命中体自己的飞行位置，也不改无敌帧或账本。
 */
public final class ArcHitbox implements TechniqueHitbox {
    @Override
    /** 搜索半径比扇形半径多 1 格，高度多留 2 格。 */
    public AABB broadPhase(TechniqueHitEntity entity) {
        Vec3 origin = casterFeet(entity);
        double radius = entity.arcRadius() + 1.0;
        return new AABB(origin, origin).inflate(radius, entity.arcHeight() + 2.0, radius);
    }

    @Override
    /**
     * 水平距离不超过半径，竖直范围从脚下到扇形高度，不足 2.5 格时按 2.5。
     * 角度只取扇形张角的一半；目标几乎在原点正上方时不再比角度。
     */
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 origin = casterFeet(entity);
        Vec3 forward = casterLook(entity);
        AABB box = target.getBoundingBox();
        double deltaX = box.getCenter().x - origin.x;
        double deltaZ = box.getCenter().z - origin.z;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontal > entity.arcRadius()) {
            return false;
        }
        double feet = origin.y;
        double top = feet + Math.max(entity.arcHeight(), 2.5);
        if (box.maxY < feet || box.minY > top) {
            return false;
        }
        if (horizontal < 1.0E-4) {
            return true;
        }
        double normalizedX = deltaX / horizontal;
        double normalizedZ = deltaZ / horizontal;
        double dot = Math.max(-1.0, Math.min(1.0, forward.x * normalizedX + forward.z * normalizedZ));
        double angle = Math.toDegrees(Math.acos(dot));
        return angle <= entity.arcDegrees() * 0.5;
    }

    private static Vec3 casterFeet(TechniqueHitEntity entity) {
        if (entity.anchor() != null) {
            return entity.anchor();
        }
        if (entity.getCaster() != null) {
            return entity.getCaster().position();
        }
        return entity.position();
    }

    private static Vec3 casterLook(TechniqueHitEntity entity) {
        Vec3 look = entity.aim() != null
                ? entity.aim()
                : (entity.getCaster() != null ? entity.getCaster().getLookAngle() : entity.forward());
        double lookX = look.x;
        double lookZ = look.z;
        double length = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (length < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(lookX / length, 0.0, lookZ / length);
    }
}
