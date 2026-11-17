package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
import com.bslx.XueXiMod.shu.TechniqueIds;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StaffSkills {
    private static final String ROOT_KEY = "xueximod_staff_skills";
    private static final String TECHNIQUES_KEY = "Techniques";
    private static final String SLOT_PREFIX = "Slot";
    private static final String PRESERVED_KEY = "PreservedSlots";
    private static final String HELD_KEY = "MigrationHeld";

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
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            CompoundTag entry = list.getCompound(entryIndex);
            String techniqueId = TechniqueIds.canonicalId(entry.getString("Id"));
            ShuAttribute embryo = entry.contains("Embryo")
                    ? ShuAttribute.fromId(entry.getString("Embryo"))
                    : null;
            result.add(new Entry(techniqueId, embryo));
        }
        return result;
    }

    public static boolean overflowHeld(ItemStack stack) {
        if (!StaffItem.holds(stack)) {
            return false;
        }
        int slots = slotCount(stack);
        if (slots <= 0 || slots > StaffData.SLOT_CAP) {
            return true;
        }
        CompoundTag skills = skillsTag(stack);
        if (skills == null) {
            return false;
        }
        if (skills.getBoolean(HELD_KEY)) {
            return true;
        }
        if (!skills.contains(TECHNIQUES_KEY)) {
            int counted = countOldEntries(skills);
            return counted > slots;
        }
        return skills.getList(TECHNIQUES_KEY, 10).size() > slots;
    }

    public static String signature(ItemStack stack) {
        StringBuilder builder = new StringBuilder();
        for (Entry entry : entries(stack)) {
            builder.append(entry.techniqueId).append('\n');
        }
        return builder.toString();
    }

    public static String castRefusal(ServerPlayer player, ItemStack stack) {
        if (!StaffItem.holds(stack) || player == null) {
            return null;
        }
        int slots = slotCount(stack);
        if (slots <= 0 || slots > StaffData.SLOT_CAP) {
            return "gui.xueximod.staff.bad_slots";
        }
        if (overflowHeld(stack)) {
            return "gui.xueximod.staff.overflow_cast";
        }
        PlayerShuData data = PlayerShuData.get(player);
        for (Entry entry : entries(stack)) {
            Technique technique = TechniqueManager.getById(entry.techniqueId);
            if (technique == null) {
                return "gui.xueximod.staff.unknown";
            }
            if (!TechniqueAccess.allowsPlayerUse(technique)) {
                return TechniqueAccess.DENIAL_TEXT;
            }
            if (technique.getType() == TechniqueType.SWORD) {
                return "gui.xueximod.staff.sword";
            }
            if (!data.hasLearned(technique.getId()) || !data.isUnlocked(technique)) {
                return "gui.xueximod.staff.unlearned";
            }
        }
        return null;
    }

    public static boolean add(ItemStack stack, String techniqueId, ShuAttribute embryo) {
        if (!StaffItem.holds(stack) || overflowHeld(stack)) return false;
        int slotLimit = slotCount(stack);
        CompoundTag root = stack.getTag();
        CompoundTag skills = (root != null && root.contains(ROOT_KEY)) ? root.getCompound(ROOT_KEY) : null;
        ListTag list;
        if (skills != null && skills.contains(TECHNIQUES_KEY)) {
            list = skills.getList(TECHNIQUES_KEY, 10);
        } else if (skills != null) {
            list = flattenOldSlots(skills, slotLimit);
        } else {
            list = new ListTag();
        }
        if (list.size() >= slotLimit) return false;

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
        entry.putString("Id", TechniqueIds.canonicalId(techniqueId));
        if (embryo != null && embryo != ShuAttribute.NONE) {
            entry.putString("Embryo", embryo.getId());
        }
        list.add(entry);
        return true;
    }

    public static void remove(ItemStack stack, int index) {
        if (!StaffItem.holds(stack) || overflowHeld(stack) || index < 0) return;
        ListTag list = readTechniques(stack);
        if (index >= list.size()) return;
        list.remove(index);
    }

    public static void move(ItemStack stack, int index, int delta) {
        if (!StaffItem.holds(stack) || overflowHeld(stack) || (delta != -1 && delta != 1)) return;
        ListTag list = readTechniques(stack);
        int target = index + delta;
        if (index < 0 || index >= list.size() || target < 0 || target >= list.size()) return;
        Tag moved = list.get(index);
        list.remove(index);
        list.add(target, moved);
    }

    public static Skill asSkill(ItemStack stack) {
        if (overflowHeld(stack)) {
            return null;
        }
        ListTag list = readTechniques(stack);
        if (list.isEmpty()) return null;
        List<Technique> techniques = new ArrayList<>();
        List<ShuAttribute> embryos = new ArrayList<>();
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            CompoundTag entry = list.getCompound(entryIndex);
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

    private static CompoundTag skillsTag(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY)) {
            return null;
        }
        return root.getCompound(ROOT_KEY);
    }

    private static ListTag migrate(ItemStack stack, CompoundTag root, CompoundTag skills) {
        ListTag list = flattenOldSlots(skills, Integer.MAX_VALUE);
        int slotLimit = Math.min(slotCount(stack), StaffData.SLOT_CAP);
        if (list.size() <= slotLimit) {
            removeOldSlots(skills);
            skills.remove(HELD_KEY);
            skills.put(TECHNIQUES_KEY, list);
            root.put(ROOT_KEY, skills);
            return list;
        }
        if (!skills.contains(PRESERVED_KEY)) {
            skills.put(PRESERVED_KEY, list.copy());
        }
        skills.putBoolean(HELD_KEY, true);
        root.put(ROOT_KEY, skills);
        return new ListTag();
    }

    private static int countOldEntries(CompoundTag skills) {
        return flattenOldSlots(skills, Integer.MAX_VALUE).size();
    }

    private static ListTag flattenOldSlots(CompoundTag skills, int slotLimit) {
        ListTag list = new ListTag();
        List<Integer> slots = new ArrayList<>();
        for (String key : skills.getAllKeys()) {
            if (key.startsWith(SLOT_PREFIX) && !key.equals(PRESERVED_KEY)) {
                try {
                    slots.add(Integer.parseInt(key.substring(SLOT_PREFIX.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        slots.sort(Integer::compareTo);
        for (int slotNumber : slots) {
            if (list.size() >= slotLimit) {
                break;
            }
            Skill skill = Skill.fromNbt(skills.getCompound(SLOT_PREFIX + slotNumber));
            if (skill == null) continue;
            for (int techniqueIndex = 0; techniqueIndex < skill.getTechniques().size() && list.size() < slotLimit; techniqueIndex++) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", skill.getTechniques().get(techniqueIndex).getId());
                ShuAttribute embryo = skill.getEmbryo(techniqueIndex);
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
