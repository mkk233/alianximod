package com.bslx.XueXiMod.shu.technique.release.selfstatus;

/**
 * {@link SelfStatusCast#prepare} 的结果。拒绝时没有可提交对象，提交器不得扣款。
 */
public final class StatusPreparation {
    /** 首次预检的明确分类。调用方不能靠提示文本再猜一次。 */
    public enum Classification {
        /** 可以进入复核和扣款。 */
        READY,
        /** 预检拒绝。不得据此推进游标。 */
        REJECTED,
        /** 当前状态不适用。只有多段链的调用方可以推进一格。 */
        NOT_APPLICABLE
    }

    private final PreparedStatusChange preparedChange;
    private final String rejectionText;
    private final Classification classification;

    private StatusPreparation(PreparedStatusChange preparedChange, String rejectionText,
                              Classification classification) {
        this.preparedChange = preparedChange;
        this.rejectionText = rejectionText;
        this.classification = classification;
    }

    /** 预检通过。提交器可以进入复核和扣款。 */
    public static StatusPreparation ready(PreparedStatusChange preparedChange) {
        return new StatusPreparation(preparedChange, null, Classification.READY);
    }

    /** 预检拒绝。没有可提交对象，不得扣款。 */
    public static StatusPreparation reject(String rejectionText) {
        return new StatusPreparation(null, rejectionText, Classification.REJECTED);
    }

    /** 首次预检确认当前状态不适用。没有可提交对象，不得扣款。 */
    public static StatusPreparation notApplicable(String rejectionText) {
        return new StatusPreparation(null, rejectionText, Classification.NOT_APPLICABLE);
    }

    /** 是否有可提交对象。 */
    public boolean ready() {
        return this.preparedChange != null;
    }

    /** 预检通过时的变更。拒绝时为空。 */
    public PreparedStatusChange change() {
        return this.preparedChange;
    }

    /** 首次预检分类。 */
    public Classification classification() {
        return this.classification;
    }

    /** 是否明确为当前状态不适用。其他拒绝不是这种结果。 */
    public boolean notApplicable() {
        return this.classification == Classification.NOT_APPLICABLE;
    }

    /** 预检拒绝时的玩家提示。 */
    public String rejectionText() {
        return this.rejectionText;
    }
}
