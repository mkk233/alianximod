package com.bslx.XueXiMod.shu.technique.sword;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * 真剑的可失败右键归还。先确认背包能完整容纳旧主手，再一次性交换。
 * 放不下时不调用会掉落物品的回收。
 */
public final class SwordFlightReturn {
    private SwordFlightReturn() {
    }

    /** 快捷栏 0 到 8，再主背包 9 到 35。不看副手和护甲。 */
    public static int firstRealSwordSlot(ServerPlayer player) {
        if (player == null) {
            return -1;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot <= 35; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof SwordItem && stack.getCount() == 1) {
                return slot;
            }
        }
        return -1;
    }

    /** 主手空则直接放入。主手有物时，只有背包能完整收下旧主手才交换。 */
    public static boolean tryReturn(ServerPlayer player, ItemStack carried) {
        if (player == null || carried == null || carried.isEmpty() || !(carried.getItem() instanceof SwordItem)) {
            return false;
        }
        Inventory inventory = player.getInventory();
        ItemStack sword = carried.copy();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, sword);
            return ItemStack.matches(player.getItemInHand(InteractionHand.MAIN_HAND), sword);
        }
        ItemStack displaced = mainHand.copy();
        if (!canTakeWhole(inventory, displaced)) {
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (!inventory.add(displaced)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, displaced);
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        if (!ItemStack.matches(player.getItemInHand(InteractionHand.MAIN_HAND), sword)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, displaced);
            return false;
        }
        return true;
    }

    private static boolean canTakeWhole(Inventory inventory, ItemStack stack) {
        int remaining = stack.getCount();
        int limit = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize());
        for (int slot = 0; slot < inventory.items.size() && remaining > 0; slot++) {
            ItemStack present = inventory.items.get(slot);
            if (present.isEmpty()) {
                remaining -= limit;
            } else if (ItemStack.isSameItemSameTags(present, stack)) {
                remaining -= Math.max(0, limit - present.getCount());
            }
        }
        return remaining <= 0;
    }
}
