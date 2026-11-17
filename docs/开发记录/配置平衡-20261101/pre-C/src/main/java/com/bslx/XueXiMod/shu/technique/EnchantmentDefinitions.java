package com.bslx.XueXiMod.shu.technique;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantDefinition;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantSlice;
import com.bslx.XueXiMod.shu.technique.release.fire.ResourceEffectKeys;

public final class EnchantmentDefinitions {
    private EnchantmentDefinitions() {}

    public static void registerAll() {
        register("magic_ignite", ShuAttribute.FIRE, "fire");
        register("vine_mark", ShuAttribute.WOOD, "wood");
        register("tide_mark", ShuAttribute.WATER, "water");
        register("sinking_edge", ShuAttribute.EARTH, "earth");
        register("keen_mark", ShuAttribute.METAL, "metal");
        registerResource(ResourceEffectKeys.TRAIL, ShuAttribute.FIRE, ResourceEnchantSlice.trail());
        registerResource(ResourceEffectKeys.SOW, ShuAttribute.FIRE, ResourceEnchantSlice.sow());
        registerResource(ResourceEffectKeys.KINDLE, ShuAttribute.FIRE, ResourceEnchantSlice.kindle());
        registerResource(ResourceEffectKeys.DETONATE, ShuAttribute.FIRE, ResourceEnchantSlice.detonate());
        registerResource(ResourceEffectKeys.GLOOM, ShuAttribute.FIRE, ResourceEnchantSlice.gloom());
        registerResource(ResourceEffectKeys.BREAK_GROUND, ShuAttribute.EARTH, ResourceEnchantSlice.breakGround());
    }

    private static void registerResource(String enchantmentId, ShuAttribute element, ResourceEnchantSlice slice) {
        EnchantmentRegistry.register(new ResourceEnchantDefinition(enchantmentId, element, slice));
    }

    private static void register(String enchantmentId, ShuAttribute element, String effectId) {
        EnchantmentRegistry.register(new EnchantmentDefinition() {
            @Override
            public String id() {
                return enchantmentId;
            }

            @Override
            public ShuAttribute element() {
                return element;
            }

            @Override
            public EnchantmentAccumulator createAccumulator() {
                return new EnchantmentAccumulator(effectId, 0, 0.5f, 5f, 1f, 1f, 1f, 1f);
            }

            @Override
            public EnchantmentAccumulator fold(ElementReaction reaction, EnchantmentAccumulator current) {
                EnchantmentAccumulator base = current != null ? current : createAccumulator();
                return new EnchantmentAccumulator(
                        base.effectId(),
                        base.layers() + 1,
                        base.radiusBase(),
                        base.durationBase(),
                        base.radiusMultiplier() * reaction.radiusMultiplier(),
                        base.durationMultiplier() * reaction.durationMultiplier(),
                        base.damageMultiplier() * reaction.damageMultiplier(),
                        base.speedMultiplier() * reaction.speedMultiplier());
            }

            @Override
            public int magicDamage(ElementReaction reaction) {
                if ("tide_mark".equals(enchantmentId)) {
                    return 2;
                }
                return (int) Math.ceil(2.0 * reaction.damageMultiplier() - 1.0E-6);
            }
        });
    }
}
