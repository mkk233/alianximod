package com.bslx.XueXiMod.status.wood.vine;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

/** 缠枝自己的束缚标记。不改属性，移动限制由专用逻辑处理。 */
public final class RoseBindEffect extends MobEffect {
    public RoseBindEffect() {
        super(MobEffectCategory.HARMFUL, 0xC45B78);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
    }
}
