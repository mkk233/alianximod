package com.bslx.XueXiMod.status.fire.seed;

import java.util.Optional;
import java.util.UUID;

/**
 * 一个目标的纯余额事务。多个施法上下文共享这一份余额，账本仍按上下文和目标分开。
 * 这里不碰线程和实体；服务端入口在 {@link FireSeedState}。
 */
final class FireSeedAccount {
    private int balance;

    FireSeedAccount(int stored) {
        this.balance = FireSeedMath.clamp(stored);
    }

    /** 当前余额。 */
    int balance() {
        return this.balance;
    }

    /** 这一施法还没生产或消费过该目标时，按倍率向下取整后入账，并占掉生产机会。 */
    int grant(FireCastContext cast, UUID targetId, int baseAmount, double productionMultiplier) {
        if (!open(cast, targetId) || baseAmount < 0 || !FireSeedMath.finiteNonNegative(productionMultiplier)) {
            return 0;
        }
        if (cast.hasConsumed(targetId) || cast.hasProduced(targetId)) {
            return 0;
        }
        int gain = FireSeedMath.floorProduction(baseAmount, productionMultiplier);
        if (gain <= 0) {
            return 0;
        }
        // 请求经倍率取整后仍为正才占生产机会。已经是 100 时增加量可以是 0，机会照样记下。
        cast.markProduced(targetId);
        return this.addClamped(gain);
    }

    /**
     * 新合并产种。不看 hasConsumed。请求大于 0 就占生产机会，即使余额已满、实际增加为 0。
     */
    SeedMergeResult grantMerged(FireCastContext cast, UUID targetId, int requested) {
        if (!open(cast, targetId) || requested < 0) {
            return SeedMergeResult.rejected();
        }
        if (cast.hasProduced(targetId)) {
            return SeedMergeResult.already(this.balance);
        }
        if (requested == 0) {
            return SeedMergeResult.idle(this.balance);
        }
        cast.markProduced(targetId);
        int increased = this.addClamped(requested);
        return SeedMergeResult.committed(requested, increased, this.balance);
    }

    /** 窗口产种直接入余额。不占命中产种机会，也不写消费标记。 */
    int depositUnscoped(int flooredPoints) {
        if (flooredPoints <= 0) {
            return 0;
        }
        return this.addClamped(flooredPoints);
    }

    /** 只看余额和请求，不扣点、不占消费机会。 */
    Optional<FireSeedSnapshot> preview(FireCastContext cast, UUID targetId, int requested, double furnaceBonus) {
        return this.plan(cast, targetId, requested, furnaceBonus);
    }

    /** 按预览扣余额并占掉这一施法对该目标的消费机会。炉心加成只写进快照。 */
    Optional<FireSeedSnapshot> consume(FireCastContext cast, UUID targetId, int requested, double furnaceBonus) {
        Optional<FireSeedSnapshot> snapshot = this.plan(cast, targetId, requested, furnaceBonus);
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        this.balance -= snapshot.get().amount();
        cast.markConsumed(targetId);
        return snapshot;
    }

    /** 共享燃烧按段扣燃料。不占用施法的一次引爆机会。 */
    int draw(int spent) {
        if (spent <= 0 || this.balance <= 0) {
            return 0;
        }
        int removed = spent;
        if (removed > this.balance) {
            removed = this.balance;
        }
        this.balance -= removed;
        return removed;
    }

    private Optional<FireSeedSnapshot> plan(FireCastContext cast, UUID targetId, int requested, double furnaceBonus) {
        if (!open(cast, targetId) || requested < 0 || !FireSeedMath.finiteNonNegative(furnaceBonus)) {
            return Optional.empty();
        }
        if (cast.hasConsumed(targetId)) {
            return Optional.empty();
        }
        int invested = FireSeedMath.invested(this.balance, requested);
        if (invested <= 0) {
            return Optional.empty();
        }
        return Optional.of(FireSeedSnapshot.of(cast.castId(), cast.casterId(), targetId, invested, furnaceBonus));
    }

    private int addClamped(int gain) {
        long sum = (long) this.balance + (long) gain;
        if (sum > FireSeedMath.UPPER_BOUND) {
            sum = FireSeedMath.UPPER_BOUND;
        }
        int increased = (int) sum - this.balance;
        this.balance = (int) sum;
        return increased;
    }

    private static boolean open(FireCastContext cast, UUID targetId) {
        return cast != null && targetId != null;
    }
}
