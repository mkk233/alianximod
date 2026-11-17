package com.bslx.XueXiMod.shu.technique.sword;

import com.bslx.XueXiMod.shu.compute.ComputeCalls;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 追踪剑本拍的方向和位移。先沿用现有转向上限，再按剩余夹角限制步长。
 * 远处对准时仍可走到基础速度；没有可用目标时不由这里伪造目标。
 * 转向和步长公式在 JavaReference.trackingStep。
 */
public final class SwordTrackingStep {
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
        ComputeCalls.trackingStep(currentDirection, towardTarget, baseSpeed);
        return new SwordTrackingStep(ComputeCalls.trackingDirection(), ComputeCalls.trackingStepLength());
    }

    /** 清理完成后再决定是否留下归宗标记。不合格、未标记或已经命中则留空，交给原有自主选敌。 */
    public static UUID retainedMark(UUID markedId, boolean passesTrackingFilter, boolean alreadyStruck) {
        if (markedId == null || !passesTrackingFilter || alreadyStruck) {
            return null;
        }
        return markedId;
    }
}
