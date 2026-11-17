package com.bslx.XueXiMod.shu.technique.release.capture;

import org.jetbrains.annotations.Nullable;

/**
 * 结构编译结果。不适用时没有计划和拒绝码。
 */
public record ActorCaptureInspection(
        ActorCaptureStatus status,
        @Nullable CapturedStructurePlan plan,
        @Nullable CaptureRefusal refusal,
        String playerText) {

    public ActorCaptureInspection {
        if (status == null || playerText == null) {
            throw new IllegalArgumentException("捕获检查不完整");
        }
        if (status == ActorCaptureStatus.NOT_APPLICABLE && (plan != null || refusal != null || !playerText.isEmpty())) {
            throw new IllegalArgumentException("普通链不能带捕获计划");
        }
        if (status == ActorCaptureStatus.PLAN && (plan == null || refusal != null)) {
            throw new IllegalArgumentException("捕获计划不完整");
        }
        if (status == ActorCaptureStatus.REFUSED && (plan != null || refusal == null || playerText.isBlank())) {
            throw new IllegalArgumentException("捕获拒绝不完整");
        }
    }
}
