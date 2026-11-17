package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 显式元素来源才改伤害。普通攻击没有这个范围时保持原样。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class FireRuntimeEvents {
    private FireRuntimeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        ElementDamageScope.HurtCall call = ElementDamageScope.currentHurt();
        if (call == null || call.frame() == null || event.isCanceled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (call.target() != victim || call.source() != event.getSource()) {
            return;
        }
        if (victim.level().isClientSide()) {
            return;
        }
        ElementDamageScope.Frame frame = call.frame();
        if (frame.fire() && !frame.furnaceSettled()) {
            double bonus = casterBonus(victim, frame);
            double updated = ElementAdjustment.fireAmount(event.getAmount(), frame.fireBase(), bonus, false);
            event.setAmount((float) updated);
            frame.settleFurnace();
        }
        if ((frame.water() || frame.ice()) && !frame.vulnerabilityApplied()) {
            double multiplier = FurnaceService.waterIceMultiplier(victim);
            double updated = ElementAdjustment.vulnerableAmount(event.getAmount(), multiplier, false);
            event.setAmount((float) updated);
            frame.applyVulnerability();
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        boolean follow = FireVisualRelay.watching(entity)
                || entity.getPersistentData().contains(FireLimits.SAVE_KEY);
        BurnCarrier.tick(entity);
        if (follow) {
            FireVisualRelay.afterCarrierTick(entity);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled()) {
            return;
        }
        BurnCarrier.clear(event.getEntity());
        FireVisualRelay.abort(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            BurnCarrier.clear(event.getEntity());
            FireVisualRelay.abort(event.getEntity());
            return;
        }
        BurnCarrier.copy(event.getOriginal(), event.getEntity());
        FireVisualRelay.publish(event.getEntity());
    }

    private static double casterBonus(LivingEntity victim, ElementDamageScope.Frame frame) {
        ServerPlayer caster = OnlineCaster.find(victim, frame.casterId());
        if (caster == null) {
            return 0.0d;
        }
        double queried = FurnaceService.fireBonus(caster);
        if (!Double.isFinite(queried) || queried < 0.0d) {
            return 0.0d;
        }
        return queried;
    }
}
