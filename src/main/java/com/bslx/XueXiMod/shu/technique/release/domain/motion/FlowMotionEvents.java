package com.bslx.XueXiMod.shu.technique.release.domain.motion;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.FlowPlayerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 流场实体事件服务。死亡、离维和换维都退役这个根的流场，不留世界级黑名单。
 * 球形场地和护体不登记这份持续位移意图。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class FlowMotionEvents {
    private FlowMotionEvents() {
    }

    @SubscribeEvent
    /** 实体离维时退役它的流场。不退水势，也不关闭领域。 */
    public static void onLeave(EntityLeaveLevelEvent event) {
        FlowMotionIntents.retireEntity(event.getEntity());
    }

    @SubscribeEvent
    /** 实体死亡时退役它的流场。不退水势，也不关闭领域。 */
    public static void onDeath(LivingDeathEvent event) {
        FlowMotionIntents.retireEntity(event.getEntity());
    }

    @SubscribeEvent
    /** 玩家换维时退役流场并清玩家桥。不清其他实体的意图。 */
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        FlowMotionIntents.retireEntity(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            FlowPlayerBridge.clear(serverPlayer);
        }
    }

    @SubscribeEvent
    /** 玩家下线时退役流场并清玩家桥。不保留跨线状态。 */
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            FlowMotionIntents.retireEntity(serverPlayer);
            FlowPlayerBridge.clear(serverPlayer);
        }
    }
}
