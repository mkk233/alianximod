package com.bslx.XueXiMod.shu.technique.release.firecast;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.logging.LogUtils;
import com.bslx.XueXiMod.network.FireVisualPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.RoundHandle;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
import com.bslx.XueXiMod.shu.technique.release.domain.BoundHostBridge;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainBindingRuntime;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainGeometry;
import com.bslx.XueXiMod.shu.technique.release.domain.GloomVolumes;
import com.bslx.XueXiMod.shu.technique.release.domain.HostView;
import com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainCycle;
import com.bslx.XueXiMod.shu.technique.release.fire.FireDamage;
import com.bslx.XueXiMod.status.fire.seed.FireSeedProduction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 灼地宿主。位置在出生时定死。每服务器刻重算贴地盒子，出生刻就做第一次接触。
 * 年龄到达 100 先结束，不再检测、产种或结算绑定。已有燃烧和火种余额不在这里删除。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class SurfaceFireHosts {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    public static final int FIELD_LIFE = 100;
    private static final int VISUAL_REFRESH = 40;
    private static final AtomicLong NEXT_VISUAL = new AtomicLong(1L);
    private static final HashMap<UUID, Host> HOSTS = new HashMap<>();

    private SurfaceFireHosts() {
    }

    public static int configuredLifetimeTicks() {
        return BalanceService.intValue(SpecialBalance.SCORCH_FIELD, BalanceKeys.LIFETIME_TICKS);
    }

    public static boolean open(ServerLevel level, ServerPlayer player, ReleaseBatch batch, @Nullable BoundReleasePlan plan,
                               @Nullable EnchantmentSnapshot snapshot) {
        return open(level, player, batch, plan, snapshot, null, null);
    }

    public static boolean open(ServerLevel level, ServerPlayer player, ReleaseBatch batch, @Nullable BoundReleasePlan plan,
                               @Nullable EnchantmentSnapshot snapshot, @Nullable ResourceEnchantLoad load,
                               @Nullable UUID rootCastId) {
        if (level == null || player == null || batch == null || level.isClientSide() || level.getServer() == null) {
            return false;
        }
        Vec3 feet = player.position();
        Vec3 look = player.getLookAngle();
        double lookX = look == null ? 0.0d : look.x;
        double lookZ = look == null ? 0.0d : look.z;
        double horizontal = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double stepX = horizontal < 1.0E-4d ? 0.0d : lookX / horizontal;
        double stepZ = horizontal < 1.0E-4d ? 1.0d : lookZ / horizontal;
        double sampleX = feet.x + stepX * SurfaceContactField.FORWARD_STEP;
        double sampleZ = feet.z + stepZ * SurfaceContactField.FORWARD_STEP;
        double searchY = feet.y;
        CastFrame frame = CastFrame.current();
        if (frame != null && frame.overrideGeometry && frame.origin != null) {
            sampleX = frame.origin.x;
            sampleZ = frame.origin.z;
            searchY = frame.origin.y;
        }
        double surfaceY = StandSurface.columnTop(level, sampleX, sampleZ, searchY, SurfaceContactField.VERTICAL_SLACK);
        if (Double.isNaN(surfaceY)) {
            return false;
        }
        UUID releaseToken = batch.fireContext().castId();
        ScorchSeedLedger.retainRelease(releaseToken);
        int birthTick = level.getServer().getTickCount();
        long birthGameTime = level.getGameTime();
        boolean bound = plan != null && plan.members() != null && !plan.members().isEmpty();
        Host host = new Host(level, player.getUUID(), releaseToken, sampleX, sampleZ, surfaceY, birthTick, birthGameTime,
                bound ? plan : null);
        HOSTS.put(host.instanceId, host);
        if (host.boundPlan() != null) {
            host.registered = true;
            BoundHostBridge.register(host);
        }
        if (snapshot != null || (load != null && load.gloom())) {
            ContactEnchantState.open(host, snapshot, load, rootCastId);
        }
        host.settle(birthTick);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        int now = event.getServer().getTickCount();
        ScorchSeedLedger.discardMaturePeriods(now);
        ArrayList<Host> snapshot = new ArrayList<>(HOSTS.values());
        for (Host host : snapshot) {
            host.settle(now);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ArrayList<Host> snapshot = new ArrayList<>(HOSTS.values());
        for (Host host : snapshot) {
            host.close();
        }
        ScorchSeedLedger.clearTransient();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID ownerId = player.getUUID();
        ArrayList<Host> snapshot = new ArrayList<>(HOSTS.values());
        for (Host host : snapshot) {
            if (ownerId.equals(host.ownerId())) {
                host.close();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ArrayList<Host> snapshot = new ArrayList<>(HOSTS.values());
        for (Host host : snapshot) {
            if (host.hostLevel() == serverLevel) {
                host.close();
            }
        }
    }

    static final class Host implements ContactCarrier, HostView, GloomVolumes {
        private final UUID instanceId = UUID.randomUUID();
        private final UUID ownerId;
        private final UUID releaseToken;
        private final ServerLevel level;
        private final double centerX;
        private final double centerZ;
        private final double referenceY;
        private final int birthTick;
        private final int expiresAt;
        private final long birthGameTime;
        private final long expireGameTime;
        private final long visualId;
        private final String dimensionId;
        private final DomainGeometry geometry;
        private final @Nullable BoundReleasePlan plan;
        private final ArrayList<RoundHandle> openRounds = new ArrayList<>();
        private final HashSet<UUID> seenTargets = new HashSet<>();
        private int nextBindingBeat;
        private int handledTick = Integer.MIN_VALUE;
        private int geometryTick = Integer.MIN_VALUE;
        private long sentSignature = Long.MIN_VALUE;
        private int geometryVersion = 1;
        private int refusedVersion = -1;
        private boolean closed;
        private boolean registered;
        private List<SurfaceContactField.Slice> slices = List.of();
        private AABB union;

        private Host(ServerLevel level, UUID ownerId, UUID releaseToken, double centerX, double centerZ, double referenceY,
                     int birthTick, long birthGameTime, @Nullable BoundReleasePlan plan) {
            this.level = level;
            this.ownerId = ownerId;
            this.releaseToken = releaseToken;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.referenceY = referenceY;
            this.birthTick = birthTick;
            int lifeTicks = configuredLifetimeTicks();
            this.expiresAt = birthTick + lifeTicks;
            this.birthGameTime = birthGameTime;
            this.expireGameTime = birthGameTime + lifeTicks;
            this.visualId = NEXT_VISUAL.getAndIncrement();
            this.dimensionId = level.dimension().location().toString();
            this.plan = plan;
            this.geometry = new SurfacePatchGeometry(this);
            this.nextBindingBeat = plan == null ? 0 : birthTick + DomainBindingRuntime.PERIOD_TICKS;
        }

        List<SurfaceContactField.Slice> currentSlices() {
            return slices;
        }

        @Override
        public int exportVolumes(double[] destination) {
            int count = slices.size();
            if (destination == null || destination.length < count * 6) {
                return count;
            }
            int slot = 0;
            for (SurfaceContactField.Slice slice : slices) {
                destination[slot] = slice.lowX();
                destination[slot + 1] = slice.lowY();
                destination[slot + 2] = slice.lowZ();
                destination[slot + 3] = slice.highX();
                destination[slot + 4] = slice.highY();
                destination[slot + 5] = slice.highZ();
                slot += 6;
            }
            return count;
        }

        AABB unionBounds() {
            return union;
        }

        private void settle(int now) {
            if (closed) {
                return;
            }
            MinecraftServer server = level.getServer();
            if (server == null || server.getLevel(level.dimension()) != level) {
                close();
                return;
            }
            if (ReleaseInput.hasExpired(now, expiresAt)) {
                close();
                return;
            }
            if (handledTick == now) {
                return;
            }
            refresh(now);
            handledTick = now;
            publishVisual(now);
            grantContacts(server, now);
            if (!closed) {
                NetherDomainCycle.sync(this);
            }
        }

        private void refresh(int now) {
            if (geometryTick == now) {
                return;
            }
            slices = SurfaceContactField.collect(level, centerX, centerZ, referenceY);
            geometryTick = now;
            union = null;
            for (SurfaceContactField.Slice slice : slices) {
                AABB box = slice.box();
                union = union == null ? box : union.minmax(box);
            }
        }

        private void grantContacts(MinecraftServer server, int now) {
            if (slices.isEmpty() || union == null) {
                return;
            }
            ServerPlayer caster = server.getPlayerList().getPlayer(ownerId);
            if (caster == null || !caster.isAlive()) {
                return;
            }
            int fieldAge = now - birthTick;
            List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, union, LivingEntity::isAlive);
            for (LivingEntity target : found) {
                if (target == null || target.level() != level || !target.isAlive()) {
                    continue;
                }
                if (!ContactPolicy.mayContactAffect(ownerId, target) || !DamagePermission.allows(caster, target)) {
                    continue;
                }
                AABB body = target.getBoundingBox();
                if (!meetsSlice(body)) {
                    continue;
                }
                UUID targetId = target.getUUID();
                boolean firstSight = seenTargets.add(targetId);
                if (firstSight) {
                    if (ScorchSeedLedger.claimFirst(ownerId, releaseToken, targetId)) {
                        double settledDamage = SpecialBalance.settle(SpecialBalance.SCORCH_FIELD, 1.0d);
                        FireDamage.strikeOnce(target, ownerId, settledDamage, 1.0d, true);
                        int added = FireSeedProduction.produce(target, caster, ScorchSeedLedger.FIRST_BASE);
                        if (added > 0) {
                            sendSpark(target, body);
                        }
                    }
                    continue;
                }
                if (ScorchSeedLedger.admitPeriod(ownerId, targetId, fieldAge, now)) {
                    int added = FireSeedProduction.produce(target, caster, ScorchSeedLedger.PERIOD_BASE);
                    if (added > 0) {
                        sendSpark(target, body);
                    }
                }
            }
        }

        private boolean meetsSlice(AABB body) {
            for (SurfaceContactField.Slice slice : slices) {
                if (slice.meets(body)) {
                    return true;
                }
            }
            return false;
        }

        private void sendSpark(LivingEntity target, AABB body) {
            SurfaceContactField.Slice source = strongestOverlap(body);
            if (source == null || level.isClientSide()) {
                return;
            }
            double fromX = (source.lowX() + source.highX()) * 0.5d;
            double fromY = source.lowY() + 0.05d;
            double fromZ = (source.lowZ() + source.highZ()) * 0.5d;
            FireVisualPacket packet = FireVisualPacket.spark(target.getId(), fromX, fromY, fromZ, 0);
            for (ServerPlayer watcher : level.players()) {
                ModNetwork.sendToPlayer(watcher, packet);
            }
        }

        private SurfaceContactField.Slice strongestOverlap(AABB body) {
            SurfaceContactField.Slice chosen = null;
            double chosenVolume = 0.0d;
            for (SurfaceContactField.Slice slice : slices) {
                double overlap = overlapVolume(slice, body);
                if (overlap > chosenVolume) {
                    chosen = slice;
                    chosenVolume = overlap;
                }
            }
            return chosen;
        }

        private static double overlapVolume(SurfaceContactField.Slice slice, AABB body) {
            double sharedLowX = Math.max(slice.lowX(), body.minX);
            double sharedHighX = Math.min(slice.highX(), body.maxX);
            double sharedLowY = Math.max(slice.lowY(), body.minY);
            double sharedHighY = Math.min(slice.highY(), body.maxY);
            double sharedLowZ = Math.max(slice.lowZ(), body.minZ);
            double sharedHighZ = Math.min(slice.highZ(), body.maxZ);
            double spanX = sharedHighX - sharedLowX;
            double spanY = sharedHighY - sharedLowY;
            double spanZ = sharedHighZ - sharedLowZ;
            if (spanX <= 0.0d || spanY <= 0.0d || spanZ <= 0.0d) {
                return 0.0d;
            }
            return spanX * spanY * spanZ;
        }

        private void publishVisual(int now) {
            long signature = sliceSignature();
            boolean changed = signature != sentSignature;
            if (!changed && now % VISUAL_REFRESH != 0) {
                return;
            }
            if (changed) {
                geometryVersion++;
                if (geometryVersion <= 0) {
                    geometryVersion = 1;
                }
                sentSignature = signature;
                refusedVersion = -1;
            }
            if (refusedVersion == geometryVersion) {
                return;
            }
            double[] packed = packSlices();
            int sliceCount = packed.length / 6;
            if (!SurfaceSnapshotFrames.withinBudget(sliceCount)) {
                refusedVersion = geometryVersion;
                LOGGER.warn("灼地地表快照超出传输上限，拒绝同步，不截断。host={} version={} slices={} bytes={} capSlices={} capBytes={}",
                        visualId, geometryVersion, sliceCount, SurfaceSnapshotFrames.transmissionBytes(sliceCount),
                        SurfaceSnapshotFrames.OVERALL_SLICES, SurfaceSnapshotFrames.OVERALL_BYTES);
                return;
            }
            int perPacket = SurfaceSnapshotFrames.PER_PACKET_SLICES;
            int fragmentCount = sliceCount == 0 ? 1 : (sliceCount + perPacket - 1) / perPacket;
            int fragmentIndex = 0;
            while (fragmentIndex < fragmentCount) {
                int from = fragmentIndex * perPacket * 6;
                int endSlice = Math.min(sliceCount, (fragmentIndex + 1) * perPacket);
                int until = endSlice * 6;
                double[] piece = new double[Math.max(0, until - from)];
                if (piece.length > 0) {
                    System.arraycopy(packed, from, piece, 0, piece.length);
                }
                FireVisualPacket packet = FireVisualPacket.surface(visualId, dimensionId, centerX, referenceY, centerZ,
                        birthGameTime, expireGameTime, 0, geometryVersion, fragmentIndex, fragmentCount, piece);
                for (ServerPlayer watcher : level.players()) {
                    ModNetwork.sendToPlayer(watcher, packet);
                }
                fragmentIndex++;
            }
        }

        private double[] packSlices() {
            double[] packed = new double[slices.size() * 6];
            int slot = 0;
            for (SurfaceContactField.Slice slice : slices) {
                packed[slot] = slice.lowX();
                packed[slot + 1] = slice.lowY();
                packed[slot + 2] = slice.lowZ();
                packed[slot + 3] = slice.highX();
                packed[slot + 4] = slice.highY();
                packed[slot + 5] = slice.highZ();
                slot += 6;
            }
            return packed;
        }

        private long sliceSignature() {
            long mixed = 1L;
            for (SurfaceContactField.Slice slice : slices) {
                mixed = mixed * 31L + Math.round(slice.lowX() * 1000.0d);
                mixed = mixed * 31L + Math.round(slice.lowY() * 1000.0d);
                mixed = mixed * 31L + Math.round(slice.lowZ() * 1000.0d);
                mixed = mixed * 31L + Math.round(slice.highX() * 1000.0d);
                mixed = mixed * 31L + Math.round(slice.highY() * 1000.0d);
                mixed = mixed * 31L + Math.round(slice.highZ() * 1000.0d);
            }
            return mixed * 31L + slices.size();
        }

        private void close() {
            if (closed) {
                return;
            }
            closed = true;
            NetherDomainCycle.drop(instanceId);
            int remaining = ScorchSeedLedger.releaseHost(releaseToken);
            if (remaining == 0) {
                ScorchSeedLedger.discardRelease(releaseToken);
            }
            if (registered) {
                BoundHostBridge.unregister(instanceId);
                registered = false;
            }
            HOSTS.remove(instanceId);
            seenTargets.clear();
            if (level != null && !level.isClientSide()) {
                FireVisualPacket packet = FireVisualPacket.surfaceEnd(visualId, dimensionId, birthGameTime, expireGameTime);
                for (ServerPlayer watcher : level.players()) {
                    ModNetwork.sendToPlayer(watcher, packet);
                }
            }
            cancelOpenRounds();
        }

        @Override
        public UUID hostId() {
            return instanceId;
        }

        @Override
        public int revision() {
            return 1;
        }

        @Override
        public UUID ownerId() {
            return ownerId;
        }

        @Override
        public boolean skipsAbsentOwnerBeats() {
            return true;
        }

        @Override
        public boolean active() {
            if (closed || level == null || level.getServer() == null) {
                return false;
            }
            return !ReleaseInput.hasExpired(level.getServer().getTickCount(), expiresAt);
        }

        @Override
        public boolean touches(LivingEntity target) {
            if (target == null || !target.isAlive() || target.level() != level) {
                return false;
            }
            refresh(level.getServer() == null ? geometryTick : level.getServer().getTickCount());
            return meetsSlice(target.getBoundingBox());
        }

        @Override
        public ServerLevel hostLevel() {
            return level;
        }

        @Override
        public AABB queryBox() {
            if (level.getServer() != null) {
                refresh(level.getServer().getTickCount());
            }
            if (union == null) {
                return new AABB(centerX, referenceY, centerZ, centerX, referenceY, centerZ);
            }
            return union;
        }

        @Override
        public UUID instanceId() {
            return instanceId;
        }

        @Override
        public DomainGeometry geometry() {
            return geometry;
        }

        @Override
        public BoundReleasePlan boundPlan() {
            return plan;
        }

        @Override
        public int startedAt() {
            return birthTick;
        }

        @Override
        public int expiresAt() {
            return expiresAt;
        }

        @Override
        public int nextBindingBeat() {
            return nextBindingBeat;
        }

        @Override
        public void advanceBindingBeat(int followingBeat) {
            this.nextBindingBeat = followingBeat;
        }

        @Override
        public boolean roundStillHosted(int now) {
            if (closed || level == null || ownerId == null) {
                return false;
            }
            if (ReleaseInput.hasExpired(now, expiresAt)) {
                return false;
            }
            MinecraftServer server = level.getServer();
            if (server == null) {
                return false;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            return owner != null && owner.isAlive() && owner.level() == level;
        }

        @Override
        public void trackRound(RoundHandle handle, int now) {
            if (handle == null) {
                return;
            }
            if (!roundStillHosted(now)) {
                if (!handle.done()) {
                    handle.cancel();
                }
                return;
            }
            if (handle.done()) {
                return;
            }
            openRounds.add(handle);
        }

        @Override
        public void sweepFinishedRounds() {
            openRounds.removeIf(RoundHandle::done);
        }

        @Override
        public void cancelOpenRounds() {
            if (openRounds.isEmpty()) {
                return;
            }
            ArrayList<RoundHandle> pendingRounds = new ArrayList<>(openRounds);
            openRounds.clear();
            Throwable failure = null;
            for (RoundHandle handle : pendingRounds) {
                if (handle == null || handle.done()) {
                    continue;
                }
                try {
                    handle.cancel();
                } catch (Throwable thrown) {
                    if (failure == null) {
                        failure = thrown;
                    }
                }
            }
            if (failure instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            if (failure instanceof Error errorFailure) {
                throw errorFailure;
            }
        }
    }
}
