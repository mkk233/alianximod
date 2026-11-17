package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 跟随施法者。普通路径每拍改用眼睛和当前视线；自动持续则钉住发射源。
 * 钉住时保持原点。垂落转给 {@link DropMotion}。不改无敌帧或账本。
 */
public final class FollowCasterMotion implements TechniqueMotion {
    private final DropMotion drop = new DropMotion();

    @Override
    /** 施法者已经不在时保持不动。无锁定且有锚点时朝锚点，锁定时朝标记目标。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        LaunchAim aim = entity.launchAim();
        if (aim != null && aim.automaticLifetime()) {
            this.followLaunch(entity, level, aim);
            return;
        }
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
        Vec3 direction = caster.getLookAngle();
        if (entity.anchor() != null && !entity.lockThisCast()) {
            Vec3 toward = entity.anchor().subtract(origin);
            if (toward.lengthSqr() > 1.0E-8) {
                direction = toward.normalize();
            }
        } else if (entity.lockThisCast()) {
            LivingEntity target = LockTargetService.getMarkedTarget(level, entity.casterUuid());
            if (target != null) {
                Vec3 toward = target.getBoundingBox().getCenter().subtract(origin);
                if (toward.lengthSqr() > 1.0E-8) {
                    direction = toward.normalize();
                }
            }
        }
        entity.setForward(direction);
        entity.placeAt(origin, direction);
    }

    /** 钉在这一次的发射源上。目标仍有效就朝它，失效后留最后合法方向。垂落仍向下。 */
    private void followLaunch(TechniqueHitEntity entity, ServerLevel level, LaunchAim aim) {
        if (entity.pinned()) {
            entity.placeAt(entity.origin(), entity.forward());
            return;
        }
        Vec3 origin = aim.origin();
        entity.setOrigin(origin);
        if (entity.dropThisCast()) {
            entity.setForward(new Vec3(0.0, -1.0, 0.0));
            this.drop.tick(entity, level);
            return;
        }
        Vec3 direction = aim.direction();
        LivingEntity target = aim.findTarget(level);
        if (target != null) {
            Vec3 towardTarget = target.getBoundingBox().getCenter().subtract(origin);
            if (towardTarget.lengthSqr() > 1.0E-8) {
                direction = towardTarget.normalize();
                aim.rememberDirection(direction);
            }
        }
        entity.setForward(direction);
        entity.placeAt(origin, direction);
    }
}
