package com.bslx.XueXiMod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** 客户端已经按纠正重设位置。确认前服务端继续拒绝旧位置提案。 */
public final class FlowCorrectionAckC2SPacket {
    private final int sequence;

    public FlowCorrectionAckC2SPacket(int sequence) {
        this.sequence = sequence;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, FlowCorrectionAckC2SPacket.class, FlowCorrectionAckC2SPacket::encode,
                FlowCorrectionAckC2SPacket::decode, FlowCorrectionAckC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void send(int sequence) {
        ModNetwork.CHANNEL.sendToServer(new FlowCorrectionAckC2SPacket(sequence));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(sequence);
    }

    public static FlowCorrectionAckC2SPacket decode(FriendlyByteBuf buffer) {
        return new FlowCorrectionAckC2SPacket(buffer.readVarInt());
    }

    public static void handle(FlowCorrectionAckC2SPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> FlowPlayerBridge.acceptAck(player, packet.sequence));
        context.setPacketHandled(true);
    }
}
