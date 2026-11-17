package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.TechniqueIds;
import com.bslx.XueXiMod.shu.technique.ChainStructureEpoch;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** 按术式标识保存释放定义。登记时写入，释放前查询。不校验定义内容，也不执行释放。 */
public final class ReleaseRegistry {
    private static final Map<String, ReleaseDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static final java.util.Set<String> HOST_GROUP = java.util.Set.of("space_cut");

    private ReleaseRegistry() {}

    /** 按定义标识覆盖旧登记，并标记链结构纪元变化。空定义会失败。 */
    public static void register(ReleaseDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
        ChainStructureEpoch.changed();
    }

    /** 按规范化标识取定义。传入空或未登记时返回空。 */
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
