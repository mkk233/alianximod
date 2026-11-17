package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record CloneActionRequest(
        CloneHandle handle,
        UUID rootEventId,
        CloneTriggerInput.Source source,
        int sourceDepth,
        String actionKey,
        long requestSerial,
        long requestedTick,
        long executeTick,
        long expireTick,
        @Nullable Vec3 manualLook,
        @Nullable UUID attackerId) {

    public CloneActionRequest {
        if (handle == null || rootEventId == null || source == null || actionKey == null || actionKey.isBlank()) {
            throw new IllegalArgumentException("分身请求不完整");
        }
    }
}
