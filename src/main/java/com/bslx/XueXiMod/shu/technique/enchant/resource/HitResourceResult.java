package com.bslx.XueXiMod.shu.technique.enchant.resource;

import com.bslx.XueXiMod.status.fire.seed.SeedMergeResult;
import com.bslx.XueXiMod.shu.technique.release.fire.DetonatePlan;

/** 一次已接受命中的资源处理结果。未接受或参数无效时不会写成成功。 */
public final class HitResourceResult {
    public enum Status {
        NOT_ACCEPTED,
        REJECTED,
        APPLIED
    }

    public enum KindleStatus {
        ABSENT,
        MOUNTED,
        DUPLICATE,
        REJECTED
    }

    private final Status status;
    private final int manaQuote;
    private final SeedMergeResult seeds;
    private final KindleStatus kindleStatus;
    private final DetonatePlan.Outcome detonate;

    private HitResourceResult(Status status, int manaQuote, SeedMergeResult seeds, KindleStatus kindleStatus,
                              DetonatePlan.Outcome detonate) {
        this.status = status;
        this.manaQuote = manaQuote;
        this.seeds = seeds;
        this.kindleStatus = kindleStatus;
        this.detonate = detonate;
    }

    static HitResourceResult notAccepted(int manaQuote) {
        return new HitResourceResult(Status.NOT_ACCEPTED, manaQuote, null, KindleStatus.ABSENT, null);
    }

    static HitResourceResult rejected(int manaQuote) {
        return new HitResourceResult(Status.REJECTED, manaQuote, null, KindleStatus.ABSENT, null);
    }

    static HitResourceResult applied(int manaQuote, SeedMergeResult seeds, KindleStatus kindleStatus,
                                     DetonatePlan.Outcome detonate) {
        return new HitResourceResult(Status.APPLIED, manaQuote, seeds, kindleStatus, detonate);
    }

    public Status status() {
        return this.status;
    }

    public int manaQuote() {
        return this.manaQuote;
    }

    public SeedMergeResult seeds() {
        return this.seeds;
    }

    public KindleStatus kindleStatus() {
        return this.kindleStatus;
    }

    public DetonatePlan.Outcome detonate() {
        return this.detonate;
    }

    public boolean acceptedHit() {
        return this.status == Status.APPLIED;
    }
}
