package com.bslx.XueXiMod.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** 燎里后环已经触发。只告诉客户端从哪一拍开始画，不延长命中体。 */
public final class LiaoliRingPacket {
    private final UUID casterId;
    private final long birthTick;

    public LiaoliRingPacket(UUID casterId, long birthTick) {
        this.casterId = casterId;
        this.birthTick = birthTick;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, LiaoliRingPacket.class, LiaoliRingPacket::encode,
                LiaoliRingPacket::decode, LiaoliRingPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** 发给正在看这位施法者的客户端，包括施法者自己。 */
    public static void send(ServerPlayer player) {
        if (player == null || player.level() == null || player.level().isClientSide()) {
            return;
        }
        long birthTick = player.level().getGameTime();
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new LiaoliRingPacket(player.getUUID(), birthTick));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.casterId);
        buffer.writeLong(this.birthTick);
    }

    public static LiaoliRingPacket decode(FriendlyByteBuf buffer) {
        return new LiaoliRingPacket(buffer.readUUID(), buffer.readLong());
    }

    public static void handle(LiaoliRingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> LiaoliRingBridge.accept(packet));
        contextSupplier.get().setPacketHandled(true);
    }

    public UUID casterId() {
        return this.casterId;
    }

    public long birthTick() {
        return this.birthTick;
    }
}
