package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.gui.ModMenuTypes;
import com.bslx.XueXiMod.gui.ShuManagementMenu;
import com.bslx.XueXiMod.player.PlayerShuData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 打开术管理GUI请求包 - 客户端→服务端
 * M键按下后发送，服务端打开 ShuManagementMenu
 */
public class OpenShuGuiPacket {

    public OpenShuGuiPacket() {}

    public void encode(FriendlyByteBuf buf) {
        // 无数据
    }

    public static OpenShuGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenShuGuiPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 先同步最新数据到客户端
            PlayerShuData data = PlayerShuData.get(player);
            ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));

            // 打开GUI
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("gui.xueximod.shu_management");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player p) {
                    return new ShuManagementMenu(containerId, playerInv);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int id) {
        channel.registerMessage(id,
                OpenShuGuiPacket.class,
                OpenShuGuiPacket::encode,
                OpenShuGuiPacket::decode,
                OpenShuGuiPacket::handle
        );
    }

    public static class Registration implements NetworkPacket {
        @Override
        public void register(SimpleChannel channel, int id) {
            OpenShuGuiPacket.register(channel, id);
        }
    }
}
