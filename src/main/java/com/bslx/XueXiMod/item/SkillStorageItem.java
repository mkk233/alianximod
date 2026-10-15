package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.gui.SpellStorageMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class SkillStorageItem extends Item {
    public SkillStorageItem(Properties properties) {
        super(properties);
    }

    public static ItemStack find(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof SkillStorageItem) return stack;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() instanceof SkillStorageItem) return offhand;
        return ItemStack.EMPTY;
    }

    public static ItemStack getOrCreate(Player player) {
        ItemStack existing = find(player);
        if (!existing.isEmpty()) return existing;
        ItemStack stack = new ItemStack(ModItems.skill_storage.get());
        if (!player.getInventory().add(stack)) {
            return ItemStack.EMPTY;
        }
        return find(player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("术式收纳箱");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player p) {
                    return new SpellStorageMenu(containerId, playerInv);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
