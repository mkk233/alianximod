package com.bslx.XueXiMod.status.fire.furnace;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

/** 能改炉心存档、效果、属性或伤害的入口必须在服务端主线程。 */
final class FurnaceThreadGate {
    private FurnaceThreadGate() {
    }

    /** 是不是服务端，并且当前就在该服务器线程上。 */
    static boolean onServerThread(Player player) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }
        MinecraftServer host = player.level().getServer();
        boolean sameThread = host != null && host.isSameThread();
        return FurnaceMath.allowsServerWork(host != null, sameThread);
    }
}
