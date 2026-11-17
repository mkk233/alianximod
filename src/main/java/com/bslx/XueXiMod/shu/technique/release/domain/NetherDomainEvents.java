package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 只登记一次。登录、换维和卸载负责把幽冥租约收干净，不在每次初始化里再注册。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class NetherDomainEvents {
    private NetherDomainEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetherDomainCycle.sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetherDomainCycle.sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer watcher) {
            NetherDomainCycle.sendSnapshot(watcher);
        }
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            NetherDomainCycle.dropLevel(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        NetherDomainCycle.clearAll();
    }
}
