package com.bslx.XueXiMod.status;

import net.minecraft.world.entity.LivingEntity;

/** 潮湿四档的只读查询。不写状态，不注册事件。没有实体时按 0 档；已死实体仍可读湿度。 */
public final class WetnessTiers {
    private WetnessTiers() {}

    public record Profile(
            int tier,
            double movementReduction,
            float attackReward,
            float qualifyingFunds,
            float meleeCost,
            float arrowCostRatio,
            int breathMultiplier) {
    }

    /** 一档只减速；二档起给攻击奖励并降低入卷费用；四档再把呼吸消耗乘 2。 */
    public static Profile ofValue(int wetness) {
        int tier = WetnessState.tierOf(wetness);
        if (tier <= 0) {
            return new Profile(0, 0.0D, 0.0F, 20.0F, 20.0F, 1.0F, 1);
        }
        if (tier == 1) {
            return new Profile(1, 0.15D, 0.0F, 20.0F, 20.0F, 1.0F, 1);
        }
        if (tier == 2) {
            return new Profile(2, 0.15D, 1.0F, 15.0F, 15.0F, 0.75F, 1);
        }
        if (tier == 3) {
            return new Profile(3, 0.30D, 2.0F, 15.0F, 15.0F, 0.75F, 1);
        }
        return new Profile(4, 0.30D, 2.0F, 10.0F, 10.0F, 0.5F, 2);
    }

    /** 按实体当前湿度取档。 */
    public static Profile of(LivingEntity entity) {
        if (entity == null) {
            return ofValue(0);
        }
        return ofValue(WetnessState.read(entity));
    }
}
