package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.water.CarrierWatch;
import com.bslx.XueXiMod.shu.technique.release.water.WaterBeatLedger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 登出和停服时关掉还开着的父释放，避免回执表跨服一直留着。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class HostCastEvents {
    private HostCastEvents() {
    }

    /** 开服时清掉残留父释放和拍号。 */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (event.getServer() != null) {
            HostCast.closeAll();
            clearBeats(null);
        }
    }

    /** 停服时清掉父释放和拍号。 */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (event.getServer() != null) {
            HostCast.closeAll();
            clearBeats(null);
        }
    }

    /** 服务端玩家每拍观察主手和所选快捷栏。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.level().isClientSide()) {
            return;
        }
        CarrierWatch.observe(serverPlayer.getUUID(), serverPlayer.getMainHandItem(), serverPlayer.getInventory().selected);
    }

    /** 登出时关闭该玩家的父释放和拍号。 */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) {
            return;
        }
        HostCast.closeOwned(event.getEntity().getUUID());
        clearBeats(event.getEntity().getUUID());
    }

    /** 换维时清拍号，不关闭父释放。 */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() == null) {
            return;
        }
        clearBeats(event.getEntity().getUUID());
    }

    /** 玩家死亡时清拍号。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        clearBeats(player.getUUID());
    }

    /** 死亡克隆时按原玩家清拍号。 */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || event.getOriginal() == null) {
            return;
        }
        clearBeats(event.getOriginal().getUUID());
    }

    private static void clearBeats(java.util.UUID playerId) {
        if (playerId == null) {
            CarrierWatch.clearAll();
            WaterBeatLedger.clearAll();
            return;
        }
        CarrierWatch.clearPlayer(playerId);
        WaterBeatLedger.clearPlayer(playerId);
    }
}
