package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record ClonePresentationNotice(
        ResourceKey<Level> dimension,
        long sessionToken,
        UUID deploymentId,
        long actionToken,
        long eventSerial,
        PresentationKey presentationKey,
        int presentationRevision,
        Vec3 worldPosition,
        Vec3 lookUnit,
        NoticePhase phase,
        int durationTicks,
        VolumeVisualKind volumeVisual) {

    public ClonePresentationNotice {
        if (dimension == null || deploymentId == null || presentationKey == null || phase == null
                || volumeVisual == null) {
            throw new IllegalArgumentException("表现通知不完整");
        }
        if (sessionToken == 0L || eventSerial <= 0L || durationTicks < 0 || durationTicks > 200) {
            throw new IllegalArgumentException("表现通知的时序无效");
        }
        if (!CaptureVectors.finite(worldPosition) || !CaptureVectors.unit(lookUnit)) {
            throw new IllegalArgumentException("表现通知的坐标无效");
        }
        double limit = 3.0E7D;
        if (Math.abs(worldPosition.x) > limit || Math.abs(worldPosition.y) > limit || Math.abs(worldPosition.z) > limit) {
            throw new IllegalArgumentException("表现通知的坐标超出范围");
        }
    }
}
