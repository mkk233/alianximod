package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.client.GuardClientLifecycle;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.UUID;

/**
 * 护体同步的纯计算与编解码。不构造世界，也不走客户端运行时。
 */
public final class GuardSyncWireCheck {
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static int assertionCount;
    private static int failures;
    private static int passedSections;
    private static int failedSections;
    private static int skippedSections;
    private static String protocolOutcome = "UNEXECUTED";

    private GuardSyncWireCheck() {
    }

    public static void main(String[] arguments) {
        runCore("round-trip", GuardSyncWireCheck::roundTripBuffered);
        runCore("refresh-phase", GuardSyncWireCheck::refreshKeepsPhase);
        runCore("repeated-stale", GuardSyncWireCheck::repeatedAndStalePackets);
        runCore("duplicate-pose", GuardSyncWireCheck::duplicatePoseClockStays);
        runCore("newer-identity", GuardSyncWireCheck::newerIdentityReplacesHighRevision);
        runCore("late-join", GuardSyncWireCheck::lateJoinStartsAtReceivedAge);
        runCore("old-remove", GuardSyncWireCheck::oldRemoveLeavesNewerRing);
        runCore("world-clear", GuardSyncWireCheck::worldClearOnlyOnClient);
        runCore("non-negative-age", GuardSyncWireCheck::ageStaysNonNegative);
        try {
            GuardSyncPacket.ClientRings.clear();
        } catch (RuntimeException failure) {
            System.out.println("清理视图抛出 " + failure.getClass().getName());
        } catch (LinkageError failure) {
            System.out.println("清理视图抛出 " + failure.getClass().getName());
        }
        runProtocol();
        System.out.println("未执行客户端渲染、发包和世界卸载事件");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("PASS " + passedSections);
        System.out.println("FAIL " + failedSections);
        System.out.println("UNEXECUTED " + skippedSections);
        System.out.println("PROTOCOL " + protocolOutcome);
        if (failures != 0 || skippedSections != 0) {
            System.exit(1);
        }
    }

    private static void runCore(String section, Runnable action) {
        int failuresBefore = failures;
        try {
            action.run();
        } catch (LinkageError failure) {
            skippedSections++;
            System.out.println("UNEXECUTED " + section + " " + failure.getClass().getName());
            return;
        } catch (RuntimeException failure) {
            skippedSections++;
            check(false, section + " 抛出 " + failure.getClass().getName() + " " + failure.getMessage());
            System.out.println("UNEXECUTED " + section + " " + failure.getClass().getName());
            return;
        }
        if (failures > failuresBefore) {
            failedSections++;
            System.out.println("FAIL " + section);
            return;
        }
        passedSections++;
        System.out.println("PASS " + section);
    }

    private static void runProtocol() {
        int failuresBefore = failures;
        try {
            boolean matched = "5".equals(ModNetwork.protocolVersion());
            check(matched, "协议版本应为5且两端共用这一份");
            if (failures > failuresBefore) {
                protocolOutcome = "FAIL";
                System.out.println("FAIL protocol");
                return;
            }
            protocolOutcome = "PASS";
            System.out.println("PASS protocol");
        } catch (LinkageError failure) {
            protocolOutcome = "UNEXECUTED";
            System.out.println("UNEXECUTED protocol " + failure.getClass().getName());
        } catch (RuntimeException failure) {
            protocolOutcome = "UNEXECUTED";
            System.out.println("UNEXECUTED protocol " + failure.getClass().getName() + " " + failure.getMessage());
        }
    }

    private static void roundTripBuffered() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            roundTrip(buffer);
        } finally {
            buffer.release();
        }
    }

    private static void roundTrip(FriendlyByteBuf buffer) {
        GuardSyncPacket original = ring(GuardSyncPacket.FULL, 1200, 4, 6, 18000, 0.25f, 7);
        original.encode(buffer);
        GuardSyncPacket decoded = GuardSyncPacket.decode(buffer);
        check(original.matches(decoded), "编解码字段不一致");
        buffer.clear();
        GuardSyncPacket removal = ring(GuardSyncPacket.REMOVE, 1200, 4, 6, 18000, 0f, 0);
        removal.encode(buffer);
        check(removal.matches(GuardSyncPacket.decode(buffer)), "删除包编解码不一致");
    }

    private static void refreshKeepsPhase() {
        GuardSyncPacket.ClientRings.clear();
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 2, 1, 30, 0.2f, 3), 500L);
        GuardSyncPacket.ClientRings.View view = only("刷新前应有一环");
        if (view == null) {
            return;
        }
        check(near(view.animationAge(500L, 0f), 30.0), "新观察者按收到年龄起播");
        check(near(view.animationAge(510L, 0.25f), 40.25), "年龄加本地tick和partial");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 3, 1, 0, 0.8f, 3), 510L);
        check(view.revision == 3, "同一次激活的刷新写入新revision");
        check(view.animationStart == 100, "刷新不换激活身份");
        check(near(view.fromX, 0.8f), "刷新可以更新姿态");
        check(near(view.animationAge(510L, 0f), 40.0), "刷新年龄不归零");
        check(near(view.animationAge(512L, 0f), 42.0), "刷新后相位继续向前");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 100, 3, 2, 1, 0.4f, 2), 512L);
        check(near(view.fromX, 0.4f), "同身份姿态包可以摆动");
        check(near(view.animationAge(512L, 0f), 42.0), "重复姿态包不倒退");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 4, 1, 50, 0.6f, 1), 512L);
        check(near(view.animationAge(512L, 0f), 50.0), "更晚的年龄只向前校正");
        check(near(view.animationAge(513L, 0f), 51.0), "校正后继续增加");
    }

    private static void repeatedAndStalePackets() {
        GuardSyncPacket.ClientRings.clear();
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 2, 5, 10, 0.2f, 4), 20L);
        GuardSyncPacket.ClientRings.View view = only("乱序前应有一环");
        if (view == null) {
            return;
        }
        long poseClock = view.poseStartMillis;
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 100, 2, 4, 80, 0.9f, 1), 24L);
        check(view.sequence == 5 && near(view.fromX, 0.2f), "更小sequence的姿态包丢弃");
        check(view.poseStartMillis == poseClock, "丢弃的姿态包不重摆");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 100, 1, 30, 80, 0.7f, 1), 24L);
        check(view.revision == 2 && near(view.fromX, 0.2f), "更旧revision的姿态包丢弃");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 2, 3, 1, 0.1f, 8), 24L);
        check(view.sequence == 5 && near(view.fromX, 0.2f), "同revision更旧的快照丢弃");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 100, 2, 6, 1, 0.4f, 2), 24L);
        check(view.sequence == 6 && near(view.fromX, 0.4f), "更新的姿态包生效");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 5, 1, 1, 0.6f, 1), 24L);
        check(view.revision == 5 && view.sequence == 1 && near(view.fromX, 0.6f), "更高revision覆盖旧状态");
        check(near(view.animationAge(24L, 0f), 14.0), "乱序丢弃期间相位不倒退");
    }

    private static void duplicatePoseClockStays() {
        GuardSyncPacket.ClientRings.clear();
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 2, 5, 10, 0.2f, 4), 40L);
        GuardSyncPacket.ClientRings.View view = only("重复姿态前应有一环");
        if (view == null) {
            return;
        }
        view.poseStartMillis = 11L;
        float keptFrom = view.fromX;
        int keptPoseTicks = view.poseTicks;
        GuardSyncPacket.ClientRings.accept(sized(GuardSyncPacket.FULL, 100, 2, 5, 20, 0.9f, 8, 1.4f), 44L);
        check(view.poseStartMillis == 11L, "重复FULL保留姿态时钟");
        check(near(view.fromX, keptFrom) && view.poseTicks == keptPoseTicks, "重复FULL不重放姿态");
        check(near(view.radius, 1.4f), "重复FULL仍更新尺寸");
        check(near(view.animationAge(44L, 0f), 20.0), "重复FULL仍向前校正年龄");
        view.poseStartMillis = 11L;
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 100, 2, 5, 21, 0.3f, 1), 44L);
        check(view.poseStartMillis == 11L && near(view.fromX, keptFrom), "重复POSE保留姿态时钟");
        check(view.revision == 2 && view.sequence == 5, "重复POSE不改代际");
        view.poseStartMillis = 11L;
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 100, 2, 6, 21, 0.55f, 6), 45L);
        check(view.poseStartMillis != 11L, "合法新POSE更新姿态时钟");
        check(view.sequence == 6 && near(view.fromX, 0.55f) && view.poseTicks == 6, "合法新POSE写入新姿态");
        long updatedClock = view.poseStartMillis;
        float updatedFrom = view.fromX;
        int updatedRevision = view.revision;
        int updatedSequence = view.sequence;
        float updatedRadius = view.radius;
        double updatedAge = view.animationAge(46L, 0f);
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 100, 9, 1, 80, 0.1f, 2), 46L);
        check(view.poseStartMillis == updatedClock && near(view.fromX, updatedFrom), "高revision姿态包不重摆");
        check(view.revision == updatedRevision && view.sequence == updatedSequence, "高revision姿态包不改代际");
        check(near(view.radius, updatedRadius) && near(view.animationAge(46L, 0f), updatedAge), "高revision姿态包不改变视图");
    }

    private static void newerIdentityReplacesHighRevision() {
        GuardSyncPacket.ClientRings.clear();
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 50, 9, 10, 0.2f, 4), 1000L);
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 180, 1, 1, 4, 0.7f, 2), 1005L);
        GuardSyncPacket.ClientRings.View view = only("新身份应替换旧环");
        if (view == null) {
            return;
        }
        check(view.animationStart == 180 && view.revision == 1, "新激活不因旧高revision被拒绝");
        check(near(view.fromX, 0.7f), "新身份使用自己的姿态");
        check(near(view.animationAge(1005L, 0f), 4.0), "新身份从收到的年龄起播");
        check(near(view.animationAge(1008L, 0.5f), 7.5), "新身份随后用本地tick推进");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 100, 900, 1, 3, 0.1f, 6), 1008L);
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 100, 900, 8, 3, 0.15f, 1), 1008L);
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.POSE, 200, 1, 1, 3, 0.3f, 1), 1008L);
        check(GuardSyncPacket.ClientRings.all().size() == 1, "旧身份或尚无快照的姿态包不新建环");
        check(view.animationStart == 180 && view.revision == 1 && near(view.fromX, 0.7f), "旧激活不能重建或摆动新环");
    }

    private static void lateJoinStartsAtReceivedAge() {
        GuardSyncPacket.ClientRings.clear();
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 50, 1, 1, 80, 0.2f, 3), 9000L);
        GuardSyncPacket.ClientRings.View view = only("晚加入应有一环");
        if (view == null) {
            return;
        }
        check(near(view.animationAge(9000L, 0f), 80.0), "晚加入不从零起播");
        check(near(view.animationAge(9002L, 0.5f), 82.5), "晚加入之后加本地tick和partial");
    }

    private static void oldRemoveLeavesNewerRing() {
        GuardSyncPacket.ClientRings.clear();
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 180, 1, 4, 4, 0.7f, 2), 30L);
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.REMOVE, 100, 999, 1, 1, 0f, 0), 31L);
        check(GuardSyncPacket.ClientRings.all().size() == 1, "旧激活的删除不拆新环");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.REMOVE, 180, 0, 9, 1, 0f, 0), 31L);
        check(GuardSyncPacket.ClientRings.all().size() == 1, "revision落后的删除不生效");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.REMOVE, 180, 1, 3, 1, 0f, 0), 31L);
        check(GuardSyncPacket.ClientRings.all().size() == 1, "同revision更旧sequence的删除不生效");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.REMOVE, 180, 1, 4, 1, 0f, 0), 31L);
        check(GuardSyncPacket.ClientRings.all().isEmpty(), "相同身份且revision不落后时删除");
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 180, 2, 4, 8, 0.2f, 2), 32L);
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.REMOVE, 180, 3, 1, 1, 0f, 0), 33L);
        check(GuardSyncPacket.ClientRings.all().isEmpty(), "更新revision的删除可以拆掉当前环");
    }

    private static void worldClearOnlyOnClient() {
        GuardSyncPacket.ClientRings.clear();
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 10, 1, 1, 2, 0.2f, 1), 1L);
        GuardClientLifecycle.clearIfClientSide(false);
        check(GuardSyncPacket.ClientRings.all().size() == 1, "服务端世界卸载不清客户端环");
        GuardClientLifecycle.clearIfClientSide(true);
        check(GuardSyncPacket.ClientRings.all().isEmpty(), "客户端世界卸载清掉旧环");
    }

    private static void ageStaysNonNegative() {
        GuardSyncPacket.ClientRings.clear();
        GuardSyncPacket.ClientRings.accept(ring(GuardSyncPacket.FULL, 10, 1, 1, -5, 0.2f, 1), 40L);
        GuardSyncPacket.ClientRings.View view = only("负年龄也应留下环");
        if (view == null) {
            return;
        }
        check(near(view.animationAge(40L, 0f), 0.0), "负年龄钳到0");
        check(near(view.animationAge(39L, 0.5f), 0.5), "本地tick倒退时不出现负年龄");
        check(near(view.animationAge(41L, Float.NaN), 1.0), "非法partial不参与年龄");
    }

    private static GuardSyncPacket ring(int kind, int animationStart, int revision, int sequence,
                                        int animationAgeTicks, float fromX, int poseTicks) {
        return sized(kind, animationStart, revision, sequence, animationAgeTicks, fromX, poseTicks, 0.9f);
    }

    private static GuardSyncPacket sized(int kind, int animationStart, int revision, int sequence,
                                         int animationAgeTicks, float fromX, int poseTicks, float ringRadius) {
        return new GuardSyncPacket(kind, OWNER_ID, revision, sequence,
                240, 80, animationStart, animationAgeTicks,
                12f, ringRadius, 0.12f, 0.55f,
                fromX, 1f, 0f,
                0f, 1f, 0f,
                poseTicks);
    }

    private static GuardSyncPacket.ClientRings.View only(String label) {
        List<GuardSyncPacket.ClientRings.View> views = GuardSyncPacket.ClientRings.all();
        check(views.size() == 1, label);
        if (views.size() != 1) {
            return null;
        }
        return views.get(0);
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) <= 1.0E-6;
    }

    private static void check(boolean passed, String label) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + label);
        }
    }
}
