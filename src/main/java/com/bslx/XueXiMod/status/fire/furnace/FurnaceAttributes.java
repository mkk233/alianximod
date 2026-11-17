package com.bslx.XueXiMod.status.fire.furnace;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** 同一组修饰器标识。升级先摘再挂，避免普通和过载叠两层。 */
final class FurnaceAttributes {
    private static final UUID HEALTH_MODIFIER = UUID.fromString("c3a1f7e2-6b48-4d91-9f05-1e7a4b8c2d60");
    private static final UUID ARMOR_MODIFIER = UUID.fromString("d4b2a8f3-7c59-4e02-a016-2f8b5c9d3e71");
    private static final UUID TOUGHNESS_MODIFIER = UUID.fromString("e5c3b904-8d6a-4f13-b127-309c6dae4f82");

    private FurnaceAttributes() {
    }

    /** 按档位重挂生命、护甲和韧性，并把当前生命夹进新上限。玩家再据此建立或更新炉心运行态。 */
    static void attach(LivingEntity entity, int amplifier) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        float healthBefore = entity.getHealth();
        FurnaceMath.Tier tier = FurnaceMath.tierOfAmplifier(amplifier);
        removeAll(entity);
        addAll(entity, tier);
        if (entity instanceof Player player) {
            FurnaceStore.onAttached(player, amplifier);
        }
        clampHealth(entity, healthBefore);
    }

    /** 摘掉三组修饰器，并把当前生命夹进恢复后的上限。 */
    static void detach(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        float healthBefore = entity.getHealth();
        removeAll(entity);
        clampHealth(entity, healthBefore);
    }

    /** 三组炉心修饰器里是不是至少还挂着一组。 */
    static boolean wearing(LivingEntity entity) {
        return hasModifier(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER)
                || hasModifier(entity, Attributes.ARMOR, ARMOR_MODIFIER)
                || hasModifier(entity, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER);
    }

    private static void addAll(LivingEntity entity, FurnaceMath.Tier tier) {
        put(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER, "xueximod.furnace_health",
                FurnaceMath.totalModifier(FurnaceMath.healthFactor(tier)));
        put(entity, Attributes.ARMOR, ARMOR_MODIFIER, "xueximod.furnace_armor",
                FurnaceMath.totalModifier(FurnaceMath.armorFactor(tier)));
        put(entity, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER, "xueximod.furnace_toughness",
                FurnaceMath.totalModifier(FurnaceMath.toughnessFactor(tier)));
    }

    private static void removeAll(LivingEntity entity) {
        remove(entity, Attributes.MAX_HEALTH, HEALTH_MODIFIER);
        remove(entity, Attributes.ARMOR, ARMOR_MODIFIER);
        remove(entity, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER);
    }

    private static void put(LivingEntity entity, Attribute attribute, UUID modifierId, String modifierName, double amount) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance == null) {
            return;
        }
        attributeInstance.removeModifier(modifierId);
        attributeInstance.addPermanentModifier(new AttributeModifier(
                modifierId,
                modifierName,
                amount,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void remove(LivingEntity entity, Attribute attribute, UUID modifierId) {
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(modifierId);
        }
    }

    private static boolean hasModifier(LivingEntity entity, Attribute attribute, UUID modifierId) {
        if (entity == null) {
            return false;
        }
        AttributeInstance attributeInstance = entity.getAttribute(attribute);
        return attributeInstance != null && attributeInstance.getModifier(modifierId) != null;
    }

    /** 退出只恢复上限。当前生命保持进入钳制后的数值，不按新上限比例回填。 */
    private static void clampHealth(LivingEntity entity, float healthBefore) {
        float healthCap = entity.getMaxHealth();
        float kept = healthBefore;
        if (kept > healthCap) {
            kept = healthCap;
        }
        if (kept < 0.0F) {
            kept = 0.0F;
        }
        entity.setHealth(kept);
    }
}
