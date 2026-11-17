package com.bslx.XueXiMod.status;

import com.bslx.XueXiMod.shu.technique.release.domain.PressureBreath;

/**
 * 直接调用已编译的分档、下降和呼吸纯函数。不复制公式，也不构造生物。
 */
public final class WetnessTierCheck {
    private static int assertionCount;
    private static int failures;

    private WetnessTierCheck() {
    }

    public static void main(String[] arguments) {
        section("分档", WetnessTierCheck::tiers);
        section("下降", WetnessTierCheck::descent);
        section("呼吸", WetnessTierCheck::breath);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未运行：非空实体的 of、beforeTravel、afterAiStep、属性修饰挂载、LivingBreatheEvent。没有游戏世界。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void tiers() {
        expectTier(0, 0);
        expectTier(1, 1);
        expectTier(25, 1);
        expectTier(26, 2);
        expectTier(50, 2);
        expectTier(51, 3);
        expectTier(75, 3);
        expectTier(76, 4);
        expectTier(100, 4);
        expectTier(-1, 0);
        expectTier(Integer.MIN_VALUE, 0);
        expectTier(101, 4);
        expectTier(Integer.MAX_VALUE, 4);
        check(WetnessTiers.of(null).tier() == 0 && WetnessTiers.of(null).qualifyingFunds() == 20f,
                "没有实体仍是 0 档资格 20，实际档 " + WetnessTiers.of(null).tier());
        WetnessTiers.Profile negative = WetnessTiers.ofValue(-8);
        check(negative.tier() == 0 && negative.movementReduction() == 0.0D && negative.attackReward() == 0f
                        && negative.meleeCost() == 20f && negative.arrowCostRatio() == 1f && negative.breathMultiplier() == 1,
                "负数没有溢出成别的费用");
        WetnessTiers.Profile overflow = WetnessTiers.ofValue(180);
        check(overflow.tier() == 4 && overflow.qualifyingFunds() == 10f && overflow.arrowCostRatio() == 0.5f,
                "超上限仍是 4 档，没有把湿度本身当成费用");
        check(profileFinal(), "Profile 组件字段是 final");
    }

    private static void expectTier(int wetness, int tier) {
        WetnessTiers.Profile profile = WetnessTiers.ofValue(wetness);
        boolean movement;
        boolean reward;
        boolean gate;
        boolean melee;
        boolean ratio;
        boolean breath;
        if (tier == 0) {
            movement = profile.movementReduction() == 0.0D;
            reward = profile.attackReward() == 0f;
            gate = profile.qualifyingFunds() == 20f;
            melee = profile.meleeCost() == 20f;
            ratio = profile.arrowCostRatio() == 1f;
            breath = profile.breathMultiplier() == 1;
        } else if (tier == 1) {
            movement = profile.movementReduction() == 0.15D;
            reward = profile.attackReward() == 0f;
            gate = profile.qualifyingFunds() == 20f;
            melee = profile.meleeCost() == 20f;
            ratio = profile.arrowCostRatio() == 1f;
            breath = profile.breathMultiplier() == 1;
        } else if (tier == 2) {
            movement = profile.movementReduction() == 0.15D;
            reward = profile.attackReward() == 1f;
            gate = profile.qualifyingFunds() == 15f;
            melee = profile.meleeCost() == 15f;
            ratio = profile.arrowCostRatio() == 0.75f;
            breath = profile.breathMultiplier() == 1;
        } else if (tier == 3) {
            movement = profile.movementReduction() == 0.30D;
            reward = profile.attackReward() == 2f;
            gate = profile.qualifyingFunds() == 15f;
            melee = profile.meleeCost() == 15f;
            ratio = profile.arrowCostRatio() == 0.75f;
            breath = profile.breathMultiplier() == 1;
        } else {
            movement = profile.movementReduction() == 0.30D;
            reward = profile.attackReward() == 2f;
            gate = profile.qualifyingFunds() == 10f;
            melee = profile.meleeCost() == 10f;
            ratio = profile.arrowCostRatio() == 0.5f;
            breath = profile.breathMultiplier() == 2;
        }
        check(profile.tier() == tier && movement && reward && gate && melee && ratio && breath,
                "湿度 " + wetness + " 期望档 " + tier + " 实际 " + profile);
    }

    private static boolean profileFinal() {
        boolean allFinal = true;
        for (java.lang.reflect.Field field : WetnessTiers.Profile.class.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            if (!java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                allFinal = false;
            }
        }
        return allFinal;
    }

    private static void descent() {
        check(WetnessFlight.SLOW_LIMIT == -0.08D && WetnessFlight.FAST_LIMIT == -0.30D
                        && WetnessFlight.SLOW_LIMIT > WetnessFlight.FAST_LIMIT,
                "三级上限应慢于四级");
        check(WetnessFlight.limitedVertical(3, 0.0D) == -0.08D, "三级把向上或过慢的竖直速度收到 -0.08");
        check(WetnessFlight.limitedVertical(3, -0.08D) == -0.08D, "三级刚好等于上限时保持");
        check(WetnessFlight.limitedVertical(3, -0.20D) == -0.20D, "三级已经更快时不抬回去");
        check(WetnessFlight.limitedVertical(4, -0.10D) == -0.30D, "四级慢于 -0.30 时收到 -0.30");
        check(WetnessFlight.limitedVertical(4, -0.30D) == -0.30D, "四级刚好等于上限时保持");
        check(WetnessFlight.limitedVertical(4, -0.50D) == -0.50D, "四级已经更快时保留");
        check(WetnessFlight.limitedVertical(2, -0.01D) == -0.01D, "二级不夹竖直速度");
        check(WetnessFlight.limitedVertical(1, 0.40D) == 0.40D, "一级不夹竖直速度");
        check(WetnessFlight.limitedVertical(0, -0.01D) == -0.01D, "零档不夹竖直速度");
        check(WetnessFlight.descentEligible(true, false, false, false, true, true, true),
                "玩家在空中时进入下降资格");
        check(!WetnessFlight.descentEligible(false, true, false, false, true, false, false), "乘客不下降");
        check(!WetnessFlight.descentEligible(false, false, true, false, true, true, true),
                "落地不强制穿地");
        check(!WetnessFlight.descentEligible(false, false, false, true, true, true, true),
                "实际流体里不额外下压");
        check(WetnessFlight.descentEligible(false, false, false, false, true, false, false),
                "飞行身体不再决定资格，空中仍允许");
        check(WetnessFlight.descentEligible(false, false, false, false, false, true, false),
                "正在飞不再决定资格，空中仍允许");
        check(WetnessFlight.descentEligible(false, false, false, false, false, false, true),
                "显式探针不再决定资格，空中仍允许");
        check(WetnessFlight.descentEligible(false, false, false, false, false, false, false),
                "没有飞行类别的空中生物现在也进入下降资格");
        SampleFlyer flyer = new SampleFlyer();
        check(flyer.wetnessDescentActive(), "扩展接口默认启用");
        WetnessFlight.register(null);
    }

    private static void breath() {
        PressureBreath.BreathMath both = PressureBreath.combineBreath(1, 2f, 2);
        check(both.denyBreath() && both.consumeAmount() == 2, "领域 2 倍和四级 2 倍同时仍是 2 倍，实际 " + both);
        PressureBreath.BreathMath domainOnly = PressureBreath.combineBreath(3, 2f, 1);
        check(domainOnly.denyBreath() && domainOnly.consumeAmount() == 6, "只有领域时按领域倍率，实际 " + domainOnly);
        PressureBreath.BreathMath wetOnly = PressureBreath.combineBreath(3, 1f, 2);
        check(wetOnly.denyBreath() && wetOnly.consumeAmount() == 6, "只有四级时按 2 倍，实际 " + wetOnly);
        PressureBreath.BreathMath capped = PressureBreath.combineBreath(3, 4f, 4);
        check(capped.denyBreath() && capped.consumeAmount() == 6, "两边都高于 2 时封顶 2 倍，实际 " + capped);
        PressureBreath.BreathMath untouched = PressureBreath.combineBreath(4, 1f, 1);
        check(!untouched.denyBreath() && untouched.consumeAmount() == 4, "两边都没有加速时不改消耗，实际 " + untouched);
        PressureBreath.BreathMath zeroConsume = PressureBreath.combineBreath(0, 2f, 2);
        check(zeroConsume.denyBreath() && zeroConsume.consumeAmount() == 0,
                "原版本拍消耗 0 时不放大，实际 " + zeroConsume);
        PressureBreath.BreathMath airTier = PressureBreath.combineBreath(0, 1f, 2);
        check(airTier.denyBreath() && airTier.consumeAmount() == 0,
                "空气中的四级仍拒绝呼吸和回气，但不凭空扣正数，实际 " + airTier);
        check(PressureBreath.resists(null), "空实体按豁免返回");
        check(!PressureBreath.marksSuffocation(null), "空实体不标记深海窒息");
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

    private static final class SampleFlyer implements WetnessFlyer {
    }
}
