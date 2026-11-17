package com.bslx.XueXiMod.status.wood.resource;

import java.util.List;
import java.util.UUID;

/** ready 看到的只读预览。持有它不能改账。potentialUnits 是本组被取走、但不入账的潜力合计。 */
public record GroundConsumePreview(UUID ownerId, int potentialUnits, List<GroundPieceTake> pieces) {
    public GroundConsumePreview {
        pieces = pieces == null ? List.of() : List.copyOf(pieces);
    }
}
