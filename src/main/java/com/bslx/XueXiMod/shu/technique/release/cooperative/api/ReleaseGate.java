package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import org.jetbrains.annotations.Nullable;

/**
 * 一个族实际使用的门。
 * 与偏转服务里的私有嵌套类同名，但是不同的类型；消费者只引用本类型。
 * 接触门没有时间线，次数仍由原冲刺窗口决定。
 */
public record ReleaseGate(ReleaseGateKind kind, @Nullable TimelineSpec timeline) {
    public ReleaseGate {
        if (kind == null) {
            throw new IllegalArgumentException("释放门缺少种类");
        }
        if (kind == ReleaseGateKind.CONTACT) {
            if (timeline != null) {
                throw new IllegalArgumentException("接触门不携带定时时间线");
            }
        } else if (timeline == null) {
            throw new IllegalArgumentException("这个释放门需要时间线");
        } else if (kind == ReleaseGateKind.IMMEDIATE) {
            if (timeline.windupTicks() != CooperativeStructureLimits.IMMEDIATE_SLASH_RELEASE_TICK
                    || timeline.recoveryTicks() != CooperativeStructureLimits.IMMEDIATE_SLASH_RECOVERY_TICKS
                    || timeline.movementPolicy() != MovementPolicy.IN_PLACE) {
                throw new IllegalArgumentException("立即斩击的时间线必须是 0 刻出刀和 8 刻收势");
            }
        } else if (timeline.windupTicks() < CooperativeStructureLimits.WINDUP_LOW) {
            throw new IllegalArgumentException("定时门的前摇至少为 1 刻");
        }
    }
}
