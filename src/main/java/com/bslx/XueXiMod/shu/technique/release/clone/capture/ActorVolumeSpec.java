package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.shu.technique.release.clone.core.VolumeVisualKind;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

/**
 * 一份不可变的静止体积。几何和伤害都在部署时冻结，运行时不再缩放。
 */
public record ActorVolumeSpec(
        long sessionToken,
        ResourceKey<Level> dimension,
        UUID ownerId,
        UUID rootEventId,
        UUID deploymentId,
        long actionToken,
        Vec3 eyeOrigin,
        Vec3 forwardUnit,
        Vec3 upUnit,
        Vec3 rightUnit,
        double depthBlocks,
        double heightBlocks,
        double widthBlocks,
        int lifetimeTicks,
        ConfiguredDamage configuredDamage,
        int adapterVersion,
        VolumeVisualKind volumeVisual) {

    public ActorVolumeSpec {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(rootEventId, "rootEventId");
        Objects.requireNonNull(deploymentId, "deploymentId");
        Objects.requireNonNull(eyeOrigin, "eyeOrigin");
        Objects.requireNonNull(forwardUnit, "forwardUnit");
        Objects.requireNonNull(upUnit, "upUnit");
        Objects.requireNonNull(rightUnit, "rightUnit");
        Objects.requireNonNull(configuredDamage, "configuredDamage");
        Objects.requireNonNull(volumeVisual, "volumeVisual");
    }
}
