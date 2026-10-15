package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DropMotion implements TechniqueMotion {
    @Override
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
        entity.placeAt(sample.position(), sample.direction());
    }
}
