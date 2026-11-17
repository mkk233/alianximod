package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 直线弹道。没有锁定时沿原方向匀速走。
 * 锁定时自动持续找自己的目标，其余用已标记目标。只改方向，不改无敌帧或账本。
 */
public final class ProjectileMotion implements TechniqueMotion {
    @Override
    /** 找不到目标时采样点为空，公共直线保持原来的前向。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        Vec3 targetPoint = null;
        if (entity.lockThisCast()) {
            LaunchAim aim = entity.launchAim();
            LivingEntity target;
            if (aim != null && aim.automaticLifetime()) {
                target = aim.findTarget(level);
            } else {
                target = LockTargetService.getMarkedTarget(level, entity.casterUuid());
            }
            if (target != null) {
                targetPoint = target.getBoundingBox().getCenter();
            }
        }
        TrajectorySampler.Sample sample = TrajectorySampler.sample(
                Trajectory.STRAIGHT, entity.origin(), entity.forward(), entity.speed(),
                entity.lifetime(), entity.age(), targetPoint, false, entity.forward(), null);
        entity.placeAt(sample.position(), sample.direction());
    }
}
