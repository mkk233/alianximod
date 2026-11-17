package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.shu.balance.BalanceDisplay;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.network.HeldSwordGate;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SpellAssemblyScreen extends AbstractContainerScreen<SpellAssemblyMenu> {
    private static final int PREVIEW_X = SpellAssemblyMenu.PREVIEW_X;
    private static final int PREVIEW_Y = 6;

    private static final int PANEL = 0xFF1E1E1E;
    private static final int FRAME = 0xFF080808;
    private static final int FILTER_BG = 0xFF2B2B2B;
    private static final int LIST_BG = 0xFF171717;
    private static final int INV_BG = 0xFF232323;
    private static final int GEAR_BG = 0xFF1A1A1A;
    private static final int PREVIEW_BG = 0xFF2E2E2E;
    private static final int SLOT_FILL = 0xFF0D0D0D;
    private static final int SLOT_BORDER = 0xFF3C3C3C;

    private static final int BTN_LEFT = 6;
    private static final int BTN_STEP = 32;
    private static final int BTN_W = 30;
    private static final int ATTR_STEP = 26;
    private static final int ATTR_W = 24;
    private static final int BTN_H = 14;
    private static final int ROW1_Y = 6;
    private static final int ROW2_Y = 22;
    private static final int BTN_NORMAL = 0xFF3A3A3A;
    private static final int BTN_SELECTED = 0xFF6B9E3E;
    private static final int RETRIEVE_W = 36;
    private static final int RETRIEVE_H = 14;

    private Button retrieveButton;
    private int selectedGear = -1;
    private int panelScroll;

    private static final String[] TYPE_LABELS = {"全部", "剑术", "术法", "动作", "常用"};
    private static final TechniqueType[] TYPE_VALUES =
            {null, TechniqueType.SWORD, TechniqueType.MAGIC, TechniqueType.MOVEMENT, TechniqueType.UNIVERSAL};
    private static final String[] ATTR_LABELS = {"无", "木", "火", "水", "土", "金"};
    private static final ShuAttribute[] ATTR_VALUES =
            {ShuAttribute.NONE, ShuAttribute.WOOD, ShuAttribute.FIRE, ShuAttribute.WATER, ShuAttribute.EARTH, ShuAttribute.METAL};

    public SpellAssemblyScreen(SpellAssemblyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = menu.panelWidth();
        this.imageHeight = menu.panelHeight();
        this.titleLabelY = -999;
        this.inventoryLabelY = -999;
    }

    @Override
    protected void init() {
        super.init();
        this.retrieveButton = Button.builder(Component.literal("取回"), pressed -> {
            if (this.selectedGear >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.selectedGear);
            }
        }).bounds(0, 0, RETRIEVE_W, RETRIEVE_H).build();
        this.retrieveButton.visible = false;
        this.retrieveButton.active = false;
        addRenderableWidget(this.retrieveButton);
    }

    @Override
    public void removed() {
        this.selectedGear = -1;
        super.removed();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        int width = this.menu.panelWidth();
        int height = this.menu.panelHeight();
        graphics.fill(left, top, left + width, top + height, PANEL);
        graphics.fill(left, top, left + width, top + 2, FRAME);
        graphics.fill(left, top + height - 2, left + width, top + height, FRAME);
        graphics.fill(left, top, left + 2, top + height, FRAME);
        graphics.fill(left + width - 2, top, left + width, top + height, FRAME);
        graphics.fill(left + 3, top + 3, left + SpellAssemblyMenu.GEAR_X - 2, top + SpellAssemblyMenu.LIST_Y - 1, FILTER_BG);
        graphics.fill(left + 3, top + SpellAssemblyMenu.LIST_Y - 1, left + SpellAssemblyMenu.GEAR_X - 2, top + SpellAssemblyMenu.BOTTOM_Y - 1, LIST_BG);
        graphics.fill(left + 3, top + SpellAssemblyMenu.BOTTOM_Y - 1, left + SpellAssemblyMenu.GEAR_X - 2, top + height - 3, INV_BG);
        graphics.fill(left + SpellAssemblyMenu.GEAR_X - 2, top + SpellAssemblyMenu.BOTTOM_Y - 1,
                left + SpellAssemblyMenu.PREVIEW_X - 2, top + height - 3, GEAR_BG);
        graphics.fill(left + SpellAssemblyMenu.PREVIEW_X - 2, top + 3, left + width - 3, top + height - 3, PREVIEW_BG);
        for (Slot slot : this.menu.slots) {
            drawSlotBox(graphics, left + slot.x, top + slot.y);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        placePanel();
        syncRetrieveButton();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFilterButtons(graphics);
        renderStaffOrder(graphics);
        renderPreview(graphics, mouseX, mouseY);
        renderStaffHover(graphics, mouseX, mouseY);
        if (this.selectedGear >= 0) {
            int hintX = this.leftPos + PREVIEW_X;
            int hintY = this.topPos + PREVIEW_Y + 78 + 96 + RETRIEVE_H + 4;
            graphics.drawString(this.font, HeldSwordGate.STAFF_HINT, hintX, hintY, 0xFFCC88, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int overflow = this.imageHeight - (this.height - 8);
        if (overflow > 0) {
            int step = (int) Math.signum(delta) * 12;
            this.panelScroll = Math.max(0, Math.min(overflow, this.panelScroll - step));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void placePanel() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        int overflow = this.imageHeight - (this.height - 8);
        if (overflow <= 0) {
            this.panelScroll = 0;
            this.topPos = (this.height - this.imageHeight) / 2;
            return;
        }
        if (this.panelScroll > overflow) {
            this.panelScroll = overflow;
        }
        this.topPos = 4 - this.panelScroll;
    }

    private void renderStaffOrder(GuiGraphics graphics) {
        if (!this.menu.staff()) {
            return;
        }
        int columns = 8;
        for (int gearIndex = 0; gearIndex < this.menu.gearSlotCount(); gearIndex++) {
            Slot slot = this.menu.slots.get(SpellAssemblyMenu.STORAGE + gearIndex);
            int numberX = this.leftPos + slot.x;
            int numberY = this.topPos + slot.y - 9;
            if (this.menu.gearStride() <= SpellAssemblyMenu.CELL) {
                numberY = this.topPos + slot.y + 9;
            }
            graphics.drawString(this.font, Integer.toString(gearIndex + 1), numberX, numberY, 0xFFCC88, false);
            int column = gearIndex % columns;
            int row = gearIndex / columns;
            if (column < columns - 1 && gearIndex + 1 < this.menu.gearSlotCount()) {
                graphics.drawString(this.font, ">", this.leftPos + slot.x + 12, this.topPos + slot.y + 4, 0x88CCFF, false);
            }
            if (column == columns - 1 && row < this.menu.gearRows() - 1) {
                graphics.drawString(this.font, "v", this.leftPos + slot.x + 4, this.topPos + slot.y + SpellAssemblyMenu.CELL, 0x88CCFF, false);
            }
        }
    }

    private void renderStaffHover(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.menu.staff()) {
            return;
        }
        Slot hovered = this.slotAt(mouseX, mouseY);
        if (hovered == null) {
            return;
        }
        int menuIndex = this.menu.slots.indexOf(hovered);
        if (menuIndex < SpellAssemblyMenu.STORAGE
                || menuIndex >= SpellAssemblyMenu.STORAGE + this.menu.gearSlotCount()) {
            return;
        }
        int shownIndex = menuIndex - SpellAssemblyMenu.STORAGE;
        SkillStoneData data = SkillStoneItem.getData(hovered.getItem());
        String techniqueName = "";
        String branchText = "";
        if (data != null && data.getTechnique() != null) {
            Technique technique = data.getTechnique();
            techniqueName = technique.getName();
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            ChainRole role = definition == null ? ChainRole.ATTACK : definition.role();
            branchText = Component.translatable(role.languageKey()).getString();
        }
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("gui.xueximod.staff.slot_line", shownIndex + 1, techniqueName));
        if (!branchText.isEmpty()) {
            lines.add(Component.translatable("gui.xueximod.staff.branch", branchText));
        }
        graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int buttonIndex = buttonAt(mouseX, mouseY, TYPE_LABELS.length, ROW1_Y, BTN_STEP, BTN_W);
            if (buttonIndex >= 0) {
                if (buttonIndex == 0) {
                    this.menu.setFilters(null, null);
                } else {
                    this.menu.setFilters(TYPE_VALUES[buttonIndex], this.menu.currentAttribute());
                }
                return true;
            }
            buttonIndex = buttonAt(mouseX, mouseY, ATTR_LABELS.length, ROW2_Y, ATTR_STEP, ATTR_W);
            if (buttonIndex >= 0) {
                this.menu.setFilters(this.menu.currentType(), ATTR_VALUES[buttonIndex]);
                return true;
            }
            Slot clicked = this.slotAt((int) mouseX, (int) mouseY);
            if (clicked != null) {
                int gearIndex = this.menu.lockedGearIndex(clicked);
                if (gearIndex >= 0) {
                    this.selectedGear = gearIndex;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderFilterButtons(GuiGraphics graphics) {
        int typeSel = -1;
        TechniqueType type = this.menu.currentType();
        if (type == null) {
            typeSel = 0;
        } else {
            for (int typeIndex = 1; typeIndex < TYPE_VALUES.length; typeIndex++) {
                if (TYPE_VALUES[typeIndex] == type) {
                    typeSel = typeIndex;
                    break;
                }
            }
        }
        int attrSel = -1;
        ShuAttribute attr = this.menu.currentAttribute();
        if (attr != null) {
            for (int attrIndex = 0; attrIndex < ATTR_VALUES.length; attrIndex++) {
                if (ATTR_VALUES[attrIndex] == attr) {
                    attrSel = attrIndex;
                    break;
                }
            }
        }
        for (int typeIndex = 0; typeIndex < TYPE_LABELS.length; typeIndex++) {
            drawFilterButton(graphics, typeIndex, ROW1_Y, TYPE_LABELS[typeIndex], typeIndex == typeSel, BTN_STEP, BTN_W);
        }
        for (int attrIndex = 0; attrIndex < ATTR_LABELS.length; attrIndex++) {
            drawFilterButton(graphics, attrIndex, ROW2_Y, ATTR_LABELS[attrIndex], attrIndex == attrSel, ATTR_STEP, ATTR_W);
        }
    }

    private void drawFilterButton(GuiGraphics graphics, int index, int rowY, String label, boolean selected, int step, int width) {
        int buttonX = this.leftPos + BTN_LEFT + index * step;
        int buttonY = this.topPos + rowY;
        graphics.fill(buttonX, buttonY, buttonX + width, buttonY + BTN_H, selected ? BTN_SELECTED : BTN_NORMAL);
        int textX = buttonX + (width - this.font.width(label)) / 2;
        int textY = buttonY + (BTN_H - this.font.lineHeight) / 2;
        graphics.drawString(this.font, label, textX, textY, selected ? 0xFFFFFF : 0xDDDDDD, false);
    }

    private int buttonAt(double mouseX, double mouseY, int count, int rowY, int step, int width) {
        for (int buttonIndex = 0; buttonIndex < count; buttonIndex++) {
            int buttonX = this.leftPos + BTN_LEFT + buttonIndex * step;
            int buttonY = this.topPos + rowY;
            if (mouseX >= buttonX && mouseX < buttonX + width && mouseY >= buttonY && mouseY < buttonY + BTN_H) {
                return buttonIndex;
            }
        }
        return -1;
    }

    private void drawSlotBox(GuiGraphics graphics, int slotX, int slotY) {
        graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT_FILL);
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY, SLOT_BORDER);
        graphics.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, SLOT_BORDER);
        graphics.fill(slotX - 1, slotY, slotX, slotY + 16, SLOT_BORDER);
        graphics.fill(slotX + 16, slotY, slotX + 17, slotY + 16, SLOT_BORDER);
    }

    private void syncRetrieveButton() {
        if (this.retrieveButton == null) {
            return;
        }
        if (this.selectedGear >= 0 && !this.menu.retrievalReady(this.selectedGear)) {
            this.selectedGear = -1;
        }
        boolean ready = this.selectedGear >= 0;
        this.retrieveButton.visible = ready;
        this.retrieveButton.active = ready;
        if (ready) {
            this.retrieveButton.setX(this.leftPos + PREVIEW_X);
            this.retrieveButton.setY(this.topPos + PREVIEW_Y + 78 + 96);
        }
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        Slot hovered = this.slotAt(mouseX, mouseY);
        if (hovered == null || !hovered.hasItem()) {
            return;
        }
        SkillStoneData data = SkillStoneItem.getData(hovered.getItem());
        if (data == null || data.getTechnique() == null) {
            return;
        }
        Technique technique = data.getTechnique();
        int textX = this.leftPos + PREVIEW_X;
        int textY = this.topPos + PREVIEW_Y + 78;
        int iconX = this.leftPos + PREVIEW_X + 28;
        int iconY = this.topPos + PREVIEW_Y + 28;
        graphics.pose().pushPose();
        graphics.pose().translate(iconX, iconY, 0.0F);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.renderItem(hovered.getItem(), 0, 0);
        graphics.pose().popPose();
        graphics.drawString(this.font, "名称 " + technique.getName(), textX, textY, 0xFFFFFF, false);
        graphics.drawString(this.font, "类型 " + typeName(technique), textX, textY + 12, 0xFFFFFF, false);
        graphics.drawString(this.font, "属性 " + attrName(technique.getAttribute()), textX, textY + 24, 0xFFFFFF, false);
        graphics.drawString(this.font, "伤害 " + trim(BalanceDisplay.damage(technique.getId(), technique.getBaseDamage())), textX, textY + 36, 0xFFFFFF, false);
        graphics.drawString(this.font, "冷却 " + BalanceDisplay.cooldown(technique.getId(), technique.getBaseCooldown()), textX, textY + 48, 0xFFFFFF, false);
        graphics.drawString(this.font, "法力消耗 " + BalanceDisplay.mana(technique.getId(), BalanceKeys.MANA_COST, technique.manaCost()), textX, textY + 60, 0xFFFFFF, false);
        graphics.drawString(this.font, technique.getDescription(), textX, textY + 78, 0xDDDDDD, false);
    }

    private Slot slotAt(int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private static String typeName(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition != null && definition.domain() != null) {
            return technique.isEnchantment() ? "领域附魔" : "领域术式";
        }
        return switch (technique.getType()) {
            case SWORD -> "剑术";
            case MAGIC -> "术法";
            case MOVEMENT -> "动作";
            case UNIVERSAL -> "常用";
        };
    }

    private static String attrName(ShuAttribute attribute) {
        return switch (attribute) {
            case NONE -> "无";
            case WOOD -> "木";
            case FIRE -> "火";
            case WATER -> "水";
            case EARTH -> "土";
            case METAL -> "金";
        };
    }

    private static String trim(float value) {
        if (value == (int) value) {
            return Integer.toString((int) value);
        }
        return Float.toString(value);
    }
}
