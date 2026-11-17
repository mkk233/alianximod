package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.item.StaffData;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.network.BoundSkillSyncPacket;
import com.bslx.XueXiMod.player.PlayerBoundSkills;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.player.PlayerSpellStorage;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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
    private final int boundHotbar;
    private final ItemStack boundStaff;
    private final boolean[] preservedChain = new boolean[4];
    private final int gearStride;
    private boolean staffLocked;
    private boolean suppress;
    private TechniqueType typeFilter;
    private ShuAttribute attrFilter;

    public SpellAssemblyMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.SPELL_ASSEMBLY.get(), containerId);
        this.player = playerInventory.player;
        this.staff = StaffItem.holds(player.getMainHandItem());
        this.boundHotbar = player.getInventory().selected;
        this.boundStaff = this.staff ? player.getMainHandItem() : ItemStack.EMPTY;
        int rawSlots = staff ? ((StaffItem) boundStaff.getItem()).data().slotCount() : 32;
        this.gearSlots = staff ? Math.max(1, Math.min(StaffData.SLOT_CAP, rawSlots)) : 32;
        this.gearStride = staff && this.gearSlots > 8 ? CELL + 10 : CELL;
        this.storage = new SimpleContainer(STORAGE);
        this.gear = new SimpleContainer(gearSlots);
        if (player instanceof ServerPlayer) {
            SimpleContainer loaded = PlayerSpellStorage.load(player);
            PlayerSpellStorage.migrate(player, loaded);
            for (int storageIndex = 0; storageIndex < STORAGE; storageIndex++) {
                storage.setItem(storageIndex, loaded.getItem(storageIndex));
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
            addSlot(new GearSlot(index, GEAR_X + col * CELL, BOTTOM_Y + row * gearStride));
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

    public int gearStride() {
        return gearStride;
    }

    public int panelWidth() {
        return GUI_W;
    }

    public int panelHeight() {
        int rows = gearRows();
        int gearBottom = BOTTOM_Y + Math.max(0, rows - 1) * gearStride + CELL;
        int inventoryBottom = BOTTOM_Y + 3 * CELL + 2 + CELL;
        return Math.max(gearBottom, inventoryBottom) + 4;
    }

    public boolean staffLocked() {
        return staffLocked;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (staff && index == boundMenuSlot()) {
            return ItemStack.EMPTY;
        }
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
        return player.isAlive()
                && SkillCarrier.canRelease(player.getItemInHand(InteractionHand.MAIN_HAND))
                && boundStaffIntact();
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player clickingPlayer) {
        if (staff && touchesBoundStaff(slotIndex, button, clickType)) {
            if (clickingPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.literal("装配时不能更换这根法杖"), true);
                serverPlayer.closeContainer();
            }
            return;
        }
        if (staff && !boundStaffIntact()) {
            if (clickingPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
            return;
        }
        if (staffLocked && slotIndex >= 0 && slotIndex < this.slots.size() && this.slots.get(slotIndex) instanceof GearSlot) {
            if (clickingPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("gui.xueximod.staff.overflow_edit"), true);
            }
            return;
        }
        if (slotIndex >= 0 && slotIndex < this.slots.size() && this.slots.get(slotIndex) instanceof GearSlot gearSlot && gearSlot.preserved()) {
            if (clickingPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.literal("这条剑技超过 8 段，未改动原数据"), true);
            }
            return;
        }
        super.clicked(slotIndex, button, clickType, clickingPlayer);
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
        for (int gearIndex = 0; gearIndex < gearSlots; gearIndex++) {
            gear.setItem(gearIndex, ItemStack.EMPTY);
        }
        if (staff) {
            if (StaffSkills.overflowHeld(boundStaff)) {
                staffLocked = true;
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(Component.translatable("gui.xueximod.staff.overflow_edit"), true);
                }
                return;
            }
            List<StaffSkills.Entry> entries = StaffSkills.entries(boundStaff);
            for (int entryIndex = 0; entryIndex < entries.size() && entryIndex < gearSlots; entryIndex++) {
                gear.setItem(entryIndex, stone(entries.get(entryIndex).techniqueId, entries.get(entryIndex).embryo));
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            for (int slot = 0; slot < 4; slot++) {
                Skill skill = PlayerBoundSkills.get(serverPlayer, slot);
                if (skill == null) {
                    continue;
                }
                List<Technique> techniques = skill.getTechniques();
                List<ShuAttribute> embryos = skill.getEmbryos();
                if (techniques.size() > Skill.CHAIN_LIMIT) {
                    preservedChain[slot] = true;
                    continue;
                }
                for (int techniqueIndex = 0; techniqueIndex < techniques.size() && techniqueIndex < Skill.CHAIN_LIMIT; techniqueIndex++) {
                    ShuAttribute embryo = techniqueIndex < embryos.size() ? embryos.get(techniqueIndex) : ShuAttribute.NONE;
                    gear.setItem(slot * 8 + techniqueIndex, stone(techniques.get(techniqueIndex).getId(), embryo));
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
            if (staffLocked || StaffSkills.overflowHeld(boundStaff)) {
                return;
            }
            if (!boundStaffIntact()) {
                serverPlayer.closeContainer();
                return;
            }
            ItemStack hand = boundStaff;
            int count = StaffSkills.count(hand);
            for (int skillIndex = count - 1; skillIndex >= 0; skillIndex--) {
                StaffSkills.remove(hand, skillIndex);
            }
            for (int gearIndex = 0; gearIndex < gearSlots; gearIndex++) {
                SkillStoneData data = SkillStoneItem.getData(gear.getItem(gearIndex));
                if (data == null || data.getTechnique() == null) {
                    continue;
                }
                if (!StaffSkills.add(hand, data.getSpellId(), data.getEmbryoAttribute())) {
                    break;
                }
            }
        } else {
            for (int slot = 0; slot < 4; slot++) {
                if (preservedChain[slot]) {
                    continue;
                }
                List<Technique> techniques = new ArrayList<>();
                List<ShuAttribute> embryos = new ArrayList<>();
                for (int slotIndex = 0; slotIndex < Skill.CHAIN_LIMIT; slotIndex++) {
                    SkillStoneData data = SkillStoneItem.getData(gear.getItem(slot * 8 + slotIndex));
                    if (data == null || data.getTechnique() == null) {
                        continue;
                    }
                    if (data.getTechnique().getType() == TechniqueType.MAGIC) {
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
        if (!TechniqueAccess.allowsPlayerUse(technique)) {
            return TechniqueAccess.DENIAL_TEXT;
        }
        if (staff) {
            if (technique.getType() == TechniqueType.SWORD) {
                return "无法装配：剑术不能装入法杖";
            }
            int filled = 0;
            for (int slotIndex = 0; slotIndex < gearSlots; slotIndex++) {
                if (slotIndex != gearIndex && SkillStoneItem.getData(gear.getItem(slotIndex)) != null) {
                    filled++;
                }
            }
            if (filled >= gearSlots) {
                return "无法装配：法杖槽位已满";
            }
            if (player instanceof ServerPlayer serverPlayer) {
                com.bslx.XueXiMod.player.PlayerShuData learned = com.bslx.XueXiMod.player.PlayerShuData.get(serverPlayer);
                if (!learned.hasLearned(technique.getId()) || !learned.isUnlocked(technique)) {
                    return "无法装配：还没有学会或解锁这个术式";
                }
            }
            return null;
        }
        if (technique.getType() == TechniqueType.MAGIC) {
            return "无法装配：法术不能装入剑技槽";
        }
        int chain = gearIndex / 8;
        int cost = technique.assemblyCost();
        for (int slotIndex = 0; slotIndex < 8; slotIndex++) {
            int index = chain * 8 + slotIndex;
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

    private boolean boundStaffIntact() {
        if (!staff) {
            return true;
        }
        ItemStack current = player.getInventory().getItem(boundHotbar);
        if (current != boundStaff || !StaffItem.holds(current)) {
            return false;
        }
        return ((StaffItem) current.getItem()).data().slotCount() == gearSlots;
    }

    public boolean retrievalReady(int gearIndex) {
        if (!staff || gearIndex < 0 || gearIndex >= gearSlots || !boundStaffIntact()) {
            return false;
        }
        return requiresRetrieval(gear.getItem(gearIndex));
    }

    public int lockedGearIndex(Slot slot) {
        if (!staff || !(slot instanceof GearSlot gearSlot) || !requiresRetrieval(gearSlot.getItem())) {
            return -1;
        }
        return gearSlot.gearIndex;
    }

    @Override
    public boolean clickMenuButton(Player clickingPlayer, int buttonId) {
        if (!(clickingPlayer instanceof ServerPlayer serverPlayer) || !staff || buttonId < 0 || buttonId >= gearSlots) {
            return false;
        }
        return retrieveLocked(serverPlayer, buttonId);
    }

    private boolean retrieveLocked(ServerPlayer serverPlayer, int gearIndex) {
        if (!boundStaffIntact()) {
            return false;
        }
        ItemStack stone = gear.getItem(gearIndex);
        if (!requiresRetrieval(stone)) {
            return false;
        }
        SkillStoneData data = SkillStoneItem.getData(stone);
        int listIndex = matchingStaffIndex(data, gearIndex);
        if (listIndex < 0) {
            return false;
        }
        ItemStack moving = stone.copy();
        moving.setCount(1);
        if (!giveEntire(serverPlayer, moving)) {
            serverPlayer.displayClientMessage(Component.literal("背包和术式收纳都已满，还留在法杖上"), true);
            return false;
        }
        StaffSkills.remove(boundStaff, listIndex);
        suppress = true;
        gear.setItem(gearIndex, ItemStack.EMPTY);
        suppress = false;
        return true;
    }

    private int matchingStaffIndex(SkillStoneData data, int gearIndex) {
        if (data == null || data.getTechnique() == null) {
            return -1;
        }
        int occurrence = 0;
        for (int earlierIndex = 0; earlierIndex < gearIndex; earlierIndex++) {
            SkillStoneData earlier = SkillStoneItem.getData(gear.getItem(earlierIndex));
            if (sameStone(data, earlier)) {
                occurrence++;
            }
        }
        java.util.List<StaffSkills.Entry> entries = StaffSkills.entries(boundStaff);
        int seen = 0;
        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            StaffSkills.Entry entry = entries.get(entryIndex);
            if (!data.getSpellId().equals(entry.techniqueId)) {
                continue;
            }
            ShuAttribute entryEmbryo = entry.embryo == null ? ShuAttribute.NONE : entry.embryo;
            if (entryEmbryo != data.getEmbryoAttribute()) {
                continue;
            }
            if (seen == occurrence) {
                return entryIndex;
            }
            seen++;
        }
        return -1;
    }

    private static boolean sameStone(SkillStoneData left, SkillStoneData right) {
        if (left == null || right == null || left.getTechnique() == null || right.getTechnique() == null) {
            return false;
        }
        return left.getSpellId().equals(right.getSpellId()) && left.getEmbryoAttribute() == right.getEmbryoAttribute();
    }

    private boolean requiresRetrieval(ItemStack stack) {
        SkillStoneData data = SkillStoneItem.getData(stack);
        if (data == null || data.getTechnique() == null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(data.getTechnique().getId());
        return definition != null && definition.requiresHeldSword();
    }

    private boolean giveEntire(ServerPlayer serverPlayer, ItemStack moving) {
        if (roomInInventory(serverPlayer, moving)) {
            ItemStack copy = moving.copy();
            int originalCount = copy.getCount();
            serverPlayer.getInventory().add(copy);
            if (copy.isEmpty()) {
                return true;
            }
            if (copy.getCount() != originalCount) {
                return false;
            }
        }
        if (!roomInStorage(moving)) {
            return false;
        }
        ItemStack leftover = storage.addItem(moving.copy());
        if (!leftover.isEmpty()) {
            return false;
        }
        PlayerSpellStorage.save(serverPlayer, storage);
        return true;
    }

    private static boolean roomInInventory(ServerPlayer serverPlayer, ItemStack moving) {
        int needed = moving.getCount();
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            ItemStack existing = serverPlayer.getInventory().getItem(slotIndex);
            if (existing.isEmpty()) {
                needed -= moving.getMaxStackSize();
            } else if (ItemStack.isSameItemSameTags(existing, moving)) {
                needed -= existing.getMaxStackSize() - existing.getCount();
            }
            if (needed <= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean roomInStorage(ItemStack moving) {
        int needed = moving.getCount();
        for (int slotIndex = 0; slotIndex < storage.getContainerSize(); slotIndex++) {
            ItemStack existing = storage.getItem(slotIndex);
            if (existing.isEmpty()) {
                needed -= moving.getMaxStackSize();
            } else if (ItemStack.isSameItemSameTags(existing, moving)) {
                needed -= existing.getMaxStackSize() - existing.getCount();
            }
            if (needed <= 0) {
                return true;
            }
        }
        return false;
    }

    private int boundMenuSlot() {
        return STORAGE + gearSlots + 27 + boundHotbar;
    }

    private boolean touchesBoundStaff(int slotIndex, int button, ClickType clickType) {
        if (slotIndex == boundMenuSlot()) {
            return true;
        }
        return clickType == ClickType.SWAP && button == boundHotbar;
    }

    private boolean preservedChain(int gearIndex) {
        int chain = gearIndex / Skill.CHAIN_LIMIT;
        return chain >= 0 && chain < preservedChain.length && preservedChain[chain];
    }

    private static ItemStack stone(String techniqueId, ShuAttribute embryo) {
        Technique technique = TechniqueManager.getById(techniqueId);
        if (technique == null) {
            return ItemStack.EMPTY;
        }
        return SkillStoneItem.create(SkillStoneData.imprint(technique, embryo == null ? ShuAttribute.NONE : embryo));
    }

    private class StorageSlot extends Slot {
        private StorageSlot(SimpleContainer container, int index, int slotX, int slotY) {
            super(container, index, slotX, slotY);
        }

        @Override
        public boolean isActive() {
            return matchesFilter(getItem());
        }
    }

    private class GearSlot extends Slot {
        private final int gearIndex;

        private GearSlot(int gearIndex, int slotX, int slotY) {
            super(gear, gearIndex, slotX, slotY);
            this.gearIndex = gearIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (staffLocked || !boundStaffIntact() || preservedChain(gearIndex)) {
                return false;
            }
            return rejectReason(stack, gearIndex) == null;
        }

        @Override
        public boolean mayPickup(Player player) {
            if (!boundStaffIntact() || preservedChain(gearIndex)) {
                return false;
            }
            return !staff || !requiresRetrieval(getItem());
        }

        private boolean preserved() {
            return preservedChain(gearIndex);
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
