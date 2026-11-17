package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.item.SkillStorageData;
import com.bslx.XueXiMod.item.SkillStorageItem;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.network.BoundSkillSyncPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.SkillAssemblyPacket;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SkillAssemblyScreen extends AbstractContainerScreen<SkillAssemblyMenu> {

    private static final int PANEL = 0xFF3A3A3A;
    private static final int LEFT_BG = 0xFF1A1A1A;
    private static final int CYAN = 0xFF55FFFF;
    private static final int CYAN_SEMI = 0x6655FFFF;

    private static final int LEFT_X = 6;
    private static final int LEFT_W = 80;
    private static final int MID_X = 92;
    private static final int MID_W = 64;
    private static final int RIGHT_X = 162;
    private static final int RIGHT_W = 88;

    private static final int TITLE_Y = 5;
    private static final int CLOSE_X = 228;
    private static final int CLOSE_Y = 4;
    private static final int CLOSE_W = 16;
    private static final int CLOSE_H = 12;

    private static final int COLUMN_TOP = 20;
    private static final int COLUMN_BOTTOM = 122;

    private static final int ROW_H = 12;
    private static final int LIST_HEADER_Y = 21;
    private static final int LIST_ROWS_Y = 34;

    private static final int MID_ROW_H = 24;
    private static final int MID_ROW_STEP = 25;

    private static final int CELL = 16;
    private static final int CELL_GAP = 2;
    private static final int GRID_Y = 36;

    private static final int STAFF_GRID_X = 92;
    private static final int STAFF_GRID_MARGIN = 8;

    private static final int DETAIL_Y = 126;
    private static final int BUTTON_Y = 168;

    private static final String[] SLOT_HINTS = {"右键", "C", "V", "R"};

    private int slotCount = 4;
    private int currentSlot = 0;
    private int selectedStorage = -1;
    private int selectedTech = -1;
    private int detailMode = 0;
    private int listScroll = 0;
    private int detailY = DETAIL_Y;
    private int panelScroll;
    private int staffButtonOffset = BUTTON_Y;
    private Button putButton;
    private Button takeButton;
    private Button leftButton;
    private Button rightButton;

    public SkillAssemblyScreen(SkillAssemblyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 196;
    }

    @Override
    protected void init() {
        this.slotCount = computeSlotCount();
        int buttonY = BUTTON_Y;
        if (isStaffMode()) {
            int columns = staffColumns();
            int rows = staffRows();
            int gridBottom = GRID_Y + rows * (CELL + CELL_GAP);
            this.detailY = gridBottom + 8;
            buttonY = this.detailY + 40;
            this.imageWidth = Math.max(256, STAFF_GRID_X + columns * (CELL + CELL_GAP) + STAFF_GRID_MARGIN);
            this.imageHeight = buttonY + 28;
            this.staffButtonOffset = buttonY;
        } else if (this.slotCount > 4) {
            this.imageHeight = 200;
            buttonY = 174;
        }
        super.init();
        int screenX = leftPos;
        int screenY = topPos;
        this.putButton = Button.builder(Component.literal("放入当前槽"), ignored -> put())
                .pos(screenX + 6, screenY + buttonY).size(70, 20).build();
        this.takeButton = Button.builder(Component.literal("取出"), ignored -> remove())
                .pos(screenX + 81, screenY + buttonY).size(40, 20).build();
        this.leftButton = Button.builder(Component.literal("左移"), ignored -> moveLeft())
                .pos(screenX + 126, screenY + buttonY).size(40, 20).build();
        this.rightButton = Button.builder(Component.literal("右移"), ignored -> moveRight())
                .pos(screenX + 171, screenY + buttonY).size(40, 20).build();
        addRenderableWidget(this.putButton);
        addRenderableWidget(this.takeButton);
        addRenderableWidget(this.leftButton);
        addRenderableWidget(this.rightButton);
    }

    private List<SkillStorageData.Entry> storageEntries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return List.of();
        ItemStack storage = SkillStorageItem.find(minecraft.player);
        if (storage.isEmpty()) return List.of();
        return SkillStorageData.entries(storage);
    }

    private ItemStack mainHandItem() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
    }

    private int computeSlotCount() {
        ItemStack mainHand = mainHandItem();
        if (StaffItem.holds(mainHand)) {
            return ((StaffItem) mainHand.getItem()).data().slotCount();
        }
        return 4;
    }

    private boolean isStaffMode() {
        return StaffItem.holds(mainHandItem());
    }

    private List<StaffSkills.Entry> staffEntries() {
        return StaffSkills.entries(mainHandItem());
    }

    private Technique staffSlotTechnique(int index) {
        List<StaffSkills.Entry> entries = staffEntries();
        if (index < 0 || index >= entries.size()) return null;
        StaffSkills.Entry entry = entries.get(index);
        if (entry == null) return null;
        return TechniqueManager.getById(entry.techniqueId);
    }

    private int staffColumns() {
        if (slotCount <= 8) {
            return Math.max(1, slotCount);
        }
        return 8;
    }

    private int staffRows() {
        return Math.max(1, (slotCount + staffColumns() - 1) / staffColumns());
    }

    private void renderStaffSlots(GuiGraphics graphics) {
        List<StaffSkills.Entry> entries = staffEntries();
        int columns = staffColumns();
        for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
            int column = slotIndex % columns;
            int row = slotIndex / columns;
            int cellX = STAFF_GRID_X + column * (CELL + CELL_GAP);
            int cellY = GRID_Y + row * (CELL + CELL_GAP);
            Technique technique = null;
            if (slotIndex < entries.size() && entries.get(slotIndex) != null) {
                technique = TechniqueManager.getById(entries.get(slotIndex).techniqueId);
            }
            if (slotIndex == currentSlot) {
                graphics.fill(cellX - 1, cellY - 1, cellX + CELL + 1, cellY + CELL + 1, CYAN);
            }
            if (technique != null) {
                blitTechniqueIcon(graphics, technique, cellX, cellY, CELL);
            } else {
                graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, 0xFF2A2A2A);
            }
            graphics.drawString(font, Integer.toString(slotIndex + 1), cellX, cellY - 8, 0xFFCC88, false);
        }
    }

    private Skill slotSkill(int slot) {
        ItemStack mainHand = mainHandItem();
        if (StaffItem.holds(mainHand)) {
            return null;
        }
        return BoundSkillSyncPacket.clientSlot(slot);
    }

    private int usedCost(int slot) {
        Skill skill = slotSkill(slot);
        if (skill == null) return 0;
        int sum = 0;
        for (Technique technique : skill.getTechniques()) {
            sum += technique.assemblyCost();
        }
        return sum;
    }

    private int capacity() {
        int swordLevel = ShuSyncPacket.ClientShuCache.getSwordLevel();
        return 10 + (Math.max(swordLevel, 1) - 1) * 5;
    }

    private Technique detailTechnique() {
        if (detailMode == 1) {
            List<SkillStorageData.Entry> entries = storageEntries();
            if (selectedStorage >= 0 && selectedStorage < entries.size()) {
                return TechniqueManager.getById(entries.get(selectedStorage).spellId());
            }
        } else if (detailMode == 2) {
            if (isStaffMode()) {
                return staffSlotTechnique(currentSlot);
            }
            Skill skill = slotSkill(currentSlot);
            if (skill != null && selectedTech >= 0 && selectedTech < skill.getTechniques().size()) {
                return skill.getTechniques().get(selectedTech);
            }
        }
        return null;
    }

    private static String typeName(TechniqueType type) {
        return switch (type) {
            case SWORD -> "剑术";
            case MOVEMENT -> "动作";
            case MAGIC -> "术法";
            case UNIVERSAL -> "通用";
        };
    }

    private static String attributeName(ShuAttribute attr) {
        return switch (attr) {
            case NONE -> "无";
            case WOOD -> "木";
            case FIRE -> "火";
            case WATER -> "水";
            case EARTH -> "土";
            case METAL -> "金";
        };
    }

    private static ResourceLocation techniqueIcon(Technique technique) {
        String kind = technique.getType() == TechniqueType.SWORD ? "attack" : "other";
        ShuAttribute attr = technique.getAttribute();
        String textureKey = attr == null ? "none" : attr.getId();
        return ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "textures/gui/technique/" + kind + "_" + textureKey + ".png");
    }

    private static void blitTechniqueIcon(GuiGraphics graphics, Technique technique, int drawX, int drawY, int size) {
        graphics.blit(techniqueIcon(technique), drawX, drawY, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
    }

    private int visibleListRows() {
        return (COLUMN_BOTTOM - LIST_ROWS_Y) / ROW_H;
    }

    private int cellsPerRow() {
        return RIGHT_W / (CELL + CELL_GAP);
    }

    private int gridRows() {
        return (COLUMN_BOTTOM - GRID_Y) / (CELL + CELL_GAP);
    }

    private void clampListScroll(int size) {
        int upperBound = Math.max(0, size - visibleListRows());
        if (listScroll < 0) listScroll = 0;
        if (listScroll > upperBound) listScroll = upperBound;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + LEFT_X, topPos + COLUMN_TOP, leftPos + LEFT_X + LEFT_W, topPos + COLUMN_BOTTOM, LEFT_BG);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, isStaffMode() ? "伞的装配" : "剑技装配", imageWidth / 2, TITLE_Y, 0xFFFFFF);
        graphics.drawString(font, "×", CLOSE_X + 4, CLOSE_Y + 1, 0xFFFFFF, false);

        List<SkillStorageData.Entry> entries = storageEntries();
        clampListScroll(entries.size());
        graphics.drawString(font, "匣子成品", LEFT_X, LIST_HEADER_Y, 0xFFFF55, false);
        if (entries.isEmpty()) {
            graphics.drawString(font, "（空）", LEFT_X, LIST_ROWS_Y, 0xAAAAAA, false);
        } else {
            for (int rowIndex = 0; rowIndex < visibleListRows(); rowIndex++) {
                int index = listScroll + rowIndex;
                if (index >= entries.size()) break;
                int rowY = LIST_ROWS_Y + rowIndex * ROW_H;
                SkillStorageData.Entry entry = entries.get(index);
                Technique technique = TechniqueManager.getById(entry.spellId());
                String name = technique != null ? technique.getName() : entry.spellId();
                if (technique != null) {
                    blitTechniqueIcon(graphics, technique, LEFT_X + 1, rowY + 1, 10);
                } else {
                    graphics.fill(LEFT_X + 2, rowY + 2, LEFT_X + 10, rowY + 10, 0xFF888888);
                }
                graphics.drawString(font, name, LEFT_X + 13, rowY, index == selectedStorage ? CYAN : 0xFFFFFF, false);
            }
        }

        if (isStaffMode()) {
            renderStaffSlots(graphics);
        } else {
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
                int slotY = COLUMN_TOP + slotIndex * MID_ROW_STEP;
                if (slotIndex == currentSlot) {
                    graphics.fill(MID_X, slotY, MID_X + MID_W, slotY + MID_ROW_H, CYAN_SEMI);
                }
                String hint = slotIndex < SLOT_HINTS.length ? SLOT_HINTS[slotIndex] : "未绑定";
                graphics.drawString(font, "槽" + (slotIndex + 1) + " " + hint, MID_X + 3, slotY + 6,
                        slotIndex == currentSlot ? CYAN : 0xFFFFFF, false);
            }

            graphics.drawString(font, "已用复杂度：" + usedCost(currentSlot) + "/" + capacity(), RIGHT_X, COLUMN_TOP, CYAN, false);
            Skill skill = slotSkill(currentSlot);
            List<Technique> techs = (skill == null) ? List.of() : skill.getTechniques();
            for (int cellIndex = 0; cellIndex < gridRows() * cellsPerRow(); cellIndex++) {
                int col = cellIndex % cellsPerRow();
                int row = cellIndex / cellsPerRow();
                int cellX = RIGHT_X + col * (CELL + CELL_GAP);
                int cellY = GRID_Y + row * (CELL + CELL_GAP);
                boolean hasTech = cellIndex < techs.size();
                boolean sel = detailMode == 2 && hasTech && cellIndex == selectedTech;
                if (sel) {
                    graphics.fill(cellX - 1, cellY - 1, cellX + CELL + 1, cellY + CELL + 1, CYAN);
                }
                if (hasTech) {
                    blitTechniqueIcon(graphics, techs.get(cellIndex), cellX, cellY, CELL);
                } else {
                    graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, 0xFF2A2A2A);
                }
            }
        }

        Technique detail = detailTechnique();
        if (detail == null) {
            graphics.drawString(font, "未选择术式", LEFT_X, detailY, 0xAAAAAA, false);
        } else {
            String typeText = typeName(detail.getType());
            String attrText = attributeName(detail.getAttribute());
            graphics.drawString(font, "类型：", LEFT_X, detailY, 0xFFFFFF, false);
            graphics.drawString(font, typeText, LEFT_X + font.width("类型："), detailY, CYAN, false);
            int attributeX = LEFT_X + font.width("类型：" + typeText) + 10;
            graphics.drawString(font, "属性：", attributeX, detailY, 0xFFFFFF, false);
            graphics.drawString(font, attrText, attributeX + font.width("属性："), DETAIL_Y, CYAN, false);

            graphics.drawString(font, "描述：", LEFT_X, detailY + 12, 0xFFFFFF, false);
            graphics.drawString(font, detail.getDescription(), LEFT_X + font.width("描述："), detailY + 12, CYAN, false);

            graphics.drawString(font, "装配复杂度：", LEFT_X, detailY + 24, 0xFFFFFF, false);
            graphics.drawString(font, String.valueOf(detail.assemblyCost()),
                    LEFT_X + font.width("装配复杂度："), detailY + 24, CYAN, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = (int) mouseX - leftPos;
        int panelY = (int) mouseY - topPos;

        if (button == 0) {
            if (panelX >= CLOSE_X && panelX < CLOSE_X + CLOSE_W && panelY >= CLOSE_Y && panelY < CLOSE_Y + CLOSE_H) {
                onClose();
                return true;
            }

            List<SkillStorageData.Entry> entries = storageEntries();
            if (panelX >= LEFT_X && panelX < LEFT_X + LEFT_W && panelY >= LIST_ROWS_Y && panelY < COLUMN_BOTTOM) {
                int row = (panelY - LIST_ROWS_Y) / ROW_H;
                int index = listScroll + row;
                if (index >= 0 && index < entries.size()) {
                    selectedStorage = index;
                    selectedTech = -1;
                    detailMode = 1;
                    return true;
                }
            }

            if (isStaffMode()) {
                int columns = staffColumns();
                int rows = staffRows();
                if (panelX >= STAFF_GRID_X && panelY >= GRID_Y
                        && panelY < GRID_Y + rows * (CELL + CELL_GAP)) {
                    int column = (panelX - STAFF_GRID_X) / (CELL + CELL_GAP);
                    int row = (panelY - GRID_Y) / (CELL + CELL_GAP);
                    int slotIndex = row * columns + column;
                    if (column >= 0 && column < columns && slotIndex >= 0 && slotIndex < slotCount) {
                        if (staffSlotTechnique(slotIndex) != null) {
                            currentSlot = slotIndex;
                            detailMode = 2;
                            selectedTech = 0;
                        } else if (selectedStorage >= 0) {
                            currentSlot = slotIndex;
                            put();
                        }
                        return true;
                    }
                }
            } else {
                if (panelX >= MID_X && panelX < MID_X + MID_W && panelY >= COLUMN_TOP && panelY < COLUMN_TOP + slotCount * MID_ROW_STEP) {
                    int slotIndex = (panelY - COLUMN_TOP) / MID_ROW_STEP;
                    if (slotIndex >= 0 && slotIndex < slotCount) {
                        currentSlot = slotIndex;
                        if (detailMode == 2) {
                            detailMode = 0;
                            selectedTech = -1;
                        }
                        return true;
                    }
                }

                Skill skill = slotSkill(currentSlot);
                List<Technique> techs = (skill == null) ? List.of() : skill.getTechniques();
                if (panelX >= RIGHT_X && panelX < RIGHT_X + RIGHT_W
                        && panelY >= GRID_Y && panelY < GRID_Y + gridRows() * (CELL + CELL_GAP)) {
                    int col = (panelX - RIGHT_X) / (CELL + CELL_GAP);
                    int row = (panelY - GRID_Y) / (CELL + CELL_GAP);
                    if (col >= 0 && col < cellsPerRow() && row >= 0 && row < gridRows()) {
                        int index = row * cellsPerRow() + col;
                        if (index < techs.size()) {
                            selectedTech = index;
                            detailMode = 2;
                        } else {
                            put();
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        alignStaffPanel();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void alignStaffPanel() {
        if (!isStaffMode() || this.putButton == null) {
            return;
        }
        this.leftPos = (this.width - this.imageWidth) / 2;
        int overflow = this.imageHeight - (this.height - 8);
        if (overflow <= 0) {
            this.panelScroll = 0;
            this.topPos = (this.height - this.imageHeight) / 2;
        } else {
            this.panelScroll = Math.max(0, Math.min(overflow, this.panelScroll));
            this.topPos = 4 - this.panelScroll;
        }
        this.putButton.setX(this.leftPos + 6);
        this.takeButton.setX(this.leftPos + 81);
        this.leftButton.setX(this.leftPos + 126);
        this.rightButton.setX(this.leftPos + 171);
        int buttonTop = this.topPos + this.staffButtonOffset;
        this.putButton.setY(buttonTop);
        this.takeButton.setY(buttonTop);
        this.leftButton.setY(buttonTop);
        this.rightButton.setY(buttonTop);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isStaffMode()) {
            int overflow = this.imageHeight - (this.height - 8);
            if (overflow > 0) {
                int step = (int) Math.signum(delta) * 12;
                this.panelScroll = Math.max(0, Math.min(overflow, this.panelScroll - step));
                return true;
            }
        }
        List<SkillStorageData.Entry> entries = storageEntries();
        if (entries.size() > visibleListRows()) {
            int upperBound = Math.max(0, entries.size() - visibleListRows());
            listScroll = Math.max(0, Math.min(upperBound, listScroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void put() {
        if (selectedStorage < 0 || selectedStorage >= storageEntries().size()) return;
        ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                SkillAssemblyPacket.ACTION_PUT, currentSlot, selectedStorage, -1));
    }

    private void remove() {
        if (isStaffMode()) {
            if (currentSlot < 0 || staffSlotTechnique(currentSlot) == null) return;
            ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                    SkillAssemblyPacket.ACTION_REMOVE, currentSlot, -1, 0));
            return;
        }
        if (selectedTech < 0) return;
        ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                SkillAssemblyPacket.ACTION_REMOVE, currentSlot, -1, selectedTech));
    }

    private void moveLeft() {
        if (isStaffMode()) {
            if (currentSlot <= 0) return;
            ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                    SkillAssemblyPacket.ACTION_MOVE_LEFT, currentSlot, -1, 0));
            return;
        }
        if (selectedTech <= 0) return;
        ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                SkillAssemblyPacket.ACTION_MOVE_LEFT, currentSlot, -1, selectedTech));
    }

    private void moveRight() {
        if (isStaffMode()) {
            if (currentSlot < 0 || currentSlot >= slotCount - 1) return;
            ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                    SkillAssemblyPacket.ACTION_MOVE_RIGHT, currentSlot, -1, 0));
            return;
        }
        Skill skill = slotSkill(currentSlot);
        if (skill == null || selectedTech < 0 || selectedTech >= skill.getTechniques().size() - 1) return;
        ModNetwork.CHANNEL.sendToServer(new SkillAssemblyPacket(
                SkillAssemblyPacket.ACTION_MOVE_RIGHT, currentSlot, -1, selectedTech));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
