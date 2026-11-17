package com.bslx.XueXiMod.shu;

import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;

/**
 * 术 - 最小的单一功能单元
 *
 * complexity: 复杂度。剑术/法术总等级 >= complexity 才解锁；0=默认解锁。
 * 动作/通用始终解锁。
 * 装配、链和释放查询都读这份不可变配置。不负责生成命中体、扣费或结算。
 */
public class Technique {
    private final String techniqueId;
    private final String name;
    private final TechniqueType type;
    private final float baseDamage;
    private final int baseCooldown;
    private final int levelCap;
    private final String description;
    private final int complexity;
    private final ShuAttribute attribute;
    private final float projectileSpeed;
    private final boolean enchantment;
    private final TechniqueAvailability availability;

    /** 未写复杂度、属性、弹速、附魔和可用性时，按 0、无属性、1、非附魔、玩家可用补齐。 */
    public Technique(String techniqueId, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int levelCap, String description) {
        this(techniqueId, name, type, baseDamage, baseCooldown, levelCap, description, 0, ShuAttribute.NONE, 1.0f);
    }

    /** 只补复杂度；属性、弹速、附魔和可用性仍用上面的默认值。 */
    public Technique(String techniqueId, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int levelCap, String description,
                     int complexity) {
        this(techniqueId, name, type, baseDamage, baseCooldown, levelCap, description, complexity, ShuAttribute.NONE, 1.0f);
    }

    /** 补属性和弹速，仍按非附魔、玩家可用建立。 */
    public Technique(String techniqueId, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int levelCap, String description,
                     int complexity, ShuAttribute attribute, float projectileSpeed) {
        this(techniqueId, name, type, baseDamage, baseCooldown, levelCap, description, complexity, attribute, projectileSpeed, false);
    }

    /** 补附魔标记，可用性仍按玩家可用建立。 */
    public Technique(String techniqueId, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int levelCap, String description,
                     int complexity, ShuAttribute attribute, float projectileSpeed, boolean enchantment) {
        this(techniqueId, name, type, baseDamage, baseCooldown, levelCap, description,
                complexity, attribute, projectileSpeed, enchantment, TechniqueAvailability.PLAYER);
    }

    /** 保留全部配置。传入空的可用性改记为玩家可用。 */
    public Technique(String techniqueId, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int levelCap, String description,
                     int complexity, ShuAttribute attribute, float projectileSpeed, boolean enchantment,
                     TechniqueAvailability availability) {
        this.techniqueId = techniqueId;
        this.name = name;
        this.type = type;
        this.baseDamage = baseDamage;
        this.baseCooldown = baseCooldown;
        this.levelCap = levelCap;
        this.description = description;
        this.complexity = complexity;
        this.attribute = attribute;
        this.projectileSpeed = projectileSpeed;
        this.enchantment = enchantment;
        this.availability = availability == null ? TechniqueAvailability.PLAYER : availability;
    }

    public String getId() { return techniqueId; }
    public String getName() { return name; }
    public TechniqueType getType() { return type; }
    public float getBaseDamage() { return baseDamage; }
    public int getBaseCooldown() { return baseCooldown; }
    public int getMaxLevel() { return levelCap; }
    public String getDescription() { return description; }
    public int getComplexity() { return complexity; }

    /** 旧解锁等级入口，现在直接返回复杂度。 */
    @Deprecated
    public int getUnlockLevel() { return complexity; }
    public ShuAttribute getAttribute() { return attribute; }
    public float getProjectileSpeed() { return projectileSpeed; }
    public boolean isEnchantment() { return enchantment; }
    public TechniqueAvailability availability() { return availability; }
    /** 正伤害，或已登记领域、屏障、正半径场地、护体或接触条件时为真。未登记则为假。 */
    public boolean canReceiveEnchantment() {
        if (baseDamage > 0f) {
            return true;
        }
        ReleaseDefinition release = ReleaseRegistry.get(techniqueId);
        return release != null && (release.domain() != null || release.barrier() != null
                || release.fieldRadius() > 0f || release.guard() != null
                || ConditionalEffects.supportsContact(release));
    }

    /** 先用释放定义的非负显式装配消耗；否则附魔2、动作0、正伤害1，其余为0。 */
    public int assemblyCost() {
        ReleaseDefinition release = ReleaseRegistry.get(techniqueId);
        if (release != null && release.explicitAssemblyCost() >= 0) {
            return release.explicitAssemblyCost();
        }
        if (isEnchantment()) return 2;
        if (getType() == TechniqueType.MOVEMENT) return 0;
        if (getBaseDamage() > 0f) return 1;
        return 0;
    }

    /** 附魔、条件、领域、规则和显式魔力分别查表；都没有时把基础伤害乘 2 后向上取整。 */
    public int manaCost() {
        if (isEnchantment()) {
            int explicitPrice = com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantCatalog
                    .explicitManaPrice(techniqueId);
            if (explicitPrice >= 0) {
                return explicitPrice;
            }
            EnchantmentDefinition definition = EnchantmentRegistry.get(techniqueId);
            if (definition == null) return 1;
            ElementReaction reaction = ElementReaction.of(ShuAttribute.NONE, ShuAttribute.NONE);
            return Math.max(1, definition.magicDamage(reaction));
        }
        ReleaseDefinition release = ReleaseRegistry.get(techniqueId);
        if (release != null && release.conditionalEffect() != null && release.explicitManaCost() >= 0) {
            return release.explicitManaCost();
        }
        if (release != null && release.domain() != null) {
            return Math.max(0, release.domain().initialCost());
        }
        if (release != null && release.kind() == ReleaseKind.RULE) return 0;
        if (release != null && release.explicitManaCost() >= 0) {
            return release.explicitManaCost();
        }
        return Math.max(0, (int) Math.ceil(baseDamage * 2.0));
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) return true;
        if (!(candidate instanceof Technique other)) return false;
        return techniqueId.equals(other.techniqueId);
    }

    @Override
    public int hashCode() {
        return techniqueId.hashCode();
    }
}
