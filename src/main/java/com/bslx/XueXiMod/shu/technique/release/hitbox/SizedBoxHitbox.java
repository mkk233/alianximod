package com.bslx.XueXiMod.shu.technique.release.hitbox;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SizedBoxHitbox implements TechniqueHitbox {
    @Override
    public AABB broadPhase(TechniqueHitEntity entity) {
        double reach = Math.max(entity.length(), entity.width()) + 1.0;
        return new AABB(entity.previousPosition(), entity.position()).inflate(reach, Math.max(entity.hitHeight(), 1.0), reach);
    }

    @Override
    public boolean contains(TechniqueHitEntity entity, LivingEntity target) {
        if (entity.beam()) {
            return intersectsBeam(entity, target);
        }
        if (entity.pose3d()) {
            return intersectsTiltedPlane(entity, target);
        }
        Vec3 forward = flat(entity.forward());
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x);
        Vec3 rel = target.getBoundingBox().getCenter().subtract(entity.position());
        double localForward = rel.x * forward.x + rel.z * forward.z;
        double localRight = rel.x * right.x + rel.z * right.z;
        boolean inside = Math.abs(localForward) <= entity.length() * 0.5
                && Math.abs(localRight) <= entity.width() * 0.5
                && Math.abs(rel.y) <= entity.hitHeight() * 0.5;
        if (inside) {
            return true;
        }
        AABB box = target.getBoundingBox().inflate(0.2);
        Vec3 from = entity.previousPosition();
        Vec3 to = entity.position();
        return box.contains(to) || box.contains(from) || box.clip(from, to).isPresent();
    }

    private static boolean intersectsBeam(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 forward = entity.forward();
        if (forward.lengthSqr() < 1.0E-8) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }
        Vec3 start = entity.position();
        Vec3 end = start.add(forward.scale(Math.max(entity.length(), 0.0)));
        AABB box = target.getBoundingBox().inflate(entity.width() * 0.5, entity.height() * 0.5, entity.width() * 0.5);
        return box.contains(start) || box.contains(end) || box.clip(start, end).isPresent();
    }

    private static boolean intersectsTiltedPlane(TechniqueHitEntity entity, LivingEntity target) {
        Vec3 normal = entity.forward();
        if (normal.lengthSqr() < 1.0E-8) {
            normal = new Vec3(0.0, 0.0, 1.0);
        } else {
            normal = normal.normalize();
        }
        Vec3 helper = Math.abs(normal.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = helper.cross(normal);
        if (right.lengthSqr() < 1.0E-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        right = right.normalize();
        Vec3 up = normal.cross(right).normalize();
        double radians = Math.toRadians(entity.roll());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vec3 axisRight = right.scale(cos).add(up.scale(sin));
        Vec3 axisUp = up.scale(cos).subtract(right.scale(sin));
        Vec3 rel = target.getBoundingBox().getCenter().subtract(entity.position());
        double reach = target.getBbWidth() * 0.5;
        return Math.abs(rel.dot(normal)) <= target.getBbHeight() * 0.5 + 0.35
                && Math.abs(rel.dot(axisRight)) <= entity.width() * 0.5 + reach
                && Math.abs(rel.dot(axisUp)) <= entity.length() * 0.5 + reach;
    }

    private static Vec3 flat(Vec3 direction) {
        double length = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (length < 1.0E-4) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(direction.x / length, 0.0, direction.z / length);
    }
}
