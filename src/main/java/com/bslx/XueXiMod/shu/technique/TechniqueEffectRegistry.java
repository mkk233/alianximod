package com.bslx.XueXiMod.shu.technique;

import com.bslx.XueXiMod.shu.TechniqueIds;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class TechniqueEffectRegistry {
    private static final Map<String, TechniqueEffect> EFFECTS = new LinkedHashMap<>();

    public static void register(String effectId, TechniqueEffect effect) {
        EFFECTS.put(effectId, effect);
        ChainStructureEpoch.changed();
    }

    @Nullable
    public static TechniqueEffect get(String effectId) {
        if (effectId == null) {
            return null;
        }
        return EFFECTS.get(TechniqueIds.canonicalId(effectId));
    }
}
