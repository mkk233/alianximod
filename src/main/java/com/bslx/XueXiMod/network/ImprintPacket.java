package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.gui.ImprintDeskMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 客户端确认刻印：把选中的术 ID 发给服务端。
 */
public class ImprintPacket {
    private final String techniqueId;

    public ImprintPacket(String techniqueId) {
        this.techniqueId = techniqueId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(techniqueId);
    }

    public static ImprintPacket decode(FriendlyByteBuf buf) {
        return new ImprintPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.containerMenu instanceof ImprintDeskMenu menu) {
                if (!menu.stillValid(player)) return;
                menu.imprint(player, techniqueId);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int id) {
        channel.registerMessage(id,
                ImprintPacket.class,
                ImprintPacket::encode,
                ImprintPacket::decode,
                ImprintPacket::handle
        );
    }

    public static class Registration implements NetworkPacket {
        @Override
        public void register(SimpleChannel channel, int id) {
            ImprintPacket.register(channel, id);
        }
    }
}
