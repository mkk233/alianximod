package com.bslx.XueXiMod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** 某个实体当前的共享火种余额。只供客户端显示，不回写账本。 */
public final class FireSeedVisualPacket {
    private final int entityId;
    private final UUID entityUuid;
    private final int balance;

    public FireSeedVisualPacket(int entityId, UUID entityUuid, int balance) {
        this.entityId = entityId;
        this.entityUuid = entityUuid;
        this.balance = balance;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, FireSeedVisualPacket.class, FireSeedVisualPacket::encode,
                FireSeedVisualPacket::decode, FireSeedVisualPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.entityId);
        buffer.writeUUID(this.entityUuid);
        buffer.writeVarInt(this.balance);
    }

    public static FireSeedVisualPacket decode(FriendlyByteBuf buffer) {
        return new FireSeedVisualPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt());
    }

    public static void handle(FireSeedVisualPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> FireSeedVisualBridge.accept(packet));
        contextSupplier.get().setPacketHandled(true);
    }

    public int entityId() {
        return this.entityId;
    }

    public UUID entityUuid() {
        return this.entityUuid;
    }

    public int balance() {
        return this.balance;
    }
}
