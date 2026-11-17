package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CancelReason;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 攻击族跟着服务器、世界和主人的生命周期取消，不把队列留到下一局。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class CooperativeFamilyEvents {
    private CooperativeFamilyEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            CooperativeRuntime.service().tick(level, event.getServer().getTickCount());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            CooperativeRuntime.service().cancelOwner(
                    CooperativeRuntime.sessions().sessionOf(player.getServer()), player.getUUID(), CancelReason.OWNER_OFFLINE);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            CooperativeRuntime.service().cancelOwner(
                    CooperativeRuntime.sessions().sessionOf(player.getServer()), player.getUUID(), CancelReason.OWNER_DEATH);
        }
    }

    @SubscribeEvent
    public static void onCopied(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player && player.getServer() != null) {
            CancelReason reason = event.isWasDeath() ? CancelReason.OWNER_DEATH : CancelReason.OWNER_COPIED;
            CooperativeRuntime.service().cancelOwner(
                    CooperativeRuntime.sessions().sessionOf(player.getServer()), player.getUUID(), reason);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            CooperativeRuntime.service().cancelOwner(
                    CooperativeRuntime.sessions().sessionOf(player.getServer()), player.getUUID(), CancelReason.OWNER_DIMENSION);
        }
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CooperativeRuntime.service().cancelWorld(
                    CooperativeRuntime.sessions().sessionOf(level.getServer()), level.dimension(), CancelReason.REGION_UNLOADED);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        CooperativeRuntime.service().stopServer(
                CooperativeRuntime.sessions().sessionOf(event.getServer()), CancelReason.SERVER_STOP);
    }
}
