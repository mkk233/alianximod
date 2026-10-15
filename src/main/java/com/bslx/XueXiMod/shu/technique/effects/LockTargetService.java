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
    private static final double MAX_SIDE_DIST = 8.0;

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

        AABB box = new AABB(eye, end).inflate(MAX_SIDE_DIST);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        LivingEntity best = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 center = candidate.getBoundingBox().getCenter();
            double distSqr = distanceSqrToSegment(center, eye, end);
            if (distSqr > MAX_SIDE_DIST * MAX_SIDE_DIST) {
                continue;
            }
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                best = candidate;
            }
        }
        return best;
    }

    private static double distanceSqrToSegment(Vec3 p, Vec3 a, Vec3 b) {
        double abx = b.x - a.x;
        double aby = b.y - a.y;
        double abz = b.z - a.z;
        double apx = p.x - a.x;
        double apy = p.y - a.y;
        double apz = p.z - a.z;
        double abSqr = abx * abx + aby * aby + abz * abz;
        double t = (apx * abx + apy * aby + apz * abz) / abSqr;
        t = Math.max(0.0, Math.min(1.0, t));
        double dx = p.x - (a.x + t * abx);
        double dy = p.y - (a.y + t * aby);
        double dz = p.z - (a.z + t * abz);
        return dx * dx + dy * dy + dz * dz;
    }
}
