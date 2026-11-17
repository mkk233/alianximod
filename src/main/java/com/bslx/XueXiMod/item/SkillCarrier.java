package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.common.ToolActions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SkillCarrier {
    public static final TagKey<Item> SWORD_SKILL_CARRIERS = ItemTags.create(
            ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "sword_skill_carriers"));

    private static final List<Predicate<ItemStack>> EXTERNAL = new ArrayList<>();

    public static boolean canRelease(ItemStack stack) {
        return !stack.isEmpty()
                && (acceptsSwordSkill(stack) || stack.getItem() instanceof StaffItem);
    }

    public static boolean allowsMagic(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof StaffItem;
    }

    /**
     * 剑技载体：剑、镐、斧、锄、铲、三叉戟，以及标了工具动作或物品标签的外部武器。
     * 法杖不算剑技载体。别的模组也可以调用 {@link #registerExternal(Predicate)}。
     */
    public static boolean acceptsSwordSkill(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof StaffItem) return false;
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof DiggerItem || item instanceof TridentItem) return true;
        if (stack.is(SWORD_SKILL_CARRIERS)) return true;
        if (stack.canPerformAction(ToolActions.SWORD_DIG)
                || stack.canPerformAction(ToolActions.PICKAXE_DIG)
                || stack.canPerformAction(ToolActions.AXE_DIG)
                || stack.canPerformAction(ToolActions.SHOVEL_DIG)
                || stack.canPerformAction(ToolActions.HOE_DIG)) {
            return true;
        }
        for (Predicate<ItemStack> test : EXTERNAL) {
            if (test.test(stack)) return true;
        }
        return false;
    }

    public static void registerExternal(Predicate<ItemStack> test) {
        if (test != null) EXTERNAL.add(test);
    }
}
