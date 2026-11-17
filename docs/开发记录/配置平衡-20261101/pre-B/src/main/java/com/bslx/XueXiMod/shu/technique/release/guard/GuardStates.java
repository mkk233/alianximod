package com.bslx.XueXiMod.shu.technique.release.guard;

import com.bslx.XueXiMod.network.GuardSyncPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingBandGeometry;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingDeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.hosts.RingFollow;
import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 每人每类护体最多一个。不进场地表，也不增加场地层数。 */
public final class GuardStates {
    private static final ArrayList<BodyGuard> ACTIVE = new ArrayList<>();
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Integer>> WAVES = new ConcurrentHashMap<>();

    private GuardStates() {
    }

    /** 当下就能生成时返回是否生成成功。排到以后的，只表示已经排上。 */
    public static boolean schedule(ServerPlayer player, GuardDefinition guard, int fireTick, long waveKey) {
        return schedule(player, guard, fireTick, waveKey, ReleaseInput.BASE);
    }

    public static boolean schedule(ServerPlayer player, GuardDefinition guard, int fireTick, long waveKey, ReleaseInput input) {
        if (player == null || guard == null || player.getServer() == null) {
            return false;
        }
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        UUID playerId = player.getUUID();
        int epoch = ChargeGate.epoch(playerId);
        if (fireTick <= player.getServer().getTickCount()) {
            if (!ChargeGate.same(playerId, epoch) || !player.isAlive()) {
                return false;
            }
            return activate(player, guard, waveKey, false, frozen);
        }
        ChainScheduler.schedule(player.getServer(), fireTick, () -> {
            if (!ChargeGate.same(playerId, epoch) || !player.isAlive()) {
                return;
            }
            activate(player, guard, waveKey, false, frozen);
        });
        return true;
    }

    public static boolean activate(ServerPlayer player, GuardDefinition guard, long waveKey) {
        return activate(player, guard, waveKey, ReleaseInput.BASE);
    }

    public static boolean activate(ServerPlayer player, GuardDefinition guard, long waveKey, ReleaseInput input) {
        return activate(player, guard, waveKey, false, input);
    }

    /** 生成费已由父会话预留。 */
    public static boolean activateReserved(ServerPlayer player, GuardDefinition guard, long waveKey) {
        return activateReserved(player, guard, waveKey, ReleaseInput.BASE);
    }

    public static boolean activateReserved(ServerPlayer player, GuardDefinition guard, long waveKey, ReleaseInput input) {
        return activate(player, guard, waveKey, true, input);
    }

    private static boolean activate(ServerPlayer player, GuardDefinition guard, long waveKey, boolean reserved, ReleaseInput input) {
        if (player == null || guard == null || player.getServer() == null) {
            return false;
        }
        if (!claimWave(player.getUUID(), waveKey, player.getServer().getTickCount())) {
            return true;
        }
        if (!reserved && !MagicPool.trySpend(player, guard.spawnCost(), true)) {
            forgetWave(player.getUUID(), waveKey);
            return false;
        }
        if (!reserved && guard.spawnCost() > 0) {
            MagicPool.showBalance(player);
        }
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        int now = player.getServer().getTickCount();
        BodyGuard existing = find(player.getUUID(), guard.typeKey());
        if (existing != null) {
            RefreshCarry carried = retune(guard, frozen, now, existing.animationStart, existing.nextUpkeepAt, existing.revision);
            detachDeflection(existing);
            existing.guard = carried.derived;
            existing.expiresAt = carried.expiresAt;
            existing.revision = carried.revision;
            existing.animationStart = carried.animationStart;
            existing.nextUpkeepAt = carried.nextUpkeepAt;
            existing.dimension = player.level().dimension();
            existing.serverLevel = player.serverLevel();
            if (reserved) {
                PublicationAttempt.reportPlacedCopy(0);
            }
            attachDeflection(existing);
            broadcastFull(player, existing);
            return true;
        }
        GuardDefinition derived = guard.withInput(frozen);
        BodyGuard created = new BodyGuard();
        created.ownerId = player.getUUID();
        created.dimension = player.level().dimension();
        created.serverLevel = player.serverLevel();
        created.guard = derived;
        created.expiresAt = expiryOf(now, derived.lifetimeTicks());
        created.nextUpkeepAt = now + derived.upkeepPeriod();
        created.animationStart = now;
        created.revision = 1;
        created.fromNormal = GuardPose.baseNormal(derived.baseTilt());
        created.towardNormal = created.fromNormal;
        created.phaseStart = now;
        created.phaseEnd = now;
        ACTIVE.add(created);
        if (reserved) {
            PublicationAttempt.reportPlacedCopy(0);
        }
        attachDeflection(created);
        broadcastFull(player, created);
        return true;
    }

    /** 从注册定义重新派生。动画起点和维护节拍原样带回，供刷新写回。 */
    public static RefreshCarry retune(GuardDefinition registered, ReleaseInput input, int now,
                                      int animationStart, int nextUpkeepAt, int revision) {
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        GuardDefinition derived = registered.withInput(frozen);
        return new RefreshCarry(derived, expiryOf(now, derived.lifetimeTicks()), animationStart, nextUpkeepAt, revision + 1);
    }

    public static int expiryOf(int now, int scaledLife) {
        if (scaledLife <= 0) {
            return now + scaledLife;
        }
        return ReleaseInput.expiryTick(now, scaledLife);
    }

    public static boolean withinLife(int now, int expiresAt) {
        return !ReleaseInput.hasExpired(now, expiresAt);
    }

    public static final class RefreshCarry {
        public final GuardDefinition derived;
        public final int expiresAt;
        public final int animationStart;
        public final int nextUpkeepAt;
        public final int revision;

        private RefreshCarry(GuardDefinition derived, int expiresAt, int animationStart, int nextUpkeepAt, int revision) {
            this.derived = derived;
            this.expiresAt = expiresAt;
            this.animationStart = animationStart;
            this.nextUpkeepAt = nextUpkeepAt;
            this.revision = revision;
        }
    }

    public static float unloadRatio(ServerPlayer player) {
        BodyGuard guard = living(player);
        if (guard == null) {
            return 0f;
        }
        float ratio = guard.guard.unloadRatio();
        if (!Float.isFinite(ratio) || ratio <= 0f) {
            return 0f;
        }
        return ratio > 0.6f ? 0.6f : ratio;
    }

    public static void strike(ServerPlayer player, int poseKind, Vec3 incoming) {
        BodyGuard guard = living(player);
        if (guard == null || poseKind == GuardPose.NONE) {
            return;
        }
        int now = player.getServer().getTickCount();
        Vec3 target = poseKind == GuardPose.MELEE ? GuardPose.worldLevel() : GuardPose.leanToward(incoming);
        beginMove(player, guard, target, GuardPose.CAUSE_HIT, GuardPose.ENTER_TICKS, now);
        guard.holdUntil = now + GuardPose.ENTER_TICKS + GuardPose.HOLD_TICKS;
        guard.returning = false;
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        Iterator<BodyGuard> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            BodyGuard guard = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(guard.ownerId);
            if (owner == null || !owner.isAlive() || owner.level().dimension() != guard.dimension) {
                drop(guard);
                iterator.remove();
                if (owner != null) {
                    broadcastRemove(owner, guard);
                }
                continue;
            }
            if (!withinLife(now, guard.expiresAt)) {
                drop(guard);
                iterator.remove();
                broadcastRemove(owner, guard);
                continue;
            }
            advancePose(owner, guard, now);
            if (now >= guard.nextUpkeepAt) {
                if (!MagicPool.trySpend(owner, guard.guard.upkeepCost(), false)) {
                    drop(guard);
                    iterator.remove();
                    broadcastRemove(owner, guard);
                    continue;
                }
                guard.nextUpkeepAt += guard.guard.upkeepPeriod();
            }
            if ((now & 1) == 0) {
                GuardPreview.look(owner, guard, now);
            }
        }
        forgetOldWaves(now);
    }

    public static void closeOwned(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        Iterator<BodyGuard> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            BodyGuard guard = iterator.next();
            if (!guard.ownerId.equals(playerId)) {
                continue;
            }
            iterator.remove();
            drop(guard);
            broadcastRemove(player, guard);
        }
        WAVES.remove(playerId);
    }

    public static GuardSyncPacket snapshot(UUID ownerId) {
        for (BodyGuard guard : ACTIVE) {
            if (guard.ownerId.equals(ownerId)) {
                return fullPacket(guard);
            }
        }
        return null;
    }

    static void preview(ServerPlayer owner, BodyGuard guard, Vec3 incoming, int entityId, int now) {
        if (guard.poseCause == GuardPose.CAUSE_HIT && now < guard.holdUntil) {
            return;
        }
        if (guard.previewEntityId == entityId && guard.previewDirection != null
                && guard.previewDirection.distanceToSqr(incoming) < 0.04) {
            return;
        }
        guard.previewEntityId = entityId;
        guard.previewDirection = incoming;
        beginMove(owner, guard, GuardPose.leanToward(incoming), GuardPose.CAUSE_PREVIEW, GuardPose.ENTER_TICKS, now);
        guard.holdUntil = now + GuardPose.ENTER_TICKS + 4;
        guard.returning = false;
    }

    private static void advancePose(ServerPlayer owner, BodyGuard guard, int now) {
        if (guard.poseCause == GuardPose.CAUSE_NONE || now < guard.phaseEnd || now < guard.holdUntil || guard.returning) {
            if (guard.returning && now >= guard.phaseEnd) {
                guard.poseCause = GuardPose.CAUSE_NONE;
                guard.returning = false;
                guard.previewEntityId = Integer.MIN_VALUE;
            }
            return;
        }
        Vec3 base = GuardPose.baseNormal(guard.guard.baseTilt());
        beginMove(owner, guard, base, guard.poseCause, GuardPose.RETURN_TICKS, now);
        guard.returning = true;
        guard.holdUntil = now;
    }

    private static void beginMove(ServerPlayer owner, BodyGuard guard, Vec3 target, int cause, int ticks, int now) {
        guard.fromNormal = guard.towardNormal == null ? GuardPose.baseNormal(guard.guard.baseTilt()) : guard.towardNormal;
        guard.towardNormal = target;
        guard.poseCause = cause;
        guard.phaseStart = now;
        guard.phaseEnd = now + Math.max(1, ticks);
        guard.sequence += 1;
        GuardSyncPacket.sendAround(owner, posePacket(guard, ticks));
    }

    private static BodyGuard living(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return null;
        }
        BodyGuard guard = find(player.getUUID(), null);
        if (guard == null || !withinLife(player.getServer().getTickCount(), guard.expiresAt)) {
            return null;
        }
        if (player.level().dimension() != guard.dimension) {
            return null;
        }
        return guard;
    }

    private static BodyGuard find(UUID ownerId, String typeKey) {
        for (BodyGuard guard : ACTIVE) {
            if (!guard.ownerId.equals(ownerId)) {
                continue;
            }
            if (typeKey == null || guard.guard.typeKey().equals(typeKey)) {
                return guard;
            }
        }
        return null;
    }

    private static boolean claimWave(UUID playerId, long waveKey, int now) {
        ConcurrentHashMap<Long, Integer> waves = WAVES.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
        Integer previous = waves.putIfAbsent(waveKey, now);
        return previous == null;
    }

    private static void forgetWave(UUID playerId, long waveKey) {
        ConcurrentHashMap<Long, Integer> waves = WAVES.get(playerId);
        if (waves != null) {
            waves.remove(waveKey);
        }
    }

    private static void forgetOldWaves(int now) {
        for (ConcurrentHashMap<Long, Integer> waves : WAVES.values()) {
            waves.entrySet().removeIf(entry -> now - entry.getValue() > 400);
        }
    }

    private static void broadcastFull(ServerPlayer owner, BodyGuard guard) {
        GuardSyncPacket.sendAround(owner, fullPacket(guard));
    }

    public static void bindContact(ServerPlayer player, EnchantmentSnapshot snapshot) {
        BodyGuard guard = living(player);
        if (guard == null || snapshot == null) {
            return;
        }
        if (guard.contact != null) {
            guard.contact.close();
            guard.contact = null;
        }
        guard.contactRevision += 1;
        guard.contact = ContactEnchantState.open(new GuardTouch(guard), snapshot);
    }

    private static void drop(BodyGuard guard) {
        guard.retired = true;
        detachDeflection(guard);
        if (guard.contact != null) {
            guard.contact.close();
            guard.contact = null;
        }
    }

    private static void attachDeflection(BodyGuard guard) {
        detachDeflection(guard);
        if (guard.retired || guard.guard == null || !guard.guard.deflectsProjectiles() || guard.serverLevel == null) {
            return;
        }
        if (guard.ownerId == null || guard.hostId == null) {
            return;
        }
        guard.deflectionHost = new RingDeflectionHost(
                new LiveRing(guard, guard.revision),
                guard.ownerId,
                guard.hostId,
                guard.revision,
                guard.serverLevel,
                guard.guard.radius(),
                guard.guard.band() * 0.5);
        DeflectionHosts.register(guard.deflectionHost);
    }

    private static void detachDeflection(BodyGuard guard) {
        if (guard.deflectionHost == null) {
            return;
        }
        DeflectionHosts.unregister(guard.deflectionHost.key());
        guard.deflectionHost = null;
    }

    /**
     * 偏转环面跟当前主人，法线用姿态插值。插值公式在 {@link RingBandGeometry#blendNormal}。
     * 主人不在本维度或这一代 revision 已经换掉时不再跟踪。
     */
    private static final class LiveRing implements RingFollow {
        private final BodyGuard guard;
        private final int boundRevision;

        private LiveRing(BodyGuard guard, int boundRevision) {
            this.guard = guard;
            this.boundRevision = boundRevision;
        }

        @Override
        public boolean live() {
            if (guard.retired || guard.revision != boundRevision || guard.serverLevel == null || guard.guard == null) {
                return false;
            }
            if (guard.serverLevel.getServer() == null) {
                return false;
            }
            int now = guard.serverLevel.getServer().getTickCount();
            if (!withinLife(now, guard.expiresAt)) {
                return false;
            }
            ServerPlayer owner = guard.serverLevel.getServer().getPlayerList().getPlayer(guard.ownerId);
            return owner != null && owner.isAlive() && owner.level() == guard.serverLevel;
        }

        @Override
        public Vec3 center() {
            ServerPlayer owner = owner();
            if (owner == null || guard.guard == null) {
                return Vec3.ZERO;
            }
            return owner.position().add(0.0, owner.getBbHeight() * guard.guard.bodyHeight(), 0.0);
        }

        @Override
        public Vec3 planeNormal() {
            int now = 0;
            if (guard.serverLevel != null && guard.serverLevel.getServer() != null) {
                now = guard.serverLevel.getServer().getTickCount();
            }
            return RingBandGeometry.blendNormal(guard.fromNormal, guard.towardNormal, guard.phaseStart, guard.phaseEnd, now);
        }

        @Override
        public Vec3 ownerAnchor() {
            ServerPlayer owner = owner();
            if (owner == null) {
                return center();
            }
            return owner.position();
        }

        private ServerPlayer owner() {
            if (guard.serverLevel == null || guard.serverLevel.getServer() == null || guard.ownerId == null) {
                return null;
            }
            return guard.serverLevel.getServer().getPlayerList().getPlayer(guard.ownerId);
        }
    }

    private static void broadcastRemove(ServerPlayer owner, BodyGuard guard) {
        GuardSyncPacket packet = removePacket(guard);
        GuardSyncPacket.sendAround(owner, packet);
        ServerLevel savedLevel = guard.serverLevel;
        if (owner == null || savedLevel == null || savedLevel == owner.serverLevel()) {
            return;
        }
        for (ServerPlayer watcher : savedLevel.players()) {
            if (watcher == null || watcher == owner) {
                continue;
            }
            ModNetwork.sendToPlayer(watcher, packet);
        }
    }

    private static GuardSyncPacket removePacket(BodyGuard guard) {
        int serverTick = serverTick(guard);
        return new GuardSyncPacket(GuardSyncPacket.REMOVE, guard.ownerId, guard.revision, guard.sequence,
                0, 0, guard.animationStart, animationAgeTicks(serverTick, guard.animationStart),
                0f, 0f, 0f, 0f,
                0f, 0f, 0f,
                0f, 0f, 0f, 0);
    }

    private static GuardSyncPacket fullPacket(BodyGuard guard) {
        int serverTick = serverTick(guard);
        SnapshotPose pose = snapshotPose(guard.fromNormal, guard.towardNormal, guard.phaseStart, guard.phaseEnd, serverTick);
        return new GuardSyncPacket(GuardSyncPacket.FULL, guard.ownerId, guard.revision, guard.sequence,
                guard.expiresAt, guard.nextUpkeepAt, guard.animationStart, animationAgeTicks(serverTick, guard.animationStart),
                guard.guard.baseTilt(),
                (float) guard.guard.radius(), (float) guard.guard.band(), (float) guard.guard.bodyHeight(),
                (float) pose.from.x, (float) pose.from.y, (float) pose.from.z,
                (float) pose.toward.x, (float) pose.toward.y, (float) pose.toward.z,
                pose.poseTicks);
    }

    private static GuardSyncPacket posePacket(BodyGuard guard, int ticks) {
        int serverTick = serverTick(guard);
        Vec3 from = guard.fromNormal;
        Vec3 toward = guard.towardNormal;
        return new GuardSyncPacket(GuardSyncPacket.POSE, guard.ownerId, guard.revision, guard.sequence,
                guard.expiresAt, guard.nextUpkeepAt, guard.animationStart, animationAgeTicks(serverTick, guard.animationStart),
                guard.guard.baseTilt(),
                (float) guard.guard.radius(), (float) guard.guard.band(), (float) guard.guard.bodyHeight(),
                (float) from.x, (float) from.y, (float) from.z,
                (float) toward.x, (float) toward.y, (float) toward.z, ticks);
    }

    private static int serverTick(BodyGuard guard) {
        if (guard.serverLevel == null || guard.serverLevel.getServer() == null) {
            return 0;
        }
        return guard.serverLevel.getServer().getTickCount();
    }

    static int animationAgeTicks(int serverTick, int animationStart) {
        int elapsed = serverTick - animationStart;
        if (elapsed < 0) {
            return 0;
        }
        return elapsed;
    }

    static SnapshotPose snapshotPose(Vec3 fromNormal, Vec3 towardNormal, int phaseStart, int phaseEnd, int serverTick) {
        Vec3 target = towardNormal == null ? GuardPose.worldLevel() : towardNormal;
        boolean settled = phaseEnd <= phaseStart || serverTick >= phaseEnd;
        if (settled) {
            return new SnapshotPose(target, target, 0);
        }
        Vec3 storedFrom = fromNormal == null ? target : fromNormal;
        Vec3 blended = RingBandGeometry.blendNormal(storedFrom, target, phaseStart, phaseEnd, serverTick);
        int remaining = phaseEnd - serverTick;
        return new SnapshotPose(blended, target, remaining);
    }

    static final class SnapshotPose {
        final Vec3 from;
        final Vec3 toward;
        final int poseTicks;

        private SnapshotPose(Vec3 from, Vec3 toward, int poseTicks) {
            this.from = from;
            this.toward = toward;
            this.poseTicks = poseTicks;
        }
    }

    static final class BodyGuard {
        private UUID ownerId;
        private ResourceKey<Level> dimension;
        private GuardDefinition guard;
        private int expiresAt;
        private int nextUpkeepAt;
        private int animationStart;
        private int revision;
        private int sequence;
        private int poseCause;
        private int phaseStart;
        private int phaseEnd;
        private int holdUntil;
        private boolean returning;
        private Vec3 fromNormal = GuardPose.worldLevel();
        private Vec3 towardNormal = GuardPose.worldLevel();
        private Vec3 previewDirection;
        private int previewEntityId = Integer.MIN_VALUE;
        private UUID hostId = UUID.randomUUID();
        private ContactEnchantState contact;
        private int contactRevision = 1;
        private boolean retired;
        private ServerLevel serverLevel;
        private RingDeflectionHost deflectionHost;
    }

    private static final class GuardTouch implements ContactCarrier {
        private final BodyGuard guard;

        private GuardTouch(BodyGuard guard) {
            this.guard = guard;
        }

        @Override
        public UUID hostId() {
            return guard.hostId;
        }

        @Override
        public int revision() {
            return guard.contactRevision;
        }

        @Override
        public UUID ownerId() {
            return guard.ownerId;
        }

        @Override
        public boolean active() {
            return !guard.retired;
        }

        @Override
        public boolean touches(LivingEntity target) {
            if (target == null || guard.guard == null) {
                return false;
            }
            ServerLevel level = hostLevel();
            if (level == null) {
                return false;
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(guard.ownerId);
            if (owner == null) {
                return false;
            }
            Vec3 normal = guard.towardNormal == null ? GuardPose.worldLevel() : guard.towardNormal;
            if (normal.lengthSqr() < 1.0E-6) {
                normal = GuardPose.worldLevel();
            }
            normal = normal.normalize();
            Vec3 center = owner.position().add(0.0, owner.getBbHeight() * guard.guard.bodyHeight(), 0.0);
            double radius = guard.guard.radius();
            double reach = guard.guard.band() * 0.5 + ContactEnchantState.SURFACE_SLOP;
            AABB body = target.getBoundingBox();
            Vec3 nearest = nearest(body, center);
            Vec3 relative = nearest.subtract(center);
            double along = relative.dot(normal);
            Vec3 planar = relative.subtract(normal.scale(along));
            double radial = planar.length();
            Vec3 ringPoint = radial < 1.0E-4
                    ? center
                    : center.add(planar.scale(radius / radial));
            Vec3 closest = nearest(body, ringPoint);
            return closest.distanceToSqr(ringPoint) <= reach * reach;
        }

        @Override
        public ServerLevel hostLevel() {
            return guard.serverLevel;
        }

        @Override
        public AABB queryBox() {
            ServerLevel level = hostLevel();
            if (level == null || guard.guard == null) {
                return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            }
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(guard.ownerId);
            if (owner == null) {
                return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            }
            double reach = guard.guard.radius() + guard.guard.band() + 2.0;
            return owner.getBoundingBox().inflate(reach);
        }

        private static Vec3 nearest(AABB body, Vec3 point) {
            double clampedX = point.x < body.minX ? body.minX : (point.x > body.maxX ? body.maxX : point.x);
            double clampedY = point.y < body.minY ? body.minY : (point.y > body.maxY ? body.maxY : point.y);
            double clampedZ = point.z < body.minZ ? body.minZ : (point.z > body.maxZ ? body.maxZ : point.z);
            return new Vec3(clampedX, clampedY, clampedZ);
        }
    }
}
