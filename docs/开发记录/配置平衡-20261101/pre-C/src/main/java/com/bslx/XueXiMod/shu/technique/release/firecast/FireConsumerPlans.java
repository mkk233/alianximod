package com.bslx.XueXiMod.shu.technique.release.firecast;

import com.bslx.XueXiMod.shu.technique.release.fire.BurnComponent;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLayer;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLimits;
import com.bslx.XueXiMod.shu.technique.release.fire.SegmentPlan;
import com.bslx.XueXiMod.shu.technique.release.fire.SharedSpec;
import com.bslx.XueXiMod.status.fire.seed.FireSeedMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 三个消费者的纯计划。炉心不写进基础值，由消费服务合入一次。
 * 不产火种，也不套旧爆发档案。
 */
public final class FireConsumerPlans {
    public static final String SPREAD_KEY = "xueximod:fire_liaoyuan";
    public static final String SKY_KEY = "xueximod:fire_fentian";
    public static final int ALL_SEEDS = Integer.MAX_VALUE;
    public static final int SPREAD_INTERVAL = 20;
    public static final int SPREAD_BASE_SEGMENTS = 3;
    public static final int SKY_INTERVAL = 4;
    public static final int SKY_SEGMENT_LIMIT = 10;

    private FireConsumerPlans() {
    }

    /** 燎原挂载参数。n 与炉心在扣种前锁进足额段 4×(1+0.1n+F)，燃料仍按段支付。 */
    public static Optional<SharedSpec> spreadSpec(int balance, double furnaceBonus) {
        if (balance <= 0 || !FireSeedMath.finiteNonNegative(furnaceBonus)) {
            return Optional.empty();
        }
        int tiers = balance / 10;
        int segmentLimit = SPREAD_BASE_SEGMENTS + tiers;
        double fullDamage = FireLayer.combine(4.0d, 0.1d * tiers, furnaceBonus);
        return SharedSpec.of(SPREAD_INTERVAL, segmentLimit, FireLimits.STANDARD_SEGMENT_DEMAND, fullDamage, tiers);
    }

    /** 焚天挂载参数。指数和炉心锁定，最多 10 段、40 刻，没有余数尾段。 */
    public static Optional<SharedSpec> skySpec(int balance, double furnaceBonus) {
        if (balance <= 0 || !FireSeedMath.finiteNonNegative(furnaceBonus)) {
            return Optional.empty();
        }
        int tiers = balance / 10;
        if (tiers > FireLimits.TIER_CAP) {
            tiers = FireLimits.TIER_CAP;
        }
        double exponent = Math.pow(1.2d, tiers);
        if (!Double.isFinite(exponent)) {
            return Optional.empty();
        }
        double fullDamage = 2.0d * (exponent + furnaceBonus);
        return SharedSpec.of(SKY_INTERVAL, SKY_SEGMENT_LIMIT, FireLimits.STANDARD_SEGMENT_DEMAND, fullDamage, tiers);
    }

    /** 旧预付档案：基础仍是 2。运行时火场不走这里，挂载用 {@link #spreadSpec}。 */
    public static Optional<SegmentPlan> spread(int amount, int tiers, int remainder) {
        if (amount <= 0 || tiers < 0 || remainder < 0) {
            return Optional.empty();
        }
        int segments = SPREAD_BASE_SEGMENTS + tiers;
        if (segments > FireLimits.SEGMENT_CAP) {
            return Optional.empty();
        }
        Optional<BurnComponent> component = BurnComponent.of(2.0d, 0.1d * tiers);
        if (component.isEmpty()) {
            return Optional.empty();
        }
        ArrayList<BurnComponent> pieces = new ArrayList<>();
        int segmentIndex = 0;
        while (segmentIndex < segments) {
            pieces.add(component.get());
            segmentIndex += 1;
        }
        return SegmentPlan.of(SPREAD_INTERVAL, pieces);
    }

    /** 燎里引爆：B=S，A=0。物理刺击和迎面速度不在这里。 */
    public static Optional<BurnComponent> thrust(int amount, int tiers, int remainder) {
        if (amount <= 0 || tiers < 0 || remainder < 0) {
            return Optional.empty();
        }
        return BurnComponent.of(amount, 0.0d);
    }

    /**
     * 焚天展示用的等长完整段。运行时挂载走 {@link #skySpec}，不足 10 点只在结算时按燃料比例缩放。
     */
    public static Optional<SegmentPlan> sky(int amount, int tiers, int remainder) {
        if (amount <= 0 || tiers < 0 || remainder < 0) {
            return Optional.empty();
        }
        int lockedTiers = amount / 10;
        if (lockedTiers > FireLimits.TIER_CAP) {
            lockedTiers = FireLimits.TIER_CAP;
        }
        double exponent = Math.pow(1.2d, lockedTiers);
        if (!Double.isFinite(exponent) || exponent < 1.0d) {
            return Optional.empty();
        }
        double above = exponent - 1.0d;
        if (above < 0.0d) {
            above = 0.0d;
        }
        Optional<BurnComponent> full = BurnComponent.of(2.0d, above);
        if (full.isEmpty()) {
            return Optional.empty();
        }
        ArrayList<BurnComponent> pieces = new ArrayList<>();
        int segmentIndex = 0;
        while (segmentIndex < SKY_SEGMENT_LIMIT) {
            pieces.add(full.get());
            segmentIndex += 1;
        }
        return SegmentPlan.of(SKY_INTERVAL, List.copyOf(pieces));
    }
}
