package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.skill.Skill;

import java.util.List;

/**
 * 附魔类型的统一结算。新附魔只要在 EnchantmentRegistry 登记，就从这里出数值。
 */
public final class EnchantBonus {
    public static final EnchantBonus NONE = new EnchantBonus(0f, 0f, 0, 1f, 1f, 0, false, 0f, 1f);

    public final float attackDamage;
    public final float addedDamage;
    public final int burnSeconds;
    public final float radiusMultiplier;
    public final float cooldownMultiplier;
    public final int controlTicks;
    public final boolean extinguish;
    public final float knockback;
    public final float speedMultiplier;

    public EnchantBonus(float attackDamage, float addedDamage, int burnSeconds, float radiusMultiplier,
                        float cooldownMultiplier, int controlTicks, boolean extinguish, float knockback, float speedMultiplier) {
        this.attackDamage = attackDamage;
        this.addedDamage = addedDamage;
        this.burnSeconds = burnSeconds;
        this.radiusMultiplier = radiusMultiplier;
        this.cooldownMultiplier = cooldownMultiplier;
        this.controlTicks = controlTicks;
        this.extinguish = extinguish;
        this.knockback = knockback;
        this.speedMultiplier = speedMultiplier;
    }

    public static EnchantBonus resolve(Skill skill, List<Integer> enchantIndexes, float attackBase, int layers) {
        if (skill == null || enchantIndexes == null || enchantIndexes.isEmpty() || layers < 1) {
            return new EnchantBonus(attackBase, 0f, 0, 1f, 1f, 0, false, 0f, 1f);
        }
        float attack = attackBase;
        float added = 0f;
        float radius = 1f;
        float duration = 1f;
        float speed = 1f;
        int controlTicks = 0;
        boolean extinguish = false;
        float knockback = 0f;
        boolean same = false;
        boolean any = false;
        boolean metal = false;
        int times = Math.max(1, layers);
        var techniques = skill.getTechniques();
        for (int layer = 0; layer < times; layer++) {
            for (int index : enchantIndexes) {
                if (index < 0 || index >= techniques.size()) {
                    continue;
                }
                EnchantmentDefinition definition = EnchantmentRegistry.get(techniques.get(index).getId());
                if (definition == null || definition.createAccumulator() == null) {
                    continue;
                }
                String effectId = definition.createAccumulator().effectId();
                ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
                boolean sameLayer = reaction.kind() == ElementReaction.Kind.SAME;
                if (sameLayer) {
                    same = true;
                }
                switch (effectId) {
                    case "fire" -> {
                        any = true;
                        if (added <= 0f) {
                            added = 2f;
                        }
                        if (sameLayer) {
                            added = bump(added, true);
                            attack = bump(attack, true);
                        }
                        if (reaction.kind() == ElementReaction.Kind.GENERATE) {
                            radius *= reaction.radiusMultiplier();
                            duration *= reaction.durationMultiplier();
                        }
                    }
                    case "wood" -> {
                        any = true;
                        if (added <= 0f) {
                            added = 2f;
                        }
                        if (sameLayer) {
                            added = bump(added, true);
                            attack = bump(attack, true);
                        }
                        int ticks = Math.max(1, Math.round(40f * reaction.durationMultiplier()));
                        controlTicks = Math.max(controlTicks, ticks);
                    }
                    case "water" -> {
                        any = true;
                        if (added <= 0f) {
                            added = 2f;
                        }
                        if (sameLayer) {
                            added = bump(added, true);
                            attack = bump(attack, true);
                        }
                        extinguish = true;
                        speed *= reaction.speedMultiplier();
                    }
                    case "earth" -> {
                        any = true;
                        if (added <= 0f) {
                            added = 2f;
                        }
                        if (sameLayer) {
                            added = bump(added, true);
                            attack = bump(attack, true);
                        }
                        radius *= 1.1f * reaction.distanceMultiplier();
                        knockback = Math.max(knockback, 0.35f);
                    }
                    case "metal" -> {
                        any = true;
                        metal = true;
                        if (added <= 0f) {
                            added = 2f;
                        }
                        if (sameLayer) {
                            added = bump(added, true);
                            attack = bump(attack, true);
                        }
                        radius *= reaction.searchMultiplier();
                    }
                    default -> {
                    }
                }
            }
        }
        if (!any) {
            return new EnchantBonus(attackBase, 0f, 0, 1f, 1f, 0, false, 0f, 1f);
        }
        float cooldown = 1f;
        if (same) {
            cooldown *= times <= 1 ? 0.7f : 0.4f;
        }
        if (metal) {
            cooldown *= 0.9f;
        }
        int burn = duration == 1f && !same ? 0 : 0;
        boolean hasFire = false;
        for (int index : enchantIndexes) {
            if (index < 0 || index >= techniques.size()) {
                continue;
            }
            EnchantmentDefinition definition = EnchantmentRegistry.get(techniques.get(index).getId());
            if (definition != null && definition.createAccumulator() != null
                    && "fire".equals(definition.createAccumulator().effectId())) {
                hasFire = true;
                break;
            }
        }
        if (hasFire) {
            burn = Math.max(1, Math.round(5f * duration));
        } else {
            burn = 0;
        }
        return new EnchantBonus(attack, added, burn, radius, cooldown, controlTicks, extinguish, knockback, speed);
    }

    private static float bump(float value, boolean sameAttribute) {
        if (!sameAttribute) {
            return value;
        }
        float delta = value * 0.3f;
        if (delta > 0f && delta < 1f) {
            delta = 1f;
        }
        return value + delta;
    }
}
