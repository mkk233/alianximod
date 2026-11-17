package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import com.bslx.XueXiMod.status.WetnessState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.UUID;

/**
 * 领域原生脉冲服务。同一轮先收齐候选再给一次；潮湿和水势节流由静态窗口记住。
 * 球形场地在自己的十刻脉冲里合并效果，护体不发潮湿、减速或水势。
 */
public final class DomainPulses {
    private static final HashMap<UUID, WetWindow> WET_WINDOWS = new HashMap<>();
    private static final HashMap<UUID, Integer> NEXT_MOMENTUM = new HashMap<>();

    private DomainPulses() {
    }

    /** 清空潮湿窗口和水势节流。不退回已经发出的状态或水势。 */
    public static void clear() {
        WET_WINDOWS.clear();
        NEXT_MOMENTUM.clear();
    }

    /** 对到期领域发一次原生效果。未到期、无政策或不敌向的目标跳过，不扣魔力。 */
    public static void fire(Iterable<DomainInstance> domains, int now) {
        HashMap<UUID, WetOffer> wetOffers = new HashMap<>();
        HashMap<UUID, SlowOffer> slowOffers = new HashMap<>();
        for (DomainInstance domain : domains) {
            if (!domain.active() || domain.definition().nativePolicy() == null) {
                continue;
            }
            DomainNativePolicy policy = domain.definition().nativePolicy();
            if (policy.wetAmount() > 0 && due(now, domain.startedAt(), policy.wetDelay(), policy.wetPeriod())) {
                collectWet(domain, policy, wetOffers);
            }
            if (policy.slowAmplifier() >= 0 && due(now, domain.startedAt(), policy.slowDelay(), policy.slowPeriod())) {
                collectSlow(domain, policy, slowOffers);
            }
            if (policy.momentumAmount() > 0 && due(now, domain.startedAt(), policy.momentumDelay(), policy.momentumPeriod())) {
                momentumInside(domain, policy, now);
            }
        }
        for (WetOffer offer : wetOffers.values()) {
            grantWet(offer, now);
        }
        for (SlowOffer offer : slowOffers.values()) {
            applySlow(offer);
        }
    }

    private static void grantWet(WetOffer offer, int now) {
        UUID targetId = offer.living.getUUID();
        WetWindow window = WET_WINDOWS.get(targetId);
        if (window == null || now >= window.untilTick) {
            WetnessState.grant(offer.living, offer.amount);
            WET_WINDOWS.put(targetId, new WetWindow(now + offer.period, offer.amount));
            return;
        }
        if (offer.amount <= window.grantedAmount) {
            return;
        }
        WetnessState.grant(offer.living, offer.amount - window.grantedAmount);
        window.grantedAmount = offer.amount;
    }

    private static boolean due(int now, int startedAt, int delay, int period) {
        if (period <= 0 || now < startedAt + delay) {
            return false;
        }
        return (now - startedAt) % period == 0;
    }

    private static void collectWet(DomainInstance domain, DomainNativePolicy policy, HashMap<UUID, WetOffer> offers) {
        AABB area = domain.geometry().bounds();
        for (LivingEntity living : domain.hostLevel().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (!DomainNative.hostileInside(domain, living)) {
                continue;
            }
            WetOffer current = offers.get(living.getUUID());
            if (current != null && current.amount >= policy.wetAmount()) {
                continue;
            }
            offers.put(living.getUUID(), new WetOffer(living, policy.wetAmount(), policy.wetPeriod()));
        }
    }

    private static void collectSlow(DomainInstance domain, DomainNativePolicy policy, HashMap<UUID, SlowOffer> offers) {
        AABB area = domain.geometry().bounds();
        for (LivingEntity living : domain.hostLevel().getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (!DomainNative.hostileInside(domain, living)) {
                continue;
            }
            SlowOffer current = offers.get(living.getUUID());
            if (current != null && current.amplifier > policy.slowAmplifier()) {
                continue;
            }
            if (current != null && current.amplifier == policy.slowAmplifier() && current.duration >= policy.slowDuration()) {
                continue;
            }
            offers.put(living.getUUID(), new SlowOffer(living, policy.slowAmplifier(), policy.slowDuration()));
        }
    }

    private static void applySlow(SlowOffer offer) {
        MobEffectInstance current = offer.living.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (current != null && current.getAmplifier() > offer.amplifier) {
            return;
        }
        if (current != null && current.getAmplifier() == offer.amplifier && current.getDuration() >= offer.duration) {
            return;
        }
        offer.living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, offer.duration, offer.amplifier));
    }

    private static void momentumInside(DomainInstance domain, DomainNativePolicy policy, int now) {
        AABB area = domain.geometry().bounds().inflate(1.0);
        for (ServerPlayer player : domain.hostLevel().players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (!Relations.permits(domain.ownerId(), player) && !Relations.ally(domain.ownerId(), player)) {
                continue;
            }
            if (!domain.geometry().fits(player.getBoundingBox())
                    || !domain.geometry().fullyContains(player.getBoundingBox())) {
                continue;
            }
            if (!area.intersects(player.getBoundingBox())) {
                continue;
            }
            int ready = NEXT_MOMENTUM.getOrDefault(player.getUUID(), 0);
            if (now < ready) {
                continue;
            }
            WaterMomentum.credit(player, policy.momentumAmount());
            NEXT_MOMENTUM.put(player.getUUID(), now + policy.momentumPeriod());
        }
    }

    private static final class WetWindow {
        private final int untilTick;
        private int grantedAmount;

        private WetWindow(int untilTick, int grantedAmount) {
            this.untilTick = untilTick;
            this.grantedAmount = grantedAmount;
        }
    }

    private static final class WetOffer {
        private final LivingEntity living;
        private final int amount;
        private final int period;

        private WetOffer(LivingEntity living, int amount, int period) {
            this.living = living;
            this.amount = amount;
            this.period = period;
        }
    }

    private static final class SlowOffer {
        private final LivingEntity living;
        private final int amplifier;
        private final int duration;

        private SlowOffer(LivingEntity living, int amplifier, int duration) {
            this.living = living;
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }
}
