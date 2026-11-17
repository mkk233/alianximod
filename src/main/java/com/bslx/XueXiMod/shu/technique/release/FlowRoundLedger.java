package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.release.payload.FlowSuccessPayload;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一份账本里已经收下的持续接触。同一目标、同一轮、同一复制序号只收一次。
 * 伤害仍按原规则留下最后一份再乘份数。潮印条数按份累加。
 * 非伤害载荷合并成一份。结算后清掉本轮数字和载荷，已经见过的轮次留着。
 */
public final class FlowRoundLedger {
    public static final int FLUSH_DELAY = 6;

    private final TideMarkCopies tideCopies = new TideMarkCopies();
    private final Map<UUID, Integer> counts = new HashMap<>();
    private final Map<UUID, Float> damages = new HashMap<>();
    private final Map<UUID, FlowSuccessPayload> payloads = new HashMap<>();

    public static int roundIndex(int hitInterval, int originTick, int nowTick) {
        if (hitInterval <= 0) {
            return 0;
        }
        int elapsed = Math.max(0, nowTick - originTick);
        return elapsed / hitInterval;
    }

    public boolean accept(UUID targetId, int roundIndex, int copyIndex, float damage, int marks,
                          FlowSuccessPayload flowPayload) {
        if (!this.tideCopies.accept(targetId, roundIndex, copyIndex, marks)) {
            return false;
        }
        this.counts.merge(targetId, 1, Integer::sum);
        this.damages.put(targetId, damage);
        if (flowPayload != null) {
            FlowSuccessPayload stored = this.payloads.get(targetId);
            this.payloads.put(targetId, stored == null ? flowPayload : stored.mergedWith(flowPayload));
        }
        return true;
    }

    public Set<UUID> targets() {
        return new HashSet<>(this.counts.keySet());
    }

    public int count(UUID targetId) {
        Integer stored = this.counts.get(targetId);
        return stored == null ? 0 : stored;
    }

    public Float damage(UUID targetId) {
        return this.damages.get(targetId);
    }

    public float aggregatedDamage(UUID targetId) {
        Float stored = this.damage(targetId);
        if (stored == null) {
            return 0f;
        }
        return stored * this.count(targetId);
    }

    public FlowSuccessPayload payload(UUID targetId) {
        return this.payloads.get(targetId);
    }

    public int takeMarks(UUID targetId) {
        return this.tideCopies.take(targetId);
    }

    /** 清掉已经交出的本轮数字和载荷。见过的轮次和复制序号不动。 */
    public void clearSettled() {
        this.counts.clear();
        this.damages.clear();
        this.payloads.clear();
    }
}
