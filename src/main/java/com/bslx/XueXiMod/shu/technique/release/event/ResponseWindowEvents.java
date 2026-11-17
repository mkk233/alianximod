package com.bslx.XueXiMod.shu.technique.release.event;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 事件窗口的生命周期。死亡、离线、换维和停止时撤销，正常到期由每拍检查。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class ResponseWindowEvents {
    private ResponseWindowEvents() {
    }

    /** 每拍结束时收掉已到期或失效的窗口。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        ResponseWindowService.tick(event.getServer());
    }

    /** 死亡时撤销这位玩家的窗口。 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResponseWindowService.close(player.getUUID());
        }
    }

    /** 离线时撤销这位玩家的窗口。 */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResponseWindowService.close(player.getUUID());
        }
    }

    /** 换维时撤销这位玩家的窗口。 */
    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResponseWindowService.close(player.getUUID());
        }
    }

    /** 停止时撤销全部窗口。 */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ResponseWindowService.clear();
    }
}
