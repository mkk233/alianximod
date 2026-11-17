package com.bslx.XueXiMod.shu.technique.release.clone.core;

import org.jetbrains.annotations.Nullable;

public final class CloneTriggerPolicy {
    private CloneTriggerPolicy() {
    }

    public static boolean accept(CloneTriggerInput input, CloneRuntimeState state) {
        return rejection(input, state) == null;
    }

    @Nullable
    static CloneResults.RejectReason rejection(CloneTriggerInput input, CloneRuntimeState state) {
        if (input == null || state == null) {
            return CloneResults.RejectReason.ACTOR_GONE;
        }
        if (CloneSourceScope.blocksResponse() || input.sourceDepth() > 0) {
            return CloneResults.RejectReason.BUSY;
        }
        if (state.budgetSpent || state.response == CloneResponseState.SPENT) {
            return CloneResults.RejectReason.BUDGET_SPENT;
        }
        if (state.response == CloneResponseState.COOLDOWN) {
            return CloneResults.RejectReason.COOLDOWN;
        }
        if (state.response != CloneResponseState.READY || state.pending != null) {
            return CloneResults.RejectReason.BUSY;
        }
        if (state.life != CloneLifeState.ACTIVE) {
            return CloneResults.RejectReason.ACTOR_GONE;
        }
        return null;
    }
}
