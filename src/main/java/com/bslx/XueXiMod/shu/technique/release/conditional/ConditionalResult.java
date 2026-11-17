package com.bslx.XueXiMod.shu.technique.release.conditional;

/** 一次条件释放的同步结果。准备阶段不算提交。 */
public final class ConditionalResult {
    private final boolean committed;
    private final String reason;

    private ConditionalResult(boolean committed, String reason) {
        this.committed = committed;
        this.reason = reason == null ? "" : reason;
    }

    /** 条件效果已经提交。 */
    public static ConditionalResult committed() {
        return new ConditionalResult(true, "");
    }

    /** 条件效果没有提交。准备阶段也用这个结果。 */
    public static ConditionalResult rejected(String reason) {
        return new ConditionalResult(false, reason);
    }

    /** 是否已经提交。 */
    public boolean isCommitted() {
        return committed;
    }

    /** 拒绝原因。提交成功时为空。 */
    public String reason() {
        return reason;
    }
}
