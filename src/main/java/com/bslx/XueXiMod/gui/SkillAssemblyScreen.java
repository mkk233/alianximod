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

    public SkillAssemblyScreen(SkillAssemblyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        this.imageHeight = 196;
    }

    @Override
    protected void init() {
        this.slotCount = computeSlotCount();
        if (this.slotCount > 4) {
            this.imageHeight = 200;
        }
        if (isStaffMode()) {
            this.imageWidth = Math.max(256, STAFF_GRID_X + this.slotCount * (CELL + CELL_GAP) + STAFF_GRID_MARGIN);
        }
        super.init();
        int x = leftPos;
        int y = topPos;
        int buttonY = this.slotCount > 4 ? 174 : BUTTON_Y;
        addRenderableWidget(Button.builder(Component.literal("放入当前槽"), b -> put())
                .pos(x + 6, y + buttonY).size(70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取出"), b -> remove())
                .pos(x + 81, y + buttonY).size(40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("左移"), b -> moveLeft())
                .pos(x + 126, y + buttonY).size(40, 20).build());
        addRenderableWidget(Button.builder(Component.literal("右移"), b -> moveRight())
                .pos(x + 171, y + buttonY).size(40, 20).build());
    }

    private List<SkillStorageData.Entry> storageEntries() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return List.of();
        ItemStack storage = SkillStorageItem.find(mc.player);
        if (storage.isEmpty()) return List.of();
        return SkillStorageData.entries(storage);
    }

    private ItemStack mainHandItem() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
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
        StaffSkills.Entry e = entries.get(index);
        if (e == null) return null;
        return TechniqueManager.getById(e.techniqueId);
    }

    private void renderStaffSlots(GuiGraphics g) {
        List<StaffSkills.Entry> entries = staffEntries();
        for (int i = 0; i < slotCount; i++) {
            int cx = STAFF_GRID_X + i * (CELL + CELL_GAP);
            int cy = GRID_Y;
            Technique t = null;
            if (i < entries.size() && entries.get(i) != null) {
                t = TechniqueManager.getById(entries.get(i).techniqueId);
            }
            if (i == currentSlot) {
                g.fill(cx - 1, cy - 1, cx + CELL + 1, cy + CELL + 1, CYAN);
            }
            if (t != null) {
                blitTechniqueIcon(g, t, cx, cy, CELL);
            } else {
                g.fill(cx, cy, cx + CELL, cy + CELL, 0xFF2A2A2A);
            }
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
        for (Technique t : skill.getTechniques()) {
            sum += t.assemblyCost();
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
            case MAGIC -> "法术";
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
        String id = attr == null ? "none" : attr.getId();
        return ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "textures/gui/technique/" + kind + "_" + id + ".png");
    }

    private static void blitTechniqueIcon(GuiGraphics g, Technique technique, int x, int y, int size) {
        g.blit(techniqueIcon(technique), x, y, size, size, 0.0F, 0.0F, 32, 32, 32, 32);
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
        int max = Math.max(0, size - visibleListRows());
        if (listScroll < 0) listScroll = 0;
        if (listScroll > max) listScroll = max;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        g.fill(leftPos + LEFT_X, topPos + COLUMN_TOP, leftPos + LEFT_X + LEFT_W, topPos + COLUMN_BOTTOM, LEFT_BG);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawCenteredString(font, isStaffMode() ? "法杖装配" : "剑技装配", imageWidth / 2, TITLE_Y, 0xFFFFFF);
        g.drawString(font, "×", CLOSE_X + 4, CLOSE_Y + 1, 0xFFFFFF, false);

        List<SkillStorageData.Entry> entries = storageEntries();
        clampListScroll(entries.size());
        g.drawString(font, "匣子成品", LEFT_X, LIST_HEADER_Y, 0xFFFF55, false);
        if (entries.isEmpty()) {
            g.drawString(font, "（空）", LEFT_X, LIST_ROWS_Y, 0xAAAAAA, false);
        } else {
            for (int i = 0; i < visibleListRows(); i++) {
                int index = listScroll + i;
                if (index >= entries.size()) break;
                int y = LIST_ROWS_Y + i * ROW_H;
                SkillStorageData.Entry e = entries.get(index);
                Technique t = TechniqueManager.getById(e.spellId());
                String name = t != null ? t.getName() : e.spellId();
                if (t != null) {
                    blitTechniqueIcon(g, t, LEFT_X + 1, y + 1, 10);
                } else {
                    g.fill(LEFT_X + 2, y + 2, LEFT_X + 10, y + 10, 0xFF888888);
                }
                g.drawString(font, name, LEFT_X + 13, y, index == selectedStorage ? CYAN : 0xFFFFFF, false);
            }
        }

        if (isStaffMode()) {
            renderStaffSlots(g);
        } else {
            for (int s = 0; s < slotCount; s++) {
                int ry = COLUMN_TOP + s * MID_ROW_STEP;
                if (s == currentSlot) {
                    g.fill(MID_X, ry, MID_X + MID_W, ry + MID_ROW_H, CYAN_SEMI);
                }
                String hint = s < SLOT_HINTS.length ? SLOT_HINTS[s] : "未绑定";
                g.drawString(font, "槽" + (s + 1) + " " + hint, MID_X + 3, ry + 6,
                        s == currentSlot ? CYAN : 0xFFFFFF, false);
            }

            g.drawString(font, "已用复杂度：" + usedCost(currentSlot) + "/" + capacity(), RIGHT_X, COLUMN_TOP, CYAN, false);
            Skill skill = slotSkill(currentSlot);
            List<Technique> techs = (skill == null) ? List.of() : skill.getTechniques();
            for (int i = 0; i < gridRows() * cellsPerRow(); i++) {
                int col = i % cellsPerRow();
                int row = i / cellsPerRow();
                int cx = RIGHT_X + col * (CELL + CELL_GAP);
                int cy = GRID_Y + row * (CELL + CELL_GAP);
                boolean hasTech = i < techs.size();
                boolean sel = detailMode == 2 && hasTech && i == selectedTech;
                if (sel) {
                    g.fill(cx - 1, cy - 1, cx + CELL + 1, cy + CELL + 1, CYAN);
                }
                if (hasTech) {
                    blitTechniqueIcon(g, techs.get(i), cx, cy, CELL);
                } else {
                    g.fill(cx, cy, cx + CELL, cy + CELL, 0xFF2A2A2A);
                }
            }
        }

        Technique detail = detailTechnique();
        if (detail == null) {
            g.drawString(font, "未选择术式", LEFT_X, DETAIL_Y, 0xAAAAAA, false);
        } else {
            String typeVal = typeName(detail.getType());
            String attrVal = attributeName(detail.getAttribute());
            g.drawString(font, "类型：", LEFT_X, DETAIL_Y, 0xFFFFFF, false);
            g.drawString(font, typeVal, LEFT_X + font.width("类型："), DETAIL_Y, CYAN, false);
            int ax = LEFT_X + font.width("类型：" + typeVal) + 10;
            g.drawString(font, "属性：", ax, DETAIL_Y, 0xFFFFFF, false);
            g.drawString(font, attrVal, ax + font.width("属性："), DETAIL_Y, CYAN, false);

            g.drawString(font, "描述：", LEFT_X, DETAIL_Y + 12, 0xFFFFFF, false);
            g.drawString(font, detail.getDescription(), LEFT_X + font.width("描述："), DETAIL_Y + 12, CYAN, false);

            g.drawString(font, "装配复杂度：", LEFT_X, DETAIL_Y + 24, 0xFFFFFF, false);
            g.drawString(font, String.valueOf(detail.assemblyCost()),
                    LEFT_X + font.width("装配复杂度："), DETAIL_Y + 24, CYAN, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gx = (int) mouseX - leftPos;
        int gy = (int) mouseY - topPos;

        if (button == 0) {
            if (gx >= CLOSE_X && gx < CLOSE_X + CLOSE_W && gy >= CLOSE_Y && gy < CLOSE_Y + CLOSE_H) {
                onClose();
                return true;
            }

            List<SkillStorageData.Entry> entries = storageEntries();
            if (gx >= LEFT_X && gx < LEFT_X + LEFT_W && gy >= LIST_ROWS_Y && gy < COLUMN_BOTTOM) {
                int row = (gy - LIST_ROWS_Y) / ROW_H;
                int index = listScroll + row;
                if (index >= 0 && index < entries.size()) {
                    selectedStorage = index;
                    selectedTech = -1;
                    detailMode = 1;
                    return true;
                }
            }

            if (isStaffMode()) {
                if (gx >= STAFF_GRID_X && gy >= GRID_Y && gy < GRID_Y + CELL) {
                    int i = (gx - STAFF_GRID_X) / (CELL + CELL_GAP);
                    if (i >= 0 && i < slotCount) {
                        if (staffSlotTechnique(i) != null) {
                            currentSlot = i;
                            detailMode = 2;
                            selectedTech = 0;
                        } else if (selectedStorage >= 0) {
                            currentSlot = i;
                            put();
                        }
                        return true;
                    }
                }
            } else {
                if (gx >= MID_X && gx < MID_X + MID_W && gy >= COLUMN_TOP && gy < COLUMN_TOP + slotCount * MID_ROW_STEP) {
                    int s = (gy - COLUMN_TOP) / MID_ROW_STEP;
                    if (s >= 0 && s < slotCount) {
                        currentSlot = s;
                        if (detailMode == 2) {
                            detailMode = 0;
                            selectedTech = -1;
                        }
                        return true;
                    }
                }

                Skill skill = slotSkill(currentSlot);
                List<Technique> techs = (skill == null) ? List.of() : skill.getTechniques();
                if (gx >= RIGHT_X && gx < RIGHT_X + RIGHT_W
                        && gy >= GRID_Y && gy < GRID_Y + gridRows() * (CELL + CELL_GAP)) {
                    int col = (gx - RIGHT_X) / (CELL + CELL_GAP);
                    int row = (gy - GRID_Y) / (CELL + CELL_GAP);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<SkillStorageData.Entry> entries = storageEntries();
        if (entries.size() > visibleListRows()) {
            int max = Math.max(0, entries.size() - visibleListRows());
            listScroll = Math.max(0, Math.min(max, listScroll - (int) Math.signum(delta)));
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
