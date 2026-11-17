package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ClonePositionPolicy {
    private static final double UNIT_SLACK = 1.0E-3D;
    private static final double MINIMUM_SEPARATION = 1.0E-8D;

    private ClonePositionPolicy() {
    }

    @Nullable
    public static CloneActorFrame frame(CloneEntity actor, CloneTriggerInput input) {
        if (actor == null || input == null || !(actor.level() instanceof ServerLevel level)) {
            return null;
        }
        Vec3 eyeOrigin = actor.getEyePosition();
        Vec3 actorPosition = actor.position();
        if (!finite(eyeOrigin) || !finite(actorPosition)) {
            return null;
        }
        if (input.source() == CloneTriggerInput.Source.MANUAL) {
            Vec3 lookUnit = input.manualLook();
            if (!unit(lookUnit)) {
                return null;
            }
            return build(actor, level, actorPosition, eyeOrigin, lookUnit, null,
                    CloneActorFrame.AimSource.MANUAL_DIRECTION);
        }
        UUID attackerId = input.attackerId();
        if (attackerId == null) {
            return null;
        }
        Entity found = level.getEntity(attackerId);
        if (!(found instanceof LivingEntity living) || !living.isAlive() || living.isRemoved() || living.level() != level) {
            return null;
        }
        Vec3 center = living.getBoundingBox().getCenter();
        Vec3 delta = center.subtract(eyeOrigin);
        if (!finite(delta) || delta.lengthSqr() < MINIMUM_SEPARATION) {
            return null;
        }
        return build(actor, level, actorPosition, eyeOrigin, delta.normalize(), attackerId,
                CloneActorFrame.AimSource.HURT_TARGET);
    }

    public static boolean unit(@Nullable Vec3 look) {
        if (!finite(look)) {
            return false;
        }
        double lengthSquared = look.lengthSqr();
        return Math.abs(lengthSquared - 1.0D) <= UNIT_SLACK;
    }

    private static CloneActorFrame build(CloneEntity actor, ServerLevel level, Vec3 actorPosition, Vec3 eyeOrigin,
                                        Vec3 lookUnit, @Nullable UUID targetId, CloneActorFrame.AimSource aimSource) {
        return new CloneActorFrame(level, actor.getUUID(), actor.generation(), actor.ownerId(), actorPosition,
                eyeOrigin, lookUnit, targetId, actor.actionSequence(), aimSource);
    }

    private static boolean finite(@Nullable Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
