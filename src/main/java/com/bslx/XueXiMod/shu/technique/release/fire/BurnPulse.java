package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.UUID;

/** 本拍应打出的一段。伤害是挂载时封好的数，施法者只保留当时的 UUID。 */
public final class BurnPulse {
    private final String effectKey;
    private final UUID casterId;
    private final UUID castId;
    private final double damage;
    private final int ordinal;
    private final int segmentCount;

    public BurnPulse(String effectKey, UUID casterId, UUID castId, double damage, int ordinal, int segmentCount) {
        this.effectKey = effectKey;
        this.casterId = casterId;
        this.castId = castId;
        this.damage = damage;
        this.ordinal = ordinal;
        this.segmentCount = segmentCount;
    }

    public String effectKey() {
        return this.effectKey;
    }

    public UUID casterId() {
        return this.casterId;
    }

    public UUID castId() {
        return this.castId;
    }

    public double damage() {
        return this.damage;
    }

    public int ordinal() {
        return this.ordinal;
    }

    public int segmentCount() {
        return this.segmentCount;
    }
}
