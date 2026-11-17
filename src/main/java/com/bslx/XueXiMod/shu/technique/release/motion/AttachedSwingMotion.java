package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 贴着施法者横扫。年龄从寿命起点的左侧 70 度转到右侧 70 度。
 * 施法者不在时停在原地。不改无敌帧或账本。
 */
public final class AttachedSwingMotion implements TechniqueMotion {
    @Override
    /** 位置固定在施法者前方 2 格、眼下约四成身高处，偏航跟着横扫角。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        ServerPlayer caster = entity.getCaster();
        if (caster == null) {
            return;
        }
        double progress = entity.age() / (double) Math.max(1, entity.lifetime());
        double angleDeg = -70.0 + 140.0 * progress;
        double yawRad = Math.toRadians(caster.getYRot());
        double sweepRad = Math.toRadians(angleDeg);
        double cos = Math.cos(sweepRad);
        double sin = Math.sin(sweepRad);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double deltaX = forwardX * cos - forwardZ * sin;
        double deltaZ = forwardX * sin + forwardZ * cos;
        double height = caster.getY() + caster.getBbHeight() * 0.6;
        Vec3 position = new Vec3(caster.getX() + deltaX * 2.0, height, caster.getZ() + deltaZ * 2.0);
        entity.setPos(position.x, position.y, position.z);
        entity.setYRot(caster.getYRot() + (float) angleDeg);
        entity.setXRot(0.0F);
    }
}
