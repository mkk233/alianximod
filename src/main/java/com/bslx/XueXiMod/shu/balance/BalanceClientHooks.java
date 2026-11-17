package com.bslx.XueXiMod.shu.balance;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 只在客户端加载。专用服务器不引用这个类。
 * 客户端线程提前绑到客户端，这样集成服务器装上服务端快照后，界面不会误读另一侧。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BalanceClientHooks {
    private BalanceClientHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> BalanceService.bind(BalanceSide.CLIENT));
    }
}
