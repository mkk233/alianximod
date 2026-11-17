package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CloneRuntime {
    public static final int OWNER_ACTOR_CAP = 4;
    public static final int SERVER_ACTOR_CAP = 128;
    public static final int ACTOR_REQUEST_CAP = 1;
    public static final int SERVER_REQUEST_CAP = 128;
    public static final int COMMITS_PER_TICK = 32;
    public static final int REQUEST_LIFE_TICKS = 2;
    public static final int DECOY_LEASE_CAP = 8;
    public static final int DECOY_CANDIDATE_CAP = 32;
    public static final int DECOY_ATTRACT_CAP = 8;
    public static final int DECOY_QUERIES_PER_TICK = 4;
    public static final int PERIOD_TICKS = 20;
    public static final int OWNER_RECORD_CAP = 1024;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object DIRECTORY_GATE = new Object();
    private static final IdentityHashMap<MinecraftServer, CloneRuntime> DIRECTORY = new IdentityHashMap<>();

    private final MinecraftServer server;
    private final long sessionToken;
    private final LinkedHashMap<UUID, OwnerRecord> owners = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, CloneRuntimeState> actors = new LinkedHashMap<>();
    private final ArrayDeque<CloneActionRequest> queue = new ArrayDeque<>();

    private long monotonicTick;
    private long nextGeneration = 1L;
    private long nextSerial = 1L;
    private long nextToken = 1L;
    private int nextStagger;
    private int decoyCursor;
    private int serverReserved;
    private boolean accepting = true;

    private CloneRuntime(MinecraftServer server) {
        this.server = server;
        long token = ThreadLocalRandom.current().nextLong();
        this.sessionToken = token == 0L ? 1L : token;
    }

    public static void start(MinecraftServer server) {
        if (server == null || !server.isSameThread()) {
            return;
        }
        synchronized (DIRECTORY_GATE) {
            if (!DIRECTORY.containsKey(server)) {
                DIRECTORY.put(server, new CloneRuntime(server));
            }
        }
    }

    public static void stop(MinecraftServer server) {
        if (server == null || !server.isSameThread()) {
            return;
        }
        CloneRuntime runtime;
        synchronized (DIRECTORY_GATE) {
            runtime = DIRECTORY.get(server);
        }
        if (runtime == null) {
            return;
        }
        runtime.accepting = false;
        runtime.shutdown(CloneEndReason.SERVER_STOP);
        synchronized (DIRECTORY_GATE) {
            DIRECTORY.remove(server, runtime);
        }
    }

    @Nullable
    static MinecraftServer serverFor(long sessionToken) {
        synchronized (DIRECTORY_GATE) {
            for (CloneRuntime runtime : DIRECTORY.values()) {
                if (runtime.sessionToken == sessionToken && runtime.server.isSameThread()) {
                    return runtime.server;
                }
            }
        }
        return null;
    }

    @Nullable
    public static CloneRuntime get(MinecraftServer server) {
        if (server == null || !server.isSameThread()) {
            return null;
        }
        synchronized (DIRECTORY_GATE) {
            return DIRECTORY.get(server);
        }
    }

    public long tick() {
        return this.monotonicTick;
    }

    public long sessionToken() {
        return this.sessionToken;
    }

    public boolean accepting() {
        return this.accepting;
    }

    MinecraftServer server() {
        return this.server;
    }

    void advanceTick() {
        if (!this.accepting) {
            return;
        }
        if (this.monotonicTick == Long.MAX_VALUE) {
            this.accepting = false;
            this.shutdown(CloneEndReason.SERVER_STOP);
            return;
        }
        this.monotonicTick++;
    }

    void drain() {
        if (!this.accepting) {
            return;
        }
        if (this.actors.isEmpty() && this.queue.isEmpty() && this.owners.isEmpty()) {
            return;
        }
        long started = PerformanceRecorder.begin();
        try {
            this.expireActors();
            this.watchOwners();
            this.refreshCooldowns();
            this.pumpQueue();
            this.runDecoys();
            this.dropIdleOwners();
        } catch (RuntimeException exception) {
            LOGGER.error("分身运行态在 tick {} 失败", this.monotonicTick, exception);
        } finally {
            PerformanceRecorder.end(PerfMetric.CLONE_TICK, started);
        }
    }

    @Nullable
    CloneResults.RejectReason reserve(UUID ownerId) {
        OwnerRecord record = this.owners.get(ownerId);
        if (record != null && this.monotonicTick < record.spawnReadyTick) {
            return CloneResults.RejectReason.COOLDOWN;
        }
        boolean created = false;
        if (record == null) {
            if (this.owners.size() >= OWNER_RECORD_CAP) {
                return CloneResults.RejectReason.CAP_REACHED;
            }
            record = new OwnerRecord(ownerId);
            this.owners.put(ownerId, record);
            created = true;
        }
        if (record.actors.size() + record.reserved >= OWNER_ACTOR_CAP
                || this.actors.size() + this.serverReserved >= SERVER_ACTOR_CAP) {
            if (created) {
                this.owners.remove(ownerId);
            }
            return CloneResults.RejectReason.CAP_REACHED;
        }
        record.reserved++;
        this.serverReserved++;
        return null;
    }

    void releaseReserve(UUID ownerId) {
        OwnerRecord record = this.owners.get(ownerId);
        if (record != null && record.reserved > 0) {
            record.reserved--;
        }
        if (this.serverReserved > 0) {
            this.serverReserved--;
        }
        this.dropOwnerIfIdle(ownerId);
    }

    long allocateGeneration() {
        if (this.nextGeneration >= Long.MAX_VALUE - 1L) {
            return 0L;
        }
        long generation = this.nextGeneration;
        this.nextGeneration++;
        return generation;
    }

    long allocateSerial() {
        if (this.nextSerial >= Long.MAX_VALUE - 1L) {
            return 0L;
        }
        long serial = this.nextSerial;
        this.nextSerial++;
        return serial;
    }

    long allocateToken() {
        if (this.nextToken >= Long.MAX_VALUE - 1L) {
            return 0L;
        }
        long token = this.nextToken;
        this.nextToken++;
        return token;
    }

    void armSpawnCooldown(UUID ownerId, int cooldownTicks) {
        OwnerRecord record = this.owners.get(ownerId);
        if (record == null) {
            return;
        }
        long ready = this.monotonicTick + Math.max(0, cooldownTicks);
        if (ready < this.monotonicTick) {
            ready = Long.MAX_VALUE;
        }
        record.spawnReadyTick = ready;
    }

    int spawnCooldownLeft(UUID ownerId) {
        OwnerRecord record = this.owners.get(ownerId);
        if (record == null) {
            return 0;
        }
        return clampTicks(record.spawnReadyTick - this.monotonicTick);
    }

    int responseCooldownLeft(CloneRuntimeState state) {
        if (state.response != CloneResponseState.COOLDOWN) {
            return 0;
        }
        return clampTicks(state.cooldownReadyTick - this.monotonicTick);
    }

    @Nullable
    CloneRuntimeState publish(ServerPlayer owner, CloneEntity actor, CloneSpec spec, long generation) {
        OwnerRecord record = this.owners.get(owner.getUUID());
        if (record == null || actor == null || spec == null || generation <= 0L) {
            return null;
        }
        if (this.actors.containsKey(actor.getUUID()) || record.actors.containsKey(actor.getUUID())) {
            return null;
        }
        if (record.reserved > 0) {
            record.reserved--;
        }
        if (this.serverReserved > 0) {
            this.serverReserved--;
        }
        int stagger = this.nextStagger;
        if (this.nextStagger < Integer.MAX_VALUE) {
            this.nextStagger++;
        }
        ResourceKey<Level> dimension = owner.level().dimension();
        CloneRuntimeState state = new CloneRuntimeState(actor.getUUID(), owner.getUUID(), generation, spec,
                dimension, this.monotonicTick, stagger);
        state.actor = actor;
        record.actors.put(actor.getUUID(), state);
        this.actors.put(actor.getUUID(), state);
        actor.mirror(state);
        return state;
    }

    @Nullable
    CloneRuntimeState owned(UUID ownerId, UUID actorId) {
        OwnerRecord record = this.owners.get(ownerId);
        if (record == null || actorId == null) {
            return null;
        }
        return record.actors.get(actorId);
    }

    @Nullable
    CloneRuntimeState find(UUID actorId) {
        if (actorId == null) {
            return null;
        }
        return this.actors.get(actorId);
    }

    List<CloneRuntimeState> ownedActors(UUID ownerId) {
        OwnerRecord record = this.owners.get(ownerId);
        if (record == null) {
            return List.of();
        }
        return new ArrayList<>(record.actors.values());
    }

    int queueSize() {
        return this.queue.size();
    }

    List<CloneRuntimeState> ownedActorsIn(ResourceKey<Level> dimension) {
        List<CloneRuntimeState> found = new ArrayList<>();
        if (dimension == null) {
            return found;
        }
        for (CloneRuntimeState state : this.actors.values()) {
            if (dimension.equals(state.dimension)) {
                found.add(state);
            }
        }
        return found;
    }

    boolean enqueue(CloneActionRequest request) {
        if (this.queue.size() >= SERVER_REQUEST_CAP) {
            return false;
        }
        this.queue.addLast(request);
        return true;
    }

    void endActor(CloneRuntimeState state, CloneEndReason reason) {
        if (state == null || state.life == CloneLifeState.ENDED || state.cleaning) {
            return;
        }
        state.prepareCancelled = true;
        state.cleaning = true;
        state.life = CloneLifeState.ENDING;
        CloneEntity actor = state.actor;
        if (actor != null) {
            actor.mirror(state);
        }
        this.cancelPending(state);
        if (actor != null && actor.level() instanceof ServerLevel level) {
            CloneDecoyPolicy.clear(level, actor, state.leases);
        } else {
            state.leases.clear();
        }
        OwnerRecord record = this.owners.get(state.ownerId);
        if (record != null) {
            record.actors.remove(state.actorId);
        }
        this.actors.remove(state.actorId);
        if (actor != null && !actor.isRemoved()) {
            actor.discard();
        }
        state.actor = null;
        state.life = CloneLifeState.ENDED;
        if (!state.endCounted) {
            state.endCounted = true;
            PerformanceRecorder.add(PerfCounter.CLONE_ENDS, 1L);
        }
        this.dropOwnerIfIdle(state.ownerId);
        if (reason == null) {
            LOGGER.error("分身 {} 结束时没有原因", state.actorId);
        }
    }

    private void cancelPending(CloneRuntimeState state) {
        CloneActionRequest pending = state.pending;
        if (pending == null || state.executing || state.commitLine) {
            return;
        }
        this.queue.removeIf(item -> item.requestSerial() == pending.requestSerial());
        state.pending = null;
        if (state.life == CloneLifeState.ACTIVE && state.response == CloneResponseState.RESERVED) {
            state.response = CloneResponseState.READY;
        }
        if (state.noteRejection(pending.requestSerial())) {
            PerformanceRecorder.add(PerfCounter.CLONE_RESPONSE_REJECTIONS, 1L);
        }
    }

    private void expireActors() {
        List<CloneRuntimeState> snapshot = new ArrayList<>(this.actors.values());
        for (CloneRuntimeState state : snapshot) {
            if (state.life != CloneLifeState.ACTIVE || !state.spec.lifetime().expires()) {
                continue;
            }
            long due = state.spawnedTick + state.spec.lifetime().spanTicks();
            if (due < state.spawnedTick) {
                due = Long.MAX_VALUE;
            }
            if (this.monotonicTick >= due) {
                this.endActor(state, CloneEndReason.EXPIRE);
            }
        }
    }

    private void watchOwners() {
        List<CloneRuntimeState> snapshot = new ArrayList<>(this.actors.values());
        for (CloneRuntimeState state : snapshot) {
            if (state.life != CloneLifeState.ACTIVE) {
                continue;
            }
            if (Math.floorMod(state.staggerSlot, PERIOD_TICKS) != Math.floorMod(this.monotonicTick, PERIOD_TICKS)) {
                continue;
            }
            ServerPlayer owner = this.server.getPlayerList().getPlayer(state.ownerId);
            if (owner == null || owner.hasDisconnected()) {
                this.endActor(state, CloneEndReason.OWNER_LOGOUT);
                continue;
            }
            if (!owner.isAlive() || owner.isRemoved()) {
                this.endActor(state, CloneEndReason.OWNER_DEATH);
                continue;
            }
            if (!owner.level().dimension().equals(state.dimension)) {
                this.endActor(state, CloneEndReason.OWNER_DIMENSION);
                continue;
            }
            CloneEntity actor = state.actor;
            if (actor == null || actor.isRemoved() || !actor.isAlive()) {
                this.endActor(state, CloneEndReason.UNLOAD);
            }
        }
    }

    private void refreshCooldowns() {
        for (CloneRuntimeState state : this.actors.values()) {
            if (state.life == CloneLifeState.ACTIVE
                    && state.response == CloneResponseState.COOLDOWN
                    && this.monotonicTick >= state.cooldownReadyTick) {
                state.response = CloneResponseState.READY;
                if (state.actor != null) {
                    state.actor.mirror(state);
                }
            }
        }
    }

    private void pumpQueue() {
        int commits = 0;
        while (!this.queue.isEmpty() && commits < COMMITS_PER_TICK) {
            CloneActionRequest request = this.queue.peekFirst();
            if (this.monotonicTick > request.expireTick()) {
                this.queue.pollFirst();
                CloneService.cancelQueued(this, request);
                continue;
            }
            if (request.executeTick() > this.monotonicTick) {
                break;
            }
            this.queue.pollFirst();
            if (CloneService.perform(this, request)) {
                commits++;
            }
        }
    }

    private void runDecoys() {
        int size = this.actors.size();
        if (size == 0) {
            return;
        }
        List<CloneRuntimeState> ordered = new ArrayList<>(this.actors.values());
        int walked = 0;
        int performed = 0;
        int cursor = this.decoyCursor;
        if (cursor < 0 || cursor >= ordered.size()) {
            cursor = 0;
        }
        while (walked < ordered.size() && performed < DECOY_QUERIES_PER_TICK) {
            CloneRuntimeState state = ordered.get(cursor);
            cursor++;
            if (cursor >= ordered.size()) {
                cursor = 0;
            }
            walked++;
            if (state.life != CloneLifeState.ACTIVE || state.spec.decoyRadiusBlocks() <= 0.0D) {
                continue;
            }
            if (Math.floorMod(state.staggerSlot, PERIOD_TICKS) != Math.floorMod(this.monotonicTick, PERIOD_TICKS)) {
                continue;
            }
            CloneEntity actor = state.actor;
            if (actor == null || actor.isRemoved() || !(actor.level() instanceof ServerLevel level)) {
                continue;
            }
            try {
                if (CloneDecoyPolicy.pass(level, actor, state)) {
                    performed++;
                }
            } catch (RuntimeException exception) {
                LOGGER.error("分身 {} 诱敌失败", state.actorId, exception);
            }
        }
        this.decoyCursor = cursor;
    }

    private void dropIdleOwners() {
        List<UUID> ownerIds = new ArrayList<>(this.owners.keySet());
        for (UUID ownerId : ownerIds) {
            this.dropOwnerIfIdle(ownerId);
        }
    }

    private void dropOwnerIfIdle(UUID ownerId) {
        OwnerRecord record = this.owners.get(ownerId);
        if (record == null) {
            return;
        }
        if (record.actors.isEmpty() && record.reserved == 0 && this.monotonicTick >= record.spawnReadyTick) {
            this.owners.remove(ownerId);
        }
    }

    private void shutdown(CloneEndReason reason) {
        List<CloneRuntimeState> snapshot = new ArrayList<>(this.actors.values());
        for (CloneRuntimeState state : snapshot) {
            this.endActor(state, reason);
        }
        this.queue.clear();
        this.owners.clear();
        this.actors.clear();
        this.serverReserved = 0;
    }

    private static int clampTicks(long remaining) {
        if (remaining <= 0L) {
            return 0;
        }
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }

    private static final class OwnerRecord {
        final UUID ownerId;
        final LinkedHashMap<UUID, CloneRuntimeState> actors = new LinkedHashMap<>();
        int reserved;
        long spawnReadyTick;

        private OwnerRecord(UUID ownerId) {
            this.ownerId = ownerId;
        }
    }
}
