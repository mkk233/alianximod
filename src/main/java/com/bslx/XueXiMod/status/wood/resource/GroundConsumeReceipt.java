package com.bslx.XueXiMod.status.wood.resource;

import java.util.List;
import java.util.UUID;

/** 整组消费成功后的不可变收据。效果由调用方在拿到收据之后自己做。 */
public record GroundConsumeReceipt(UUID ownerId, int potentialUnits, List<GroundPieceTake> pieces) {
    public GroundConsumeReceipt {
        pieces = pieces == null ? List.of() : List.copyOf(pieces);
    }
}
