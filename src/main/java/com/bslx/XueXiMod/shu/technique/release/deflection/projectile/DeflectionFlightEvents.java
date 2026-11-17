package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.deflection.melee.MeleeDeflection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 捕获箭的生命周期。不占用 ModEvents 里已有的宿主卸载钩子。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class DeflectionFlightEvents {
    private DeflectionFlightEvents() {
    }

    /** 箭重新进入世界时，按已保存的捕获状态恢复自由飞行。 */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof AbstractArrow arrow) {
            ArrowCaptureBook.resumeLoaded(arrow);
        }
    }

    /** 箭离开世界时结束捕获并清掉这支箭的防重。 */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof AbstractArrow arrow) {
            ArrowCaptureBook.endProjectile(arrow);
        }
    }

    /** 主人死亡时放出他名下的捕获箭。 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArrowCaptureBook.releaseOwned(player.getUUID());
        }
    }

    /** 主人离线时放出他名下的捕获箭。 */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArrowCaptureBook.releaseOwned(player.getUUID());
        }
    }

    /** 主人换维时放出他名下的捕获箭。 */
    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArrowCaptureBook.releaseOwned(player.getUUID());
        }
    }

    /** 世界卸载时放出该世界里的捕获箭。 */
    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ArrowCaptureBook.releaseLevel(level);
        }
    }

    /** 停服时放出全部捕获箭并清临时账。 */
    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        ArrowCaptureBook.releaseAll();
        MeleeDeflection.closeStale();
        ArrowTickState.clear();
        ArrowBook.clearPending();
    }

    /** 每拍末清掉只在本拍有效的待用随机、记号和近战残账。 */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        ArrowBook.clearPending();
        ArrowTickState.clear();
        MeleeDeflection.closeStale();
    }
}
