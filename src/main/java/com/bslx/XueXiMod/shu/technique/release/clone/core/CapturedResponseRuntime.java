package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadAdapter;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorVolumeRuntime;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorVolumeSpec;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.CapturedActionAuthority;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CapturedResponseRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String IMMEDIATE_MISS = "水幻形这一下没有打出去";

    private CapturedResponseRuntime() {
    }

    public static int tick(MinecraftServer server, int commitBudget) {
        CloneRuntime runtime = CloneRuntime.get(server);
        if (runtime == null || !runtime.accepting() || runtime.board.ticketOrder.isEmpty()) {
            return 0;
        }
        int commits = 0;
        int guard = runtime.board.ticketOrder.size();
        int walked = 0;
        while (!runtime.board.ticketOrder.isEmpty() && walked < guard) {
            UUID deploymentId = runtime.board.ticketOrder.peekFirst();
            CapturedResponseTicket ticket = runtime.board.tickets.get(deploymentId);
            if (ticket == null) {
                runtime.board.ticketOrder.pollFirst();
                walked++;
                continue;
            }
            if (runtime.tick() > ticket.expireTick()) {
                runtime.board.ticketOrder.pollFirst();
                runtime.board.tickets.remove(deploymentId);
                walked++;
                failTicket(runtime, ticket, CaptureEndReason.TICKET_EXPIRED);
                continue;
            }
            if (ticket.executeTick() > runtime.tick() || commitBudget <= 0 || commits >= commitBudget) {
                break;
            }
            runtime.board.ticketOrder.pollFirst();
            runtime.board.tickets.remove(deploymentId);
            walked++;
            if (execute(runtime, ticket)) {
                commits++;
            }
        }
        return commits;
    }

    public static void cancelOwned(UUID ownerId, CaptureEndReason reason) {
        if (ownerId == null || reason == null) {
            return;
        }
        for (CloneRuntime runtime : CloneRuntime.sameThreadRuntimes()) {
            cancelMatching(runtime, reason, deployment -> ownerId.equals(deployment.lease.ownerId()));
        }
    }

    public static void cancelDimension(ResourceKey<Level> dimension, CaptureEndReason reason) {
        if (dimension == null || reason == null) {
            return;
        }
        for (CloneRuntime runtime : CloneRuntime.sameThreadRuntimes()) {
            cancelMatching(runtime, reason, deployment -> dimension.equals(deployment.body.dimension)
                    || ticketInDimension(runtime, deployment, dimension));
        }
    }

    public static void shutdown(CaptureEndReason reason) {
        CaptureEndReason actual = reason == null ? CaptureEndReason.SERVER_STOP : reason;
        for (CloneRuntime runtime : CloneRuntime.sameThreadRuntimes()) {
            List<CapturedDeployment> snapshot = new ArrayList<>(runtime.board.deployments.values());
            for (CapturedDeployment deployment : snapshot) {
                release(runtime, deployment.lease, actual, actual != CaptureEndReason.VOLUME_FINISHED, false);
            }
            runtime.board.clear();
        }
    }

    public static boolean idle() {
        return CloneRuntime.allCapturedIdle();
    }

    @Nullable
    static UUID presentationOwnerId(CloneRuntime runtime, UUID deploymentId) {
        if (runtime == null || deploymentId == null) {
            return null;
        }
        CapturedDeployment deployment = runtime.board.deployments.get(deploymentId);
        if (deployment == null || deployment.lease.released()
                || deployment.lease.sessionToken() != runtime.sessionToken()
                || !deploymentId.equals(deployment.lease.deploymentId())) {
            return null;
        }
        return deployment.lease.ownerId();
    }

    static void attach(CloneRuntime runtime, DeploymentLease lease, CloneRuntimeState state) {
        runtime.board.deployments.put(lease.deploymentId(), new CapturedDeployment(lease, state));
    }

    static boolean hasTicket(CloneRuntime runtime, UUID deploymentId) {
        return runtime.board.tickets.containsKey(deploymentId);
    }

    static void registerTicket(CloneRuntime runtime, CapturedResponseTicket ticket) {
        runtime.board.tickets.put(ticket.deploymentId(), ticket);
        runtime.board.ticketOrder.addLast(ticket.deploymentId());
    }

    static int detachedFor(CloneRuntime runtime, UUID ownerId) {
        if (runtime == null || ownerId == null) {
            return 0;
        }
        Integer current = runtime.board.detachedByOwner.get(ownerId);
        return current == null ? 0 : current;
    }

    static int detachedTotal(CloneRuntime runtime) {
        return runtime == null ? 0 : runtime.board.detachedUnits;
    }

    static void holdDetached(CloneRuntime runtime, @Nullable DeploymentLease lease) {
        if (runtime == null || lease == null || lease.released() || lease.phase() == DeploymentLease.Phase.BODY) {
            return;
        }
        CapturedDeployment deployment = runtime.board.deployments.get(lease.deploymentId());
        if (deployment == null || deployment.lease != lease || deployment.detachedHeld) {
            return;
        }
        deployment.detachedHeld = true;
        runtime.board.detachedUnits++;
        Integer current = runtime.board.detachedByOwner.get(lease.ownerId());
        runtime.board.detachedByOwner.put(lease.ownerId(), current == null ? 1 : current + 1);
    }

    static int dismissOwned(UUID ownerId) {
        if (ownerId == null) {
            return 0;
        }
        int dismissed = 0;
        for (CloneRuntime runtime : CloneRuntime.sameThreadRuntimes()) {
            List<CapturedDeployment> snapshot = new ArrayList<>(runtime.board.deployments.values());
            for (CapturedDeployment deployment : snapshot) {
                if (!ownerId.equals(deployment.lease.ownerId()) || deployment.lease.released()) {
                    continue;
                }
                boolean countCancel = deployment.lease.phase() != DeploymentLease.Phase.BODY
                        || deployment.body.leaseTransferred;
                release(runtime, deployment.lease, null, countCancel, true);
                dismissed++;
            }
        }
        return dismissed;
    }

    static boolean cancelActor(CloneRuntime runtime, UUID ownerId, UUID actorId) {
        if (runtime == null || ownerId == null || actorId == null) {
            return false;
        }
        CapturedDeployment found = null;
        for (CapturedDeployment deployment : runtime.board.deployments.values()) {
            if (actorId.equals(deployment.actorId) && ownerId.equals(deployment.lease.ownerId())) {
                found = deployment;
                break;
            }
        }
        if (found == null || found.lease.released()) {
            return false;
        }
        boolean countCancel = found.lease.phase() != DeploymentLease.Phase.BODY || found.body.leaseTransferred;
        release(runtime, found.lease, null, countCancel, true);
        return true;
    }

    static void release(CloneRuntime runtime, DeploymentLease lease, @Nullable CaptureEndReason reason,
                        boolean countCancel, boolean endBody) {
        if (runtime == null || lease == null || lease.sessionToken() != runtime.sessionToken() || lease.released()) {
            return;
        }
        if (!runtime.server().isSameThread()) {
            return;
        }
        CapturedDeployment deployment = runtime.board.deployments.get(lease.deploymentId());
        if (deployment == null || deployment.lease != lease) {
            return;
        }
        removeTicket(runtime, lease.deploymentId());
        if (!lease.release()) {
            return;
        }
        dropDetached(runtime, deployment);
        runtime.board.deployments.remove(lease.deploymentId());
        ClonePresentation.forget(lease.deploymentId());
        if (countCancel) {
            PerformanceRecorder.add(PerfCounter.CAPTURE_RESPONSE_CANCELS, 1L);
        }
        CloneRuntimeState body = deployment.body;
        if (!endBody || body == null || body.cleaning || body.life == CloneLifeState.ENDED
                || body.life == CloneLifeState.ENDING) {
            return;
        }
        runtime.endActor(body, bodyEndReason(reason));
    }

    private static boolean execute(CloneRuntime runtime, CapturedResponseTicket ticket) {
        CapturedDeployment deployment = runtime.board.deployments.get(ticket.deploymentId());
        if (deployment == null || deployment.lease.released() || deployment.lease.phase() != DeploymentLease.Phase.TICKET) {
            removeTicket(runtime, ticket.deploymentId());
            return false;
        }
        CaptureEndReason failure = validate(runtime, ticket);
        if (failure != null) {
            failOpen(runtime, deployment, ticket, failure);
            return false;
        }
        ActorPayloadAdapter adapter = ActorPayloadRegistry.find(ticket.payload().payloadTechniqueId());
        if (adapter == null || adapter.version() != ticket.payload().adapterVersion()) {
            failOpen(runtime, deployment, ticket, CaptureEndReason.ADAPTER_MISMATCH);
            return false;
        }
        ActorVolumeSpec volumeSpec;
        try {
            CapturedActionAuthority authority = new CapturedActionAuthority(ticket.sessionToken(), ticket.ownerId(),
                    ticket.rootEventId(), ticket.deploymentId(), ticket.actionToken(), ticket.payload().adapterVersion());
            volumeSpec = adapter.instantiate(ticket.payload(), ticket.actorFrame(), authority);
        } catch (RuntimeException exception) {
            LOGGER.error("水幻形体积没有生成 deployment={}", ticket.deploymentId(), exception);
            failOpen(runtime, deployment, ticket, CaptureEndReason.FAILED_SUBMIT);
            return false;
        }
        if (volumeSpec == null || !deployment.lease.transferToVolume()) {
            failOpen(runtime, deployment, ticket, CaptureEndReason.FAILED_SUBMIT);
            return false;
        }
        boolean submitted;
        try {
            submitted = ActorVolumeRuntime.submit(volumeSpec, deployment.lease);
        } catch (RuntimeException exception) {
            LOGGER.error("水幻形体积没有提交 deployment={}", ticket.deploymentId(), exception);
            submitted = false;
        }
        if (!submitted) {
            failOpen(runtime, deployment, ticket, CaptureEndReason.FAILED_SUBMIT);
            return false;
        }
        PerformanceRecorder.add(PerfCounter.CLONE_RESPONSE_COMMITS, 1L);
        ClonePresentation.emitVolume(runtime.server(), ticket, deployment.body.spec, volumeCenter(ticket),
                volumeSpec.volumeVisual());
        scatter(runtime, deployment, ticket);
        if (deployment.body.life == CloneLifeState.ACTIVE) {
            runtime.endActor(deployment.body, CloneEndReason.BUDGET_SPENT);
        }
        return true;
    }

    private static void failTicket(CloneRuntime runtime, CapturedResponseTicket ticket, CaptureEndReason reason) {
        CapturedDeployment deployment = runtime.board.deployments.get(ticket.deploymentId());
        if (deployment == null) {
            removeTicket(runtime, ticket.deploymentId());
            return;
        }
        failOpen(runtime, deployment, ticket, reason);
    }

    private static void failOpen(CloneRuntime runtime, CapturedDeployment deployment, CapturedResponseTicket ticket,
                                 CaptureEndReason reason) {
        scatter(runtime, deployment, ticket);
        tellImmediate(runtime, ticket);
        release(runtime, deployment.lease, reason, true, true);
    }

    private static void scatter(CloneRuntime runtime, CapturedDeployment deployment, CapturedResponseTicket ticket) {
        Vec3 feet = ticket.actorFrame().feetPosition();
        Vec3 look = ticket.actorFrame().lookUnit();
        CloneEntity actor = deployment.body.actor;
        if (actor != null && !actor.isRemoved()) {
            feet = actor.position();
        }
        if (deployment.body.frozenLook != null && CaptureVectors.unit(deployment.body.frozenLook)) {
            look = deployment.body.frozenLook;
        }
        ClonePresentation.emit(runtime.server(), deployment.body, feet, look, NoticePhase.SCATTER, VolumeVisualKind.NONE);
    }

    private static void tellImmediate(CloneRuntime runtime, CapturedResponseTicket ticket) {
        if (!ticket.manualFeedback()) {
            return;
        }
        ServerPlayer owner = runtime.server().getPlayerList().getPlayer(ticket.ownerId());
        if (owner == null || owner.hasDisconnected()) {
            return;
        }
        owner.sendSystemMessage(Component.literal(IMMEDIATE_MISS));
    }

    @Nullable
    private static CaptureEndReason validate(CloneRuntime runtime, CapturedResponseTicket ticket) {
        if (ticket.sessionToken() != runtime.sessionToken()) {
            return CaptureEndReason.FAILED_SUBMIT;
        }
        ServerPlayer owner = runtime.server().getPlayerList().getPlayer(ticket.ownerId());
        if (owner == null || owner.hasDisconnected()) {
            return CaptureEndReason.OWNER_LOGOUT;
        }
        if (!owner.isAlive() || owner.isRemoved()) {
            return CaptureEndReason.OWNER_DEATH;
        }
        if (!owner.level().dimension().equals(ticket.actorFrame().dimension())) {
            return CaptureEndReason.OWNER_DIMENSION;
        }
        ServerLevel level = runtime.server().getLevel(ticket.actorFrame().dimension());
        if (level == null) {
            return CaptureEndReason.WORLD_UNLOAD;
        }
        BlockPos eyeBlock = BlockPos.containing(ticket.actorFrame().eyeOrigin());
        if (!level.isLoaded(eyeBlock)) {
            return CaptureEndReason.AREA_UNLOADED;
        }
        CapturedAttackSnapshot payload = ticket.payload();
        if (!finitePositive(payload.depthBlocks()) || !finitePositive(payload.heightBlocks())
                || !finitePositive(payload.widthBlocks()) || payload.lifetimeTicks() <= 0
                || payload.configuredDamage() == null) {
            return CaptureEndReason.INVALID_GEOMETRY;
        }
        ActorPayloadAdapter adapter = ActorPayloadRegistry.find(payload.payloadTechniqueId());
        if (adapter == null || adapter.version() != payload.adapterVersion()) {
            return CaptureEndReason.ADAPTER_MISMATCH;
        }
        return null;
    }

    private static Vec3 volumeCenter(CapturedResponseTicket ticket) {
        Vec3 look = ticket.actorFrame().lookUnit();
        return ticket.actorFrame().eyeOrigin().add(look.scale(ticket.payload().depthBlocks() * 0.5D));
    }

    private static boolean finitePositive(double extent) {
        return Double.isFinite(extent) && extent > 0.0D;
    }

    private static void cancelMatching(CloneRuntime runtime, CaptureEndReason reason, DeploymentMatch match) {
        List<CapturedDeployment> snapshot = new ArrayList<>(runtime.board.deployments.values());
        boolean countCancel = reason != CaptureEndReason.VOLUME_FINISHED;
        for (CapturedDeployment deployment : snapshot) {
            if (match.matches(deployment) && !deployment.lease.released()) {
                release(runtime, deployment.lease, reason, countCancel, true);
            }
        }
    }

    private static boolean ticketInDimension(CloneRuntime runtime, CapturedDeployment deployment,
                                            ResourceKey<Level> dimension) {
        CapturedResponseTicket ticket = runtime.board.tickets.get(deployment.lease.deploymentId());
        return ticket != null && dimension.equals(ticket.actorFrame().dimension());
    }

    private static void removeTicket(CloneRuntime runtime, UUID deploymentId) {
        runtime.board.tickets.remove(deploymentId);
        runtime.board.ticketOrder.remove(deploymentId);
    }

    private static void dropDetached(CloneRuntime runtime, CapturedDeployment deployment) {
        if (!deployment.detachedHeld) {
            return;
        }
        deployment.detachedHeld = false;
        if (runtime.board.detachedUnits > 0) {
            runtime.board.detachedUnits--;
        }
        UUID ownerId = deployment.lease.ownerId();
        Integer current = runtime.board.detachedByOwner.get(ownerId);
        if (current == null) {
            return;
        }
        if (current <= 1) {
            runtime.board.detachedByOwner.remove(ownerId);
        } else {
            runtime.board.detachedByOwner.put(ownerId, current - 1);
        }
    }

    private static CloneEndReason bodyEndReason(@Nullable CaptureEndReason reason) {
        if (reason == null) {
            return CloneEndReason.DISMISS;
        }
        return switch (reason) {
            case OWNER_LOGOUT -> CloneEndReason.OWNER_LOGOUT;
            case OWNER_DEATH -> CloneEndReason.OWNER_DEATH;
            case OWNER_DIMENSION -> CloneEndReason.OWNER_DIMENSION;
            case AREA_UNLOADED -> CloneEndReason.UNLOAD;
            case WORLD_UNLOAD -> CloneEndReason.WORLD_UNLOAD;
            case SERVER_STOP -> CloneEndReason.SERVER_STOP;
            case TICKET_EXPIRED -> CloneEndReason.EXPIRE;
            default -> CloneEndReason.DISMISS;
        };
    }

    @FunctionalInterface
    private interface DeploymentMatch {
        boolean matches(CapturedDeployment deployment);
    }
}
