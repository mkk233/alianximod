package com.bslx.XueXiMod.shu.technique.release.entity;

import java.util.HashSet;
import java.util.UUID;

/**
 * 不启动 Minecraft。只跑父令牌、宿主号和回执门闩。
 * 实体生成、魔力收据和会话调度依赖服务端对象，这里不跑。
 */
public final class HostCastTokenCheck {
    public static void main(String[] arguments) {
        HostCast.closeAll();
        sameOwnerSameMomentUsesDifferentTokens();
        copiesShareOneHostAndNotifyOnce();
        firstFailureDoesNotBlockSecond();
        repeatAndLateCallbackDoNotSpawn();
        sessionIdentityStaysSplit();
        holdKeepsLaterVolleyOpen();
        logoutAndStopCloseOnlyTheRightCasts();
        HostCast.closeAll();
        System.out.println("宿主令牌检查通过");
    }

    private static void sameOwnerSameMomentUsesDifferentTokens() {
        UUID ownerId = UUID.randomUUID();
        HostCast leftCast = HostCast.open(ownerId);
        HostCast rightCast = HostCast.open(ownerId);
        UUID leftHost = leftCast.hostId(0, 0);
        UUID rightHost = rightCast.hostId(0, 0);
        check(!leftCast.tokenId().equals(rightCast.tokenId()), "两次独立释放必须各有父令牌");
        check(!leftHost.equals(rightHost), "同一主人、同一成员、同一波次也不能共用宿主号");
        check(leftHost.equals(leftCast.hostId(0, 0)), "同一次重试必须复用宿主号");
        check(!leftCast.hostId(0, 0).equals(leftCast.hostId(0, 1)), "不同波次不能共用宿主号");
        check(!leftCast.hostId(0, 0).equals(leftCast.hostId(1, 0)), "不同成员不能共用宿主号");
        HashSet<UUID> seenHosts = new HashSet<>();
        for (int memberIndex = 0; memberIndex < 4; memberIndex++) {
            for (int volley = 0; volley < 4; volley++) {
                check(seenHosts.add(leftCast.hostId(memberIndex, volley)), "成员和波次的组合必须唯一");
            }
        }
        leftCast.close();
        rightCast.close();
    }

    private static void copiesShareOneHostAndNotifyOnce() {
        HostCast cast = HostCast.open(UUID.randomUUID());
        UUID hostId = cast.hostId(0, 0);
        int[] spawnTimes = new int[1];
        int[] noteTimes = new int[1];
        HostGroupListener listener = receipt -> noteTimes[0] += 1;
        for (int copyIndex = 0; copyIndex < 3; copyIndex++) {
            check(cast.hostId(0, 0).equals(hostId), "复制份必须共用这一组");
            enterOnce(cast, hostId, spawnTimes, listener);
        }
        check(spawnTimes[0] == 1, "三份复制只能进入一次");
        check(noteTimes[0] == 1, "三份复制只能通知一次");
        HostGroupReceipt stored = cast.existing(hostId);
        check(stored != null && stored.committed(), "成功回执要留下来");
        check(!stored.notifyCommit(listener), "已经通知过的回执不能再通知");
        check(noteTimes[0] == 1, "重复通知不能增加次数");
        cast.close();
    }

    private static void firstFailureDoesNotBlockSecond() {
        UUID ownerId = UUID.randomUUID();
        HostCast failedCast = HostCast.open(ownerId);
        HostCast laterCast = HostCast.open(ownerId);
        UUID failedHost = failedCast.hostId(0, 0);
        UUID laterHost = laterCast.hostId(0, 0);
        HostGroupReceipt rejected = failedCast.remember(failedHost, HostGroupReceipt.rejected(failedHost, 4, "宿主组没有完整生成"));
        HostGroupReceipt kept = failedCast.remember(failedHost, HostGroupReceipt.committed(failedHost, 4));
        check(kept == rejected && !kept.committed(), "失败回执不能被后来的成功盖掉");
        check(laterCast.priorOrBlock(laterHost, 4) == null, "前一组失败不能挡住后一组");
        int[] spawnTimes = new int[1];
        int[] noteTimes = new int[1];
        enterOnce(laterCast, laterHost, spawnTimes, receipt -> noteTimes[0] += 1);
        check(spawnTimes[0] == 1 && noteTimes[0] == 1, "后一组仍要能单独成功");
        check(laterCast.priorOrBlock(failedHost, 4) == null, "后一组的账上不能出现前一组的宿主号");
        UUID secondVolley = failedCast.hostId(0, 1);
        check(failedCast.priorOrBlock(secondVolley, 4) == null, "同一父释放的下一波不能继承上一波的失败");
        failedCast.close();
        laterCast.close();
    }

    private static void repeatAndLateCallbackDoNotSpawn() {
        HostCast cast = HostCast.open(UUID.randomUUID());
        UUID hostId = cast.hostId(0, 0);
        int[] spawnTimes = new int[1];
        enterOnce(cast, hostId, spawnTimes, receipt -> {
        });
        enterOnce(cast, hostId, spawnTimes, receipt -> {
        });
        check(spawnTimes[0] == 1, "同组重复进入不能再生成");
        cast.close();
        UUID lateHost = cast.hostId(0, 1);
        int[] lateSpawns = new int[1];
        HostGroupReceipt lateReceipt = cast.priorOrBlock(lateHost, 9);
        if (lateReceipt == null) {
            lateSpawns[0] += 1;
        }
        HostGroupReceipt repeatReceipt = cast.priorOrBlock(lateHost, 11);
        check(lateSpawns[0] == 0, "父释放结束后不能再生成");
        check(lateReceipt != null && lateReceipt.kind() == HostGroupReceipt.Kind.CANCELLED, "迟到调用要留下终态");
        check(repeatReceipt == lateReceipt, "第二次迟到调用要交回同一张终态");
        check(!lateReceipt.notifyCommit(receipt -> lateSpawns[0] += 1), "终态取消不能通知成功");
        check(lateSpawns[0] == 0, "迟到调用不能记成一次提交");
    }

    private static void sessionIdentityStaysSplit() {
        UUID contactGroup = UUID.randomUUID();
        UUID otherContact = UUID.randomUUID();
        UUID memberZero = HostGroupIds.sessionHost(contactGroup, 0);
        UUID memberOne = HostGroupIds.sessionHost(contactGroup, 1);
        check(memberZero.equals(HostGroupIds.sessionHost(contactGroup, 0)), "会话宿主号要稳定");
        check(!memberZero.equals(memberOne), "会话的两名成员不能共用宿主号");
        check(!memberZero.equals(HostGroupIds.sessionHost(otherContact, 0)), "不同接触组不能共用宿主号");
    }

    private static void holdKeepsLaterVolleyOpen() {
        HostCast cast = HostCast.open(UUID.randomUUID());
        cast.hold();
        cast.hold();
        cast.release();
        check(!cast.closed(), "还有波次没回来时不能关掉父释放");
        check(cast.priorOrBlock(cast.hostId(0, 1), 2) == null, "父释放还开着时下一波可以进入");
        cast.release();
        check(cast.closed(), "最后一波结束要关掉父释放");
        check(cast.priorOrBlock(cast.hostId(0, 2), 3) != null, "关掉之后的波次不能再进入");
    }

    private static void logoutAndStopCloseOnlyTheRightCasts() {
        UUID ownerId = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        HostCast ownedCast = HostCast.open(ownerId);
        HostCast otherCast = HostCast.open(otherOwner);
        HostCast.closeOwned(ownerId);
        check(ownedCast.closed(), "登出要关掉这名玩家还开着的父释放");
        check(!otherCast.closed(), "登出不能关掉别人的父释放");
        HostCast.closeAll();
        check(otherCast.closed(), "停服要关掉剩余的父释放");
        check(ownedCast.priorOrBlock(ownedCast.hostId(0, 0), 1) != null, "关掉之后不能再进入");
    }

    private static void enterOnce(HostCast cast, UUID hostId, int[] spawnTimes, HostGroupListener listener) {
        HostGroupReceipt blocked = cast.priorOrBlock(hostId, 1);
        if (blocked != null) {
            return;
        }
        spawnTimes[0] += 1;
        HostGroupReceipt stored = cast.remember(hostId, HostGroupReceipt.committed(hostId, 1));
        stored.notifyCommit(listener);
    }

    private static void check(boolean passed, String label) {
        if (!passed) {
            throw new IllegalStateException(label);
        }
    }
}
