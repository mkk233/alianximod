package com.bslx.XueXiMod.shu.technique.release.deflection;

import com.bslx.XueXiMod.status.WetnessTiers;

/**
 * 纯报价。不读玩家，不预留，不改随机数。
 * 四参数重载固定走 0 档。带 Profile 的重载只用调用方锁住的那一份门槛、近战费和箭比例。
 * {@code Quote.remaining} 仍是扣费后的水势。{@code residualDirect} 是箭上应留下的直接伤害：完整为 D，部分为 {@code max(0, D - M / r)}。
 */
public final class DeflectionEconomy {
    public static final float QUALIFYING_FUNDS = 20f;
    public static final int FULL_DEFLECTION_LIMIT = 3;

    private DeflectionEconomy() {
    }

    /** 用 0 档门槛报价。不读玩家、不预留。 */
    public static Quote quote(float available, float direct, int fullCount, boolean isMelee) {
        return quote(available, direct, fullCount, isMelee, WetnessTiers.ofValue(0));
    }

    /** 按传入档位报价。水不够完整偏转时，箭可以部分卸力。 */
    public static Quote quote(float available, float direct, int fullCount, boolean isMelee,
                              WetnessTiers.Profile profile) {
        WetnessTiers.Profile locked = profile == null ? WetnessTiers.ofValue(0) : profile;
        if (!Float.isFinite(available) || available < 0f) {
            return new Quote(Quote.Outcome.NONE, 0f, 0f, 0f);
        }
        float gate = locked.qualifyingFunds();
        if (isMelee) {
            return quoteMelee(available, gate, locked.meleeCost());
        }
        if (fullCount < 0 || fullCount >= FULL_DEFLECTION_LIMIT) {
            return idle(available);
        }
        if (!Float.isFinite(direct) || direct <= 0f) {
            return idle(available);
        }
        if (!Float.isFinite(gate) || gate < 0f || available < gate) {
            return idle(available);
        }
        float ratio = locked.arrowCostRatio();
        if (!Float.isFinite(ratio) || ratio <= 0f) {
            return idle(available);
        }
        float fullCost = direct * ratio;
        if (!Float.isFinite(fullCost)) {
            return idle(available);
        }
        if (available >= fullCost) {
            return new Quote(Quote.Outcome.FULL, fullCost, available - fullCost, direct);
        }
        float absorbed = available / ratio;
        float residual = direct - absorbed;
        if (!Float.isFinite(residual) || residual < 0f) {
            residual = 0f;
        }
        return new Quote(Quote.Outcome.PARTIAL, available, 0f, residual);
    }

    private static Quote quoteMelee(float available, float gate, float meleeCost) {
        if (!Float.isFinite(gate) || gate < 0f || available < gate) {
            return idle(available);
        }
        if (!Float.isFinite(meleeCost) || meleeCost < 0f || meleeCost > available) {
            return idle(available);
        }
        return new Quote(Quote.Outcome.FULL, meleeCost, available - meleeCost, 0f);
    }

    private static Quote idle(float available) {
        return new Quote(Quote.Outcome.NONE, 0f, available, 0f);
    }

    public record Quote(Outcome outcome, float cost, float remaining, float residualDirect) {
        public enum Outcome {
            /** 不偏转。 */
            NONE,
            /** 完整偏转。 */
            FULL,
            /** 只卸掉一部分直接伤害。 */
            PARTIAL
        }

        /** 完整或部分都算这次报价成交。 */
        public boolean engages() {
            return this.outcome == Outcome.FULL || this.outcome == Outcome.PARTIAL;
        }

        /** 只有完整偏转占用三次上限里的一次。 */
        public boolean spendsFullAttempt() {
            return this.outcome == Outcome.FULL;
        }
    }
}
