package com.bslx.XueXiMod.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public class SkillCarrier {

    public static boolean canRelease(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof StaffItem);
    }

    public static boolean allowsMagic(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof StaffItem;
    }
}
