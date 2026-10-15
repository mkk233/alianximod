package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.network.ImprintPacket;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 刻印台工作台界面：输入原胚、选择术、输出术式。
 */
public class ImprintDeskScreen extends AbstractContainerScreen<ImprintDeskMenu> {
    private static final ResourceLocation INVENTORY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/inventory.png");
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_DARK = 0xFF8B8B8B;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int TEXT = 0x404040;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 38;
    private static final int LIST_W = 160;
    private static final int ROW_H = 12;
    private static final int VISIBLE_ROWS = 3;

    private Button confirmButton;
    private String selectedTechniqueId;
    private int scrollOffset;

    public ImprintDeskScreen(ImprintDeskMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = ImprintDeskMenu.IMAGE_WIDTH;
        this.imageHeight = ImprintDeskMenu.IMAGE_HEIGHT;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 92;
    }

    @Override
    protected void init() {
        super.init();
        confirmButton = Button.builder(Component.literal("刻印"), b -> {
                    if (selectedTechniqueId != null) {
                        ModNetwork.CHANNEL.sendToServer(new ImprintPacket(selectedTechniqueId));
                    }
                })
                .pos(leftPos + 70, topPos + 17)
                .size(40, 20)
                .build();
        addRenderableWidget(confirmButton);
    }

    @Override
    protected void containerTick() {
        boolean ready = !menu.getEmbryoStack().isEmpty()
                && selectedTechniqueId != null
                && menu.getOutputStack().isEmpty();
        confirmButton.active = ready;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + ImprintDeskMenu.INV_TEXTURE_Y, PANEL);
        guiGraphics.fill(x, y, x + imageWidth, y + 1, PANEL_DARK);
        guiGraphics.fill(x, y, x + 1, y + ImprintDeskMenu.INV_TEXTURE_Y, PANEL_DARK);
        guiGraphics.fill(x + imageWidth - 1, y, x + imageWidth, y + ImprintDeskMenu.INV_TEXTURE_Y, 0xFFFFFFFF);
        guiGraphics.fill(x, y + ImprintDeskMenu.INV_TEXTURE_Y - 1, x + imageWidth, y + ImprintDeskMenu.INV_TEXTURE_Y, 0xFFFFFFFF);

        drawSlot(guiGraphics, x + 25, y + 17);
        drawSlot(guiGraphics, x + 133, y + 17);
        guiGraphics.fill(x + LIST_X, y + LIST_Y - 2, x + LIST_X + LIST_W, y + LIST_Y + VISIBLE_ROWS * ROW_H + 2, 0xFF8B8B8B);
        guiGraphics.fill(x + LIST_X + 1, y + LIST_Y - 1, x + LIST_X + LIST_W - 1, y + LIST_Y + VISIBLE_ROWS * ROW_H + 1, 0xFF373737);

        guiGraphics.blit(INVENTORY_TEXTURE, x, y + ImprintDeskMenu.INV_TEXTURE_Y, 0, 83, 176, 83);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, SLOT_BG);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);

        ItemStack embryo = menu.getEmbryoStack();
        if (embryo.isEmpty()) {
            guiGraphics.drawString(font, "放入原胚后选择术式", LIST_X, LIST_Y + 8, 0xAAAAAA, false);
            return;
        }

        List<Technique> learned = menu.getLearnedTechniques();
        clampScroll(learned.size());
        if (learned.isEmpty()) {
            guiGraphics.drawString(font, "没有可刻印的术", LIST_X + 4, LIST_Y + 8, 0xFFAAAA, false);
            return;
        }

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scrollOffset + i;
            if (index >= learned.size()) break;
            Technique t = learned.get(index);
            boolean selected = t.getId().equals(selectedTechniqueId);
            int color = selected ? 0xFFFFA0 : 0xFFFFFF;
            String prefix = selected ? "> " : "  ";
            guiGraphics.drawString(font, prefix + t.getName(), LIST_X + 2, LIST_Y + i * ROW_H, color, false);
        }

        renderPreview(guiGraphics, embryo, learned);
    }

    private void renderPreview(GuiGraphics guiGraphics, ItemStack embryo, List<Technique> learned) {
        Technique technique = findSelected(learned);
        if (technique == null) return;
        ShuAttribute material = ShuEmbryoItem.getAttribute(embryo);
        SkillStoneData preview = SkillStoneData.imprint(technique, material);
        String reaction = ElementReaction.of(material, technique.getAttribute()).describe();
        boolean bonus = !"无属性反应".equals(reaction);
        guiGraphics.drawString(font, "类型：" + typeName(technique.getType())
                + " 属性：" + attributeName(technique.getAttribute()), 8, 76, TEXT, false);
        guiGraphics.drawString(font, "描述：" + technique.getDescription(), 8, 85, TEXT, false);
        guiGraphics.drawString(font, "伤害 " + format(preview.getDamage())
                + "  冷却 " + preview.getCooldown() + "t"
                + "  " + reaction, 8, 94, bonus ? 0x1A6B2A : TEXT, false);
    }

    private Technique findSelected(List<Technique> learned) {
        if (selectedTechniqueId == null) return null;
        for (Technique t : learned) {
            if (t.getId().equals(selectedTechniqueId)) return t;
        }
        return null;
    }

    private static String format(float value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.format("%.1f", value);
    }

    private static String typeName(TechniqueType type) {
        switch (type) {
            case SWORD: return "剑术";
            case MOVEMENT: return "动作";
            case MAGIC: return "法术";
            default: return "通用";
        }
    }

    private static String attributeName(ShuAttribute attribute) {
        switch (attribute) {
            case NONE: return "无";
            case WOOD: return "木";
            case FIRE: return "火";
            case WATER: return "水";
            case EARTH: return "土";
            default: return "金";
        }
    }

    private void clampScroll(int size) {
        int max = Math.max(0, size - VISIBLE_ROWS);
        scrollOffset = Mth.clamp(scrollOffset, 0, max);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!menu.getEmbryoStack().isEmpty()) {
            List<Technique> learned = menu.getLearnedTechniques();
            int listLeft = leftPos + LIST_X;
            int listTop = topPos + LIST_Y;
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int index = scrollOffset + i;
                if (index >= learned.size()) break;
                int rowY = listTop + i * ROW_H;
                if (mouseX >= listLeft && mouseX < listLeft + LIST_W
                        && mouseY >= rowY && mouseY < rowY + ROW_H) {
                    selectedTechniqueId = learned.get(index).getId();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<Technique> learned = menu.getLearnedTechniques();
        if (learned.size() > VISIBLE_ROWS) {
            clampScroll(learned.size());
            scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(delta), 0, learned.size() - VISIBLE_ROWS);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
