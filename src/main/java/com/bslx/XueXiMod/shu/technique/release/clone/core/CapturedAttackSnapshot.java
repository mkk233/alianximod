package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;

import java.util.UUID;

public record CapturedAttackSnapshot(
        String payloadTechniqueId,
        int adapterVersion,
        long structureEpoch,
        UUID ownerId,
        UUID rootEventId,
        ConfiguredDamage configuredDamage,
        double depthBlocks,
        double heightBlocks,
        double widthBlocks,
        int lifetimeTicks,
        int configuredPayloadMana,
        int configuredPayloadCooldownTicks) {

    public CapturedAttackSnapshot {
        if (payloadTechniqueId == null || payloadTechniqueId.isBlank() || ownerId == null || rootEventId == null
                || configuredDamage == null) {
            throw new IllegalArgumentException("捕获快照不完整");
        }
        if (adapterVersion < 1 || configuredPayloadMana < 0 || configuredPayloadCooldownTicks < 0 || lifetimeTicks <= 0) {
            throw new IllegalArgumentException("捕获快照的版本、费用或寿命无效");
        }
        if (!finitePositive(depthBlocks) || !finitePositive(heightBlocks) || !finitePositive(widthBlocks)) {
            throw new IllegalArgumentException("捕获快照的几何无效");
        }
        float amount = configuredDamage.amount();
        if (!Float.isFinite(amount) || amount < 0.0F) {
            throw new IllegalArgumentException("捕获快照的伤害无效");
        }
    }

    private static boolean finitePositive(double extent) {
        return Double.isFinite(extent) && extent > 0.0D;
    }
}
