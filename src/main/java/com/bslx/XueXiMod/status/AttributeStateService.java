package com.bslx.XueXiMod.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;

/** 读写属性状态，并在读档后让精确数值和药水效果对齐。 */
public final class AttributeStateService {
    private static final ThreadLocal<Integer> DIRECT_EDITS = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> EFFECT_PASSES = ThreadLocal.withInitial(() -> 0);
    private static final ArrayDeque<DeferredEffect> DEFERRED = new ArrayDeque<>();

    private AttributeStateService() {}

    /** 读精确整数。实体或定义缺失时是 0。 */
    public static int read(LivingEntity entity, AttributeStateDefinition definition) {
        if (entity == null || definition == null) {
            return 0;
        }
        return AttributeStateData.read(entity, definition.identifier());
    }

    /** 把精确整数夹进定义的上下限后写入。 */
    public static void write(LivingEntity entity, AttributeStateDefinition definition, int amount) {
        if (entity == null || definition == null) {
            return;
        }
        int clamped = amount;
        if (clamped < 0) {
            clamped = 0;
        }
        if (clamped > definition.upperBound()) {
            clamped = definition.upperBound();
        }
        AttributeStateData.write(entity, definition.identifier(), clamped);
    }

    /** 数值还在但药水没了，视为到期，清掉数值。档位和药水不一致时，保留剩余时间并改档。 */
    public static void reconcile(LivingEntity entity, AttributeStateDefinition definition, MobEffect effect, int tier) {
        if (entity == null || entity.level().isClientSide() || definition == null || effect == null) {
            return;
        }
        int stored = read(entity, definition);
        MobEffectInstance instance = entity.getEffect(effect);
        if (stored <= 0) {
            if (instance != null) {
                entity.removeEffect(effect);
            }
            return;
        }
        if (instance == null) {
            write(entity, definition, 0);
            return;
        }
        int amplifier = tier - 1;
        if (tier <= 0 || instance.getAmplifier() == amplifier) {
            return;
        }
        int remaining = instance.getDuration();
        pushEdit();
        try {
            entity.removeEffect(effect);
            write(entity, definition, stored);
            entity.addEffect(new MobEffectInstance(effect, remaining, amplifier, false, true, true));
        } finally {
            popEdit();
        }
    }

    /**
     * 主动改精确值。先摘掉旧药水，再按调用方给出的剩余时间挂回去。
     * 不把时间改成新的满持续时间。药水迭代途中先记下，离开迭代再挂，避免重入刷新。
     */
    public static void replace(LivingEntity entity, AttributeStateDefinition definition, MobEffect effect,
                               int amount, int durationTicks, int amplifier) {
        if (entity == null || definition == null || effect == null || entity.level().isClientSide()) {
            return;
        }
        pushEdit();
        try {
            int clamped = amount;
            if (clamped < 0) {
                clamped = 0;
            }
            if (clamped > definition.upperBound()) {
                clamped = definition.upperBound();
            }
            if (clamped <= 0 || durationTicks <= 0) {
                clearDirect(entity, definition, effect);
                return;
            }
            if (entity.hasEffect(effect)) {
                entity.removeEffect(effect);
            }
            write(entity, definition, clamped);
            int safeAmplifier = amplifier;
            if (safeAmplifier < 0) {
                safeAmplifier = 0;
            }
            if (insideEffectPass()) {
                DEFERRED.addLast(new DeferredEffect(entity, effect, durationTicks, safeAmplifier));
                return;
            }
            entity.addEffect(new MobEffectInstance(effect, durationTicks, safeAmplifier, false, true, true));
        } finally {
            popEdit();
        }
    }

    /** 主动清零。和自然到期共用移除回调时，用 {@link #isDirectEdit()} 区分，避免再当成自然清除。 */
    public static void clear(LivingEntity entity, AttributeStateDefinition definition, MobEffect effect) {
        if (entity == null || definition == null || entity.level().isClientSide()) {
            return;
        }
        pushEdit();
        try {
            clearDirect(entity, definition, effect);
        } finally {
            popEdit();
        }
    }

    public static boolean isDirectEdit() {
        return DIRECT_EDITS.get() > 0;
    }

    /** 进入一层药水迭代。 */
    public static void enterEffectPass() {
        EFFECT_PASSES.set(EFFECT_PASSES.get() + 1);
    }

    /** 离开一层药水迭代。最外层离开时补挂刚才延后的效果。 */
    public static void leaveEffectPass() {
        int depth = EFFECT_PASSES.get() - 1;
        if (depth <= 0) {
            EFFECT_PASSES.remove();
            flushDeferred();
            return;
        }
        EFFECT_PASSES.set(depth);
    }

    /** 现在是不是还在药水迭代里面。 */
    public static boolean insideEffectPass() {
        return EFFECT_PASSES.get() > 0;
    }

    private static void clearDirect(LivingEntity entity, AttributeStateDefinition definition, MobEffect effect) {
        if (effect != null && entity.hasEffect(effect)) {
            entity.removeEffect(effect);
        }
        write(entity, definition, 0);
    }

    private static void pushEdit() {
        DIRECT_EDITS.set(DIRECT_EDITS.get() + 1);
    }

    private static void popEdit() {
        int depth = DIRECT_EDITS.get() - 1;
        if (depth <= 0) {
            DIRECT_EDITS.remove();
            return;
        }
        DIRECT_EDITS.set(depth);
    }

    private static void flushDeferred() {
        if (DEFERRED.isEmpty()) {
            return;
        }
        ArrayList<DeferredEffect> pending = new ArrayList<>(DEFERRED);
        DEFERRED.clear();
        for (DeferredEffect effect : pending) {
            LivingEntity entity = effect.entity;
            if (entity == null || entity.isRemoved() || entity.level().isClientSide()) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(effect.effect, effect.durationTicks, effect.amplifier, false, true, true));
        }
    }

    private record DeferredEffect(LivingEntity entity, MobEffect effect, int durationTicks, int amplifier) {
    }
}
