package com.bslx.XueXiMod.client.action.skinned;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.action.RenderPoseScope;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 注册表晚就绪时做一次内存核对。断线只清渲染临时状态。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SkinAssetLifecycle {
    private SkinAssetLifecycle() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (SkinAssets.current().status() != SkinAssets.Status.NOT_READY) {
            return;
        }
        SkinAssets.promoteIfReady();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RenderPoseScope.discardOpenFrames();
        HandFrameCalibration.invalidate();
    }
}
