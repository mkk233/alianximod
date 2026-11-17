package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.firecast.SurfaceSnapshotFrames;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** 火场、已挂燃烧和炉心成功反馈。只从服务器发到客户端，不改结算。 */
public final class FireVisualPacket {
    public static final int GROUND = 1;
    public static final int GROUND_END = 2;
    public static final int BURN = 3;
    public static final int BURN_END = 4;
    public static final int FURNACE = 5;
    public static final int SURFACE = 6;
    public static final int SURFACE_END = 7;
    public static final int SPARK = 8;

    private final int kind;
    private final long zoneId;
    private final int entityId;
    private final int burnKind;
    private final int tiers;
    private final int remainder;
    private final int segmentCount;
    private final int interval;
    private final int cursor;
    private final int wait;
    private final int furnaceCue;
    private final boolean naturalEnd;
    private final double originX;
    private final double originY;
    private final double originZ;
    private final double radius;
    private final long birthTick;
    private final long expireTick;
    private final String dimensionId;
    private final double[] slices;

    private FireVisualPacket(int kind, long zoneId, int entityId, int burnKind, int tiers, int remainder,
                             int segmentCount, int interval, int cursor, int wait, int furnaceCue,
                             boolean naturalEnd, double originX, double originY, double originZ, double radius,
                             long birthTick, long expireTick, String dimensionId, double[] slices) {
        this.kind = kind;
        this.zoneId = zoneId;
        this.entityId = entityId;
        this.burnKind = burnKind;
        this.tiers = tiers;
        this.remainder = remainder;
        this.segmentCount = segmentCount;
        this.interval = interval;
        this.cursor = cursor;
        this.wait = wait;
        this.furnaceCue = furnaceCue;
        this.naturalEnd = naturalEnd;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.radius = radius;
        this.birthTick = birthTick;
        this.expireTick = expireTick;
        this.dimensionId = dimensionId == null ? "" : dimensionId;
        this.slices = slices == null ? new double[0] : slices;
    }

    public static FireVisualPacket ground(long zoneId, String dimensionId, double originX, double originY,
                                          double originZ, double radius, long birthTick, long expireTick) {
        return new FireVisualPacket(GROUND, zoneId, -1, 0, 0, 0, 0, 0, 0, 0, 0, false,
                originX, originY, originZ, radius, birthTick, expireTick, dimensionId, null);
    }

    public static FireVisualPacket groundEnd(long zoneId) {
        return new FireVisualPacket(GROUND_END, zoneId, -1, 0, 0, 0, 0, 0, 0, 0, 0, false,
                0.0d, 0.0d, 0.0d, 0.0d, 0L, 0L, "", null);
    }

    public static FireVisualPacket burn(int entityId, int burnKind, int tiers, int remainder, int segmentCount,
                                        int interval, int cursor, int wait) {
        return new FireVisualPacket(BURN, 0L, entityId, burnKind, tiers, remainder, segmentCount, interval,
                cursor, wait, 0, false, 0.0d, 0.0d, 0.0d, 0.0d, 0L, 0L, "", null);
    }

    public static FireVisualPacket burnEnd(int entityId, int burnKind, boolean naturalEnd, int remainder,
                                           int segmentCount, int interval) {
        return new FireVisualPacket(BURN_END, 0L, entityId, burnKind, 0, remainder, segmentCount, interval,
                0, 0, 0, naturalEnd, 0.0d, 0.0d, 0.0d, 0.0d, 0L, 0L, "", null);
    }

    public static FireVisualPacket furnace(int entityId, int furnaceCue) {
        return new FireVisualPacket(FURNACE, 0L, entityId, 0, 0, 0, 0, 0, 0, 0, furnaceCue, false,
                0.0d, 0.0d, 0.0d, 0.0d, 0L, 0L, "", null);
    }

    /**
     * 分片快照。固定字段里 remainder 是几何版本，segmentCount 是分片总数，interval 是本片下标。
     * 切片字节仍只加在 SURFACE 后面，旧的 GROUND 布局不变。
     */
    public static FireVisualPacket surface(long zoneId, String dimensionId, double originX, double originY,
                                           double originZ, long birthTick, long expireTick, int style,
                                           int geometryVersion, int fragmentIndex, int fragmentCount, double[] slices) {
        return new FireVisualPacket(SURFACE, zoneId, -1, 0, 0, geometryVersion, fragmentCount, fragmentIndex, 0, 0, style, false,
                originX, originY, originZ, 0.0d, birthTick, expireTick, dimensionId, slices);
    }

    /** 结束包沿用已有的维度、出生和到期字段，客户端靠原来的到期时刻挡住迟到分片。 */
    public static FireVisualPacket surfaceEnd(long zoneId, String dimensionId, long birthTick, long expireTick) {
        String dimension = dimensionId == null ? "" : dimensionId;
        return new FireVisualPacket(SURFACE_END, zoneId, -1, 0, 0, 0, 0, 0, 0, 0, 0, false,
                0.0d, 0.0d, 0.0d, 0.0d, birthTick, expireTick, dimension, null);
    }

    public static FireVisualPacket spark(int entityId, double originX, double originY, double originZ, int style) {
        return new FireVisualPacket(SPARK, 0L, entityId, 0, 0, 0, 0, 0, 0, 0, style, false,
                originX, originY, originZ, 0.0d, 0L, 0L, "", null);
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, FireVisualPacket.class, FireVisualPacket::encode, FireVisualPacket::decode,
                FireVisualPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.kind);
        buffer.writeLong(this.zoneId);
        buffer.writeVarInt(this.entityId);
        buffer.writeVarInt(this.burnKind);
        buffer.writeVarInt(this.tiers);
        buffer.writeVarInt(this.remainder);
        buffer.writeVarInt(this.segmentCount);
        buffer.writeVarInt(this.interval);
        buffer.writeVarInt(this.cursor);
        buffer.writeVarInt(this.wait);
        buffer.writeVarInt(this.furnaceCue);
        buffer.writeBoolean(this.naturalEnd);
        buffer.writeDouble(this.originX);
        buffer.writeDouble(this.originY);
        buffer.writeDouble(this.originZ);
        buffer.writeDouble(this.radius);
        buffer.writeLong(this.birthTick);
        buffer.writeLong(this.expireTick);
        buffer.writeUtf(this.dimensionId);
        if (this.kind == SURFACE) {
            int sliceCount = this.slices.length / 6;
            if (sliceCount < 0 || sliceCount > SurfaceSnapshotFrames.PER_PACKET_SLICES || this.slices.length % 6 != 0) {
                buffer.writeVarInt(SurfaceSnapshotFrames.PER_PACKET_SLICES + 1);
            } else {
                buffer.writeVarInt(sliceCount);
                for (int slot = 0; slot < this.slices.length; slot++) {
                    buffer.writeDouble(this.slices[slot]);
                }
            }
        }
    }

    public static FireVisualPacket decode(FriendlyByteBuf buffer) {
        int kind = buffer.readVarInt();
        long zoneId = buffer.readLong();
        int entityId = buffer.readVarInt();
        int burnKind = buffer.readVarInt();
        int tiers = buffer.readVarInt();
        int remainder = buffer.readVarInt();
        int segmentCount = buffer.readVarInt();
        int interval = buffer.readVarInt();
        int cursor = buffer.readVarInt();
        int wait = buffer.readVarInt();
        int furnaceCue = buffer.readVarInt();
        boolean naturalEnd = buffer.readBoolean();
        double originX = buffer.readDouble();
        double originY = buffer.readDouble();
        double originZ = buffer.readDouble();
        double radius = buffer.readDouble();
        long birthTick = buffer.readLong();
        long expireTick = buffer.readLong();
        String dimensionId = buffer.readUtf();
        double[] slices = null;
        int surfaceFragments = segmentCount;
        if (kind == SURFACE) {
            int sliceCount = buffer.readVarInt();
            if (sliceCount < 0 || sliceCount > SurfaceSnapshotFrames.PER_PACKET_SLICES) {
                slices = new double[0];
                surfaceFragments = 0;
            } else {
                slices = new double[sliceCount * 6];
                for (int slot = 0; slot < slices.length; slot++) {
                    slices[slot] = buffer.readDouble();
                }
            }
        }
        return new FireVisualPacket(kind, zoneId, entityId, burnKind, tiers, remainder, surfaceFragments, interval,
                cursor, wait, furnaceCue, naturalEnd, originX, originY, originZ, radius, birthTick, expireTick,
                dimensionId, slices);
    }

    public static void handle(FireVisualPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> FireVisualBridge.accept(packet));
        contextSupplier.get().setPacketHandled(true);
    }

    public int kind() {
        return this.kind;
    }

    public long zoneId() {
        return this.zoneId;
    }

    public int entityId() {
        return this.entityId;
    }

    public int burnKind() {
        return this.burnKind;
    }

    public int tiers() {
        return this.tiers;
    }

    public int remainder() {
        return this.remainder;
    }

    public int segmentCount() {
        return this.segmentCount;
    }

    public int interval() {
        return this.interval;
    }

    public int cursor() {
        return this.cursor;
    }

    public int waitTicks() {
        return this.wait;
    }

    public int furnaceCue() {
        return this.furnaceCue;
    }

    public boolean naturalEnd() {
        return this.naturalEnd;
    }

    public double originX() {
        return this.originX;
    }

    public double originY() {
        return this.originY;
    }

    public double originZ() {
        return this.originZ;
    }

    public double radius() {
        return this.radius;
    }

    public long birthTick() {
        return this.birthTick;
    }

    public long expireTick() {
        return this.expireTick;
    }

    public String dimensionId() {
        return this.dimensionId;
    }

    public double[] slices() {
        return this.slices;
    }
}
