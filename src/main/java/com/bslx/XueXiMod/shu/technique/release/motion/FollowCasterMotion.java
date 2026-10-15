package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FollowCasterMotion implements TechniqueMotion {
    private final DropMotion drop = new DropMotion();

    @Override
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        if (entity.pinned()) {
            entity.placeAt(entity.origin(), entity.forward());
            return;
        }
        ServerPlayer caster = entity.getCaster();
        if (caster == null) {
            return;
        }
        Vec3 origin = caster.getEyePosition();
        entity.setOrigin(origin);
        entity.setForward(caster.getLookAngle());
        if (entity.dropThisCast()) {
            this.drop.tick(entity, level);
            return;
        }
        Vec3 dir = caster.getLookAngle();
        if (entity.anchor() != null && !entity.lockThisCast()) {
            Vec3 toward = entity.anchor().subtract(origin);
            if (toward.lengthSqr() > 1.0E-8) {
                dir = toward.normalize();
            }
        } else if (entity.lockThisCast()) {
            LivingEntity target = LockTargetService.getMarkedTarget(level, entity.casterUuid());
            if (target != null) {
                Vec3 toward = target.getBoundingBox().getCenter().subtract(origin);
                if (toward.lengthSqr() > 1.0E-8) {
                    dir = toward.normalize();
                }
            }
        }
        entity.setForward(dir);
        entity.placeAt(origin, dir);
    }
}
