package com.bslx.XueXiMod.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.function.Consumer;

public class StaffItem extends Item {
    private static final String MANA_KEY = "xueximod_staff_mana";
    private static final String PAY_ID = "xuexi_pay_id";

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

    public static UUID payIdentity(ItemStack stack) {
        if (!holds(stack)) {
            return null;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(PAY_ID)) {
            tag.putUUID(PAY_ID, UUID.randomUUID());
        }
        return tag.getUUID(PAY_ID);
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

    private static boolean tagsEqualIgnoringDamage(CompoundTag first, CompoundTag second) {
        CompoundTag firstCopy = first == null ? new CompoundTag() : first.copy();
        CompoundTag secondCopy = second == null ? new CompoundTag() : second.copy();
        firstCopy.remove("Damage");
        secondCopy.remove("Damage");
        firstCopy.remove("xueximod_cast_ready");
        secondCopy.remove("xueximod_cast_ready");
        firstCopy.remove("xueximod_cast_session");
        secondCopy.remove("xueximod_cast_session");
        firstCopy.remove("xueximod_technique_ready");
        secondCopy.remove("xueximod_technique_ready");
        firstCopy.remove("xueximod_chain_step");
        secondCopy.remove("xueximod_chain_step");
        firstCopy.remove("xueximod_chain_sign");
        secondCopy.remove("xueximod_chain_sign");
        return firstCopy.equals(secondCopy);
    }
}
