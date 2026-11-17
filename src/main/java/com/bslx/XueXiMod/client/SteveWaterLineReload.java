package com.bslx.XueXiMod.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 资源重载时重读水线网格。平时只用已经缓存的方体和曲线。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SteveWaterLineReload {
    private static boolean registered;

    private SteveWaterLineReload() {
    }

    @SubscribeEvent
    public static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        if (registered) {
            return;
        }
        registered = true;
        event.registerReloadListener(new Listener());
    }

    private static final class Listener implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(
                PreparableReloadListener.PreparationBarrier barrier,
                ResourceManager resourceManager,
                ProfilerFiller preparations,
                ProfilerFiller reloadProfiler,
                Executor backgroundExecutor,
                Executor gameExecutor) {
            return barrier.wait(Unit.INSTANCE).thenRunAsync(
                    () -> SteveWaterLineMesh.publish(resourceManager), gameExecutor);
        }
    }
}
