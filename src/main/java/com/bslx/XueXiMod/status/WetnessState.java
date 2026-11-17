package com.bslx.XueXiMod.status;

import com.bslx.XueXiMod.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** 潮湿。0 到 100，四档，每次成功施加都刷新 50 秒。只能由术式调用 {@link #grant}。 */
public final class WetnessState {
    public static final AttributeStateDefinition DEFINITION = new AttributeStateDefinition("wetness", 100);
    public static final int DURATION_TICKS = 1000;

    private WetnessState() {}

    /** 1 到 25、26 到 50、51 到 75、76 及以上分别是一到四档。不是正数时没有档。 */
    public static int tierOf(int amount) {
        if (amount <= 0) {
            return 0;
        }
        if (amount <= 25) {
            return 1;
        }
        if (amount <= 50) {
            return 2;
        }
        if (amount <= 75) {
            return 3;
        }
        return 4;
    }

    /** 读精确湿度。 */
    public static int read(LivingEntity target) {
        return AttributeStateService.read(target, DEFINITION);
    }

    /** 增加湿度、夹到 100，并按新档位把药水计时刷成 {@link #DURATION_TICKS}。 */
    public static void grant(LivingEntity target, int added) {
        if (target == null || !target.isAlive() || added <= 0 || target.level().isClientSide()) {
            return;
        }
        int current = read(target);
        int updated = current + added;
        if (updated > DEFINITION.upperBound()) {
            updated = DEFINITION.upperBound();
        }
        AttributeStateService.write(target, DEFINITION, updated);
        int tier = tierOf(updated);
        MobEffect effect = ModMobEffects.WETNESS.get();
        target.addEffect(new MobEffectInstance(effect, DURATION_TICKS, tier - 1, false, true, true));
    }

    /** 用现存湿度让药水档位对齐。数值还在但药水没了时，对齐会清掉数值。 */
    public static void reconcile(LivingEntity target) {
        int stored = read(target);
        AttributeStateService.reconcile(target, DEFINITION, ModMobEffects.WETNESS.get(), tierOf(stored));
    }

    /** 身上潮湿药水还剩多少刻。没有效果时是 0。 */
    public static int remainingDuration(LivingEntity target) {
        if (target == null) {
            return 0;
        }
        MobEffectInstance instance = target.getEffect(ModMobEffects.WETNESS.get());
        if (instance == null) {
            return 0;
        }
        return instance.getDuration();
    }

    /** 按调用方给出的剩余时间改精确湿度。不走 {@link #grant}，因此不会把计时刷回 1000。 */
    public static void replaceExact(LivingEntity target, int amount, int durationTicks) {
        if (target == null || target.level() == null || target.level().isClientSide()) {
            return;
        }
        MobEffect effect = ModMobEffects.WETNESS.get();
        if (amount <= 0 || durationTicks <= 0) {
            clear(target);
            return;
        }
        int amplifier = tierOf(amount) - 1;
        if (amplifier < 0) {
            amplifier = 0;
        }
        AttributeStateService.replace(target, DEFINITION, effect, amount, durationTicks, amplifier);
    }

    /** 按剩余时间改精确湿度。不走 {@link #grant}，因此不会把计时刷回 1000。 */
    public static void replacePreservingDuration(LivingEntity target, int amount) {
        if (target == null || target.level().isClientSide()) {
            return;
        }
        MobEffect effect = ModMobEffects.WETNESS.get();
        int durationTicks = remainingDuration(target);
        if (amount <= 0 || durationTicks <= 0) {
            clear(target);
            return;
        }
        int amplifier = tierOf(amount) - 1;
        if (amplifier < 0) {
            amplifier = 0;
        }
        AttributeStateService.replace(target, DEFINITION, effect, amount, durationTicks, amplifier);
    }

    public static void clear(LivingEntity target) {
        if (target == null || target.level().isClientSide()) {
            return;
        }
        AttributeStateService.clear(target, DEFINITION, ModMobEffects.WETNESS.get());
    }
}
