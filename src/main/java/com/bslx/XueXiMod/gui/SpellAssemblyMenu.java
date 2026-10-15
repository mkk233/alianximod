package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.network.BoundSkillSyncPacket;
import com.bslx.XueXiMod.player.PlayerBoundSkills;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.player.PlayerSpellStorage;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;

import java.util.ArrayList;
import java.util.List;

public class SpellAssemblyMenu extends AbstractContainerMenu {
    public static final int STORAGE = PlayerSpellStorage.SIZE;
    public static final int CELL = 18;
    public static final int LEFT = 4;
    public static final int FILTER_H = 34;
    public static final int LIST_Y = 40;
    public static final int LIST_H = 4 * CELL;
    public static final int BOTTOM_Y = LIST_Y + LIST_H + 2;
    public static final int INV_W = 9 * CELL;
    public static final int GEAR_X = LEFT + INV_W + 3;
    public static final int GEAR_W = 8 * CELL;
    public static final int PREVIEW_X = GEAR_X + GEAR_W + 3;
    public static final int PREVIEW_W = 96;
    public static final int GUI_W = PREVIEW_X + PREVIEW_W + 4;
    public static final int GUI_H = BOTTOM_Y + 3 * CELL + 2 + CELL + 4;
    private final SimpleContainer storage;
    private final SimpleContainer gear;
    private final Player player;
    private final boolean staff;
    private final int gearSlots;
    private boolean suppress;
    private TechniqueType typeFilter;
    private ShuAttribute attrFilter;

    public SpellAssemblyMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SPELL_ASSEMBLY.get(), containerId);
        this.player = playerInventory.player;
        this.staff = StaffItem.holds(player.getMainHandItem());
        this.gearSlots = staff ? Math.max(1, ((StaffItem) player.getMainHandItem().getItem()).data().slotCount()) : 32;
        this.storage = new SimpleContainer(STORAGE);
        this.gear = new SimpleContainer(gearSlots);
        if (player instanceof ServerPlayer) {
            SimpleContainer loaded = PlayerSpellStorage.load(player);
            PlayerSpellStorage.migrate(player, loaded);
            for (int i = 0; i < STORAGE; i++) {
                storage.setItem(i, loaded.getItem(i));
            }
            PlayerSpellStorage.save(player, storage);
            fillGear();
        }
        for (int index = 0; index < PlayerSpellStorage.SIZE; index++) {
            int col = index % 8;
            int row = index / 8;
            addSlot(new StorageSlot(storage, index, LEFT + col * CELL, LIST_Y + row * CELL));
        }
        for (int index = 0; index < gearSlots; index++) {
            int col = index % 8;
            int row = index / 8;
            addSlot(new GearSlot(index, GEAR_X + col * CELL, BOTTOM_Y + row * CELL));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, LEFT + col * CELL, BOTTOM_Y + row * CELL));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, LEFT + col * CELL, BOTTOM_Y + 3 * CELL + 2));
        }
    }

    public boolean matchesFilter(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        SkillStoneData data = SkillStoneItem.getData(stack);
        if (data == null || data.getTechnique() == null) {
            return typeFilter == null && attrFilter == null;
        }
        Technique technique = data.getTechnique();
        if (typeFilter != null && technique.getType() != typeFilter) {
            return false;
        }
        return attrFilter == null || technique.getAttribute() == attrFilter;
    }

    public void setFilters(TechniqueType type, ShuAttribute attribute) {
        this.typeFilter = type;
        this.attrFilter = attribute;
    }

    public TechniqueType currentType() {
        return typeFilter;
    }

    public ShuAttribute currentAttribute() {
        return attrFilter;
    }

    public boolean staff() {
        return staff;
    }

    public int gearRows() {
        return (gearSlots + 7) / 8;
    }

    public int gearSlotCount() {
        return gearSlots;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        int gearStart = STORAGE;
        int gearEnd = STORAGE + gearSlots;
        if (index < STORAGE) {
            if (!moveItemStackTo(stack, gearEnd, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < gearEnd) {
            if (!moveItemStackTo(stack, 0, STORAGE, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, gearEnd, false)) {
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
        return player.isAlive() && SkillCarrier.canRelease(player.getItemInHand(InteractionHand.MAIN_HAND));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            PlayerSpellStorage.save(player, storage);
        }
    }

    private void fillGear() {
        suppress = true;
        for (int i = 0; i < gearSlots; i++) {
            gear.setItem(i, ItemStack.EMPTY);
        }
        if (staff) {
            List<StaffSkills.Entry> entries = StaffSkills.entries(player.getMainHandItem());
            for (int i = 0; i < entries.size() && i < gearSlots; i++) {
                gear.setItem(i, stone(entries.get(i).techniqueId, entries.get(i).embryo));
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            for (int slot = 0; slot < 4; slot++) {
                Skill skill = PlayerBoundSkills.get(serverPlayer, slot);
                if (skill == null) {
                    continue;
                }
                List<Technique> techniques = skill.getTechniques();
                List<ShuAttribute> embryos = skill.getEmbryos();
                for (int i = 0; i < techniques.size() && i < 8; i++) {
                    ShuAttribute embryo = i < embryos.size() ? embryos.get(i) : ShuAttribute.NONE;
                    gear.setItem(slot * 8 + i, stone(techniques.get(i).getId(), embryo));
                }
            }
        }
        suppress = false;
    }

    private void writeGear() {
        if (!(player instanceof ServerPlayer serverPlayer) || suppress) {
            return;
        }
        if (staff) {
            ItemStack hand = player.getMainHandItem();
            int count = StaffSkills.count(hand);
            for (int i = count - 1; i >= 0; i--) {
                StaffSkills.remove(hand, i);
            }
            for (int i = 0; i < gearSlots; i++) {
                SkillStoneData data = SkillStoneItem.getData(gear.getItem(i));
                if (data == null || data.getTechnique() == null) {
                    continue;
                }
                if (data.getTechnique().getType() == TechniqueType.SWORD) {
                    continue;
                }
                if (!StaffSkills.add(hand, data.getSpellId(), data.getEmbryoAttribute())) {
                    break;
                }
            }
        } else {
            for (int slot = 0; slot < 4; slot++) {
                List<Technique> techniques = new ArrayList<>();
                List<ShuAttribute> embryos = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    SkillStoneData data = SkillStoneItem.getData(gear.getItem(slot * 8 + i));
                    if (data == null || data.getTechnique() == null) {
                        continue;
                    }
                    if (data.getTechnique().getType() == TechniqueType.MAGIC && !SkillCarrier.allowsMagic(player.getMainHandItem())) {
                        continue;
                    }
                    techniques.add(data.getTechnique());
                    embryos.add(data.getEmbryoAttribute());
                }
                if (techniques.isEmpty()) {
                    PlayerBoundSkills.set(serverPlayer, slot, null);
                } else {
                    PlayerBoundSkills.set(serverPlayer, slot, new Skill(techniques.get(0).getName(), SkillType.SWORD, techniques, embryos));
                }
            }
            BoundSkillSyncPacket.send(serverPlayer);
        }
    }

    private String rejectReason(ItemStack stack, int gearIndex) {
        SkillStoneData data = SkillStoneItem.getData(stack);
        if (data == null || data.getTechnique() == null) {
            return "无法装配：这不是术式石";
        }
        Technique technique = data.getTechnique();
        if (staff) {
            if (technique.getType() == TechniqueType.SWORD) {
                return "无法装配：剑术不能装入法杖";
            }
            int filled = 0;
            for (int i = 0; i < gearSlots; i++) {
                if (i != gearIndex && SkillStoneItem.getData(gear.getItem(i)) != null) {
                    filled++;
                }
            }
            if (filled >= gearSlots) {
                return "无法装配：法杖槽位已满";
            }
            return null;
        }
        if (technique.getType() == TechniqueType.MAGIC && !SkillCarrier.allowsMagic(player.getMainHandItem())) {
            return "无法装配：法术不能装入剑技槽";
        }
        int chain = gearIndex / 8;
        int cost = technique.assemblyCost();
        for (int i = 0; i < 8; i++) {
            int index = chain * 8 + i;
            if (index == gearIndex) {
                continue;
            }
            SkillStoneData other = SkillStoneItem.getData(gear.getItem(index));
            if (other != null && other.getTechnique() != null) {
                cost += other.getTechnique().assemblyCost();
            }
        }
        int level = player instanceof ServerPlayer serverPlayer ? PlayerShuData.get(serverPlayer).getSwordLevel() : 1;
        int capacity = 10 + (Math.max(level, 1) - 1) * 5;
        if (cost > capacity) {
            return "无法装配：超出剑技容量";
        }
        return null;
    }

    private static ItemStack stone(String techniqueId, ShuAttribute embryo) {
        Technique technique = TechniqueManager.getById(techniqueId);
        if (technique == null) {
            return ItemStack.EMPTY;
        }
        return SkillStoneItem.create(SkillStoneData.imprint(technique, embryo == null ? ShuAttribute.NONE : embryo));
    }

    private class StorageSlot extends Slot {
        private StorageSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return matchesFilter(getItem());
        }
    }

    private class GearSlot extends Slot {
        private final int gearIndex;

        private GearSlot(int gearIndex, int x, int y) {
            super(gear, gearIndex, x, y);
            this.gearIndex = gearIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return rejectReason(stack, gearIndex) == null;
        }

        @Override
        public void set(ItemStack stack) {
            if (!stack.isEmpty()) {
                String reason = rejectReason(stack, gearIndex);
                if (reason != null) {
                    player.displayClientMessage(Component.literal(reason), true);
                    return;
                }
            }
            super.set(stack);
            writeGear();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            writeGear();
        }
    }
}
