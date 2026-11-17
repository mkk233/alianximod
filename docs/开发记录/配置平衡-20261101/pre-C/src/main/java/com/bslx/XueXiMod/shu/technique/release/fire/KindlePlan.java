package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantNumbers;

import java.util.Optional;

/**
 * 引燃计划。启动档 n 为余额除以 10，不足 1 档不挂。
 * 期限是 40 + 10n tick，段数等于期限除以 10，所以末段落在到期当刻。
 * 伤害是 2 × (1 + 启动时的炉心热量)，资源增量固定为 0。燃料仍是共享余额的每段 10 点。
 */
public final class KindlePlan {
    private KindlePlan() {
    }

    public static int durationTicks(int tierCount) {
        if (tierCount < 1) {
            return 0;
        }
        return ResourceEnchantNumbers.KINDLE_FLAT_TICKS + ResourceEnchantNumbers.KINDLE_TIER_TICKS * tierCount;
    }

    public static int segmentCount(int tierCount) {
        if (tierCount < 1) {
            return 0;
        }
        return durationTicks(tierCount) / ResourceEnchantNumbers.KINDLE_INTERVAL_TICKS;
    }

    public static Optional<SharedSpec> specify(int balance, double furnaceBonus) {
        if (balance < FireLimits.STANDARD_SEGMENT_DEMAND || !Double.isFinite(furnaceBonus) || furnaceBonus < 0.0d) {
            return Optional.empty();
        }
        int tierCount = balance / FireLimits.STANDARD_SEGMENT_DEMAND;
        if (tierCount > FireLimits.TIER_CAP) {
            tierCount = FireLimits.TIER_CAP;
        }
        int hops = segmentCount(tierCount);
        double damage = FireLayer.combine(ResourceEnchantNumbers.KINDLE_SEGMENT_DAMAGE, 0.0d, furnaceBonus);
        return SharedSpec.of(ResourceEnchantNumbers.KINDLE_INTERVAL_TICKS, hops,
                ResourceEnchantNumbers.KINDLE_SEGMENT_DEMAND, damage, tierCount);
    }
}
