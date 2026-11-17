package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 短距准备被拒绝。身体留在最后一个已经验证安全的位置，不传回旧点。 */
public record RejectedReposition(CancelReason reason) implements RepositionAttempt {
    public RejectedReposition {
        if (reason == null) {
            throw new IllegalArgumentException("短距拒绝缺少原因");
        }
    }
}
