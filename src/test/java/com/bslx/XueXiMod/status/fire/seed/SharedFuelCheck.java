package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.shu.technique.release.fire.BurnDispatch;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnLedger;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnPulse;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnTrackState;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLimits;
import com.bslx.XueXiMod.shu.technique.release.fire.FuelMode;
import com.bslx.XueXiMod.shu.technique.release.fire.PumpResult;
import com.bslx.XueXiMod.shu.technique.release.fire.SharedSpec;
import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 共享燃料的纯状态断言。不创建实体，不写 NBT，不走 hurt，也不启动游戏。
 */
public final class SharedFuelCheck {
    private static final double TOLERANCE = 1.0e-6d;
    private static final UUID CASTER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
    private static final UUID TARGET = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
    private static int assertionCount;
    private static int failures;

    private SharedFuelCheck() {
    }

    public static void main(String[] programArguments) {
        declareDefaultDamage();
        section("分配", SharedFuelCheck::allocation);
        section("一层两段", SharedFuelCheck::oneLayerTwoSegments);
        section("两层同拍充足", SharedFuelCheck::twoLayersFull);
        section("不足按比例", SharedFuelCheck::scarceSplit);
        section("进行中补种", SharedFuelCheck::grantWhileWaiting);
        section("余额归零不复活", SharedFuelCheck::zeroDoesNotRevive);
        section("先后手", SharedFuelCheck::orderContest);
        section("挂载去重", SharedFuelCheck::mountDedup);
        section("同拍不重复扣", SharedFuelCheck::repeatTick);
        section("焚天动态", SharedFuelCheck::skyDynamic);
        section("旧预付与恢复", SharedFuelCheck::prepaidAndRestore);
        section("版本区分", SharedFuelCheck::versions);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：LivingEntity、CompoundTag 往返、无敌帧、护甲与抗火。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void allocation() {
        int[] equal = FuelShare.divide(15, new int[] {10, 10}, new long[] {11L, 22L}, 3L);
        check(equal.length == 2 && equal[0] + equal[1] == 15, "15 点按需求分完");
        check(equal[0] <= 10 && equal[1] <= 10, "单层不超过自己的需求");
        check((equal[0] == 8 && equal[1] == 7) || (equal[0] == 7 && equal[1] == 8), "15 点是 8 和 7");
        int[] ample = FuelShare.divide(40, new int[] {10, 10}, new long[] {11L, 22L}, 3L);
        check(ample[0] == 10 && ample[1] == 10, "燃料够时各自拿满需求");
        int firstWins = 0;
        int secondWins = 0;
        for (long turn = 0L; turn < 64L; turn++) {
            int[] onePoint = FuelShare.divide(1, new int[] {10, 10}, new long[] {11L, 22L}, turn);
            check(onePoint[0] + onePoint[1] == 1, "1 点只给一层");
            if (onePoint[0] == 1) {
                firstWins += 1;
            }
            if (onePoint[1] == 1) {
                secondWins += 1;
            }
        }
        check(firstWins > 0 && secondWins > 0, "余数平局会随回合轮换，不固定给第一层");
    }

    private static void oneLayerTwoSegments() {
        FireSeedAccount account = new FireSeedAccount(20);
        BurnLedger ledger = mount("layer", UUID.randomUUID(), 1, 2, 4.0d, account.balance());
        PumpResult first = settle(ledger, account, 1L);
        PumpResult second = settle(ledger, account, 2L);
        check(first.pulses().size() == 1 && close(first.pulses().get(0).damage(), 4.0d), "第一段按实付打满");
        check(second.pulses().size() == 1 && close(second.pulses().get(0).damage(), 4.0d), "第二段继续打满");
        check(account.balance() == 0 && ledger.trackCount() == 0, "两段之后余额和轨道都结束");
    }

    private static void twoLayersFull() {
        FireSeedAccount account = new FireSeedAccount(20);
        BurnLedger ledger = new BurnLedger();
        check(admit(ledger, "alpha", UUID.randomUUID(), 1, 1, 4.0d, 20), "第一层挂上");
        check(admit(ledger, "beta", UUID.randomUUID(), 1, 1, 6.0d, 20), "第二层挂上");
        PumpResult result = settle(ledger, account, 1L);
        check(result.spent() == 20 && result.pulses().size() == 2, "同拍两层都付满");
        check(close(result.pulses().get(0).damage(), 4.0d) && close(result.pulses().get(1).damage(), 6.0d),
                "燃料够时各自保持完整段");
        check(account.balance() == 0 && ledger.trackCount() == 0, "两层把余额用完后一起结束");
    }

    private static void scarceSplit() {
        FireSeedAccount account = new FireSeedAccount(15);
        BurnLedger ledger = new BurnLedger();
        check(admit(ledger, "alpha", UUID.randomUUID(), 1, 1, 4.0d, 15), "不足时第一层");
        check(admit(ledger, "beta", UUID.randomUUID(), 1, 1, 4.0d, 15), "不足时第二层");
        PumpResult result = settle(ledger, account, 7L);
        check(result.spent() == 15 && account.balance() == 0, "实扣不超过余额");
        check(result.pulses().size() == 2, "两层都有实付就都打");
        double first = result.pulses().get(0).damage();
        double second = result.pulses().get(1).damage();
        check(close(first + second, 4.0d * 15.0d / 10.0d), "伤害按实付比例缩放且整数守恒");
        check(!close(first, 4.0d) && !close(second, 4.0d), "没有一层拿走全部 10 点");
    }

    private static void grantWhileWaiting() {
        FireSeedAccount account = new FireSeedAccount(15);
        BurnLedger ledger = mount("waiting", UUID.randomUUID(), 4, 3, 2.0d, account.balance());
        PumpResult first = advanceUntilDue(ledger, account, 4, 1L);
        check(first.pulses().size() == 1 && account.balance() == 5 && ledger.trackCount() == 1, "先付 10 点后轨道还在");
        int gained = account.grant(new FireCastContext(UUID.randomUUID(), CASTER), TARGET, 10, 1.0d);
        check(gained == 10 && account.balance() == 15, "等待期间补进的火种进同一余额");
        PumpResult second = advanceUntilDue(ledger, account, 4, 10L);
        check(second.pulses().size() == 1 && close(second.pulses().get(0).damage(), 2.0d), "后续段花掉补进的火种");
        check(account.balance() == 5, "补种之后仍按段扣");
    }

    private static void zeroDoesNotRevive() {
        FireSeedAccount account = new FireSeedAccount(10);
        BurnLedger ledger = mount("ending", UUID.randomUUID(), 1, 3, 2.0d, account.balance());
        PumpResult spent = settle(ledger, account, 1L);
        check(spent.pulses().size() == 1 && ledger.trackCount() == 0 && account.balance() == 0, "付完归零就结束");
        int gained = account.grant(new FireCastContext(UUID.randomUUID(), CASTER), TARGET, 20, 1.0d);
        PumpResult later = settle(ledger, account, 2L);
        check(gained == 20 && later.pulses().isEmpty() && later.spent() == 0 && ledger.trackCount() == 0,
                "结束后补种不把旧层叫回来");
    }

    private static void orderContest() {
        FireSeedAccount detonated = new FireSeedAccount(20);
        BurnLedger waitingBurn = mount("burn", UUID.randomUUID(), 1, 2, 4.0d, detonated.balance());
        detonated.consume(new FireCastContext(UUID.randomUUID(), CASTER), TARGET, Integer.MAX_VALUE, 0.0d);
        PumpResult afterDetonate = settle(waitingBurn, detonated, 1L);
        check(detonated.balance() == 0 && afterDetonate.pulses().isEmpty() && waitingBurn.trackCount() == 0,
                "引爆先手清空后燃烧没有燃料");
        FireSeedAccount burned = new FireSeedAccount(20);
        BurnLedger partial = mount("burn", UUID.randomUUID(), 1, 2, 4.0d, burned.balance());
        PumpResult firstBurn = settle(partial, burned, 1L);
        check(firstBurn.spent() == 10 && burned.balance() == 10, "燃烧先手只拿走这一段");
        var remainder = burned.consume(new FireCastContext(UUID.randomUUID(), CASTER), TARGET, Integer.MAX_VALUE, 0.0d);
        check(remainder.isPresent() && remainder.get().amount() == 10 && burned.balance() == 0, "引爆只拿到剩余");
    }

    private static void mountDedup() {
        FireSeedAccount account = new FireSeedAccount(100);
        BurnLedger ledger = new BurnLedger();
        UUID castId = UUID.randomUUID();
        SharedSpec spec = FireConsumerPlans.spreadSpec(20, 0.0d).orElseThrow();
        check(ledger.admitShared("field", spec, castId, CASTER, TARGET, 20, 0, 0.0d, 1L), "第一次挂上");
        check(!ledger.admitShared("field", spec, castId, CASTER, TARGET, 20, 0, 0.0d, 2L), "同一次施法不重复挂");
        check(ledger.admitShared("field", spec, UUID.randomUUID(), CASTER, TARGET, 20, 0, 0.0d, 3L), "独立释放可以叠");
        check(account.balance() == 100, "挂载失败或成功都不预扣");
        BurnLedger capped = new BurnLedger();
        int mounted = 0;
        for (int cursor = 0; cursor < FireLimits.PLAN_CAP; cursor++) {
            if (capped.admitShared("slot_" + cursor, spec, UUID.randomUUID(), CASTER, TARGET, 20, 0, 0.0d, 0L)) {
                mounted += 1;
            }
        }
        check(mounted == FireLimits.PLAN_CAP, "容量允许八条");
        check(!capped.admitShared("slot_extra", spec, UUID.randomUUID(), CASTER, TARGET, 20, 0, 0.0d, 0L),
                "第九条在扣点前拒绝");
        check(account.balance() == 100, "拒绝挂载没有动余额");
    }

    private static void repeatTick() {
        PurseMemory memory = new PurseMemory(30);
        BurnLedger ledger = memory.read();
        check(admit(ledger, "due", UUID.randomUUID(), 4, 2, 4.0d, 30), "间隔 4 的层");
        memory.write(ledger);
        for (long turn = 1L; turn <= 3L; turn++) {
            BurnDispatch.tick(memory, pulse -> memory.strikes += 1, memory, turn);
        }
        check(memory.strikes == 0 && memory.balance() == 30, "没到期的拍不扣");
        int balanceBeforeStrike = 0;
        BurnDispatch.tick(memory, pulse -> {
            memory.strikes += 1;
            memory.grantDuringStrike = 7;
            BurnDispatch.tick(memory, nested -> memory.nestedStrikes += 1, memory, 4L);
            BurnLedger fresh = memory.read();
            fresh.admitShared("late", FireConsumerPlans.spreadSpec(20, 0.0d).orElseThrow(), UUID.randomUUID(),
                    CASTER, TARGET, memory.balance(), 0, 0.0d, 9L);
            memory.write(fresh);
        }, memory, 4L);
        balanceBeforeStrike = 30 - 10 + 7;
        check(memory.strikes == 1 && memory.nestedStrikes == 0, "同一段的迟到回调不再扣一次");
        check(memory.balance() == balanceBeforeStrike, "回调里的补种留在已扣余额上");
        List<BurnTrackState> stored = memory.read().export();
        check(stored.size() == 2, "回调新挂的层没有被旧账本盖掉");
    }

    private static void skyDynamic() {
        declareDefaultDamage();
        SharedSpec spec = FireConsumerPlans.skySpec(10, 0.0d).orElseThrow();
        check(spec.segmentLimit() == 10 && spec.intervalTicks() == 4 && spec.dynamicDemand(),
                "10 点仍是 10 段、间隔 4，需求按当时余额取整");
        FireSeedAccount account = new FireSeedAccount(10);
        BurnLedger ledger = new BurnLedger();
        check(ledger.admitShared(FireConsumerPlans.SKY_KEY, spec, UUID.randomUUID(), CASTER, TARGET, 10, 0, 0.0d, 0L),
                "焚天挂上");
        account.grant(new FireCastContext(UUID.randomUUID(), CASTER), TARGET, 90, 1.0d);
        int steps = 0;
        ArrayList<Double> damages = new ArrayList<>();
        int spent = 0;
        while (ledger.trackCount() > 0 && steps < 80) {
            PumpResult result = ledger.pump(account.balance(), steps + 1L);
            account.draw(result.spent());
            spent += result.spent();
            for (BurnPulse pulse : result.pulses()) {
                damages.add(pulse.damage());
            }
            steps += 1;
        }
        check(damages.size() == 10 && steps == 40 && spent == 100, "补种不延长 40 刻，但提高后续消耗");
        check(close(damages.get(0), 33.958682112d) && close(damages.get(9), 9.0d),
                "挂载后补满的首段按 100 点计，末段仍消耗 10 点");
    }

    private static void prepaidAndRestore() {
        ArrayList<Double> sealed = new ArrayList<>();
        sealed.add(2.0d);
        sealed.add(3.0d);
        BurnTrackState prepaid = new BurnTrackState("old_burn", false, UUID.randomUUID(), CASTER, TARGET, 20, 2, 0,
                0.0d, 4, 1, 0, sealed);
        BurnLedger ledger = BurnLedger.restore(List.of(prepaid));
        FireSeedAccount account = new FireSeedAccount(20);
        PumpResult result = settle(ledger, account, 1L);
        check(result.spent() == 0 && account.balance() == 20 && result.pulses().size() == 1
                && close(result.pulses().get(0).damage(), 2.0d), "旧预付轨道回放不再扣火种");
        BurnLedger shared = mount("shared", UUID.randomUUID(), 1, 3, 4.0d, 30);
        FireSeedAccount fuel = new FireSeedAccount(30);
        PumpResult opened = settle(shared, fuel, 1L);
        check(opened.pulses().size() == 1 && opened.pulses().get(0).ordinal() == 0, "第一段序号是 0");
        BurnLedger loaded = BurnLedger.restore(shared.export());
        PumpResult continued = settle(loaded, fuel, 2L);
        check(continued.pulses().size() == 1 && continued.pulses().get(0).ordinal() == 1, "恢复后不重打已经结算的段");
    }

    private static void versions() {
        check(FuelMode.PREPAID.equals(FuelMode.kind(FireLimits.PREPAID_VERSION, FuelMode.SHARED)),
                "版本 1 即使带着共享字样也按预付");
        check(FuelMode.SHARED.equals(FuelMode.kind(FireLimits.NBT_VERSION, FuelMode.SHARED)), "版本 2 明确共享");
        check(FuelMode.PREPAID.equals(FuelMode.kind(FireLimits.NBT_VERSION, "")), "版本 2 缺标记按预付");
        check(FuelMode.SHARED.equals(FuelMode.kind(FireLimits.CONFIGURED_VERSION, FuelMode.SHARED)),
                "版本 3 明确共享");
        check(FuelMode.kind(FireLimits.CONFIGURED_VERSION + 1, FuelMode.SHARED) == null, "其它版本整份丢弃");
    }

    private static BurnLedger mount(String effectKey, UUID castId, int intervalTicks, int segmentLimit,
                                    double fullDamage, int observedBalance) {
        BurnLedger ledger = new BurnLedger();
        check(admit(ledger, effectKey, castId, intervalTicks, segmentLimit, fullDamage, observedBalance),
                effectKey + "挂上");
        return ledger;
    }

    private static boolean admit(BurnLedger ledger, String effectKey, UUID castId, int intervalTicks,
                                 int segmentLimit, double fullDamage, int observedBalance) {
        SharedSpec spec = SharedSpec.of(intervalTicks, segmentLimit, FireLimits.STANDARD_SEGMENT_DEMAND, fullDamage,
                0).orElseThrow();
        return ledger.admitShared(effectKey, spec, castId, CASTER, TARGET, observedBalance, observedBalance % 10,
                0.0d, 0L);
    }

    private static PumpResult settle(BurnLedger ledger, FireSeedAccount account, long turn) {
        PumpResult result = ledger.pump(account.balance(), turn);
        account.draw(result.spent());
        return result;
    }

    private static PumpResult advanceUntilDue(BurnLedger ledger, FireSeedAccount account, int intervalTicks,
                                              long turn) {
        PumpResult result = null;
        for (int cursor = 0; cursor < intervalTicks; cursor++) {
            result = settle(ledger, account, turn + cursor);
        }
        return result;
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= TOLERANCE;
    }

    private static void declareDefaultDamage() {
        if (!BalanceRegistry.isDeclared("fire_fentian", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_fentian").damageMultiplier().build());
        }
        if (!BalanceRegistry.isDeclared("fire_liaoyuan", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_liaoyuan").damageMultiplier().build());
        }
    }

    private static void section(String title, Runnable body) {
        int before = failures;
        body.run();
        System.out.println(title + (failures == before ? " 通过" : " 失败"));
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }

    private static final class PurseMemory implements BurnDispatch.Memory, BurnDispatch.FuelPurse {
        private BurnLedger ledger = new BurnLedger();
        private int balance;
        private int grantDuringStrike;
        private int strikes;
        private int nestedStrikes;
        private boolean alive = true;

        private PurseMemory(int opening) {
            this.balance = opening;
        }

        @Override
        public int balance() {
            return this.balance + this.grantDuringStrike;
        }

        @Override
        public void commitSpent(int spent) {
            if (spent <= 0) {
                return;
            }
            if (spent >= this.balance) {
                this.balance = 0;
                return;
            }
            this.balance -= spent;
        }

        @Override
        public BurnLedger read() {
            return BurnLedger.restore(this.ledger.export());
        }

        @Override
        public void write(BurnLedger ledger) {
            this.ledger = BurnLedger.restore(ledger.export());
        }

        @Override
        public long generation() {
            return 1L;
        }

        @Override
        public void discard() {
            this.ledger = new BurnLedger();
        }

        @Override
        public boolean alive() {
            return this.alive;
        }
    }
}
