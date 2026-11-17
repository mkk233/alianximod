package com.bslx.XueXiMod.status.fire.seed;

/** 一次根产种事务的结果。COMMITTED 且 increased 为 0 表示已经满额，机会仍然被这次用掉。 */
public final class SeedMergeResult {
    public enum Kind {
        REJECTED,
        IDLE,
        ALREADY,
        COMMITTED
    }

    private final Kind kind;
    private final int requested;
    private final int increased;
    private final int balance;

    private SeedMergeResult(Kind kind, int requested, int increased, int balance) {
        this.kind = kind;
        this.requested = requested;
        this.increased = increased;
        this.balance = balance;
    }

    public static SeedMergeResult rejected() {
        return new SeedMergeResult(Kind.REJECTED, 0, 0, 0);
    }

    static SeedMergeResult idle(int balance) {
        return new SeedMergeResult(Kind.IDLE, 0, 0, balance);
    }

    static SeedMergeResult already(int balance) {
        return new SeedMergeResult(Kind.ALREADY, 0, 0, balance);
    }

    static SeedMergeResult committed(int requested, int increased, int balance) {
        return new SeedMergeResult(Kind.COMMITTED, requested, increased, balance);
    }

    /** 拒绝、零请求、这一施法已经产过，还是这次已经提交。 */
    public Kind kind() {
        return this.kind;
    }

    /** 合并后准备入账的点数。 */
    public int requested() {
        return this.requested;
    }

    /** 余额实际增加的点数。 */
    public int increased() {
        return this.increased;
    }

    /** 提交后的余额。 */
    public int balance() {
        return this.balance;
    }

    /** 这次有没有占掉产种机会。 */
    public boolean markedOpportunity() {
        return this.kind == Kind.COMMITTED;
    }
}
