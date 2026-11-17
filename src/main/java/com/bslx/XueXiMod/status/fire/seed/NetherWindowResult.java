package com.bslx.XueXiMod.status.fire.seed;

import java.util.UUID;

/** 一个幽冥窗口的仲裁结果。只有 {@link Kind#GRANTED} 带胜出领域和点数。 */
public final class NetherWindowResult {
    public enum Kind {
        REJECTED,
        EMPTY,
        ALREADY,
        GRANTED
    }

    private final Kind kind;
    private final UUID domainId;
    private final int flooredPoints;
    private final int increased;

    private NetherWindowResult(Kind kind, UUID domainId, int flooredPoints, int increased) {
        this.kind = kind;
        this.domainId = domainId;
        this.flooredPoints = flooredPoints;
        this.increased = increased;
    }

    static NetherWindowResult rejected() {
        return new NetherWindowResult(Kind.REJECTED, null, 0, 0);
    }

    static NetherWindowResult empty() {
        return new NetherWindowResult(Kind.EMPTY, null, 0, 0);
    }

    static NetherWindowResult already() {
        return new NetherWindowResult(Kind.ALREADY, null, 0, 0);
    }

    static NetherWindowResult granted(UUID domainId, int flooredPoints, int increased) {
        return new NetherWindowResult(Kind.GRANTED, domainId, flooredPoints, increased);
    }

    /** 拒绝、没有胜出、这一窗口已经入过账，还是这次入了账。 */
    public Kind kind() {
        return this.kind;
    }

    /** 胜出领域。没有胜出时为空。 */
    public UUID domainId() {
        return this.domainId;
    }

    /** 胜出请求向下取整后的点数。 */
    public int flooredPoints() {
        return this.flooredPoints;
    }

    /** 这次实际增加的火种。余额已满时可以是 0。 */
    public int increased() {
        return this.increased;
    }
}
