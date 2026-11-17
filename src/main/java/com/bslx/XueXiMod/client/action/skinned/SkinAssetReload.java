package com.bslx.XueXiMod.client.action.skinned;

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

/** 客户端资源重载只登记一次。读盘发生在应用阶段的客户端线程。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SkinAssetReload {
    private static boolean registered;

    private SkinAssetReload() {
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
            return barrier.wait(Unit.INSTANCE).thenRunAsync(SkinAssets::publishFromDevProperty, gameExecutor);
        }
    }
}
