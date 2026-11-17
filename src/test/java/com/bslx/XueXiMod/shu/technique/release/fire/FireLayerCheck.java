package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.UUID;

/** 同层公式、元素只加一次、蒸汽友敌和固定伤害。不启动游戏。 */
public final class FireLayerCheck {
    private static final double TOLERANCE = 1.0e-4d;
    private static int assertionCount;
    private static int failures;

    private FireLayerCheck() {
    }

    public static void main(String[] arguments) {
        section("同层", FireLayerCheck::sameLayer);
        section("水冰一次", FireLayerCheck::waterIceOnce);
        section("蒸汽", FireLayerCheck::steam);
        section("范围", FireLayerCheck::scopes);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：真实 hurt、护甲吸收、抗火药水、事件取消。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void sameLayer() {
        double once = FireLayer.combine(2.0d, 0.1d, 0.3d);
        double prepared = 2.0d * (1.0d + 0.1d);
        double adjusted = ElementAdjustment.fireAmount(prepared, 2.0d, 0.3d, false);
        double settled = ElementAdjustment.fireAmount(adjusted, 2.0d, 0.3d, true);
        double multiplied = prepared * (1.0d + 0.3d);
        check(near(once, 2.8d), "B=2 A=0.1 F=0.3 得到 2.8");
        check(near(adjusted, once), "未结算事件补上的是 B×F，和同层公式相同");
        check(near(settled, once), "已结算后再遇到事件保持原值");
        check(Math.abs(once - multiplied) > 0.01d, "同层不是先乘资源再乘炉心");
        check(near(ElementAdjustment.fireAmount(prepared, 2.0d, 0.0d, false), prepared), "没有炉心时不改伤害");
        check(!FireLayer.acceptableDamage(Double.NaN), "非有限伤害拒绝");
        check(!FireLayer.acceptableDamage(0.0d), "零伤害拒绝");
        check(!FireLayer.acceptableDamage(-1.0d), "负伤害拒绝");
        check(FireLayer.acceptableDamage(FireLimits.DAMAGE_CAP), "上限本身仍接受");
        check(!FireLayer.acceptableDamage(FireLimits.DAMAGE_CAP + 1.0d), "超过上限拒绝");
        check(BurnComponent.of(2.0d, -0.1d).isEmpty(), "负的资源增量拒绝");
        check(BurnComponent.of(0.0d, 0.0d).isEmpty(), "零基础拒绝");
    }

    private static void waterIceOnce() {
        double water = ElementAdjustment.vulnerableAmount(10.0d, 1.4d, false);
        double held = ElementAdjustment.vulnerableAmount(water, 1.7d, true);
        double overdrive = ElementAdjustment.vulnerableAmount(10.0d, 1.7d, false);
        check(near(water, 14.0d), "普通炉心水冰承伤 1.4");
        check(near(held, 14.0d), "已经乘过就不再乘过载倍率");
        check(near(overdrive, 17.0d), "过载单独是 1.7");
        check(near(ElementAdjustment.vulnerableAmount(10.0d, 1.0d, false), 10.0d), "无炉心倍率 1 不改伤害");
        check(near(ElementAdjustment.vulnerableAmount(10.0d, Double.NaN, false), 10.0d), "非有限倍率不改伤害");
    }

    private static void steam() {
        check(SteamTuning.BASE_DAMAGE == 2.0f, "蒸汽基础伤害固定 2");
        check(SteamTuning.RADIUS == 3.0d, "蒸汽半径 3");
        check(SteamTuning.reaches(0.0d), "中心点在球内");
        check(SteamTuning.reaches(9.0d), "3 格边界在球内");
        check(!SteamTuning.reaches(9.0d + 1.0e-6d), "超过 3 格不在球内");
        check(!SteamHostility.struck(true, false, false, true), "自己排除");
        check(!SteamHostility.struck(false, true, false, true), "同队排除");
        check(!SteamHostility.struck(false, false, true, true), "驯服主人或队友的宠物排除");
        check(!SteamHostility.struck(false, false, false, false), "关系策略拒绝时不打");
        check(SteamHostility.struck(false, false, false, true), "其余敌对目标可打");
        SteamVector overlap = SteamVector.horizontal(0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
        check(near(overlap.towardX(), 0.0d) && near(overlap.towardZ(), -1.0d), "重合时按朝南的反向量交给击退");
        SteamVector east = SteamVector.horizontal(0.0d, 0.0d, 3.0d, 0.0d, 90.0d);
        check(near(east.towardX(), -3.0d) && near(east.towardZ(), 0.0d), "分开的目标沿连线，不改用朝向");
    }

    private static void scopes() {
        Object target = new Object();
        Object source = new Object();
        check(ElementDamageScope.currentHurt() == null, "未进入 hurt 时没有元素来源");
        ElementDamageScope.ElementArm waterArm = ElementDamageScope.armWaterIce();
        ElementDamageScope.HurtCall outer = ElementDamageScope.enterHurt(target, source);
        try {
            ElementDamageScope.Frame waterIce = outer.frame();
            check(waterIce.water() && waterIce.ice() && !waterIce.fire(), "水冰同时标记但仍是一次 hurt");
            check(!waterIce.vulnerabilityApplied(), "易伤还没乘");
            ElementDamageScope.ElementArm fireArm = ElementDamageScope.armFire(UUID.randomUUID(), 2.0d, false);
            ElementDamageScope.HurtCall inner = ElementDamageScope.enterHurt(target, source);
            try {
                check(inner.identity() != outer.identity(), "同一目标和来源的嵌套也是另一次 hurt");
                check(inner.target() == target && inner.source() == source, "内层记录的是这次调用自己的目标和来源");
                check(ElementDamageScope.currentHurt() == inner, "事件只看见栈顶这次 hurt");
                ElementDamageScope.Frame fire = inner.frame();
                check(fire.fire() && !fire.furnaceSettled() && near(fire.fireBase(), 2.0d), "内层火伤尚未结算炉心");
                fire.settleFurnace();
                fire.applyVulnerability();
                check(fire.furnaceSettled() && fire.vulnerabilityApplied(), "结算标记留在内层");
                check(!outer.frame().vulnerabilityApplied(), "内层不得消耗外层易伤");
            } finally {
                ElementDamageScope.exitHurt(inner);
                ElementDamageScope.disarm(fireArm);
            }
            check(ElementDamageScope.currentHurt() == outer, "内层结束后回到外层这次 hurt");
            ElementDamageScope.HurtCall unmarked = ElementDamageScope.enterHurt(target, source);
            try {
                check(unmarked.frame() == null, "未标记的嵌套 hurt 不继承外层");
                check(outer.frame() == waterIce, "外层帧仍留在自己的调用上");
            } finally {
                ElementDamageScope.exitHurt(unmarked);
            }
        } finally {
            ElementDamageScope.exitHurt(outer);
            ElementDamageScope.disarm(waterArm);
        }
        check(ElementDamageScope.currentHurt() == null, "全部退出后不再把后续伤害当成元素");
        ElementDamageScope.ElementArm pending = ElementDamageScope.armWater();
        ElementDamageScope.ElementArm neutral = ElementDamageScope.armNeutral();
        ElementDamageScope.HurtCall steam = ElementDamageScope.enterHurt(target, source);
        try {
            check(steam.frame() == null, "显式中性不提供元素");
        } finally {
            ElementDamageScope.exitHurt(steam);
            ElementDamageScope.disarm(neutral);
        }
        ElementDamageScope.HurtCall later = ElementDamageScope.enterHurt(target, source);
        try {
            check(later.frame() != null && later.frame().water() && !later.frame().fire(), "中性只领走自己，水帧留给下一次");
        } finally {
            ElementDamageScope.exitHurt(later);
            ElementDamageScope.disarm(pending);
        }
        ElementDamageScope.ElementArm iceArm = ElementDamageScope.armIce();
        ElementDamageScope.HurtCall ice = ElementDamageScope.enterHurt(target, source);
        try {
            check(ice.frame().ice() && !ice.frame().water(), "单独冰");
        } finally {
            ElementDamageScope.exitHurt(ice);
            ElementDamageScope.disarm(iceArm);
        }
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
