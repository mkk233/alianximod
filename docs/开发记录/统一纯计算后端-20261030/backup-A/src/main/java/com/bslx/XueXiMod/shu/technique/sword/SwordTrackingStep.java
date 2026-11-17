package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.shu.technique.release.motion.TrackingSupport;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 追踪剑本拍的方向和位移。先沿用现有转向上限，再按剩余夹角限制步长。
 * 远处对准时仍可走到基础速度；没有可用目标时不由这里伪造目标。
 */
public final class SwordTrackingStep {
    private static final double ALIGNED_TOLERANCE = 1.0E-4;
    private static final double ZERO_SPAN = 1.0E-8;
    private static final double APPROACH_FACTOR = 0.8;

    public final Vec3 direction;
    public final double stepLength;

    private SwordTrackingStep(Vec3 direction, double stepLength) {
        this.direction = direction;
        this.stepLength = stepLength;
    }

    public Vec3 displacement() {
        return this.direction.scale(this.stepLength);
    }

    /**
     * @param currentDirection 本拍开始时的飞行方向，可为零
     * @param towardTarget     指向目标中心的向量；零向量表示目标就在当前位置
     * @param baseSpeed        幻剑 2.5 或真剑 1.2，步长不会超过它
     */
    public static SwordTrackingStep plan(Vec3 currentDirection, Vec3 towardTarget, double baseSpeed) {
        double travelSpeed = finiteSpeed(baseSpeed);
        Vec3 heldDirection = unitOrForward(currentDirection);
        if (towardTarget == null || !finite(towardTarget) || towardTarget.lengthSqr() < ZERO_SPAN) {
            return new SwordTrackingStep(heldDirection, 0.0);
        }
        double distance = towardTarget.length();
        if (!Double.isFinite(distance) || distance < ZERO_SPAN) {
            return new SwordTrackingStep(heldDirection, 0.0);
        }
        Vec3 turned = TrackingSupport.turnToward(heldDirection, towardTarget);
        Vec3 nextDirection = unitOrForward(turned);
        Vec3 targetUnit = towardTarget.scale(1.0 / distance);
        double alignment = nextDirection.dot(targetUnit);
        if (!Double.isFinite(alignment)) {
            return new SwordTrackingStep(nextDirection, 0.0);
        }
        double remaining = Math.acos(clampUnit(alignment));
        if (!Double.isFinite(remaining) || remaining >= Math.PI * 0.5) {
            return new SwordTrackingStep(nextDirection, 0.0);
        }
        double stepLength;
        if (remaining <= ALIGNED_TOLERANCE) {
            stepLength = Math.min(travelSpeed, distance);
        } else {
            double turnSine = Math.sin(TrackingSupport.turnLimitRadians());
            double speedCap = Math.min(travelSpeed, APPROACH_FACTOR * distance * turnSine);
            double cosine = Math.cos(remaining);
            if (!Double.isFinite(cosine) || cosine < 0.0) {
                cosine = 0.0;
            }
            stepLength = speedCap * cosine;
        }
        if (!Double.isFinite(stepLength) || stepLength < 0.0) {
            stepLength = 0.0;
        }
        if (stepLength > travelSpeed) {
            stepLength = travelSpeed;
        }
        return new SwordTrackingStep(nextDirection, stepLength);
    }

    /** 清理完成后再决定是否留下归宗标记。不合格、未标记或已经命中则留空，交给原有自主选敌。 */
    public static UUID retainedMark(UUID markedId, boolean passesTrackingFilter, boolean alreadyStruck) {
        if (markedId == null || !passesTrackingFilter || alreadyStruck) {
            return null;
        }
        return markedId;
    }

    private static double finiteSpeed(double baseSpeed) {
        if (!Double.isFinite(baseSpeed) || baseSpeed <= 0.0) {
            return 0.0;
        }
        return baseSpeed;
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static double clampUnit(double alignment) {
        if (alignment > 1.0) {
            return 1.0;
        }
        if (alignment < -1.0) {
            return -1.0;
        }
        return alignment;
    }

    private static Vec3 unitOrForward(Vec3 vector) {
        if (vector == null || !finite(vector)) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double length = vector.length();
        if (!Double.isFinite(length) || length < ZERO_SPAN) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return vector.scale(1.0 / length);
    }
}
