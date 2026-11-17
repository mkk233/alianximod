package com.bslx.XueXiMod.animation.runtime;

import java.util.UUID;

/** 先核对主人、许可和攻击组，再消费。对不上时不消费。 */
public final class CommitAuthorization {
    public enum Outcome {
        MISMATCH,
        DUPLICATE,
        SPENT,
        READY
    }

    private CommitAuthorization() {
    }

    public static boolean matches(UUID playerId, CommitPermit permit, ActionCommit commit) {
        if (playerId == null || permit == null || commit == null) {
            return false;
        }
        if (!playerId.equals(permit.ownerId())) {
            return false;
        }
        if (commit.groupId() == null || !permit.groupId().equals(commit.groupId())) {
            return false;
        }
        return commit.actionId() != null;
    }

    public static Outcome decide(UUID playerId, CommitPermit permit, ActionCommit commit, long serverTick) {
        if (!matches(playerId, permit, commit)) {
            return Outcome.MISMATCH;
        }
        if (GroupReplayLedger.covers(playerId, commit.groupId(), serverTick)) {
            return Outcome.DUPLICATE;
        }
        if (!permit.consume()) {
            return Outcome.SPENT;
        }
        return Outcome.READY;
    }
}
