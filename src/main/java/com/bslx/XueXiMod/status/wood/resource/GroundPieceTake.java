package com.bslx.XueXiMod.status.wood.resource;

import java.util.UUID;

/**
 * 一组消费里的一条。potentialUnits 是被取走区间的潜力，只说明这笔不再留在地面，不会入账。
 * exhaustsFragment 为真时片段删除；否则 nextRevision 是留下的那段的新修订号。
 */
public record GroundPieceTake(
        UUID fragmentId,
        int seenRevision,
        int units,
        FlowerKind flower,
        ResourceOrigin origin,
        int potentialUnits,
        boolean exhaustsFragment,
        int nextRevision) {
}
