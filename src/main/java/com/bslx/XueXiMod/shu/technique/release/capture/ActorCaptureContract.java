package com.bslx.XueXiMod.shu.technique.release.capture;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;

import java.util.Set;

/**
 * 释放定义上的捕获声明。默认是空能力。例外集合只放行完整计划已经消费的载荷类型。
 */
public record ActorCaptureContract(
        ActorCaptureMode mode,
        ChainRole capturedRole,
        int maximumEntries,
        ShuAttribute requiredAttribute,
        boolean requireExplicitAdapter,
        boolean rejectModifiers,
        boolean rejectNesting,
        CaptureCarrierMode carrierMode,
        Set<TechniqueType> capturedTypeExceptions) {

    public static final ActorCaptureContract NONE = new ActorCaptureContract(
            ActorCaptureMode.NONE, ChainRole.ATTACK, 0, ShuAttribute.NONE, false, false, false,
            CaptureCarrierMode.NONE, Set.of());

    public ActorCaptureContract {
        if (mode == null || capturedRole == null || requiredAttribute == null || carrierMode == null
                || capturedTypeExceptions == null) {
            throw new IllegalArgumentException("捕获声明不完整");
        }
        if (maximumEntries < 0) {
            throw new IllegalArgumentException("捕获条目上限不能为负");
        }
        capturedTypeExceptions = Set.copyOf(capturedTypeExceptions);
    }
}
