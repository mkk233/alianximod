package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Predicate;

/** Shared per-projectile acquisition and bounded steering; no global target state. */
public final class TrackingSupport {
    private static final double TURN_LIMIT = Math.toRadians(12.0);

    /** 现有每拍转向上限。转向算法本身不变。 */
    public static double turnLimitRadians() {
        return TURN_LIMIT;
    }

    private TrackingSupport() {}

    @Nullable
    public static LivingEntity resolveTarget(ServerLevel level, Vec3 position,
                                             @Nullable UUID lockedTarget, Predicate<LivingEntity> eligible) {
        Predicate<LivingEntity> valid = target -> !target.isRemoved() && target.isAlive()
                && !target.isSpectator() && target.level() == level && eligible.test(target);
        if (lockedTarget != null) {
            Entity current = level.getEntity(lockedTarget);
            if (current instanceof LivingEntity living && valid.test(living)) {
                return living;
            }
        }
        AABB area = new AABB(position, position).inflate(5.0);
        return LivingCandidateQueries.entityQuery(level, area, valid).stream()
                .filter(target -> target.getBoundingBox().getCenter().distanceToSqr(position) <= 25.0)
                .min(Comparator.comparingDouble((LivingEntity target) ->
                        target.getBoundingBox().getCenter().distanceToSqr(position))
                        .thenComparingInt(Entity::getId))
                .orElse(null);
    }

    public static Vec3 turnToward(Vec3 direction, Vec3 desiredDirection) {
        Vec3 current = unit(direction);
        Vec3 desired = unit(desiredDirection);
        if (current == null) return desired != null ? desired : new Vec3(0.0, 0.0, 1.0);
        if (desired == null) return current;
        double dot = Math.max(-1.0, Math.min(1.0, current.dot(desired)));
        double angle = Math.acos(dot);
        if (angle <= TURN_LIMIT) return desired;
        Vec3 tangent = unit(desired.subtract(current.scale(dot)));
        if (tangent == null) {
            Vec3 helper = Math.abs(current.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            tangent = current.cross(helper).normalize();
        }
        return current.scale(Math.cos(TURN_LIMIT)).add(tangent.scale(Math.sin(TURN_LIMIT))).normalize();
    }

    @Nullable
    private static Vec3 unit(@Nullable Vec3 vector) {
        if (vector == null || !Double.isFinite(vector.x) || !Double.isFinite(vector.y)
                || !Double.isFinite(vector.z)) return null;
        double length = vector.length();
        return !Double.isFinite(length) || length < 1.0E-8 ? null : vector.scale(1.0 / length);
    }
}
