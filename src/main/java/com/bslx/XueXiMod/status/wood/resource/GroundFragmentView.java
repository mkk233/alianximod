package com.bslx.XueXiMod.status.wood.resource;

import java.util.UUID;

/** 查询返回的不可变片段。potentialUnits 是根的原始潜力，不是已经入账的岁华。 */
public record GroundFragmentView(
        UUID fragmentId,
        int revision,
        UUID rootId,
        int rootSerial,
        UUID batchId,
        long batchSerial,
        UUID ownerId,
        ResourceOrigin origin,
        FlowerKind flower,
        int lowInclusive,
        int highExclusive,
        int effectiveUnits,
        int potentialUnits,
        long expireGameTime,
        String dimensionId,
        double coordX,
        double coordY,
        double coordZ) {

    public int length() {
        return highExclusive - lowInclusive;
    }

    public int remainingPotential() {
        return WoodInterval.potential(potentialUnits, effectiveUnits, lowInclusive, highExclusive);
    }
}
