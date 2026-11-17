package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.UUID;

/** 扣款成功后冻结的火种快照。燃烧期间不再读取目标上的新余额。 */
public final class BurnIdentity {
    private final UUID castId;
    private final UUID casterId;
    private final UUID targetId;
    private final int amount;
    private final int tiers;
    private final int remainder;
    private final double furnaceBonus;

    public BurnIdentity(UUID castId, UUID casterId, UUID targetId, int amount, int tiers, int remainder,
                        double furnaceBonus) {
        this.castId = castId;
        this.casterId = casterId;
        this.targetId = targetId;
        this.amount = amount;
        this.tiers = tiers;
        this.remainder = remainder;
        this.furnaceBonus = furnaceBonus;
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
}
