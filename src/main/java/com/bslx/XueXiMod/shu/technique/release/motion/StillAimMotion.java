package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 静止瞄准。三维姿态钉在锚点和法线上。
 * 普通姿态只留水平朝向，未居中时再沿该方向前移半个长度。不改无敌帧或账本。
 */
public final class StillAimMotion implements TechniqueMotion {
    @Override
    /** 缺锚点、瞄准或施法者时用当前位置和安全前向，不移动到别处。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        if (entity.pose3d()) {
            Vec3 normal = entity.aim() != null ? entity.aim() : entity.forward();
            if (normal.lengthSqr() < 1.0E-8) {
                normal = new Vec3(0.0, 0.0, 1.0);
            }
            normal = normal.normalize();
            Vec3 anchorPoint = entity.anchor() != null ? entity.anchor() : entity.position();
            entity.setForward(normal);
            entity.placeAt(anchorPoint, normal);
            return;
        }
        ServerPlayer caster = entity.getCaster();
        Vec3 aim = entity.aim() != null ? entity.aim() : (caster != null ? caster.getLookAngle() : entity.forward());
        Vec3 anchor = entity.anchor() != null ? entity.anchor() : (caster != null ? caster.getEyePosition() : entity.position());
        double aimX = aim.x;
        double aimZ = aim.z;
        double horizontal = Math.sqrt(aimX * aimX + aimZ * aimZ);
        Vec3 forward = horizontal < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(aimX / horizontal, 0.0, aimZ / horizontal);
        Vec3 placementPoint = entity.centered()
                ? anchor
                : anchor.add(forward.scale(Math.max(entity.length(), 0.0) * 0.5));
        entity.setForward(forward);
        entity.placeAt(placementPoint, forward);
    }
}
