package com.bslx.XueXiMod.shu.technique;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EnchantmentRegistry {
    private static final Map<String, EnchantmentDefinition> DEFINITIONS = new LinkedHashMap<>();

    private EnchantmentRegistry() {}

    public static void register(EnchantmentDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    @Nullable
    public static EnchantmentDefinition get(String id) {
        return DEFINITIONS.get(id);
    }
}
