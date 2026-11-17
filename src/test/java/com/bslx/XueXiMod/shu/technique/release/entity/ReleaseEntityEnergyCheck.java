package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.client.WaterCurtainLayout;
import com.bslx.XueXiMod.client.WaterCurtainLayout.Panel;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.WaterSegment;
import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;

import java.util.List;

/**
 * 命中实例寿命和垂丝相位。直接调用生产方法，不复制公式，不生成实体。
 */
public final class ReleaseEntityEnergyCheck {
    private static final double FOOT = 5.62D;
    private static int assertionCount;
    private static int failures;

    private ReleaseEntityEnergyCheck() {
    }

    public static void main(String[] arguments) {
        productionLifetimes();
        templateStaysPut();
        weepingPhases();
        curtainClock();
        System.out.println("EXECUTED entity-lifetime-weeping-phase");
        System.out.println("未执行 实体 tick、NBT 往返和客户端渲染：需要世界或实体");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void productionLifetimes() {
        ReleaseInput baseline = ReleaseInput.BASE;
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        check(TechniqueHitSpec.instanceLifetime(40, baseline, false) == 40, "基准 40 被改短或拉长");
        check(TechniqueHitSpec.instanceLifetime(40, doubled, false) == 80, "两倍没有把 40 延长到 80");
        check(TechniqueHitSpec.instanceLifetime(40, quadrupled, false) == 160, "四倍没有把 40 延长到 160");
        check(TechniqueHitSpec.instanceLifetime(20, baseline, false) == 20, "默认 20 没有保持");
        check(TechniqueHitSpec.instanceLifetime(20, doubled, false) == 40, "两倍没有把 20 延长到 40");
        check(TechniqueHitSpec.instanceLifetime(20, quadrupled, false) == 80, "四倍没有把 20 延长到 80");
        check(TechniqueHitSpec.instanceLifetime(40, quadrupled, true) == 40, "弧带窗口被寿命倍率改了");
        check(TechniqueHitSpec.instanceLifetime(0, quadrupled, false) == 0, "零寿命被抬成正数");
        check(TechniqueHitSpec.instanceLifetime(-4, quadrupled, false) == -4, "负寿命被改写");
    }

    private static void templateStaysPut() {
        TechniqueHitSpec template = new TechniqueHitSpec();
        template.lifetime = 40;
        template.hitInterval = 10;
        template.hitReadyAge = 3;
        template.speed = 1.25D;
        ReleaseInput doubled = new ReleaseInput(2f);
        int firstPass = TechniqueHitSpec.instanceLifetime(template.lifetime, doubled, false);
        int secondPass = TechniqueHitSpec.instanceLifetime(template.lifetime, doubled, false);
        check(firstPass == 80 && secondPass == 80, "同一模板第二次被乘成 160，实际 " + firstPass + " " + secondPass);
        check(template.lifetime == 40, "模板寿命被写回");
        check(template.hitInterval == 10 && template.hitReadyAge == 3, "命中间隔或就绪年龄被带进寿命缩放");
        check(Math.abs(template.speed - 1.25D) < 1.0E-9D, "速度被寿命缩放改写");
    }

    private static void weepingPhases() {
        check(WeepingSettings.FILL_TICKS == 8, "填充不再是 8");
        check(WeepingSettings.drainSpan() == 8, "排空窗口不再是 8");
        check(WeepingSettings.LIFETIME == 40 && WeepingSettings.DRAIN_START == 32, "基准寿命或基准排空起点变了");
        checkPhase(40, 32);
        checkPhase(80, 72);
        checkPhase(160, 152);
        net.minecraft.world.phys.Vec3 lip = new net.minecraft.world.phys.Vec3(3.0D, 10.0D, 4.0D);
        net.minecraft.world.phys.Vec3 look = new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D);
        WaterSegment legacy = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 36);
        WaterSegment explicit = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 36, 40);
        check(Math.abs(legacy.upperDrop() - explicit.upperDrop()) < 1.0E-9D, "缺省相位不再等于寿命 40");
        WaterSegment held = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 40, 80);
        check(held.present() && Math.abs(held.upperDrop()) < 1.0E-9D, "80 拍寿命在第 40 拍已经开始排空");
        WaterSegment lateHold = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 80, 160);
        check(lateHold.present() && Math.abs(lateHold.upperDrop()) < 1.0E-9D, "160 拍寿命在第 80 拍已经开始排空");
        WeepingCurtain.FootClip landed = new WeepingCurtain.FootClip(FOOT, true, false, false, 1L);
        WaterSegment full = WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, 8, 160);
        check(WeepingCurtain.splashes(8, landed, full), "溅水没有留在第 8 拍");
        check(!WeepingCurtain.splashes(16, landed, full), "长寿命把溅水推迟了");
    }

    private static void checkPhase(int lifeTicks, int drainStart) {
        net.minecraft.world.phys.Vec3 lip = new net.minecraft.world.phys.Vec3(3.0D, 10.0D, 4.0D);
        net.minecraft.world.phys.Vec3 look = new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D);
        check(WeepingSettings.drainStartFor(lifeTicks) == drainStart, lifeTicks + " 的排空起点不是 " + drainStart);
        check(WaterCurtainLayout.drainStart(lifeTicks) == drainStart, "客户端排空起点和水体不一致");
        WaterSegment closed = flowing(lip, look, 0, lifeTicks);
        check(!closed.present(), lifeTicks + " 在 0 拍就有水");
        WaterSegment growing = flowing(lip, look, 4, lifeTicks);
        check(Math.abs(growing.upperDrop()) < 1.0E-9D, lifeTicks + " 填充时上沿离开了唇口");
        check(Math.abs(growing.lowerDrop() - FOOT * 0.5D) < 1.0E-9D, lifeTicks + " 第 4 拍没有走到一半");
        WaterSegment full = flowing(lip, look, 8, lifeTicks);
        check(full.present() && Math.abs(full.lowerDrop() - FOOT) < 1.0E-9D && Math.abs(full.upperDrop()) < 1.0E-9D,
                lifeTicks + " 第 8 拍不是满流");
        WaterSegment held = flowing(lip, look, drainStart, lifeTicks);
        check(held.present() && Math.abs(held.upperDrop()) < 1.0E-9D && Math.abs(held.lowerDrop() - FOOT) < 1.0E-9D,
                lifeTicks + " 在排空起点已经收水");
        WaterSegment draining = flowing(lip, look, drainStart + 4, lifeTicks);
        check(draining.present() && Math.abs(draining.upperDrop() - FOOT * 0.5D) < 1.0E-9D,
                lifeTicks + " 排空过半时上沿不对");
        check(Math.abs(draining.lowerDrop() - FOOT) < 1.0E-9D, lifeTicks + " 排空时下沿离开了足端");
        WaterSegment ended = flowing(lip, look, lifeTicks, lifeTicks);
        check(!ended.present(), lifeTicks + " 到期后还留着水");
    }

    private static WaterSegment flowing(net.minecraft.world.phys.Vec3 lip, net.minecraft.world.phys.Vec3 look,
                                        int age, int lifeTicks) {
        return WeepingCurtain.flowSegment(lip, look, FOOT, true, 0.5D, 0.35D, age, lifeTicks);
    }

    private static void curtainClock() {
        check(WaterCurtainLayout.flowFrame(0, 0.0F, 0) == 0, "0 拍贴图不是第 0 帧");
        check(WaterCurtainLayout.flowFrame(2, 0.0F, 0) == 1, "贴图不再是每 2 拍一帧");
        check(WaterCurtainLayout.flowFrame(4, 0.0F, 0) == 2, "第 4 拍没有落到第 2 帧");
        int drainStart = WaterCurtainLayout.drainStart(160);
        List<Panel> phased = WaterCurtainLayout.panels(1.0D, 2.0D, 0, drainStart);
        List<Panel> plain = WaterCurtainLayout.panels(1.0D, 2.0D, 0);
        check(!phased.isEmpty() && phased.size() == plain.size(), "正常排空起点把水帘分件清空了");
        check(WaterCurtainLayout.panels(1.0D, 2.0D, 0, Integer.MAX_VALUE).isEmpty(), "哨兵排空起点仍在画分件");
    }

    private static void check(boolean passed, String message) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}
