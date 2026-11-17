package com.bslx.XueXiMod.network.prison;

import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 水牢快照和删除，只从服务器发到客户端。
 * 公共包不引用客户端类型。targetNetworkId 只在同步里出现，不改服务端 UUID 身份。
 */
public final class PrisonSyncPacket {
    public static final int SNAPSHOT = 1;
    public static final int REMOVE = 2;

    private final int kind;
    private final UUID prisonId;
    private final long revision;
    private final UUID casterId;
    private final UUID targetId;
    private final int targetNetworkId;
    private final ResourceLocation dimensionId;
    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;
    private final double lowX;
    private final double lowY;
    private final double lowZ;
    private final double highX;
    private final double highY;
    private final double highZ;
    private final int lockedTier;
    private final long birthTick;
    private final long endTick;

    public PrisonSyncPacket(int kind, UUID prisonId, long revision, UUID casterId, UUID targetId, int targetNetworkId,
                            ResourceLocation dimensionId, double anchorX, double anchorY, double anchorZ,
                            double lowX, double lowY, double lowZ, double highX, double highY, double highZ,
                            int lockedTier, long birthTick, long endTick) {
        this.kind = kind;
        this.prisonId = prisonId;
        this.revision = revision;
        this.casterId = casterId;
        this.targetId = targetId;
        this.targetNetworkId = targetNetworkId;
        this.dimensionId = dimensionId;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.lowX = lowX;
        this.lowY = lowY;
        this.lowZ = lowZ;
        this.highX = highX;
        this.highY = highY;
        this.highZ = highZ;
        this.lockedTier = lockedTier;
        this.birthTick = birthTick;
        this.endTick = endTick;
    }

    public static void register(SimpleChannel channel, int packetId) {
        channel.registerMessage(packetId, PrisonSyncPacket.class, PrisonSyncPacket::encode, PrisonSyncPacket::decode,
                PrisonSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendCreated(PrisonView view) {
        Resolved resolved = resolve(view);
        if (resolved == null || resolved.target == null) {
            return;
        }
        PrisonSyncPacket packet = snapshot(view, resolved.target.getId());
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> resolved.target), packet);
    }

    public static void sendSnapshot(ServerPlayer watcher, PrisonView view, int targetNetworkId) {
        if (watcher == null || !deliverable(view)) {
            return;
        }
        ModNetwork.sendToPlayer(watcher, snapshot(view, targetNetworkId));
    }

    public static void sendRemove(PrisonView view) {
        Resolved resolved = resolve(view);
        if (resolved == null || resolved.level == null) {
            return;
        }
        PrisonSyncPacket packet = remove(view);
        for (ServerPlayer watcher : resolved.level.players()) {
            ModNetwork.sendToPlayer(watcher, packet);
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(kind);
        buffer.writeUUID(prisonId);
        buffer.writeLong(revision);
        buffer.writeUUID(casterId);
        buffer.writeUUID(targetId);
        buffer.writeVarInt(targetNetworkId);
        buffer.writeResourceLocation(dimensionId);
        buffer.writeDouble(anchorX);
        buffer.writeDouble(anchorY);
        buffer.writeDouble(anchorZ);
        buffer.writeDouble(lowX);
        buffer.writeDouble(lowY);
        buffer.writeDouble(lowZ);
        buffer.writeDouble(highX);
        buffer.writeDouble(highY);
        buffer.writeDouble(highZ);
        buffer.writeVarInt(lockedTier);
        buffer.writeLong(birthTick);
        buffer.writeLong(endTick);
    }

    public static PrisonSyncPacket decode(FriendlyByteBuf buffer) {
        return new PrisonSyncPacket(buffer.readByte(), buffer.readUUID(), buffer.readLong(), buffer.readUUID(),
                buffer.readUUID(), buffer.readVarInt(), buffer.readResourceLocation(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readVarInt(), buffer.readLong(), buffer.readLong());
    }

    public static void handle(PrisonSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> PrisonClientBridge.accept(packet));
        contextSupplier.get().setPacketHandled(true);
    }

    public int kind() {
        return kind;
    }

    public UUID prisonId() {
        return prisonId;
    }

    public long revision() {
        return revision;
    }

    public UUID casterId() {
        return casterId;
    }

    public UUID targetId() {
        return targetId;
    }

    public int targetNetworkId() {
        return targetNetworkId;
    }

    public ResourceLocation dimensionId() {
        return dimensionId;
    }

    public double anchorX() {
        return anchorX;
    }

    public double anchorY() {
        return anchorY;
    }

    public double anchorZ() {
        return anchorZ;
    }

    public double lowX() {
        return lowX;
    }

    public double lowY() {
        return lowY;
    }

    public double lowZ() {
        return lowZ;
    }

    public double highX() {
        return highX;
    }

    public double highY() {
        return highY;
    }

    public double highZ() {
        return highZ;
    }

    public int lockedTier() {
        return lockedTier;
    }

    public long birthTick() {
        return birthTick;
    }

    public long endTick() {
        return endTick;
    }

    private static PrisonSyncPacket snapshot(PrisonView view, int targetNetworkId) {
        Vec3 anchor = view.anchor() == null ? Vec3.ZERO : view.anchor();
        AABB bounds = view.bounds();
        double boxLowX = bounds == null ? anchor.x : bounds.minX;
        double boxLowY = bounds == null ? anchor.y : bounds.minY;
        double boxLowZ = bounds == null ? anchor.z : bounds.minZ;
        double boxHighX = bounds == null ? anchor.x : bounds.maxX;
        double boxHighY = bounds == null ? anchor.y : bounds.maxY;
        double boxHighZ = bounds == null ? anchor.z : bounds.maxZ;
        ResourceLocation dimensionId = view.dimension().location();
        UUID casterId = view.casterId() == null ? view.prisonId() : view.casterId();
        UUID targetId = view.targetId() == null ? view.prisonId() : view.targetId();
        return new PrisonSyncPacket(SNAPSHOT, view.prisonId(), view.revision(), casterId, targetId,
                targetNetworkId, dimensionId, anchor.x, anchor.y, anchor.z,
                boxLowX, boxLowY, boxLowZ, boxHighX, boxHighY, boxHighZ,
                view.lockedTier(), view.birthTick(), view.endTick());
    }

    private static boolean deliverable(PrisonView view) {
        return view != null && view.prisonId() != null && view.dimension() != null;
    }

    private static PrisonSyncPacket remove(PrisonView view) {
        ResourceLocation dimensionId = view.dimension() == null ? new ResourceLocation("minecraft", "overworld")
                : view.dimension().location();
        UUID casterId = view.casterId() == null ? view.prisonId() : view.casterId();
        UUID targetId = view.targetId() == null ? view.prisonId() : view.targetId();
        return new PrisonSyncPacket(REMOVE, view.prisonId(), view.revision(), casterId, targetId, -1, dimensionId,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, view.lockedTier(), view.birthTick(), view.endTick());
    }

    private static Resolved resolve(PrisonView view) {
        if (!deliverable(view)) {
            return null;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        ServerLevel level = server.getLevel(view.dimension());
        if (level == null) {
            return null;
        }
        Entity target = view.targetId() == null ? null : level.getEntity(view.targetId());
        return new Resolved(level, target);
    }

    private record Resolved(ServerLevel level, Entity target) {
    }
}
