package com.bslx.XueXiMod.shu.technique.release.water;

import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;

import java.util.UUID;

/**
 * 不启动 Minecraft。载体看栈引用和选中槽，不看物品注册名。
 * 同一个 tick 内没有观察的往返换剑，生产观察同样看不见，这里不把它算成已捕获。
 */
public final class CastSourceCheck {
    public static void main(String[] arguments) {
        WaterBeatLedger.clearAll();
        CarrierWatch.clearAll();
        HostCast.closeAll();
        sameModelResets();
        replacedStackResets();
        selectedSlotReturnResets();
        wearStaysOnSameStack();
        swapAfterPrepareRejects();
        repeatCommitDoesNotSkip();
        otherSlotDoesNotRewriteSession();
        failedOtherSlotLeavesSession();
        interleavedSessionsStaySplit();
        WaterBeatLedger.clearAll();
        CarrierWatch.clearAll();
        HostCast.closeAll();
        System.out.println("水刃载体来源检查通过");
    }

    private static void sameModelResets() {
        UUID playerId = UUID.randomUUID();
        BladeStand firstSword = new BladeStand();
        BladeStand secondSword = new BladeStand();
        CastSource firstSource = sourceOf(playerId, 0, firstSword, 0);
        UUID firstGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(playerId, firstSource, firstGroup, 100L) == 0, "同型号第一拍是左斜");
        check(WaterBeatLedger.commitHeld(firstGroup, 100L), "同型号第一拍要提交");
        CarrierWatch.observe(playerId, secondSword, 0);
        check(!CarrierWatch.allows(playerId, firstSource), "换成另一把同型号剑后，旧来源失效");
        CastSource secondSource = sourceOf(playerId, 0, secondSword, 0);
        UUID secondGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(playerId, secondSource, secondGroup, 114L) == 0, "同型号新栈在 40 内也回左斜");
        WaterBeatLedger.abandonHeld(secondGroup);
    }

    private static void replacedStackResets() {
        UUID playerId = UUID.randomUUID();
        BladeStand original = new BladeStand();
        CastSource originalSource = sourceOf(playerId, 1, original, 0);
        UUID committed = UUID.randomUUID();
        WaterBeatLedger.prepareHeld(playerId, originalSource, committed, 200L);
        WaterBeatLedger.commitHeld(committed, 200L);
        BladeStand replacement = new BladeStand();
        CarrierWatch.observe(playerId, replacement, 0);
        CastSource replaced = sourceOf(playerId, 1, replacement, 0);
        UUID nextGroup = UUID.randomUUID();
        check(replaced.selectedSlot() == 0, "同槽替换不改变选中槽");
        check(WaterBeatLedger.prepareHeld(playerId, replaced, nextGroup, 210L) == 0, "同槽换上一把新栈要重置");
        WaterBeatLedger.abandonHeld(nextGroup);
    }

    private static void selectedSlotReturnResets() {
        UUID playerId = UUID.randomUUID();
        BladeStand sword = new BladeStand();
        CastSource origin = sourceOf(playerId, 0, sword, 0);
        UUID committed = UUID.randomUUID();
        WaterBeatLedger.prepareHeld(playerId, origin, committed, 300L);
        WaterBeatLedger.commitHeld(committed, 300L);
        CarrierWatch.observe(playerId, sword, 1);
        CarrierWatch.observe(playerId, sword, 0);
        check(!CarrierWatch.allows(playerId, origin), "选中槽走开再回来后，旧代数失效");
        CastSource returned = sourceOf(playerId, 0, sword, 0);
        UUID nextGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(playerId, returned, nextGroup, 310L) == 0, "选中槽回来后从左斜开始");
        WaterBeatLedger.abandonHeld(nextGroup);
    }

    private static void wearStaysOnSameStack() {
        UUID playerId = UUID.randomUUID();
        BladeStand sword = new BladeStand();
        CastSource source = sourceOf(playerId, 0, sword, 0);
        UUID committed = UUID.randomUUID();
        WaterBeatLedger.prepareHeld(playerId, source, committed, 400L);
        WaterBeatLedger.commitHeld(committed, 400L);
        sword.wearCount = sword.wearCount + 3;
        int generation = CarrierWatch.observe(playerId, sword, 0);
        check(generation == source.generation(), "同一把栈的耐久变化不增加代数");
        check(CarrierWatch.allows(playerId, source), "耐久变化后旧来源仍然有效");
        UUID nextGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(playerId, source, nextGroup, 414L) == 1, "耐久变化在 40 内仍衔接");
        WaterBeatLedger.abandonHeld(nextGroup);
    }

    private static void swapAfterPrepareRejects() {
        UUID playerId = UUID.randomUUID();
        BladeStand firstSword = new BladeStand();
        BladeStand secondSword = new BladeStand();
        CastSource source = sourceOf(playerId, 0, firstSword, 0);
        UUID prepared = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(playerId, source, prepared, 500L) == 0, "激活前先准备候选");
        CarrierWatch.observe(playerId, secondSword, 0);
        check(!CarrierWatch.allows(playerId, source), "准备之后、激活之前换剑必须拒绝");
        check(!WaterBeatLedger.commitHeld(prepared, 500L), "失配不能把这一拍记到新剑上");
        CastSource fresh = sourceOf(playerId, 0, secondSword, 0);
        UUID restarted = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(playerId, fresh, restarted, 510L) == 0, "被拒绝的候选不推进下一把剑");
        WaterBeatLedger.abandonHeld(restarted);
    }

    private static void repeatCommitDoesNotSkip() {
        UUID playerId = UUID.randomUUID();
        BladeStand sword = new BladeStand();
        CastSource source = sourceOf(playerId, 0, sword, 0);
        UUID groupId = UUID.randomUUID();
        WaterBeatLedger.prepareHeld(playerId, source, groupId, 600L);
        check(WaterBeatLedger.commitHeld(groupId, 600L), "第一次提交成功");
        check(!WaterBeatLedger.commitHeld(groupId, 600L), "重复提交不能再推进");
        UUID nextGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(playerId, source, nextGroup, 610L) == 1, "重复提交之后只前进一拍");
        WaterBeatLedger.abandonHeld(nextGroup);
    }

    private static void otherSlotDoesNotRewriteSession() {
        UUID playerId = UUID.randomUUID();
        BladeStand sword = new BladeStand();
        CastSource slotOne = sourceOf(playerId, 1, sword, 0);
        HostCast session = HostCast.open(playerId, slotOne);
        UUID slotOneGroup = UUID.randomUUID();
        WaterBeatLedger.prepareHeld(playerId, session.source(), slotOneGroup, 700L);
        WaterBeatLedger.commitHeld(slotOneGroup, 700L);
        CastSource slotTwo = new CastSource(2, sword, 0, slotOne.generation());
        HostCast later = HostCast.open(playerId, slotTwo);
        UUID slotTwoGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(playerId, later.source(), slotTwoGroup, 710L) == 0, "槽 2 成功从自己的左斜开始");
        check(WaterBeatLedger.commitHeld(slotTwoGroup, 710L), "槽 2 可以单独提交");
        UUID slotOneNext = UUID.randomUUID();
        check(session.source() == slotOne, "槽 2 成功后，槽 1 会话仍是原来的父来源");
        check(WaterBeatLedger.prepareHeld(playerId, session.source(), slotOneNext, 720L) == 1, "槽 1 碰撞仍衔接槽 1");
        WaterBeatLedger.abandonHeld(slotOneNext);
        session.close();
        later.close();
    }

    private static void failedOtherSlotLeavesSession() {
        UUID playerId = UUID.randomUUID();
        BladeStand sword = new BladeStand();
        CastSource slotOne = sourceOf(playerId, 1, sword, 0);
        HostCast session = HostCast.open(playerId, slotOne);
        UUID slotOneGroup = UUID.randomUUID();
        WaterBeatLedger.prepareHeld(playerId, session.source(), slotOneGroup, 800L);
        WaterBeatLedger.commitHeld(slotOneGroup, 800L);
        CastSource slotTwo = new CastSource(2, sword, 0, slotOne.generation());
        HostCast failedCast = HostCast.open(playerId, slotTwo);
        UUID failedGroup = UUID.randomUUID();
        WaterBeatLedger.prepareHeld(playerId, failedCast.source(), failedGroup, 810L);
        WaterBeatLedger.abandonHeld(failedGroup);
        UUID slotOneNext = UUID.randomUUID();
        check(session.source().skillSlot() == 1, "失败的槽 2 不改写槽 1 会话");
        check(WaterBeatLedger.prepareHeld(playerId, session.source(), slotOneNext, 820L) == 1, "槽 2 失败后槽 1 碰撞仍是槽 1");
        WaterBeatLedger.abandonHeld(slotOneNext);
        session.close();
        failedCast.close();
    }

    private static void interleavedSessionsStaySplit() {
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        BladeStand firstSword = new BladeStand();
        BladeStand secondSword = new BladeStand();
        CastSource firstSource = sourceOf(firstPlayer, 0, firstSword, 0);
        CastSource secondSource = sourceOf(secondPlayer, 0, secondSword, 0);
        HostCast firstCast = HostCast.open(firstPlayer, firstSource);
        HostCast secondCast = HostCast.open(secondPlayer, secondSource);
        UUID firstGroup = UUID.randomUUID();
        WaterBeatLedger.prepareHeld(firstPlayer, firstCast.source(), firstGroup, 900L);
        WaterBeatLedger.commitHeld(firstGroup, 900L);
        UUID secondGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(secondPlayer, secondCast.source(), secondGroup, 905L) == 0, "另一名玩家从左斜开始");
        UUID firstNext = UUID.randomUUID();
        check(WaterBeatLedger.prepareHeld(firstPlayer, firstCast.source(), firstNext, 910L) == 1, "交错会话不改前一名玩家的拍");
        check(firstCast.source() != secondCast.source(), "两份父来源不是同一对象");
        WaterBeatLedger.abandonHeld(secondGroup);
        WaterBeatLedger.abandonHeld(firstNext);
        firstCast.close();
        secondCast.close();
    }

    private static CastSource sourceOf(UUID playerId, int skillSlot, Object stackRef, int selectedSlot) {
        int generation = CarrierWatch.observe(playerId, stackRef, selectedSlot);
        return new CastSource(skillSlot, stackRef, selectedSlot, generation);
    }

    private static void check(boolean passed, String label) {
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }

    private static final class BladeStand {
        private int wearCount;
    }
}
