package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import net.minecraft.client.gui.GuiGraphics;
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

    private static final String[] TYPE_LABELS = {"全部", "剑术", "法术", "动作", "常用"};
    private static final TechniqueType[] TYPE_VALUES =
            {null, TechniqueType.SWORD, TechniqueType.MAGIC, TechniqueType.MOVEMENT, TechniqueType.UNIVERSAL};
    private static final String[] ATTR_LABELS = {"无", "木", "火", "水", "土", "金"};
    private static final ShuAttribute[] ATTR_VALUES =
            {ShuAttribute.NONE, ShuAttribute.WOOD, ShuAttribute.FIRE, ShuAttribute.WATER, ShuAttribute.EARTH, ShuAttribute.METAL};

    public SpellAssemblyScreen(SpellAssemblyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SpellAssemblyMenu.GUI_W;
        this.imageHeight = SpellAssemblyMenu.GUI_H;
        this.titleLabelY = -999;
        this.inventoryLabelY = -999;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int l = this.leftPos;
        int t = this.topPos;
        int w = SpellAssemblyMenu.GUI_W;
        int h = SpellAssemblyMenu.GUI_H;
        graphics.fill(l, t, l + w, t + h, PANEL);
        graphics.fill(l, t, l + w, t + 2, FRAME);
        graphics.fill(l, t + h - 2, l + w, t + h, FRAME);
        graphics.fill(l, t, l + 2, t + h, FRAME);
        graphics.fill(l + w - 2, t, l + w, t + h, FRAME);
        graphics.fill(l + 3, t + 3, l + SpellAssemblyMenu.GEAR_X - 2, t + SpellAssemblyMenu.LIST_Y - 1, FILTER_BG);
        graphics.fill(l + 3, t + SpellAssemblyMenu.LIST_Y - 1, l + SpellAssemblyMenu.GEAR_X - 2, t + SpellAssemblyMenu.BOTTOM_Y - 1, LIST_BG);
        graphics.fill(l + 3, t + SpellAssemblyMenu.BOTTOM_Y - 1, l + SpellAssemblyMenu.GEAR_X - 2, t + h - 3, INV_BG);
        graphics.fill(l + SpellAssemblyMenu.GEAR_X - 2, t + SpellAssemblyMenu.BOTTOM_Y - 1,
                l + SpellAssemblyMenu.PREVIEW_X - 2, t + h - 3, GEAR_BG);
        graphics.fill(l + SpellAssemblyMenu.PREVIEW_X - 2, t + 3, l + w - 3, t + h - 3, PREVIEW_BG);
        for (Slot slot : this.menu.slots) {
            drawSlotBox(graphics, l + slot.x, t + slot.y);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFilterButtons(graphics);
        renderPreview(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int idx = buttonAt(mouseX, mouseY, TYPE_LABELS.length, ROW1_Y, BTN_STEP, BTN_W);
            if (idx >= 0) {
                if (idx == 0) {
                    this.menu.setFilters(null, null);
                } else {
                    this.menu.setFilters(TYPE_VALUES[idx], this.menu.currentAttribute());
                }
                return true;
            }
            idx = buttonAt(mouseX, mouseY, ATTR_LABELS.length, ROW2_Y, ATTR_STEP, ATTR_W);
            if (idx >= 0) {
                this.menu.setFilters(this.menu.currentType(), ATTR_VALUES[idx]);
                return true;
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
            for (int i = 1; i < TYPE_VALUES.length; i++) {
                if (TYPE_VALUES[i] == type) {
                    typeSel = i;
                    break;
                }
            }
        }
        int attrSel = -1;
        ShuAttribute attr = this.menu.currentAttribute();
        if (attr != null) {
            for (int i = 0; i < ATTR_VALUES.length; i++) {
                if (ATTR_VALUES[i] == attr) {
                    attrSel = i;
                    break;
                }
            }
        }
        for (int i = 0; i < TYPE_LABELS.length; i++) {
            drawFilterButton(graphics, i, ROW1_Y, TYPE_LABELS[i], i == typeSel, BTN_STEP, BTN_W);
        }
        for (int i = 0; i < ATTR_LABELS.length; i++) {
            drawFilterButton(graphics, i, ROW2_Y, ATTR_LABELS[i], i == attrSel, ATTR_STEP, ATTR_W);
        }
    }

    private void drawFilterButton(GuiGraphics graphics, int index, int rowY, String label, boolean selected, int step, int width) {
        int x = this.leftPos + BTN_LEFT + index * step;
        int y = this.topPos + rowY;
        graphics.fill(x, y, x + width, y + BTN_H, selected ? BTN_SELECTED : BTN_NORMAL);
        int tx = x + (width - this.font.width(label)) / 2;
        int ty = y + (BTN_H - this.font.lineHeight) / 2;
        graphics.drawString(this.font, label, tx, ty, selected ? 0xFFFFFF : 0xDDDDDD, false);
    }

    private int buttonAt(double mouseX, double mouseY, int count, int rowY, int step, int width) {
        for (int i = 0; i < count; i++) {
            int x = this.leftPos + BTN_LEFT + i * step;
            int y = this.topPos + rowY;
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + BTN_H) {
                return i;
            }
        }
        return -1;
    }

    private void drawSlotBox(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 16, y + 16, SLOT_FILL);
        graphics.fill(x - 1, y - 1, x + 17, y, SLOT_BORDER);
        graphics.fill(x - 1, y + 16, x + 17, y + 17, SLOT_BORDER);
        graphics.fill(x - 1, y, x, y + 16, SLOT_BORDER);
        graphics.fill(x + 16, y, x + 17, y + 16, SLOT_BORDER);
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
        int x = this.leftPos + PREVIEW_X;
        int y = this.topPos + PREVIEW_Y + 78;
        int iconX = this.leftPos + PREVIEW_X + 28;
        int iconY = this.topPos + PREVIEW_Y + 28;
        graphics.pose().pushPose();
        graphics.pose().translate(iconX, iconY, 0.0F);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.renderItem(hovered.getItem(), 0, 0);
        graphics.pose().popPose();
        graphics.drawString(this.font, "名称 " + technique.getName(), x, y, 0xFFFFFF, false);
        graphics.drawString(this.font, "类型 " + typeName(technique.getType()), x, y + 12, 0xFFFFFF, false);
        graphics.drawString(this.font, "属性 " + attrName(technique.getAttribute()), x, y + 24, 0xFFFFFF, false);
        graphics.drawString(this.font, "伤害 " + trim(technique.getBaseDamage()), x, y + 36, 0xFFFFFF, false);
        graphics.drawString(this.font, "冷却 " + technique.getBaseCooldown(), x, y + 48, 0xFFFFFF, false);
        graphics.drawString(this.font, "魔力消耗 " + technique.manaCost(), x, y + 60, 0xFFFFFF, false);
        graphics.drawString(this.font, technique.getDescription(), x, y + 78, 0xDDDDDD, false);
    }

    private Slot slotAt(int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private static String typeName(TechniqueType type) {
        return switch (type) {
            case SWORD -> "剑术";
            case MAGIC -> "法术";
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
