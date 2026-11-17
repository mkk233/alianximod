package com.bslx.XueXiMod.shu.technique.effects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LockTargetService {
    private static final double RANGE = 20.0;
    private static final double SIDE_DISTANCE_UPPER_BOUND = 8.0;

    private static final Map<UUID, Integer> MARKS = new ConcurrentHashMap<>();

    private LockTargetService() {
    }

    public static LivingEntity resolve(ServerPlayer player) {
        LivingEntity target = findTarget(player);
        if (target != null) {
            mark(player.getUUID(), target.getId());
        } else {
            clear(player.getUUID());
        }
        return target;
    }

    public static void mark(UUID playerId, int entityId) {
        MARKS.put(playerId, entityId);
    }

    public static void clear(UUID playerId) {
        MARKS.remove(playerId);
    }

    public static LivingEntity getMarkedTarget(ServerLevel level, UUID playerId) {
        Integer entityId = MARKS.get(playerId);
        if (entityId == null) {
            return null;
        }
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            MARKS.remove(playerId);
            return null;
        }
        return living;
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(RANGE));

        AABB box = new AABB(eye, end).inflate(SIDE_DISTANCE_UPPER_BOUND);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive());

        LivingEntity best = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 center = candidate.getBoundingBox().getCenter();
            double distSqr = distanceSqrToSegment(center, eye, end);
            if (distSqr > SIDE_DISTANCE_UPPER_BOUND * SIDE_DISTANCE_UPPER_BOUND) {
                continue;
            }
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                best = candidate;
            }
        }
        return best;
    }

    private static double distanceSqrToSegment(Vec3 point, Vec3 start, Vec3 end) {
        double abx = end.x - start.x;
        double aby = end.y - start.y;
        double abz = end.z - start.z;
        double apx = point.x - start.x;
        double apy = point.y - start.y;
        double apz = point.z - start.z;
        double abSqr = abx * abx + aby * aby + abz * abz;
        double projection = (apx * abx + apy * aby + apz * abz) / abSqr;
        projection = Math.max(0.0, Math.min(1.0, projection));
        double deltaX = point.x - (start.x + projection * abx);
        double deltaY = point.y - (start.y + projection * aby);
        double deltaZ = point.z - (start.z + projection * abz);
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }
}
