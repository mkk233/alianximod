package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CaptureEndReason;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneEntity;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneRuntime;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneService;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneSourceScope;
import com.bslx.XueXiMod.shu.technique.release.clone.core.DeploymentLease;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端静止体积。只由分身运行时调用，自己不注册服务器 tick。
 * 空表立即返回。结束只交还同一张部署租约。
 */
public final class ActorVolumeRuntime {
    private static final int ATTEMPT_LIMIT = 64;
    private static final int CHUNK_SPAN_LIMIT = 4096;
    private static final double PATH_LIMIT = 1024.0d;
    private static final double PATH_STRIDE = 8.0d;
    private static final double SURFACE_SLOP = 1.0E-4d;
    private static final Map<UUID, ActiveVolume> ACTIVE = new LinkedHashMap<>();
    private static volatile Thread boundThread;
    private static int tickDepth;
    private static boolean closing;

    private ActorVolumeRuntime() {
    }

    public static boolean submit(@Nullable ActorVolumeSpec spec, @Nullable DeploymentLease lease) {
        if (spec == null || lease == null || !ActorVolumeGeometry.usable(spec)) {
            return false;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (!onServer(server)) {
            return false;
        }
        CloneRuntime runtime = CloneRuntime.get(server);
        if (runtime == null || runtime.sessionToken() != lease.sessionToken()
                || runtime.sessionToken() != spec.sessionToken()) {
            return false;
        }
        if (lease.released() || lease.phase() != DeploymentLease.Phase.VOLUME) {
            return false;
        }
        if (!lease.deploymentId().equals(spec.deploymentId()) || !lease.ownerId().equals(spec.ownerId())) {
            return false;
        }
        if (ACTIVE.containsKey(spec.deploymentId())) {
            return false;
        }
        ACTIVE.put(spec.deploymentId(), new ActiveVolume(spec, lease));
        return true;
    }

    public static void tick(@Nullable MinecraftServer server) {
        if (!onServer(server) || ACTIVE.isEmpty() || tickDepth > 0) {
            return;
        }
        tickDepth++;
        try {
            List<UUID> deploymentIds = new ArrayList<>(ACTIVE.keySet());
            for (UUID deploymentId : deploymentIds) {
                ActiveVolume active = ACTIVE.get(deploymentId);
                if (active == null) {
                    continue;
                }
                if (released(active.lease)) {
                    ACTIVE.remove(deploymentId);
                    continue;
                }
                long started = PerformanceRecorder.begin();
                VolumeStep step;
                try {
                    step = evaluate(server, active);
                } catch (RuntimeException failure) {
                    step = VolumeStep.end(CaptureEndReason.FAILED_SUBMIT);
                } finally {
                    PerformanceRecorder.end(PerfMetric.ACTOR_VOLUME_UPDATE, started);
                }
                PerformanceRecorder.add(PerfCounter.ACTOR_VOLUME_UPDATES, 1L);
                if (!step.retain()) {
                    ACTIVE.remove(deploymentId);
                    if (step.reason() != null && !active.lease.released()) {
                        CloneService.finishDeployment(active.lease, step.reason());
                    }
                }
            }
        } finally {
            tickDepth--;
        }
    }

    public static void shutdown(@Nullable CaptureEndReason reason) {
        if (!onBoundThread() || closing) {
            return;
        }
        closing = true;
        try {
            List<ActiveVolume> drained = new ArrayList<>(ACTIVE.values());
            ACTIVE.clear();
            CaptureEndReason endReason = reason == null ? CaptureEndReason.SERVER_STOP : reason;
            for (ActiveVolume active : drained) {
                if (!active.lease.released()) {
                    CloneService.finishDeployment(active.lease, endReason);
                }
            }
        } finally {
            closing = false;
        }
    }

    public static boolean idle() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.isSameThread()) {
            return false;
        }
        return ACTIVE.isEmpty();
    }

    private static VolumeStep evaluate(MinecraftServer server, ActiveVolume active) {
        ActorVolumeSpec spec = active.spec;
        DeploymentLease lease = active.lease;
        if (released(lease)) {
            return VolumeStep.dropReleased();
        }
        if (lease.phase() != DeploymentLease.Phase.VOLUME
                || !lease.deploymentId().equals(spec.deploymentId())
                || !lease.ownerId().equals(spec.ownerId())
                || lease.sessionToken() != spec.sessionToken()) {
            return VolumeStep.end(CaptureEndReason.INVALID_GEOMETRY);
        }
        CloneRuntime runtime = CloneRuntime.get(server);
        if (runtime == null || runtime.sessionToken() != spec.sessionToken()) {
            return VolumeStep.end(CaptureEndReason.SERVER_STOP);
        }
        ActorPayloadAdapter adapter = ActorPayloadRegistry.find(TideSeverActorAdapter.TECHNIQUE_ID);
        if (adapter == null || adapter.version() != spec.adapterVersion()) {
            return VolumeStep.end(CaptureEndReason.ADAPTER_MISMATCH);
        }
        if (!ActorVolumeGeometry.usable(spec)) {
            return VolumeStep.end(CaptureEndReason.INVALID_GEOMETRY);
        }
        if (active.elapsedTicks >= spec.lifetimeTicks()) {
            return VolumeStep.end(CaptureEndReason.VOLUME_FINISHED);
        }
        ServerLevel level = server.getLevel(spec.dimension());
        if (level == null) {
            return VolumeStep.end(CaptureEndReason.WORLD_UNLOAD);
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(spec.ownerId());
        if (owner == null) {
            return VolumeStep.end(CaptureEndReason.OWNER_LOGOUT);
        }
        if (!owner.isAlive() || owner.isDeadOrDying()) {
            return VolumeStep.end(CaptureEndReason.OWNER_DEATH);
        }
        if (owner.level() != level) {
            return VolumeStep.end(CaptureEndReason.OWNER_DIMENSION);
        }
        AABB bounds = ActorVolumeGeometry.worldBounds(spec);
        if (bounds == null) {
            return VolumeStep.end(CaptureEndReason.INVALID_GEOMETRY);
        }
        if (!regionLoaded(level, bounds)) {
            return VolumeStep.end(CaptureEndReason.AREA_UNLOADED);
        }
        VolumeStep settled = settle(server, level, owner, active, spec, bounds);
        if (!settled.retain()) {
            return settled;
        }
        if (released(active.lease)) {
            return VolumeStep.dropReleased();
        }
        active.elapsedTicks++;
        if (active.elapsedTicks >= spec.lifetimeTicks()) {
            return VolumeStep.end(CaptureEndReason.VOLUME_FINISHED);
        }
        return VolumeStep.keep();
    }

    private static VolumeStep settle(MinecraftServer server, ServerLevel level, ServerPlayer owner,
                                     ActiveVolume active, ActorVolumeSpec spec, AABB bounds) {
        List<LivingEntity> candidates = LivingCandidateQueries.entityQuery(level, bounds, ActorVolumeRuntime::coarseTarget);
        if (candidates == null) {
            return VolumeStep.end(CaptureEndReason.INVALID_GEOMETRY);
        }
        for (LivingEntity target : candidates) {
            if (released(active.lease)) {
                return VolumeStep.dropReleased();
            }
            if (!owner.isAlive() || owner.isDeadOrDying()) {
                return VolumeStep.end(CaptureEndReason.OWNER_DEATH);
            }
            if (owner.level() != level) {
                return VolumeStep.end(CaptureEndReason.OWNER_DIMENSION);
            }
            UUID targetId = target.getUUID();
            if (targetId == null || active.attemptedIds.contains(targetId)) {
                continue;
            }
            if (active.attemptedIds.size() >= ATTEMPT_LIMIT) {
                break;
            }
            if (target instanceof CloneEntity || targetId.equals(owner.getUUID())) {
                continue;
            }
            AABB targetBounds = target.getBoundingBox();
            if (!ActorVolumeGeometry.intersectsObb(spec, targetBounds)) {
                continue;
            }
            Vec3 aimPoint = ActorVolumeGeometry.overlapPoint(spec, targetBounds);
            if (aimPoint == null || !pathLoaded(level, spec.eyeOrigin(), aimPoint)) {
                continue;
            }
            if (solidBetween(level, owner, spec.eyeOrigin(), aimPoint)) {
                continue;
            }
            if (!DamagePermission.allows(owner, target) || CloneEntity.rejectsPulseTarget(owner, target)
                    || target instanceof CloneEntity) {
                continue;
            }
            active.attemptedIds.add(targetId);
            CloneSourceScope scope = CloneSourceScope.open(
                    server, CloneSourceScope.Origin.CLONE_RESPONSE, spec.rootEventId(), 1);
            try {
                TechniqueHits.classifyRespecting(
                        target, owner, spec.configuredDamage().amount(), TideMarkSuccess.NONE);
            } finally {
                scope.close();
            }
        }
        return VolumeStep.keep();
    }

    private static boolean coarseTarget(@Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && !target.isSpectator()
                && !target.isRemoved()
                && !(target instanceof CloneEntity);
    }

    private static boolean regionLoaded(ServerLevel level, AABB bounds) {
        if (!insideWorld(bounds.minX) || !insideWorld(bounds.maxX)
                || !insideWorld(bounds.minY) || !insideWorld(bounds.maxY)
                || !insideWorld(bounds.minZ) || !insideWorld(bounds.maxZ)) {
            return false;
        }
        int startChunkX = chunkColumn(bounds.minX, false);
        int startChunkZ = chunkColumn(bounds.minZ, false);
        int endChunkX = chunkColumn(bounds.maxX, true);
        int endChunkZ = chunkColumn(bounds.maxZ, true);
        if (endChunkX < startChunkX) {
            endChunkX = startChunkX;
        }
        if (endChunkZ < startChunkZ) {
            endChunkZ = startChunkZ;
        }
        long spanCount = (long) (endChunkX - startChunkX + 1) * (long) (endChunkZ - startChunkZ + 1);
        if (spanCount <= 0L || spanCount > CHUNK_SPAN_LIMIT) {
            return false;
        }
        for (int columnX = startChunkX; columnX <= endChunkX; columnX++) {
            for (int columnZ = startChunkZ; columnZ <= endChunkZ; columnZ++) {
                if (!level.hasChunk(columnX, columnZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean insideWorld(double coordinate) {
        return Double.isFinite(coordinate) && Math.abs(coordinate) <= 3.0E7d;
    }

    private static int chunkColumn(double coordinate, boolean upperEdge) {
        double sample = upperEdge ? coordinate - 1.0E-7d : coordinate;
        return (int) Math.floor(sample / 16.0d);
    }

    private static boolean pathLoaded(ServerLevel level, Vec3 eyeOrigin, Vec3 aimPoint) {
        double separation = eyeOrigin.distanceTo(aimPoint);
        if (!Double.isFinite(separation)) {
            return false;
        }
        if (separation <= SURFACE_SLOP) {
            return level.hasChunkAt(BlockPos.containing(eyeOrigin));
        }
        if (separation > PATH_LIMIT) {
            return false;
        }
        int steps = Math.max(1, (int) Math.ceil(separation / PATH_STRIDE));
        for (int step = 0; step <= steps; step++) {
            double blend = step / (double) steps;
            Vec3 sample = new Vec3(
                    eyeOrigin.x + (aimPoint.x - eyeOrigin.x) * blend,
                    eyeOrigin.y + (aimPoint.y - eyeOrigin.y) * blend,
                    eyeOrigin.z + (aimPoint.z - eyeOrigin.z) * blend);
            if (!level.hasChunkAt(BlockPos.containing(sample))) {
                return false;
            }
        }
        return true;
    }

    private static boolean solidBetween(ServerLevel level, ServerPlayer owner, Vec3 eyeOrigin, Vec3 aimPoint) {
        double aimDistance = eyeOrigin.distanceTo(aimPoint);
        if (!Double.isFinite(aimDistance) || aimDistance <= SURFACE_SLOP) {
            return false;
        }
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyeOrigin, aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        double blockDistance = eyeOrigin.distanceTo(blockHit.getLocation());
        return Double.isFinite(blockDistance) && blockDistance + SURFACE_SLOP < aimDistance;
    }

    private static boolean released(DeploymentLease lease) {
        return lease.released() || lease.phase() == DeploymentLease.Phase.RELEASED;
    }

    private static boolean onServer(@Nullable MinecraftServer server) {
        if (server != null && server.isSameThread()) {
            boundThread = Thread.currentThread();
            return true;
        }
        return false;
    }

    private static boolean onBoundThread() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return onServer(server);
        }
        return Thread.currentThread() == boundThread;
    }

    private static final class ActiveVolume {
        private final ActorVolumeSpec spec;
        private final DeploymentLease lease;
        private final Set<UUID> attemptedIds = new LinkedHashSet<>();
        private int elapsedTicks;

        private ActiveVolume(ActorVolumeSpec spec, DeploymentLease lease) {
            this.spec = spec;
            this.lease = lease;
        }
    }

    private record VolumeStep(boolean retain, @Nullable CaptureEndReason reason) {
        private VolumeStep {
            if (retain && reason != null) {
                throw new IllegalArgumentException("仍保留的体积不能带结束原因");
            }
        }

        private static VolumeStep keep() {
            return new VolumeStep(true, null);
        }

        private static VolumeStep dropReleased() {
            return new VolumeStep(false, null);
        }

        private static VolumeStep end(CaptureEndReason reason) {
            return new VolumeStep(false, Objects.requireNonNull(reason, "reason"));
        }
    }
}
