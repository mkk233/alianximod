package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** 来源失效后的位置纠正。不使用会清掉非相对轴速度的原版绝对传送包。 */
public final class FlowCorrectionS2CPacket {
    private final UUID rootId;
    private final int sequence;
    private final double worldX;
    private final double worldY;
    private final double worldZ;
    private final float yaw;
    private final float pitch;

    public FlowCorrectionS2CPacket(UUID rootId, int sequence, double worldX, double worldY, double worldZ, float yaw,
                                   float pitch) {
        this.rootId = rootId;
        this.sequence = sequence;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, FlowCorrectionS2CPacket.class, FlowCorrectionS2CPacket::encode,
                FlowCorrectionS2CPacket::decode, FlowCorrectionS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerPlayer player, Entity root, int sequence) {
        if (player == null || root == null) {
            return;
        }
        ModNetwork.sendToPlayer(player, new FlowCorrectionS2CPacket(root.getUUID(), sequence, root.getX(), root.getY(),
                root.getZ(), root.getYRot(), root.getXRot()));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(rootId);
        buffer.writeVarInt(sequence);
        buffer.writeDouble(worldX);
        buffer.writeDouble(worldY);
        buffer.writeDouble(worldZ);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
    }

    public static FlowCorrectionS2CPacket decode(FriendlyByteBuf buffer) {
        return new FlowCorrectionS2CPacket(buffer.readUUID(), buffer.readVarInt(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(FlowCorrectionS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> FlowClientBridge.acceptCorrection(packet.rootId, packet.sequence, packet.worldX,
                packet.worldY, packet.worldZ, packet.yaw, packet.pitch));
        context.setPacketHandled(true);
    }
}
