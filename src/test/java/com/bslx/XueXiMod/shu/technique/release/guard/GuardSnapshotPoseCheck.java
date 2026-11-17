package com.bslx.XueXiMod.shu.technique.release.guard;

import net.minecraft.world.phys.Vec3;

/**
 * FULL 快照只读取当前插值，不依赖世界。服务端姿态字段本身不在这里改。
 */
public final class GuardSnapshotPoseCheck {
    private static int assertionCount;
    private static int failures;

    private GuardSnapshotPoseCheck() {
    }

    public static void main(String[] arguments) {
        try {
            check(GuardStates.animationAgeTicks(150, 100) == 50, "年龄是当前tick减去激活tick");
            check(GuardStates.animationAgeTicks(100, 100) == 0, "刚激活年龄为0");
            check(GuardStates.animationAgeTicks(90, 100) == 0, "tick倒退时年龄钳到0");
            Vec3 fromNormal = new Vec3(0.0, 1.0, 0.0);
            Vec3 towardNormal = new Vec3(1.0, 0.0, 0.0);
            GuardStates.SnapshotPose started = GuardStates.snapshotPose(fromNormal, towardNormal, 100, 110, 100);
            check(started.poseTicks == 10, "过渡刚开始时剩余是整段");
            check(near(started.from.x, 0.0) && near(started.from.y, 1.0) && near(started.from.z, 0.0), "起点快照仍是当前法线");
            check(same(started.toward, towardNormal), "起点目标仍是当前toward");
            GuardStates.SnapshotPose midway = GuardStates.snapshotPose(fromNormal, towardNormal, 100, 110, 105);
            double unit = Math.sqrt(0.5);
            check(midway.poseTicks == 5, "中途剩余是结束tick减当前tick");
            check(near(midway.from.x, unit) && near(midway.from.y, unit) && near(midway.from.z, 0.0), "中途from是当前插值法线");
            check(same(midway.toward, towardNormal), "中途toward保持当前目标");
            GuardStates.SnapshotPose finished = GuardStates.snapshotPose(fromNormal, towardNormal, 100, 110, 110);
            check(finished.poseTicks == 0, "完成后剩余为0");
            check(same(finished.from, finished.toward), "完成后from和toward相同");
            check(same(finished.toward, towardNormal), "完成后停在当前目标");
            GuardStates.SnapshotPose idle = GuardStates.snapshotPose(fromNormal, fromNormal, 40, 40, 80);
            check(idle.poseTicks == 0 && same(idle.from, idle.toward) && same(idle.from, fromNormal), "无过渡时两端相同");
            GuardStates.SnapshotPose empty = GuardStates.snapshotPose(null, null, 8, 8, 8);
            Vec3 upward = new Vec3(0.0, 1.0, 0.0);
            check(empty.poseTicks == 0 && same(empty.from, empty.toward) && same(empty.from, upward), "空法线完成时用世界上方");
            System.out.println("EXECUTED guard-snapshot-pose");
        } catch (LinkageError failure) {
            System.out.println("UNEXECUTED guard-snapshot-pose 需要游戏引导 " + failure.getClass().getName());
        } catch (RuntimeException failure) {
            check(false, "姿态快照检查抛出 " + failure.getClass().getName() + " " + failure.getMessage());
        }
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static boolean same(Vec3 left, Vec3 right) {
        return left != null && right != null
                && near(left.x, right.x)
                && near(left.y, right.y)
                && near(left.z, right.z);
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
