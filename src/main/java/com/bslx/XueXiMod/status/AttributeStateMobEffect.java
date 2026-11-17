package com.bslx.XueXiMod.status;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/** 属性状态的药水外壳。效果被移除时清掉对应的精确数值。潮湿单独挂固定比例的移速。 */
public final class AttributeStateMobEffect extends MobEffect {
    private static final UUID SPEED_MODIFIER = UUID.fromString("6b8e1d24-5c7a-4f93-9e10-2a4c8d6f0b31");

    private final AttributeStateDefinition definition;

    public AttributeStateMobEffect(AttributeStateDefinition definition, int color) {
        super(MobEffectCategory.HARMFUL, color);
        this.definition = definition;
    }

    public AttributeStateDefinition definition() {
        return this.definition;
    }

    /** 挂上效果时，只有潮湿按档位加固定比例的减速。 */
    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.addAttributeModifiers(entity, attributes, amplifier);
        if (this.definition != WetnessState.DEFINITION || amplifier < 0) {
            return;
        }
        double reduction = WetnessTiers.ofValue(wetnessForAmplifier(amplifier)).movementReduction();
        if (reduction <= 0.0D) {
            return;
        }
        AttributeInstance speed = attributes.getInstance(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(SPEED_MODIFIER);
        speed.addPermanentModifier(new AttributeModifier(
                SPEED_MODIFIER,
                "xueximod.wetness_speed",
                -reduction,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    /** 去掉潮湿减速。效果已经不在身上时，把对应的精确数值清成 0。 */
    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        if (this.definition == WetnessState.DEFINITION) {
            AttributeInstance speed = attributes.getInstance(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(SPEED_MODIFIER);
            }
        }
        super.removeAttributeModifiers(entity, attributes, amplifier);
        if (!entity.hasEffect(this)) {
            AttributeStateData.write(entity, this.definition.identifier(), 0);
        }
    }

    /** 药水 amplifier 比档位少 1。各档下沿进入同一份湿度表，避免原版按 amplifier 连乘。 */
    private static int wetnessForAmplifier(int amplifier) {
        if (amplifier >= 3) {
            return 76;
        }
        if (amplifier == 2) {
            return 51;
        }
        if (amplifier == 1) {
            return 26;
        }
        return 1;
    }
}
