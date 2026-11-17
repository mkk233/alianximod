package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CancelReason;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 某一具分身的一次短距计划。只存在于所属服务器运行时里。 */
final class CloneRepositionPlan {
    final UUID planId;
    final CloneHandle handle;
    final Vec3 originFeet;
    final Vec3 targetFeet;
    final int durationTicks;
    int completedTicks;
    long lastAdvancedTick = Long.MIN_VALUE;
    boolean closed;
    @Nullable
    CancelReason failure;

    CloneRepositionPlan(UUID planId, CloneHandle handle, Vec3 originFeet, Vec3 targetFeet, int durationTicks) {
        this.planId = planId;
        this.handle = handle;
        this.originFeet = originFeet;
        this.targetFeet = targetFeet;
        this.durationTicks = durationTicks;
    }

    boolean arrived() {
        return this.completedTicks >= this.durationTicks;
    }
}
