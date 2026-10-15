package com.bslx.XueXiMod.shu.technique;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EnchantmentResolution {
    private static final EnchantmentResolution EMPTY = new EnchantmentResolution(new LinkedHashMap<>());

    private final Map<String, EnchantmentAccumulator> effects;

    private EnchantmentResolution(Map<String, EnchantmentAccumulator> effects) {
        this.effects = Collections.unmodifiableMap(effects);
    }

    public static EnchantmentResolution empty() {
        return EMPTY;
    }

    public boolean has(String effectId) {
        return effects.containsKey(effectId);
    }

    @Nullable
    public EnchantmentAccumulator effect(String effectId) {
        return effects.get(effectId);
    }

    public float damageMultiplier() {
        float result = 1f;
        for (EnchantmentAccumulator accumulator : effects.values()) {
            result *= accumulator.damageMultiplier();
        }
        return result;
    }

    public float speedMultiplier() {
        float result = 1f;
        for (EnchantmentAccumulator accumulator : effects.values()) {
            result *= accumulator.speedMultiplier();
        }
        return result;
    }

    public EnchantmentResolution with(EnchantmentAccumulator accumulator) {
        Map<String, EnchantmentAccumulator> copy = new LinkedHashMap<>(effects);
        copy.put(accumulator.effectId(), accumulator);
        return new EnchantmentResolution(copy);
    }
}
