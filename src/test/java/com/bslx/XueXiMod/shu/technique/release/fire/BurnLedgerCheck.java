package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 燃烧状态机和三式检查输入。配方只出现在这里，运行模块只接收分量和间隔。
 * 不启动游戏，也不读写实体。
 */
public final class BurnLedgerCheck {
    private static final double TOLERANCE = 1.0e-4d;
    private static final UUID CAST = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CASTER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_CASTER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TARGET = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static int assertionCount;
    private static int failures;

    private BurnLedgerCheck() {
    }

    public static void main(String[] arguments) {
        declareDefaultDamage();
        section("预检", BurnLedgerCheck::rejects);
        section("冲突", BurnLedgerCheck::conflicts);
        section("节奏", BurnLedgerCheck::timing);
        section("快照", BurnLedgerCheck::snapshot);
        section("恢复", BurnLedgerCheck::restore);
        section("燎原", BurnLedgerCheck::meadow);
        section("燎里", BurnLedgerCheck::innerThrust);
        section("焚天", BurnLedgerCheck::calamity);
        section("旧档案输入", BurnLedgerCheck::archiveInput);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：实体 NBT 往返、死亡事件、卸载暂停的真实时钟、hurt 免疫。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void rejects() {
        check(!BurnSchedule.validKey(null), "空键拒绝");
        check(!BurnSchedule.validKey(""), "空字符串拒绝");
        check(!BurnSchedule.validKey("Scorch"), "大写拒绝");
        check(!BurnSchedule.validKey("a/b"), "斜杠拒绝");
        check(BurnSchedule.validKey("a".repeat(FireLimits.KEY_CAP)), "48 字键接受");
        check(!BurnSchedule.validKey("a".repeat(FireLimits.KEY_CAP + 1)), "49 字键拒绝");
        check(SegmentPlan.of(0, List.of(component(2.0d, 0.0d))).isEmpty(), "间隔 0 拒绝");
        check(SegmentPlan.of(FireLimits.INTERVAL_CAP + 1, List.of(component(2.0d, 0.0d))).isEmpty(), "超长间隔拒绝");
        check(SegmentPlan.of(4, List.of()).isEmpty(), "空段拒绝");
        ArrayList<BurnComponent> tooMany = new ArrayList<>();
        for (int cursor = 0; cursor < FireLimits.SEGMENT_CAP + 1; cursor++) {
            tooMany.add(component(2.0d, 0.0d));
        }
        check(SegmentPlan.of(4, tooMany).isEmpty(), "超过段数上限拒绝");
        BurnSchedule huge = BurnSchedule.of("huge_burn", false, 4, List.of(component(2.0d, 100000.0d))).orElseThrow();
        BurnLedger ledger = new BurnLedger();
        check(!BurnLedger.canSeal(huge, 0.0d), "封存伤害超过上限时拒绝");
        check(!ledger.admit(huge, identity(10, 0.0d)), "超限计划不挂载");
        check(ledger.trackCount() == 0, "拒绝挂载没有留下记录");
        check(!ledger.admit(plain("plain_burn", false), identity(19, -1.0d)), "负炉心拒绝");
        check(ledger.trackCount() == 0, "负炉心没有副作用");
    }

    private static void conflicts() {
        BurnLedger ledger = new BurnLedger();
        check(ledger.admit(plain("shared_burn", false), identity(10, 0.0d)), "第一次同键挂载");
        check(!ledger.duplicate(CAST, TARGET, "other_burn"), "不同效果不重复");
        check(ledger.duplicate(CAST, TARGET, "shared_burn"), "同一次施法同一效果不能再挂");
        check(!ledger.admit(plain("shared_burn", false), identity(10, 0.0d)), "重复施法被拒绝");
        check(ledger.admit(plain("shared_burn", false), identity(10, 0.0d, CASTER, UUID.randomUUID())),
                "另一次释放可以叠同一效果");
        check(ledger.admit(plain("future_calamity", true), identity(10, 0.0d)), "同一施法的另一效果可以并存");
        check(ledger.admit(plain("shared_burn", false), identity(10, 0.0d, OTHER_CASTER, UUID.randomUUID())),
                "另一施法者的新释放也可以挂");
        BurnLedger capped = new BurnLedger();
        for (int cursor = 0; cursor < FireLimits.PLAN_CAP; cursor++) {
            check(capped.admit(plain("slot_" + cursor, false), identity(10, 0.0d, CASTER, UUID.randomUUID())),
                    "第 " + (cursor + 1) + " 条计划");
        }
        check(!capped.admit(plain("slot_extra", false), identity(10, 0.0d)), "第 9 条计划拒绝");
        check(capped.trackCount() == FireLimits.PLAN_CAP, "上限之后条数不变");
    }

    private static void timing() {
        BurnLedger ledger = new BurnLedger();
        check(ledger.admit(BurnSchedule.of("timed_burn", false, 4, List.of(
                component(2.0d, 0.0d), component(2.0d, 0.0d), component(2.0d, 0.0d))).orElseThrow(),
                identity(30, 0.0d)), "三秒级间隔挂载");
        check(ledger.export().get(0).ticksUntilNext() == 4, "挂载当拍不结算，等待完整间隔");
        check(step(ledger).isEmpty(), "第 1 拍只倒计时");
        check(step(ledger).isEmpty(), "第 2 拍只倒计时");
        check(step(ledger).isEmpty(), "第 3 拍只倒计时");
        List<BurnPulse> first = step(ledger);
        check(first.size() == 1 && first.get(0).ordinal() == 0, "第 4 拍打出第一段");
        check(ledger.trackCount() == 1, "还有后续段");
        check(step(ledger).isEmpty(), "下一段重新等待");
        ledger.clear();
        check(ledger.trackCount() == 0 && ledger.advance().isEmpty(), "目标死亡后记录清空");
    }

    private static void snapshot() {
        BurnLedger ledger = new BurnLedger();
        BurnSchedule schedule = BurnSchedule.of("frozen_burn", false, 4, List.of(component(2.0d, 1.0d))).orElseThrow();
        check(ledger.admit(schedule, identity(100, 0.3d)), "带炉心挂载");
        double sealed = ledger.export().get(0).damages().get(0);
        check(near(sealed, FireLayer.combine(2.0d, 1.0d, 0.3d)), "段伤害在挂载时封存");
        check(ledger.admit(plain("later_burn", false), identity(20, 0.6d)), "新的火种走另一条计划");
        check(near(ledger.export().get(0).damages().get(0), sealed), "新火种不改已经封存的段");
        check(ledger.export().get(0).furnaceBonus() == 0.3d, "原快照里的炉心还是 0.3");
        check(ledger.export().get(0).amount() == 100, "原快照点数不变");
    }

    private static void restore() {
        ArrayList<Double> damages = new ArrayList<>();
        for (int cursor = 0; cursor < 5; cursor++) {
            damages.add(2.0d);
        }
        BurnTrackState due = state("resume_burn", 4, 0, 0, damages);
        BurnLedger resumed = BurnLedger.restore(List.of(due));
        List<BurnPulse> one = resumed.advance();
        check(one.size() == 1 && near(one.get(0).damage(), 2.0d), "到期只打一段");
        check(resumed.trackCount() == 1 && resumed.export().get(0).segmentCursor() == 1, "失败与否都已消耗这一段");
        check(resumed.export().get(0).ticksUntilNext() == 4, "下一段重新等待，不连打剩余四段");
        BurnTrackState stalled = state("resume_burn", 4, 99999, 0, damages);
        BurnLedger clamped = BurnLedger.restore(List.of(stalled));
        check(clamped.export().get(0).ticksUntilNext() == 4, "过大的等待钳回一个间隔");
        check(clamped.advance().isEmpty(), "恢复本身不补打");
        BurnTrackState broken = state("resume_burn", 4, 1, 0, List.of(Double.NaN));
        BurnLedger dropped = BurnLedger.restore(List.of(broken));
        check(dropped.trackCount() == 0 && dropped.droppedOnRestore() == 1, "非有限段伤害整条丢弃");
        ArrayList<BurnTrackState> many = new ArrayList<>();
        for (int cursor = 0; cursor < FireLimits.PLAN_CAP + 1; cursor++) {
            many.add(state("slot_" + cursor, 4, 4, 0, List.of(2.0d)));
        }
        BurnLedger truncated = BurnLedger.restore(many);
        check(truncated.trackCount() == FireLimits.PLAN_CAP, "恢复最多保留上限条数");
        check(truncated.droppedOnRestore() == 1, "超出的存档条数记为丢弃");
        BurnLedger paused = new BurnLedger();
        paused.admit(plain("paused_burn", false), identity(20, 0.0d));
        paused.pump(100, 1L);
        paused.pump(100, 2L);
        BurnLedger loaded = BurnLedger.restore(paused.export());
        check(loaded.export().get(0).ticksUntilNext() == 2, "加载保留剩余等待");
        check(loaded.pump(100, 3L).pulses().isEmpty(), "加载后的第一拍不把剩余段补齐");
    }

    private static void meadow() {
        SegmentPlan plan = meadowPlan(10);
        BurnSchedule schedule = BurnSchedule.of("future_field", false, plan.intervalTicks(), plan.components()).orElseThrow();
        BurnLedger ledger = new BurnLedger();
        check(ledger.admit(schedule, identity(100, 0.0d)), "满档燎原可以挂载");
        List<Double> damages = ledger.export().get(0).damages();
        check(damages.size() == 13, "100 点是 3+10 段");
        check(ledger.export().get(0).ticksUntilNext() == 20, "间隔 20");
        double total = 0.0d;
        for (Double damage : damages) {
            check(near(damage, 4.0d), "不含炉心时每段 4");
            total += damage;
        }
        check(near(total, 52.0d), "锁定清单仍列出 13 段合计 52");
        PumpResult opening = ledger.pump(100, 1L);
        check(opening.pulses().isEmpty() && ledger.export().get(0).ticksUntilNext() == 19, "挂载后第一拍不打");
        int paid = 0;
        int fuel = opening.balanceAfter();
        int guard = 1;
        while (ledger.trackCount() > 0 && guard < 400) {
            PumpResult result = ledger.pump(fuel, guard + 1L);
            fuel = result.balanceAfter();
            paid += result.pulses().size();
            guard += 1;
        }
        check(paid == 10 && fuel == 0, "100 点火种只够付 10 段");
    }

    private static void innerThrust() {
        check(near(FireLayer.combine(0.36d * 100.0d, 0.0d, 0.0d), 36.0d), "燎里资源火伤是 0.36S");
        check(near(FireLayer.combine(0.36d * 100.0d, 0.0d, 0.5d), 54.0d), "炉心加在同一层");
        check(BurnSchedule.of("future_thrust", false, 4, List.of(component(0.36d * 19.0d, 0.0d))).isPresent(),
                "燎里只需要一段资源火伤");
    }

    private static void calamity() {
        check(FireConsumerPlans.skySpec(0, 0.0d).isEmpty(), "0 点没有焚天燃烧");
        expectFuelledSky(10, 10, 35.5350d);
        expectFuelledSky(50, 10, 72.7045d);
        expectFuelledSky(100, 10, 185.7521d);
        SharedSpec nine = FireConsumerPlans.skySpec(9, 0.0d).orElseThrow();
        check(nine.segmentLimit() == 10 && nine.dynamicDemand(), "1到9点仍挂10段并按当时余额取整");
        List<Double> partialPulses = playSky(9);
        check(partialPulses.size() == 10 && near(partialPulses.get(0), 3.589159827d)
                && near(partialPulses.get(8), 3.509199688d) && near(partialPulses.get(9), 0.0d),
                "9点前九段各消耗1点，基础3不缩水，末段零伤害");
        List<Double> mixedPulses = playSky(19);
        check(mixedPulses.size() == 10 && near(mixedPulses.get(0), 4.413983584d)
                && near(mixedPulses.get(9), 3.509199688d), "19点十段全部烧完，末段消耗剩余 1 点");
    }

    private static void archiveInput() {
        int tiers = 4;
        int sustain = 10 * tiers;
        double burst = 0.04d * tiers;
        SegmentPlan plan = SegmentPlan.of(sustain, List.of(component(2.0d, burst))).orElseThrow();
        BurnLedger ledger = new BurnLedger();
        check(ledger.admit(BurnSchedule.of("archive_burn", false, plan.intervalTicks(), plan.components()).orElseThrow(),
                identity(40, 0.0d)), "旧档案数字可以作为计划输入");
        check(ledger.export().get(0).ticksUntilNext() == 40, "持续档加成的间隔由输入决定");
        check(near(ledger.export().get(0).damages().get(0), FireLayer.combine(2.0d, burst, 0.0d)),
                "爆发增量走同一层公式");
    }

    private static void expectFuelledSky(int amount, int paidSegments, double publishedTotal) {
        declareDefaultDamage();
        SharedSpec spec = FireConsumerPlans.skySpec(amount, 0.0d).orElseThrow();
        check(spec.segmentLimit() == FireConsumerPlans.SKY_SEGMENT_LIMIT, amount + " 点上限仍是10段");
        List<Double> pulses = playSky(amount);
        double total = 0.0d;
        for (Double damage : pulses) {
            total += damage;
        }
        check(pulses.size() == paidSegments, amount + " 点实付段数");
        check(near(total, publishedTotal), amount + " 点合计接近公布值");
    }

    private static List<Double> playSky(int amount) {
        declareDefaultDamage();
        SharedSpec spec = FireConsumerPlans.skySpec(amount, 0.0d).orElseThrow();
        BurnLedger ledger = new BurnLedger();
        check(ledger.admitShared("future_calamity", spec, UUID.randomUUID(), CASTER, TARGET, amount, amount % 10,
                0.0d, 0L), amount + " 点焚天挂载");
        ArrayList<Double> damages = new ArrayList<>();
        int fuel = amount;
        int guard = 0;
        int limit = spec.segmentLimit() * spec.intervalTicks() + 2;
        while (ledger.trackCount() > 0 && guard < limit) {
            PumpResult result = ledger.pump(fuel, guard + 1L);
            fuel = result.balanceAfter();
            for (BurnPulse pulse : result.pulses()) {
                damages.add(pulse.damage());
            }
            guard += 1;
        }
        check(ledger.trackCount() == 0, amount + " 点燃料耗尽后结束");
        return damages;
    }

    private static void declareDefaultDamage() {
        if (!BalanceRegistry.isDeclared("fire_fentian", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_fentian").damageMultiplier().build());
        }
        if (!BalanceRegistry.isDeclared("fire_liaoyuan", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_liaoyuan").damageMultiplier().build());
        }
    }

    private static List<BurnPulse> step(BurnLedger ledger) {
        return ledger.pump(100, 1L).pulses();
    }

    private static SegmentPlan meadowPlan(int tiers) {
        ArrayList<BurnComponent> components = new ArrayList<>();
        int count = 3 + tiers;
        for (int cursor = 0; cursor < count; cursor++) {
            components.add(component(2.0d, 0.1d * tiers));
        }
        return SegmentPlan.of(20, components).orElseThrow();
    }

    private static BurnComponent component(double base, double resourceAboveOne) {
        return BurnComponent.of(base, resourceAboveOne).orElseThrow();
    }

    private static BurnSchedule plain(String effectKey, boolean targetExclusive) {
        return BurnSchedule.of(effectKey, targetExclusive, 4, List.of(component(2.0d, 0.0d))).orElseThrow();
    }

    private static BurnIdentity identity(int amount, double furnaceBonus) {
        return identity(amount, furnaceBonus, CASTER, CAST);
    }

    private static BurnIdentity identity(int amount, double furnaceBonus, UUID casterId, UUID castId) {
        int tiers = amount / 10;
        int remainder = amount - tiers * 10;
        return new BurnIdentity(castId, casterId, TARGET, amount, tiers, remainder, furnaceBonus);
    }

    private static BurnTrackState state(String effectKey, int intervalTicks, int ticksUntilNext, int segmentCursor,
                                        List<Double> damages) {
        return new BurnTrackState(effectKey, false, CAST, CASTER, TARGET, 50, 5, 0, 0.0d, intervalTicks,
                ticksUntilNext, segmentCursor, damages);
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) <= TOLERANCE;
    }

    private static void section(String title, Runnable body) {
        int before = failures;
        body.run();
        System.out.println(title + " 失败 " + (failures - before));
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }
}
