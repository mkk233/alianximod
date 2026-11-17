package com.bslx.XueXiMod.shu.technique.release.capture;

import com.bslx.XueXiMod.shu.ShuAttribute;

import java.util.List;

/**
 * 一次整链捕获的原始下标。不含主人、位置、费用或武器。
 */
public record CapturedStructurePlan(
        int captureOriginalIndex,
        List<Integer> payloadOriginalIndexes,
        List<Integer> consumedOriginalIndexes,
        List<Integer> chargedOriginalIndexes,
        int nextOriginalIndex,
        String payloadTechniqueId,
        ShuAttribute embryoCopy,
        String adapterKey,
        int adapterVersion,
        long structureEpoch) {

    public CapturedStructurePlan {
        if (payloadOriginalIndexes == null || consumedOriginalIndexes == null || chargedOriginalIndexes == null
                || payloadTechniqueId == null || payloadTechniqueId.isBlank() || embryoCopy == null
                || adapterKey == null || adapterKey.isBlank()) {
            throw new IllegalArgumentException("捕获计划不完整");
        }
        if (adapterVersion < 1 || captureOriginalIndex < 0 || nextOriginalIndex < 0) {
            throw new IllegalArgumentException("捕获计划的下标或版本无效");
        }
        payloadOriginalIndexes = List.copyOf(payloadOriginalIndexes);
        consumedOriginalIndexes = List.copyOf(consumedOriginalIndexes);
        chargedOriginalIndexes = List.copyOf(chargedOriginalIndexes);
    }
}
