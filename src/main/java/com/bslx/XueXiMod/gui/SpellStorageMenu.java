package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.player.PlayerSpellStorage;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public class SpellStorageMenu extends AbstractContainerMenu {
    private final SimpleContainer storage;
    private final Player player;

    public SpellStorageMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SPELL_STORAGE.get(), containerId);
        this.player = playerInventory.player;
        this.storage = new SimpleContainer(PlayerSpellStorage.SIZE);
        if (player instanceof ServerPlayer) {
            SimpleContainer loaded = PlayerSpellStorage.load(player);
            PlayerSpellStorage.migrate(player, loaded);
            for (int segmentIndex = 0; segmentIndex < PlayerSpellStorage.SIZE; segmentIndex++) {
                storage.setItem(segmentIndex, loaded.getItem(segmentIndex));
            }
            PlayerSpellStorage.save(player, storage);
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(storage, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        int invY = 85;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, invY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < PlayerSpellStorage.SIZE) {
            if (!moveItemStackTo(stack, PlayerSpellStorage.SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, PlayerSpellStorage.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            PlayerSpellStorage.save(player, storage);
        }
    }
}
