package com.bslx.XueXiMod.shu.technique;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class TechniqueEffectRegistry {
    private static final Map<String, TechniqueEffect> EFFECTS = new LinkedHashMap<>();

    public static void register(String id, TechniqueEffect effect) {
        EFFECTS.put(id, effect);
    }

    @Nullable
    public static TechniqueEffect get(String id) {
        return EFFECTS.get(id);
    }
}
