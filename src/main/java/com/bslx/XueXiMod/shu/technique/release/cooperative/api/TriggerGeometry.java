package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 接触或定时门的一次触发几何。点和目标可缺，方向会收成单位向量。 */
public record TriggerGeometry(@Nullable Vec3 point, Vec3 direction, @Nullable UUID targetId) {
    public TriggerGeometry {
        direction = FrozenValues.unit(direction, "触发方向");
        if (point != null) {
            point = FrozenValues.finite(point, "触发点");
        }
    }
}
