package com.bslx.XueXiMod.shu;

/**
 * 熟练度系统
 * 根据使用次数计算等级和属性加成
 *
 * 等级范围 | 所需使用次数 | 伤害加成 | 速度加成 | 暴击率
 * 0       | 0           | x1.00   | x1.00   | 0%
 * 1-5     | 10-85       | x1.05-1.25 | x0.98-0.90 | 1%-5%
 * 6-10    | 130-410     | x1.30-1.50 | x0.88-0.80 | 6%-10%
 * 11-15   | 505-985     | x1.55-1.75 | x0.78-0.70 | 11%-15%
 * 16-20   | 1130-1810   | x1.80-2.00 | x0.68-0.60 | 16%-20%
 */
public class Proficiency {
    public static final int LEVEL_UPPER_BOUND = 20;

    // 每级所需累计使用次数
    private static final int[] USAGE_THRESHOLDS = {
        0, 10, 25, 45, 70, 85,        // 0-5
        130, 185, 250, 325, 410,       // 6-10
        505, 610, 725, 850, 985,       // 11-15
        1130, 1285, 1450, 1625, 1810   // 16-20
    };

    /**
     * 根据使用次数获取等级
     */
    public static int getLevel(int usageCount) {
        for (int level = LEVEL_UPPER_BOUND; level >= 0; level--) {
            if (usageCount >= USAGE_THRESHOLDS[level]) return level;
        }
        return 0;
    }

    /**
     * 获取伤害倍率
     */
    public static float getDamageMultiplier(int level) {
        return 1.0f + level * 0.05f; // 1.00 ~ 2.00
    }

    /**
     * 获取冷却倍率（越低越快）
     */
    public static float getCooldownMultiplier(int level) {
        return 1.0f - level * 0.02f; // 1.00 ~ 0.60
    }

    /**
     * 获取暴击率（0.0 ~ 1.0）
     */
    public static float getCritRate(int level) {
        return level * 0.01f; // 0% ~ 20%
    }

    /**
     * 获取升级到下一级所需使用次数
     */
    public static int getUsageToNextLevel(int currentUsage) {
        int currentLevel = getLevel(currentUsage);
        if (currentLevel >= LEVEL_UPPER_BOUND) return -1; // 已满级
        return USAGE_THRESHOLDS[currentLevel + 1] - currentUsage;
    }

    /**
     * 通过经验值快速提升所需的等级差
     * 每级需要 10 * level 经验值
     */
    public static int getXpCost(int fromLevel, int toLevel) {
        int cost = 0;
        for (int level = fromLevel + 1; level <= toLevel; level++) {
            cost += 10 * level;
        }
        return cost;
    }
}
