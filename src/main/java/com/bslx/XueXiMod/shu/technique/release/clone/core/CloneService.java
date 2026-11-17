package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAvailability;
import com.bslx.XueXiMod.shu.balance.BalanceException;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneEntityTypes;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneTechniqueRegistry;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CloneDeploymentInput;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CloneDeploymentResult;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.DeploymentSource;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.DeploymentStatus;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ExternalCooperation;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FrozenCarrierCredential;
import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
import com.bslx.XueXiMod.shu.technique.release.clone.action.CloneAction;
import com.bslx.XueXiMod.shu.technique.release.clone.action.CloneActionCapabilities;
import com.bslx.XueXiMod.shu.technique.release.clone.action.CloneActionRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.action.ClonePreparedAction;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CloneService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MANA_LIMIT = 1_000_000;
    private static final int LIFETIME_LIMIT = 1_200_000;
    private static final double HEALTH_LOWER = 1.0D;
    private static final double HEALTH_UPPER = 1024.0D;
    private static final double DECOY_UPPER = 16.0D;

    private CloneService() {
    }

    public static CloneResults.SpawnResult spawn(ServerPlayer owner, CloneKind kind) {
        if (!sameThread(owner)) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.WRONG_SERVER);
        }
        if (kind == null) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_GEOMETRY);
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null || !runtime.accepting()) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.WRONG_SERVER);
        }
        if (!owner.isAlive() || owner.isRemoved() || owner.hasDisconnected()) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.OWNER_UNAVAILABLE);
        }
        CloneResults.RejectReason availability = developmentOnly(kind);
        if (availability != null) {
            return CloneResults.spawnRejected(availability);
        }
        if (!(owner.level() instanceof ServerLevel level)) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.WRONG_DIMENSION);
        }
        EntityType<CloneEntity> entityType = CloneEntityTypes.cloneType();
        String geometryDetail = placementDetail(level, entityType, owner.getX(), owner.getY(), owner.getZ());
        if (geometryDetail != null) {
            return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_GEOMETRY, geometryDetail);
        }
        return launchWithQuote(owner, runtime, level, kind, null, true, DeploymentSource.DEVELOPMENT);
    }

    /**
     * 无副作用的玩家部署检查。通过时返回 null，调用方再进入 {@link #launchWithQuote} 扣款发布。
     */
    @Nullable
    private static CloneDeploymentResult inspectStandalone(ServerPlayer owner, CloneDeploymentInput input) {
        if (input == null) {
            return refused(DeploymentStatus.ILLEGAL_STRUCTURE, "illegal_structure");
        }
        if (input.source() != DeploymentSource.PLAYER_STANDALONE) {
            return refused(DeploymentStatus.PERMISSION, "permission");
        }
        if (!sameThread(owner)) {
            return refused(DeploymentStatus.ILLEGAL_STRUCTURE, "illegal_structure");
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null || !runtime.accepting()) {
            return refused(DeploymentStatus.ILLEGAL_STRUCTURE, "illegal_structure");
        }
        if (!owner.isAlive() || owner.isRemoved() || owner.hasDisconnected()) {
            return refused(DeploymentStatus.ILLEGAL_STRUCTURE, "illegal_structure");
        }
        if (!spawnQuoteFits(input.quote()) || input.quote().kind() != CloneKind.NORMAL) {
            return refused(DeploymentStatus.ILLEGAL_STRUCTURE, "illegal_structure");
        }
        if (!carrierStillHeld(owner, input.carrier())) {
            return refused(DeploymentStatus.CARRIER_MISSING, "carrier_missing");
        }
        if (!(owner.level() instanceof ServerLevel level)) {
            return refused(DeploymentStatus.ILLEGAL_STRUCTURE, "illegal_structure");
        }
        EntityType<CloneEntity> entityType = CloneEntityTypes.cloneType();
        if (entityType == null) {
            return refused(DeploymentStatus.PUBLISH_FAILED, "publish_failed");
        }
        String geometryDetail = placementDetail(level, entityType, owner.getX(), owner.getY(), owner.getZ());
        if (geometryDetail != null) {
            return refused(DeploymentStatus.UNSAFE_SPAWN, "unsafe_spawn");
        }
        CloneResults.RejectReason capacity = runtime.inspectCapacity(owner.getUUID(), true);
        if (capacity == CloneResults.RejectReason.COOLDOWN) {
            return refused(DeploymentStatus.COOLDOWN, "cooldown");
        }
        if (capacity != null) {
            return refused(DeploymentStatus.QUOTA, "quota");
        }
        return null;
    }

    static CloneDeploymentResult deployStandaloneBody(ServerPlayer owner, CloneDeploymentInput input) {
        CloneDeploymentResult refusal = inspectStandalone(owner, input);
        if (refusal != null) {
            return refusal;
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        ServerLevel level = (ServerLevel) owner.level();
        CloneResults.SpawnResult launched = launchWithQuote(owner, runtime, level, CloneKind.NORMAL, input.quote(),
                true, DeploymentSource.PLAYER_STANDALONE);
        return mapLaunch(launched);
    }

    static int dismissStandaloneBodies(ServerPlayer owner) {
        if (!sameThread(owner)) {
            return 0;
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null) {
            return 0;
        }
        int ended = 0;
        for (CloneRuntimeState state : runtime.ownedActors(owner.getUUID())) {
            if (!standaloneRecallable(state)) {
                continue;
            }
            runtime.endActor(state, CloneEndReason.DISMISS);
            ended++;
        }
        return ended;
    }

    private static CloneResults.SpawnResult launchWithQuote(ServerPlayer owner, CloneRuntime runtime, ServerLevel level,
                                                            CloneKind kind,
                                                            @Nullable CloneBalance.CloneSpawnQuote supplied,
                                                            boolean tellPlayer, DeploymentSource source) {
        EntityType<CloneEntity> entityType = CloneEntityTypes.cloneType();
        CloneResults.RejectReason reserved = runtime.reserve(owner.getUUID());
        if (reserved != null) {
            String detail = reserved == CloneResults.RejectReason.COOLDOWN
                    ? "冷却还没有结束，剩余 " + runtime.spawnCooldownLeft(owner.getUUID()) + " tick"
                    : CloneResults.text(reserved);
            return CloneResults.spawnRejected(reserved, detail);
        }
        boolean published = false;
        boolean refunded = false;
        ManaPaymentReceipt receipt = null;
        try {
            CloneBalance.CloneSpawnQuote quote = supplied;
            if (quote == null) {
                try {
                    quote = CloneBalance.readSpawn(BalanceSide.SERVER, kind);
                } catch (BalanceException exception) {
                    return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_QUOTE);
                }
            }
            if (!spawnQuoteFits(quote) || quote.kind() != kind) {
                return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_QUOTE);
            }
            CloneTechniqueRegistry.CloneSpawnProfile profile = CloneTechniqueRegistry.profile(kind);
            if (profile == null) {
                return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_QUOTE);
            }
            long generation = runtime.allocateGeneration();
            if (generation <= 0L) {
                return CloneResults.spawnRejected(CloneResults.RejectReason.CAP_REACHED);
            }
            receipt = ManaPaymentReceipt.charge(owner, quote.manaCost(), tellPlayer);
            if (receipt == null) {
                return CloneResults.spawnRejected(CloneResults.RejectReason.INSUFFICIENT_MANA);
            }
            CloneSpec spec = specFor(kind, profile, quote);
            CloneEntity actor = new CloneEntity(entityType, level);
            actor.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
            actor.setYHeadRot(owner.getYRot());
            actor.setYBodyRot(owner.getYRot());
            actor.setCustomName(Component.literal(displayName(kind)));
            actor.setCustomNameVisible(true);
            actor.arm(CloneEntity.ticket(runtime.sessionToken(), owner.getUUID(), generation, spec), runtime.sessionToken());
            boolean added = level.addFreshEntity(actor);
            if (!added || actor.isRemoved() || actor.generation() != generation) {
                refunded = true;
                receipt.refund(owner);
                if (!actor.isRemoved()) {
                    actor.discard();
                }
                return CloneResults.spawnRejected(CloneResults.RejectReason.INVALID_GEOMETRY, "分身没有进入世界");
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
            state.deploymentSource = source;
            if (kind == CloneKind.NORMAL) {
                state.frozenResponseCooldownTicks = frozenPulseCooldown();
            }
            published = true;
            runtime.armSpawnCooldown(owner.getUUID(), quote.cooldownTicks());
            PerformanceRecorder.add(PerfCounter.CLONE_SPAWNS, 1L);
            int responses = kind == CloneKind.PHANTOM ? 1 : CloneResults.REPEAT_CHARGES;
            String charges = responses == CloneResults.REPEAT_CHARGES ? "repeat" : Integer.toString(responses);
            String detail = "已生成" + displayName(kind)
                    + "，标识 " + actor.getUUID()
                    + "，类型 " + kind
                    + "，实扣法力 " + quote.manaCost()
                    + "，剩余响应 " + charges
                    + "，生成冷却 " + quote.cooldownTicks();
            return new CloneResults.SpawnResult(true, null, detail, state.handle(runtime.sessionToken()), kind,
                    quote.manaCost(), responses, quote.cooldownTicks());
        } finally {
            if (!published) {
                runtime.releaseReserve(owner.getUUID());
                if (receipt != null && !refunded) {
                    receipt.refund(owner);
                }
            }
        }
    }

    public static CloneResults.SpawnResult spawnCaptured(ServerPlayer owner, PreparedCapture prepared) {
        return CapturedCasts.spawn(owner, prepared);
    }

    public static boolean breakForResponse(CloneHandle handle, CaptureTriggerKind kind, @Nullable UUID subjectId,
                                           Vec3 frozenLook) {
        return CapturedCasts.breakForResponse(handle, kind, subjectId, frozenLook);
    }

    public static void finishDeployment(DeploymentLease lease, CaptureEndReason reason) {
        CapturedCasts.finish(lease, reason);
    }

    @Nullable
    public static ServerPlayer findPresentationOwner(MinecraftServer server, UUID deploymentId) {
        if (server == null || deploymentId == null || !server.isSameThread()) {
            return null;
        }
        CloneRuntime runtime = CloneRuntime.get(server);
        if (runtime == null) {
            return null;
        }
        UUID ownerId = CapturedResponseRuntime.presentationOwnerId(runtime, deploymentId);
        if (ownerId == null) {
            return null;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.hasDisconnected() || owner.isRemoved()) {
            return null;
        }
        return owner;
    }

    public static CloneResults.RequestResult request(ServerPlayer owner, CloneHandle handle, CloneTriggerInput input) {
        if (!sameThread(owner)) {
            return rejectRequest(CloneResults.RejectReason.WRONG_SERVER, null);
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null || !runtime.accepting() || handle == null || input == null) {
            return rejectRequest(CloneResults.RejectReason.WRONG_SERVER, null);
        }
        if (runtime.sessionToken() != handle.sessionToken()) {
            return rejectRequest(CloneResults.RejectReason.WRONG_SERVER, null);
        }
        if (!owner.isAlive() || owner.isRemoved() || owner.hasDisconnected()) {
            return rejectRequest(CloneResults.RejectReason.OWNER_UNAVAILABLE, null);
        }
        CloneRuntimeState state = runtime.owned(owner.getUUID(), handle.actorId());
        if (state == null) {
            CloneRuntimeState other = runtime.find(handle.actorId());
            CloneResults.RejectReason reason = other == null
                    ? CloneResults.RejectReason.ACTOR_GONE
                    : CloneResults.RejectReason.WRONG_OWNER;
            return rejectRequest(reason, null);
        }
        if (state.actorGeneration != handle.actorGeneration() || state.life != CloneLifeState.ACTIVE) {
            return rejectRequest(CloneResults.RejectReason.ACTOR_GONE, null);
        }
        if (!state.dimension.equals(handle.dimension()) || !owner.level().dimension().equals(state.dimension)) {
            return rejectRequest(CloneResults.RejectReason.WRONG_DIMENSION, null);
        }
        CloneEntity actor = state.actor;
        if (actor == null || actor.isRemoved() || !actor.isAlive() || actor.level() != owner.level()) {
            return rejectRequest(CloneResults.RejectReason.ACTOR_GONE, null);
        }
        if (input.source() == CloneTriggerInput.Source.MANUAL && !ClonePositionPolicy.unit(input.manualLook())) {
            return rejectRequest(CloneResults.RejectReason.INVALID_GEOMETRY, "视线方向无效");
        }
        if (input.source() == CloneTriggerInput.Source.HURT && input.attackerId() == null) {
            return rejectRequest(CloneResults.RejectReason.NO_TARGET, null);
        }
        int heldRequests = state.pending == null ? 0 : 1;
        if (heldRequests >= CloneRuntime.ACTOR_REQUEST_CAP) {
            return rejectRequest(CloneResults.RejectReason.BUSY, null);
        }
        CloneResults.RejectReason blocked = CloneTriggerPolicy.rejection(input, state);
        if (blocked != null) {
            String detail = blocked == CloneResults.RejectReason.COOLDOWN
                    ? "冷却还没有结束，剩余 " + runtime.responseCooldownLeft(state) + " tick"
                    : blocked == CloneResults.RejectReason.BUSY && CloneSourceScope.blocksResponse()
                    ? "这次响应还在提交，不能再触发"
                    : null;
            return rejectRequest(blocked, detail);
        }
        if (runtime.queueSize() >= CloneRuntime.SERVER_REQUEST_CAP) {
            return rejectRequest(CloneResults.RejectReason.QUEUE_FULL, null);
        }
        long serial = runtime.allocateSerial();
        if (serial <= 0L) {
            return rejectRequest(CloneResults.RejectReason.QUEUE_FULL, null);
        }
        long requestedTick = runtime.tick();
        CloneActionRequest actionRequest = new CloneActionRequest(state.handle(runtime.sessionToken()), UUID.randomUUID(),
                input.source(), input.sourceDepth(), state.spec.actionKey(), serial, requestedTick,
                requestedTick + 1L, requestedTick + CloneRuntime.REQUEST_LIFE_TICKS,
                input.manualLook(), input.attackerId());
        state.response = CloneResponseState.RESERVED;
        state.pending = actionRequest;
        actor.mirror(state);
        if (!runtime.enqueue(actionRequest)) {
            state.response = CloneResponseState.READY;
            state.pending = null;
            actor.mirror(state);
            return rejectRequest(CloneResults.RejectReason.QUEUE_FULL, null);
        }
        PerformanceRecorder.add(PerfCounter.CLONE_RESPONSE_REQUESTS, 1L);
        return CloneResults.requestAccepted();
    }

    public static void onAcceptedHurt(CloneEntity actor, DamageSource source) {
        if (actor == null || actor.level().isClientSide() || !(actor.level() instanceof ServerLevel level)) {
            return;
        }
        if (!level.getServer().isSameThread() || actor.lifeState() != CloneLifeState.ACTIVE || !actor.isAlive()) {
            return;
        }
        LivingEntity attacker = livingCause(source);
        if (attacker == null || attacker.level() != level || !attacker.isAlive() || attacker instanceof CloneEntity) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.get(level.getServer());
        if (runtime == null || !runtime.accepting()) {
            return;
        }
        CloneRuntimeState state = runtime.find(actor.getUUID());
        if (state == null || state.life != CloneLifeState.ACTIVE) {
            return;
        }
        if (state.spec.terminationPolicy() == TerminationPolicy.BREAK_ON_TRIGGER) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(state.ownerId);
        if (owner == null || !owner.isAlive() || owner.hasDisconnected()) {
            return;
        }
        if (CloneEntity.rejectsPulseTarget(owner, attacker)) {
            return;
        }
        CloneTriggerInput input = new CloneTriggerInput(CloneTriggerInput.Source.HURT, null, attacker.getUUID(), 0);
        request(owner, state.handle(runtime.sessionToken()), input);
    }

    public static boolean dismiss(ServerPlayer owner, UUID actorId) {
        if (!sameThread(owner) || actorId == null) {
            return false;
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null) {
            return false;
        }
        CloneRuntimeState state = runtime.owned(owner.getUUID(), actorId);
        boolean ended = false;
        if (state != null && state.life != CloneLifeState.ENDED) {
            runtime.endActor(state, CloneEndReason.DISMISS);
            ended = true;
        }
        return ended || CapturedResponseRuntime.cancelActor(runtime, owner.getUUID(), actorId);
    }

    public static int dismissAll(ServerPlayer owner) {
        if (!sameThread(owner)) {
            return 0;
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null) {
            return 0;
        }
        List<CloneRuntimeState> owned = runtime.ownedActors(owner.getUUID());
        int ended = 0;
        for (CloneRuntimeState state : owned) {
            if (state.life != CloneLifeState.ACTIVE) {
                continue;
            }
            runtime.endActor(state, CloneEndReason.DISMISS);
            ended++;
        }
        return ended + CapturedResponseRuntime.dismissOwned(owner.getUUID());
    }

    public static void end(CloneHandle handle, CloneEndReason reason) {
        if (handle == null || reason == null) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.get(currentServer(handle.sessionToken()));
        if (runtime == null || runtime.sessionToken() != handle.sessionToken()) {
            return;
        }
        CloneRuntimeState state = runtime.find(handle.actorId());
        if (state == null || state.actorGeneration != handle.actorGeneration()) {
            return;
        }
        runtime.endActor(state, reason);
    }

    @Nullable
    public static CloneResults.Status status(ServerPlayer owner, UUID actorId) {
        CloneRuntimeState state = ownedActive(owner, actorId);
        if (state == null) {
            return null;
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null) {
            return null;
        }
        return statusOf(runtime, state);
    }

    public static List<CloneResults.Status> list(ServerPlayer owner) {
        if (!sameThread(owner)) {
            return List.of();
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null) {
            return List.of();
        }
        List<CloneResults.Status> lines = new ArrayList<>();
        for (CloneRuntimeState state : runtime.ownedActors(owner.getUUID())) {
            if (state.life == CloneLifeState.ENDED) {
                continue;
            }
            lines.add(statusOf(runtime, state));
        }
        return lines;
    }

    @Nullable
    public static CloneHandle handleFor(ServerPlayer owner, UUID actorId) {
        CloneRuntimeState state = ownedActive(owner, actorId);
        if (state == null) {
            return null;
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null) {
            return null;
        }
        return state.handle(runtime.sessionToken());
    }

    @Nullable
    private static CaptureEndReason captureReason(CloneEndReason reason) {
        return switch (reason) {
            case OWNER_LOGOUT -> CaptureEndReason.OWNER_LOGOUT;
            case OWNER_DEATH -> CaptureEndReason.OWNER_DEATH;
            case OWNER_DIMENSION -> CaptureEndReason.OWNER_DIMENSION;
            case UNLOAD -> CaptureEndReason.AREA_UNLOADED;
            case WORLD_UNLOAD -> CaptureEndReason.WORLD_UNLOAD;
            case SERVER_STOP -> CaptureEndReason.SERVER_STOP;
            default -> null;
        };
    }

    static void endLoaded(CloneEntity actor, CloneEndReason reason) {
        if (actor == null || !(actor.level() instanceof ServerLevel level)) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.get(level.getServer());
        if (runtime == null) {
            return;
        }
        CloneRuntimeState state = runtime.find(actor.getUUID());
        if (state == null) {
            return;
        }
        runtime.endActor(state, reason);
    }

    static void endOwned(ServerPlayer owner, CloneEndReason reason) {
        if (!sameThread(owner)) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null) {
            return;
        }
        for (CloneRuntimeState state : runtime.ownedActors(owner.getUUID())) {
            runtime.endActor(state, reason);
        }
        CaptureEndReason captureReason = captureReason(reason);
        if (captureReason == null) {
            CapturedResponseRuntime.dismissOwned(owner.getUUID());
        } else {
            CapturedResponseRuntime.cancelOwned(owner.getUUID(), captureReason);
        }
    }

    static void endDimension(ServerLevel level, CloneEndReason reason) {
        if (level == null || !level.getServer().isSameThread()) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.get(level.getServer());
        if (runtime == null) {
            return;
        }
        for (CloneRuntimeState state : new ArrayList<>(runtime.ownedActorsIn(level.dimension()))) {
            runtime.endActor(state, reason);
        }
        CapturedResponseRuntime.cancelDimension(level.dimension(), CaptureEndReason.WORLD_UNLOAD);
    }

    static void cancelQueued(CloneRuntime runtime, CloneActionRequest request) {
        CloneRuntimeState state = runtime.find(request.handle().actorId());
        if (state == null || state.commitLine) {
            return;
        }
        if (state.pending != null && state.pending.requestSerial() == request.requestSerial()) {
            state.pending = null;
            if (state.cooperationHold == null && state.life == CloneLifeState.ACTIVE
                    && state.response == CloneResponseState.RESERVED) {
                state.response = CloneResponseState.READY;
                if (state.actor != null) {
                    state.actor.mirror(state);
                }
            }
            if (state.noteRejection(request.requestSerial())) {
                PerformanceRecorder.add(PerfCounter.CLONE_RESPONSE_REJECTIONS, 1L);
                tellManual(runtime, request, state.ownerId, "这次响应没有在时限内执行");
            }
        }
    }

    static boolean perform(CloneRuntime runtime, CloneActionRequest request) {
        long started = PerformanceRecorder.begin();
        Attempt attempt = new Attempt();
        CloneRuntimeState state = runtime.find(request.handle().actorId());
        ServerPlayer owner = state == null ? null : runtime.server().getPlayerList().getPlayer(state.ownerId);
        try {
            if (state == null || state.pending == null || state.pending.requestSerial() != request.requestSerial()) {
                return false;
            }
            if (state.actionSequence >= Long.MAX_VALUE - 1L) {
                runtime.endActor(state, CloneEndReason.SEQUENCE_GUARD);
                tellManual(runtime, request, state.ownerId, "动作序号已经用尽，这具分身结束");
                return false;
            }
            state.executing = true;
            owner = runtime.server().getPlayerList().getPlayer(state.ownerId);
            CloneEntity actor = state.actor;
            if (actor == null || actor.isRemoved() || !actor.isAlive() || state.life != CloneLifeState.ACTIVE) {
                return attempt.fail(CloneResults.RejectReason.ACTOR_GONE, null);
            }
            if (owner == null || owner.hasDisconnected() || !owner.isAlive() || owner.isRemoved()) {
                return attempt.fail(CloneResults.RejectReason.OWNER_UNAVAILABLE, null);
            }
            if (owner.level() != actor.level()) {
                return attempt.fail(CloneResults.RejectReason.WRONG_DIMENSION, null);
            }
            if (!state.spec.actionKey().equals(request.actionKey())) {
                return attempt.fail(CloneResults.RejectReason.UNSUPPORTED_ACTION, null);
            }
            CloneTriggerInput input = new CloneTriggerInput(request.source(), request.manualLook(),
                    request.attackerId(), request.sourceDepth());
            if (CloneSourceScope.blocksResponse() || input.sourceDepth() > 0 || state.cooperationHold != null) {
                return attempt.fail(CloneResults.RejectReason.BUSY, "这次响应还在提交，不能再触发");
            }
            if (state.budgetSpent || state.response == CloneResponseState.SPENT) {
                return attempt.fail(CloneResults.RejectReason.BUDGET_SPENT, null);
            }
            if (state.response != CloneResponseState.RESERVED || state.life != CloneLifeState.ACTIVE) {
                return attempt.fail(state.life == CloneLifeState.ACTIVE
                        ? CloneResults.RejectReason.BUSY
                        : CloneResults.RejectReason.ACTOR_GONE, null);
            }
            CloneActionRegistry.Admission admission = CloneActionRegistry.admit(state.spec.actionKey(),
                    CloneActionCapabilities.full());
            if (admission == CloneActionRegistry.Admission.UNSUPPORTED_CHAIN) {
                return attempt.fail(CloneResults.RejectReason.UNSUPPORTED_CHAIN, null);
            }
            if (admission != CloneActionRegistry.Admission.ADMITTED) {
                return attempt.fail(CloneResults.RejectReason.UNSUPPORTED_ACTION, null);
            }
            CloneAction action = CloneActionRegistry.get(state.spec.actionKey());
            if (action == null) {
                return attempt.fail(CloneResults.RejectReason.UNSUPPORTED_ACTION, null);
            }
            CloneActorFrame actorFrame = ClonePositionPolicy.frame(actor, input);
            if (actorFrame == null) {
                CloneResults.RejectReason reason = request.source() == CloneTriggerInput.Source.HURT
                        ? CloneResults.RejectReason.NO_TARGET
                        : CloneResults.RejectReason.INVALID_GEOMETRY;
                return attempt.fail(reason, null);
            }
            long token = runtime.allocateToken();
            if (token <= 0L) {
                runtime.endActor(state, CloneEndReason.SEQUENCE_GUARD);
                return attempt.fail(CloneResults.RejectReason.ACTOR_GONE, "动作序号已经用尽，这具分身结束");
            }
            state.prepareCancelled = false;
            ServiceCancel cancel = new ServiceCancel(state);
            CloneActionContext actionContext = new CloneActionContext(owner, actorFrame, request.rootEventId(),
                    token, cancel);
            try {
                attempt.prepared = action.prepare(actionContext);
            } catch (BalanceException exception) {
                return attempt.fail(CloneResults.RejectReason.INVALID_QUOTE, null);
            }
            if (attempt.prepared == null || !attempt.prepared.accepted()) {
                CloneResults.RejectReason reason = attempt.prepared == null || attempt.prepared.reason() == null
                        ? CloneResults.RejectReason.NO_TARGET
                        : attempt.prepared.reason();
                String detail = attempt.prepared == null ? null : attempt.prepared.detail();
                return attempt.fail(reason, detail);
            }
            if (cancel.cancelled() || state.life != CloneLifeState.ACTIVE || actor.isRemoved()) {
                return attempt.fail(CloneResults.RejectReason.ACTOR_GONE, null);
            }
            if (!quoteFits(attempt.prepared)) {
                return attempt.fail(CloneResults.RejectReason.INVALID_QUOTE, null);
            }
            attempt.receipt = ManaPaymentReceipt.charge(owner, attempt.prepared.quotedMana(),
                    request.source() == CloneTriggerInput.Source.MANUAL);
            if (attempt.receipt == null) {
                return attempt.fail(CloneResults.RejectReason.INSUFFICIENT_MANA, null);
            }
            crossCommitLine(runtime, state, actor, actorFrame, token, attempt.prepared.quotedCooldownTicks());
            attempt.commitLine = true;
            state.commitLine = true;
            attempt.scope = CloneSourceScope.open(runtime.server(), CloneSourceScope.Origin.CLONE_RESPONSE,
                    request.rootEventId(), 1);
            attempt.prepared.commit();
            settleCommitted(runtime, state);
            return true;
        } catch (RuntimeException exception) {
            if (attempt.commitLine) {
                LOGGER.error("分身响应在提交线之后失败 actor={} root={}",
                        state == null ? null : state.actorId, request.rootEventId(), exception);
                return true;
            }
            LOGGER.error("分身响应在提交前失败 actor={} root={}",
                    state == null ? null : state.actorId, request.rootEventId(), exception);
            attempt.fail(CloneResults.RejectReason.INVALID_QUOTE, null);
            return false;
        } finally {
            if (attempt.scope != null) {
                try {
                    attempt.scope.close();
                } catch (RuntimeException exception) {
                    LOGGER.error("分身来源作用域关闭失败 actor={}", state == null ? null : state.actorId, exception);
                }
            }
            if (!attempt.commitLine) {
                if (attempt.prepared != null) {
                    try {
                        attempt.prepared.cancelBeforeCommit();
                    } catch (RuntimeException exception) {
                        LOGGER.error("分身准备取消失败 actor={}", state == null ? null : state.actorId, exception);
                    }
                }
                if (attempt.receipt != null && owner != null && !owner.isRemoved()) {
                    attempt.refundOnce(owner);
                }
                if (attempt.countRejection && state != null && state.noteRejection(request.requestSerial())) {
                    PerformanceRecorder.add(PerfCounter.CLONE_RESPONSE_REJECTIONS, 1L);
                    tellManual(runtime, request, state.ownerId,
                            attempt.detail == null ? CloneResults.text(attempt.failure) : attempt.detail);
                }
                restoreReady(state);
            } else if (state != null) {
                settleCommitted(runtime, state);
            }
            if (state != null) {
                state.executing = false;
            }
            PerformanceRecorder.end(PerfMetric.CLONE_RESPONSE, started);
        }
    }

    private static void crossCommitLine(CloneRuntime runtime, CloneRuntimeState state, CloneEntity actor,
                                        CloneActorFrame actorFrame, long token, int cooldownTicks) {
        state.response = CloneResponseState.COMMITTING;
        state.occupiedToken = token;
        if (state.spec.budget() == CloneSpec.ResponseBudget.ONE) {
            state.budgetSpent = true;
        }
        long ready = runtime.tick() + Math.max(cooldownTicks, 1);
        if (ready < runtime.tick()) {
            ready = Long.MAX_VALUE;
        }
        state.cooldownReadyTick = ready;
        state.actionSequence++;
        actor.mirror(state);
        actor.face(actorFrame.lookUnit());
        PerformanceRecorder.add(PerfCounter.CLONE_RESPONSE_COMMITS, 1L);
    }

    private static void settleCommitted(CloneRuntime runtime, CloneRuntimeState state) {
        if (state.life != CloneLifeState.ACTIVE) {
            return;
        }
        if (state.spec.endAfterBudgetSpent()) {
            state.response = CloneResponseState.SPENT;
            state.budgetSpent = true;
            if (state.actor != null) {
                state.actor.mirror(state);
            }
            runtime.endActor(state, CloneEndReason.BUDGET_SPENT);
            return;
        }
        if (state.response == CloneResponseState.COMMITTING) {
            state.response = CloneResponseState.COOLDOWN;
            state.pending = null;
            state.commitLine = false;
            if (state.actor != null) {
                state.actor.mirror(state);
            }
        }
    }

    private static void restoreReady(@Nullable CloneRuntimeState state) {
        if (state == null || state.commitLine || state.life != CloneLifeState.ACTIVE) {
            return;
        }
        if (state.cooperationHold != null) {
            state.pending = null;
            if (state.response != CloneResponseState.RESERVED) {
                state.response = CloneResponseState.RESERVED;
            }
            if (state.actor != null) {
                state.actor.mirror(state);
            }
            return;
        }
        if (state.response == CloneResponseState.RESERVED) {
            state.response = CloneResponseState.READY;
            state.pending = null;
            if (state.actor != null) {
                state.actor.mirror(state);
            }
        }
    }

    private static boolean quoteFits(ClonePreparedAction prepared) {
        int mana = prepared.quotedMana();
        int cooldown = prepared.quotedCooldownTicks();
        ConfiguredDamage damage = prepared.configuredDamage();
        if (mana < 0 || mana > MANA_LIMIT || cooldown < 0 || damage == null) {
            return false;
        }
        float amount = damage.amount();
        return Float.isFinite(amount) && amount >= 0.0F;
    }

    private static boolean spawnQuoteFits(CloneBalance.CloneSpawnQuote quote) {
        if (quote == null) {
            return false;
        }
        return quote.manaCost() >= 0 && quote.manaCost() <= MANA_LIMIT
                && quote.cooldownTicks() >= 0
                && quote.lifetimeTicks() >= 0 && quote.lifetimeTicks() <= LIFETIME_LIMIT
                && Double.isFinite(quote.bodyHealth())
                && quote.bodyHealth() >= HEALTH_LOWER && quote.bodyHealth() <= HEALTH_UPPER
                && Double.isFinite(quote.decoyRadiusBlocks())
                && quote.decoyRadiusBlocks() >= 0.0D && quote.decoyRadiusBlocks() <= DECOY_UPPER;
    }

    private static CloneSpec specFor(CloneKind kind, CloneTechniqueRegistry.CloneSpawnProfile profile,
                                     CloneBalance.CloneSpawnQuote quote) {
        boolean phantom = kind == CloneKind.PHANTOM;
        ExternalCooperation cooperation = phantom ? ExternalCooperation.CLOSED : ExternalCooperation.OPEN;
        return new CloneSpec(profile.techniqueId(), kind, new CloneSpec.LifetimePolicy(quote.lifetimeTicks()),
                quote.bodyHealth(), phantom ? CloneSpec.ResponseBudget.ONE : CloneSpec.ResponseBudget.REPEAT,
                phantom, profile.actionKey(), CloneSpec.TriggerMode.MANUAL_AND_HURT, CloneSpec.Station.STATIONARY,
                quote.decoyRadiusBlocks(), TerminationPolicy.BUDGET, PresentationKey.LEGACY_HUMANOID, 0, null,
                cooperation);
    }

    @Nullable
    private static CloneResults.RejectReason developmentOnly(CloneKind kind) {
        CloneTechniqueRegistry.CloneSpawnProfile profile = CloneTechniqueRegistry.profile(kind);
        Technique body = CloneTechniqueRegistry.technique(kind);
        Technique pulse = CloneTechniqueRegistry.pulse();
        if (profile == null || body == null || pulse == null || profile.kind() != kind
                || !CloneBalance.PULSE_ID.equals(profile.actionKey())) {
            return CloneResults.RejectReason.DEVELOPMENT_ENTRY_ONLY;
        }
        if (kind == CloneKind.PHANTOM
                && (body.availability() != TechniqueAvailability.DEVELOPMENT_ONLY
                || pulse.availability() != TechniqueAvailability.DEVELOPMENT_ONLY)) {
            return CloneResults.RejectReason.DEVELOPMENT_ENTRY_ONLY;
        }
        return null;
    }

    private static CloneDeploymentResult mapLaunch(CloneResults.SpawnResult launched) {
        if (launched.accepted() && launched.handle() != null) {
            return new CloneDeploymentResult(DeploymentStatus.SUCCESS, launched.handle(), null);
        }
        CloneResults.RejectReason reason = launched.reason();
        if (reason == CloneResults.RejectReason.COOLDOWN) {
            return refused(DeploymentStatus.COOLDOWN, "cooldown");
        }
        if (reason == CloneResults.RejectReason.CAP_REACHED || reason == CloneResults.RejectReason.QUEUE_FULL) {
            return refused(DeploymentStatus.QUOTA, "quota");
        }
        if (reason == CloneResults.RejectReason.INSUFFICIENT_MANA) {
            return refused(DeploymentStatus.PAYMENT_FAILED, "payment_failed");
        }
        if (reason == CloneResults.RejectReason.INVALID_GEOMETRY) {
            return refused(DeploymentStatus.PUBLISH_FAILED, "publish_failed");
        }
        if (reason == CloneResults.RejectReason.INVALID_QUOTE) {
            return refused(DeploymentStatus.ILLEGAL_STRUCTURE, "illegal_structure");
        }
        return refused(DeploymentStatus.PUBLISH_FAILED, "publish_failed");
    }

    private static CloneDeploymentResult refused(DeploymentStatus status, String refusalKey) {
        return new CloneDeploymentResult(status, null, refusalKey);
    }

    private static boolean carrierStillHeld(ServerPlayer owner, FrozenCarrierCredential carrier) {
        ItemStack held = owner.getMainHandItem();
        if (held == null || held.isEmpty()) {
            return false;
        }
        CastSource probe = new CastSource(carrier.skillSlot(), held, owner.getInventory().selected, carrier.generation());
        return carrier.matches(probe);
    }

    private static boolean standaloneRecallable(CloneRuntimeState state) {
        if (state.life != CloneLifeState.ACTIVE || state.spec.kind() != CloneKind.NORMAL) {
            return false;
        }
        if (!state.spec.externalCooperation().enabled()) {
            return false;
        }
        if (state.spec.terminationPolicy() == TerminationPolicy.BREAK_ON_TRIGGER || state.spec.deploymentId() != null) {
            return false;
        }
        if (state.deploymentLease != null || state.cosmeticOnly || state.capturedSnapshot != null) {
            return false;
        }
        CloneEntity actor = state.actor;
        return actor == null || !actor.capturedCosmetic();
    }

    private static int frozenPulseCooldown() {
        try {
            CloneBalance.ClonePulseQuote pulseQuote = CloneBalance.readPulse(BalanceSide.SERVER);
            if (pulseQuote.cooldownTicks() >= 0) {
                return pulseQuote.cooldownTicks();
            }
        } catch (RuntimeException exception) {
            LOGGER.error("部署时没有读到内部响应冷却，改用代码默认", exception);
        }
        return CloneBalance.PULSE_COOLDOWN_TICKS;
    }

    @Nullable
    static String placementDetail(ServerLevel level, EntityType<CloneEntity> entityType,
                                          double feetX, double feetY, double feetZ) {
        if (!Double.isFinite(feetX) || !Double.isFinite(feetY) || !Double.isFinite(feetZ)) {
            return "生成坐标无效";
        }
        EntityDimensions dimensions = entityType.getDimensions();
        AABB body = dimensions.makeBoundingBox(feetX, feetY, feetZ);
        BlockPos feet = BlockPos.containing(feetX, feetY, feetZ);
        BlockPos crown = BlockPos.containing(feetX, feetY + dimensions.height, feetZ);
        if (!level.isLoaded(feet) || !level.isLoaded(crown)) {
            return "生成位置还未加载";
        }
        WorldBorder border = level.getWorldBorder();
        if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(crown)
                || !border.isWithinBounds(body.minX, body.minZ)
                || !border.isWithinBounds(body.maxX, body.minZ)
                || !border.isWithinBounds(body.minX, body.maxZ)
                || !border.isWithinBounds(body.maxX, body.maxZ)) {
            return "生成位置超出世界边界";
        }
        for (VoxelShape shape : level.getBlockCollisions(null, body)) {
            if (!shape.isEmpty()) {
                return "生成位置卡在实心方块里";
            }
        }
        return null;
    }

    private static CloneResults.Status statusOf(CloneRuntime runtime, CloneRuntimeState state) {
        CloneEntity actor = state.actor;
        Vec3 actorPosition = actor == null ? Vec3.ZERO : actor.position();
        double healthNow = actor == null ? 0.0D : actor.getHealth();
        int responses = state.spec.budget() == CloneSpec.ResponseBudget.REPEAT
                ? CloneResults.REPEAT_CHARGES
                : state.budgetSpent ? 0 : 1;
        return new CloneResults.Status(state.actorId, state.spec.kind(), state.life, state.response, actorPosition,
                healthNow, state.spec.bodyHealth(), responses, runtime.responseCooldownLeft(state));
    }

    @Nullable
    private static CloneRuntimeState ownedActive(ServerPlayer owner, UUID actorId) {
        if (!sameThread(owner) || actorId == null) {
            return null;
        }
        CloneRuntime runtime = CloneRuntime.get(owner.getServer());
        if (runtime == null) {
            return null;
        }
        CloneRuntimeState state = runtime.owned(owner.getUUID(), actorId);
        if (state == null || state.life == CloneLifeState.ENDED) {
            return null;
        }
        return state;
    }

    private static CloneResults.RequestResult rejectRequest(CloneResults.RejectReason reason, @Nullable String detail) {
        PerformanceRecorder.add(PerfCounter.CLONE_RESPONSE_REJECTIONS, 1L);
        if (detail == null || detail.isBlank()) {
            return CloneResults.requestRejected(reason);
        }
        return CloneResults.requestRejected(reason, detail);
    }

    private static void tellManual(CloneRuntime runtime, CloneActionRequest request, UUID ownerId, String detail) {
        if (request.source() != CloneTriggerInput.Source.MANUAL || detail == null || detail.isBlank()) {
            return;
        }
        ServerPlayer owner = runtime.server().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.hasDisconnected()) {
            return;
        }
        owner.sendSystemMessage(Component.literal(detail));
    }

    @Nullable
    private static LivingEntity livingCause(@Nullable DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity cause = source.getEntity();
        if (cause instanceof LivingEntity causeLiving && causeLiving.isAlive()) {
            return causeLiving;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity directLiving && directLiving.isAlive()) {
            return directLiving;
        }
        return null;
    }

    static boolean sameThread(@Nullable ServerPlayer owner) {
        return owner != null && owner.getServer() != null && owner.getServer().isSameThread();
    }

    @Nullable
    private static net.minecraft.server.MinecraftServer currentServer(long sessionToken) {
        return CloneRuntime.serverFor(sessionToken);
    }

    private static String displayName(CloneKind kind) {
        return kind == CloneKind.PHANTOM ? "幻影分身·基础" : "普通分身·基础";
    }

    private static final class ServiceCancel implements CloneActionContext.CancelToken {
        private final CloneRuntimeState state;

        private ServiceCancel(CloneRuntimeState state) {
            this.state = state;
        }

        @Override
        public boolean cancelled() {
            return this.state.prepareCancelled;
        }
    }

    private static final class Attempt {
        @Nullable
        private ClonePreparedAction prepared;
        @Nullable
        private ManaPaymentReceipt receipt;
        @Nullable
        private CloneSourceScope scope;
        @Nullable
        private CloneResults.RejectReason failure;
        @Nullable
        private String detail;
        private boolean commitLine;
        private boolean countRejection;
        private boolean refunded;

        private boolean fail(CloneResults.RejectReason reason, @Nullable String text) {
            this.failure = reason;
            this.detail = text;
            this.countRejection = true;
            return false;
        }

        private void refundOnce(ServerPlayer owner) {
            if (this.refunded || this.commitLine || this.receipt == null) {
                return;
            }
            this.refunded = true;
            this.receipt.refund(owner);
        }
    }
}
