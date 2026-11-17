package com.bslx.XueXiMod.status.fire.furnace;

/** 炉心进入、升级、被动结束和主动冷却的一次结果。成功才允许未来术式结算魔力或冷却。 */
public final class FurnaceChange {
    private final FurnaceMath.Outcome outcome;

    public FurnaceChange(FurnaceMath.Outcome outcome) {
        this.outcome = outcome == null ? FurnaceMath.Outcome.REJECT_NULL : outcome;
    }

    /** 这次转换的结果。 */
    public FurnaceMath.Outcome outcome() {
        return this.outcome;
    }

    /** 进入、升级、结束或冷却是否成功。 */
    public boolean succeeded() {
        return this.outcome.succeeded();
    }
}
