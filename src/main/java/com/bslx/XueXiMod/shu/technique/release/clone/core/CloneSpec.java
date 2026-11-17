package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ExternalCooperation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record CloneSpec(
        String techniqueId,
        CloneKind kind,
        LifetimePolicy lifetime,
        double bodyHealth,
        ResponseBudget budget,
        boolean endAfterBudgetSpent,
        String actionKey,
        TriggerMode triggerMode,
        Station station,
        double decoyRadiusBlocks,
        TerminationPolicy terminationPolicy,
        PresentationKey presentationKey,
        int presentationRevision,
        @Nullable UUID deploymentId,
        ExternalCooperation externalCooperation) {

    /** 旧调用保持源兼容，外部协同默认关闭。普通分身在规格工厂里显式传入 {@link ExternalCooperation#OPEN}。 */
    public CloneSpec(String techniqueId, CloneKind kind, LifetimePolicy lifetime, double bodyHealth,
                     ResponseBudget budget, boolean endAfterBudgetSpent, String actionKey, TriggerMode triggerMode,
                     Station station, double decoyRadiusBlocks, TerminationPolicy terminationPolicy,
                     PresentationKey presentationKey, int presentationRevision, @Nullable UUID deploymentId) {
        this(techniqueId, kind, lifetime, bodyHealth, budget, endAfterBudgetSpent, actionKey, triggerMode, station,
                decoyRadiusBlocks, terminationPolicy, presentationKey, presentationRevision, deploymentId,
                ExternalCooperation.CLOSED);
    }

    public record LifetimePolicy(int spanTicks) {
        public boolean expires() {
            return this.spanTicks > 0;
        }
    }

    public enum ResponseBudget {
        ONE,
        REPEAT
    }

    public enum Station {
        STATIONARY
    }

    public enum TriggerMode {
        MANUAL_AND_HURT,
        CONTACT_OR_HURT
    }

    public CloneSpec {
        if (techniqueId == null || techniqueId.isBlank()) {
            throw new IllegalArgumentException("分身规格缺少来源术式");
        }
        if (kind == null || lifetime == null || budget == null || actionKey == null || actionKey.isBlank()
                || triggerMode == null || station == null || terminationPolicy == null || presentationKey == null
                || externalCooperation == null) {
            throw new IllegalArgumentException("分身规格不完整");
        }
        if (!Double.isFinite(bodyHealth) || !Double.isFinite(decoyRadiusBlocks)) {
            throw new IllegalArgumentException("分身规格含有非有限数");
        }
        boolean developmentSpec = terminationPolicy == TerminationPolicy.BUDGET
                && presentationKey == PresentationKey.LEGACY_HUMANOID
                && triggerMode == TriggerMode.MANUAL_AND_HURT;
        if (deploymentId == null && !developmentSpec) {
            throw new IllegalArgumentException("捕获部署缺少标识");
        }
    }
}
