package com.bslx.XueXiMod.shu.technique.query;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 绑定服务器、tick 边界丢掉候选引用，世界卸载和停服时清掉缓存。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class LivingCandidateEvents {
    private LivingCandidateEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LivingCandidateQueries.bindServer(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LivingCandidateQueries.releaseServer(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTickStart(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        LivingCandidateQueries.expireTick(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LivingCandidateQueries.expireTick(event.getServer());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            LivingCandidateQueries.unloadLevel(level);
        }
    }
}
