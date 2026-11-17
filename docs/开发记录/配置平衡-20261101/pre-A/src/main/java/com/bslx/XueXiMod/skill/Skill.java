package com.bslx.XueXiMod.skill;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * 技 - 术的组合体
 * 由玩家在技法撰写台自行组合命名
 */
public class Skill {
    public static final int CHAIN_LIMIT = 8;
    private String name;                   // 自定义名称
    private final SkillType type;          // 技类型
    private final List<Technique> techniques; // 组成的术列表
    private final List<ShuAttribute> embryos; // 每个术刻印时的原胚属性

    public Skill(String name, SkillType type, List<Technique> techniques) {
        this(name, type, techniques, new ArrayList<>());
    }

    public Skill(String name, SkillType type, List<Technique> techniques, List<ShuAttribute> embryos) {
        this.name = name;
        this.type = type;
        this.techniques = new ArrayList<>(techniques);
        this.embryos = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < this.techniques.size(); slotIndex++) {
            ShuAttribute embryo = (embryos != null && slotIndex < embryos.size()) ? embryos.get(slotIndex) : null;
            this.embryos.add(embryo != null ? embryo : ShuAttribute.NONE);
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public SkillType getType() { return type; }
    public List<Technique> getTechniques() { return techniques; }

    public ShuAttribute getEmbryo(int index) {
        if (index < 0 || index >= embryos.size()) return ShuAttribute.NONE;
        return embryos.get(index);
    }

    public List<ShuAttribute> getEmbryos() { return embryos; }

    /**
     * 计算技的总基础伤害（所有术的伤害之和）
     */
    public float getTotalBaseDamage() {
        float total = 0;
        for (Technique technique : techniques) {
            total += technique.getBaseDamage();
        }
        return total;
    }

    /**
     * 计算技的总基础冷却（取最长冷却）
     */
    public int getTotalBaseCooldown() {
        int upperBound = 0;
        for (int slotIndex = 0; slotIndex < techniques.size(); slotIndex++) {
            Technique technique = techniques.get(slotIndex);
            double multiplier = ElementReaction.of(getEmbryo(slotIndex), technique.getAttribute()).cooldownMultiplier();
            int cooldown = (int) Math.round(technique.getBaseCooldown() * multiplier);
            upperBound = Math.max(upperBound, cooldown);
        }
        return upperBound;
    }

    /**
     * 将技序列化为NBT
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Type", type.name());

        ListTag techList = new ListTag();
        for (int slotIndex = 0; slotIndex < techniques.size(); slotIndex++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", techniques.get(slotIndex).getId());
            ShuAttribute embryo = embryos.get(slotIndex);
            if (embryo != ShuAttribute.NONE) {
                entry.putString("Embryo", embryo.getId());
            }
            techList.add(entry);
        }
        tag.put("Techniques", techList);
        return tag;
    }

    /**
     * 从NBT反序列化技
     */
    public static Skill fromNbt(CompoundTag tag) {
        if (tag == null) return null;

        String typeName = tag.getString("Type");
        if (typeName.isEmpty()) return null;
        SkillType type;
        if ("SWORD".equals(typeName)) {
            type = SkillType.SWORD;
        } else if ("MAGIC".equals(typeName)) {
            type = SkillType.MAGIC;
        } else {
            return null;
        }

        String name = tag.getString("Name");

        List<Technique> techs = new ArrayList<>();
        List<ShuAttribute> embryos = new ArrayList<>();
        Tag raw = tag.get("Techniques");
        if (raw instanceof ListTag techList) {
            for (int slotIndex = 0; slotIndex < techList.size(); slotIndex++) {
                Tag element = techList.get(slotIndex);
                if (element instanceof StringTag text) {
                    Technique technique = TechniqueManager.getById(text.getAsString());
                    if (technique != null) {
                        techs.add(technique);
                        embryos.add(ShuAttribute.NONE);
                    }
                } else if (element instanceof CompoundTag entry) {
                    Technique technique = TechniqueManager.getById(entry.getString("Id"));
                    if (technique != null) {
                        techs.add(technique);
                        ShuAttribute embryo = entry.contains("Embryo")
                                ? ShuAttribute.fromId(entry.getString("Embryo"))
                                : ShuAttribute.NONE;
                        embryos.add(embryo);
                    }
                }
            }
        }

        return new Skill(name, type, techs, embryos);
    }
}
