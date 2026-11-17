package com.bslx.XueXiMod.shu.technique.query;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 同一服务端 tick、同一世界里可再过滤的 LivingEntity 空间候选。
 * 只保存原版顺序的空间候选，不保存谓词结果、锁定选择或伤害结论。
 * 钩子未齐、线程或服务器不符、查询过大时直接走原版查询。
 */
public final class LivingCandidateQueries {
    public static final int ENTRY_LIMIT = 4;
    public static final int CANDIDATE_LIMIT = 256;
    public static final double EDGE_LIMIT = 48.0D;
    public static final double MARGIN_BLOCKS = 8.0D;

    private static final int HOOK_ENTITY = 1;
    private static final int HOOK_SECTION = 2;
    private static final int HOOKS_READY = HOOK_ENTITY | HOOK_SECTION;
    private static final Predicate<LivingEntity> spatialPredicate = living -> true;

    private static final AtomicInteger hookMask = new AtomicInteger();
    private static final AtomicInteger revision = new AtomicInteger();
    private static final Map<ServerLevel, LevelStore> stores = new IdentityHashMap<>();
    private static final ThreadLocal<MoveStack> moveStack = ThreadLocal.withInitial(MoveStack::new);

    private static volatile boolean discardPending;
    private static MinecraftServer boundServer;
    private static Thread boundThread;
    private static int openTick = Integer.MIN_VALUE;

    private LivingCandidateQueries() {
    }

    public static void markEntityHooks() {
        hookMask.updateAndGet(current -> current | HOOK_ENTITY);
    }

    public static void markSectionHooks() {
        hookMask.updateAndGet(current -> current | HOOK_SECTION);
    }

    public static void bindServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        dropAll(true);
        discardPending = false;
        revision.incrementAndGet();
        Thread running = server.getRunningThread();
        boundThread = running != null ? running : Thread.currentThread();
        boundServer = server;
    }

    public static void releaseServer(MinecraftServer server) {
        if (boundServer != null && server != boundServer) {
            return;
        }
        boundServer = null;
        boundThread = null;
        revision.incrementAndGet();
        dropAll(true);
        discardPending = false;
    }

    public static void expireTick(MinecraftServer server) {
        if (server == null || server != boundServer || Thread.currentThread() != boundThread) {
            return;
        }
        drainDiscard();
        openTick = server.getTickCount();
        revision.incrementAndGet();
        stores.clear();
    }

    private static void alignTick(ServerLevel serverLevel) {
        int tick = serverLevel.getServer().getTickCount();
        if (tick == openTick) {
            return;
        }
        openTick = tick;
        revision.incrementAndGet();
        stores.clear();
    }

    public static void unloadLevel(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (boundServer == null || serverLevel.getServer() != boundServer) {
            return;
        }
        revision.incrementAndGet();
        if (Thread.currentThread() != boundThread) {
            discardPending = true;
            return;
        }
        dropOne(serverLevel, true);
    }

    public static void beginPositionChange(Entity entity, double nextX, double nextY, double nextZ) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        boolean changed = entity.getX() != nextX || entity.getY() != nextY || entity.getZ() != nextZ;
        moveStack.get().push(changed);
        if (changed) {
            invalidate(entity.level());
        }
    }

    public static void endPositionChange(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        if (moveStack.get().pop()) {
            invalidate(entity.level());
        }
    }

    public static void invalidateLiving(Object stored) {
        if (stored instanceof LivingEntity living) {
            invalidate(living.level());
        }
    }

    public static void invalidateSection(EntitySection<?> section) {
        if (section == null) {
            return;
        }
        try (Stream<?> stream = section.getEntities()) {
            Iterator<?> iterator = stream.iterator();
            while (iterator.hasNext()) {
                Object stored = iterator.next();
                if (stored instanceof LivingEntity living) {
                    invalidate(living.level());
                    return;
                }
            }
        }
    }

    public static void invalidate(Level level) {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (boundServer == null || serverLevel.getServer() != boundServer) {
            return;
        }
        revision.incrementAndGet();
        if (Thread.currentThread() != boundThread) {
            discardPending = true;
            return;
        }
        dropOne(serverLevel, true);
    }

    public static List<LivingEntity> entityQuery(Level level, AABB box, Predicate<? super LivingEntity> predicate) {
        return lookup(level, box, predicate, false);
    }

    public static List<LivingEntity> targetQuery(Level level, AABB box, Predicate<? super LivingEntity> predicate) {
        return lookup(level, box, predicate, true);
    }

    private static List<LivingEntity> lookup(Level level, AABB box, Predicate<? super LivingEntity> predicate, boolean targetKind) {
        long lookupStart = PerformanceRecorder.begin();
        int stage = 0;
        try {
            PerformanceRecorder.add(PerfCounter.CANDIDATE_QUERY_REQUESTS, 1L);
            if (Thread.currentThread() == boundThread) {
                drainDiscard();
            }
            if (!reusable(level, box)) {
                return finish(readWorld(level, box, predicate, targetKind), PerfCounter.CANDIDATE_QUERY_BYPASSES, targetKind);
            }
            ServerLevel serverLevel = (ServerLevel) level;
            alignTick(serverLevel);
            LevelStore store = stores.get(serverLevel);
            if (store != null) {
                StoredRegion region = store.find(box);
                if (region != null && region.denial) {
                    return finish(readWorld(level, box, predicate, targetKind), PerfCounter.CANDIDATE_QUERY_BYPASSES, targetKind);
                }
                if (region != null) {
                    stage = 1;
                    List<LivingEntity> filtered = select(region.ordered, box, predicate);
                    stage = 0;
                    return finish(filtered, PerfCounter.CANDIDATE_QUERY_REUSES, targetKind);
                }
                if (store.entries.size() >= ENTRY_LIMIT) {
                    return finish(readWorld(level, box, predicate, targetKind), PerfCounter.CANDIDATE_QUERY_BYPASSES, targetKind);
                }
            }
            AABB coverage = coverageFor(box);
            int seen = revision.get();
            List<LivingEntity> rawCandidates = readWorld(level, coverage, spatialPredicate, targetKind);
            if (seen != revision.get()) {
                return finish(readWorld(level, box, predicate, targetKind), PerfCounter.CANDIDATE_QUERY_BYPASSES, targetKind);
            }
            if (rawCandidates.size() > CANDIDATE_LIMIT) {
                rememberDenial(serverLevel, coverage);
                return finish(select(rawCandidates, box, predicate), PerfCounter.CANDIDATE_QUERY_BYPASSES, targetKind);
            }
            List<LivingEntity> frozen = List.copyOf(rawCandidates);
            if (seen != revision.get()) {
                return finish(readWorld(level, box, predicate, targetKind), PerfCounter.CANDIDATE_QUERY_BYPASSES, targetKind);
            }
            if (!rememberRegion(serverLevel, coverage, frozen)) {
                return finish(select(frozen, box, predicate), PerfCounter.CANDIDATE_QUERY_BYPASSES, targetKind);
            }
            stage = 2;
            List<LivingEntity> filtered = select(frozen, box, predicate);
            stage = 0;
            return finish(filtered, PerfCounter.CANDIDATE_QUERY_BUILDS, targetKind);
        } catch (Throwable thrown) {
            if (stage == 1) {
                PerformanceRecorder.add(PerfCounter.CANDIDATE_QUERY_REUSES, 1L);
            } else if (stage == 2) {
                PerformanceRecorder.add(PerfCounter.CANDIDATE_QUERY_BUILDS, 1L);
            } else {
                PerformanceRecorder.add(PerfCounter.CANDIDATE_QUERY_BYPASSES, 1L);
            }
            throw thrown;
        } finally {
            PerformanceRecorder.end(PerfMetric.CANDIDATE_QUERY_LOOKUP, lookupStart);
        }
    }

    private static boolean reusable(Level level, AABB box) {
        if (hookMask.get() != HOOKS_READY || boundServer == null || Thread.currentThread() != boundThread) {
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getServer() != boundServer) {
            return false;
        }
        return finiteBox(box) && longestEdge(box) <= EDGE_LIMIT;
    }

    private static boolean finiteBox(AABB box) {
        if (box == null) {
            return false;
        }
        return Double.isFinite(box.minX) && Double.isFinite(box.minY) && Double.isFinite(box.minZ)
                && Double.isFinite(box.maxX) && Double.isFinite(box.maxY) && Double.isFinite(box.maxZ)
                && box.minX <= box.maxX && box.minY <= box.maxY && box.minZ <= box.maxZ;
    }

    private static double longestEdge(AABB box) {
        double spanX = box.maxX - box.minX;
        double spanY = box.maxY - box.minY;
        double spanZ = box.maxZ - box.minZ;
        return Math.max(spanX, Math.max(spanY, spanZ));
    }

    private static AABB coverageFor(AABB box) {
        AABB expanded = box.inflate(MARGIN_BLOCKS);
        if (longestEdge(expanded) <= EDGE_LIMIT) {
            return expanded;
        }
        return box;
    }

    private static boolean covers(AABB coverage, AABB box) {
        return box.minX >= coverage.minX && box.minY >= coverage.minY && box.minZ >= coverage.minZ
                && box.maxX <= coverage.maxX && box.maxY <= coverage.maxY && box.maxZ <= coverage.maxZ;
    }

    private static List<LivingEntity> select(List<LivingEntity> ordered, AABB box, Predicate<? super LivingEntity> predicate) {
        int lowX = SectionPos.posToSectionCoord(box.minX - 2.0D);
        int lowY = SectionPos.posToSectionCoord(box.minY - 4.0D);
        int lowZ = SectionPos.posToSectionCoord(box.minZ - 2.0D);
        int highX = SectionPos.posToSectionCoord(box.maxX + 2.0D);
        int highY = SectionPos.posToSectionCoord(box.maxY);
        int highZ = SectionPos.posToSectionCoord(box.maxZ + 2.0D);
        ArrayList<LivingEntity> accepted = new ArrayList<>();
        for (LivingEntity candidate : ordered) {
            if (!insideSections(candidate, lowX, lowY, lowZ, highX, highY, highZ)) {
                continue;
            }
            if (!candidate.getBoundingBox().intersects(box)) {
                continue;
            }
            if (!predicate.test(candidate)) {
                continue;
            }
            accepted.add(candidate);
        }
        return accepted;
    }

    private static boolean insideSections(Entity entity, int lowX, int lowY, int lowZ, int highX, int highY, int highZ) {
        BlockPos feet = entity.blockPosition();
        int sectionX = SectionPos.blockToSectionCoord(feet.getX());
        int sectionY = SectionPos.blockToSectionCoord(feet.getY());
        int sectionZ = SectionPos.blockToSectionCoord(feet.getZ());
        return sectionX >= lowX && sectionX <= highX
                && sectionY >= lowY && sectionY <= highY
                && sectionZ >= lowZ && sectionZ <= highZ;
    }

    private static List<LivingEntity> readWorld(Level level, AABB box, Predicate<? super LivingEntity> predicate, boolean targetKind) {
        long started = PerformanceRecorder.begin();
        PerfMetric metric = targetKind ? PerfMetric.TARGET_QUERY : PerfMetric.ENTITY_QUERY;
        PerfCounter calls = targetKind ? PerfCounter.TARGET_QUERY_CALLS : PerfCounter.ENTITY_QUERY_CALLS;
        try {
            List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box, predicate);
            PerformanceRecorder.add(calls, 1L);
            return found;
        } finally {
            PerformanceRecorder.end(metric, started);
        }
    }

    private static List<LivingEntity> finish(List<LivingEntity> listed, PerfCounter terminal, boolean targetKind) {
        PerformanceRecorder.add(terminal, 1L);
        PerfCounter candidates = targetKind ? PerfCounter.TARGET_CANDIDATES : PerfCounter.ENTITY_CANDIDATES;
        PerformanceRecorder.add(candidates, listed.size());
        return listed;
    }

    private static void rememberDenial(ServerLevel serverLevel, AABB coverage) {
        LevelStore store = storeFor(serverLevel);
        if (store.entries.size() >= ENTRY_LIMIT) {
            return;
        }
        store.entries.add(new StoredRegion(coverage, List.of(), true));
    }

    private static boolean rememberRegion(ServerLevel serverLevel, AABB coverage, List<LivingEntity> frozen) {
        LevelStore store = storeFor(serverLevel);
        if (store.entries.size() >= ENTRY_LIMIT) {
            return false;
        }
        store.entries.add(new StoredRegion(coverage, frozen, false));
        return true;
    }

    private static LevelStore storeFor(ServerLevel serverLevel) {
        LevelStore store = stores.get(serverLevel);
        if (store == null) {
            store = new LevelStore();
            stores.put(serverLevel, store);
        }
        return store;
    }

    private static void dropOne(ServerLevel serverLevel, boolean countInvalidation) {
        LevelStore store = stores.remove(serverLevel);
        if (store == null || store.entries.isEmpty()) {
            return;
        }
        store.entries.clear();
        if (countInvalidation) {
            PerformanceRecorder.add(PerfCounter.CANDIDATE_QUERY_INVALIDATIONS, 1L);
        }
    }

    private static void dropAll(boolean countInvalidation) {
        if (countInvalidation) {
            for (LevelStore store : stores.values()) {
                if (store != null && !store.entries.isEmpty()) {
                    PerformanceRecorder.add(PerfCounter.CANDIDATE_QUERY_INVALIDATIONS, 1L);
                }
            }
        }
        stores.clear();
    }

    private static void drainDiscard() {
        if (!discardPending) {
            return;
        }
        discardPending = false;
        dropAll(true);
    }

    private static final class LevelStore {
        private final ArrayList<StoredRegion> entries = new ArrayList<>();

        private StoredRegion find(AABB box) {
            StoredRegion denied = null;
            for (int slot = 0; slot < this.entries.size(); slot++) {
                StoredRegion region = this.entries.get(slot);
                if (!covers(region.area, box)) {
                    continue;
                }
                if (!region.denial) {
                    return region;
                }
                if (denied == null) {
                    denied = region;
                }
            }
            return denied;
        }
    }

    private static final class StoredRegion {
        private final AABB area;
        private final List<LivingEntity> ordered;
        private final boolean denial;

        private StoredRegion(AABB area, List<LivingEntity> ordered, boolean denial) {
            this.area = area;
            this.ordered = ordered;
            this.denial = denial;
        }
    }

    private static final class MoveStack {
        private int top;
        private int[] flags = new int[16];

        private void push(boolean changed) {
            if (this.top >= this.flags.length) {
                this.flags = Arrays.copyOf(this.flags, this.flags.length * 2);
            }
            this.flags[this.top] = changed ? 1 : 0;
            this.top++;
        }

        private boolean pop() {
            if (this.top <= 0) {
                return false;
            }
            this.top--;
            return this.flags[this.top] != 0;
        }
    }
}
