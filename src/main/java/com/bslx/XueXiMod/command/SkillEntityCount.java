package com.bslx.XueXiMod.command;

/**
 * 全服测试倍率。每个技能生成口本来生成 1 个实体时，改为生成这么多个。
 * 1 表示原版数量。
 */
public final class SkillEntityCount {
    public static final int UPPER_BOUND = 4096;
    private static int entityCount = 1;

    private SkillEntityCount() {
    }

    public static int entityCount() {
        return entityCount;
    }

    public static void setEntityCount(int requested) {
        if (requested < 1) {
            entityCount = 1;
            return;
        }
        entityCount = requested > UPPER_BOUND ? UPPER_BOUND : requested;
    }
}
