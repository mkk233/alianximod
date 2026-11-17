package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class CloneContacts {
    static final int CONTACT_CANDIDATE_CAP = 32;

    private CloneContacts() {
    }

    static void sweep(CloneRuntime runtime) {
        for (CloneRuntimeState state : runtime.loadedActors()) {
            if (!armedBody(state)) {
                continue;
            }
            CloneEntity actor = state.actor;
            if (actor == null || actor.isRemoved() || actor.capturedCosmetic()
                    || !(actor.level() instanceof ServerLevel level)) {
                continue;
            }
            List<LivingEntity> found = LivingCandidateQueries.entityQuery(level, actor.getBoundingBox(),
                    candidate -> candidate != actor);
            int examined = 0;
            for (LivingEntity candidate : found) {
                if (examined >= CONTACT_CANDIDATE_CAP) {
                    break;
                }
                examined++;
                if (offer(runtime, state, actor, candidate)) {
                    break;
                }
            }
        }
    }

    static void offerEntity(CloneEntity actor, LivingEntity candidate) {
        if (actor == null || candidate == null || actor.level().isClientSide()
                || !(actor.level() instanceof ServerLevel level)) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.get(level.getServer());
        if (runtime == null) {
            return;
        }
        offer(runtime, runtime.find(actor.getUUID()), actor, candidate);
    }

    private static boolean offer(CloneRuntime runtime, CloneRuntimeState state, CloneEntity actor,
                                 LivingEntity candidate) {
        if (!eligible(runtime, state, actor, candidate)) {
            return false;
        }
        Vec3 frozenLook = state.frozenLook == null ? actor.getLookAngle() : state.frozenLook;
        Vec3 aimed = CaptureVectors.aimOrFrozen(actor.getEyePosition(), candidate.getBoundingBox().getCenter(),
                frozenLook);
        if (!CaptureVectors.unit(aimed)) {
            return false;
        }
        return CapturedCasts.breakForResponse(state.handle(runtime.sessionToken()), CaptureTriggerKind.CONTACT,
                candidate.getUUID(), aimed);
    }

    private static boolean eligible(CloneRuntime runtime, CloneRuntimeState state, CloneEntity actor,
                                    LivingEntity candidate) {
        if (!armedBody(state) || actor == null || candidate == null || candidate == actor || actor.capturedCosmetic()) {
            return false;
        }
        if (candidate.isInvulnerable() || candidate.invulnerableTime > 0) {
            return false;
        }
        if (!actor.getBoundingBox().intersects(candidate.getBoundingBox())) {
            return false;
        }
        ServerPlayer owner = runtime.server().getPlayerList().getPlayer(state.ownerId);
        return !CloneEntity.rejectsPulseTarget(owner, candidate);
    }

    private static boolean armedBody(CloneRuntimeState state) {
        if (state == null || state.cosmeticOnly || state.life != CloneLifeState.ACTIVE || state.budgetSpent
                || state.leaseTransferred) {
            return false;
        }
        if (state.spec.triggerMode() != CloneSpec.TriggerMode.CONTACT_OR_HURT
                || state.spec.terminationPolicy() != TerminationPolicy.BREAK_ON_TRIGGER) {
            return false;
        }
        DeploymentLease lease = state.deploymentLease;
        return lease != null && !lease.released() && lease.phase() == DeploymentLease.Phase.BODY;
    }
}
