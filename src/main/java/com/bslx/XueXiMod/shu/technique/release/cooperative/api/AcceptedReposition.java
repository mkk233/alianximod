package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 已经通过安全检查的短距计划。合同参数 maxDistance 在这里叫 distanceLimit。 */
public record AcceptedReposition(UUID planId, CloneHandle handle, Vec3 destination, double distanceLimit,
                                 int durationTicks) implements RepositionAttempt {
    public AcceptedReposition {
        if (planId == null || handle == null) {
            throw new IllegalArgumentException("短距计划不完整");
        }
        destination = FrozenValues.finite(destination, "短距目的地");
        if (!Double.isFinite(distanceLimit) || distanceLimit < 0.0D
                || distanceLimit > CooperativeStructureLimits.REPOSITION_DISTANCE_CAP) {
            throw new IllegalArgumentException("短距距离超出 0 到 8 格");
        }
        if (durationTicks < 1 || durationTicks > CooperativeStructureLimits.REPOSITION_TICK_CAP) {
            throw new IllegalArgumentException("短距时长只能是 1 或 2 刻");
        }
    }
}
