package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class StillAimMotion implements TechniqueMotion {
    @Override
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        if (entity.pose3d()) {
            Vec3 normal = entity.aim() != null ? entity.aim() : entity.forward();
            if (normal.lengthSqr() < 1.0E-8) {
                normal = new Vec3(0.0, 0.0, 1.0);
            }
            normal = normal.normalize();
            Vec3 at = entity.anchor() != null ? entity.anchor() : entity.position();
            entity.setForward(normal);
            entity.placeAt(at, normal);
            return;
        }
        ServerPlayer caster = entity.getCaster();
        Vec3 aim = entity.aim() != null ? entity.aim() : (caster != null ? caster.getLookAngle() : entity.forward());
        Vec3 anchor = entity.anchor() != null ? entity.anchor() : (caster != null ? caster.getEyePosition() : entity.position());
        double x = aim.x;
        double z = aim.z;
        double horizontal = Math.sqrt(x * x + z * z);
        Vec3 forward = horizontal < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(x / horizontal, 0.0, z / horizontal);
        Vec3 pos = entity.centered()
                ? anchor
                : anchor.add(forward.scale(Math.max(entity.length(), 0.0) * 0.5));
        entity.setForward(forward);
        entity.placeAt(pos, forward);
    }
}
