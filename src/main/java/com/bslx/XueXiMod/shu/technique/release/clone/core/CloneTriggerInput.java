package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record CloneTriggerInput(
        Source source,
        @Nullable Vec3 manualLook,
        @Nullable UUID attackerId,
        int sourceDepth) {

    public enum Source {
        MANUAL,
        HURT
    }

    public CloneTriggerInput {
        if (source == null) {
            throw new IllegalArgumentException("分身触发缺少来源");
        }
    }
}
