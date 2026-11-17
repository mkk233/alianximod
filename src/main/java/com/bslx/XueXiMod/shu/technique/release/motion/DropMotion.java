package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.DropTilt;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 垂落采样。锁定这一发时，采样点改成标记目标中心。
 * 有地面锚点才把竖直结果铺成斜线，没有锚点时保持竖直。不改无敌帧或账本。
 */
public final class DropMotion implements TechniqueMotion {
    @Override
    /** 采样后同时改位置和朝向。目标只改落点，不改成追踪运动。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        Vec3 targetPoint = null;
        if (entity.lockThisCast()) {
            LivingEntity target = LockTargetService.getMarkedTarget(level, entity.casterUuid());
            if (target != null) {
                targetPoint = target.getBoundingBox().getCenter();
            }
        }
        TrajectorySampler.Sample sample = TrajectorySampler.sample(
                Trajectory.DROP, entity.origin(), entity.forward(), entity.speed(),
                entity.lifetime(), entity.age(), targetPoint, false, entity.forward(), null);
        entity.placeAt(slantedPosition(entity, sample.position()), slantedHeading(entity, sample.direction()));
    }

    /** 公共采样只提供竖直位移。没有落点锚点时位置和朝向都保持原样。 */
    static Vec3 slantedPosition(TechniqueHitEntity entity, Vec3 verticalPosition) {
        Vec3 landing = entity.dropLanding();
        Vec3 origin = entity.origin();
        if (landing == null || origin == null || verticalPosition == null) {
            return verticalPosition;
        }
        double verticalFall = origin.y - verticalPosition.y;
        return DropTilt.pointAtFall(origin, landing, verticalFall);
    }

    static Vec3 slantedHeading(TechniqueHitEntity entity, Vec3 fallbackHeading) {
        Vec3 landing = entity.dropLanding();
        Vec3 origin = entity.origin();
        if (landing == null || origin == null) {
            return fallbackHeading;
        }
        return DropTilt.unitToward(origin, landing);
    }
}
