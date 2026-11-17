package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public final class CloneDecoyPolicy {
    private CloneDecoyPolicy() {
    }

    public static boolean pass(ServerLevel level, CloneEntity actor, CloneRuntimeState state) {
        double radius = state.spec.decoyRadiusBlocks();
        if (!Double.isFinite(radius) || radius <= 0.0D || state.life != CloneLifeState.ACTIVE) {
            return false;
        }
        prune(level, actor, state);
        AABB box = new AABB(actor.position(), actor.position()).inflate(radius);
        List<LivingEntity> found = LivingCandidateQueries.entityQuery(level, box, CloneDecoyPolicy::coarse);
        PerformanceRecorder.add(PerfCounter.CLONE_DECOY_PASSES, 1L);
        double radiusSquared = radius * radius;
        int inspected = 0;
        int attracted = 0;
        for (LivingEntity living : found) {
            if (inspected >= CloneRuntime.DECOY_CANDIDATE_CAP || attracted >= CloneRuntime.DECOY_ATTRACT_CAP) {
                break;
            }
            inspected++;
            if (!(living instanceof Mob mob) || mob == actor || !mob.isAlive() || mob.isRemoved() || mob.level() != level) {
                continue;
            }
            if (mob.distanceToSqr(actor) > radiusSquared) {
                continue;
            }
            if (!mob.canAttack(actor) || !mob.hasLineOfSight(actor)) {
                continue;
            }
            LivingEntity currentTarget = mob.getTarget();
            UUID ownerId = state.ownerId;
            if (currentTarget == actor) {
                continue;
            }
            boolean chasingOwner = currentTarget != null && currentTarget.getUUID().equals(ownerId);
            if (currentTarget != null && !chasingOwner) {
                continue;
            }
            if (state.leases.contains(mob.getUUID())) {
                continue;
            }
            if (state.leases.size() >= CloneRuntime.DECOY_LEASE_CAP) {
                break;
            }
            mob.setTarget(actor);
            state.leases.add(mob.getUUID());
            attracted++;
        }
        return true;
    }

    public static void clear(ServerLevel level, CloneEntity actor, List<UUID> leases) {
        for (UUID lease : List.copyOf(leases)) {
            Entity found = level.getEntity(lease);
            if (found instanceof Mob mob && mob.getTarget() == actor) {
                mob.setTarget(null);
            }
        }
        leases.clear();
    }

    private static void prune(ServerLevel level, CloneEntity actor, CloneRuntimeState state) {
        state.leases.removeIf(lease -> stale(level, actor, lease));
    }

    private static boolean stale(ServerLevel level, CloneEntity actor, UUID lease) {
        Entity found = level.getEntity(lease);
        if (!(found instanceof Mob mob) || mob.isRemoved()) {
            return true;
        }
        return mob.getTarget() != actor;
    }

    private static boolean coarse(LivingEntity living) {
        return living instanceof Enemy && (living instanceof Zombie || living instanceof AbstractSkeleton);
    }
}
