package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantNumbers;

import java.util.Optional;

/**
 * 引燃计划。启动档 n 为余额除以 10，不足 1 档不挂。
 * 期限默认是 40 + 10n tick，段数等于期限除以间隔，所以默认末段落在到期当刻。
 * 伤害默认是 2 × (1 + 启动时的炉心热量)，挂载时按引燃缩放一次。燃料仍是共享余额的每段 10 点。
 */
public final class KindlePlan {
    private KindlePlan() {
    }

    public static int durationTicks(int tierCount) {
        if (tierCount < 1) {
            return 0;
        }
        long flatTicks = BalanceService.intValue(ResourceEffectKeys.KINDLE, SpecialBalance.FLAT_TICKS);
        long tierTicks = BalanceService.intValue(ResourceEffectKeys.KINDLE, SpecialBalance.TIER_TICKS);
        long duration = flatTicks + tierTicks * (long) tierCount;
        if (duration > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) duration;
    }

    public static int segmentCount(int tierCount) {
        if (tierCount < 1) {
            return 0;
        }
        int intervalTicks = BalanceService.intValue(ResourceEffectKeys.KINDLE, SpecialBalance.INTERVAL_TICKS);
        if (intervalTicks < 1) {
            return 0;
        }
        return durationTicks(tierCount) / intervalTicks;
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
        int intervalTicks = BalanceService.intValue(ResourceEffectKeys.KINDLE, SpecialBalance.INTERVAL_TICKS);
        double segmentDamage = BalanceService.number(ResourceEffectKeys.KINDLE, BalanceKeys.SEGMENT_DAMAGE);
        double rawDamage = FireLayer.combine(segmentDamage, 0.0d, furnaceBonus);
        double settledDamage = SpecialBalance.settle(ResourceEffectKeys.KINDLE, rawDamage);
        return SharedSpec.of(intervalTicks, hops, ResourceEnchantNumbers.KINDLE_SEGMENT_DEMAND, settledDamage, tierCount);
    }
}
