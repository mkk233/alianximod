package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.Optional;

/** 一层共享燃料燃烧在挂载时锁住的参数。这里还没有付款。 */
public final class SharedSpec {
    /** 每段使用挂载时锁定的完整段伤害，实付不足时按比例缩放。 */
    public static final int FIXED_FULL = 0;
    /** 每段按当时余额和剩余段数向上取整，伤害由实际消耗和当时余额计算。 */
    public static final int DYNAMIC_CEILING = 1;

    private final int intervalTicks;
    private final int segmentLimit;
    private final int segmentDemand;
    private final double fullSegmentDamage;
    private final int observedTiers;
    private final int pricing;
    private final double furnaceSnapshot;

    private SharedSpec(int intervalTicks, int segmentLimit, int segmentDemand, double fullSegmentDamage,
                       int observedTiers, int pricing, double furnaceSnapshot) {
        this.intervalTicks = intervalTicks;
        this.segmentLimit = segmentLimit;
        this.segmentDemand = segmentDemand;
        this.fullSegmentDamage = fullSegmentDamage;
        this.observedTiers = observedTiers;
        this.pricing = pricing;
        this.furnaceSnapshot = furnaceSnapshot;
    }

    public static Optional<SharedSpec> of(int intervalTicks, int segmentLimit, int segmentDemand,
                                          double fullSegmentDamage, int observedTiers) {
        return build(intervalTicks, segmentLimit, segmentDemand, fullSegmentDamage, observedTiers,
                FIXED_FULL, 0.0d);
    }

    /** 需求为 0：挂载时不锁定完整段伤害，每段按当时余额向上取整。 */
    public static Optional<SharedSpec> dynamic(int intervalTicks, int segmentLimit, int observedTiers,
                                               double furnaceSnapshot) {
        return build(intervalTicks, segmentLimit, 0, 0.0d, observedTiers, DYNAMIC_CEILING, furnaceSnapshot);
    }

    public int intervalTicks() {
        return this.intervalTicks;
    }

    public int segmentLimit() {
        return this.segmentLimit;
    }

    public int segmentDemand() {
        return this.segmentDemand;
    }

    public double fullSegmentDamage() {
        return this.fullSegmentDamage;
    }

    public int observedTiers() {
        return this.observedTiers;
    }

    public boolean dynamicDemand() {
        return this.pricing == DYNAMIC_CEILING;
    }

    public double furnaceSnapshot() {
        return this.furnaceSnapshot;
    }

    private static Optional<SharedSpec> build(int intervalTicks, int segmentLimit, int segmentDemand,
                                              double fullSegmentDamage, int observedTiers, int pricing,
                                              double furnaceSnapshot) {
        if (intervalTicks < FireLimits.INTERVAL_FLOOR || intervalTicks > FireLimits.INTERVAL_CAP) {
            return Optional.empty();
        }
        if (segmentLimit <= 0 || segmentLimit > FireLimits.SEGMENT_CAP) {
            return Optional.empty();
        }
        boolean dynamic = pricing == DYNAMIC_CEILING;
        if (pricing != FIXED_FULL && !dynamic) {
            return Optional.empty();
        }
        if (dynamic) {
            if (segmentDemand != 0 || fullSegmentDamage != 0.0d) {
                return Optional.empty();
            }
        } else if (segmentDemand <= 0 || segmentDemand > FireLimits.DEMAND_CAP
                || !FireLayer.acceptableSettled(fullSegmentDamage)) {
            return Optional.empty();
        }
        if (observedTiers < 0 || observedTiers > FireLimits.TIER_CAP) {
            return Optional.empty();
        }
        if (!Double.isFinite(furnaceSnapshot) || furnaceSnapshot < 0.0d) {
            return Optional.empty();
        }
        return Optional.of(new SharedSpec(intervalTicks, segmentLimit, segmentDemand, fullSegmentDamage,
                observedTiers, pricing, furnaceSnapshot));
    }
}
