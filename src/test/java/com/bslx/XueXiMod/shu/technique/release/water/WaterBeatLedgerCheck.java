package com.bslx.XueXiMod.shu.technique.release.water;

import com.bslx.XueXiMod.animation.runtime.CommitPermit;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupReceipt;

import java.util.UUID;

/**
 * 不启动 Minecraft。只跑拍号账、成功回执上的许可，以及三拍冻结帧的差别。
 * 宿主生成、动作运行时和游戏时钟依赖服务端，这里不跑。
 */
public final class WaterBeatLedgerCheck {
    public static void main(String[] arguments) {
        WaterBeatLedger.clearAll();
        windowEdge();
        failureLeavesWindow();
        carrierAndSlot();
        tripleAndDual();
        clearOnePlayer();
        permitStaysCached();
        framesStayDistinct();
        WaterBeatLedger.clearAll();
        System.out.println("水刃三拍检查通过");
    }

    private static void windowEdge() {
        UUID playerId = UUID.randomUUID();
        UUID firstGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 0, "iron", firstGroup, 100L) == 0, "首拍是左斜");
        check(WaterBeatLedger.commit(firstGroup, 100L), "首拍要能提交");
        UUID heldGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 0, "iron", heldGroup, 140L) == 1, "间隔恰好 40 仍衔接");
        WaterBeatLedger.abandon(heldGroup);
        UUID resetGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 0, "iron", resetGroup, 141L) == 0, "间隔 41 回左斜");
        WaterBeatLedger.abandon(resetGroup);
    }

    private static void failureLeavesWindow() {
        UUID playerId = UUID.randomUUID();
        UUID committedGroup = UUID.randomUUID();
        WaterBeatLedger.prepare(playerId, 1, "iron", committedGroup, 200L);
        check(WaterBeatLedger.commit(committedGroup, 200L), "失败用例要先有一次成功");
        UUID failedGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 1, "iron", failedGroup, 210L) == 1, "失败前看到下一拍");
        WaterBeatLedger.abandon(failedGroup);
        check(!WaterBeatLedger.commit(failedGroup, 210L), "丢掉的候选不能补记");
        UUID laterGroup = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 1, "iron", laterGroup, 220L) == 1, "失败不刷新窗口");
        WaterBeatLedger.abandon(laterGroup);
    }

    private static void carrierAndSlot() {
        UUID playerId = UUID.randomUUID();
        UUID swordGroup = UUID.randomUUID();
        WaterBeatLedger.prepare(playerId, 0, "iron", swordGroup, 300L);
        WaterBeatLedger.commit(swordGroup, 300L);
        UUID otherSlot = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 2, "iron", otherSlot, 305L) == 0, "另一槽从左斜开始");
        WaterBeatLedger.abandon(otherSlot);
        UUID swapped = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 0, "diamond", swapped, 310L) == 0, "换载体清回左斜");
        WaterBeatLedger.abandon(swapped);
        UUID returned = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 0, "iron", returned, 320L) == 0, "换回原来的载体也不接旧窗口");
        WaterBeatLedger.abandon(returned);
    }

    private static void tripleAndDual() {
        UUID playerId = UUID.randomUUID();
        UUID tripleGroup = UUID.randomUUID();
        int sharedBeat = WaterBeatLedger.prepare(playerId, 0, "iron", tripleGroup, 400L);
        check(WaterBeatLedger.prepare(playerId, 0, "iron", tripleGroup, 401L) == sharedBeat, "同组第二份不另算拍");
        check(WaterBeatLedger.prepare(playerId, 0, "iron", tripleGroup, 402L) == sharedBeat, "同组第三份不另算拍");
        check(WaterBeatLedger.commit(tripleGroup, 400L), "一分为三只提交一次");
        check(!WaterBeatLedger.commit(tripleGroup, 400L), "同组重复提交不推进");
        UUID afterTriple = UUID.randomUUID();
        check(WaterBeatLedger.prepare(playerId, 0, "iron", afterTriple, 405L) == 1, "三份之后只前进一拍");
        WaterBeatLedger.abandon(afterTriple);

        UUID memberLead = UUID.randomUUID();
        UUID memberLate = UUID.randomUUID();
        UUID memberRetry = UUID.randomUUID();
        WaterBeatLedger.prepare(playerId, 3, "iron", memberLead, 500L);
        WaterBeatLedger.commit(memberLead, 500L);
        check(WaterBeatLedger.prepare(playerId, 3, "iron", memberLate, 505L) == 1, "后一个成员在自己的提交点才看到下一拍");
        WaterBeatLedger.abandon(memberLate);
        check(WaterBeatLedger.prepare(playerId, 3, "iron", memberRetry, 506L) == 1, "后一个成员失败不预推进");
        WaterBeatLedger.abandon(memberRetry);
    }

    private static void clearOnePlayer() {
        UUID staying = UUID.randomUUID();
        UUID leaving = UUID.randomUUID();
        UUID stayingGroup = UUID.randomUUID();
        UUID leavingGroup = UUID.randomUUID();
        WaterBeatLedger.prepare(staying, 0, "iron", stayingGroup, 600L);
        WaterBeatLedger.commit(stayingGroup, 600L);
        WaterBeatLedger.prepare(leaving, 0, "iron", leavingGroup, 600L);
        WaterBeatLedger.commit(leavingGroup, 600L);
        WaterBeatLedger.clearPlayer(leaving);
        UUID stayingNext = UUID.randomUUID();
        UUID leavingNext = UUID.randomUUID();
        check(WaterBeatLedger.prepare(staying, 0, "iron", stayingNext, 610L) == 1, "清理别人不能清掉这一槽");
        check(WaterBeatLedger.prepare(leaving, 0, "iron", leavingNext, 610L) == 0, "死亡或换维后从左斜开始");
        WaterBeatLedger.abandon(stayingNext);
        WaterBeatLedger.abandon(leavingNext);
    }

    private static void permitStaysCached() {
        UUID ownerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        HostGroupReceipt committed = HostGroupReceipt.committed(groupId, 7);
        CommitPermit firstPermit = committed.commitPermit(ownerId);
        CommitPermit secondPermit = committed.commitPermit(ownerId);
        check(firstPermit != null && firstPermit == secondPermit, "同一张成功回执必须交回同一份许可");
        check(ownerId.equals(firstPermit.ownerId()) && groupId.equals(firstPermit.groupId()), "许可记下主人和宿主组");
        check(!firstPermit.consumed(), "构造许可本身还没消费");
        check(HostGroupReceipt.rejected(groupId, 7, "失败").commitPermit(ownerId) == null, "失败回执不发许可");
        check(HostGroupReceipt.cancelled(groupId, 7, "取消").commitPermit(ownerId) == null, "取消回执不发许可");
        check(committed.commitPermit(null) == null, "没有主人不发许可");
    }

    private static void framesStayDistinct() {
        ArcBandSnapshot probe = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0),
                new ArcVec(0.0, 0.0, 1.0),
                new ArcVec(1.0, 0.0, 0.0),
                new ArcVec(0.0, 1.0, 0.0),
                0, 2.4, 0.16, 0.0, 1.0, -1L, 6, 4);
        check(probe != null, "共享探针要能冻结");
        WaterBeatFrame copied = WaterBeatFrame.copy(probe);
        check(copied != null, "帧要能从快照抄出");
        check(near(copied.forwardEast(), 0.0) && near(copied.forwardAbove(), 0.0) && near(copied.forwardSouth(), 1.0),
                "前向抄的是 forwardAxis");
        check(near(copied.normalEast(), 0.0) && near(copied.normalAbove(), 1.0) && near(copied.normalSouth(), 0.0),
                "平面抄的是 normalAxis，不是 right");
        ArcVec right = new ArcVec(copied.normalEast(), copied.normalAbove(), copied.normalSouth())
                .cross(new ArcVec(copied.forwardEast(), copied.forwardAbove(), copied.forwardSouth()));
        check(near(right.east(), 1.0) && near(right.above(), 0.0) && near(right.south(), 0.0),
                "探针满足 R = N × F = (1, 0, 0)");

        ArcVec origin = new ArcVec(0.0, 1.0, 0.0);
        ArcVec forward = new ArcVec(0.0, 0.0, 1.0);
        ArcBandSnapshot leftBeat = ArcBandSnapshot.freezeLook(origin, forward, null, false, 0.0, 0, 2.4, 0.16, 1.0);
        ArcBandSnapshot rightBeat = ArcBandSnapshot.freezeLook(origin, forward, null, false, 0.0, 1, 2.4, 0.16, 1.0);
        ArcBandSnapshot flatBeat = ArcBandSnapshot.freezeLook(origin, forward, null, false, 0.0, 2, 2.4, 0.16, 1.0);
        check(leftBeat != null && rightBeat != null && flatBeat != null, "三拍都要能冻结");
        check(leftBeat.beat() == 0 && rightBeat.beat() == 1 && flatBeat.beat() == 2, "拍号要写进快照");
        check(leftBeat.startAngle() != rightBeat.startAngle() && rightBeat.startAngle() != flatBeat.startAngle(),
                "三拍起始角要不同");
        check(!sameDirection(leftBeat.normalAxis(), rightBeat.normalAxis())
                && !sameDirection(rightBeat.normalAxis(), flatBeat.normalAxis()), "三拍法线要不同");
        check(obeysFrame(leftBeat) && obeysFrame(rightBeat) && obeysFrame(flatBeat), "斜拍仍满足 F × R = N");
    }

    private static boolean obeysFrame(ArcBandSnapshot snapshot) {
        WaterBeatFrame copied = WaterBeatFrame.copy(snapshot);
        ArcVec forward = new ArcVec(copied.forwardEast(), copied.forwardAbove(), copied.forwardSouth());
        ArcVec normal = new ArcVec(copied.normalEast(), copied.normalAbove(), copied.normalSouth());
        ArcVec right = normal.cross(forward);
        ArcVec rebuilt = forward.cross(right);
        return near(rebuilt.east(), normal.east())
                && near(rebuilt.above(), normal.above())
                && near(rebuilt.south(), normal.south());
    }

    private static boolean sameDirection(ArcVec left, ArcVec right) {
        return near(left.east(), right.east()) && near(left.above(), right.above()) && near(left.south(), right.south());
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0E-6;
    }

    private static void check(boolean passed, String label) {
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
