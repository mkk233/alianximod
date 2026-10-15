package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络通道注册
 * 登记顺序：SkillUsePacket（技使用）、ShuSyncPacket（术数据同步）、
 * OpenShuGuiPacket（打开术管理）、ImprintPacket（刻印确认）
 */
public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void register() {
        SkillUsePacket.register(CHANNEL, 0);
        ShuSyncPacket.register(CHANNEL, 1);
        OpenShuGuiPacket.register(CHANNEL, 2);
        ImprintPacket.register(CHANNEL, 3);
        SkillAssemblyPacket.register(CHANNEL, 4);
        BoundSkillSyncPacket.register(CHANNEL, 5);
    }
}
