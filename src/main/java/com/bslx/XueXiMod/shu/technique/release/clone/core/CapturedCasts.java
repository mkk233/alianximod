package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneEntityTypes;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

final class CapturedCasts {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MANA_LIMIT = 1_000_000;
    private static final int LIFETIME_LIMIT = 1_200_000;
    private static final double DECOY_UPPER = 16.0D;
    private static final String TECHNIQUE_ID = "water_phantom";

    private CapturedCasts() {
    }

    static CloneResults.SpawnResult spawn(ServerPlayer owner, PreparedCapture prepared) {
        if (!CloneService.sameThread(owner)) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.WRONG_SERVER);
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null || !runtime.accepting()) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.WRONG_SERVER);
        }
        if (prepared == null) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_QUOTE);
        }
        if (!owner.isAlive() || owner.isRemoved() || owner.hasDisconnected()) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.OWNER_UNAVAILABLE);
        }
        if (prepared.authority() != CloneSpawnAuthority.PLAYER_CAPTURED_CAST
                || !owner.getUUID().equals(prepared.ownerId())
                || !owner.getUUID().equals(prepared.snapshot().ownerId())) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.OWNER_UNAVAILABLE);
        }
        if (!(owner.level() instanceof ServerLevel level) || !level.dimension().equals(prepared.dimension())) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_GEOMETRY, "生成维度和主人不一致");
        }
        CloneResults.RejectReason quoted = quoteProblem(prepared);
        if (quoted != null) {
            return CloneResults.spawnRejected(quoted);
        }
        if (!CaptureVectors.unit(prepared.lookUnit()) || !CaptureVectors.finite(prepared.eyeOrigin())) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_GEOMETRY, "视线或眼睛位置无效");
        }
        EntityType<CloneEntity> entityType = CloneEntityTypes.cloneType();
        Vec3 feet = prepared.feetPosition();
        String geometryDetail = CloneService.placementDetail(level, entityType, feet.x, feet.y, feet.z);
        if (geometryDetail != null) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_GEOMETRY, geometryDetail);
        }
        CloneResults.RejectReason reserved = runtime.reserve(owner.getUUID(), false);
        if (reserved != null) {
            return CloneResults.spawnRejected(reserved);
        }
        boolean published = false;
        boolean reserveHeld = true;
        boolean refunded = false;
        ManaPaymentReceipt receipt = null;
        CloneRuntimeState publishedState = null;
        try {
            long generation = runtime.allocateGeneration();
            long actionToken = runtime.allocateToken();
            if (generation <= 0L || actionToken <= 0L) {
                return CloneResults.spawnRejected(CloneResults.RejectReason.CAP_REACHED);
            }
            receipt = ManaPaymentReceipt.charge(owner, prepared.totalMana(), true);
            if (receipt == null) {
                return CloneResults.spawnRejected(CloneResults.RejectReason.INSUFFICIENT_MANA);
            }
            UUID deploymentId = UUID.randomUUID();
            DeploymentLease lease = new DeploymentLease(deploymentId, owner.getUUID(), runtime.sessionToken());
            CloneSpec spec = specFor(prepared, deploymentId);
            CloneEntity actor = new CloneEntity(entityType, level);
            actor.moveTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
            actor.face(prepared.lookUnit());
            actor.arm(CloneEntity.ticket(runtime.sessionToken(), owner.getUUID(), generation, spec), runtime.sessionToken());
            actor.markCaptured(prepared.immediate());
            boolean added = level.addFreshEntity(actor);
            if (!added || actor.isRemoved() || actor.generation() != generation) {
                refunded = true;
                receipt.refund(owner);
                if (!actor.isRemoved()) {
                    actor.discard();
                }
                return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_GEOMETRY, "水幻形没有进入世界");
            }
            CloneRuntimeState state = runtime.publish(owner, actor, spec, generation);
            if (state == null) {
                refunded = true;
                receipt.refund(owner);
                if (!actor.isRemoved()) {
                    actor.discard();
                }
                return CloneResults.spawnRejected(CloneResults.RejectReason.CAP_REACHED);
            }
            reserveHeld = false;
            publishedState = state;
            state.deploymentLease = lease;
            state.capturedSnapshot = prepared.snapshot();
            state.cosmeticOnly = prepared.immediate();
            state.frozenLook = prepared.lookUnit();
            state.occupiedToken = actionToken;
            CapturedResponseRuntime.attach(runtime, lease, state);
            ClonePresentation.emit(runtime.server(), state, actor.position(), prepared.lookUnit(),
                    NoticePhase.MAINTAIN, VolumeVisualKind.NONE);
            if (prepared.immediate() && !breakForResponse(state.handle(runtime.sessionToken()),
                    CaptureTriggerKind.IMMEDIATE, null, prepared.lookUnit())) {
                runtime.endActor(state, CloneEndReason.DISMISS);
                refunded = true;
                receipt.refund(owner);
                return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_GEOMETRY, "水幻形没有形成反击");
            }
            published = true;
            PerformanceRecorder.add(PerfCounter.CAPTURE_DEPLOYS, 1L);
            String detail = "已部署水幻形，标识 " + actor.getUUID() + "，实扣法力 " + prepared.totalMana();
            return new CloneResults.SpawnResult(true, null, detail, state.handle(runtime.sessionToken()),
                    CloneKind.PHANTOM, prepared.totalMana(), 1, 0);
        } catch (RuntimeException exception) {
            LOGGER.error("水幻形部署失败 owner={}", owner.getUUID(), exception);
            if (!published && publishedState != null && !publishedState.cleaning
                    && publishedState.life != CloneLifeState.ENDED) {
                runtime.endActor(publishedState, CloneEndReason.DISMISS);
            }
            if (published && publishedState != null) {
                return new CloneResults.SpawnResult(true, null, "已部署水幻形，实扣法力 " + prepared.totalMana(),
                        publishedState.handle(runtime.sessionToken()), CloneKind.PHANTOM, prepared.totalMana(), 1, 0);
            }
            return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_QUOTE);
        } finally {
            if (reserveHeld) {
                runtime.releaseReserve(owner.getUUID());
            }
            if (!published && receipt != null && !refunded) {
                receipt.refund(owner);
            }
        }
    }

    static boolean breakForResponse(CloneHandle handle, CaptureTriggerKind kind, @Nullable UUID subjectId,
                                    Vec3 frozenLook) {
        if (handle == null || kind == null || !CaptureVectors.unit(frozenLook)) {
            return false;
        }
        if ((kind == CaptureTriggerKind.CONTACT || kind == CaptureTriggerKind.ACCEPTED_HURT) && subjectId == null) {
            return false;
        }
        CloneRuntime runtime = CloneRuntime.runtimeForSession(handle.sessionToken());
        if (runtime == null || !runtime.accepting() || CloneSourceScope.blocksResponse()) {
            return false;
        }
        CloneRuntimeState state = runtime.find(handle.actorId());
        if (state == null || state.actorGeneration != handle.actorGeneration() || state.life != CloneLifeState.ACTIVE
                || !state.dimension.equals(handle.dimension())) {
            return false;
        }
        if (state.spec.triggerMode() != CloneSpec.TriggerMode.CONTACT_OR_HURT
                || state.spec.terminationPolicy() != TerminationPolicy.BREAK_ON_TRIGGER) {
            return false;
        }
        DeploymentLease lease = state.deploymentLease;
        if (lease == null || lease.released() || lease.phase() != DeploymentLease.Phase.BODY
                || lease.sessionToken() != runtime.sessionToken() || state.budgetSpent || state.leaseTransferred
                || CapturedResponseRuntime.hasTicket(runtime, lease.deploymentId())) {
            return false;
        }
        if (kind == CaptureTriggerKind.IMMEDIATE && !state.cosmeticOnly) {
            return false;
        }
        if (kind != CaptureTriggerKind.IMMEDIATE && state.cosmeticOnly) {
            return false;
        }
        CloneEntity actor = state.actor;
        CapturedAttackSnapshot snapshot = state.capturedSnapshot;
        if (actor == null || actor.isRemoved() || snapshot == null || state.occupiedToken <= 0L
                || actor.level() != runtime.server().getLevel(state.dimension)) {
            return false;
        }
        if (runtime.tick() >= Long.MAX_VALUE - 2L) {
            return false;
        }
        DetachedActorFrame actorFrame = new DetachedActorFrame(runtime.sessionToken(), state.dimension, state.ownerId,
                snapshot.rootEventId(), lease.deploymentId(), actor.position(), actor.getEyePosition(), frozenLook,
                runtime.server().getTickCount());
        long executeTick = runtime.tick() + 1L;
        CapturedResponseTicket ticket = new CapturedResponseTicket(runtime.sessionToken(), state.ownerId,
                snapshot.rootEventId(), lease.deploymentId(), state.occupiedToken, actorFrame, snapshot, executeTick,
                executeTick + 1L, kind == CaptureTriggerKind.IMMEDIATE);
        if (!lease.transferToTicket()) {
            return false;
        }
        state.budgetSpent = true;
        state.leaseTransferred = true;
        state.response = CloneResponseState.SPENT;
        actor.mirror(state);
        CapturedResponseRuntime.registerTicket(runtime, ticket);
        PerformanceRecorder.add(PerfCounter.CAPTURE_BREAKS, 1L);
        if (kind != CaptureTriggerKind.IMMEDIATE) {
            ClonePresentation.emit(runtime.server(), state, actor.position(), frozenLook, NoticePhase.SCATTER,
                    VolumeVisualKind.NONE);
            runtime.endActor(state, CloneEndReason.BUDGET_SPENT);
        }
        return true;
    }

    static void finish(DeploymentLease lease, CaptureEndReason reason) {
        if (lease == null || reason == null) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.runtimeForSession(lease.sessionToken());
        if (runtime == null) {
            return;
        }
        CapturedResponseRuntime.release(runtime, lease, reason, reason != CaptureEndReason.VOLUME_FINISHED, true);
    }

    private static CloneSpec specFor(PreparedCapture prepared, UUID deploymentId) {
        return new CloneSpec(TECHNIQUE_ID, CloneKind.PHANTOM, new CloneSpec.LifetimePolicy(prepared.residenceTicks()),
                prepared.bodyHealth(), CloneSpec.ResponseBudget.ONE, true, TECHNIQUE_ID,
                CloneSpec.TriggerMode.CONTACT_OR_HURT, CloneSpec.Station.STATIONARY, prepared.decoyRadiusBlocks(),
                TerminationPolicy.BREAK_ON_TRIGGER, PresentationKey.WATER_OUTLINE, 0, deploymentId);
    }

    @Nullable
    private static CloneResults.RejectReason quoteProblem(PreparedCapture prepared) {
        if (prepared.totalMana() > MANA_LIMIT || prepared.residenceTicks() > LIFETIME_LIMIT) {
            return CloneResults.RejectReason.INVALID_QUOTE;
        }
        if (prepared.bodyHealth() != 1.0D || prepared.snapshot().configuredPayloadMana() > MANA_LIMIT) {
            return CloneResults.RejectReason.INVALID_QUOTE;
        }
        double radius = prepared.decoyRadiusBlocks();
        if (!Double.isFinite(radius) || radius < 0.0D || radius > DECOY_UPPER) {
            return CloneResults.RejectReason.INVALID_QUOTE;
        }
        if (prepared.immediate() && radius != 0.0D) {
            return CloneResults.RejectReason.INVALID_GEOMETRY;
        }
        return null;
    }
}
