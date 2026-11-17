package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainSight;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 幽冥范围同步。通道登记仍是 {@code register(channel, packetId)}，标识沿用公共网络入口。
 * 前缀保持原来的单盒字段，后面追加同一修订的真实切片分片。未登记时发送直接跳过。
 */
public final class NetherDomainSync {
    public static final int PIECE_LIMIT = 48;
    public static final int VOLUME_CAP = 3072;

    private static volatile boolean channelReady;

    private final boolean removed;
    private final UUID domainId;
    private final int revision;
    private final UUID ownerId;
    private final String dimensionId;
    private final double lowX;
    private final double lowY;
    private final double lowZ;
    private final double highX;
    private final double highY;
    private final double highZ;
    private final long visualEnd;
    private final int volumeCount;
    private final int fragmentIndex;
    private final int fragmentCount;
    private final double[] piece;

    public NetherDomainSync(boolean removed, UUID domainId, int revision, UUID ownerId, String dimensionId,
                            double lowX, double lowY, double lowZ, double highX, double highY, double highZ,
                            long visualEnd, int volumeCount, int fragmentIndex, int fragmentCount, double[] piece) {
        this.removed = removed;
        this.domainId = domainId;
        this.revision = revision;
        this.ownerId = ownerId;
        this.dimensionId = dimensionId == null ? "" : dimensionId;
        this.lowX = lowX;
        this.lowY = lowY;
        this.lowZ = lowZ;
        this.highX = highX;
        this.highY = highY;
        this.highZ = highZ;
        this.visualEnd = visualEnd;
        this.volumeCount = volumeCount;
        this.fragmentIndex = fragmentIndex;
        this.fragmentCount = fragmentCount;
        this.piece = piece == null ? new double[0] : piece.clone();
    }

    public static boolean channelReady() {
        return channelReady;
    }

    public static void register(SimpleChannel channel, int packetId) {
        channel.registerMessage(packetId, NetherDomainSync.class, NetherDomainSync::encode, NetherDomainSync::decode,
                NetherDomainSync::handle);
        channelReady = true;
    }

    public static void emit(ServerPlayer watcher, boolean removed, UUID domainId, int revision, UUID ownerId,
                            String dimensionId, double lowX, double lowY, double lowZ, double highX, double highY,
                            double highZ, long visualEnd, double[] volumes) {
        if (!channelReady || watcher == null || domainId == null || ownerId == null) {
            return;
        }
        double[] source = volumes == null ? new double[0] : volumes;
        int total = source.length / 6;
        if (removed) {
            ModNetwork.sendToPlayer(watcher, new NetherDomainSync(true, domainId, revision, ownerId, dimensionId,
                    lowX, lowY, lowZ, highX, highY, highZ, visualEnd, 0, 0, 1, new double[0]));
            return;
        }
        int fragmentCount = total == 0 ? 1 : (total + PIECE_LIMIT - 1) / PIECE_LIMIT;
        int fragmentIndex = 0;
        while (fragmentIndex < fragmentCount) {
            int fromSlice = fragmentIndex * PIECE_LIMIT;
            int untilSlice = Math.min(total, fromSlice + PIECE_LIMIT);
            int pieceCount = Math.max(0, untilSlice - fromSlice);
            double[] piece = new double[pieceCount * 6];
            if (pieceCount > 0) {
                System.arraycopy(source, fromSlice * 6, piece, 0, piece.length);
            }
            ModNetwork.sendToPlayer(watcher, new NetherDomainSync(false, domainId, revision, ownerId, dimensionId,
                    lowX, lowY, lowZ, highX, highY, highZ, visualEnd, total, fragmentIndex, fragmentCount, piece));
            fragmentIndex++;
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(removed);
        buffer.writeUUID(domainId);
        buffer.writeVarInt(revision);
        buffer.writeUUID(ownerId);
        buffer.writeUtf(dimensionId);
        buffer.writeDouble(lowX);
        buffer.writeDouble(lowY);
        buffer.writeDouble(lowZ);
        buffer.writeDouble(highX);
        buffer.writeDouble(highY);
        buffer.writeDouble(highZ);
        buffer.writeLong(visualEnd);
        buffer.writeVarInt(volumeCount);
        buffer.writeVarInt(fragmentIndex);
        buffer.writeVarInt(fragmentCount);
        int pieceCount = piece.length / 6;
        buffer.writeVarInt(pieceCount);
        int cursor = 0;
        while (cursor < piece.length) {
            buffer.writeDouble(piece[cursor]);
            cursor++;
        }
    }

    public static NetherDomainSync decode(FriendlyByteBuf buffer) {
        boolean removed = buffer.readBoolean();
        UUID domainId = buffer.readUUID();
        int revision = buffer.readVarInt();
        UUID ownerId = buffer.readUUID();
        String dimensionId = buffer.readUtf();
        double lowX = buffer.readDouble();
        double lowY = buffer.readDouble();
        double lowZ = buffer.readDouble();
        double highX = buffer.readDouble();
        double highY = buffer.readDouble();
        double highZ = buffer.readDouble();
        long visualEnd = buffer.readLong();
        int volumeCount = buffer.readVarInt();
        int fragmentIndex = buffer.readVarInt();
        int fragmentCount = buffer.readVarInt();
        int pieceCount = buffer.readVarInt();
        double[] piece = new double[Math.max(0, pieceCount) * 6];
        int cursor = 0;
        while (cursor < piece.length) {
            piece[cursor] = buffer.readDouble();
            cursor++;
        }
        return new NetherDomainSync(removed, domainId, revision, ownerId, dimensionId, lowX, lowY, lowZ, highX, highY,
                highZ, visualEnd, volumeCount, fragmentIndex, fragmentCount, piece);
    }

    public static void handle(NetherDomainSync packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> NetherDomainSight.accept(packet));
        contextSupplier.get().setPacketHandled(true);
    }

    public boolean removed() {
        return removed;
    }

    public UUID domainId() {
        return domainId;
    }

    public int revision() {
        return revision;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String dimensionId() {
        return dimensionId;
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

    public long visualEnd() {
        return visualEnd;
    }

    public int volumeCount() {
        return volumeCount;
    }

    public int fragmentIndex() {
        return fragmentIndex;
    }

    public int fragmentCount() {
        return fragmentCount;
    }

    public double[] piece() {
        return piece;
    }
}
