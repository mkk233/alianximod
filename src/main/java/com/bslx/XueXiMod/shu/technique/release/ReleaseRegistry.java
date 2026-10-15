package com.bslx.XueXiMod.shu.technique.release;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ReleaseRegistry {
    private static final Map<String, ReleaseDefinition> DEFINITIONS = new LinkedHashMap<>();

    private ReleaseRegistry() {}

    public static void register(ReleaseDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    @Nullable
    public static ReleaseDefinition get(String id) {
        return DEFINITIONS.get(id);
    }
}
