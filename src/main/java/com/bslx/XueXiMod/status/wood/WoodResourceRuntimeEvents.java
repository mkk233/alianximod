package com.bslx.XueXiMod.status.wood;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.SuihuaSyncPacket;
import com.bslx.XueXiMod.shu.technique.release.statusquery.ResourceStatuses;
import com.bslx.XueXiMod.status.wood.resource.SuihuaSnapshot;
import com.bslx.XueXiMod.status.wood.resource.SuihuaStatus;
import com.bslx.XueXiMod.status.wood.resource.WoodResourceService;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * 木系资源的公共事件接线。只调用资源作者的公开入口，不改账本。
 * 不订阅 PlayerEvent.Clone，避免把已清掉的岁华抄回新身体。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class WoodResourceRuntimeEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static MinecraftServer activeServer;
    private static boolean worldReady;
    private static boolean sinkInstalled;
    private static boolean providerRegistered;

    private WoodResourceRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        activeServer = server;
        worldReady = server != null;
        installSinkOnce();
        registerSuihuaOnce();
        runQuiet("启动", () -> WoodResourceService.onServerStart(server));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!worldReady || event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        activeServer = server;
        installSinkOnce();
        runQuiet("tick", () -> WoodResourceService.onServerTick(server));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOwnerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            rememberServer(player);
            runQuiet("死亡", () -> WoodResourceService.onOwnerDeath(player));
        }
    }

    @SubscribeEvent
    public static void onOwnerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            rememberServer(player);
            runQuiet("登出", () -> WoodResourceService.onOwnerLogout(player));
        }
    }

    @SubscribeEvent
    public static void onOwnerDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        rememberServer(player);
        runQuiet("换维", () -> WoodResourceService.onOwnerChangeDimension(player));
        sendCurrent(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            rememberServer(player);
            sendCurrent(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            rememberServer(player);
            sendCurrent(player);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getChunk() == null) {
            return;
        }
        rememberServer(level.getServer());
        runQuiet("区块卸载", () -> WoodResourceService.onChunkUnload(level, event.getChunk().getPos()));
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        rememberServer(level.getServer());
        runQuiet("维度卸载", () -> WoodResourceService.onLevelUnload(level));
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        activeServer = server;
        runQuiet("停止", () -> WoodResourceService.onServerStop(server));
        worldReady = false;
        activeServer = null;
    }

    private static void installSinkOnce() {
        if (sinkInstalled) {
            return;
        }
        WoodResourceService.installBalanceSink(WoodResourceRuntimeEvents::notifyBalance);
        sinkInstalled = true;
    }

    private static void registerSuihuaOnce() {
        if (providerRegistered) {
            return;
        }
        if (ResourceStatuses.find(SuihuaStatus.IDENTIFIER).isEmpty()) {
            ResourceStatuses.register(SuihuaStatus.PROVIDER);
        }
        providerRegistered = true;
    }

    private static void notifyBalance(UUID ownerId, SuihuaSnapshot snapshot) {
        MinecraftServer server = activeServer;
        if (server == null || ownerId == null || snapshot == null) {
            return;
        }
        if (!WoodResourceService.ledgerWritable(server)) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
        if (player == null) {
            return;
        }
        SuihuaSyncPacket.send(player, ownerId, snapshot);
    }

    private static void sendCurrent(ServerPlayer player) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null || !WoodResourceService.ledgerWritable(server)) {
            return;
        }
        try {
            SuihuaSnapshot snapshot = WoodResourceService.readSuihua(server, player.getUUID());
            SuihuaSyncPacket.send(player, player.getUUID(), snapshot);
        } catch (RuntimeException failure) {
            LOGGER.warn("岁华登录同步失败，不把未确认余额写成新档", failure);
        }
    }

    private static void rememberServer(ServerPlayer player) {
        if (player != null) {
            rememberServer(player.getServer());
        }
    }

    private static void rememberServer(MinecraftServer server) {
        if (server != null) {
            activeServer = server;
        }
    }

    private static void runQuiet(String action, Runnable work) {
        try {
            work.run();
        } catch (Throwable failure) {
            LOGGER.error("木系资源{}未完成，不追加退还或重发奖", action, failure);
        }
    }
}
