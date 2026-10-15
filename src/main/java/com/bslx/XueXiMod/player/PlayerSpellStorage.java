package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.item.SkillStorageData;
import com.bslx.XueXiMod.item.SkillStorageItem;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerSpellStorage {
    public static final String KEY = "xueximod_spell_storage";
    public static final int SIZE = 27;

    private PlayerSpellStorage() {
    }

    public static SimpleContainer load(Player player) {
        SimpleContainer container = new SimpleContainer(SIZE);
        CompoundTag root = player.getPersistentData().getCompound(KEY);
        ListTag list = root.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot >= 0 && slot < SIZE) {
                container.setItem(slot, ItemStack.of(entry));
            }
        }
        return container;
    }

    public static void save(Player player, SimpleContainer container) {
        ListTag list = new ListTag();
        for (int i = 0; i < SIZE; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) i);
            stack.save(entry);
            list.add(entry);
        }
        CompoundTag root = new CompoundTag();
        root.put("Items", list);
        player.getPersistentData().put(KEY, root);
    }

    public static void migrate(Player player, SimpleContainer container) {
        migrateStack(player.getInventory().items, container);
        migrateOne(player.getOffhandItem(), container);
    }

    private static void migrateStack(java.util.List<ItemStack> stacks, SimpleContainer container) {
        for (ItemStack stack : stacks) {
            migrateOne(stack, container);
        }
    }

    private static void migrateOne(ItemStack pouch, SimpleContainer container) {
        if (pouch.isEmpty() || !(pouch.getItem() instanceof SkillStorageItem)) {
            return;
        }
        CompoundTag tag = pouch.getTag();
        if (tag == null || !tag.contains(SkillStorageData.ITEMS_KEY)) {
            return;
        }
        ListTag list = tag.getList(SkillStorageData.ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag entry = list.getCompound(i);
            String spellId = entry.getString("spell_id");
            if (spellId.isEmpty()) {
                list.remove(i);
                continue;
            }
            String embryoId = entry.getString("embryo");
            int count = entry.getInt("count");
            Technique technique = TechniqueManager.getById(spellId);
            if (technique == null || count <= 0) {
                continue;
            }
            ShuAttribute embryo = ShuAttribute.fromId(embryoId);
            int moved = 0;
            while (moved < count) {
                ItemStack stone = SkillStoneItem.create(SkillStoneData.imprint(technique, embryo));
                int stackSize = Math.min(stone.getMaxStackSize(), count - moved);
                stone.setCount(stackSize);
                ItemStack leftover = container.addItem(stone);
                int accepted = stackSize - leftover.getCount();
                if (accepted <= 0) {
                    break;
                }
                moved += accepted;
            }
            if (moved >= count) {
                list.remove(i);
            } else {
                entry.putInt("count", count - moved);
            }
        }
        if (list.isEmpty()) {
            tag.remove(SkillStorageData.ITEMS_KEY);
        } else {
            tag.put(SkillStorageData.ITEMS_KEY, list);
        }
    }

    public static boolean insertOne(Player player, ItemStack stone) {
        SimpleContainer container = load(player);
        migrate(player, container);
        ItemStack one = stone.copy();
        one.setCount(1);
        ItemStack leftover = container.addItem(one);
        if (!leftover.isEmpty()) {
            return false;
        }
        save(player, container);
        return true;
    }
}
