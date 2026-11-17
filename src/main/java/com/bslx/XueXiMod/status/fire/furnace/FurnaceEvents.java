package com.bslx.XueXiMod.status.fire.furnace;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家 tick 开始于效果计时之前，用来拆隐藏效果。
 * 本地 {@code Player.tick} 先发 pre-tick，再进入 {@code LivingEntity.tick} 的 {@code tickEffects}。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class FurnaceEvents {
    private FurnaceEvents() {
    }

    /** 这一拍开始先对齐效果和运行态，结束时再推进一拍热量。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player == null || player.level().isClientSide()) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            FurnaceRuntime.reconcile(player);
            return;
        }
        if (event.phase == TickEvent.Phase.END) {
            FurnaceRuntime.advance(player);
        }
    }

    /** 登录后对齐，并把热量时基改到当前拍。 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) {
            return;
        }
        FurnaceRuntime.reconcile(player);
        FurnaceStore.resyncClock(player);
    }

    /** 死亡克隆清掉炉心；存活克隆抄运行态并按原档位重挂无限效果。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClone(PlayerEvent.Clone event) {
        Player successor = event.getEntity();
        if (successor == null || successor.level().isClientSide()) {
            return;
        }
        Player previous = event.getOriginal();
        if (event.isWasDeath()) {
            if (successor.hasEffect(FurnaceMobEffect.effect())) {
                successor.removeEffect(FurnaceMobEffect.effect());
            }
            FurnaceStore.clear(successor);
            if (FurnaceAttributes.wearing(successor)) {
                FurnaceAttributes.detach(successor);
            }
            return;
        }
        MobEffectInstance carried = previous == null ? null : previous.getEffect(FurnaceMobEffect.effect());
        boolean previousEffect = FurnaceMobEffect.isFurnace(carried);
        boolean previousStore = previous != null && FurnaceStore.present(previous);
        if (!previousEffect && !previousStore) {
            return;
        }
        FurnaceStore.copy(previous, successor);
        int amplifier = FurnaceStore.storedAmplifier(successor);
        if (previousEffect) {
            amplifier = FurnaceMath.legalAmplifier(carried.getAmplifier());
        }
        if (!previousStore && previousEffect) {
            FurnaceLog.repair("非死亡克隆时炉心效果没有运行态，热量按0重建", successor);
        }
        successor.forceAddEffect(FurnaceMobEffect.infinite(amplifier), successor);
        FurnaceStore.resyncClock(successor);
    }

    /** 死亡时去掉炉心效果、运行态和属性。 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (player.hasEffect(FurnaceMobEffect.effect())) {
            player.removeEffect(FurnaceMobEffect.effect());
        }
        if (FurnaceStore.present(player)) {
            FurnaceStore.clear(player);
        }
        if (FurnaceAttributes.wearing(player)) {
            FurnaceAttributes.detach(player);
        }
    }

    /** 有炉心时按当前档位缩小这次治疗。 */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        MobEffectInstance instance = player.getEffect(FurnaceMobEffect.effect());
        if (!FurnaceMobEffect.isFurnace(instance)) {
            return;
        }
        double factor = FurnaceMath.healFactor(FurnaceMath.tierOfAmplifier(instance.getAmplifier()));
        event.setAmount((float) (event.getAmount() * factor));
    }

    /** 非玩家，或档位不是普通、过载时，拒绝挂上炉心。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (!FurnaceMobEffect.isFurnace(instance)) {
            return;
        }
        int amplifier = instance.getAmplifier();
        if (!(event.getEntity() instanceof Player) || amplifier < 0 || amplifier > 1) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** 新效果和被替换的旧效果都收成无限、合法档。 */
    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        FurnaceMobEffect.normalize(event.getEffectInstance());
        FurnaceMobEffect.normalize(event.getOldEffectInstance());
    }

    /** 炉心被移除时清运行态。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRemove(MobEffectEvent.Remove event) {
        if (event.isCanceled() || event.getEffectInstance() == null) {
            return;
        }
        if (!(event.getEffect() instanceof FurnaceMobEffect)) {
            return;
        }
        clearStore(event.getEntity());
    }

    /** 炉心到期时清运行态。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExpire(MobEffectEvent.Expired event) {
        if (event.isCanceled() || !FurnaceMobEffect.isFurnace(event.getEffectInstance())) {
            return;
        }
        clearStore(event.getEntity());
    }

    private static void clearStore(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        FurnaceStore.clear(player);
    }
}
