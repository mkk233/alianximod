package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 远距通道或额度被拒绝。身体留在原地，不生成替身，也不消耗响应。 */
public record RejectedRecall(CancelReason reason) implements RecallAttempt {
    public RejectedRecall {
        if (reason == null) {
            throw new IllegalArgumentException("远距拒绝缺少原因");
        }
    }
}
