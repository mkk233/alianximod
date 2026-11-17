package com.bslx.XueXiMod.status.wood.resource;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 花瓣与岁华的服务端入口。公共修改都要求当前 MinecraftServer 的主线程，以及一次真实的服务端调用者。
 * 事件注册、网络包和 ResourceStatuses.register 由整合作者来做。这里只提供可调用方法。
 */
public final class WoodResourceService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, MinecraftServer> TICKET_SERVERS = new HashMap<>();
    private static volatile SuihuaBalanceSink balanceSink;
    private static boolean flushing;

    private WoodResourceService() {
    }

    public static void installBalanceSink(SuihuaBalanceSink sink) {
        balanceSink = sink;
    }

    /**
     * 取走自上次成功通知或上次 drain 之后余额变过的主人。没有安装 sink 时靠它避免每 tick 全量广播。
     * 安装了 sink 且通知成功的主人不会再出现；通知抛错或在回调里又改了账的，会留到下一次 tick。
     */
    public static List<UUID> drainDirtyOwners(MinecraftServer server) {
        WoodResourceSavedData data = dataOrNull(server);
        if (data == null) {
            return List.of();
        }
        return data.ledger().drainDirty();
    }

    public static boolean ledgerWritable(MinecraftServer server) {
        WoodResourceSavedData data = dataOrNull(server);
        return data != null && data.writable();
    }

    /** 服务端主线程读取。缺记录、错线程或账本被未知版本封存时返回全零，并且不写存档。 */
    public static SuihuaSnapshot readSuihua(MinecraftServer server, UUID ownerId) {
        WoodResourceSavedData data = dataOrNull(server);
        if (data == null || ownerId == null) {
            return SuihuaSnapshot.empty();
        }
        return data.ledger().snapshot(ownerId);
    }

    /** 签发当 tick 有效。失败返回 null，不留下空批次。成功会推进持久编号。 */
    public static ProductionTicket beginProduction(ServerPlayer owner, ResourceOrigin origin) {
        if (!livingActor(owner) || origin == null) {
            return null;
        }
        MinecraftServer server = owner.getServer();
        WoodResourceSavedData data = dataOrNull(server);
        if (data == null || !data.writable()) {
            return null;
        }
        ProductionTicket ticket = data.ledger().beginTicket(owner.getUUID(), origin, server.getTickCount());
        if (ticket == null) {
            return null;
        }
        TICKET_SERVERS.put(ticket.token(), server);
        data.setDirty();
        return ticket;
    }

    public static CreateResult publish(ProductionTicket ticket, List<GroundLotSpec> lots) {
        if (ticket == null) {
            return CreateResult.rejected(WoodReject.TICKET);
        }
        MinecraftServer server = TICKET_SERVERS.get(ticket.token());
        if (server == null) {
            return CreateResult.rejected(WoodReject.TICKET, ticket.serial());
        }
        if (!server.isSameThread()) {
            return CreateResult.rejected(WoodReject.THREAD, ticket.serial());
        }
        WoodResourceSavedData data = dataOrNull(server);
        if (data == null || !data.writable()) {
            TICKET_SERVERS.remove(ticket.token());
            return CreateResult.rejected(data == null ? WoodReject.THREAD : WoodReject.SEALED, ticket.serial());
        }
        ServerPlayer owner = findPlayer(server, ticket.ownerId());
        if (!livingActor(owner)) {
            data.ledger().burnIfOpen(ticket);
            TICKET_SERVERS.remove(ticket.token());
            return CreateResult.rejected(WoodReject.ACTOR, ticket.serial());
        }
        if (lots == null) {
            data.ledger().burnIfOpen(ticket);
            TICKET_SERVERS.remove(ticket.token());
            return CreateResult.rejected(WoodReject.VALIDATION, ticket.serial());
        }
        if (lots.size() > WoodLimits.ROOTS_PER_BATCH) {
            data.ledger().burnIfOpen(ticket);
            TICKET_SERVERS.remove(ticket.token());
            return CreateResult.rejected(WoodReject.CAPACITY, ticket.serial());
        }
        List<WoodLedger.LotDraft> drafts = new ArrayList<>();
        for (GroundLotSpec lot : lots) {
            WoodLedger.LotDraft draft = resolveLot(server, lot);
            if (draft == null) {
                data.ledger().burnIfOpen(ticket);
                TICKET_SERVERS.remove(ticket.token());
                return CreateResult.rejected(WoodReject.VALIDATION, ticket.serial());
            }
            drafts.add(draft);
        }
        if (!(owner.level() instanceof ServerLevel ownerLevel)) {
            data.ledger().burnIfOpen(ticket);
            TICKET_SERVERS.remove(ticket.token());
            return CreateResult.rejected(WoodReject.ACTOR, ticket.serial());
        }
        CreateResult result = data.ledger().publishTicket(ticket, server.getTickCount(),
                ownerLevel.dimension().location().toString(), ownerLevel.getGameTime(), drafts);
        TICKET_SERVERS.remove(ticket.token());
        if (result.accepted()) {
            data.setDirty();
        }
        return result;
    }

    public static GroundQueryResult query(ServerLevel level, UUID ownerId, AABB bounds, int limit) {
        if (level == null || !serverThread(level.getServer())) {
            return GroundQueryResult.rejected(WoodReject.THREAD);
        }
        if (ownerId == null || bounds == null) {
            return GroundQueryResult.rejected(WoodReject.VALIDATION);
        }
        WoodResourceSavedData data = dataOrNull(level.getServer());
        if (data == null) {
            return GroundQueryResult.rejected(WoodReject.THREAD);
        }
        if (!data.writable()) {
            return GroundQueryResult.rejected(WoodReject.SEALED);
        }
        return data.ledger().query(ownerId, level.dimension().location().toString(),
                bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ, limit);
    }

    public static Optional<GroundConsumeReceipt> consumePrepared(ServerPlayer actor, List<FragmentTake> takes,
                                                                 Predicate<GroundConsumePreview> ready) {
        if (!livingActor(actor) || ready == null) {
            return Optional.empty();
        }
        WoodResourceSavedData data = dataOrNull(actor.getServer());
        if (data == null || !data.writable()) {
            return Optional.empty();
        }
        Optional<GroundConsumeReceipt> receipt = data.ledger().consume(actor.getUUID(), takes, ready);
        if (receipt.isPresent()) {
            data.setDirty();
        }
        return receipt;
    }

    public static SplitResult split(ServerPlayer actor, FragmentHandle fragment, int leftUnits) {
        if (!serverThread(actor == null ? null : actor.getServer())) {
            return SplitResult.rejected(WoodReject.THREAD);
        }
        if (!livingActor(actor)) {
            return SplitResult.rejected(WoodReject.ACTOR);
        }
        if (fragment == null) {
            return SplitResult.rejected(WoodReject.VALIDATION);
        }
        WoodResourceSavedData data = dataOrNull(actor.getServer());
        if (data == null || !data.writable()) {
            return SplitResult.rejected(data == null ? WoodReject.THREAD : WoodReject.SEALED);
        }
        SplitResult result = data.ledger().split(actor.getUUID(), fragment.fragmentId(), fragment.revision(), leftUnits);
        if (result.accepted()) {
            data.setDirty();
        }
        return result;
    }

    /** 只有 EXTERNAL 会真正删除。其它原因直接拒绝，不能借此取得自然凋谢。删除本身不发岁华。 */
    public static RemoveResult discard(ServerPlayer actor, FragmentHandle fragment, RemovalReason reason) {
        if (!serverThread(actor == null ? null : actor.getServer())) {
            return RemoveResult.rejected(WoodReject.THREAD);
        }
        if (!livingActor(actor)) {
            return RemoveResult.rejected(WoodReject.ACTOR);
        }
        if (fragment == null) {
            return RemoveResult.rejected(WoodReject.VALIDATION);
        }
        WoodResourceSavedData data = dataOrNull(actor.getServer());
        if (data == null || !data.writable()) {
            return RemoveResult.rejected(data == null ? WoodReject.THREAD : WoodReject.SEALED);
        }
        RemoveResult result = data.ledger().discard(actor.getUUID(), fragment.fragmentId(), fragment.revision(), reason);
        if (result.accepted()) {
            data.setDirty();
        }
        return result;
    }

    /**
     * 未满三度时不调用 ready、不扣账。满额时 ready 看到清零前的六项；返回的也是这份消耗前快照。
     * 失败、抛错或回调里再改账，都不扣。成功后 sink 收到的是清零后的绝对余额。
     */
    public static Optional<SuihuaSnapshot> consumeFullPrepared(ServerPlayer player, Predicate<SuihuaSnapshot> ready) {
        if (!livingActor(player) || ready == null) {
            return Optional.empty();
        }
        MinecraftServer server = player.getServer();
        WoodResourceSavedData data = dataOrNull(server);
        if (data == null || !data.writable()) {
            return Optional.empty();
        }
        Optional<SuihuaSnapshot> spent = data.ledger().consumeFull(player.getUUID(), ready);
        if (spent.isPresent()) {
            data.setDirty();
            flushBalance(server);
        }
        return spent;
    }

    /** 每个服务器 tick 调用一次。推荐 ServerTickEvent 的 END 阶段。重复调用不会重复发奖。 */
    public static void onServerTick(MinecraftServer server) {
        WoodResourceSavedData data = dataOrNull(server);
        if (data == null || !data.writable()) {
            return;
        }
        WoodLedger.TickReport report = data.ledger().tick(new ServerProbe(server), server.getTickCount());
        for (UUID token : report.expiredTickets) {
            TICKET_SERVERS.remove(token);
        }
        if (report.persistentChange) {
            data.setDirty();
        }
        flushBalance(server);
    }

    /** 清本人地面和岁华，不把地面潜力加进岁华。玩家可以已经不在存活状态，但必须仍是服务端对象。 */
    public static void onOwnerDeath(ServerPlayer player) {
        clearOwner(player, true, "死亡");
    }

    /** 清本人全部地面，岁华保留，不发奖。 */
    public static void onOwnerLogout(ServerPlayer player) {
        clearOwner(player, false, "登出");
    }

    /** 清本人全部地面，岁华保留，不发奖。换维之后如果地面还留着，稍后的 tick 可能把已到期的部分入账。 */
    public static void onOwnerChangeDimension(ServerPlayer player) {
        clearOwner(player, false, "换维");
    }

    /** 清这个区块里的地面记录，不发奖，也不去加载区块。 */
    public static void onChunkUnload(ServerLevel level, ChunkPos chunk) {
        if (level == null || chunk == null || !serverThread(level.getServer())) {
            return;
        }
        WoodResourceSavedData data = dataOrNull(level.getServer());
        if (data == null || !data.writable()) {
            return;
        }
        int chunkX = chunk.x;
        int chunkZ = chunk.z;
        if (data.ledger().clearChunk(level.dimension().location().toString(), chunkX, chunkZ)) {
            data.setDirty();
        }
    }

    /** 清这个维度里的地面记录，不发奖。岁华仍留在主世界那一份账上。 */
    public static void onLevelUnload(ServerLevel level) {
        if (level == null || !serverThread(level.getServer())) {
            return;
        }
        WoodResourceSavedData data = dataOrNull(level.getServer());
        if (data == null || !data.writable()) {
            return;
        }
        if (data.ledger().clearDimension(level.dimension().location().toString())) {
            data.setDirty();
        }
    }

    /**
     * 世界加载完成后调用。会读取主世界账本：恢复岁华，丢弃上一会话的地面且不发奖。
     * 不要在更早的、主世界尚未就绪的时机调用。
     */
    public static void onServerStart(MinecraftServer server) {
        if (!serverThread(server)) {
            return;
        }
        TICKET_SERVERS.entrySet().removeIf(entry -> entry.getValue() != server);
        if (dataOrNull(server) == null) {
            LOGGER.warn("木属性资源账本未能在启动时加载");
        }
    }

    /** 丢掉未发布票据。不发奖，也不在停服时抹掉本会话地面；下次启动再丢弃地面。 */
    public static void onServerStop(MinecraftServer server) {
        if (!serverThread(server)) {
            return;
        }
        WoodResourceSavedData data = dataOrNull(server);
        if (data != null && data.writable()) {
            for (UUID token : data.ledger().discardOpenTickets()) {
                TICKET_SERVERS.remove(token);
            }
        }
        TICKET_SERVERS.entrySet().removeIf(entry -> entry.getValue() == server);
    }

    private static void clearOwner(ServerPlayer player, boolean death, String action) {
        if (!sessionActor(player)) {
            LOGGER.warn("花瓣与岁华{}清理被拒绝：需要服务端主线程上的玩家", action);
            return;
        }
        MinecraftServer server = player.getServer();
        WoodResourceSavedData data = dataOrNull(server);
        if (data == null || !data.writable()) {
            return;
        }
        boolean changed = death
                ? data.ledger().clearDeath(player.getUUID())
                : data.ledger().clearGround(player.getUUID());
        if (changed) {
            data.setDirty();
        }
        if (death) {
            flushBalance(server);
        }
    }

    private static void flushBalance(MinecraftServer server) {
        if (flushing || !serverThread(server) || balanceSink == null) {
            return;
        }
        WoodResourceSavedData data = dataOrNull(server);
        if (data == null) {
            return;
        }
        SuihuaBalanceSink sink = balanceSink;
        flushing = true;
        try {
            List<UUID> owners = data.ledger().peekDirty();
            List<UUID> delivered = new ArrayList<>();
            for (UUID ownerId : owners) {
                SuihuaSnapshot before = data.ledger().snapshot(ownerId);
                try {
                    sink.onChanged(ownerId, before);
                    if (before.equals(data.ledger().snapshot(ownerId))) {
                        delivered.add(ownerId);
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn("岁华余额通知失败，主人 {} 仍保留为脏", ownerId, exception);
                }
            }
            data.ledger().forgetDirty(delivered);
        } finally {
            flushing = false;
        }
    }

    private static WoodLedger.LotDraft resolveLot(MinecraftServer server, GroundLotSpec lot) {
        if (lot == null || lot.flower() == null || lot.dimension() == null || lot.position() == null) {
            return null;
        }
        if (lot.effectiveUnits() <= 0 || lot.potentialUnits() < 0 || lot.lifeTicks() <= 0L) {
            return null;
        }
        if (!WoodInterval.addressable(lot.position().x, lot.position().y, lot.position().z)) {
            return null;
        }
        ServerLevel lotLevel = server.getLevel(lot.dimension());
        if (lotLevel == null) {
            return null;
        }
        BlockPos block = BlockPos.containing(lot.position());
        if (!lotLevel.hasChunkAt(block)) {
            return null;
        }
        long gameTime = lotLevel.getGameTime();
        if (gameTime > Long.MAX_VALUE - lot.lifeTicks()) {
            return null;
        }
        return new WoodLedger.LotDraft(lot.flower(), lot.effectiveUnits(), lot.potentialUnits(), lot.lifeTicks(),
                gameTime + lot.lifeTicks(), lotLevel.dimension().location().toString(),
                lot.position().x, lot.position().y, lot.position().z);
    }

    private static ServerPlayer findPlayer(MinecraftServer server, UUID ownerId) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player != null && ownerId.equals(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    private static WoodResourceSavedData dataOrNull(MinecraftServer server) {
        if (!serverThread(server) || server.overworld() == null) {
            return null;
        }
        return WoodResourceSavedData.get(server);
    }

    private static boolean livingActor(ServerPlayer actor) {
        return sessionActor(actor) && actor.isAlive();
    }

    private static boolean sessionActor(ServerPlayer actor) {
        if (actor == null || actor.level() == null || actor.level().isClientSide()) {
            return false;
        }
        return serverThread(actor.getServer());
    }

    private static boolean serverThread(MinecraftServer server) {
        return server != null && server.isSameThread();
    }

    private static final class ServerProbe implements WoodLedger.WorldProbe {
        private final MinecraftServer server;
        private final Map<UUID, ServerPlayer> online;

        private ServerProbe(MinecraftServer server) {
            this.server = server;
            this.online = new HashMap<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player != null) {
                    online.put(player.getUUID(), player);
                }
            }
        }

        @Override
        public boolean ownerOnline(UUID ownerId) {
            return online.containsKey(ownerId);
        }

        @Override
        public boolean ownerAlive(UUID ownerId) {
            ServerPlayer player = online.get(ownerId);
            return player != null && player.isAlive();
        }

        @Override
        public boolean ownerInDimension(UUID ownerId, String dimensionId) {
            ServerPlayer player = online.get(ownerId);
            if (player == null || player.level() == null || dimensionId == null) {
                return false;
            }
            return dimensionId.equals(player.level().dimension().location().toString());
        }

        @Override
        public boolean chunkLoaded(String dimensionId, int chunkX, int chunkZ) {
            ServerLevel level = dimensionLevel(dimensionId);
            if (level == null) {
                return false;
            }
            BlockPos block = new BlockPos(chunkX * 16, 0, chunkZ * 16);
            return level.hasChunkAt(block);
        }

        @Override
        public long gameTime(String dimensionId) {
            ServerLevel level = dimensionLevel(dimensionId);
            return level == null ? -1L : level.getGameTime();
        }

        private ServerLevel dimensionLevel(String dimensionId) {
            if (dimensionId == null || dimensionId.isBlank()) {
                return null;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(dimensionId);
            if (parsed == null) {
                return null;
            }
            return server.getLevel(ResourceKey.create(Registries.DIMENSION, parsed));
        }
    }
}
