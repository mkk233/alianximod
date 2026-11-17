package com.bslx.XueXiMod.shu;

import com.bslx.XueXiMod.shu.technique.ChainStructureEpoch;
import java.util.*;

/**
 * 术管理器 - 静态注册表
 * 存存所有已注册的术，提供查询方法
 */
public class TechniqueManager {
    private static final Map<String, Technique> TECHNIQUES = new LinkedHashMap<>();

    public static void register(Technique technique) {
        TECHNIQUES.put(technique.getId(), technique);
        ChainStructureEpoch.changed();
    }

    public static Technique getById(String techniqueId) {
        if (techniqueId == null) {
            return null;
        }
        return TECHNIQUES.get(TechniqueIds.canonicalId(techniqueId));
    }

    public static Collection<Technique> getAll() {
        return Collections.unmodifiableCollection(TECHNIQUES.values());
    }

    public static List<Technique> playerCatalog() {
        return TechniqueAccess.playerCatalog();
    }

    public static List<Technique> getByType(TechniqueType type) {
        List<Technique> result = new ArrayList<>();
        for (Technique technique : playerCatalog()) {
            if (technique.getType() == type) result.add(technique);
        }
        return result;
    }

    /**
     * 获取可用于剑技组合的术（剑术 + 通用）
     */
    public static List<Technique> getSwordCompatible() {
        List<Technique> result = new ArrayList<>();
        for (Technique technique : playerCatalog()) {
            if (technique.getType().canUseForSwordSkill()) result.add(technique);
        }
        return result;
    }

    /**
     * 获取可用于法技组合的术（法术 + 通用）
     */
    public static List<Technique> getMagicCompatible() {
        List<Technique> result = new ArrayList<>();
        for (Technique technique : playerCatalog()) {
            if (technique.getType().canUseForMagicSkill()) result.add(technique);
        }
        return result;
    }
}
