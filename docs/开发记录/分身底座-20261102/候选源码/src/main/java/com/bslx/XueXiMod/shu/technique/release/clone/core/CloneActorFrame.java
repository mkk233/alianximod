package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record CloneActorFrame(
        ServerLevel level,
        UUID actorId,
        long actorGeneration,
        UUID ownerId,
        Vec3 actorPosition,
        Vec3 eyeOrigin,
        Vec3 lookUnit,
        @Nullable UUID targetId,
        long actionSequence,
        AimSource aimSource) {

    public enum AimSource {
        MANUAL_DIRECTION,
        HURT_TARGET
    }

    public CloneActorFrame {
        if (level == null || actorId == null || ownerId == null || actorPosition == null
                || eyeOrigin == null || lookUnit == null || aimSource == null) {
            throw new IllegalArgumentException("分身动作帧不完整");
        }
    }
}
