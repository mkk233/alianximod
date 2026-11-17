package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public record CloneHandle(
        long sessionToken,
        ResourceKey<Level> dimension,
        UUID actorId,
        long actorGeneration) {

    public CloneHandle {
        if (dimension == null || actorId == null) {
            throw new IllegalArgumentException("分身句柄不完整");
        }
    }
}
