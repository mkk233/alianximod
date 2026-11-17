package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.shu.technique.release.motion.TrackingSupport;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 直接调用生产追踪步长。不启动 Minecraft，因此不覆盖归宗发射、方块裁剪和伤害。
 */
public final class SwordTrackingStepCheck {
    private static final double HALF_EAST = 0.3;
    private static final double HALF_ABOVE = 0.9;
    private static final double HALF_SOUTH = 0.3;

    public static void main(String[] arguments) {
        int cases = 0;
        cases += retainedMarkKeepsOnlyEligible();
        cases += turnLimitStaysTwelveDegrees();
        cases += boundedSamplesStayFinite();
        cases += stationaryTargetsCollide();
        cases += movingTargetStaysFinite();
        cases += yawTakesShortArc();
        cases += zeroLengthInsideBoxCounts();
        System.out.println("剑追踪步长检查通过 cases=" + cases);
        System.out.println("未运行：归宗实体发射、方块裁剪、真实 hurtAlong。障碍仍由 flyStep 先 clip 再结算。");
    }

    private static int retainedMarkKeepsOnlyEligible() {
        UUID markedId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        check(markedId.equals(SwordTrackingStep.retainedMark(markedId, true, false)), "清理后合格标记要留下");
        check(SwordTrackingStep.retainedMark(null, true, false) == null, "没有标记保持空槽");
        check(SwordTrackingStep.retainedMark(markedId, false, false) == null, "不合格标记不写入");
        check(SwordTrackingStep.retainedMark(markedId, true, true) == null, "已经命中的标记不留下");
        return 4;
    }

    private static int turnLimitStaysTwelveDegrees() {
        double turnLimit = TrackingSupport.turnLimitRadians();
        check(Math.abs(turnLimit - Math.toRadians(12.0)) < 1.0E-12, "转向上限仍是 12 度");
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);
        Vec3 slight = new Vec3(Math.sin(Math.toRadians(6.0)), 0.0, Math.cos(Math.toRadians(6.0)));
        Vec3 snapped = TrackingSupport.turnToward(forward, slight);
        check(snapped.distanceTo(slight) < 1.0E-6, "12 度以内仍直接对准");
        Vec3 side = new Vec3(1.0, 0.0, 0.0);
        Vec3 turned = TrackingSupport.turnToward(forward, side);
        check(Math.abs(turned.dot(forward) - Math.cos(turnLimit)) < 1.0E-6, "超过上限时只转 12 度");
        check(Math.abs(turned.length() - 1.0) < 1.0E-6, "转向结果仍是单位向量");
        Vec3 kept = TrackingSupport.turnToward(forward, null);
        check(kept.distanceTo(forward) < 1.0E-9, "没有目标方向时保持当前方向");
        return 5;
    }

    private static int boundedSamplesStayFinite() {
        SwordTrackingStep aligned = SwordTrackingStep.plan(new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 0.0, 15.0), 1.2);
        assertStep(aligned, 1.2);
        check(Math.abs(aligned.stepLength - 1.2) < 1.0E-9, "远处对准保持真剑基础速度");
        SwordTrackingStep phantom = SwordTrackingStep.plan(new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 0.0, 15.0), 2.5);
        assertStep(phantom, 2.5);
        check(Math.abs(phantom.stepLength - 2.5) < 1.0E-9, "远处对准保持幻剑基础速度");
        SwordTrackingStep close = SwordTrackingStep.plan(new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 0.0, 0.4), 2.5);
        assertStep(close, 2.5);
        check(Math.abs(close.stepLength - 0.4) < 1.0E-9, "对准时步长不超过到中心的距离");
        Vec3 offset = new Vec3(Math.sin(Math.toRadians(30.0)), 0.0, Math.cos(Math.toRadians(30.0))).scale(5.0);
        SwordTrackingStep partial = SwordTrackingStep.plan(new Vec3(0.0, 0.0, 1.0), offset, 2.5);
        assertStep(partial, 2.5);
        double cap = Math.min(2.5, 0.8 * 5.0 * Math.sin(Math.toRadians(12.0))) * Math.cos(Math.toRadians(18.0));
        check(partial.stepLength > 0.0 && partial.stepLength <= cap + 1.0E-6, "30 度步长受剩余夹角约束");
        SwordTrackingStep reversed = SwordTrackingStep.plan(new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 0.0, -5.0), 2.5);
        assertStep(reversed, 2.5);
        check(reversed.stepLength == 0.0, "剩余夹角大于等于 90 度时不前移");
        check(Math.abs(reversed.direction.dot(new Vec3(0.0, 0.0, 1.0)) - Math.cos(Math.toRadians(12.0))) < 1.0E-5,
                "对向目标本拍只转 12 度");
        SwordTrackingStep sameSpot = SwordTrackingStep.plan(new Vec3(0.0, 1.0, 0.0), Vec3.ZERO, 2.5);
        assertStep(sameSpot, 2.5);
        check(sameSpot.stepLength == 0.0, "目标就在当前位置时步长为 0");
        check(sameSpot.direction.distanceTo(new Vec3(0.0, 1.0, 0.0)) < 1.0E-9, "零距离不改已有方向");
        SwordTrackingStep cold = SwordTrackingStep.plan(Vec3.ZERO, new Vec3(0.0, 3.0, 4.0), 1.2);
        assertStep(cold, 1.2);
        SwordTrackingStep broken = SwordTrackingStep.plan(new Vec3(0.0, 0.0, 1.0), new Vec3(Double.NaN, 0.0, 1.0), 2.5);
        assertStep(broken, 2.5);
        check(broken.stepLength == 0.0, "非有限目标向量不前移");
        SwordTrackingStep stalled = SwordTrackingStep.plan(new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 0.0, 3.0), Double.NEGATIVE_INFINITY);
        assertStep(stalled, 0.0);
        check(stalled.stepLength == 0.0, "无效基础速度步长为 0");
        return 12;
    }

    private static int stationaryTargetsCollide() {
        double[] distances = {0.1, 0.5, 2.0, 5.0, 15.0};
        double[] bearings = {0.0, 30.0, 90.0, 179.0, 180.0};
        Vec3[] approaches = {
                new Vec3(0.0, 0.0, 1.0),
                new Vec3(2.0, 4.0, 3.0).normalize(),
                new Vec3(0.0, 1.0, 0.0)
        };
        String[] labels = {"水平", "斜向", "竖直"};
        int cases = 0;
        for (int approachIndex = 0; approachIndex < approaches.length; approachIndex++) {
            Vec3 towardUnit = approaches[approachIndex];
            for (double distance : distances) {
                for (double bearing : bearings) {
                    requireHit(labels[approachIndex] + " 真剑", towardUnit, distance, bearing, 1.2, 59, false);
                    requireHit(labels[approachIndex] + " 幻剑", towardUnit, distance, bearing, 2.5, 80, true);
                    cases += 2;
                }
            }
        }
        return cases;
    }

    private static int movingTargetStaysFinite() {
        Vec3 position = Vec3.ZERO;
        Vec3 direction = new Vec3(1.0, 0.0, 0.0);
        Vec3 target = new Vec3(0.0, 0.0, 5.0);
        Vec3 targetStep = new Vec3(0.2, 0.05, 0.0);
        double speed = 2.5;
        double largestGap = position.distanceTo(target);
        for (int tick = 1; tick <= 80; tick++) {
            SwordTrackingStep planned = SwordTrackingStep.plan(direction, target.subtract(position), speed);
            assertStep(planned, speed);
            Vec3 nextPosition = position.add(planned.displacement());
            double jumped = position.distanceTo(nextPosition);
            if (jumped > speed + 1.0E-6) {
                throw new IllegalStateException("移动目标跳位 " + jumped);
            }
            position = nextPosition;
            direction = planned.direction;
            target = target.add(targetStep);
            double gap = position.distanceTo(target);
            if (gap > largestGap) {
                largestGap = gap;
            }
        }
        check(Double.isFinite(largestGap) && largestGap < 12.0, "移动目标间距被拉开到 " + largestGap);
        System.out.println("移动目标最大间距 " + largestGap + " 末距 " + position.distanceTo(target));
        return 1;
    }

    /** 同一套包围盒：点在盒内算接触，零长度落在盒外不算。不是 hurtAlong 的放大盒。 */
    private static int zeroLengthInsideBoxCounts() {
        Vec3 origin = Vec3.ZERO;
        check(segmentHits(origin, origin, origin), "零长度且起点在目标盒内算接触");
        check(!segmentHits(origin, origin, new Vec3(5.0, 0.0, 0.0)), "零长度且在盒外不算命中");
        return 2;
    }

    private static int yawTakesShortArc() {
        float ahead = Mth.rotLerp(1.0F, 179.0F, -179.0F);
        float back = Mth.rotLerp(1.0F, -179.0F, 179.0F);
        float halfway = Mth.rotLerp(0.5F, 179.0F, -179.0F);
        float longArc = Mth.lerp(0.5F, 179.0F, -179.0F);
        check(Math.abs(ahead - 181.0F) < 1.0E-3F, "179 到 -179 走短弧");
        check(Math.abs(back + 181.0F) < 1.0E-3F, "-179 到 179 走短弧");
        check(Math.abs(halfway - 180.0F) < 1.0E-3F, "半拍落在短弧");
        check(Math.abs(longArc) < 1.0E-3F, "普通 lerp 对照仍是长弧");
        return 4;
    }

    private static void requireHit(String label, Vec3 towardUnit, double distance, double bearing,
                                   double speed, int tickBudget, boolean limitFlown) {
        Pursuit pursuit = pursue(towardUnit, distance, bearing, speed, tickBudget, limitFlown);
        if (!pursuit.struck) {
            throw new IllegalStateException(label + " 距离 " + distance + " 偏角 " + bearing
                    + " 未在寿命内碰到包围盒 ticks=" + pursuit.ticks
                    + " flown=" + pursuit.flown + " gap=" + pursuit.finalGap);
        }
    }

    private static Pursuit pursue(Vec3 towardUnit, double distance, double bearing, double speed,
                                  int tickBudget, boolean limitFlown) {
        Vec3 target = towardUnit.scale(distance);
        Vec3 position = Vec3.ZERO;
        Vec3 direction = rotateAround(towardUnit, bearing);
        double flown = 0.0;
        double finalGap = distance;
        for (int tick = 1; tick <= tickBudget; tick++) {
            SwordTrackingStep planned = SwordTrackingStep.plan(direction, target.subtract(position), speed);
            assertStep(planned, speed);
            direction = planned.direction;
            if (planned.stepLength <= 1.0E-8) {
                finalGap = position.distanceTo(target);
                if (segmentHits(position, position, target)) {
                    return new Pursuit(true, tick, flown, finalGap);
                }
                continue;
            }
            Vec3 nextPosition = position.add(planned.displacement());
            double traveled = position.distanceTo(nextPosition);
            if (!(traveled <= speed + 1.0E-6) || !Double.isFinite(nextPosition.x)
                    || !Double.isFinite(nextPosition.y) || !Double.isFinite(nextPosition.z)) {
                throw new IllegalStateException("轨迹步长或坐标无效");
            }
            flown += traveled;
            boolean struck = segmentHits(position, nextPosition, target);
            position = nextPosition;
            finalGap = position.distanceTo(target);
            if (struck) {
                return new Pursuit(true, tick, flown, finalGap);
            }
            if (limitFlown && flown >= 64.0) {
                return new Pursuit(false, tick, flown, finalGap);
            }
        }
        return new Pursuit(false, tickBudget, flown, finalGap);
    }

    private static Vec3 rotateAround(Vec3 forward, double degrees) {
        Vec3 helper = Math.abs(forward.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 axis = forward.cross(helper).normalize();
        double radians = Math.toRadians(degrees);
        return forward.scale(Math.cos(radians)).add(axis.cross(forward).scale(Math.sin(radians)));
    }

    private static boolean segmentHits(Vec3 start, Vec3 stop, Vec3 center) {
        double[] span = new double[] {0.0, 1.0};
        if (!overlaps(start.x, stop.x - start.x, center.x - HALF_EAST, center.x + HALF_EAST, span)) {
            return false;
        }
        if (!overlaps(start.y, stop.y - start.y, center.y - HALF_ABOVE, center.y + HALF_ABOVE, span)) {
            return false;
        }
        return overlaps(start.z, stop.z - start.z, center.z - HALF_SOUTH, center.z + HALF_SOUTH, span);
    }

    private static boolean overlaps(double origin, double delta, double low, double high, double[] span) {
        if (Math.abs(delta) < 1.0E-12) {
            return origin >= low && origin <= high;
        }
        double near = (low - origin) / delta;
        double far = (high - origin) / delta;
        if (near > far) {
            double swap = near;
            near = far;
            far = swap;
        }
        if (near > span[0]) {
            span[0] = near;
        }
        if (far < span[1]) {
            span[1] = far;
        }
        return span[0] <= span[1];
    }

    private static void assertStep(SwordTrackingStep planned, double speed) {
        Vec3 direction = planned.direction;
        if (!Double.isFinite(direction.x) || !Double.isFinite(direction.y) || !Double.isFinite(direction.z)) {
            throw new IllegalStateException("方向不是有限数");
        }
        double length = direction.length();
        if (Math.abs(length - 1.0) > 1.0E-6) {
            throw new IllegalStateException("方向长度 " + length);
        }
        if (!Double.isFinite(planned.stepLength) || planned.stepLength < -1.0E-12 || planned.stepLength > speed + 1.0E-9) {
            throw new IllegalStateException("步长 " + planned.stepLength + " 基础速度 " + speed);
        }
        double traveled = planned.displacement().length();
        if (Math.abs(traveled - planned.stepLength) > 1.0E-6) {
            throw new IllegalStateException("位移与步长不一致");
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) {
            throw new IllegalStateException(label);
        }
    }

    private static final class Pursuit {
        final boolean struck;
        final int ticks;
        final double flown;
        final double finalGap;

        Pursuit(boolean struck, int ticks, double flown, double finalGap) {
            this.struck = struck;
            this.ticks = ticks;
            this.flown = flown;
            this.finalGap = finalGap;
        }
    }
}
