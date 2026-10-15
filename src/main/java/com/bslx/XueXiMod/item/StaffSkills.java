package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StaffSkills {
    private static final String ROOT_KEY = "xueximod_staff_skills";
    private static final String TECHNIQUES_KEY = "Techniques";
    private static final String SLOT_PREFIX = "Slot";

    public static final class Entry {
        public final String techniqueId;
        public final ShuAttribute embryo;

        public Entry(String techniqueId, ShuAttribute embryo) {
            this.techniqueId = techniqueId;
            this.embryo = embryo;
        }
    }

    public static int slotCount(ItemStack stack) {
        if (!StaffItem.holds(stack)) return 0;
        return ((StaffItem) stack.getItem()).data().slotCount();
    }

    public static int count(ItemStack stack) {
        return readTechniques(stack).size();
    }

    public static List<Entry> entries(ItemStack stack) {
        List<Entry> result = new ArrayList<>();
        ListTag list = readTechniques(stack);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String id = entry.getString("Id");
            ShuAttribute embryo = entry.contains("Embryo")
                    ? ShuAttribute.fromId(entry.getString("Embryo"))
                    : null;
            result.add(new Entry(id, embryo));
        }
        return result;
    }

    public static boolean add(ItemStack stack, String techniqueId, ShuAttribute embryo) {
        if (!StaffItem.holds(stack)) return false;
        int max = slotCount(stack);
        CompoundTag root = stack.getTag();
        CompoundTag skills = (root != null && root.contains(ROOT_KEY)) ? root.getCompound(ROOT_KEY) : null;
        ListTag list;
        if (skills != null && skills.contains(TECHNIQUES_KEY)) {
            list = skills.getList(TECHNIQUES_KEY, 10);
        } else if (skills != null) {
            list = flattenOldSlots(stack, skills);
        } else {
            list = new ListTag();
        }
        if (list.size() >= max) return false;

        if (root == null) {
            root = stack.getOrCreateTag();
        }
        skills = root.getCompound(ROOT_KEY);
        if (!skills.contains(TECHNIQUES_KEY)) {
            skills.put(TECHNIQUES_KEY, list);
            removeOldSlots(skills);
            root.put(ROOT_KEY, skills);
        }

        CompoundTag entry = new CompoundTag();
        entry.putString("Id", techniqueId);
        if (embryo != null && embryo != ShuAttribute.NONE) {
            entry.putString("Embryo", embryo.getId());
        }
        list.add(entry);
        return true;
    }

    public static void remove(ItemStack stack, int index) {
        if (!StaffItem.holds(stack) || index < 0) return;
        ListTag list = readTechniques(stack);
        if (index >= list.size()) return;
        list.remove(index);
    }

    public static void move(ItemStack stack, int index, int delta) {
        if (!StaffItem.holds(stack) || (delta != -1 && delta != 1)) return;
        ListTag list = readTechniques(stack);
        int target = index + delta;
        if (index < 0 || index >= list.size() || target < 0 || target >= list.size()) return;
        Tag moved = list.get(index);
        list.remove(index);
        list.add(target, moved);
    }

    public static Skill asSkill(ItemStack stack) {
        ListTag list = readTechniques(stack);
        if (list.isEmpty()) return null;
        List<Technique> techniques = new ArrayList<>();
        List<ShuAttribute> embryos = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Technique technique = TechniqueManager.getById(entry.getString("Id"));
            if (technique == null) continue;
            techniques.add(technique);
            embryos.add(entry.contains("Embryo")
                    ? ShuAttribute.fromId(entry.getString("Embryo"))
                    : ShuAttribute.NONE);
        }
        if (techniques.isEmpty()) return null;
        return new Skill("法杖", SkillType.MAGIC, techniques, embryos);
    }

    private static ListTag readTechniques(ItemStack stack) {
        if (!StaffItem.holds(stack)) return new ListTag();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY)) return new ListTag();
        CompoundTag skills = root.getCompound(ROOT_KEY);
        if (!skills.contains(TECHNIQUES_KEY)) {
            return migrate(stack, root, skills);
        }
        return skills.getList(TECHNIQUES_KEY, 10);
    }

    private static ListTag migrate(ItemStack stack, CompoundTag root, CompoundTag skills) {
        ListTag list = flattenOldSlots(stack, skills);
        removeOldSlots(skills);
        skills.put(TECHNIQUES_KEY, list);
        root.put(ROOT_KEY, skills);
        return list;
    }

    private static ListTag flattenOldSlots(ItemStack stack, CompoundTag skills) {
        ListTag list = new ListTag();
        int max = slotCount(stack);
        List<Integer> slots = new ArrayList<>();
        for (String key : skills.getAllKeys()) {
            if (key.startsWith(SLOT_PREFIX)) {
                try {
                    slots.add(Integer.parseInt(key.substring(SLOT_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        slots.sort(Integer::compareTo);
        for (int slot : slots) {
            if (list.size() >= max) break;
            Skill skill = Skill.fromNbt(skills.getCompound(SLOT_PREFIX + slot));
            if (skill == null) continue;
            for (int i = 0; i < skill.getTechniques().size() && list.size() < max; i++) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", skill.getTechniques().get(i).getId());
                ShuAttribute embryo = skill.getEmbryo(i);
                if (embryo != ShuAttribute.NONE) {
                    entry.putString("Embryo", embryo.getId());
                }
                list.add(entry);
            }
        }
        return list;
    }

    private static void removeOldSlots(CompoundTag skills) {
        List<String> keys = new ArrayList<>();
        for (String key : skills.getAllKeys()) {
            if (key.startsWith(SLOT_PREFIX)) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            skills.remove(key);
        }
    }
}
