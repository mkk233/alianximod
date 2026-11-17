package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.player.LearningGraph;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.player.StudyService;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 刻印台：原胚输入格 + 术式输出格。
 */
public class ImprintDeskMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 186;
    public static final int INV_TEXTURE_Y = 103;

    private final BlockPos blockPosition;
    public final Player player;
    private final SimpleContainer crafts = new SimpleContainer(2);

    public ImprintDeskMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData != null ? extraData.readBlockPos() : BlockPos.ZERO);
    }

    public ImprintDeskMenu(int containerId, Inventory playerInv, BlockPos blockPosition) {
        super(ModMenuTypes.IMPRINT_DESK.get(), containerId);
        this.blockPosition = blockPosition;
        this.player = playerInv.player;

        this.addSlot(new MovingSlot(crafts, 0, 26, 18, true) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ShuEmbryoItem;
            }
        });
        this.addSlot(new MovingSlot(crafts, 1, 134, 18, true) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new MovingSlot(playerInv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18, false));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new MovingSlot(playerInv, col, 8 + col * 18, 162, false));
        }
    }

    public void placeClientSlot(int menuIndex, int slotX, int slotY, boolean active) {
        if (menuIndex < 0 || menuIndex >= slots.size() || !player.level().isClientSide) {
            return;
        }
        Slot slot = slots.get(menuIndex);
        if (slot instanceof MovingSlot moving) {
            moving.setClientActive(active);
        }
    }

    public ItemStack getEmbryoStack() {
        return crafts.getItem(0);
    }

    public ItemStack getOutputStack() {
        return crafts.getItem(1);
    }

    public List<Technique> getLearnedTechniques() {
        List<Technique> result = new ArrayList<>();
        if (player.level().isClientSide) {
            Set<String> learned = ShuSyncPacket.ClientShuCache.getLearnedIds();
            for (Technique technique : TechniqueManager.playerCatalog()) {
                if (ShuSyncPacket.ClientShuCache.hasLearned(technique.getId())
                        && ShuSyncPacket.ClientShuCache.isUnlocked(technique)) {
                    result.add(technique);
                }
            }
            return result;
        }
        PlayerShuData data = PlayerShuData.get(player);
        for (Technique technique : TechniqueManager.playerCatalog()) {
            if (data.hasLearned(technique.getId()) && data.isUnlocked(technique)) {
                result.add(technique);
            }
        }
        return result;
    }

    public List<Technique> getStudyTechniques() {
        if (player.level().isClientSide) {
            return LearningGraph.studyable(clientData());
        }
        return LearningGraph.studyable(PlayerShuData.get(player));
    }

    private static PlayerShuData clientData() {
        PlayerShuData data = new PlayerShuData();
        for (String techniqueId : ShuSyncPacket.ClientShuCache.getLearnedIds()) {
            data.learnTechnique(techniqueId);
        }
        return data;
    }

    public boolean imprint(Player imprintPlayer, String techniqueId) {
        return ImprintActions.imprint(imprintPlayer, techniqueId, crafts);
    }

    public boolean study(Player studyPlayer, String techniqueId) {
        return StudyService.study(studyPlayer, techniqueId, crafts.getItem(0));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == 0 || index == 1) {
            if (!this.moveItemStackTo(stack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ShuEmbryoItem) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPosition),
                player, ModBlocks.IMPRINT_DESK.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            this.clearContainer(player, crafts);
        }
    }

    public static class MovingSlot extends Slot {
        private boolean clientActive;

        public MovingSlot(net.minecraft.world.Container container, int slot, int x, int y, boolean active) {
            super(container, slot, x, y);
            this.clientActive = active;
        }

        public void setClientActive(boolean active) {
            this.clientActive = active;
        }

        @Override
        public boolean isActive() {
            return clientActive;
        }
    }
}
