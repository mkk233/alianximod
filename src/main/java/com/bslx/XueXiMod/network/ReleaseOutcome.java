package com.bslx.XueXiMod.network;

/** 一次成员提交的结果。入队或 runner 返回都不算提交。 */
public final class ReleaseOutcome {
    public enum Kind {
        COMMITTED,
        REJECTED,
        CANCELLED
    }

    private final Kind kind;
    private final int tick;
    private final String reason;

    private ReleaseOutcome(Kind kind, int tick, String reason) {
        this.kind = kind;
        this.tick = tick;
        this.reason = reason == null ? "" : reason;
    }

    public static ReleaseOutcome committed(int tick) {
        return new ReleaseOutcome(Kind.COMMITTED, tick, "");
    }

    public static ReleaseOutcome rejected(int tick, String reason) {
        return new ReleaseOutcome(Kind.REJECTED, tick, reason);
    }

    public static ReleaseOutcome cancelled(int tick, String reason) {
        return new ReleaseOutcome(Kind.CANCELLED, tick, reason);
    }

    public boolean committed() {
        return kind == Kind.COMMITTED;
    }

    public int tick() {
        return tick;
    }

    public String reason() {
        return reason;
    }
}
