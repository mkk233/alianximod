package com.bslx.XueXiMod.shu.technique.release.cooperative.action;

import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.MovementPolicy;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGate;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.TimelineSpec;
import com.bslx.XueXiMod.shu.technique.release.cooperative.attack.ServerSpecialRead;
import net.minecraft.resources.ResourceLocation;

/**
 * 回旋只描述服务端定时门。默认前摇 6 刻、收势 6 刻，原地，自身没有命中体。
 * 动作标识只是数据，这里不写关键帧，也不转动玩家视角。
 */
public final class SpinTimelineDefinition {
    public static final String WINDUP_TICKS = "windupTicks";
    public static final String RECOVERY_TICKS = "recoveryTicks";
    public static final int DEFAULT_WINDUP_TICKS = 6;
    public static final int DEFAULT_RECOVERY_TICKS = 6;

    private SpinTimelineDefinition() {
    }

    public static ResourceLocation actionId() {
        return CooperativeActionIds.SPIN;
    }

    /** 动作开始前调用一次，把读到的刻数冻进执行计划。不要另存一套动画时长。 */
    public static TimelineSpec serverTimeline() {
        String techniqueId = CooperativeTechniqueIds.SPIN;
        int windupTicks = ServerSpecialRead.integer(techniqueId, WINDUP_TICKS, DEFAULT_WINDUP_TICKS);
        int recoveryTicks = ServerSpecialRead.integer(techniqueId, RECOVERY_TICKS, DEFAULT_RECOVERY_TICKS);
        return new TimelineSpec(windupTicks, recoveryTicks, MovementPolicy.IN_PLACE);
    }

    public static ReleaseGate timedGate() {
        return new ReleaseGate(ReleaseGateKind.TIMED, serverTimeline());
    }

    /** 只声明前摇和收势。标准魔力、冷却不在这里重复，回旋也没有伤害倍率。 */
    public static void declareSpecialBalance() {
        SpecialBalance.publish(SpecialBalance.begin(CooperativeTechniqueIds.SPIN)
                .integerParameter(WINDUP_TICKS, RawDefault.literal(DEFAULT_WINDUP_TICKS),
                        BalanceBound.inclusive(CooperativeStructureLimits.WINDUP_LOW, CooperativeStructureLimits.WINDUP_HIGH),
                        BalanceUnit.TICKS, "回旋前摇")
                .integerParameter(RECOVERY_TICKS, RawDefault.literal(DEFAULT_RECOVERY_TICKS),
                        BalanceBound.inclusive(CooperativeStructureLimits.RECOVERY_LOW, CooperativeStructureLimits.RECOVERY_HIGH),
                        BalanceUnit.TICKS, "回旋收势"));
    }
}
