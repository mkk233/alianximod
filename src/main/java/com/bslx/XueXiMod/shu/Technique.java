package com.bslx.XueXiMod.shu;

import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;

/**
 * 术 - 最小的单一功能单元
 *
 * complexity: 复杂度。剑术/法术总等级 >= complexity 才解锁；0=默认解锁。
 * 动作/通用始终解锁。
 */
public class Technique {
    private final String id;
    private final String name;
    private final TechniqueType type;
    private final float baseDamage;
    private final int baseCooldown;
    private final int maxLevel;
    private final String description;
    private final int complexity;
    private final ShuAttribute attribute;
    private final float projectileSpeed;
    private final boolean enchantment;

    public Technique(String id, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int maxLevel, String description) {
        this(id, name, type, baseDamage, baseCooldown, maxLevel, description, 0, ShuAttribute.NONE, 1.0f);
    }

    public Technique(String id, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int maxLevel, String description,
                     int complexity) {
        this(id, name, type, baseDamage, baseCooldown, maxLevel, description, complexity, ShuAttribute.NONE, 1.0f);
    }

    public Technique(String id, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int maxLevel, String description,
                     int complexity, ShuAttribute attribute, float projectileSpeed) {
        this(id, name, type, baseDamage, baseCooldown, maxLevel, description, complexity, attribute, projectileSpeed, false);
    }

    public Technique(String id, String name, TechniqueType type,
                     float baseDamage, int baseCooldown, int maxLevel, String description,
                     int complexity, ShuAttribute attribute, float projectileSpeed, boolean enchantment) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.baseDamage = baseDamage;
        this.baseCooldown = baseCooldown;
        this.maxLevel = maxLevel;
        this.description = description;
        this.complexity = complexity;
        this.attribute = attribute;
        this.projectileSpeed = projectileSpeed;
        this.enchantment = enchantment;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public TechniqueType getType() { return type; }
    public float getBaseDamage() { return baseDamage; }
    public int getBaseCooldown() { return baseCooldown; }
    public int getMaxLevel() { return maxLevel; }
    public String getDescription() { return description; }
    public int getComplexity() { return complexity; }

    @Deprecated
    public int getUnlockLevel() { return complexity; }
    public ShuAttribute getAttribute() { return attribute; }
    public float getProjectileSpeed() { return projectileSpeed; }
    public boolean isEnchantment() { return enchantment; }
    public boolean canReceiveEnchantment() { return baseDamage > 0f; }

    public int assemblyCost() {
        if (isEnchantment()) return 2;
        if (getType() == TechniqueType.MOVEMENT) return 0;
        if (getBaseDamage() > 0f) return 1;
        return 0;
    }

    public int manaCost() {
        if (isEnchantment()) {
            EnchantmentDefinition definition = EnchantmentRegistry.get(id);
            if (definition == null) return 1;
            ElementReaction reaction = ElementReaction.of(ShuAttribute.NONE, ShuAttribute.NONE);
            return Math.max(1, definition.magicDamage(reaction));
        }
        ReleaseDefinition release = ReleaseRegistry.get(id);
        if (release != null && release.kind() == ReleaseKind.RULE) return 0;
        return Math.max(0, (int) Math.ceil(baseDamage * 2.0));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Technique other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
