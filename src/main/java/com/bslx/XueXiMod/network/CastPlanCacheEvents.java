package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 服务端启动时绑定计划缓存，停服时清空并丢掉引用。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class CastPlanCacheEvents {
    private CastPlanCacheEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CastPlanCache.bindServer(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CastPlanCache.releaseServer(event.getServer());
    }
}
