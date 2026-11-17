package com.bslx.XueXiMod.shu.technique.release.capture;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 当前这一局、这一个服务器 tick 里已经成功部署的主人。
 * 只在生成被接受后写入。服务器实例不同或 tick 不同都不再沿用旧记录。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class CaptureAcceptanceLedger {
    private static MinecraftServer boundServer;
    private static int boundTick;
    private static final Set<UUID> acceptedOwners = new HashSet<>();

    private CaptureAcceptanceLedger() {
    }

    static boolean alreadyAccepted(UUID ownerId, MinecraftServer server, int gameTick) {
        if (ownerId == null || server == null || !sameWindow(server, gameTick)) {
            discardOtherWindow(server, gameTick);
            return false;
        }
        return acceptedOwners.contains(ownerId);
    }

    static void record(UUID ownerId, MinecraftServer server, int gameTick) {
        if (ownerId == null || server == null) {
            return;
        }
        if (!sameWindow(server, gameTick)) {
            acceptedOwners.clear();
            boundServer = server;
            boundTick = gameTick;
        }
        acceptedOwners.add(ownerId);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (boundServer == null) {
            return;
        }
        if (event.phase != TickEvent.Phase.START && event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        boolean otherServer = server != boundServer;
        boolean otherTick = server.getTickCount() != boundTick;
        boolean tickFinished = event.phase == TickEvent.Phase.END
                && server == boundServer
                && server.getTickCount() == boundTick;
        if (otherServer || otherTick || tickFinished) {
            releaseWindow();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (event.getServer() != null) {
            releaseWindow();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }
        acceptedOwners.remove(owner.getUUID());
        if (acceptedOwners.isEmpty()) {
            releaseWindow();
        }
    }

    private static boolean sameWindow(MinecraftServer server, int gameTick) {
        return boundServer == server && boundTick == gameTick;
    }

    private static void discardOtherWindow(MinecraftServer server, int gameTick) {
        if (boundServer == null) {
            return;
        }
        if (server == null || boundServer != server || boundTick != gameTick) {
            releaseWindow();
        }
    }

    private static void releaseWindow() {
        acceptedOwners.clear();
        boundServer = null;
        boundTick = 0;
    }
}
