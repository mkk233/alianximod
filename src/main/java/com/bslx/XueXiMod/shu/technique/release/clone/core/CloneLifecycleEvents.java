package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class CloneLifecycleEvents {
    private CloneLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CloneRuntime.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CloneRuntime.stop(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            CloneRuntime runtime = CloneRuntime.get(event.getServer());
            if (runtime != null) {
                runtime.advanceTick();
            }
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        CloneRuntime runtime = CloneRuntime.get(event.getServer());
        if (runtime != null) {
            runtime.drain();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            CloneService.endOwned(owner, CloneEndReason.OWNER_LOGOUT);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof CloneEntity actor) {
            CloneService.endLoaded(actor, CloneEndReason.DEATH);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer owner) {
            CloneService.endOwned(owner, CloneEndReason.OWNER_DEATH);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original)) {
            return;
        }
        CloneEndReason reason = event.isWasDeath() ? CloneEndReason.OWNER_DEATH : CloneEndReason.OWNER_DIMENSION;
        CloneService.endOwned(original, reason);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            CloneService.endOwned(owner, CloneEndReason.OWNER_DIMENSION);
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof CloneEntity actor)) {
            return;
        }
        CloneService.endLoaded(actor, CloneEndReason.UNLOAD);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CloneService.endDimension(level, CloneEndReason.WORLD_UNLOAD);
        }
    }
}
