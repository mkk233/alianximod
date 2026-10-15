package com.bslx.XueXiMod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class StaffItem extends Item {
    private static final String MANA_KEY = "xueximod_staff_mana";

    private final StaffData data;

    public StaffItem(StaffData data, Properties properties) {
        super(data.maxManaBonus() > 0 ? properties.durability(data.maxManaBonus()) : properties);
        this.data = data;
    }

    public StaffData data() { return data; }

    public static boolean holds(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof StaffItem;
    }

    public static int storedMana(ItemStack stack) {
        if (!holds(stack)) return 0;
        int bonus = ((StaffItem) stack.getItem()).data().maxManaBonus();
        if (bonus <= 0) return 0;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(MANA_KEY)) {
            int migrated = Math.max(0, Math.min(bonus, tag.getInt(MANA_KEY)));
            stack.setDamageValue(bonus - migrated);
            tag.remove(MANA_KEY);
            return migrated;
        }
        return Math.max(0, Math.min(bonus, bonus - stack.getDamageValue()));
    }

    public static void setStoredMana(ItemStack stack, int value) {
        if (!holds(stack)) return;
        int bonus = ((StaffItem) stack.getItem()).data().maxManaBonus();
        if (bonus <= 0) return;
        stack.setDamageValue(bonus - Math.max(0, Math.min(bonus, value)));
        CompoundTag tag = stack.getTag();
        if (tag != null) tag.remove(MANA_KEY);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) return true;
        if (oldStack.getItem() != newStack.getItem()) return true;
        return !tagsEqualIgnoringDamage(oldStack.getTag(), newStack.getTag());
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        return 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return data.maxManaBonus() > 0;
    }

    private static boolean tagsEqualIgnoringDamage(CompoundTag a, CompoundTag b) {
        CompoundTag ca = a == null ? new CompoundTag() : a.copy();
        CompoundTag cb = b == null ? new CompoundTag() : b.copy();
        ca.remove("Damage");
        cb.remove("Damage");
        return ca.equals(cb);
    }
}
