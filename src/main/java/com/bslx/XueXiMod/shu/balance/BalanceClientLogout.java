package com.bslx.XueXiMod.shu.balance;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 断线只清客户端快照。集成服务器的服务端快照留到服务端停止时再清。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BalanceClientLogout {
    private BalanceClientLogout() {
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BalanceService.clear(BalanceSide.CLIENT);
        BalanceService.bind(BalanceSide.CLIENT);
    }
}
