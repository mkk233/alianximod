package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import org.jetbrains.annotations.Nullable;

/**
 * 合同中的动作时间线。旧名 ActionTimelineSpec 指这一类型。
 * 回旋使用 IN_PLACE。立即斩击的前摇固定为 0，收势使用结构常数 8 刻。
 */
public record TimelineSpec(int windupTicks, int recoveryTicks, MovementPolicy movementPolicy) {
    public TimelineSpec {
        if (movementPolicy == null) {
            throw new IllegalArgumentException("时间线缺少移动声明");
        }
        if (windupTicks < 0 || windupTicks > CooperativeStructureLimits.WINDUP_HIGH) {
            throw new IllegalArgumentException("前摇刻数超出结构范围");
        }
        if (recoveryTicks < CooperativeStructureLimits.RECOVERY_LOW
                || recoveryTicks > CooperativeStructureLimits.RECOVERY_HIGH) {
            throw new IllegalArgumentException("收势刻数超出结构范围");
        }
    }

    public int releaseTick() {
        return windupTicks;
    }

    public int endTickOffset() {
        return windupTicks + recoveryTicks;
    }
}
