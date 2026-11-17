package com.bslx.XueXiMod.debug.performance;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 公共 Forge 总线上的性能记录生命周期。不限制 Dist，因此集成服和专用服都会加载。
 * 不引用客户端类。同优先级监听器的先后不保证，观测窗口不能当成盖住了所有同优先级监听器。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PerformanceEvents {
    private PerformanceEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        Thread serverThread = server.getRunningThread();
        if (serverThread == null) {
            serverThread = Thread.currentThread();
        }
        PerformanceRecorder.bindFreshServer(server, serverThread);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PerformanceRecorder.releaseServer(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTickStart(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (!PerformanceRecorder.sameServer(event.getServer())) {
            return;
        }
        PerformanceRecorder.onTickStart();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!PerformanceRecorder.sameServer(event.getServer())) {
            return;
        }
        PerformanceRecorder.onTickEnd();
    }
}
