package com.bslx.XueXiMod.shu.technique.release.cooperative;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 本批术式的执行能力。没有登记的术式按旧路径 {@link TechniqueExecutionCapabilities#none()} 处理。
 */
public final class CooperativeCapabilityIndex {
    private static final ConcurrentMap<String, TechniqueExecutionCapabilities> CAPABILITIES = new ConcurrentHashMap<>();

    private CooperativeCapabilityIndex() {
    }

    public static void remember(String techniqueId, TechniqueExecutionCapabilities capabilities) {
        if (techniqueId == null || techniqueId.isBlank() || capabilities == null) {
            throw new IllegalArgumentException("执行能力登记不完整");
        }
        TechniqueExecutionCapabilities previous = CAPABILITIES.putIfAbsent(techniqueId, capabilities);
        if (previous != null && previous != capabilities) {
            throw new IllegalStateException("执行能力重复登记: " + techniqueId);
        }
    }

    public static TechniqueExecutionCapabilities find(String techniqueId) {
        if (techniqueId == null || techniqueId.isBlank()) {
            return TechniqueExecutionCapabilities.none();
        }
        TechniqueExecutionCapabilities stored = CAPABILITIES.get(techniqueId);
        return stored == null ? TechniqueExecutionCapabilities.none() : stored;
    }

    public static boolean opensFamily(String techniqueId) {
        TechniqueExecutionCapabilities capabilities = find(techniqueId);
        return capabilities.isTimedRelease()
                || capabilities.isExplicitAttackAdapter()
                || capabilities.isCooperativeRule()
                || capabilities.isStandaloneDeployment();
    }
}
