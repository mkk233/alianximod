package com.bslx.XueXiMod.shu;

/**
 * 历史术式编号到现行编号。只认完整编号，不看显示名，也不做子串替换。
 */
public final class TechniqueIds {
    private TechniqueIds() {
    }

    public static String canonicalId(String techniqueId) {
        if ("water_open_ring".equals(techniqueId)) {
            return "water_blade";
        }
        return techniqueId;
    }
}
