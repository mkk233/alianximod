package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class AttachedSwingMotion implements TechniqueMotion {
    @Override
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
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        double dx = fx * cos - fz * sin;
        double dz = fx * sin + fz * cos;
        double height = caster.getY() + caster.getBbHeight() * 0.6;
        Vec3 pos = new Vec3(caster.getX() + dx * 2.0, height, caster.getZ() + dz * 2.0);
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setYRot(caster.getYRot() + (float) angleDeg);
        entity.setXRot(0.0F);
    }
}
