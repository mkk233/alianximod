package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.List;
import java.util.UUID;

/** 可往返存档的一条燃烧。校验在恢复时做，这里只保存读到的原始值。 */
public final class BurnTrackState {
    private final String effectKey;
    private final boolean targetExclusive;
    private final UUID castId;
    private final UUID casterId;
    private final UUID targetId;
    private final int amount;
    private final int tiers;
    private final int remainder;
    private final double furnaceBonus;
    private final int intervalTicks;
    private final int ticksUntilNext;
    private final int segmentCursor;
    private final List<Double> damages;
    private final String fuelMode;
    private final int segmentDemand;
    private final double fullSegmentDamage;
    private final int segmentLimit;
    private final long layerIdentity;
    private final int pricing;
    private final double furnaceSnapshot;

    public BurnTrackState(String effectKey, boolean targetExclusive, UUID castId, UUID casterId, UUID targetId,
                          int amount, int tiers, int remainder, double furnaceBonus, int intervalTicks,
                          int ticksUntilNext, int segmentCursor, List<Double> damages) {
        this(effectKey, targetExclusive, castId, casterId, targetId, amount, tiers, remainder, furnaceBonus,
                intervalTicks, ticksUntilNext, segmentCursor, damages, FuelMode.PREPAID, 0, 0.0d,
                damages == null ? 0 : damages.size(), 0L, SharedSpec.FIXED_FULL, furnaceBonus);
    }

    public BurnTrackState(String effectKey, boolean targetExclusive, UUID castId, UUID casterId, UUID targetId,
                          int amount, int tiers, int remainder, double furnaceBonus, int intervalTicks,
                          int ticksUntilNext, int segmentCursor, List<Double> damages, String fuelMode,
                          int segmentDemand, double fullSegmentDamage, int segmentLimit, long layerIdentity,
                          int pricing, double furnaceSnapshot) {
        this.effectKey = effectKey;
        this.targetExclusive = targetExclusive;
        this.castId = castId;
        this.casterId = casterId;
        this.targetId = targetId;
        this.amount = amount;
        this.tiers = tiers;
        this.remainder = remainder;
        this.furnaceBonus = furnaceBonus;
        this.intervalTicks = intervalTicks;
        this.ticksUntilNext = ticksUntilNext;
        this.segmentCursor = segmentCursor;
        this.damages = damages == null ? List.of() : List.copyOf(damages);
        this.fuelMode = fuelMode == null ? FuelMode.PREPAID : fuelMode;
        this.segmentDemand = segmentDemand;
        this.fullSegmentDamage = fullSegmentDamage;
        this.segmentLimit = segmentLimit;
        this.layerIdentity = layerIdentity;
        this.pricing = pricing;
        this.furnaceSnapshot = furnaceSnapshot;
    }

    public BurnTrackState(String effectKey, boolean targetExclusive, UUID castId, UUID casterId, UUID targetId,
                          int amount, int tiers, int remainder, double furnaceBonus, int intervalTicks,
                          int ticksUntilNext, int segmentCursor, List<Double> damages, String fuelMode,
                          int segmentDemand, double fullSegmentDamage, int segmentLimit, long layerIdentity) {
        this(effectKey, targetExclusive, castId, casterId, targetId, amount, tiers, remainder, furnaceBonus,
                intervalTicks, ticksUntilNext, segmentCursor, damages, fuelMode, segmentDemand, fullSegmentDamage,
                segmentLimit, layerIdentity, SharedSpec.FIXED_FULL, furnaceBonus);
    }

    public String effectKey() {
        return this.effectKey;
    }

    public boolean targetExclusive() {
        return this.targetExclusive;
    }

    public UUID castId() {
        return this.castId;
    }

    public UUID casterId() {
        return this.casterId;
    }

    public UUID targetId() {
        return this.targetId;
    }

    public int amount() {
        return this.amount;
    }

    public int tiers() {
        return this.tiers;
    }

    public int remainder() {
        return this.remainder;
    }

    public double furnaceBonus() {
        return this.furnaceBonus;
    }

    public int intervalTicks() {
        return this.intervalTicks;
    }

    public int ticksUntilNext() {
        return this.ticksUntilNext;
    }

    public int segmentCursor() {
        return this.segmentCursor;
    }

    public List<Double> damages() {
        return this.damages;
    }

    public String fuelMode() {
        return this.fuelMode;
    }

    public boolean sharedFuel() {
        return FuelMode.SHARED.equals(this.fuelMode);
    }

    public int segmentDemand() {
        return this.segmentDemand;
    }

    public double fullSegmentDamage() {
        return this.fullSegmentDamage;
    }

    public int segmentLimit() {
        return this.segmentLimit;
    }

    public long layerIdentity() {
        return this.layerIdentity;
    }

    public boolean dynamicDemand() {
        return this.pricing == SharedSpec.DYNAMIC_CEILING;
    }

    public int pricing() {
        return this.pricing;
    }

    public double furnaceSnapshot() {
        return this.furnaceSnapshot;
    }
}
