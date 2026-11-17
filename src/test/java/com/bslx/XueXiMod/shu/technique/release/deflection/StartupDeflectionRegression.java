package com.bslx.XueXiMod.shu.technique.release.deflection;

import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowBook;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowDamageFreeze;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;
import java.util.function.IntUnaryOperator;

/**
 * 基数 0 的暴击抽签和待定 0 走生产报价、冻结公式和内存账。
 * consider 需要服务端世界，这里不调用它。
 */
public final class StartupDeflectionRegression {
    private static int assertionCount;

    private StartupDeflectionRegression() {
    }

    public static void main(String[] arguments) {
        ArrowBook.clearPending();
        zeroRollOnce();
        oneRollStaysValid();
        quietBaseDoesNotRoll();
        pendingZeroReuseConsumeClear();
        zeroQuoteDoesNotFreeze();
        System.out.println("断言 " + assertionCount);
        System.out.println("零报价暴击和待定伤害检查通过");
    }

    private static void zeroRollOnce() {
        Roller roller = new Roller(0);
        float direct = ArrowDamageFreeze.freeze(0.0D, 0.0D, true, roller, 0f);
        check(roller.calls == 1, "基数 0 且暴击时 roller 只调用一次");
        check(roller.boundSeen == 2, "基数 0 的暴击上界是 2");
        check(direct == 0f, "roller 返回 0 时直接伤害是 0");
    }

    private static void oneRollStaysValid() {
        Roller roller = new Roller(1);
        float direct = ArrowDamageFreeze.freeze(0.0D, 0.0D, true, roller, 0f);
        check(roller.calls == 1 && roller.boundSeen == 2, "roller 返回 1 时仍然只抽一次");
        check(direct == 1f, "roller 返回 1 时直接伤害是 1");
        DeflectionEconomy.Quote priced = DeflectionEconomy.quote(40f, direct, 0, false);
        check(priced.outcome() == DeflectionEconomy.Quote.Outcome.FULL && priced.cost() == 1f
                && priced.engages() && priced.spendsFullAttempt(), "1 点伤害仍然可以完整成交");
    }

    private static void quietBaseDoesNotRoll() {
        Roller roller = new Roller(1);
        float direct = ArrowDamageFreeze.freeze(0.0D, 0.0D, false, roller, 0f);
        check(roller.calls == 0 && direct == 0f, "非暴击且基数 0 不抽随机");
    }

    private static void pendingZeroReuseConsumeClear() {
        UUID arrowId = UUID.randomUUID();
        CompoundTag tag = new CompoundTag();
        ArrowBook.rememberPending(arrowId, 0f);
        Float firstRead = ArrowBook.peekPending(arrowId);
        Float secondRead = ArrowBook.peekPending(arrowId);
        check(firstRead != null && secondRead != null && firstRead.floatValue() == 0f
                && secondRead.floatValue() == 0f, "待定 0 可以重复读取");
        check(ArrowBook.replacesHurt(tag, arrowId), "待定 0 让后续命中改走已报价");
        check(!ArrowBook.hasFrozen(tag), "待定 0 不写永久冻结");
        ArrowBook.consumePending(arrowId);
        check(ArrowBook.peekPending(arrowId) == null, "消费后待定 0 消失");
        check(!ArrowBook.replacesHurt(tag, arrowId), "消费后不再替换原伤害");
        ArrowBook.rememberPending(arrowId, 0f);
        ArrowBook.clearPending();
        check(ArrowBook.peekPending(arrowId) == null, "清理后待定 0 不留到下一次");
        ArrowBook.rememberPending(arrowId, Float.NaN);
        check(ArrowBook.peekPending(arrowId) == null, "非数不进入待定");
        ArrowBook.clearPending();
    }

    private static void zeroQuoteDoesNotFreeze() {
        DeflectionEconomy.Quote refused = DeflectionEconomy.quote(40f, 0f, 0, false);
        check(refused.outcome() == DeflectionEconomy.Quote.Outcome.NONE && refused.cost() == 0f
                && !refused.engages() && !refused.spendsFullAttempt(), "0 伤害没有费用，也不是完整或部分");
        CompoundTag tag = new CompoundTag();
        check(!ArrowBook.applyCommit(tag, refused, 0f, 0, true), "不成交的 0 伤害不落账");
        check(!tag.getBoolean(ArrowBook.QUOTED) && !ArrowBook.hasFrozen(tag), "0 伤害不留下冻结标记");
    }

    private static void check(boolean passed, String label) {
        assertionCount++;
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }

    private static final class Roller implements IntUnaryOperator {
        private final int result;
        private int calls;
        private int boundSeen = -1;

        private Roller(int result) {
            this.result = result;
        }

        @Override
        public int applyAsInt(int bound) {
            this.calls++;
            this.boundSeen = bound;
            return this.result;
        }
    }
}
