package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.status.wood.resource.SuihuaSnapshot;
import com.bslx.XueXiMod.status.wood.resource.client.SuihuaClientCache;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 只从服务端发给本人。载荷是主人 UUID 和岁华七项绝对整数。
 * 不引用 Minecraft 客户端类；专用服务器加载本类时不会去拉客户端。
 */
public final class SuihuaSyncPacket {
    private static final int LEGACY_PACKET_BYTES = 40;
    private static final int PACKET_BYTES = 44;

    private final UUID ownerId;
    private final int peach;
    private final int bamboo;
    private final int wisteria;
    private final int plum;
    private final int orchid;
    private final int higan;
    private final int rose;

    public SuihuaSyncPacket(UUID ownerId, int peach, int bamboo, int wisteria, int plum, int orchid, int higan, int rose) {
        this.ownerId = ownerId;
        this.peach = peach;
        this.bamboo = bamboo;
        this.wisteria = wisteria;
        this.plum = plum;
        this.orchid = orchid;
        this.higan = higan;
        this.rose = rose;
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, SuihuaSyncPacket.class, SuihuaSyncPacket::encode,
                SuihuaSyncPacket::decode, SuihuaSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerPlayer player, UUID ownerId, SuihuaSnapshot snapshot) {
        if (player == null || ownerId == null || snapshot == null) {
            return;
        }
        ModNetwork.sendToPlayer(player, new SuihuaSyncPacket(ownerId, snapshot.peach(), snapshot.bamboo(),
                snapshot.wisteria(), snapshot.plum(), snapshot.orchid(), snapshot.higan(), snapshot.rose()));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(ownerId);
        buffer.writeInt(peach);
        buffer.writeInt(bamboo);
        buffer.writeInt(wisteria);
        buffer.writeInt(plum);
        buffer.writeInt(orchid);
        buffer.writeInt(higan);
        buffer.writeInt(rose);
    }

    public static SuihuaSyncPacket decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() < LEGACY_PACKET_BYTES) {
            throw new DecoderException("suihua sync truncated");
        }
        UUID ownerId = buffer.readUUID();
        int peach = buffer.readInt();
        int bamboo = buffer.readInt();
        int wisteria = buffer.readInt();
        int plum = buffer.readInt();
        int orchid = buffer.readInt();
        int higan = buffer.readInt();
        int rose = buffer.readableBytes() >= Integer.BYTES ? buffer.readInt() : 0;
        return new SuihuaSyncPacket(ownerId, peach, bamboo, wisteria, plum, orchid, higan, rose);
    }

    public static void handle(SuihuaSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (packet == null || packet.ownerId == null || context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            context.setPacketHandled(true);
            return;
        }
        UUID ownerId = packet.ownerId;
        int peach = packet.peach;
        int bamboo = packet.bamboo;
        int wisteria = packet.wisteria;
        int plum = packet.plum;
        int orchid = packet.orchid;
        int higan = packet.higan;
        int rose = packet.rose;
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SuihuaClientCache.replace(ownerId, peach, bamboo, wisteria, plum, orchid, higan, rose)));
        context.setPacketHandled(true);
    }
}
