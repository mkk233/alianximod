package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantCatalog;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.skill.Skill;

import java.util.List;
import java.util.UUID;

/**
 * 附魔类型的统一结算。旧五元素仍从效果键出数值。
 * 本批六个新附魔不进入火木水土金分支，类型载荷在 resourceLoad。
 */
public final class EnchantBonus {
    public static final EnchantBonus NONE = new EnchantBonus(0f, 0f, 0, 1f, 1f, 0, false, 0f, 1f, 0);

    public final float attackDamage;
    public final float addedDamage;
    public final int burnSeconds;
    public final float radiusMultiplier;
    public final float cooldownMultiplier;
    public final int controlTicks;
    public final boolean extinguish;
    public final float knockback;
    public final float speedMultiplier;
    /** 潮印条数。每条固定 +2 伤，不进 added，也不吃同水的 30%。 */
    public final int tideMarks;
    /** 点火附魔的固定火种。有旧火效果就是 2，不随层数、伤害或时长变化。种火的 10 不写在这里。 */
    public final int seedPoints;
    /** 本批新附魔载荷。为空时旧结算数字保持原样。 */
    public final ResourceEnchantLoad resourceLoad;

    public EnchantBonus(float attackDamage, float addedDamage, int burnSeconds, float radiusMultiplier,
                        float cooldownMultiplier, int controlTicks, boolean extinguish, float knockback,
                        float speedMultiplier, int tideMarks) {
        this(attackDamage, addedDamage, burnSeconds, radiusMultiplier, cooldownMultiplier, controlTicks,
                extinguish, knockback, speedMultiplier, tideMarks, 0);
    }

    public EnchantBonus(float attackDamage, float addedDamage, int burnSeconds, float radiusMultiplier,
                        float cooldownMultiplier, int controlTicks, boolean extinguish, float knockback,
                        float speedMultiplier, int tideMarks, int seedPoints) {
        this(attackDamage, addedDamage, burnSeconds, radiusMultiplier, cooldownMultiplier, controlTicks,
                extinguish, knockback, speedMultiplier, tideMarks, seedPoints, ResourceEnchantLoad.EMPTY);
    }

    public EnchantBonus(float attackDamage, float addedDamage, int burnSeconds, float radiusMultiplier,
                        float cooldownMultiplier, int controlTicks, boolean extinguish, float knockback,
                        float speedMultiplier, int tideMarks, int seedPoints, ResourceEnchantLoad resourceLoad) {
        this.attackDamage = attackDamage;
        this.addedDamage = addedDamage;
        this.burnSeconds = burnSeconds;
        this.radiusMultiplier = radiusMultiplier;
        this.cooldownMultiplier = cooldownMultiplier;
        this.controlTicks = controlTicks;
        this.extinguish = extinguish;
        this.knockback = knockback;
        this.speedMultiplier = speedMultiplier;
        this.tideMarks = Math.max(0, tideMarks);
        this.seedPoints = Math.max(0, seedPoints);
        this.resourceLoad = resourceLoad == null ? ResourceEnchantLoad.EMPTY : resourceLoad;
    }

    public float tideFlat() {
        return this.tideMarks * 2f;
    }

    /** 攻击宿主的一份伤害：底伤、其它附魔 added、潮印固定伤、武器。不再另加 magicDamage。 */
    public float carriedDamage(float weaponBonus) {
        return this.attackDamage + this.addedDamage + this.tideFlat() + weaponBonus;
    }

    public TideMarkSuccess tideSuccess(UUID casterId) {
        return TideMarkSuccess.of(this.tideMarks, casterId);
    }

    public static EnchantBonus resolve(Skill skill, List<Integer> enchantIndexes, float attackBase, int layers) {
        ResourceEnchantLoad resourceLoad = ResourceEnchantCatalog.collect(skill, enchantIndexes);
        if (skill == null || enchantIndexes == null || enchantIndexes.isEmpty() || layers < 1) {
            return new EnchantBonus(attackBase, 0f, 0, 1f, 1f, 0, false, 0f, 1f, 0, 0, resourceLoad);
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
        int tideMarks = 0;
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
                        tideMarks += 1;
                        if (sameLayer) {
                            attack = bump(attack, true);
                        }
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
            return new EnchantBonus(attackBase, 0f, 0, 1f, 1f, 0, false, 0f, 1f, 0, 0, resourceLoad);
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
        int seedPoints = hasFire ? 2 : 0;
        return new EnchantBonus(attack, added, burn, radius, cooldown, controlTicks, extinguish, knockback, speed,
                tideMarks, seedPoints, resourceLoad);
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
