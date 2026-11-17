package com.bslx.XueXiMod.combat.event;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.combat.counter.CounterService;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 迎击的唯一 Forge 订阅。不注册第二遍，也不读取木系资源。
 * 每次包装进入压一个伤害事件槽；正常退出把该槽里的事件交回服务，异常退出只丢掉该槽。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class CounterRuntimeEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<DamageEventSlots<LivingDamageEvent>> DAMAGE_SLOTS =
            ThreadLocal.withInitial(DamageEventSlots::new);

    private CounterRuntimeEvents() {
    }

    /** 由 hurt 包装在 CounterService.enterHurt 之后调用，为这一层调用压一个空槽。 */
    public static void markHurtEntered() {
        DAMAGE_SLOTS.get().push();
    }

    /**
     * 正常出口在 finishHurt 之前调用。此时伤害事件派发已经返回，取消标记是最终值。
     * 只把当前槽里的事件再交给服务；空槽不记成命中。
     */
    public static void sealDamage() {
        LivingDamageEvent damageEvent = DAMAGE_SLOTS.get().popSeal();
        if (damageEvent == null) {
            return;
        }
        try {
            CounterService.onLivingDamage(damageEvent);
        } catch (Throwable failure) {
            LOGGER.error("counter damage seal failed", failure);
        }
    }

    /** 异常出口只丢掉当前槽，不把它记成有效命中，也不弹出更外层的槽。 */
    public static void discardDamage() {
        DAMAGE_SLOTS.get().popDiscard();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        CounterService.onLivingHurt(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        DAMAGE_SLOTS.get().writeCurrent(event);
        CounterService.onLivingDamage(event);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        CounterService.onServerTick(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOwnerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            CounterService.onOwnerDeath(player);
        }
    }

    @SubscribeEvent
    public static void onOwnerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CounterService.onOwnerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onOwnerDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CounterService.onOwnerDimension(player);
        }
    }

    /**
     * 停服不扫在线玩家。没有玩家时也调用全表清理。
     * 服务端线程和空服务器由 CounterService 自己判断；当前线程的伤害事件槽在这里丢掉。
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        try {
            CounterService.onServerStopping(event.getServer());
        } finally {
            DAMAGE_SLOTS.remove();
        }
    }
}
