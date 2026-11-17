package com.bslx.XueXiMod.shu.balance;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** 只携带服务端已经接受的覆盖值，不含有效结果，避免客户端再乘一次全局倍率。 */
public final class BalanceSyncPacket {
    private final byte[] payload;

    public BalanceSyncPacket(byte[] payload) {
        this.payload = payload == null ? new byte[0] : payload;
    }

    public static void encode(BalanceSyncPacket message, FriendlyByteBuf buffer) {
        buffer.writeByteArray(message.payload);
    }

    public static BalanceSyncPacket decode(FriendlyByteBuf buffer) {
        return new BalanceSyncPacket(buffer.readByteArray());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        byte[] accepted = payload;
        context.enqueueWork(() -> BalanceLifecycle.applyClientPayload(accepted));
        context.setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int packetId) {
        channel.registerMessage(packetId,
                BalanceSyncPacket.class,
                BalanceSyncPacket::encode,
                BalanceSyncPacket::decode,
                BalanceSyncPacket::handle);
    }
}
