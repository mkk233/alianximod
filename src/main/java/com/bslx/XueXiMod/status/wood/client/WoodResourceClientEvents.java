package com.bslx.XueXiMod.status.wood.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.status.wood.resource.client.SuihuaClientCache;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 只在客户端加载。断线清掉岁华缓存，专用服务器不会加载这一类。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WoodResourceClientEvents {
    private WoodResourceClientEvents() {
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut logoutEvent) {
        if (logoutEvent == null) {
            return;
        }
        SuihuaClientCache.clearAll();
    }
}
