package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;
import com.bslx.XueXiMod.status.fire.seed.FuelShare;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 焚天动态燃尽。只跑纯账本，不创建实体，不写存档，也不启动游戏。 */
public final class SkyDynamicBurnCheck {
    private static final double TOLERANCE = 1.0e-4d;
    private static final UUID CASTER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID TARGET = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static int assertionCount;
    private static int failures;

    private SkyDynamicBurnCheck() {
    }

    public static void main(String[] arguments) {
        declareDefaultDamage();
        section("向上取整", SkyDynamicBurnCheck::ceiling);
        section("无补充", SkyDynamicBurnCheck::steady);
        section("中途补火", SkyDynamicBurnCheck::refill);
        section("炉心", SkyDynamicBurnCheck::furnace);
        section("零消耗", SkyDynamicBurnCheck::zero);
        section("耗尽后结束", SkyDynamicBurnCheck::exhaustion);
        section("空窗补火", SkyDynamicBurnCheck::refillAfterEmpty);
        section("燎原不变", SkyDynamicBurnCheck::spreadUnchanged);
        section("同拍分账", SkyDynamicBurnCheck::sharedContest);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：实体 NBT 往返、真实补种、无敌帧与护甲。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void ceiling() {
        check(FireConsumerPlans.skyDemand(100, 10) == 10, "整除时需求就是 S/R");
        check(FireConsumerPlans.skyDemand(9, 10) == 1, "不足一段时至少取 1");
        check(FireConsumerPlans.skyDemand(0, 4) == 0, "没有余额时需求是 0");
        check(FireConsumerPlans.skyDemand(21, 10) == 3, "21 点对 10 段向上取整为 3");
    }

    private static void steady() {
        Playback plain = play(100, 0.0d, 0, 0);
        check(plain.pulses.size() == 10 && plain.ticks == 40 && plain.spent == 100 && plain.remaining == 0,
                "100 点十段后烧完");
        check(near(plain.total, 185.752092672d), "F=0 合计约 185.752093");
        Playback ten = play(10, 0.0d, 0, 0);
        check(ten.pulses.size() == 10 && near(ten.pulses.get(0), 3.6d) && near(ten.pulses.get(9), 3.509199688d),
                "10 点分成十段，每段 1 点，基础 3 不缩水");
    }

    private static void refill() {
        Playback plain = play(100, 0.0d, 5, 50);
        Playback furnace = play(100, 0.6d, 5, 50);
        check(plain.pulses.size() == 10 && plain.spent == 150 && plain.remaining == 0, "补 50 后仍是十段并烧完 150");
        check(near(plain.total, 311.013866496d), "补火后 F=0 约 311.013866");
        check(near(furnace.total, 356.013866496d), "补火后 F=0.6 约 356.013866");
        check(plain.demands.get(5) == 20 && plain.demands.get(0) == 10, "第六段按补后余额提高需求");
    }

    private static void furnace() {
        Playback boosted = play(100, 0.6d, 0, 0);
        check(boosted.pulses.size() == 10 && near(boosted.total, 215.752092672d), "F=0.6 约 215.752093");
        check(near(boosted.pulses.get(0), 36.958682112d), "炉心快照加进第一段");
    }

    private static void zero() {
        check(FireConsumerPlans.skyDamage(0, 0, 0.0d) == 0.0d, "零消耗伤害是 0");
        check(FireConsumerPlans.skySpec(0, 0.0d).isEmpty(), "零火种不挂载");
        Playback partial = play(9, 0.0d, 0, 0);
        check(partial.pulses.size() == 10 && near(partial.pulses.get(9), 0.0d) && partial.spent == 9,
                "1 到 9 点挂满十段，没有消耗的段没有伤害");
        check(near(partial.pulses.get(0), 3.589159827d), "1 点仍保留基础 3");
    }

    private static void exhaustion() {
        Playback drained = play(100, 0.0d, 0, 0);
        check(drained.remaining == 0 && drained.finished, "余额耗尽且十段结束后轨道不留");
        PumpResult later = drained.ledger.pump(40, 99L);
        check(later.spent() == 0 && later.pulses().isEmpty(), "结束后补进的余额不复活旧层");
    }

    private static void spreadUnchanged() {
        SharedSpec spread = FireConsumerPlans.spreadSpec(100, 0.0d).orElseThrow();
        check(!spread.dynamicDemand() && spread.segmentDemand() == 10 && spread.segmentLimit() == 13
                && near(spread.fullSegmentDamage(), 8.0d), "燎原仍是固定 10 点需求和锁定完整段");
        BurnLedger ledger = new BurnLedger();
        check(ledger.admitShared(FireConsumerPlans.SPREAD_KEY, spread, UUID.randomUUID(), CASTER, TARGET, 100, 0,
                0.0d, 1L), "燎原挂上");
        int fuel = 15;
        PumpResult opening = null;
        for (int tick = 0; tick < 20; tick++) {
            opening = ledger.pump(fuel, tick + 1L);
            fuel = opening.balanceAfter();
        }
        check(opening != null && opening.spent() == 10 && opening.pulses().size() == 1
                && near(opening.pulses().get(0).damage(), 8.0d), "燎原单独结算仍付固定 10 点");
    }

    private static void sharedContest() {
        SharedSpec sky = FireConsumerPlans.skySpec(15, 0.0d).orElseThrow();
        SharedSpec spread = SharedSpec.of(4, 1, 10, 2.0d, 0).orElseThrow();
        BurnLedger ledger = new BurnLedger();
        check(ledger.admitShared(FireConsumerPlans.SKY_KEY, sky, UUID.randomUUID(), CASTER, TARGET, 15, 5, 0.0d, 11L),
                "同拍焚天");
        check(ledger.admitShared(FireConsumerPlans.SPREAD_KEY, spread, UUID.randomUUID(), CASTER, TARGET, 15, 5,
                0.0d, 22L), "同拍固定需求层");
        for (int tick = 0; tick < 3; tick++) {
            PumpResult waiting = ledger.pump(15, tick + 1L);
            check(waiting.spent() == 0 && waiting.pulses().isEmpty(), "未到期不扣");
        }
        PumpResult due = ledger.pump(15, 4L);
        check(due.spent() == 12 && due.pulses().size() == 2, "焚天需求 2、固定需求 10，15 点够付");
        double skyDamage = 0.0d;
        double spreadDamage = 0.0d;
        for (BurnPulse pulse : due.pulses()) {
            if (FireConsumerPlans.SKY_KEY.equals(pulse.effectKey())) {
                skyDamage = pulse.damage();
            }
            if (FireConsumerPlans.SPREAD_KEY.equals(pulse.effectKey())) {
                spreadDamage = pulse.damage();
            }
        }
        check(near(spreadDamage, 2.0d), "固定需求层按实付比例保持原缩放");
        check(near(skyDamage, FireConsumerPlans.skyDamage(15, 2, 0.0d)), "焚天按分账前余额和实付 2 计伤");
        int[] split = FuelShare.divide(15, new int[] {10, 10}, new long[] {11L, 22L}, 4L);
        check(split[0] + split[1] == 15, "两个固定需求 10 的分账规则不变");
    }

    private static void refillAfterEmpty() {
        Playback resumed = play(1, 0.0d, 1, 10);
        check(resumed.pulses.size() == 10 && resumed.spent == 11, "余额先空也不结束，后续补火仍可烧掉");
        check(resumed.demands.get(0) == 1 && resumed.demands.get(1) == 2, "空窗后的下一段按补入余额重新取整");
    }

    private static Playback play(int opening, double furnace, int refillBeforeSegment, int refill) {
        declareDefaultDamage();
        Optional<SharedSpec> specified = FireConsumerPlans.skySpec(opening, furnace);
        check(specified.isPresent(), opening + " 点可以挂焚天");
        BurnLedger ledger = new BurnLedger();
        SharedSpec spec = specified.orElseThrow();
        check(ledger.admitShared(FireConsumerPlans.SKY_KEY, spec, UUID.randomUUID(), CASTER, TARGET, opening,
                opening % 10, furnace, 7L), opening + " 点焚天挂上");
        ArrayList<Double> pulses = new ArrayList<>();
        ArrayList<Integer> demands = new ArrayList<>();
        int fuel = opening;
        int spent = 0;
        int ticks = 0;
        int segments = 0;
        while (ledger.trackCount() > 0 && ticks < 80) {
            if (segments == refillBeforeSegment && refill > 0 && ledger.export().get(0).ticksUntilNext() == 1) {
                fuel = Math.min(100, fuel + refill);
            }
            int before = fuel;
            int remaining = spec.segmentLimit() - segments;
            PumpResult result = ledger.pump(fuel, ticks + 1L);
            if (!result.pulses().isEmpty() || result.spent() > 0) {
                demands.add(FireConsumerPlans.skyDemand(before, remaining));
                segments += 1;
            }
            fuel = result.balanceAfter();
            spent += result.spent();
            for (BurnPulse pulse : result.pulses()) {
                pulses.add(pulse.damage());
            }
            ticks += 1;
        }
        return new Playback(ledger, pulses, demands, sum(pulses), spent, fuel, ticks, ledger.trackCount() == 0);
    }

    /** 游戏封存后倍率默认是 1。纯测试目录未声明时补这一把键，已声明则不覆盖。 */
    private static void declareDefaultDamage() {
        if (BalanceRegistry.isDeclared("fire_fentian", BalanceKeys.DAMAGE_MULTIPLIER)
                && BalanceRegistry.isDeclared("fire_liaoyuan", BalanceKeys.DAMAGE_MULTIPLIER)) {
            return;
        }
        if (!BalanceRegistry.isDeclared("fire_fentian", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_fentian").damageMultiplier().build());
        }
        if (!BalanceRegistry.isDeclared("fire_liaoyuan", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_liaoyuan").damageMultiplier().build());
        }
    }

    private static double sum(List<Double> values) {
        double total = 0.0d;
        for (Double value : values) {
            total += value;
        }
        return total;
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) <= TOLERANCE;
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

    private static final class Playback {
        private final BurnLedger ledger;
        private final List<Double> pulses;
        private final List<Integer> demands;
        private final double total;
        private final int spent;
        private final int remaining;
        private final int ticks;
        private final boolean finished;

        private Playback(BurnLedger ledger, List<Double> pulses, List<Integer> demands, double total, int spent,
                         int remaining, int ticks, boolean finished) {
            this.ledger = ledger;
            this.pulses = pulses;
            this.demands = demands;
            this.total = total;
            this.spent = spent;
            this.remaining = remaining;
            this.ticks = ticks;
            this.finished = finished;
        }
    }
}
