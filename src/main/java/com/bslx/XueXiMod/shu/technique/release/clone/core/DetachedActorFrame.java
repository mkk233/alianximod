package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record DetachedActorFrame(
        long sessionToken,
        ResourceKey<Level> dimension,
        UUID ownerId,
        UUID rootEventId,
        UUID deploymentId,
        Vec3 feetPosition,
        Vec3 eyeOrigin,
        Vec3 lookUnit,
        long frozenGameTick) {

    public DetachedActorFrame {
        if (dimension == null || ownerId == null || rootEventId == null || deploymentId == null) {
            throw new IllegalArgumentException("脱离身体的行动帧不完整");
        }
        if (!CaptureVectors.finite(feetPosition) || !CaptureVectors.finite(eyeOrigin) || !CaptureVectors.unit(lookUnit)) {
            throw new IllegalArgumentException("脱离身体的行动帧坐标无效");
        }
        if (sessionToken == 0L) {
            throw new IllegalArgumentException("脱离身体的行动帧没有会话");
        }
    }
}
