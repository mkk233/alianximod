package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.item.SkillCarrier;
import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.item.StaffData;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.network.BoundSkillSyncPacket;
import com.bslx.XueXiMod.network.ChainAdmission;
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
import com.bslx.XueXiMod.shu.technique.release.SwordMulticastGate;
import com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureCarrierGate;
import com.bslx.XueXiMod.shu.technique.release.capture.ActorCaptureTexts;
import com.bslx.XueXiMod.shu.technique.release.capture.CaptureCarrierCheck;
import com.bslx.XueXiMod.shu.technique.release.capture.CaptureCarrierDecision;
import net.minecraft.nbt.CompoundTag;
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

/**
 * 玩家打开装配界面时的菜单。负责术式收纳、伞槽或四条剑技槽的摆放、过滤和写回。
 * 不负责释放、命中或扣魔力。
 */
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

    /** 服务端载入收纳并按主手伞或已绑定剑技填槽；客户端只排槽位。 */
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

    /** 空槽始终显示。有筛选时，术式石的类型和属性都要对上。 */
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

    /** 记下当前类型和属性筛选。空值表示这一项不限。 */
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

    /** 按伞槽行数把面板加高，避免装配槽和背包重叠。 */
    public int panelHeight() {
        int rows = gearRows();
        int gearBottom = BOTTOM_Y + Math.max(0, rows - 1) * gearStride + CELL;
        int inventoryBottom = BOTTOM_Y + 3 * CELL + 2 + CELL;
        return Math.max(gearBottom, inventoryBottom) + 4;
    }

    public boolean staffLocked() {
        return staffLocked;
    }

    /** 收纳、装配槽和背包之间快速移动。正在装配的那把伞不能被移走。 */
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

    /** 主手仍可释放，且打开时的那把伞还在原快捷栏并保持槽数，界面才继续有效。 */
    @Override
    public boolean stillValid(Player player) {
        return player.isAlive()
                && SkillCarrier.canRelease(player.getItemInHand(InteractionHand.MAIN_HAND))
                && boundStaffIntact();
    }

    /** 拦住更换这把伞，以及溢出锁定或超长剑技链的装配槽。 */
    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player clickingPlayer) {
        if (staff && touchesBoundStaff(slotIndex, button, clickType)) {
            if (clickingPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.literal("装配时不能更换这把伞"), true);
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

    /** 关闭时只保存术式收纳。装配槽在每次放入或取出时已经写回。 */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer) {
            PlayerSpellStorage.save(player, storage);
        }
    }

    /** 把伞条目或四条剑技填进装配槽。溢出的伞和超过段数上限的剑技只保留原数据。 */
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

    /** 先备好整份候选。伞一次替换；剑技四条都通过后才写入。跳过术法和被保留的长链。 */
    private boolean writeGear() {
        if (!(player instanceof ServerPlayer serverPlayer) || suppress) {
            return true;
        }
        if (staff) {
            if (staffLocked || StaffSkills.overflowHeld(boundStaff)) {
                return true;
            }
            if (!boundStaffIntact()) {
                serverPlayer.closeContainer();
                return true;
            }
            List<StaffSkills.Entry> candidate = candidateReplacing(ItemStack.EMPTY, -1);
            boolean related = StaffSkills.captureEdit(boundStaff)
                    || (candidate != null && StaffSkills.captureEdit(candidate));
            if (candidate == null) {
                return false;
            }
            if (!related && !ChainAdmission.permitsStoredChange(
                    techniquesOf(StaffSkills.entries(boundStaff)), techniquesOf(candidate))) {
                return false;
            }
            return StaffSkills.replaceEntriesExact(boundStaff, candidate).accepted();
        } else {
            Skill[] prepared = new Skill[4];
            boolean[] replace = new boolean[4];
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
                if (!ChainAdmission.permitsStoredChange(swordTechniques(PlayerBoundSkills.get(serverPlayer, slot)), techniques)) {
                    return false;
                }
                replace[slot] = true;
                prepared[slot] = techniques.isEmpty()
                        ? null
                        : new Skill(techniques.get(0).getName(), SkillType.SWORD, techniques, embryos);
            }
            for (int slot = 0; slot < 4; slot++) {
                if (replace[slot]) {
                    PlayerBoundSkills.set(serverPlayer, slot, prepared[slot]);
                }
            }
            BoundSkillSyncPacket.send(serverPlayer);
        }
        return true;
    }

    /** 和剑技写回相同地滤掉术法，避免旧链比较把未保存的术法石看成结构变化。 */
    private static List<Technique> swordTechniques(Skill skill) {
        List<Technique> techniques = new ArrayList<>();
        if (skill == null || skill.getTechniques() == null) {
            return techniques;
        }
        for (Technique technique : skill.getTechniques()) {
            if (technique == null || technique.getType() == TechniqueType.MAGIC) {
                continue;
            }
            techniques.add(technique);
        }
        return techniques;
    }

    /** 检查这枚术式石能否放进指定装配槽。能放则返回空。 */
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
            List<StaffSkills.Entry> candidate = candidateReplacing(stack, gearIndex);
            if (candidate == null) {
                return "伞条目无法识别，没有装配";
            }
            if (StaffSkills.captureEdit(boundStaff) || StaffSkills.captureEdit(candidate)) {
                return captureReject(candidate);
            }
            String staffRefusal = ChainAdmission.refusal(techniquesOf(candidate));
            if (staffRefusal != null) {
                return "无法装配：" + staffRefusal;
            }
            if (technique.getType() == TechniqueType.SWORD) {
                return "无法装配：剑术不能装入伞";
            }
            int filled = 0;
            for (int slotIndex = 0; slotIndex < gearSlots; slotIndex++) {
                if (slotIndex != gearIndex && SkillStoneItem.getData(gear.getItem(slotIndex)) != null) {
                    filled++;
                }
            }
            if (filled >= gearSlots) {
                return "无法装配：伞的槽位已满";
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
            return "无法装配：术法不能装入剑技槽";
        }
        if (SwordMulticastGate.restrictsSword(technique)) {
            return SwordMulticastGate.LIMIT_KEY;
        }
        String mixedRefusal = ChainAdmission.refusal(chainWith(technique, gearIndex));
        if (mixedRefusal != null) {
            return "无法装配：" + mixedRefusal;
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

    /** 按捕获载体规则检查整条伞候选。草稿或就绪才放行。 */
    private String captureReject(List<StaffSkills.Entry> candidate) {
        if (candidate.size() > gearSlots) {
            return "无法装配：伞的槽位已满";
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerShuData learned = PlayerShuData.get(serverPlayer);
            for (StaffSkills.Entry entry : candidate) {
                Technique listed = TechniqueManager.getById(entry.techniqueId);
                if (listed == null) {
                    return ActorCaptureTexts.UNKNOWN_ENTRY;
                }
                if (!TechniqueAccess.allowsPlayerUse(listed)) {
                    return TechniqueAccess.DENIAL_TEXT;
                }
                if (!learned.hasLearned(listed.getId()) || !learned.isUnlocked(listed)) {
                    return "无法装配：还没有学会或解锁这个术式";
                }
            }
        }
        if (candidate.isEmpty()) {
            return null;
        }
        Skill skill = StaffSkills.skillFromEntries(candidate);
        if (skill == null || skill.getTechniques().size() != candidate.size()) {
            return ActorCaptureTexts.CHAIN_MISMATCH;
        }
        CaptureCarrierCheck checked = ActorCaptureCarrierGate.inspect(skill, true, true);
        if (checked.decision() == CaptureCarrierDecision.DRAFT
                || checked.decision() == CaptureCarrierDecision.READY) {
            return null;
        }
        if (checked.decision() == CaptureCarrierDecision.NOT_APPLICABLE) {
            for (Technique listed : skill.getTechniques()) {
                if (listed != null && listed.getType() == TechniqueType.SWORD) {
                    return "无法装配：剑术不能装入伞";
                }
            }
            String mixedRefusal = ChainAdmission.refusal(skill.getTechniques());
            if (mixedRefusal != null) {
                return "无法装配：" + mixedRefusal;
            }
            return null;
        }
        return checked.playerText().isBlank() ? ActorCaptureTexts.STRUCTURE : checked.playerText();
    }

    private List<StaffSkills.Entry> candidateReplacing(ItemStack stack, int replacedIndex) {
        List<StaffSkills.Entry> candidate = new ArrayList<>();
        for (int gearCursor = 0; gearCursor < gearSlots; gearCursor++) {
            ItemStack stone = gearCursor == replacedIndex ? stack : gear.getItem(gearCursor);
            if (stone == null || stone.isEmpty()) {
                continue;
            }
            if (!stoneEmbryoRecognized(stone)) {
                return null;
            }
            SkillStoneData data = SkillStoneItem.getData(stone);
            if (data == null || data.getTechnique() == null) {
                if (stone.getItem() instanceof SkillStoneItem) {
                    return null;
                }
                continue;
            }
            ShuAttribute embryo = data.getEmbryoAttribute() == null ? ShuAttribute.NONE : data.getEmbryoAttribute();
            candidate.add(new StaffSkills.Entry(data.getSpellId(), embryo));
        }
        return candidate;
    }

    /** 用这枚石头替换指定格后，按当前槽位顺序得到完整原始链。 */
    private List<Technique> chainWith(Technique incoming, int gearIndex) {
        List<Technique> proposed = new ArrayList<>();
        int chain = gearIndex / Skill.CHAIN_LIMIT;
        int chainStart = chain * Skill.CHAIN_LIMIT;
        for (int slotIndex = 0; slotIndex < Skill.CHAIN_LIMIT; slotIndex++) {
            int listedIndex = chainStart + slotIndex;
            ItemStack listed = listedIndex == gearIndex ? ItemStack.EMPTY : gear.getItem(listedIndex);
            SkillStoneData data = SkillStoneItem.getData(listed);
            if (data != null && data.getTechnique() != null) {
                proposed.add(data.getTechnique());
            }
            if (listedIndex == gearIndex && incoming != null) {
                proposed.add(incoming);
            }
        }
        return proposed;
    }

    /** 把伞候选还原成原始术式链。无法识别的编号不进入校验。 */
    private static List<Technique> techniquesOf(List<StaffSkills.Entry> candidate) {
        List<Technique> techniques = new ArrayList<>();
        if (candidate == null) {
            return techniques;
        }
        for (StaffSkills.Entry entry : candidate) {
            Technique listed = TechniqueManager.getById(entry.techniqueId);
            if (listed != null) {
                techniques.add(listed);
            }
        }
        return techniques;
    }

    private static boolean stoneEmbryoRecognized(ItemStack stone) {
        if (stone == null || stone.isEmpty() || !(stone.getItem() instanceof SkillStoneItem)) {
            return true;
        }
        CompoundTag tag = stone.getTag();
        if (tag == null || !tag.contains(SkillStoneItem.DATA_KEY)) {
            return false;
        }
        CompoundTag data = tag.getCompound(SkillStoneItem.DATA_KEY);
        if (!data.contains("embryo_attribute")) {
            return true;
        }
        return StaffSkills.knownEmbryoId(data.getString("embryo_attribute"));
    }

    private void announceCapturedDraft() {
        if (!staff) {
            return;
        }
        String notice = StaffSkills.savedChainNotice(boundStaff);
        if (notice != null && !notice.isBlank()) {
            player.displayClientMessage(Component.literal(notice), true);
        }
    }

    private void refundUnstored(ItemStack incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            setCarried(incoming.copy());
            return;
        }
        if (ItemStack.isSameItemSameTags(carried, incoming)
                && carried.getCount() + incoming.getCount() <= carried.getMaxStackSize()) {
            carried.grow(incoming.getCount());
            return;
        }
        if (!player.getInventory().add(incoming.copy()) && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.drop(incoming.copy(), false);
        }
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

    /** 伞上这一格是否该用单独取回，而不是普通拖出。 */
    public boolean retrievalReady(int gearIndex) {
        if (!staff || gearIndex < 0 || gearIndex >= gearSlots || !boundStaffIntact()) {
            return false;
        }
        return requiresRetrieval(gear.getItem(gearIndex));
    }

    /** 装配槽需要单独取回时返回它的下标，否则为 -1。 */
    public int lockedGearIndex(Slot slot) {
        if (!staff || !(slot instanceof GearSlot gearSlot) || !requiresRetrieval(gearSlot.getItem())) {
            return -1;
        }
        return gearSlot.gearIndex;
    }

    /** 按钮号就是伞槽下标。只把必须单独取回的术式石交给背包或收纳。 */
    @Override
    public boolean clickMenuButton(Player clickingPlayer, int buttonId) {
        if (!(clickingPlayer instanceof ServerPlayer serverPlayer) || !staff || buttonId < 0 || buttonId >= gearSlots) {
            return false;
        }
        return retrieveLocked(serverPlayer, buttonId);
    }

    /** 整份交出后才从伞和装配槽拿掉。放不下就保留原处。 */
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
            serverPlayer.displayClientMessage(Component.literal("背包和术式收纳都已满，还留在伞上"), true);
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

    /** 释放定义要求手持剑时，这枚石头不能当普通物品拖出伞槽。 */
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

        /** 筛选没对上的收纳格不显示。 */
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

        /** 溢出锁定、伞已换掉或这条剑技被保留时不能放入。 */
        @Override
        public boolean mayPlace(ItemStack stack) {
            if (staffLocked || !boundStaffIntact() || preservedChain(gearIndex)) {
                return false;
            }
            return rejectReason(stack, gearIndex) == null;
        }

        /** 被保留的剑技链和必须单独取回的伞槽不能直接拿起。 */
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

        /** 先拒绝不合法的石头；写回失败则恢复原槽，不把失败结果留在伞或剑技上。 */
        @Override
        public void set(ItemStack stack) {
            if (!stack.isEmpty()) {
                String reason = rejectReason(stack, gearIndex);
                if (reason != null) {
                    Component shown = SwordMulticastGate.LIMIT_KEY.equals(reason)
                            ? Component.translatable(reason)
                            : Component.literal(reason);
                    player.displayClientMessage(shown, true);
                    if (staff) {
                        refundUnstored(stack);
                    }
                    return;
                }
            }
            ItemStack previous = getItem().copy();
            super.set(stack);
            if (!writeGear()) {
                super.set(previous);
                if (previous.isEmpty()) {
                    refundUnstored(stack);
                } else if (!player.getInventory().add(stack.copy()) && player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.drop(stack.copy(), false);
                }
                player.displayClientMessage(Component.literal(ActorCaptureTexts.WRITE_KEPT), true);
                return;
            }
            announceCapturedDraft();
        }

        /** 取出后写回失败时把物品放回槽位，并收回已经拿到鼠标上的份额。 */
        @Override
        public void onTake(Player takingPlayer, ItemStack taken) {
            super.onTake(takingPlayer, taken);
            if (!writeGear()) {
                if (getItem().isEmpty() && taken != null && !taken.isEmpty()) {
                    super.set(taken.copy());
                }
                ItemStack carried = getCarried();
                if (taken != null && !taken.isEmpty() && ItemStack.isSameItemSameTags(carried, taken)) {
                    int nextCount = carried.getCount() - taken.getCount();
                    if (nextCount <= 0) {
                        setCarried(ItemStack.EMPTY);
                    } else {
                        carried.setCount(nextCount);
                    }
                }
                takingPlayer.displayClientMessage(Component.literal(ActorCaptureTexts.WRITE_KEPT), true);
                return;
            }
            announceCapturedDraft();
        }
    }
}
