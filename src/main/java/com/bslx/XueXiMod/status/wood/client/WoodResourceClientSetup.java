package com.bslx.XueXiMod.status.wood.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.statusquery.ResourceStatuses;
import com.bslx.XueXiMod.status.wood.resource.SuihuaStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 物理客户端启动时登记岁华。和断线清理的 Forge 事件分开。
 * 专用服务器不加载这一类。已经登记过就跳过，避免和集成服的 ServerStarted 撞上。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WoodResourceClientSetup {
    private WoodResourceClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(WoodResourceClientSetup::registerSuihua);
    }

    public static void registerSuihua() {
        if (ResourceStatuses.find(SuihuaStatus.IDENTIFIER).isEmpty()) {
            ResourceStatuses.register(SuihuaStatus.PROVIDER);
        }
    }
}
