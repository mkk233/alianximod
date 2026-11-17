package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.clone.core.NoticePhase;
import com.bslx.XueXiMod.shu.technique.release.clone.core.PresentationKey;
import com.bslx.XueXiMod.shu.technique.release.clone.core.VolumeVisualKind;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 一份与实体脱离的只读表现。创建后不再改，刷新同步也不能重播。 */
public final class CloneAfterimageSnapshot {
    private final UUID deploymentId;
    private final long actionToken;
    private final long eventSerial;
    private final long sessionToken;
    private final NoticePhase phase;
    private final PresentationKey presentationKey;
    private final int presentationRevision;
    private final double worldX;
    private final double worldY;
    private final double worldZ;
    private final double lookX;
    private final double lookY;
    private final double lookZ;
    private final int durationTicks;
    private final VolumeVisualKind volumeVisual;
    private final double depthBlocks;
    private final double heightBlocks;
    private final double widthBlocks;
    private final long birthGameTime;
    private final ResourceKey<Level> dimension;

    public CloneAfterimageSnapshot(UUID deploymentId, long actionToken, long eventSerial, long sessionToken,
                                   NoticePhase phase, PresentationKey presentationKey, int presentationRevision,
                                   double worldX, double worldY, double worldZ, double lookX, double lookY,
                                   double lookZ, int durationTicks, VolumeVisualKind volumeVisual,
                                   double depthBlocks, double heightBlocks, double widthBlocks, long birthGameTime,
                                   ResourceKey<Level> dimension) {
        this.deploymentId = deploymentId;
        this.actionToken = actionToken;
        this.eventSerial = eventSerial;
        this.sessionToken = sessionToken;
        this.phase = phase;
        this.presentationKey = presentationKey;
        this.presentationRevision = presentationRevision;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.lookX = lookX;
        this.lookY = lookY;
        this.lookZ = lookZ;
        this.durationTicks = durationTicks;
        this.volumeVisual = volumeVisual;
        this.depthBlocks = depthBlocks;
        this.heightBlocks = heightBlocks;
        this.widthBlocks = widthBlocks;
        this.birthGameTime = birthGameTime;
        this.dimension = dimension;
    }

    public UUID deploymentId() {
        return this.deploymentId;
    }

    public long actionToken() {
        return this.actionToken;
    }

    public long eventSerial() {
        return this.eventSerial;
    }

    public long sessionToken() {
        return this.sessionToken;
    }

    public NoticePhase phase() {
        return this.phase;
    }

    public PresentationKey presentationKey() {
        return this.presentationKey;
    }

    public int presentationRevision() {
        return this.presentationRevision;
    }

    public double worldX() {
        return this.worldX;
    }

    public double worldY() {
        return this.worldY;
    }

    public double worldZ() {
        return this.worldZ;
    }

    public double lookX() {
        return this.lookX;
    }

    public double lookY() {
        return this.lookY;
    }

    public double lookZ() {
        return this.lookZ;
    }

    public int durationTicks() {
        return this.durationTicks;
    }

    public VolumeVisualKind volumeVisual() {
        return this.volumeVisual;
    }

    public double depthBlocks() {
        return this.depthBlocks;
    }

    public double heightBlocks() {
        return this.heightBlocks;
    }

    public double widthBlocks() {
        return this.widthBlocks;
    }

    public long birthGameTime() {
        return this.birthGameTime;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public Vec3 anchor() {
        return new Vec3(this.worldX, this.worldY, this.worldZ);
    }

    public Vec3 lookUnit() {
        return new Vec3(this.lookX, this.lookY, this.lookZ);
    }

    public boolean expired(long gameTime) {
        return gameTime >= this.birthGameTime + (long) this.durationTicks;
    }
}
