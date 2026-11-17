package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinitions;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnComponent;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnIdentity;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnLedger;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnPulse;
import com.bslx.XueXiMod.shu.technique.release.fire.BurnSchedule;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLayer;
import com.bslx.XueXiMod.shu.technique.release.fire.PumpResult;
import com.bslx.XueXiMod.shu.technique.release.fire.SegmentPlan;
import com.bslx.XueXiMod.shu.technique.release.fire.SharedSpec;
import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;
import com.bslx.XueXiMod.shu.technique.release.selfstatus.SelfStatusCast;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceMath;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 调用现行注册、火种账本、三消费者计划和燃烧账。不构造玩家，也不启动游戏。
 */
public final class SixFireFunctionalCheck {
    private static final double TOLERANCE = 1.0e-6d;
    private static final UUID CASTER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID OTHER_CASTER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static int failures;

    private SixFireFunctionalCheck() {
    }

    public static void main(String[] programArguments) {
        declareDefaultDamage();
        checkRegistration();
        checkZeroSeed();
        checkPartialSeed();
        checkExactTier();
        checkTailAndCap();
        checkFurnaceOnce();
        checkSharedContext();
        checkExclusiveTracks();
        checkFurnaceTransitions();
        if (failures != 0) {
            System.err.println("six fire functional failures=" + failures);
            System.exit(1);
        }
        System.out.println("six fire functional check passed");
        System.out.println("未执行：玩家事务、实体命中、地火 tick、混链函数、一分为三复制份。");
    }

    private static void checkRegistration() {
        ReleaseDefinitions.registerAll();
        ReleaseDefinition spread = ReleaseRegistry.get("fire_liaoyuan");
        ReleaseDefinition thrust = ReleaseRegistry.get("fire_liaoli");
        ReleaseDefinition sky = ReleaseRegistry.get("fire_fentian");
        ReleaseDefinition heat = ReleaseRegistry.get("fire_heat_up");
        ReleaseDefinition burn = ReleaseRegistry.get("fire_burn");
        ReleaseDefinition cool = ReleaseRegistry.get("fire_cool");
        ReleaseDefinition dual = ReleaseRegistry.get("rule_dual_release");
        ReleaseDefinition triple = ReleaseRegistry.get("rule_triple_cast");
        expect(spread != null && spread.kind() == ReleaseKind.HIT_VOLUME && spread.role() == ChainRole.ATTACK
                && spread.lifetimeTicks() == 200 && spread.explicitManaCost() == 60
                && !spread.refusesMixedChain() && !spread.requiresHeldSword(), "燎原注册");
        expect(thrust != null && thrust.tag() == ReleaseTag.SUSTAINED && thrust.trajectory() == Trajectory.FOLLOW_LOOK
                && thrust.lifetimeTicks() == 40 && thrust.explicitManaCost() == 40
                && thrust.requiresHeldSword() && thrust.thickness() == 0.6f, "燎里注册");
        expect(sky != null && sky.lifetimeTicks() == 40 && sky.explicitManaCost() == 100
                && sky.role() == ChainRole.ATTACK, "焚天注册");
        expect(heat instanceof SelfStatusCast heatCast && heat.kind() == ReleaseKind.SELF_STATUS
                && heat.role() == ChainRole.ACTION && heat.refusesMixedChain()
                && heat.explicitManaCost() == 20 && heat.explicitAssemblyCost() == 1
                && heatCast.action() == FurnaceMath.Action.ENTER_NORMAL, "升温注册");
        expect(burn instanceof SelfStatusCast burnCast && burn.refusesMixedChain()
                && burn.explicitManaCost() == 20 && burnCast.action() == FurnaceMath.Action.ENTER_OVERDRIVE,
                "燃烧注册");
        expect(cool instanceof SelfStatusCast coolCast && cool.refusesMixedChain()
                && cool.explicitManaCost() == 0 && coolCast.action() == FurnaceMath.Action.COOL, "冷却注册");
        expect(dual != null && dual.role() == ChainRole.RULE_DUAL, "双重释放角色");
        expect(triple != null && triple.role() == ChainRole.RULE_TRIPLE, "一分为三角色");
    }

    private static void checkZeroSeed() {
        FireSeedAccount account = new FireSeedAccount(0);
        FireCastContext cast = freshCast(CASTER);
        expect(account.consume(cast, target("zero"), FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty(), "零余额不消费");
        expect(account.balance() == 0, "零余额保持");
        expect(FireConsumerPlans.spread(0, 0, 0).isEmpty(), "燎原零火种无附着计划");
        expect(FireConsumerPlans.thrust(0, 0, 0).isEmpty(), "燎里零火种无引爆分量");
        expect(FireConsumerPlans.sky(0, 0, 0).isEmpty(), "焚天零火种无计划");
    }

    private static void checkPartialSeed() {
        FireSeedSnapshot taken = takeAll(9, 0.0d, "partial");
        expect(taken != null && taken.amount() == 9 && taken.tiers() == 0 && taken.remainder() == 9, "1到9全扣");
        Optional<SegmentPlan> spread = FireConsumerPlans.spread(taken.amount(), taken.tiers(), taken.remainder());
        expect(spread.isPresent() && spread.get().components().size() == 3
                && spread.get().intervalTicks() == FireConsumerPlans.SPREAD_INTERVAL, "少于10点三段");
        if (spread.isPresent()) {
            double damage = sealedDamage(spread.get(), 0.0d);
            expect(close(damage, FireLayer.combine(2.0d, 0.0d, 0.0d)), "少于10点单段仍是基础2");
            List<Double> spreadPulses = play(FireConsumerPlans.SPREAD_KEY, false, spread.get(), taken);
            expect(spreadPulses.size() == 1 && close(spreadPulses.get(0), 1.8d), "9点只付一段并缩放到1.8");
        }
        Optional<SegmentPlan> sky = FireConsumerPlans.sky(taken.amount(), taken.tiers(), taken.remainder());
        expect(sky.isPresent() && sky.get().components().size() == 10
                && sky.get().intervalTicks() == FireConsumerPlans.SKY_INTERVAL, "旧展示清单仍列10段");
        SharedSpec runtime = FireConsumerPlans.skySpec(9, 0.0d).orElseThrow();
        expect(runtime.dynamicDemand() && runtime.segmentLimit() == 10, "1到9点运行时仍挂10段");
        Optional<BurnComponent> thrust = FireConsumerPlans.thrust(taken.amount(), taken.tiers(), taken.remainder());
        expect(thrust.isPresent() && close(FireLayer.combine(thrust.get().base(), thrust.get().resourceAboveOne(), 0.0d),
                0.36d * 9.0d), "少于10点燎里按全部点数线性引爆");
    }

    private static void checkExactTier() {
        FireSeedSnapshot taken = takeAll(10, 0.0d, "exact");
        expect(taken != null && taken.tiers() == 1 && taken.remainder() == 0, "整十档");
        Optional<SegmentPlan> spread = FireConsumerPlans.spread(taken.amount(), taken.tiers(), taken.remainder());
        expect(spread.isPresent() && spread.get().components().size() == 4, "整十燎原四段");
        Optional<SegmentPlan> sky = FireConsumerPlans.sky(taken.amount(), taken.tiers(), taken.remainder());
        expect(sky.isPresent() && sky.get().components().size() == 10, "整十焚天上限仍是10段");
        if (sky.isPresent()) {
            BurnComponent full = sky.get().components().get(0);
            expect(close(FireLayer.combine(full.base(), full.resourceAboveOne(), 0.0d), 2.4d), "旧展示清单整十仍是2.4");
        }
        expect(close(FireConsumerPlans.skyDamage(10, 1, 0.0d), 3.6d), "运行时10点首段是基础3加1点");
    }

    private static void checkTailAndCap() {
        FireSeedSnapshot fiftyFour = takeAll(54, 0.0d, "tail");
        expect(fiftyFour != null && fiftyFour.tiers() == 5 && fiftyFour.remainder() == 4, "尾段拆分");
        Optional<SegmentPlan> sky = FireConsumerPlans.sky(fiftyFour.amount(), fiftyFour.tiers(), fiftyFour.remainder());
        expect(sky.isPresent() && sky.get().components().size() == 10, "旧展示清单上限10段");
        expect(FireConsumerPlans.skyDemand(54, 10) == 6, "54点运行时首段向上取整为6");
        FireSeedAccount capped = new FireSeedAccount(150);
        expect(capped.balance() == 100, "余额封顶100");
        FireSeedSnapshot full = takeAll(capped.balance(), 0.0d, "cap");
        expect(full != null && full.amount() == 100 && full.tiers() == 10 && full.remainder() == 0, "封顶一次全扣");
        Optional<SegmentPlan> spread = FireConsumerPlans.spread(full.amount(), full.tiers(), full.remainder());
        Optional<SegmentPlan> skyFull = FireConsumerPlans.sky(full.amount(), full.tiers(), full.remainder());
        expect(spread.isPresent() && spread.get().components().size() == 13, "满火种燎原13段");
        expect(skyFull.isPresent() && skyFull.get().components().size() == 10, "满火种焚天10段");
        if (spread.isPresent()) {
            List<Double> pulses = play(FireConsumerPlans.SPREAD_KEY, false, spread.get(), full);
            expect(pulses.size() == 10 && close(pulses.get(0), 4.0d), "满火种燎原燃料付10段");
        }
    }

    private static void checkFurnaceOnce() {
        double furnaceBonus = 0.30d;
        FireSeedSnapshot taken = takeAll(100, furnaceBonus, "furnace");
        expect(taken != null && close(taken.furnaceBonus(), furnaceBonus) && taken.amount() == 100, "炉心只记在快照");
        Optional<SegmentPlan> spread = FireConsumerPlans.spread(taken.amount(), taken.tiers(), taken.remainder());
        expect(spread.isPresent(), "炉心不挡住燎原计划");
        if (spread.isPresent()) {
            BurnComponent component = spread.get().components().get(0);
            double once = FireLayer.combine(component.base(), component.resourceAboveOne(), furnaceBonus);
            double twice = FireLayer.combine(component.base(), component.resourceAboveOne() + furnaceBonus, furnaceBonus);
            List<Double> pulses = play(FireConsumerPlans.SPREAD_KEY, false, spread.get(), taken);
            expect(close(pulses.get(0), once) && !close(once, twice), "燎原炉心只合入一次");
            expect(close(once, 4.6d), "满火种加0.30炉心为4.6");
        }
        Optional<BurnComponent> thrust = FireConsumerPlans.thrust(100, 10, 0);
        expect(thrust.isPresent(), "燎里引爆分量");
        if (thrust.isPresent()) {
            double fireOnly = FireLayer.combine(thrust.get().base(), thrust.get().resourceAboveOne(), furnaceBonus);
            double doubled = FireLayer.combine(thrust.get().base(), thrust.get().resourceAboveOne() + furnaceBonus,
                    furnaceBonus);
            expect(close(fireOnly, 46.8d) && !close(fireOnly, doubled), "燎里火伤只加一次炉心");
            expect(close(thrust.get().base(), 36.0d), "燎里基础引爆不含物理刺击");
        }
        Optional<SegmentPlan> sky = FireConsumerPlans.sky(100, 10, 0);
        expect(sky.isPresent(), "旧展示清单仍可生成");
        if (sky.isPresent()) {
            BurnComponent component = sky.get().components().get(0);
            double once = FireLayer.combine(component.base(), component.resourceAboveOne(), furnaceBonus);
            double plain = FireLayer.combine(component.base(), component.resourceAboveOne(), 0.0d);
            expect(close(once, plain + 2.0d * furnaceBonus), "旧展示清单仍把炉心加在同一层");
        }
        expect(close(FireConsumerPlans.skyDamage(100, 10, furnaceBonus),
                FireConsumerPlans.skyDamage(100, 10, 0.0d) + 1.5d), "运行时炉心按实际消耗加算一次");
    }

    private static void checkSharedContext() {
        UUID targetId = target("shared");
        FireSeedAccount account = new FireSeedAccount(40);
        FireCastContext firstCast = freshCast(CASTER);
        FireCastContext secondCast = freshCast(OTHER_CASTER);
        Optional<FireSeedSnapshot> first = account.consume(firstCast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(first.isPresent() && first.get().amount() == 40 && account.balance() == 0, "先到者扣完全部");
        expect(account.consume(secondCast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty(), "后到者读到剩余零");
        expect(account.consume(firstCast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty(), "同一上下文不重复扣");
        FireCastContext producer = freshCast(CASTER);
        int gained = account.grant(producer, targetId, 15, 1.0d);
        expect(gained == 15 && account.balance() == 15, "新上下文可以记入后续火种");
        expect(account.grant(producer, targetId, 15, 1.0d) == 0, "同一上下文不重复生产");
        expect(account.consume(firstCast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d).isEmpty()
                && account.balance() == 15, "已消费上下文不拿走新火种");
        FireCastContext laterCast = freshCast(CASTER);
        Optional<FireSeedSnapshot> later = account.consume(laterCast, targetId, FireConsumerPlans.ALL_SEEDS, 0.0d);
        expect(later.isPresent() && later.get().amount() == 15 && account.balance() == 0, "下一次上下文消费新火种");
        FireSeedAccount full = new FireSeedAccount(100);
        FireCastContext filling = freshCast(CASTER);
        expect(full.grant(filling, targetId, 20, 2.2d) == 0 && full.balance() == 100, "封顶后增加量为零");
    }

    private static void checkExclusiveTracks() {
        FireSeedSnapshot taken = takeAll(20, 0.0d, "exclusive");
        Optional<SegmentPlan> spread = FireConsumerPlans.spread(taken.amount(), taken.tiers(), taken.remainder());
        Optional<SegmentPlan> sky = FireConsumerPlans.sky(taken.amount(), taken.tiers(), taken.remainder());
        expect(spread.isPresent() && sky.isPresent(), "两份计划都能生成");
        if (spread.isEmpty() || sky.isEmpty()) {
            return;
        }
        SharedSpec spreadSpec = FireConsumerPlans.spreadSpec(20, 0.0d).orElseThrow();
        SharedSpec skySpec = FireConsumerPlans.skySpec(20, 0.0d).orElseThrow();
        UUID firstCast = UUID.randomUUID();
        UUID targetId = target("stack");
        BurnLedger ledger = new BurnLedger();
        expect(ledger.admitShared(FireConsumerPlans.SPREAD_KEY, spreadSpec, firstCast, CASTER, targetId, 20, 0, 0.0d,
                1L), "第一次燎原挂上");
        expect(!ledger.admitShared(FireConsumerPlans.SPREAD_KEY, spreadSpec, firstCast, CASTER, targetId, 20, 0, 0.0d,
                2L), "同一次施法不能再挂燎原");
        expect(ledger.admitShared(FireConsumerPlans.SPREAD_KEY, spreadSpec, UUID.randomUUID(), CASTER, targetId, 20, 0,
                0.0d, 3L), "另一次释放可以叠燎原");
        expect(ledger.admitShared(FireConsumerPlans.SKY_KEY, skySpec, firstCast, CASTER, targetId, 20, 0, 0.0d, 4L),
                "同一次施法的另一效果可以并存");
        expect(ledger.admitShared(FireConsumerPlans.SKY_KEY, skySpec, UUID.randomUUID(), OTHER_CASTER, targetId, 20, 0,
                0.0d, 5L), "焚天不再按目标排他");
    }

    private static void checkFurnaceTransitions() {
        FurnaceMath.Transition entered = FurnaceMath.transition(null, 0.2d, 4, FurnaceMath.Action.ENTER_NORMAL);
        expect(entered.outcome() == FurnaceMath.Outcome.ENTERED_NORMAL && entered.tier() == FurnaceMath.Tier.NORMAL
                && entered.bonus() == 0.0d, "无炉心进入普通并从零热量开始");
        FurnaceMath.Transition repeat = FurnaceMath.transition(FurnaceMath.Tier.NORMAL, 0.30d, 9,
                FurnaceMath.Action.ENTER_NORMAL);
        expect(repeat.outcome() == FurnaceMath.Outcome.REJECT_ALREADY_NORMAL && close(repeat.bonus(), 0.30d),
                "已有普通不刷新热量");
        FurnaceMath.Transition direct = FurnaceMath.transition(null, 0.2d, 4, FurnaceMath.Action.ENTER_OVERDRIVE);
        expect(direct.outcome() == FurnaceMath.Outcome.ENTERED_OVERDRIVE && direct.bonus() == 0.0d, "无炉心直接过载");
        FurnaceMath.Transition upgraded = FurnaceMath.transition(FurnaceMath.Tier.NORMAL, 0.30d, 9,
                FurnaceMath.Action.ENTER_OVERDRIVE);
        expect(upgraded.outcome() == FurnaceMath.Outcome.UPGRADED && upgraded.tier() == FurnaceMath.Tier.OVERDRIVE
                && close(upgraded.bonus(), 0.30d), "升级保留热量");
        FurnaceMath.Transition blocked = FurnaceMath.transition(FurnaceMath.Tier.OVERDRIVE, 0.30d, 9,
                FurnaceMath.Action.ENTER_OVERDRIVE);
        expect(blocked.outcome() == FurnaceMath.Outcome.REJECT_ALREADY_OVERDRIVE && close(blocked.bonus(), 0.30d),
                "已过载失败且热量不变");
        FurnaceMath.Transition downgrade = FurnaceMath.transition(FurnaceMath.Tier.OVERDRIVE, 0.30d, 9,
                FurnaceMath.Action.ENTER_NORMAL);
        expect(downgrade.outcome() == FurnaceMath.Outcome.REJECT_CANNOT_DOWNGRADE, "过载不能降成普通");
        FurnaceMath.Transition cooled = FurnaceMath.transition(FurnaceMath.Tier.NORMAL, 0.30d, 9, FurnaceMath.Action.COOL);
        expect(cooled.outcome() == FurnaceMath.Outcome.COOLED && cooled.tier() == null && cooled.bonus() == 0.0d,
                "主动冷却清掉档位和热量");
        FurnaceMath.Transition idle = FurnaceMath.transition(null, 0.0d, 0, FurnaceMath.Action.COOL);
        expect(idle.outcome() == FurnaceMath.Outcome.REJECT_INACTIVE, "没有炉心时冷却失败");
        double heatBonus = 0.0d;
        int climbIndex = 0;
        while (climbIndex < FurnaceMath.NORMAL_TICKS_TO_CEILING) {
            heatBonus = FurnaceMath.advanceOnline(heatBonus, FurnaceMath.Tier.NORMAL, 1L);
            climbIndex += 1;
        }
        expect(close(heatBonus, FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL)), "普通150拍封顶0.30");
        double skipped = FurnaceMath.advanceOnline(0.0d, FurnaceMath.Tier.NORMAL, FurnaceMath.NORMAL_TICKS_TO_CEILING);
        expect(close(skipped, FurnaceMath.rate(FurnaceMath.Tier.NORMAL)), "一次调用不把离线间隔乘进热量");
        double upgradedHeat = FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL);
        int upgradeIndex = 0;
        while (upgradeIndex < FurnaceMath.UPGRADE_TICKS_TO_CEILING) {
            upgradedHeat = FurnaceMath.advanceOnline(upgradedHeat, FurnaceMath.Tier.OVERDRIVE, 1L);
            upgradeIndex += 1;
        }
        expect(close(upgradedHeat, FurnaceMath.ceiling(FurnaceMath.Tier.OVERDRIVE)), "满普通再过载100拍到0.60");
        expect(close(FurnaceMath.seedMultiplier(FurnaceMath.Tier.NORMAL, 0.0d), 1.12d), "普通零热量产率1.12");
        expect(close(FurnaceMath.seedMultiplier(FurnaceMath.Tier.NORMAL, 0.30d), 1.60d), "普通满热产率1.60");
        expect(close(FurnaceMath.seedMultiplier(FurnaceMath.Tier.OVERDRIVE, 0.10d), 2.2d), "过载产率固定2.2");
        expect(!FurnaceMath.allowsServerWork(false, true) && !FurnaceMath.allowsServerWork(true, false)
                && FurnaceMath.allowsServerWork(true, true), "炉心工作要求服务端且同线程");
    }

    private static FireSeedSnapshot takeAll(int stored, double furnaceBonus, String label) {
        FireSeedAccount account = new FireSeedAccount(stored);
        Optional<FireSeedSnapshot> taken = account.consume(freshCast(CASTER), target(label),
                FireConsumerPlans.ALL_SEEDS, furnaceBonus);
        expect(taken.isPresent() && account.balance() == stored - taken.get().amount(), label + "扣后余额");
        return taken.orElse(null);
    }

    private static double sealedDamage(SegmentPlan plan, double furnaceBonus) {
        BurnComponent component = plan.components().get(0);
        return FireLayer.combine(component.base(), component.resourceAboveOne(), furnaceBonus);
    }

    private static void declareDefaultDamage() {
        if (!BalanceRegistry.isDeclared("fire_fentian", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_fentian").damageMultiplier().build());
        }
        if (!BalanceRegistry.isDeclared("fire_liaoyuan", BalanceKeys.DAMAGE_MULTIPLIER)) {
            BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder("fire_liaoyuan").damageMultiplier().build());
        }
    }

    private static List<Double> play(String effectKey, boolean targetExclusive, SegmentPlan plan, FireSeedSnapshot taken) {
        BurnLedger ledger = new BurnLedger();
        boolean admitted = ledger.admit(schedule(effectKey, targetExclusive, plan), identity(taken, CASTER));
        expect(admitted, effectKey + "挂上燃烧账");
        ArrayList<Double> damages = new ArrayList<>();
        int fuel = taken.amount();
        int steps = 0;
        int limit = plan.components().size() * plan.intervalTicks() + 2;
        while (ledger.trackCount() > 0 && steps < limit) {
            PumpResult result = ledger.pump(fuel, steps + 1L);
            fuel = result.balanceAfter();
            for (BurnPulse pulse : result.pulses()) {
                damages.add(pulse.damage());
            }
            steps += 1;
        }
        expect(ledger.trackCount() == 0, effectKey + "燃料或期限结束后轨道结束");
        return damages;
    }

    private static BurnSchedule schedule(String effectKey, boolean targetExclusive, SegmentPlan plan) {
        return BurnSchedule.of(effectKey, targetExclusive, plan.intervalTicks(), plan.components()).orElseThrow();
    }

    private static BurnIdentity identity(FireSeedSnapshot taken, UUID casterId) {
        return new BurnIdentity(taken.castId(), casterId, taken.targetId(), taken.amount(), taken.tiers(),
                taken.remainder(), taken.furnaceBonus());
    }

    private static FireCastContext freshCast(UUID casterId) {
        return new FireCastContext(UUID.randomUUID(), casterId);
    }

    private static UUID target(String label) {
        return UUID.nameUUIDFromBytes(label.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= TOLERANCE;
    }

    private static void expect(boolean condition, String label) {
        if (!condition) {
            failures += 1;
            System.err.println("fail " + label);
        }
    }
}
