package com.bslx.XueXiMod.status.fire.seed;

/** 旧档案三种收益。只按完整档计算，余数不另加。 */
public enum FireSeedArchive {
    BURST,
    SUSTAIN,
    SPREAD;

    public double yield(int tiers) {
        if (tiers <= 0) {
            return 0.0D;
        }
        return switch (this) {
            case BURST -> tiers * 4.0D / 100.0D;
            case SUSTAIN -> tiers * 10.0D;
            case SPREAD -> tiers * 10.0D / 100.0D;
        };
    }
}
