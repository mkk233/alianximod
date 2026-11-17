package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.FireVisualPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;
import com.bslx.XueXiMod.visual.fire.FireVisualLayout;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 把已经发生的火场、燃烧和炉心成功结果告诉附近客户端。
 * 不新建术式结算，也不在失败、死亡或被动结束时发冷却。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class FireVisualRelay {
    private static final double WATCH_REACH = 96.0d;
    private static final int REFRESH_GAP = 20;
    private static final Set<Long> SHOWN = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Long, BurnMemory> MEMORY = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<GroundSpot> GROUNDS = new CopyOnWriteArrayList<>();

    private FireVisualRelay() {
    }

    public static boolean watching(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        int entityId = entity.getId();
        return SHOWN.contains(FireVisualLayout.burnToken(entityId, FireVisualLayout.SPREAD))
                || SHOWN.contains(FireVisualLayout.burnToken(entityId, FireVisualLayout.SKY))
                || SHOWN.contains(FireVisualLayout.burnToken(entityId, FireVisualLayout.KINDLE));
    }

    public static void publish(LivingEntity entity) {
        afterCarrierTick(entity);
    }

    public static void afterCarrierTick(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!entity.isAlive() || entity.isRemoved()) {
            abort(entity);
            return;
        }
        BurnLedger ledger = BurnCarrier.read(entity);
        syncKind(entity, shownSpread(ledger), FireVisualLayout.SPREAD);
        syncKind(entity, find(ledger, FireConsumerPlans.SKY_KEY), FireVisualLayout.SKY);
        syncKind(entity, find(ledger, ResourceEffectKeys.KINDLE), FireVisualLayout.KINDLE);
    }

    public static void abort(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        endKind(entity, FireVisualLayout.SPREAD, false);
        endKind(entity, FireVisualLayout.SKY, false);
        endKind(entity, FireVisualLayout.KINDLE, false);
    }

    public static void openGround(ServerLevel level, long zoneId, Vec3 origin, double radius, long birthTick, long expireTick) {
        if (level == null || origin == null || level.isClientSide() || !Double.isFinite(radius) || radius <= 0.0d) {
            return;
        }
        String dimensionId = level.dimension().location().toString();
        GroundSpot spot = new GroundSpot(zoneId, dimensionId, origin.x, origin.y, origin.z, radius, birthTick, expireTick);
        GROUNDS.add(spot);
        sendGround(level, spot);
    }

    public static void closeGround(ServerLevel level, long zoneId) {
        GROUNDS.removeIf(spot -> spot.zoneId == zoneId);
        if (level == null || level.isClientSide()) {
            return;
        }
        FireVisualPacket packet = FireVisualPacket.groundEnd(zoneId);
        for (ServerPlayer watcher : level.players()) {
            ModNetwork.sendToPlayer(watcher, packet);
        }
    }

    public static void furnace(ServerPlayer player, int furnaceCue) {
        if (player == null || furnaceCue == 0 || player.level().isClientSide()) {
            return;
        }
        FireVisualPacket packet = FireVisualPacket.furnace(player.getId(), furnaceCue);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        if (event.getServer().getTickCount() % REFRESH_GAP != 0) {
            return;
        }
        for (GroundSpot spot : GROUNDS) {
            ServerLevel level = findLevel(event.getServer(), spot.dimensionId);
            if (level != null) {
                sendGround(level, spot);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendGrounds(player);
            sendCurrent(player, player);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendGrounds(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || !(event.getTarget() instanceof LivingEntity living)) {
            return;
        }
        if (living.level().isClientSide()) {
            return;
        }
        sendCurrent(watcher, living);
    }

    private static void sendCurrent(ServerPlayer watcher, LivingEntity living) {
        if (watcher == null || living == null) {
            return;
        }
        BurnLedger ledger = BurnCarrier.read(living);
        offer(watcher, living, shownSpread(ledger), FireVisualLayout.SPREAD);
        offer(watcher, living, find(ledger, FireConsumerPlans.SKY_KEY), FireVisualLayout.SKY);
        offer(watcher, living, find(ledger, ResourceEffectKeys.KINDLE), FireVisualLayout.KINDLE);
    }

    private static void offer(ServerPlayer watcher, LivingEntity living, BurnTrackState state, int burnKind) {
        if (state == null) {
            return;
        }
        remember(living.getId(), burnKind, state);
        ModNetwork.sendToPlayer(watcher, pose(living.getId(), burnKind, state));
    }

    private static void syncKind(LivingEntity entity, BurnTrackState state, int burnKind) {
        if (state != null) {
            remember(entity.getId(), burnKind, state);
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                    pose(entity.getId(), burnKind, state));
            return;
        }
        endKind(entity, burnKind, true);
    }

    private static void remember(int entityId, int burnKind, BurnTrackState state) {
        long token = FireVisualLayout.burnToken(entityId, burnKind);
        SHOWN.add(token);
        MEMORY.put(token, new BurnMemory(state.remainder(), state.segmentLimit(), state.intervalTicks()));
    }

    private static void endKind(LivingEntity entity, int burnKind, boolean naturalEnd) {
        long token = FireVisualLayout.burnToken(entity.getId(), burnKind);
        if (!SHOWN.remove(token)) {
            return;
        }
        BurnMemory memory = MEMORY.remove(token);
        int remainder = memory == null ? 0 : memory.remainder;
        int segmentCount = memory == null ? 0 : memory.segmentCount;
        int interval = memory == null ? 0 : memory.interval;
        FireVisualPacket packet = FireVisualPacket.burnEnd(entity.getId(), burnKind, naturalEnd, remainder,
                segmentCount, interval);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    private static FireVisualPacket pose(int entityId, int burnKind, BurnTrackState state) {
        return FireVisualPacket.burn(entityId, burnKind, state.tiers(), state.remainder(), state.segmentLimit(),
                state.intervalTicks(), state.segmentCursor(), state.ticksUntilNext());
    }

    /** 同一效果键只留一份画面，取当前还活着的最高档，避免按层数堆粒子。 */
    private static BurnTrackState shownSpread(BurnLedger ledger) {
        BurnTrackState chosen = null;
        if (ledger == null) {
            return null;
        }
        for (BurnTrackState state : ledger.export()) {
            if (!FireConsumerPlans.SPREAD_KEY.equals(state.effectKey())) {
                continue;
            }
            if (chosen == null || state.tiers() > chosen.tiers()
                    || (state.tiers() == chosen.tiers() && state.remainder() > chosen.remainder())) {
                chosen = state;
            }
        }
        return chosen;
    }

    private static BurnTrackState find(BurnLedger ledger, String effectKey) {
        if (ledger == null || effectKey == null) {
            return null;
        }
        for (BurnTrackState state : ledger.export()) {
            if (effectKey.equals(state.effectKey())) {
                return state;
            }
        }
        return null;
    }

    private static void sendGrounds(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        String dimensionId = level.dimension().location().toString();
        for (GroundSpot spot : GROUNDS) {
            if (!dimensionId.equals(spot.dimensionId)) {
                continue;
            }
            if (player.distanceToSqr(spot.originX, spot.originY, spot.originZ) > reachSquared(spot.radius)) {
                continue;
            }
            ModNetwork.sendToPlayer(player, groundPacket(spot));
        }
    }

    private static void sendGround(ServerLevel level, GroundSpot spot) {
        FireVisualPacket packet = groundPacket(spot);
        double reachSquared = reachSquared(spot.radius);
        for (ServerPlayer watcher : level.players()) {
            if (watcher.distanceToSqr(spot.originX, spot.originY, spot.originZ) > reachSquared) {
                continue;
            }
            ModNetwork.sendToPlayer(watcher, packet);
        }
    }

    private static FireVisualPacket groundPacket(GroundSpot spot) {
        return FireVisualPacket.ground(spot.zoneId, spot.dimensionId, spot.originX, spot.originY, spot.originZ,
                spot.radius, spot.birthTick, spot.expireTick);
    }

    private static double reachSquared(double radius) {
        double reach = WATCH_REACH + radius;
        return reach * reach;
    }

    private static ServerLevel findLevel(MinecraftServer server, String dimensionId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimensionId)) {
                return level;
            }
        }
        return null;
    }

    private static final class BurnMemory {
        private final int remainder;
        private final int segmentCount;
        private final int interval;

        private BurnMemory(int remainder, int segmentCount, int interval) {
            this.remainder = remainder;
            this.segmentCount = segmentCount;
            this.interval = interval;
        }
    }

    private static final class GroundSpot {
        private final long zoneId;
        private final String dimensionId;
        private final double originX;
        private final double originY;
        private final double originZ;
        private final double radius;
        private final long birthTick;
        private final long expireTick;

        private GroundSpot(long zoneId, String dimensionId, double originX, double originY, double originZ,
                           double radius, long birthTick, long expireTick) {
            this.zoneId = zoneId;
            this.dimensionId = dimensionId;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.radius = radius;
            this.birthTick = birthTick;
            this.expireTick = expireTick;
        }
    }
}
