package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.List;

/** 一次推进后的余额、实际扣掉的燃料和已经提交的段。 */
public final class PumpResult {
    private final int balanceAfter;
    private final int spent;
    private final List<BurnPulse> pulses;

    public PumpResult(int balanceAfter, int spent, List<BurnPulse> pulses) {
        this.balanceAfter = balanceAfter;
        this.spent = spent;
        this.pulses = pulses == null ? List.of() : List.copyOf(pulses);
    }

    public int balanceAfter() {
        return this.balanceAfter;
    }

    public int spent() {
        return this.spent;
    }

    public List<BurnPulse> pulses() {
        return this.pulses;
    }
}
