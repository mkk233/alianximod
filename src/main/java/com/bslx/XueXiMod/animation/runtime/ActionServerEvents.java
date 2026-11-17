package com.bslx.XueXiMod.animation.runtime;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.ActionSyncS2CPacket;
import com.bslx.XueXiMod.network.ModNetwork;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 死亡、离线、换维和实体离开时清掉姿态。不写 NBT。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class ActionServerEvents {
    private ActionServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ActionRuntime.finishExpired(ServerLifecycleHooks.getCurrentServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ActionRuntime.releaseServer();
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer watcher) || !(event.getTarget() instanceof ServerPlayer owner)) {
            return;
        }
        ActionSyncS2CPacket packet = ActionRuntime.snapshot(owner);
        if (packet != null) {
            ModNetwork.sendToPlayer(watcher, packet);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ActionRuntime.retire(player, player.serverLevel());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ActionRuntime.retire(player, player.serverLevel());
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            ServerLevel originLevel = player.getServer().getLevel(event.getFrom());
            ActionRuntime.retire(player, originLevel != null ? originLevel : player.serverLevel());
        }
    }

    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel serverLevel
                && (player.isRemoved() || !player.isAlive())) {
            ActionRuntime.retire(player, serverLevel);
        }
    }
}
