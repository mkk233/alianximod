package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.Optional;

/** 一层共享燃料燃烧在挂载时锁住的参数。这里还没有付款。 */
public final class SharedSpec {
    private final int intervalTicks;
    private final int segmentLimit;
    private final int segmentDemand;
    private final double fullSegmentDamage;
    private final int observedTiers;

    private SharedSpec(int intervalTicks, int segmentLimit, int segmentDemand, double fullSegmentDamage,
                       int observedTiers) {
        this.intervalTicks = intervalTicks;
        this.segmentLimit = segmentLimit;
        this.segmentDemand = segmentDemand;
        this.fullSegmentDamage = fullSegmentDamage;
        this.observedTiers = observedTiers;
    }

    public static Optional<SharedSpec> of(int intervalTicks, int segmentLimit, int segmentDemand,
                                          double fullSegmentDamage, int observedTiers) {
        if (intervalTicks < FireLimits.INTERVAL_FLOOR || intervalTicks > FireLimits.INTERVAL_CAP) {
            return Optional.empty();
        }
        if (segmentLimit <= 0 || segmentLimit > FireLimits.SEGMENT_CAP) {
            return Optional.empty();
        }
        if (segmentDemand <= 0 || segmentDemand > FireLimits.DEMAND_CAP) {
            return Optional.empty();
        }
        if (!FireLayer.acceptableDamage(fullSegmentDamage)) {
            return Optional.empty();
        }
        if (observedTiers < 0 || observedTiers > FireLimits.TIER_CAP) {
            return Optional.empty();
        }
        return Optional.of(new SharedSpec(intervalTicks, segmentLimit, segmentDemand, fullSegmentDamage, observedTiers));
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
}
