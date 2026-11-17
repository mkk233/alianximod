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
    public enum Action {
        IMPRINT,
        STUDY
    }

    private final String techniqueId;
    private final Action action;

    public ImprintPacket(String techniqueId) {
        this(techniqueId, Action.IMPRINT);
    }

    public ImprintPacket(String techniqueId, Action action) {
        this.techniqueId = techniqueId;
        this.action = action == null ? Action.IMPRINT : action;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(techniqueId);
        buf.writeEnum(action);
    }

    public static ImprintPacket decode(FriendlyByteBuf buf) {
        String techniqueId = buf.readUtf();
        Action action = buf.isReadable() ? buf.readEnum(Action.class) : Action.IMPRINT;
        return new ImprintPacket(techniqueId, action);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            if (player.containerMenu instanceof ImprintDeskMenu menu) {
                if (!menu.stillValid(player)) return;
                if (action == Action.STUDY) {
                    menu.study(player, techniqueId);
                } else {
                    menu.imprint(player, techniqueId);
                }
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int packetId) {
        channel.registerMessage(packetId,
                ImprintPacket.class,
                ImprintPacket::encode,
                ImprintPacket::decode,
                ImprintPacket::handle
        );
    }

    public static class Registration implements NetworkPacket {
        @Override
        public void register(SimpleChannel channel, int packetId) {
            ImprintPacket.register(channel, packetId);
        }
    }
}
