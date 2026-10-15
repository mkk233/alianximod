package com.bslx.XueXiMod.shu;

/**
 * 术类型枚举
 * SWORD - 剑术，只能用于剑技
 * MOVEMENT - 动作，通用
 * MAGIC - 法术，只能用于法技
 * UNIVERSAL - 通用，可用于剑技和法技
 */
public enum TechniqueType {
    SWORD,
    MOVEMENT,
    MAGIC,
    UNIVERSAL;

    /**
     * 判断该类型是否可以用于剑技组合
     */
    public boolean canUseForSwordSkill() {
        return this == SWORD || this == UNIVERSAL;
    }

    /**
     * 判断该类型是否可以用于法技组合
     */
    public boolean canUseForMagicSkill() {
        return this == MAGIC || this == UNIVERSAL;
    }
}
