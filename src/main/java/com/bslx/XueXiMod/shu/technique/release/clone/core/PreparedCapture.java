package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record PreparedCapture(
        CloneSpawnAuthority authority,
        UUID ownerId,
        ResourceKey<Level> dimension,
        boolean immediate,
        Vec3 feetPosition,
        Vec3 eyeOrigin,
        Vec3 lookUnit,
        int totalMana,
        int residenceTicks,
        double decoyRadiusBlocks,
        double bodyHealth,
        CapturedAttackSnapshot snapshot,
        long structureEpoch) {

    public PreparedCapture {
        if (authority != CloneSpawnAuthority.PLAYER_CAPTURED_CAST || ownerId == null || dimension == null
                || snapshot == null) {
            throw new IllegalArgumentException("捕获部署计划不完整");
        }
        if (!ownerId.equals(snapshot.ownerId()) || structureEpoch != snapshot.structureEpoch()) {
            throw new IllegalArgumentException("捕获部署计划和快照不一致");
        }
        if (totalMana < 0 || residenceTicks < 0 || !Double.isFinite(bodyHealth) || bodyHealth <= 0.0D
                || !Double.isFinite(decoyRadiusBlocks) || decoyRadiusBlocks < 0.0D) {
            throw new IllegalArgumentException("捕获部署计划的费用或寿命无效");
        }
        if (immediate && decoyRadiusBlocks != 0.0D) {
            throw new IllegalArgumentException("立即模式不能带诱敌半径");
        }
        if (!CaptureVectors.finite(feetPosition) || !CaptureVectors.finite(eyeOrigin) || !CaptureVectors.unit(lookUnit)) {
            throw new IllegalArgumentException("捕获部署计划的位置无效");
        }
    }
}
