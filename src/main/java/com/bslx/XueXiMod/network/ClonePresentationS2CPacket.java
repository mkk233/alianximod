package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.CloneAfterimagePlayback;
import com.bslx.XueXiMod.shu.technique.release.clone.core.ClonePresentation;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneService;
import com.bslx.XueXiMod.shu.technique.release.clone.core.ClonePresentationNotice;
import com.bslx.XueXiMod.shu.technique.release.clone.core.NoticePhase;
import com.bslx.XueXiMod.shu.technique.release.clone.core.PresentationKey;
import com.bslx.XueXiMod.shu.technique.release.clone.core.VolumeVisualKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 水幻形表现事件。字段与 {@link ClonePresentationNotice} 相同，枚举只写序号。
 * 只从服务端发往已经选中的玩家，客户端入队后不回任何业务包。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class ClonePresentationS2CPacket {
    public static final int TRACKING_WINDOW_TICKS = 200;
    public static final double TRACKING_REACH_BLOCKS = 160.0d;
    public static final double COORDINATE_SPAN = 3.0e7d;
    public static final int DURATION_LIMIT = 200;
    public static final int REVISION_LIMIT = 1_000_000;
    public static final int DIMENSION_TEXT_LIMIT = 256;
    private static final double LOOK_TOLERANCE = 1.0e-4d;
    private static final int LEDGER_LIMIT = 4096;
    private static final Map<SentKey, SentLife> sentLedger = new LinkedHashMap<>();
    private static boolean sinkInstalled;

    private final ResourceKey<Level> dimension;
    private final long sessionToken;
    private final UUID deploymentId;
    private final long actionToken;
    private final long eventSerial;
    private final PresentationKey presentationKey;
    private final int presentationRevision;
    private final double worldX;
    private final double worldY;
    private final double worldZ;
    private final double lookX;
    private final double lookY;
    private final double lookZ;
    private final NoticePhase phase;
    private final int durationTicks;
    private final VolumeVisualKind volumeVisual;

    private ClonePresentationS2CPacket(ResourceKey<Level> dimension, long sessionToken, UUID deploymentId,
                                       long actionToken, long eventSerial, PresentationKey presentationKey,
                                       int presentationRevision, double worldX, double worldY, double worldZ,
                                       double lookX, double lookY, double lookZ, NoticePhase phase,
                                       int durationTicks, VolumeVisualKind volumeVisual) {
        this.dimension = dimension;
        this.sessionToken = sessionToken;
        this.deploymentId = deploymentId;
        this.actionToken = actionToken;
        this.eventSerial = eventSerial;
        this.presentationKey = presentationKey;
        this.presentationRevision = presentationRevision;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.lookX = lookX;
        this.lookY = lookY;
        this.lookZ = lookZ;
        this.phase = phase;
        this.durationTicks = durationTicks;
        this.volumeVisual = volumeVisual;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, ClonePresentationS2CPacket.class, ClonePresentationS2CPacket::encode,
                ClonePresentationS2CPacket::decode, ClonePresentationS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** 换下 A 的无操作接收器。重复调用直接返回，避免第二次 install 抛错。 */
    public static void installSink() {
        if (sinkInstalled) {
            return;
        }
        ClonePresentation.install(ClonePresentationS2CPacket::relay);
        sinkInstalled = true;
    }

    public static void handle(ClonePresentationS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        if (packet == null || context.getSender() != null
                || context.getDirection() != NetworkDirection.PLAY_TO_CLIENT
                || FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> CloneAfterimagePlayback.accept(packet)));
    }

    public ClonePresentationNotice noticeOrNull() {
        if (!schemaAccepts(this.dimension, this.sessionToken, this.deploymentId, this.actionToken, this.eventSerial,
                this.presentationKey, this.presentationRevision, this.worldX, this.worldY, this.worldZ,
                this.lookX, this.lookY, this.lookZ, this.phase, this.durationTicks, this.volumeVisual)) {
            return null;
        }
        return new ClonePresentationNotice(this.dimension, this.sessionToken, this.deploymentId, this.actionToken,
                this.eventSerial, this.presentationKey, this.presentationRevision,
                new Vec3(this.worldX, this.worldY, this.worldZ), new Vec3(this.lookX, this.lookY, this.lookZ),
                this.phase, this.durationTicks, this.volumeVisual);
    }

    private void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.dimension.location().toString(), DIMENSION_TEXT_LIMIT);
        buffer.writeLong(this.sessionToken);
        buffer.writeUUID(this.deploymentId);
        buffer.writeLong(this.actionToken);
        buffer.writeLong(this.eventSerial);
        buffer.writeVarInt(this.presentationKey.ordinal());
        buffer.writeVarInt(this.presentationRevision);
        buffer.writeDouble(this.worldX);
        buffer.writeDouble(this.worldY);
        buffer.writeDouble(this.worldZ);
        buffer.writeDouble(this.lookX);
        buffer.writeDouble(this.lookY);
        buffer.writeDouble(this.lookZ);
        buffer.writeVarInt(this.phase.ordinal());
        buffer.writeVarInt(this.durationTicks);
        buffer.writeVarInt(this.volumeVisual.ordinal());
    }

    private static ClonePresentationS2CPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation dimensionId = ResourceLocation.tryParse(buffer.readUtf(DIMENSION_TEXT_LIMIT));
        ResourceKey<Level> dimension = dimensionId == null
                ? null
                : ResourceKey.create(Registries.DIMENSION, dimensionId);
        long sessionToken = buffer.readLong();
        UUID deploymentId = buffer.readUUID();
        long actionToken = buffer.readLong();
        long eventSerial = buffer.readLong();
        int presentationOrdinal = buffer.readVarInt();
        int presentationRevision = buffer.readVarInt();
        double worldX = buffer.readDouble();
        double worldY = buffer.readDouble();
        double worldZ = buffer.readDouble();
        double lookX = buffer.readDouble();
        double lookY = buffer.readDouble();
        double lookZ = buffer.readDouble();
        int phaseOrdinal = buffer.readVarInt();
        int durationTicks = buffer.readVarInt();
        int visualOrdinal = buffer.readVarInt();
        return new ClonePresentationS2CPacket(dimension, sessionToken, deploymentId, actionToken, eventSerial,
                enumAt(PresentationKey.values(), presentationOrdinal), presentationRevision, worldX, worldY, worldZ,
                lookX, lookY, lookZ, enumAt(NoticePhase.values(), phaseOrdinal), durationTicks,
                enumAt(VolumeVisualKind.values(), visualOrdinal));
    }

    private static void relay(ClonePresentationNotice notice) {
        if (notice == null || notice.worldPosition() == null || notice.lookUnit() == null
                || !schemaAccepts(notice.dimension(), notice.sessionToken(), notice.deploymentId(),
                notice.actionToken(), notice.eventSerial(), notice.presentationKey(), notice.presentationRevision(),
                notice.worldPosition().x, notice.worldPosition().y, notice.worldPosition().z,
                notice.lookUnit().x, notice.lookUnit().y, notice.lookUnit().z, notice.phase(),
                notice.durationTicks(), notice.volumeVisual())) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            return;
        }
        ServerLevel serverLevel = server.getLevel(notice.dimension());
        if (serverLevel == null) {
            return;
        }
        long gameTick = server.getTickCount();
        dropExpired(gameTick);
        ClonePresentationS2CPacket packet = new ClonePresentationS2CPacket(notice.dimension(), notice.sessionToken(),
                notice.deploymentId(), notice.actionToken(), notice.eventSerial(), notice.presentationKey(),
                notice.presentationRevision(), notice.worldPosition().x, notice.worldPosition().y,
                notice.worldPosition().z, notice.lookUnit().x, notice.lookUnit().y, notice.lookUnit().z,
                notice.phase(), notice.durationTicks(), notice.volumeVisual());
        Vec3 anchor = notice.worldPosition();
        double reachSquared = TRACKING_REACH_BLOCKS * TRACKING_REACH_BLOCKS;
        long retainTicks = Math.max(notice.durationTicks(), TRACKING_WINDOW_TICKS);
        long expireTick = gameTick + retainTicks;
        for (ServerPlayer viewer : serverLevel.players()) {
            if (viewer.distanceToSqr(anchor.x, anchor.y, anchor.z) > reachSquared) {
                continue;
            }
            offerViewer(viewer, notice, packet, expireTick);
        }
        ServerPlayer owner = CloneService.findPresentationOwner(server, notice.deploymentId());
        offerViewer(owner, notice, packet, expireTick);
    }

    private static void offerViewer(ServerPlayer viewer, ClonePresentationNotice notice,
                                    ClonePresentationS2CPacket packet, long expireTick) {
        if (viewer == null || viewer.isRemoved() || viewer.hasDisconnected() || notice.dimension() == null
                || viewer.level() == null || !notice.dimension().equals(viewer.level().dimension())) {
            return;
        }
        SentKey sentKey = new SentKey(viewer.getUUID(), notice.deploymentId(), notice.actionToken(),
                notice.phase().ordinal());
        if (sentLedger.containsKey(sentKey)) {
            return;
        }
        ModNetwork.sendToPlayer(viewer, packet);
        while (sentLedger.size() >= LEDGER_LIMIT) {
            Iterator<Map.Entry<SentKey, SentLife>> cursor = sentLedger.entrySet().iterator();
            if (!cursor.hasNext()) {
                break;
            }
            cursor.next();
            cursor.remove();
        }
        sentLedger.put(sentKey, new SentLife(notice.dimension(), expireTick));
    }

    private static boolean schemaAccepts(ResourceKey<Level> dimension, long sessionToken, UUID deploymentId,
                                         long actionToken, long eventSerial, PresentationKey presentationKey,
                                         int presentationRevision, double worldX, double worldY, double worldZ,
                                         double lookX, double lookY, double lookZ, NoticePhase phase,
                                         int durationTicks, VolumeVisualKind volumeVisual) {
        if (dimension == null || dimension.location() == null || deploymentId == null || presentationKey == null
                || phase == null || volumeVisual == null) {
            return false;
        }
        ResourceLocation location = dimension.location();
        if (location.getNamespace().isEmpty() || location.getPath().isEmpty()
                || location.toString().length() > DIMENSION_TEXT_LIMIT) {
            return false;
        }
        if (deploymentId.getMostSignificantBits() == 0L && deploymentId.getLeastSignificantBits() == 0L) {
            return false;
        }
        if (sessionToken < 0L || actionToken < 0L || eventSerial < 0L) {
            return false;
        }
        if (presentationRevision < 0 || presentationRevision > REVISION_LIMIT) {
            return false;
        }
        if (durationTicks < 0 || durationTicks > DURATION_LIMIT) {
            return false;
        }
        if (!insideWorld(worldX) || !insideWorld(worldY) || !insideWorld(worldZ)) {
            return false;
        }
        return unitLook(lookX, lookY, lookZ);
    }

    private static boolean insideWorld(double coordinate) {
        return Double.isFinite(coordinate) && Math.abs(coordinate) <= COORDINATE_SPAN;
    }

    private static boolean unitLook(double lookX, double lookY, double lookZ) {
        if (!Double.isFinite(lookX) || !Double.isFinite(lookY) || !Double.isFinite(lookZ)) {
            return false;
        }
        double squared = lookX * lookX + lookY * lookY + lookZ * lookZ;
        double lowerBand = (1.0d - LOOK_TOLERANCE) * (1.0d - LOOK_TOLERANCE);
        double upperBand = (1.0d + LOOK_TOLERANCE) * (1.0d + LOOK_TOLERANCE);
        return squared >= lowerBand && squared <= upperBand;
    }

    private static <T extends Enum<T>> T enumAt(T[] values, int ordinal) {
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }

    private static void dropExpired(long gameTick) {
        Iterator<Map.Entry<SentKey, SentLife>> cursor = sentLedger.entrySet().iterator();
        while (cursor.hasNext()) {
            Map.Entry<SentKey, SentLife> entry = cursor.next();
            if (gameTick >= entry.getValue().expireTick()) {
                cursor.remove();
            }
        }
    }

    private static boolean serverThread() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || sentLedger.isEmpty() || !serverThread()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        dropExpired(server.getTickCount());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer) || !serverThread()) {
            return;
        }
        UUID playerId = viewer.getUUID();
        Iterator<Map.Entry<SentKey, SentLife>> cursor = sentLedger.entrySet().iterator();
        while (cursor.hasNext()) {
            if (playerId.equals(cursor.next().getKey().playerId())) {
                cursor.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !serverThread()) {
            return;
        }
        ResourceKey<Level> dimension = serverLevel.dimension();
        Iterator<Map.Entry<SentKey, SentLife>> cursor = sentLedger.entrySet().iterator();
        while (cursor.hasNext()) {
            if (dimension.equals(cursor.next().getValue().dimension())) {
                cursor.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        sentLedger.clear();
    }

    private record SentKey(UUID playerId, UUID deploymentId, long actionToken, int phaseOrdinal) {
    }

    private record SentLife(ResourceKey<Level> dimension, long expireTick) {
    }
}
