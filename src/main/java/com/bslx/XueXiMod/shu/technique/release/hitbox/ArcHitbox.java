package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ArcHitbox implements TechniqueHitbox {
    @Override
    public AABB broadPhase(TechniqueHitEntity entity) {
        Vec3 origin = casterFeet(entity);
        double radius = entity.arcRadius() + 1.0;
        return new AABB(origin, origin).inflate(radius, entity.arcHeight() + 2.0, radius);
    }

    @Override
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 origin = casterFeet(entity);
        Vec3 forward = casterLook(entity);
        AABB box = target.getBoundingBox();
        double dx = box.getCenter().x - origin.x;
        double dz = box.getCenter().z - origin.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
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
        double nx = dx / horizontal;
        double nz = dz / horizontal;
        double dot = Math.max(-1.0, Math.min(1.0, forward.x * nx + forward.z * nz));
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
        double x = look.x;
        double z = look.z;
        double length = Math.sqrt(x * x + z * z);
        if (length < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(x / length, 0.0, z / length);
    }
}
