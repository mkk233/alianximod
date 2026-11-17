package com.bslx.XueXiMod.status.fire.furnace;

/**
 * 炉心状态机和公式。不构造玩家，也不声称牛奶、抗性或 NBT 恢复已经在游戏里走过。
 */
public final class FurnaceMathCheck {
    private static int assertionCount;
    private static int failures;

    private FurnaceMathCheck() {
    }

    public static void main(String[] arguments) {
        section("爬坡", FurnaceMathCheck::climb);
        section("离线", FurnaceMathCheck::offline);
        section("转换", FurnaceMathCheck::transitions);
        section("清洗", FurnaceMathCheck::sanitize);
        section("查询", FurnaceMathCheck::queries);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未运行：属性修饰挂载、治疗事件、牛奶、命令清除、抗性与保护、登录跨维克隆、Mixin。没有游戏世界。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void climb() {
        double bonus = 0.0D;
        int stepIndex = 0;
        while (stepIndex < 149) {
            bonus = FurnaceMath.advanceOnline(bonus, FurnaceMath.Tier.NORMAL, 1L);
            stepIndex++;
        }
        check(bonus < FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL), "普通 149 拍不应封顶，实际 " + bonus);
        bonus = FurnaceMath.advanceOnline(bonus, FurnaceMath.Tier.NORMAL, 1L);
        check(bonus == FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL), "普通 150 拍应封顶 0.30，实际 " + bonus);
        bonus = FurnaceMath.advanceOnline(bonus, FurnaceMath.Tier.NORMAL, 1L);
        check(bonus == FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL), "封顶后继续普通爬坡不应超过 0.30");

        double overdrive = 0.0D;
        int overdriveSteps = 0;
        while (overdriveSteps < FurnaceMath.OVERDRIVE_TICKS_TO_CEILING) {
            overdrive = FurnaceMath.advanceOnline(overdrive, FurnaceMath.Tier.OVERDRIVE, 1L);
            overdriveSteps++;
        }
        check(overdrive == FurnaceMath.ceiling(FurnaceMath.Tier.OVERDRIVE), "过载 200 拍应封顶 0.60，实际 " + overdrive);

        double upgraded = FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL);
        int upgradeSteps = 0;
        while (upgradeSteps < 99) {
            upgraded = FurnaceMath.advanceOnline(upgraded, FurnaceMath.Tier.OVERDRIVE, 1L);
            upgradeSteps++;
        }
        check(upgraded < FurnaceMath.ceiling(FurnaceMath.Tier.OVERDRIVE), "普通满热升级 99 拍不应到 0.60，实际 " + upgraded);
        upgraded = FurnaceMath.advanceOnline(upgraded, FurnaceMath.Tier.OVERDRIVE, 1L);
        check(upgraded == FurnaceMath.ceiling(FurnaceMath.Tier.OVERDRIVE), "普通满热升级再 100 拍应到 0.60，实际 " + upgraded);
        check(FurnaceMath.rate(FurnaceMath.Tier.NORMAL) == FurnaceMath.fromUnits(FurnaceMath.NORMAL_RATE_UNITS),
                "普通每拍 0.002");
        check(FurnaceMath.rate(FurnaceMath.Tier.OVERDRIVE) == FurnaceMath.fromUnits(FurnaceMath.OVERDRIVE_RATE_UNITS),
                "过载每拍 0.003");
    }

    private static void offline() {
        double once = FurnaceMath.advanceOnline(0.0D, FurnaceMath.Tier.NORMAL, 10_000L);
        check(once == FurnaceMath.rate(FurnaceMath.Tier.NORMAL), "一万拍间隔只加一拍，实际 " + once);
        check(FurnaceMath.advanceOnline(once, FurnaceMath.Tier.NORMAL, 0L) == once, "同一拍不重复加");
        check(FurnaceMath.advanceOnline(once, FurnaceMath.Tier.NORMAL, -40L) == once, "时钟回拨不加离线热量");
        check(FurnaceMath.advanceOnline(once, null, 5L) == once, "没有档位不增加已有热量");
    }

    private static void transitions() {
        FurnaceMath.Transition entered = FurnaceMath.transition(
                null, 0.2D, 7, FurnaceMath.Action.ENTER_NORMAL);
        check(entered.outcome() == FurnaceMath.Outcome.ENTERED_NORMAL && entered.bonus() == 0.0D
                        && entered.tier() == FurnaceMath.Tier.NORMAL && entered.lastTick() == 7,
                "无到普通成功且不沿用旧热量");
        FurnaceMath.Transition direct = FurnaceMath.transition(
                null, 0.0D, 3, FurnaceMath.Action.ENTER_OVERDRIVE);
        check(direct.outcome() == FurnaceMath.Outcome.ENTERED_OVERDRIVE && direct.tier() == FurnaceMath.Tier.OVERDRIVE,
                "无到过载成功");

        double kept = FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL);
        FurnaceMath.Transition upgraded = FurnaceMath.transition(
                FurnaceMath.Tier.NORMAL, kept, 80, FurnaceMath.Action.ENTER_OVERDRIVE);
        check(upgraded.outcome() == FurnaceMath.Outcome.UPGRADED
                        && upgraded.bonus() == kept
                        && upgraded.lastTick() == 80
                        && upgraded.tier() == FurnaceMath.Tier.OVERDRIVE,
                "普通升过载保留热量和时钟");

        FurnaceMath.Transition repeatNormal = FurnaceMath.transition(
                FurnaceMath.Tier.NORMAL, 0.12D, 40, FurnaceMath.Action.ENTER_NORMAL);
        check(repeatNormal.outcome() == FurnaceMath.Outcome.REJECT_ALREADY_NORMAL
                        && repeatNormal.bonus() == 0.12D
                        && repeatNormal.lastTick() == 40,
                "重复进入普通失败且不重置");
        FurnaceMath.Transition repeatOverdrive = FurnaceMath.transition(
                FurnaceMath.Tier.OVERDRIVE, 0.4D, 41, FurnaceMath.Action.ENTER_OVERDRIVE);
        check(repeatOverdrive.outcome() == FurnaceMath.Outcome.REJECT_ALREADY_OVERDRIVE
                        && repeatOverdrive.bonus() == 0.4D
                        && repeatOverdrive.lastTick() == 41,
                "重复进入过载失败且不重置");
        FurnaceMath.Transition downgrade = FurnaceMath.transition(
                FurnaceMath.Tier.OVERDRIVE, 0.4D, 41, FurnaceMath.Action.ENTER_NORMAL);
        check(downgrade.outcome() == FurnaceMath.Outcome.REJECT_CANNOT_DOWNGRADE
                        && downgrade.bonus() == 0.4D
                        && downgrade.tier() == FurnaceMath.Tier.OVERDRIVE
                        && downgrade.lastTick() == 41,
                "过载不能降普通，热量和时钟保持");

        FurnaceMath.Transition ended = FurnaceMath.transition(
                FurnaceMath.Tier.NORMAL, 0.1D, 9, FurnaceMath.Action.END);
        check(ended.outcome() == FurnaceMath.Outcome.ENDED && ended.tier() == null && ended.bonus() == 0.0D,
                "被动结束清档");
        FurnaceMath.Transition cooled = FurnaceMath.transition(
                FurnaceMath.Tier.OVERDRIVE, 0.2D, 9, FurnaceMath.Action.COOL);
        check(cooled.outcome() == FurnaceMath.Outcome.COOLED && cooled.tier() == null && cooled.outcome().succeeded(),
                "主动冷却先成功退出");
        FurnaceMath.Transition idle = FurnaceMath.transition(null, 0.2D, 9, FurnaceMath.Action.END);
        check(idle.outcome() == FurnaceMath.Outcome.REJECT_INACTIVE && idle.bonus() == 0.2D && idle.lastTick() == 9,
                "没有炉心时结束失败且不改传入热量");
        check(!repeatNormal.outcome().succeeded() && ended.outcome().succeeded(), "成败和结果枚举一致");
    }

    private static void sanitize() {
        FurnaceMath.Sanitized broken = FurnaceMath.sanitize(FurnaceMath.CURRENT_VERSION, 0, Double.NaN);
        check(broken.zeroedBonus() && broken.bonus() == 0.0D && !broken.rejectedVersion(), "NaN 归零");
        FurnaceMath.Sanitized negative = FurnaceMath.sanitize(FurnaceMath.CURRENT_VERSION, 1, -0.2D);
        check(negative.zeroedBonus() && negative.bonus() == 0.0D && negative.amplifier() == 1, "负数归零且过载档保留");
        FurnaceMath.Sanitized infinite = FurnaceMath.sanitize(FurnaceMath.CURRENT_VERSION, 0, Double.POSITIVE_INFINITY);
        check(infinite.zeroedBonus() && infinite.bonus() == 0.0D, "无穷大归零");
        FurnaceMath.Sanitized badVersion = FurnaceMath.sanitize(0, 1, 0.2D);
        check(badVersion.rejectedVersion() && badVersion.zeroedBonus() && badVersion.bonus() == 0.0D, "未知版本归零");
        FurnaceMath.Sanitized highNormal = FurnaceMath.sanitize(FurnaceMath.CURRENT_VERSION, 0, 0.9D);
        check(!highNormal.zeroedBonus() && highNormal.bonus() == FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL),
                "普通超上限只钳制");
        FurnaceMath.Sanitized highOverdrive = FurnaceMath.sanitize(FurnaceMath.CURRENT_VERSION, 1, 0.9D);
        check(!highOverdrive.zeroedBonus() && highOverdrive.bonus() == FurnaceMath.ceiling(FurnaceMath.Tier.OVERDRIVE),
                "过载超上限只钳制");
        FurnaceMath.Sanitized illegal = FurnaceMath.sanitize(FurnaceMath.CURRENT_VERSION, 5, 0.2D);
        check(illegal.repairedAmplifier() && illegal.amplifier() == 1 && illegal.bonus() == 0.2D, "非法正档收束到过载且不丢热量");
        FurnaceMath.Sanitized negativeAmplifier = FurnaceMath.sanitize(FurnaceMath.CURRENT_VERSION, -3, 0.2D);
        check(negativeAmplifier.repairedAmplifier() && negativeAmplifier.amplifier() == 0
                        && negativeAmplifier.bonus() == 0.2D,
                "负档收束到普通");
        check(!FurnaceMath.foreignOwner("same-player", "same-player"), "同一玩家不是外来存档");
        check(FurnaceMath.foreignOwner("other-player", "same-player"), "其他玩家的存档要归零");
        check(!FurnaceMath.foreignOwner("", "same-player"), "空身份留给写入时补，不直接当外来热量");
    }

    private static void queries() {
        check(FurnaceMath.seedMultiplier(null, 0.3D) == 1.0D, "无炉心产火种倍率 1");
        check(FurnaceMath.seedMultiplier(FurnaceMath.Tier.NORMAL, 0.0D) == 1.12D, "普通零热量产火种 1.12");
        double half = FurnaceMath.fromUnits(150);
        check(FurnaceMath.seedMultiplier(FurnaceMath.Tier.NORMAL, half) == 1.12D + 0.48D * 0.5D,
                "普通半热产火种 1.36");
        check(FurnaceMath.seedMultiplier(FurnaceMath.Tier.NORMAL, FurnaceMath.ceiling(FurnaceMath.Tier.NORMAL))
                        == 1.12D + 0.48D,
                "普通满热产火种 1.60");
        check(FurnaceMath.seedMultiplier(FurnaceMath.Tier.OVERDRIVE, 0.0D) == 2.2D, "过载产火种固定 2.2");
        check(FurnaceMath.waterIceMultiplier(null) == 1.0D, "无炉心水冰承伤 1");
        check(FurnaceMath.waterIceMultiplier(FurnaceMath.Tier.NORMAL) == 1.4D, "普通水冰承伤 1.4");
        check(FurnaceMath.waterIceMultiplier(FurnaceMath.Tier.OVERDRIVE) == 1.7D, "过载水冰承伤 1.7");
        check(FurnaceMath.healFactor(null) == 1.0D, "无炉心不缩放治疗");
        check(FurnaceMath.healFactor(FurnaceMath.Tier.NORMAL) == 0.5D, "普通治疗 0.5");
        check(FurnaceMath.healFactor(FurnaceMath.Tier.OVERDRIVE) == 0.3D, "过载治疗 0.3");
        check(FurnaceMath.totalModifier(FurnaceMath.healthFactor(FurnaceMath.Tier.NORMAL)) == 0.75D - 1.0D,
                "普通生命上限乘 0.75");
        check(FurnaceMath.totalModifier(FurnaceMath.healthFactor(FurnaceMath.Tier.OVERDRIVE)) == 0.6D - 1.0D,
                "过载生命上限乘 0.60");
        check(FurnaceMath.totalModifier(FurnaceMath.armorFactor(FurnaceMath.Tier.NORMAL)) == 0.5D - 1.0D,
                "普通护甲乘 0.50");
        check(FurnaceMath.totalModifier(FurnaceMath.armorFactor(FurnaceMath.Tier.OVERDRIVE)) == 0.25D - 1.0D,
                "过载护甲乘 0.25");
        check(FurnaceMath.toughnessFactor(FurnaceMath.Tier.NORMAL) == FurnaceMath.armorFactor(FurnaceMath.Tier.NORMAL)
                        && FurnaceMath.toughnessFactor(FurnaceMath.Tier.OVERDRIVE)
                        == FurnaceMath.armorFactor(FurnaceMath.Tier.OVERDRIVE),
                "韧性与护甲同一倍率");
        check(FurnaceMath.legalAmplifier(0) == 0 && FurnaceMath.legalAmplifier(1) == 1
                        && FurnaceMath.legalAmplifier(4) == 1 && FurnaceMath.legalAmplifier(-1) == 0,
                "只有 0 和 1 两档");
    }

    private static void section(String title, Runnable body) {
        int before = failures;
        body.run();
        System.out.println(title + (failures == before ? " 通过" : " 失败"));
    }

    private static void check(boolean condition, String label) {
        assertionCount++;
        if (!condition) {
            failures++;
            System.out.println("失败: " + label);
        }
    }
}
