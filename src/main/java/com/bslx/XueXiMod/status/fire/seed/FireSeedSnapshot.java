package com.bslx.XueXiMod.status.fire.seed;

import java.util.UUID;

/**
 * 一次成功消费的不可变快照。投入点数、档位和余数必须自洽。
 * 炉心加成只被记录，不改本次扣点。不持有余额对象或实体。
 */
public record FireSeedSnapshot(
        UUID castId,
        UUID casterId,
        UUID targetId,
        int amount,
        int tiers,
        int remainder,
        double furnaceBonus) {
    public FireSeedSnapshot {
        if (castId == null) {
            throw new IllegalArgumentException("castId");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("targetId");
        }
        if (amount <= 0 || amount > FireSeedMath.UPPER_BOUND) {
            throw new IllegalArgumentException("amount");
        }
        if (tiers != FireSeedMath.tiers(amount) || remainder != FireSeedMath.remainder(amount)) {
            throw new IllegalArgumentException("split");
        }
        if (!FireSeedMath.finiteNonNegative(furnaceBonus)) {
            throw new IllegalArgumentException("furnaceBonus");
        }
    }

    /** 按投入点数拆出档位和余数，炉心加成原样记下。 */
    public static FireSeedSnapshot of(UUID castId, UUID casterId, UUID targetId, int amount, double furnaceBonus) {
        return new FireSeedSnapshot(
                castId,
                casterId,
                targetId,
                amount,
                FireSeedMath.tiers(amount),
                FireSeedMath.remainder(amount),
                furnaceBonus);
    }
}
