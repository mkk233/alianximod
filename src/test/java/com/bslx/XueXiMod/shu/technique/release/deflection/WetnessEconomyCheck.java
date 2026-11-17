package com.bslx.XueXiMod.shu.technique.release.deflection;

import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowBook;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
import com.bslx.XueXiMod.status.WetnessTiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 报价、余伤落账和同一次收购都走生产方法。不在测试里重写 D-M/r。
 */
public final class WetnessEconomyCheck {
    private static int assertionCount;
    private static int failures;

    private WetnessEconomyCheck() {
    }

    public static void main(String[] arguments) {
        section("旧报价等于零档", WetnessEconomyCheck::legacyQuote);
        section("门槛和五笔箭", WetnessEconomyCheck::examples);
        section("余伤交给下一次报价", WetnessEconomyCheck::residual);
        section("三次之后拒绝", WetnessEconomyCheck::fullLimit);
        section("同一次收购锁住档位", WetnessEconomyCheck::lockedAcquire);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未运行：tryAcquire 的服务器线程、箭实体接触、水幕和水环乘到 hurt。衰减常数只说明没有写回剩余伤害。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void legacyQuote() {
        DeflectionEconomy.Quote legacy = DeflectionEconomy.quote(30f, 24f, 0, false);
        DeflectionEconomy.Quote tierZero = DeflectionEconomy.quote(30f, 24f, 0, false, WetnessTiers.ofValue(0));
        check(sameQuote(legacy, tierZero), "四参数报价应等于 ofValue(0)，实际 " + legacy + " / " + tierZero);
        check(legacy.outcome() == DeflectionEconomy.Quote.Outcome.FULL && legacy.cost() == 24f
                        && legacy.remaining() == 6f && legacy.residualDirect() == 24f,
                "零档足额时费用等于伤害，剩余水势是 6，箭伤仍是 24，实际 " + legacy);
        DeflectionEconomy.Quote shortFunds = DeflectionEconomy.quote(15f, 24f, 0, false);
        check(shortFunds.outcome() == DeflectionEconomy.Quote.Outcome.NONE && shortFunds.cost() == 0f
                        && shortFunds.remaining() == 15f,
                "零档 15 点低于资格 20，实际 " + shortFunds);
        DeflectionEconomy.Quote melee = DeflectionEconomy.quote(20f, 8f, 3, true);
        check(melee.outcome() == DeflectionEconomy.Quote.Outcome.FULL && melee.cost() == 20f && melee.remaining() == 0f,
                "零档近战仍是 20，实际 " + melee);
    }

    private static void examples() {
        WetnessTiers.Profile second = WetnessTiers.ofValue(40);
        WetnessTiers.Profile third = WetnessTiers.ofValue(60);
        WetnessTiers.Profile fourth = WetnessTiers.ofValue(90);
        WetnessTiers.Profile first = WetnessTiers.ofValue(10);
        DeflectionEconomy.Quote partial = DeflectionEconomy.quote(15f, 24f, 0, false, second);
        check(partial.outcome() == DeflectionEconomy.Quote.Outcome.PARTIAL && partial.cost() == 15f
                        && partial.remaining() == 0f && partial.residualDirect() == 4f && !partial.spendsFullAttempt(),
                "D24 M15 L2 应部分扣 15、水势剩余 0、箭余伤 4，实际 " + partial);
        DeflectionEconomy.Quote full = DeflectionEconomy.quote(18f, 24f, 0, false, second);
        check(full.outcome() == DeflectionEconomy.Quote.Outcome.FULL && full.cost() == 18f
                        && full.remaining() == 0f && full.residualDirect() == 24f,
                "D24 M18 L2 应完整扣 18，箭伤仍是 24，实际 " + full);
        DeflectionEconomy.Quote refused = DeflectionEconomy.quote(14f, 24f, 0, false, second);
        check(refused.outcome() == DeflectionEconomy.Quote.Outcome.NONE && refused.cost() == 0f
                        && refused.remaining() == 14f && refused.residualDirect() == 0f,
                "D24 M14 L2 资格不足，实际 " + refused);
        DeflectionEconomy.Quote fourthPartial = DeflectionEconomy.quote(10f, 24f, 0, false, fourth);
        check(fourthPartial.outcome() == DeflectionEconomy.Quote.Outcome.PARTIAL && fourthPartial.cost() == 10f
                        && fourthPartial.remaining() == 0f && fourthPartial.residualDirect() == 4f,
                "D24 M10 L4 应部分扣 10、余伤 4，实际 " + fourthPartial);
        DeflectionEconomy.Quote fourthFull = DeflectionEconomy.quote(12f, 24f, 0, false, fourth);
        check(fourthFull.outcome() == DeflectionEconomy.Quote.Outcome.FULL && fourthFull.cost() == 12f
                        && fourthFull.remaining() == 0f && fourthFull.residualDirect() == 24f,
                "D24 M12 L4 应完整扣 12，箭伤仍是 24，实际 " + fourthFull);
        DeflectionEconomy.Quote roomy = DeflectionEconomy.quote(30f, 24f, 0, false, second);
        check(roomy.cost() == 18f && roomy.remaining() == 12f && roomy.residualDirect() == 24f,
                "水势剩余应是扣费后的水势 12，不是箭伤，实际 " + roomy);
        DeflectionEconomy.Quote smallArrow = DeflectionEconomy.quote(14f, 1f, 0, false, second);
        check(smallArrow.outcome() == DeflectionEconomy.Quote.Outcome.NONE && smallArrow.remaining() == 14f,
                "L2 小箭费用够但资格 15 不够，实际 " + smallArrow);
        DeflectionEconomy.Quote thirdSame = DeflectionEconomy.quote(15f, 24f, 0, false, third);
        check(sameQuote(partial, thirdSame), "L3 与 L2 的这笔记账相同，实际 " + thirdSame);
        DeflectionEconomy.Quote firstTier = DeflectionEconomy.quote(20f, 24f, 0, false, first);
        DeflectionEconomy.Quote sameAsZero = DeflectionEconomy.quote(20f, 24f, 0, false);
        check(firstTier.outcome() == DeflectionEconomy.Quote.Outcome.PARTIAL && firstTier.cost() == 20f
                        && firstTier.remaining() == 0f && firstTier.residualDirect() == 4f
                        && sameQuote(firstTier, sameAsZero),
                "L1 资格 20、比例 1，20 对 24 与 0 档一样是部分卸力，实际 " + firstTier);
        DeflectionEconomy.Quote firstShort = DeflectionEconomy.quote(19f, 1f, 0, false, first);
        check(firstShort.outcome() == DeflectionEconomy.Quote.Outcome.NONE && firstShort.remaining() == 19f,
                "L1 小箭费用够但 19 低于资格 20，实际 " + firstShort);
        DeflectionEconomy.Quote meleeSecond = DeflectionEconomy.quote(15f, 100f, 0, true, second);
        check(meleeSecond.outcome() == DeflectionEconomy.Quote.Outcome.FULL && meleeSecond.cost() == 15f
                        && meleeSecond.remaining() == 0f,
                "L2 近战 15，实际 " + meleeSecond);
        DeflectionEconomy.Quote meleeShort = DeflectionEconomy.quote(14f, 100f, 0, true, third);
        check(meleeShort.outcome() == DeflectionEconomy.Quote.Outcome.NONE, "L3 近战 14 不够资格，实际 " + meleeShort);
        DeflectionEconomy.Quote meleeFourth = DeflectionEconomy.quote(10f, 1f, 9, true, fourth);
        check(meleeFourth.outcome() == DeflectionEconomy.Quote.Outcome.FULL && meleeFourth.cost() == 10f
                        && meleeFourth.remaining() == 0f,
                "L4 近战 10，不看箭伤和完整次数，实际 " + meleeFourth);
        check(second.arrowCostRatio() == 0.75f && fourth.arrowCostRatio() == 0.5f, "报价没有改写传入的 Profile");
    }

    private static void residual() {
        CompoundTag tag = new CompoundTag();
        WetnessTiers.Profile second = WetnessTiers.ofValue(40);
        DeflectionEconomy.Quote firstQuote = DeflectionEconomy.quote(15f, 24f, 0, false, second);
        check(ArrowBook.applyCommit(tag, firstQuote, 24f, 0, true), "部分提交应落账");
        check(ArrowBook.remaining(tag) == 4f, "标签里的剩余伤害应是余伤 4，不是费用 15，实际 " + ArrowBook.remaining(tag));
        check(ArrowBook.fullCount(tag) == 0, "部分卸力不加完整次数，实际 " + ArrowBook.fullCount(tag));
        check(ArrowBook.remaining(tag) != firstQuote.cost(), "不能用水势费用代替箭伤害");
        float curtain = BarrierDefinition.curtain().attenuation();
        float ring = GuardDefinition.personalRing().unloadRatio();
        check(curtain == 0.5f && ring == 0.20f, "水幕 0.5、水环 0.20 仍是生产常数");
        check(ArrowBook.remaining(tag) != 4f * (1f - curtain) && ArrowBook.remaining(tag) != 4f * (1f - ring),
                "基础减伤没有写回剩余直接伤害");
        float carried = ArrowBook.remaining(tag);
        DeflectionEconomy.Quote later = DeflectionEconomy.quote(20f, carried, ArrowBook.fullCount(tag), false, second);
        check(later.outcome() == DeflectionEconomy.Quote.Outcome.FULL && later.cost() == 3f
                        && later.remaining() == 17f && later.residualDirect() == 4f,
                "下一次应按余伤 4 报价，费用 3，水势剩 17，实际 " + later);
        check(ArrowBook.applyCommit(tag, later, carried, 1, true), "用余伤做完整提交");
        check(ArrowBook.remaining(tag) == 4f && ArrowBook.fullCount(tag) == 1,
                "完整提交写下传入的余伤 4，不是费用 3，次数 " + ArrowBook.fullCount(tag)
                        + " 剩余 " + ArrowBook.remaining(tag));
        CompoundTag ignored = new CompoundTag();
        check(!ArrowBook.applyCommit(ignored, firstQuote, 24f, 0, false), "未提交不写剩余伤害");
        check(!ArrowBook.hasFrozen(ignored) && ArrowBook.remaining(ignored) == 0f, "失败路径保持原标签");
    }

    private static void fullLimit() {
        CompoundTag tag = new CompoundTag();
        WetnessTiers.Profile plain = WetnessTiers.ofValue(0);
        for (int round = 0; round < 3; round++) {
            int count = ArrowBook.fullCount(tag);
            DeflectionEconomy.Quote priced = DeflectionEconomy.quote(40f, 10f, count, false, plain);
            check(priced.outcome() == DeflectionEconomy.Quote.Outcome.FULL && priced.residualDirect() == 10f,
                    "第 " + (round + 1) + " 次完整报价，实际 " + priced);
            check(ArrowBook.applyCommit(tag, priced, 10f, round, true), "第 " + (round + 1) + " 次完整落账");
        }
        check(ArrowBook.fullCount(tag) == 3, "三次完整后次数是 3");
        DeflectionEconomy.Quote fourth = DeflectionEconomy.quote(100f, 24f, ArrowBook.fullCount(tag), false,
                WetnessTiers.ofValue(100));
        check(fourth.outcome() == DeflectionEconomy.Quote.Outcome.NONE && fourth.cost() == 0f && fourth.remaining() == 100f,
                "满三次后四级优惠也不能再偏转，实际 " + fourth);
        CompoundTag mixed = new CompoundTag();
        DeflectionEconomy.Quote partial = DeflectionEconomy.quote(15f, 24f, 0, false, WetnessTiers.ofValue(26));
        check(ArrowBook.applyCommit(mixed, partial, 24f, 0, true), "夹在中间的部分卸力先落账");
        for (int round = 0; round < 2; round++) {
            DeflectionEconomy.Quote priced = DeflectionEconomy.quote(40f, 10f, ArrowBook.fullCount(mixed), false, plain);
            check(ArrowBook.applyCommit(mixed, priced, 10f, round + 1, true), "部分之后的完整次数仍可增加");
        }
        check(ArrowBook.fullCount(mixed) == 2, "一次部分加两次完整，次数仍是 2，实际 " + ArrowBook.fullCount(mixed));
        DeflectionEconomy.Quote third = DeflectionEconomy.quote(40f, 10f, ArrowBook.fullCount(mixed), false, plain);
        check(third.outcome() == DeflectionEconomy.Quote.Outcome.FULL, "部分不占名额，第三次完整仍允许");
    }

    private static void lockedAcquire() {
        DeflectionService.clearBook();
        WetnessTiers.Profile second = WetnessTiers.ofValue(40);
        float ratioBefore = second.arrowCostRatio();
        ShiftingPurse shifting = new ShiftingPurse();
        QuietHost host = host(11L);
        DeflectionService.Transaction booking = DeflectionService.open(
                host,
                touch(host),
                AttackKey.projectile(new UUID(11L, 1L), 0),
                24f,
                0,
                false,
                second,
                shifting,
                false,
                4L);
        try {
            check(shifting.reads == 2, "同一次收购应读两次可用水势，实际 " + shifting.reads);
            check(booking.acquired() && booking.quote().outcome() == DeflectionEconomy.Quote.Outcome.FULL
                            && booking.quote().cost() == 18f && booking.quote().residualDirect() == 24f
                            && shifting.reservedAmount == 18f,
                    "水势从 15 变到 18 后仍用锁住的二级比例，预留 18，箭伤 24，实际 " + booking.quote()
                            + " 预留 " + shifting.reservedAmount);
            check(second.arrowCostRatio() == ratioBefore && second.tier() == 2, "收购没有改 Profile");
        } finally {
            booking.close();
        }
        DeflectionService.clearBook();
        WetnessTiers.Profile fourth = WetnessTiers.ofValue(90);
        SteadyPurse steady = new SteadyPurse(10f);
        QuietHost nextHost = host(12L);
        DeflectionService.Transaction next = DeflectionService.open(
                nextHost,
                touch(nextHost),
                AttackKey.projectile(new UUID(12L, 1L), 1),
                4f,
                0,
                false,
                fourth,
                steady,
                false,
                5L);
        try {
            check(next.acquired() && next.quote().cost() == 2f && next.quote().residualDirect() == 4f
                            && next.quote().remaining() == 8f,
                    "下一次收购应按新的四级比例扣 2，不是沿用二级的 3，实际 " + next.quote());
        } finally {
            next.close();
        }
        DeflectionService.clearBook();
        SteadyPurse plainPurse = new SteadyPurse(24f);
        QuietHost plainHost = host(13L);
        DeflectionService.Transaction plain = DeflectionService.open(
                plainHost,
                touch(plainHost),
                AttackKey.projectile(new UUID(13L, 1L), 0),
                10f,
                0,
                false,
                null,
                plainPurse,
                false,
                6L);
        try {
            check(plain.acquired() && plain.quote().cost() == 10f && plain.quote().residualDirect() == 10f
                            && plain.quote().remaining() == 14f,
                    "空 Profile 按 0 档，费用等于伤害，实际 " + plain.quote());
        } finally {
            plain.close();
        }
        DeflectionService.clearBook();
    }

    private static boolean sameQuote(DeflectionEconomy.Quote left, DeflectionEconomy.Quote right) {
        return left.outcome() == right.outcome() && left.cost() == right.cost()
                && left.remaining() == right.remaining() && left.residualDirect() == right.residualDirect();
    }

    private static HostContact touch(QuietHost host) {
        return new HostContact(host, 0.25D, new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D));
    }

    private static QuietHost host(long generation) {
        return new QuietHost(new HostKey(new UUID(generation, 7L), generation), new UUID(generation, 9L));
    }

    private static void section(String name, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException failure) {
            assertionCount++;
            failures++;
            System.out.println("失败[" + assertionCount + "] " + name + " 抛出 " + failure);
            failure.printStackTrace(System.out);
        }
    }

    private static void check(boolean condition, String detail) {
        assertionCount++;
        if (!condition) {
            failures++;
            System.out.println("失败[" + assertionCount + "] " + detail);
        }
    }

    private static final class QuietHost implements DeflectionHost {
        private final HostKey hostKey;
        private final UUID owner;

        private QuietHost(HostKey hostKey, UUID owner) {
            this.hostKey = hostKey;
            this.owner = owner;
        }

        @Override
        public UUID ownerId() {
            return this.owner;
        }

        @Override
        public HostKey key() {
            return this.hostKey;
        }

        @Override
        public ServerLevel level() {
            return null;
        }

        @Override
        public boolean active() {
            return true;
        }

        @Override
        public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
            return Optional.empty();
        }

        @Override
        public boolean covers(Vec3 point, double projectileRadius, long gameTick) {
            return false;
        }

        @Override
        public CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick) {
            return null;
        }
    }

    private static final class ShiftingPurse implements MomentumPurse {
        private int reads;
        private float reservedAmount;

        @Override
        public float available() {
            this.reads++;
            if (this.reads == 1) {
                return 15f;
            }
            return 18f;
        }

        @Override
        public Object reserve(float amount) {
            if (amount != 18f) {
                return null;
            }
            this.reservedAmount = amount;
            return new Object();
        }

        @Override
        public boolean commit(Object reservation) {
            return false;
        }

        @Override
        public void release(Object reservation) {
        }
    }

    private static final class SteadyPurse implements MomentumPurse {
        private final float balance;
        private boolean held;

        private SteadyPurse(float balance) {
            this.balance = balance;
        }

        @Override
        public float available() {
            return this.balance;
        }

        @Override
        public Object reserve(float amount) {
            if (!(amount > 0f) || amount > this.balance) {
                return null;
            }
            this.held = true;
            return new Object();
        }

        @Override
        public boolean commit(Object reservation) {
            return false;
        }

        @Override
        public void release(Object reservation) {
            this.held = false;
        }
    }
}
