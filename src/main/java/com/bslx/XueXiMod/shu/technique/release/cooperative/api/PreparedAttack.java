package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.resources.ResourceLocation;

/**
 * {@link ActorAttackAdapter#prepare} 的不可变结果。
 * 局部形状相对玩家眼点，还没有加上其他演员的平移。
 */
public record PreparedAttack(AttackPreparation preparation, ResourceLocation presentationId,
                             ExplicitVolumeGeometry localShape) {
    public PreparedAttack {
        if (preparation == null || presentationId == null || localShape == null) {
            throw new IllegalArgumentException("已准备攻击不完整");
        }
        if (localShape.boxDepth() != preparation.volume().boxDepth()
                || localShape.boxWidth() != preparation.volume().boxWidth()
                || localShape.boxThickness() != preparation.volume().boxThickness()) {
            throw new IllegalArgumentException("已准备形状和配置尺寸不一致");
        }
    }
}
