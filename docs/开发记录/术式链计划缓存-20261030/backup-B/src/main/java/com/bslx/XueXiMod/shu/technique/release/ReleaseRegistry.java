package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.TechniqueIds;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ReleaseRegistry {
    private static final Map<String, ReleaseDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final java.util.Set<String> HOST_GROUP = java.util.Set.of("space_cut");

    private ReleaseRegistry() {}

    public static void register(ReleaseDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    @Nullable
    public static ReleaseDefinition get(String identifier) {
        if (identifier == null) {
            return null;
        }
        return DEFINITIONS.get(TechniqueIds.canonicalId(identifier));
    }

    /** 这条术式是否走宿主组提交。未登记的术式保持原来的即时释放。 */
    public static boolean submitsHostGroup(String identifier) {
        if (identifier == null) {
            return false;
        }
        return HOST_GROUP.contains(TechniqueIds.canonicalId(identifier));
    }
}
