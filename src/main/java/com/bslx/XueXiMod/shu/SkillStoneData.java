package com.bslx.XueXiMod.shu;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * 术式身份 + 刻印时固化的战斗快照。
 * 蓝图（属性/类型/复杂度）仍从 Technique 注册表读取。
 */
public class SkillStoneData {
    public static final float MATCH_DAMAGE_MULT = 1.30f;
    public static final float MATCH_COOLDOWN_MULT = 0.70f;

    private final String spellId;
    private final ShuAttribute embryoAttribute;
    private final float damage;
    private final int cooldown;
    private final float projectileSpeed;

    public SkillStoneData(String spellId, ShuAttribute embryoAttribute,
                          float damage, int cooldown, float projectileSpeed) {
        this.spellId = spellId;
        this.embryoAttribute = embryoAttribute;
        this.damage = damage;
        this.cooldown = cooldown;
        this.projectileSpeed = projectileSpeed;
    }

    public static SkillStoneData imprint(Technique technique, ShuAttribute embryoAttribute) {
        ElementReaction reaction = ElementReaction.of(embryoAttribute, technique.getAttribute());
        float damage = technique.getBaseDamage() * reaction.damageMultiplier();
        int cooldown = Math.max(1, Math.round(technique.getBaseCooldown() * reaction.cooldownMultiplier()));
        return new SkillStoneData(technique.getId(), embryoAttribute, damage, cooldown, technique.getProjectileSpeed());
    }

    public String getSpellId() { return spellId; }
    public ShuAttribute getEmbryoAttribute() { return embryoAttribute; }
    public float getDamage() { return damage; }
    public int getCooldown() { return cooldown; }
    public float getProjectileSpeed() { return projectileSpeed; }

    @Nullable
    public Technique getTechnique() {
        return TechniqueManager.getById(spellId);
    }

    public boolean isAttributeMatch() {
        Technique technique = getTechnique();
        return technique != null && isSameAttribute(technique.getAttribute(), embryoAttribute);
    }

    private static boolean isSameAttribute(ShuAttribute a, ShuAttribute b) {
        return a != null && a != ShuAttribute.NONE && a == b;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("spell_id", spellId);
        tag.putString("embryo_attribute", embryoAttribute.getId());
        CompoundTag snapshot = new CompoundTag();
        snapshot.putFloat("damage", damage);
        snapshot.putInt("cooldown", cooldown);
        snapshot.putFloat("projectile_speed", projectileSpeed);
        tag.put("snapshot", snapshot);
        return tag;
    }

    @Nullable
    public static SkillStoneData fromNbt(CompoundTag tag) {
        if (tag == null || !tag.contains("spell_id")) return null;
        String spellId = tag.getString("spell_id");
        ShuAttribute embryo = ShuAttribute.fromId(tag.getString("embryo_attribute"));
        CompoundTag snapshot = tag.getCompound("snapshot");
        float damage = snapshot.getFloat("damage");
        int cooldown = snapshot.contains("cooldown") ? snapshot.getInt("cooldown") : 1;
        float speed = snapshot.contains("projectile_speed") ? snapshot.getFloat("projectile_speed") : 1.0f;
        return new SkillStoneData(spellId, embryo, damage, cooldown, speed);
    }
}
