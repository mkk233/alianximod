package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 死亡清火种。玩家死亡克隆不再抄回来。
 * 末地回主世界的存活克隆，以及生物转化，只补火种这一个键。
 * 下界和末地传送用的是同一个玩家对象，持久数据留在身上，这里不改。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class FireSeedLifecycle {
    private FireSeedLifecycle() {
    }

    /** 死亡时只删火种。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event == null || event.isCanceled()) {
            return;
        }
        FireSeedState.discard(event.getEntity());
    }

    /** 死亡克隆丢掉新实体上的火种；存活克隆把旧实体的火种抄过来。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event == null || event.getEntity() == null) {
            return;
        }
        if (event.isWasDeath()) {
            FireSeedState.discard(event.getEntity());
            return;
        }
        FireSeedState.retainLivingClone(event.getOriginal(), event.getEntity());
    }

    /** 生物转化后，把转化前实体的火种抄到结果实体。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingConversion(LivingConversionEvent.Post event) {
        if (event == null || event.getOutcome() == null) {
            return;
        }
        FireSeedState.retainLivingClone(event.getEntity(), event.getOutcome());
    }
}
