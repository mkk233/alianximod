package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;

/** A 的最早接触返回值。签名不变。handled 为假时，这一候选仍由 A 分类。 */
public final class MeteorContactResult {
    private final boolean handled;
    private final boolean continueChildren;
    private final HitVerdict verdict;

    private MeteorContactResult(boolean handled, boolean continueChildren, HitVerdict verdict) {
        this.handled = handled;
        this.continueChildren = continueChildren;
        this.verdict = verdict;
    }

    public static MeteorContactResult notHandled() {
        return new MeteorContactResult(false, false, null);
    }

    public static MeteorContactResult handled(boolean continueChildren, HitVerdict verdict) {
        return new MeteorContactResult(true, continueChildren, verdict);
    }

    public boolean handled() {
        return this.handled;
    }

    public boolean continueChildren() {
        return this.continueChildren;
    }

    public HitVerdict verdict() {
        return this.verdict;
    }
}
