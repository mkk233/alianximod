package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.shu.TechniqueIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SkillStorageData {
    public static final String ITEMS_KEY = "Items";
    private static final String KEY_SPELL = "spell_id";
    private static final String KEY_EMBRYO = "embryo";
    private static final String KEY_COUNT = "count";

    public static final class Entry {
        private final String spellId;
        private final String embryoId;
        private final int count;

        public Entry(String spellId, String embryoId, int count) {
            this.spellId = spellId;
            this.embryoId = normalizeEmbryo(embryoId);
            this.count = count;
        }

        public String spellId() { return spellId; }
        public String embryoId() { return embryoId; }
        public int count() { return count; }
    }

    public static List<Entry> entries(ItemStack stack) {
        List<Entry> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null) return result;
        ListTag list = tag.getList(ITEMS_KEY, 10);
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            CompoundTag entry = list.getCompound(entryIndex);
            String spellId = TechniqueIds.canonicalId(entry.getString(KEY_SPELL));
            if (spellId.isEmpty()) continue;
            result.add(new Entry(spellId, entry.getString(KEY_EMBRYO), entry.getInt(KEY_COUNT)));
        }
        return result;
    }

    public static void add(ItemStack stack, String spellId, String embryoId, int count) {
        if (stack.isEmpty() || spellId == null || spellId.isEmpty() || count <= 0) return;
        String storedId = TechniqueIds.canonicalId(spellId);
        String embryo = normalizeEmbryo(embryoId);
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.getList(ITEMS_KEY, 10);
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            CompoundTag entry = list.getCompound(entryIndex);
            if (sameSpell(storedId, entry.getString(KEY_SPELL))
                    && embryo.equals(normalizeEmbryo(entry.getString(KEY_EMBRYO)))) {
                entry.putString(KEY_SPELL, storedId);
                entry.putInt(KEY_COUNT, entry.getInt(KEY_COUNT) + count);
                return;
            }
        }
        CompoundTag entry = new CompoundTag();
        entry.putString(KEY_SPELL, storedId);
        entry.putString(KEY_EMBRYO, embryo);
        entry.putInt(KEY_COUNT, count);
        list.add(entry);
        tag.put(ITEMS_KEY, list);
    }

    public static boolean consume(ItemStack stack, String spellId, String embryoId, int count) {
        if (stack.isEmpty() || spellId == null || spellId.isEmpty() || count <= 0) return false;
        String storedId = TechniqueIds.canonicalId(spellId);
        String embryo = normalizeEmbryo(embryoId);
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        ListTag list = tag.getList(ITEMS_KEY, 10);
        for (int entryIndex = 0; entryIndex < list.size(); entryIndex++) {
            CompoundTag entry = list.getCompound(entryIndex);
            if (sameSpell(storedId, entry.getString(KEY_SPELL))
                    && embryo.equals(normalizeEmbryo(entry.getString(KEY_EMBRYO)))) {
                int current = entry.getInt(KEY_COUNT);
                if (current < count) return false;
                int remaining = current - count;
                if (remaining <= 0) {
                    list.remove(entryIndex);
                } else {
                    entry.putString(KEY_SPELL, storedId);
                    entry.putInt(KEY_COUNT, remaining);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean sameSpell(String leftId, String rightId) {
        return TechniqueIds.canonicalId(leftId).equals(TechniqueIds.canonicalId(rightId));
    }

    private static String normalizeEmbryo(String embryoId) {
        if (embryoId == null || embryoId.isEmpty()) return "none";
        return embryoId;
    }
}
