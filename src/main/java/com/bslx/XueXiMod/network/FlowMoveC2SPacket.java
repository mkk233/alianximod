package com.bslx.XueXiMod.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** 一次客户端移动加上已经消费的流场回执。服务端校验后进入原版移动处理。 */
public final class FlowMoveC2SPacket {
    public static final int PLAYER_PLACE_TURN = 0;
    public static final int PLAYER_PLACE = 1;
    public static final int PLAYER_FACING = 2;
    public static final int PLAYER_STATUS = 3;
    public static final int VEHICLE = 4;

    private final int kind;
    private final double worldX;
    private final double worldY;
    private final double worldZ;
    private final float yaw;
    private final float pitch;
    private final boolean onGround;
    private final UUID instanceId;
    private final UUID rootId;
    private final long epoch;
    private final long step;
    private final boolean rising;
    private final double nativeVertical;
    private final boolean playerJumped;

    public FlowMoveC2SPacket(int kind, double worldX, double worldY, double worldZ, float yaw, float pitch, boolean onGround,
                             UUID instanceId, UUID rootId, long epoch, long step, boolean rising, double nativeVertical,
                             boolean playerJumped) {
        this.kind = kind;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.instanceId = instanceId;
        this.rootId = rootId;
        this.epoch = epoch;
        this.step = step;
        this.rising = rising;
        this.nativeVertical = nativeVertical;
        this.playerJumped = playerJumped;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, FlowMoveC2SPacket.class, FlowMoveC2SPacket::encode, FlowMoveC2SPacket::decode,
                FlowMoveC2SPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void send(FlowMoveC2SPacket packet) {
        ModNetwork.CHANNEL.sendToServer(packet);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(kind);
        buffer.writeDouble(worldX);
        buffer.writeDouble(worldY);
        buffer.writeDouble(worldZ);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeBoolean(onGround);
        buffer.writeUUID(instanceId);
        buffer.writeUUID(rootId);
        buffer.writeLong(epoch);
        buffer.writeLong(step);
        buffer.writeBoolean(rising);
        buffer.writeDouble(nativeVertical);
        buffer.writeBoolean(playerJumped);
    }

    public static FlowMoveC2SPacket decode(FriendlyByteBuf buffer) {
        return new FlowMoveC2SPacket(buffer.readByte(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(), buffer.readUUID(), buffer.readUUID(),
                buffer.readLong(), buffer.readLong(), buffer.readBoolean(), buffer.readDouble(), buffer.readBoolean());
    }

    public static void handle(FlowMoveC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (packet.kind == VEHICLE) {
                FlowPlayerBridge.acceptVehicle(player, packet);
            } else {
                FlowPlayerBridge.acceptPlayer(player, packet);
            }
        });
        context.setPacketHandled(true);
    }

    public boolean matchesPlayer(ServerPlayer player) {
        return player != null && player.getUUID().equals(rootId);
    }

    public boolean matchesRoot(Entity root) {
        return root != null && root.getUUID().equals(rootId);
    }

    public ServerboundMovePlayerPacket toPlayerPacket(ServerPlayer player) {
        return switch (kind) {
            case PLAYER_PLACE -> new ServerboundMovePlayerPacket.Pos(worldX, worldY, worldZ, onGround);
            case PLAYER_FACING -> new ServerboundMovePlayerPacket.Rot(yaw, pitch, onGround);
            case PLAYER_STATUS -> new ServerboundMovePlayerPacket.StatusOnly(onGround);
            default -> new ServerboundMovePlayerPacket.PosRot(worldX, worldY, worldZ, yaw, pitch, onGround);
        };
    }

    public ServerboundMoveVehiclePacket toVehiclePacket() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeDouble(worldX);
        buffer.writeDouble(worldY);
        buffer.writeDouble(worldZ);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        ServerboundMoveVehiclePacket packet = new ServerboundMoveVehiclePacket(buffer);
        buffer.release();
        return packet;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public UUID rootId() {
        return rootId;
    }

    public long epoch() {
        return epoch;
    }

    public long step() {
        return step;
    }

    public boolean hasRise() {
        return rising;
    }

    public double nativeVertical() {
        return nativeVertical;
    }

    public boolean playerJumped() {
        return playerJumped;
    }
}
