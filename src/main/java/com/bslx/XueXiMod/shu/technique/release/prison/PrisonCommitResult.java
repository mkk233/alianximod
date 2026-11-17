package com.bslx.XueXiMod.shu.technique.release.prison;

/** 提交结果。失败时 view 为空。服务本身不扣魔力。 */
public record PrisonCommitResult(boolean committed, String reason, PrisonView view) {
    public static PrisonCommitResult fail(String reason) {
        String token = reason == null ? "rejected" : reason;
        return new PrisonCommitResult(false, token, null);
    }

    public static PrisonCommitResult success(PrisonView view) {
        return new PrisonCommitResult(true, "", view);
    }
}
