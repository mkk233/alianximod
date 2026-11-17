package com.bslx.XueXiMod.shu.technique.release.capture;

import org.jetbrains.annotations.Nullable;

/**
 * 纯载体检查。就绪才带可执行计划；草稿不带计划，也不豁免剑术。
 */
public record CaptureCarrierCheck(
        CaptureCarrierDecision decision,
        @Nullable CapturedStructurePlan plan,
        @Nullable CaptureRefusal refusal,
        String playerText) {

    public CaptureCarrierCheck {
        if (decision == null || playerText == null) {
            throw new IllegalArgumentException("载体检查不完整");
        }
        if (decision == CaptureCarrierDecision.READY && plan == null) {
            throw new IllegalArgumentException("就绪的载体检查必须带计划");
        }
        if (decision != CaptureCarrierDecision.READY && plan != null) {
            throw new IllegalArgumentException("未就绪的载体检查不能带执行计划");
        }
        if (decision == CaptureCarrierDecision.NOT_APPLICABLE && (refusal != null || !playerText.isEmpty())) {
            throw new IllegalArgumentException("普通载体检查不能带拒绝");
        }
        if ((decision == CaptureCarrierDecision.REFUSED || decision == CaptureCarrierDecision.DRAFT)
                && (refusal == null || playerText.isBlank())) {
            throw new IllegalArgumentException("载体拒绝或草稿必须说明原因");
        }
    }
}
