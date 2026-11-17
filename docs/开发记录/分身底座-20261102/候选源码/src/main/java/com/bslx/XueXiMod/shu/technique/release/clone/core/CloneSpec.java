package com.bslx.XueXiMod.shu.technique.release.clone.core;

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
        double decoyRadiusBlocks) {

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
        MANUAL_AND_HURT
    }

    public CloneSpec {
        if (techniqueId == null || techniqueId.isBlank()) {
            throw new IllegalArgumentException("分身规格缺少来源术式");
        }
        if (kind == null || lifetime == null || budget == null || actionKey == null || actionKey.isBlank()
                || triggerMode == null || station == null) {
            throw new IllegalArgumentException("分身规格不完整");
        }
        if (!Double.isFinite(bodyHealth) || !Double.isFinite(decoyRadiusBlocks)) {
            throw new IllegalArgumentException("分身规格含有非有限数");
        }
    }
}
