package com.bslx.XueXiMod.shu.technique.release.momentum;

import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.WaterReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraSpec;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainNativePolicy;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;

/**
 * 不启动 Minecraft。只核对真实 {@link WaterReleaseDefinition} 上的重水字段。
 * 足额预留、不足变普通、失败退预留和组提交幂等仍由 HeavyWater、HostGroupContext、HostGroupCommit 执行。
 * 那些入口要 ServerPlayer 和水势能力，这里不造玩家，也不把那套账再写一遍。
 */
public final class WaterBladeMomentumCheck {
    public static void main(String[] arguments) {
        freshDefaults();
        bladeMatchesWeepingTier();
        ordersKeepEarlierFields();
        illegalLeavesPrevious();
        otherReleasesStayPlain();
        System.out.println("水刃重水定义检查通过");
    }

    private static void freshDefaults() {
        WaterReleaseDefinition plain = WaterReleaseDefinition.shot("plain_shot", Trajectory.STRAIGHT, 8, 1f, 0.2f);
        expectRelease(plain, 0f, 0, 0, 1f);
        expectNotSword(plain);
        check(plain.trajectoryMutable(), "普通射击默认可变");
        check(plain.acceptsInitialAim(), "普通射击接受出手瞄准");
    }

    private static void bladeMatchesWeepingTier() {
        WaterReleaseDefinition blade = WaterReleaseDefinition.shot("water_blade", Trajectory.STRAIGHT, 10, 1.3f, 0.35f)
                .along(3.2f).span(0.7f).energy(1.35f).wetness(6)
                .heavyMomentum(20f).pressureWetness(26)
                .swordForm();
        expectRelease(blade, 20f, 26, 6, 1.35f);
        expectSword(blade);
        check(blade.lengthScale() == 3.2f, "水刃长度保留");
        check(blade.spanScale() == 0.7f, "水刃宽度保留");
        check(blade.lifetimeTicks() == 10, "水刃寿命保留");
        check(blade.speed() == 1.3f, "水刃速度保留");
        check(blade.thickness() == 0.35f, "水刃厚度保留");
        check("water_blade".equals(blade.id()), "水刃编号保留");
    }

    private static void ordersKeepEarlierFields() {
        WaterReleaseDefinition heavyFirst = WaterReleaseDefinition.shot("heavy_first", Trajectory.STRAIGHT, 10, 1.3f, 0.35f)
                .heavyMomentum(20f).pressureWetness(26)
                .swordForm()
                .energy(1.35f).wetness(6);
        expectRelease(heavyFirst, 20f, 26, 6, 1.35f);
        expectSword(heavyFirst);

        WaterReleaseDefinition swordFirst = WaterReleaseDefinition.shot("sword_first", Trajectory.STRAIGHT, 10, 1.3f, 0.35f)
                .swordForm()
                .energy(1.35f).wetness(6)
                .heavyMomentum(20f).pressureWetness(26);
        expectRelease(swordFirst, 20f, 26, 6, 1.35f);
        expectSword(swordFirst);

        WaterReleaseDefinition wetnessFirst = WaterReleaseDefinition.shot("wetness_first", Trajectory.STRAIGHT, 10, 1.3f, 0.35f)
                .wetness(6).energy(1.35f)
                .pressureWetness(26)
                .swordForm()
                .heavyMomentum(20f);
        expectRelease(wetnessFirst, 20f, 26, 6, 1.35f);
        expectSword(wetnessFirst);

        WaterReleaseDefinition pulseMixed = WaterReleaseDefinition.shot("pulse_mixed", Trajectory.STRAIGHT, 10, 1f, 0.2f)
                .pressure(1.5f)
                .heavyMomentum(20f).pressureWetness(26)
                .energy(1.35f).wetness(6);
        expectRelease(pulseMixed, 20f, 26, 6, 1.35f);
        check(pulseMixed.pressurePulse() == 1.5f, "重水门槛不能盖掉原有压力脉冲");

        WaterReleaseDefinition heavyThenPulse = WaterReleaseDefinition.shot("heavy_then_pulse", Trajectory.STRAIGHT, 10, 1f, 0.2f)
                .pressureWetness(26).heavyMomentum(20f)
                .pressure(2.5f)
                .swordForm();
        expectRelease(heavyThenPulse, 20f, 26, 0, 1f);
        expectSword(heavyThenPulse);
        check(heavyThenPulse.pressurePulse() == 2.5f, "压力脉冲不能清掉重水门槛");
    }

    private static void illegalLeavesPrevious() {
        WaterReleaseDefinition blank = WaterReleaseDefinition.shot("blank_heavy", Trajectory.STRAIGHT, 4, 1f, 0.2f);
        blank.heavyMomentum(Float.NaN);
        blank.heavyMomentum(Float.POSITIVE_INFINITY);
        blank.heavyMomentum(Float.NEGATIVE_INFINITY);
        blank.heavyMomentum(-3f);
        blank.pressureWetness(-1);
        blank.pressureWetness(101);
        expectRelease(blank, 0f, 0, 0, 1f);

        blank.heavyMomentum(20f).pressureWetness(26).wetness(6).energy(1.35f);
        blank.heavyMomentum(Float.NaN).heavyMomentum(-1f).heavyMomentum(Float.POSITIVE_INFINITY);
        blank.pressureWetness(-4).pressureWetness(101);
        expectRelease(blank, 20f, 26, 6, 1.35f);

        blank.heavyMomentum(0f).pressureWetness(0).pressureWetness(100);
        expectRelease(blank, 0f, 100, 6, 1.35f);
        blank.pressureWetness(26);
        expectRelease(blank, 0f, 26, 6, 1.35f);
    }

    private static void otherReleasesStayPlain() {
        WaterReleaseDefinition secondShot = WaterReleaseDefinition.shot("second_shot", Trajectory.DROP, 12, 0.4f, 0.3f)
                .energy(1.1f).wetness(2).swordForm();
        expectRelease(secondShot, 0f, 0, 2, 1.1f);
        expectSword(secondShot);

        WaterReleaseDefinition zone = WaterReleaseDefinition.zone("sample_zone", 3f, 40, 8, 2);
        expectRelease(zone, 0f, 0, 0, 1f);
        expectNotSword(zone);

        WaterReleaseDefinition curtain = WaterReleaseDefinition.barrier("water_curtain", BarrierDefinition.curtain());
        expectRelease(curtain, 0f, 0, 0, 1f);
        expectNotSword(curtain);

        WaterReleaseDefinition ring = WaterReleaseDefinition.bodyGuard("water_ring", GuardDefinition.personalRing());
        expectRelease(ring, 0f, 0, 0, 1f);
        expectNotSword(ring);

        DomainDefinition domain = new DomainDefinition(
                caster -> null,
                0, 0, 20, 80,
                new DomainNativePolicy("plain_domain", 10, 0, 0, 20, 0, 0),
                new DomainAuraSpec("none", 0f),
                false);
        WaterReleaseDefinition domainRelease = WaterReleaseDefinition.domain("sample_domain", domain);
        expectRelease(domainRelease, 0f, 0, 0, 1f);
        expectNotSword(domainRelease);
    }

    private static void expectRelease(WaterReleaseDefinition definition, float heavy, int minimum, int wetness, float energy) {
        check(definition.momentumSpend() == 0f, definition.id() + " 的出手水势必须是 0");
        check(definition.heavyMomentumPerCopy() == heavy, definition.id() + " 的重水每份不对");
        check(definition.pressureWetnessMinimum() == minimum, definition.id() + " 的潮湿门槛不对");
        check(definition.wetnessGrant() == wetness, definition.id() + " 的潮湿不对");
        check(definition.energyScale() == energy, definition.id() + " 的能量不对");
    }

    private static void expectSword(WaterReleaseDefinition definition) {
        check(definition.acceptsInitialAim(), definition.id() + " 仍接受出手瞄准");
        check(definition.requiresHeldSword(), definition.id() + " 必须持剑");
        check(!definition.trajectoryMutable(), definition.id() + " 剑形弹道锁定");
        check(definition.submitsHostGroup(), definition.id() + " 剑形提交宿主组");
        check(definition.usesArcBand(), definition.id() + " 剑形使用弧带");
        check(definition.tracksBeatCycle(), definition.id() + " 剑形记录拍号");
    }

    private static void expectNotSword(WaterReleaseDefinition definition) {
        check(!definition.requiresHeldSword(), definition.id() + " 不应要求持剑");
        check(!definition.submitsHostGroup(), definition.id() + " 不应提交宿主组");
        check(!definition.usesArcBand(), definition.id() + " 不应使用弧带");
        check(!definition.tracksBeatCycle(), definition.id() + " 不应记录拍号");
    }

    private static void check(boolean passed, String label) {
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
