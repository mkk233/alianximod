package com.bslx.XueXiMod.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class SkillAssemblyMenu extends AbstractContainerMenu {

    public SkillAssemblyMenu(int containerId, Inventory playerInv) {
        super(ModMenuTypes.SKILL_ASSEMBLY.get(), containerId);
    }

    public SkillAssemblyMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
