package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 纯准备输入。没有玩家、世界、主手或线程局部释放帧。 */
public record AttackPreparation(String techniqueId, int chainIndex, ConfiguredDamage configuredDamage,
                                AdapterVolumeSpec volume, WeaponDisplaySnapshot weapon, Vec3 frozenDirection,
                                SettlementEnvelope settlement, UUID cancelId) {
    public AttackPreparation {
        if (techniqueId == null || techniqueId.isBlank()) {
            throw new IllegalArgumentException("攻击准备缺少术式");
        }
        if (chainIndex < 0) {
            throw new IllegalArgumentException("攻击准备的链下标不能为负");
        }
        if (configuredDamage == null || volume == null || weapon == null || settlement == null || cancelId == null) {
            throw new IllegalArgumentException("攻击准备不完整");
        }
        frozenDirection = FrozenValues.unit(frozenDirection, "冻结刀向");
    }
}
