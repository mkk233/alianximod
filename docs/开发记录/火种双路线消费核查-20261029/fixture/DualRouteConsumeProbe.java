package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.shu.technique.release.fire.BurnIdentity;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnLedger;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnPulse;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnSchedule;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnTrackState;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLayer;
import com.bslx.XueXiMod.shu.technique.release.fire.SegmentPlan;
import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 直接调用现行火种账本、三式计划和燃烧账。
 * 不复制扣点公式，不构造 Minecraft 实体，也不调用 FireConsumption。
 */
public final class DualRouteConsumeProbe {
    private static final double TOLERANCE = 1.0e-9d;
    private static final UUID CASTER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static int failures;

    private DualRouteConsumeProbe() {
    }

    public static void main(String[] arguments) {
        balances();
        sameObjectBothOrders();
        separateBooksSameToken();
        sharedStoredBalance();
        burnThenFreshDetonate();
        pulsesDoNotRewriteBalance();
        rejectionDoesNotSpend();
        if (failures != 0) {
            System.out.println("PROBE_FAIL " + failures);
            System.exit(1);
        }
        System.out.println("PROBE_OK");
        System.out.println("NOT_EXECUTED FireConsumption.schedule/detonate, FireSeedState.persist, AttributeStateData, LivingEntity, server thread, hurt immunity, game.");
    }

    private static void balances() {
        for (int stored : new int[] {0, 9, 10, 19, 99, 100}) {
            int wholeRequest = FireSeedMath.wholeTierSpend(stored);
            Outcome whole = spend(stored, wholeRequest, 0.0d);
            Outcome all = spend(stored, FireConsumerPlans.ALL_SEEDS, 0.0d);
            System.out.println("BALANCE stored=" + stored
                    + " wholeRequest=" + wholeRequest
                    + " wholeSpent=" + whole.spent
                    + " wholeLeft=" + whole.left
                    + " wholeMarked=" + whole.marked
                    + " allSpent=" + all.spent
                    + " allLeft=" + all.left
                    + " allTiers=" + all.tiers
                    + " allRemainder=" + all.remainder
                    + " allMarked=" + all.marked);
        }
        expect(spend(0, FireConsumerPlans.ALL_SEEDS, 0.0d).spent == 0, "0 全部消费不扣");
        expect(spend(0, FireConsumerPlans.ALL_SEEDS, 0.0d).left == 0, "0 保持 0");
        expect(!spend(0, FireConsumerPlans.ALL_SEEDS, 0.0d).marked, "0 不标记");
        Outcome nineWhole = spend(9, FireSeedMath.wholeTierSpend(9), 0.0d);
        expect(nineWhole.spent == 0 && nineWhole.left == 9 && !nineWhole.marked, "9 整档不消费");
        Outcome nineAll = spend(9, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(nineAll.spent == 9 && nineAll.left == 0 && nineAll.tiers == 0 && nineAll.remainder == 9, "9 全部消费扣完");
        Outcome tenWhole = spend(10, FireSeedMath.wholeTierSpend(10), 0.0d);
        expect(tenWhole.spent == 10 && tenWhole.left == 0 && tenWhole.remainder == 0, "10 整档扣完");
        Outcome nineteenWhole = spend(19, FireSeedMath.wholeTierSpend(19), 0.0d);
        expect(nineteenWhole.spent == 10 && nineteenWhole.left == 9 && nineteenWhole.remainder == 0, "19 整档扣 10 留 9");
        Outcome nineteenAll = spend(19, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(nineteenAll.spent == 19 && nineteenAll.left == 0 && nineteenAll.tiers == 1 && nineteenAll.remainder == 9,
                "19 全部消费扣完");
        Outcome ninetyNineWhole = spend(99, FireSeedMath.wholeTierSpend(99), 0.0d);
        expect(ninetyNineWhole.spent == 90 && ninetyNineWhole.left == 9, "99 整档扣 90 留 9");
        Outcome ninetyNineAll = spend(99, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(ninetyNineAll.spent == 99 && ninetyNineAll.left == 0 && ninetyNineAll.tiers == 9 && ninetyNineAll.remainder == 9,
                "99 全部消费扣完");
        Outcome hundredAll = spend(100, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(hundredAll.spent == 100 && hundredAll.left == 0 && hundredAll.tiers == 10 && hundredAll.remainder == 0,
                "100 全部消费后余额 0");
        Outcome hundredWhole = spend(100, FireSeedMath.wholeTierSpend(100), 0.0d);
        expect(hundredWhole.spent == 100 && hundredWhole.left == 0, "100 整档也扣完");
        Optional<com.bslx.XueXiMod.shu.technique.release.fire.BurnComponent> thrust =
                FireConsumerPlans.thrust(hundredAll.spent, hundredAll.tiers, hundredAll.remainder);
        expect(thrust.isPresent(), "100 点燎里分量来自已扣快照");
        if (thrust.isPresent()) {
            double damage = FireLayer.combine(thrust.get().base(), thrust.get().resourceAboveOne(), 0.0d);
            expect(close(damage, 36.0d), "100 点引爆火伤 36，实际 " + damage);
        }
    }

    private static void sameObjectBothOrders() {
        UUID targetId = UUID.randomUUID();
        FireCastContext cast = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount account = new FireSeedAccount(100);
        Optional<FireSeedSnapshot> preview = account.preview(cast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(preview.isPresent() && preview.get().amount() == 100 && account.balance() == 100 && !cast.hasConsumed(targetId),
                "预览不扣 100");
        Optional<SegmentPlan> spread = FireConsumerPlans.spread(preview.get().amount(), preview.get().tiers(),
                preview.get().remainder());
        expect(spread.isPresent(), "燎原计划在扣款前能建成");
        Optional<FireSeedSnapshot> burned = account.consume(cast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(burned.isPresent() && burned.get().amount() == 100 && account.balance() == 0 && cast.hasConsumed(targetId),
                "同一对象先走燃烧扣款后余额 0");
        expect(burned.get().amount() == preview.get().amount() && burned.get().tiers() == preview.get().tiers()
                && burned.get().remainder() == preview.get().remainder(), "稳定账上预览和实扣快照一致");
        Optional<FireSeedSnapshot> detonated = account.consume(cast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(detonated.isEmpty() && account.balance() == 0 && cast.hasConsumed(targetId), "先燃烧后引爆不再扣");

        FireCastContext reverse = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount reverseAccount = new FireSeedAccount(80);
        Optional<FireSeedSnapshot> firstStrike = reverseAccount.consume(reverse, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(firstStrike.isPresent() && firstStrike.get().amount() == 80 && reverseAccount.balance() == 0,
                "先引爆扣掉 80");
        Optional<com.bslx.XueXiMod.shu.technique.release.fire.BurnComponent> strike =
                FireConsumerPlans.thrust(firstStrike.get().amount(), firstStrike.get().tiers(), firstStrike.get().remainder());
        expect(strike.isPresent() && close(FireLayer.combine(strike.get().base(), strike.get().resourceAboveOne(),
                firstStrike.get().furnaceBonus()), 28.8d), "引爆伤害读取本次实扣 80");
        expect(reverseAccount.consume(reverse, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty()
                && reverseAccount.balance() == 0, "先引爆后燃烧不再扣");

        FireCastContext partial = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount partialAccount = new FireSeedAccount(19);
        Optional<FireSeedSnapshot> ten = partialAccount.consume(partial, targetId, 10, 0.0d);
        expect(ten.isPresent() && partialAccount.balance() == 9, "先扣 10 后剩 9");
        expect(partialAccount.consume(partial, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty()
                && partialAccount.balance() == 9, "同一对象不能再拿剩余 9");
    }

    private static void separateBooksSameToken() {
        UUID token = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        FireCastContext firstBook = new FireCastContext(token, CASTER);
        FireCastContext secondBook = new FireCastContext(token, CASTER);
        FireSeedAccount account = new FireSeedAccount(19);
        Optional<FireSeedSnapshot> first = account.consume(firstBook, targetId, FireSeedMath.wholeTierSpend(19), 0.0d);
        expect(first.isPresent() && account.balance() == 9, "第一本账扣掉整档");
        Optional<FireSeedSnapshot> second = account.consume(secondBook, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(second.isPresent() && second.get().amount() == 9 && account.balance() == 0,
                "同标识的新对象可以第二次消费");
        expect(second.get().castId().equals(token) && first.get().castId().equals(token), "两本账标识相同");
    }

    private static void sharedStoredBalance() {
        UUID targetId = UUID.randomUUID();
        int stored = 40;
        FireCastContext casterOne = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount opened = new FireSeedAccount(stored);
        Optional<FireSeedSnapshot> preview = opened.preview(casterOne, targetId, FireConsumerPlans.ALL_SEEDS, 0.25d);
        expect(preview.isPresent() && opened.balance() == 40, "第一次读入的预览不改存储");
        FireSeedAccount committing = new FireSeedAccount(stored);
        Optional<FireSeedSnapshot> taken = committing.consume(casterOne, targetId, FireConsumerPlans.ALL_SEEDS, 0.25d);
        expect(taken.isPresent() && taken.get().amount() == 40, "第一次实扣 40");
        if (stored != committing.balance()) {
            stored = committing.balance();
        }
        expect(stored == 0, "写回后的共享余额是 0");
        FireCastContext casterTwo = new FireCastContext(UUID.randomUUID(), OTHER);
        FireSeedAccount reread = new FireSeedAccount(stored);
        expect(reread.consume(casterTwo, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty() && reread.balance() == 0,
                "另一个新上下文读到同一份 0");
        int gained = reread.grant(casterTwo, targetId, 12, 1.0d);
        if (stored != reread.balance()) {
            stored = reread.balance();
        }
        expect(gained == 12 && stored == 12, "新上下文把后续火种写入同一份余额");
        FireCastContext casterThree = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount again = new FireSeedAccount(stored);
        Optional<FireSeedSnapshot> later = again.consume(casterThree, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(later.isPresent() && later.get().amount() == 12 && again.balance() == 0, "第三个上下文扣到刚写入的 12");
        expect(committing.consume(casterOne, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty(),
                "已经消费过的旧对象不能扣新余额");
    }

    private static void burnThenFreshDetonate() {
        UUID targetId = UUID.randomUUID();
        FireCastContext burnCast = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount account = new FireSeedAccount(100);
        Optional<FireSeedSnapshot> burned = account.consume(burnCast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(burned.isPresent() && account.balance() == 0, "燃烧提交时 100 已变成 0");
        SegmentPlan spread = FireConsumerPlans.spread(burned.get().amount(), burned.get().tiers(), burned.get().remainder())
                .orElseThrow();
        BurnLedger ledger = new BurnLedger();
        boolean mounted = ledger.admit(schedule(FireConsumerPlans.SPREAD_KEY, false, spread), identity(burned.get()));
        expect(mounted && ledger.trackCount() == 1, "燃烧轨道挂上");
        int quiet = 0;
        while (quiet < 5) {
            expect(ledger.advance().isEmpty(), "间隔未到没有脉冲");
            quiet += 1;
        }
        expect(account.balance() == 0 && ledger.export().get(0).amount() == 100, "推进不回写旧余额，轨道仍记 100");
        FireCastContext producer = new FireCastContext(UUID.randomUUID(), OTHER);
        int gained = account.grant(producer, targetId, 30, 1.0d);
        expect(gained == 30 && account.balance() == 30, "燃烧期间新上下文可以补 30");
        expect(ledger.conflicts(FireConsumerPlans.SPREAD_KEY, CASTER, false), "同施法者燎原键冲突");
        expect(!ledger.conflicts(FireConsumerPlans.SPREAD_KEY, OTHER, false), "另一施法者燎原键不冲突");
        expect(!ledger.conflicts(FireConsumerPlans.SKY_KEY, OTHER, true), "焚天键和燎原键不互相冲突");
        expect(account.grant(burnCast, targetId, 30, 1.0d) == 0 && account.balance() == 30,
                "已经消费的那本账不能再产");
        expect(account.consume(burnCast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty() && account.balance() == 30,
                "已经消费的那本账不能引爆新火种");
        FireCastContext detonator = new FireCastContext(UUID.randomUUID(), OTHER);
        Optional<FireSeedSnapshot> struck = account.consume(detonator, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(struck.isPresent() && struck.get().amount() == 30 && account.balance() == 0,
                "燃烧未结束时新上下文仍可引爆补上的 30");
        expect(ledger.trackCount() == 1 && ledger.export().get(0).amount() == 100, "引爆不取消原燃烧，也不改原快照");
        expect(ledger.conflicts("xueximod:fire_liaoli", OTHER, true) == false, "引爆没有目标级效果键");
    }

    private static void pulsesDoNotRewriteBalance() {
        UUID targetId = UUID.randomUUID();
        FireCastContext cast = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount account = new FireSeedAccount(10);
        FireSeedSnapshot taken = account.consume(cast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).orElseThrow();
        expect(account.balance() == 0, "10 点在挂载前已经扣完");
        SegmentPlan spread = FireConsumerPlans.spread(taken.amount(), taken.tiers(), taken.remainder()).orElseThrow();
        BurnLedger ledger = new BurnLedger();
        expect(ledger.admit(schedule(FireConsumerPlans.SPREAD_KEY, false, spread), identity(taken)), "10 点燎原挂上");
        ArrayList<Double> damages = new ArrayList<>();
        int steps = 0;
        int limit = spread.components().size() * spread.intervalTicks() + 2;
        while (ledger.trackCount() > 0 && steps < limit) {
            for (BurnPulse pulse : ledger.advance()) {
                damages.add(pulse.damage());
            }
            steps += 1;
            if (account.balance() != 0) {
                break;
            }
        }
        expect(account.balance() == 0, "全部脉冲之后余额仍是 0");
        expect(damages.size() == 4 && steps == 80, "10 点燎原 4 段、20 间隔，步数 " + steps + " 段数 " + damages.size());
        expect(close(damages.get(0), 2.2d) && close(damages.get(3), 2.2d), "段伤保持挂载时的 2.2");
        FireCastContext late = new FireCastContext(UUID.randomUUID(), CASTER);
        expect(account.consume(late, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty(), "打完后没有余额可再扣");

        FireSeedAccount full = new FireSeedAccount(100);
        FireCastContext skyCast = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedSnapshot skyTaken = full.consume(skyCast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).orElseThrow();
        SegmentPlan sky = FireConsumerPlans.sky(skyTaken.amount(), skyTaken.tiers(), skyTaken.remainder()).orElseThrow();
        BurnLedger skyLedger = new BurnLedger();
        expect(skyLedger.admit(schedule(FireConsumerPlans.SKY_KEY, true, sky), identity(skyTaken)), "100 点焚天挂上");
        int skySteps = 0;
        int skyHits = 0;
        while (skyLedger.trackCount() > 0 && skySteps < sky.components().size() * sky.intervalTicks() + 2) {
            skyHits += skyLedger.advance().size();
            skySteps += 1;
        }
        expect(full.balance() == 0 && skyHits == 10 && skySteps == 40, "焚天 10 段打完余额仍是 0");
        BurnTrackState gone = skyLedger.export().stream().findFirst().orElse(null);
        expect(gone == null, "段打完后轨道不保留可回写的余额");
    }

    private static void rejectionDoesNotSpend() {
        UUID targetId = UUID.randomUUID();
        FireCastContext cast = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount account = new FireSeedAccount(19);
        expect(account.preview(cast, targetId, 0, 0.0d).isEmpty() && account.balance() == 19 && !cast.hasConsumed(targetId),
                "请求 0 不扣");
        expect(account.consume(cast, targetId, 0, 0.0d).isEmpty() && account.balance() == 19 && !cast.hasConsumed(targetId),
                "消费请求 0 不标记");
        expect(account.consume(cast, targetId, -3, 0.0d).isEmpty() && account.balance() == 19, "负数请求不扣");
        expect(account.consume(cast, targetId, 10, Double.NaN).isEmpty() && account.balance() == 19, "非数炉心不扣");
        expect(account.consume(cast, targetId, 10, -1.0d).isEmpty() && !cast.hasConsumed(targetId), "负炉心不标记");
        expect(FireConsumerPlans.spread(0, 0, 0).isEmpty() && FireConsumerPlans.sky(0, 0, 0).isEmpty()
                && FireConsumerPlans.thrust(0, 0, 0).isEmpty(), "0 点三式都没有计划");
        expect(BurnSchedule.of("Bad Key", false, 4, List.of()).isEmpty(), "非法效果键没有计划");
        expect(account.balance() == 19, "无效计划检查没有顺手扣款");

        FireSeedSnapshot taken = account.consume(cast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).orElseThrow();
        BurnLedger ledger = new BurnLedger();
        SegmentPlan spread = FireConsumerPlans.spread(taken.amount(), taken.tiers(), taken.remainder()).orElseThrow();
        expect(ledger.admit(schedule(FireConsumerPlans.SPREAD_KEY, false, spread), identity(taken)), "先挂一条燎原");
        FireSeedAccount untouched = new FireSeedAccount(50);
        expect(ledger.conflicts(FireConsumerPlans.SPREAD_KEY, CASTER, false) && untouched.balance() == 50,
                "冲突判断本身不改另一份余额");
        int filled = 1;
        while (filled < 8) {
            String effectKey = "xueximod:probe_" + filled;
            SegmentPlan plan = FireConsumerPlans.spread(taken.amount(), taken.tiers(), taken.remainder()).orElseThrow();
            FireCastContext extra = new FireCastContext(UUID.randomUUID(), UUID.randomUUID());
            expect(ledger.admit(schedule(effectKey, false, plan), identity(taken, extra)), "补轨道 " + filled);
            filled += 1;
        }
        expect(ledger.trackCount() == 8, "容量到 8");
        SegmentPlan overflow = FireConsumerPlans.spread(taken.amount(), taken.tiers(), taken.remainder()).orElseThrow();
        expect(!ledger.admit(schedule("xueximod:probe_overflow", false, overflow), identity(taken)), "第 9 条挂不上");
        expect(untouched.balance() == 50, "容量拒绝没有扣那份 50");
    }

    private static Outcome spend(int stored, int requested, double furnaceBonus) {
        FireCastContext cast = new FireCastContext(UUID.randomUUID(), CASTER);
        FireSeedAccount account = new FireSeedAccount(stored);
        Optional<FireSeedSnapshot> preview = account.preview(cast, UUID.randomUUID(), requested, furnaceBonus);
        if (account.balance() != stored || cast.hasConsumed(preview.map(FireSeedSnapshot::targetId).orElse(null))) {
            failures += 1;
            System.out.println("FAIL 预览改动了 " + stored);
        }
        UUID targetId = UUID.randomUUID();
        FireSeedAccount committing = new FireSeedAccount(stored);
        FireCastContext committingCast = new FireCastContext(UUID.randomUUID(), CASTER);
        Optional<FireSeedSnapshot> taken = committing.consume(committingCast, targetId, requested, furnaceBonus);
        Outcome outcome = new Outcome();
        outcome.left = committing.balance();
        outcome.marked = committingCast.hasConsumed(targetId);
        if (taken.isPresent()) {
            outcome.spent = taken.get().amount();
            outcome.tiers = taken.get().tiers();
            outcome.remainder = taken.get().remainder();
            if (preview.isEmpty() || preview.get().amount() != taken.get().amount()) {
                failures += 1;
                System.out.println("FAIL 预览和实扣不一致 stored=" + stored + " requested=" + requested);
            }
        }
        return outcome;
    }

    private static BurnSchedule schedule(String effectKey, boolean targetExclusive, SegmentPlan plan) {
        return BurnSchedule.of(effectKey, targetExclusive, plan.intervalTicks(), plan.components()).orElseThrow();
    }

    private static BurnIdentity identity(FireSeedSnapshot taken) {
        return identity(taken, new FireCastContext(taken.castId(), taken.casterId()));
    }

    private static BurnIdentity identity(FireSeedSnapshot taken, FireCastContext cast) {
        return new BurnIdentity(cast.castId(), cast.casterId(), taken.targetId(), taken.amount(), taken.tiers(),
                taken.remainder(), taken.furnaceBonus());
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= TOLERANCE;
    }

    private static void expect(boolean condition, String label) {
        if (!condition) {
            failures += 1;
            System.out.println("FAIL " + label);
            return;
        }
        System.out.println("OK " + label);
    }

    private static final class Outcome {
        private int spent;
        private int left;
        private int tiers;
        private int remainder;
        private boolean marked;
    }
}
