package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;

import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 水牢基准寿命、壳倍率和预约输入。直接调用生产方法，不构造生物。
 */
public final class PrisonEnergyCheck {
    private static int assertionCount;
    private static int failures;

    private PrisonEnergyCheck() {
    }

    public static void main(String[] arguments) {
        durations();
        shell();
        reservation();
        expiryAddition();
        wetSupport();
        System.out.println("EXECUTED prison-duration-shell-input-wet-support");
        System.out.println("未执行 prepare 会话表和 commit 湿润生物：需要玩家和世界");
        System.out.println("静态核对 未执行：commit 里 replaceExact 的真实药水、onWetRemoved 直接编辑保护、牛奶等外部清除即时拆牢、预约第二次 commit 不再进入运行时。这些要实体和效果回调，不能当成已经集成通过。");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void durations() {
        ReleaseInput baseline = ReleaseInput.BASE;
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        check(PrisonRuntime.scaledDuration(1, 80, baseline) == 20, "一档基准不是 20");
        check(PrisonRuntime.scaledDuration(1, 80, doubled) == 40, "一档两倍不是 40");
        check(PrisonRuntime.scaledDuration(1, 80, quadrupled) == 80, "一档四倍不是 80");
        check(PrisonRuntime.scaledDuration(2, 1000, doubled) == 200, "二档两倍不是 200");
        check(PrisonRuntime.scaledDuration(2, 1000, quadrupled) == 400, "二档四倍不是 400");
        check(PrisonRuntime.scaledDuration(4, 80, doubled) == 160, "四档剩余 80 的两倍不是 160");
        check(PrisonRuntime.scaledDuration(4, 80, quadrupled) == 320, "四档剩余 80 的四倍不是 320");
        check(PrisonRuntime.scaledDuration(3, 5, doubled) == 4, "三档一半剩余没有先取基准再乘");
        check(PrisonRuntime.scaledDuration(3, 0, quadrupled) == 0, "三档非正剩余被抬成正数");
        check(PrisonRuntime.scaledDuration(4, -5, quadrupled) == -5, "四档负剩余被改写");
        check(PrisonRuntime.scaledDuration(0, 40, doubled) == 0, "无效档被乘出了寿命");
    }

    private static void shell() {
        AABB body = new AABB(0.0D, 64.0D, 0.0D, 1.0D, 66.0D, 1.0D);
        AABB outerOnce = PrisonShell.expand(body);
        AABB innerOnce = PrisonShell.innerOf(outerOnce);
        check(outerOnce != null && innerOnce != null, "基准壳是空的");
        if (outerOnce == null || innerOnce == null) {
            return;
        }
        AABB outerSame = PrisonShell.expand(body, 1.0D);
        AABB innerSame = PrisonShell.innerFor(body, 1.0D);
        check(sameBox(outerOnce, outerSame), "一倍外壳偏离了原 expand");
        check(sameBox(innerOnce, innerSame), "一倍内盒偏离了原 innerOf");
        expectScaled(body, outerOnce, innerOnce, 2.0D);
        expectScaled(body, outerOnce, innerOnce, 4.0D);
        AABB outerNan = PrisonShell.expand(body, Double.NaN);
        check(sameBox(outerOnce, outerNan), "非法倍率把外壳收掉了");
        check(PrisonShell.expand(null) == null && PrisonShell.expand(null, 2.0D) == null, "空外壳没有保持空");
        check(PrisonShell.innerFor(null, 2.0D) == null, "空内盒没有保持空");
    }

    private static void expectScaled(AABB body, AABB outerOnce, AABB innerOnce, double factor) {
        AABB outerScaled = PrisonShell.expand(body, factor);
        AABB innerScaled = PrisonShell.innerFor(body, factor);
        check(outerScaled != null && innerScaled != null, factor + " 倍壳是空的");
        if (outerScaled == null || innerScaled == null) {
            return;
        }
        check(sameScale(body, outerOnce, outerScaled, factor), factor + " 倍外壳没有绕目标中心放大");
        check(sameScale(body, innerOnce, innerScaled, factor), factor + " 倍内盒没有按同一倍率冻结");
        double baseGap = innerOnce.minX - outerOnce.minX;
        double scaledGap = innerScaled.minX - outerScaled.minX;
        check(Math.abs(scaledGap - baseGap * factor) < 1.0E-6D, factor + " 倍壳厚没有一起变");
        AABB reversed = outerScaled.inflate(-PrisonShell.PADDING);
        check(Math.abs(innerScaled.minX - reversed.minX) > 1.0E-4D, factor + " 倍内盒被固定 0.20 反推");
    }

    private static void reservation() {
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        PrisonReservation stored = new PrisonReservation(UUID.randomUUID(), null, null, true, "", doubled);
        ReleaseInput kept = PrisonRuntime.bindInput(stored, quadrupled);
        check(kept == doubled, "重试换成了后一次倍率");
        check(stored.releaseInput() == doubled, "预约里的首次输入被替换");
        check(PrisonRuntime.bindInput(null, quadrupled) == quadrupled, "首次输入没有采用这次提供的倍率");
        check(PrisonRuntime.bindInput(null, null) == ReleaseInput.BASE, "空输入没有回到基准");
        check(stored.releaseInput().sizeMultiplier() == 2f, "保存的倍率不是 2");
    }

    private static void wetSupport() {
        ReleaseInput baseline = ReleaseInput.BASE;
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        int tierFour = PrisonRuntime.scaledDuration(4, 1000, doubled);
        check(tierFour == 2000, "四档 1000 乘 2 的牢寿命不是 2000");
        check(PrisonRuntime.replacementDuration(1000, tierFour, doubled.sizeMultiplier()) == 2001,
                "四档 1000 乘 2 没有把潮湿支持期补到 2001");
        check(PrisonRuntime.scaledDuration(4, 2001, doubled) != tierFour, "支持期被拿回去重算了牢寿命");
        long birthTick = 80L;
        long endTick = ReleaseInput.expiryGameTime(birthTick, tierFour);
        check(endTick == birthTick + tierFour, "牢到期点被支持期的额外 1 拍推后");
        int tierThree = PrisonRuntime.scaledDuration(3, 1000, quadrupled);
        check(tierThree == 2000, "三档 1000 的一半乘 4 不是 2000");
        check(PrisonRuntime.replacementDuration(1000, tierThree, quadrupled.sizeMultiplier()) == 2001,
                "三档四倍没有把 1000 补到 2001");
        int tierOneShort = PrisonRuntime.scaledDuration(1, 30, doubled);
        check(tierOneShort == 40, "一档两倍牢寿命不是 40");
        check(PrisonRuntime.replacementDuration(30, tierOneShort, doubled.sizeMultiplier()) == 41,
                "一档原剩余 30 不足时没有补到 41");
        check(PrisonRuntime.replacementDuration(1000, tierOneShort, doubled.sizeMultiplier()) == 1000,
                "一档原剩余已经够用时仍被改短或改长");
        int tierTwoShort = PrisonRuntime.scaledDuration(2, 50, quadrupled);
        check(tierTwoShort == 400, "二档四倍牢寿命不是 400");
        check(PrisonRuntime.replacementDuration(50, tierTwoShort, quadrupled.sizeMultiplier()) == 401,
                "二档原剩余 50 不足时没有补到 401");
        check(PrisonRuntime.replacementDuration(1000, PrisonRuntime.scaledDuration(2, 1000, doubled), doubled.sizeMultiplier()) == 1000,
                "二档原剩余 1000 盖住 200 时仍被延长");
        int tierOneBase = PrisonRuntime.scaledDuration(1, 10, baseline);
        check(tierOneBase == 20, "基准一档寿命变了");
        check(PrisonRuntime.replacementDuration(10, tierOneBase, baseline.sizeMultiplier()) == 10,
                "BASE 在一档剩余不足时延长了潮湿");
        check(PrisonRuntime.replacementDuration(50, 400, baseline.sizeMultiplier()) == 50, "BASE 仍按倍率延长了潮湿");
        check(PrisonRuntime.replacementDuration(1000, 0, doubled.sizeMultiplier()) == 1000, "失败的非正牢寿命仍要求改潮湿");
        check(PrisonRuntime.replacementDuration(1000, -5, quadrupled.sizeMultiplier()) == 1000, "负牢寿命仍要求改潮湿");
        int firstWrite = PrisonRuntime.replacementDuration(1000, tierFour, doubled.sizeMultiplier());
        int secondWrite = PrisonRuntime.replacementDuration(firstWrite, tierFour, doubled.sizeMultiplier());
        check(firstWrite == 2001 && secondWrite == firstWrite, "同一牢寿命重试又把支持期再加了一拍");
        check(PrisonRuntime.supportingDuration(Integer.MAX_VALUE) == Integer.MAX_VALUE, "int 上界又加了一拍");
        check(PrisonRuntime.supportingDuration(Integer.MAX_VALUE - 1) == Integer.MAX_VALUE, "上界前一拍没有饱和到上界");
        check(PrisonRuntime.replacementDuration(10, Integer.MAX_VALUE, doubled.sizeMultiplier()) == Integer.MAX_VALUE,
                "最大牢寿命的支持期不是 int 上界");
        check(PrisonRuntime.replacementDuration(Integer.MAX_VALUE, Integer.MAX_VALUE, doubled.sizeMultiplier()) == Integer.MAX_VALUE,
                "已经盖住上界时仍要求再写一次");
    }

    private static void expiryAddition() {
        check(ReleaseInput.expiryGameTime(100L, 20L) == 120L, "有限寿命的到期点不是相加");
        check(ReleaseInput.expiryGameTime(Long.MAX_VALUE - 3L, 10L) == Long.MAX_VALUE, "长寿命相加没有停在 long 上界");
    }

    private static boolean sameScale(AABB body, AABB unscaled, AABB scaled, double factor) {
        double centerX = (body.minX + body.maxX) * 0.5D;
        double centerY = (body.minY + body.maxY) * 0.5D;
        double centerZ = (body.minZ + body.maxZ) * 0.5D;
        return near(scaled.minX, centerX + (unscaled.minX - centerX) * factor)
                && near(scaled.minY, centerY + (unscaled.minY - centerY) * factor)
                && near(scaled.minZ, centerZ + (unscaled.minZ - centerZ) * factor)
                && near(scaled.maxX, centerX + (unscaled.maxX - centerX) * factor)
                && near(scaled.maxY, centerY + (unscaled.maxY - centerY) * factor)
                && near(scaled.maxZ, centerZ + (unscaled.maxZ - centerZ) * factor);
    }

    private static boolean sameBox(AABB left, AABB right) {
        return left != null && right != null
                && near(left.minX, right.minX) && near(left.minY, right.minY) && near(left.minZ, right.minZ)
                && near(left.maxX, right.maxX) && near(left.maxY, right.maxY) && near(left.maxZ, right.maxZ);
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0E-6D;
    }

    private static void check(boolean passed, String message) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}
