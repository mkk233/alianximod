package com.bslx.XueXiMod.shu;

import java.util.*;

/**
 * 术管理器 - 静态注册表
 * 存存所有已注册的术，提供查询方法
 */
public class TechniqueManager {
    private static final Map<String, Technique> TECHNIQUES = new LinkedHashMap<>();

    public static void register(Technique technique) {
        TECHNIQUES.put(technique.getId(), technique);
    }

    public static Technique getById(String id) {
        return TECHNIQUES.get(id);
    }

    public static Collection<Technique> getAll() {
        return Collections.unmodifiableCollection(TECHNIQUES.values());
    }

    public static List<Technique> getByType(TechniqueType type) {
        List<Technique> result = new ArrayList<>();
        for (Technique t : TECHNIQUES.values()) {
            if (t.getType() == type) result.add(t);
        }
        return result;
    }

    /**
     * 获取可用于剑技组合的术（剑术 + 通用）
     */
    public static List<Technique> getSwordCompatible() {
        List<Technique> result = new ArrayList<>();
        for (Technique t : TECHNIQUES.values()) {
            if (t.getType().canUseForSwordSkill()) result.add(t);
        }
        return result;
    }

    /**
     * 获取可用于法技组合的术（法术 + 通用）
     */
    public static List<Technique> getMagicCompatible() {
        List<Technique> result = new ArrayList<>();
        for (Technique t : TECHNIQUES.values()) {
            if (t.getType().canUseForMagicSkill()) result.add(t);
        }
        return result;
    }
}
