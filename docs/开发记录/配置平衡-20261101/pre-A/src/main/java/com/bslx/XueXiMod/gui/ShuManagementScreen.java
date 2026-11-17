package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.shu.Proficiency;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * 术管理屏幕 - M键打开
 *
 * 界面布局：
 * ┌─────────────────────────────────────┐
 * │  术管理                      [关闭] │
 * │  剑术等级: Lv.3  法术等级: Lv.1     │
 * ├─────────────────────────────────────┤
 * │  [剑术] [动作] [法术] [通用]  ← 分类 │
 * ├─────────────────────────────────────┤
 * │  ✓ 刺击      Lv.12  ████░░ 60%     │
 * │    正前方单体突刺  伤害:6 冷却:15t  │
 * │  ✓ 横劈      Lv.8   ███░░░ 40%     │
 * │    正前方扇形AOE   伤害:4 冷却:20t  │
 * │  🔒 竖劈    需要剑术等级 Lv.3       │
 * │    高倍率单体重击   伤害:8 冷却:25t  │
 * └─────────────────────────────────────┘
 */
public class ShuManagementScreen extends AbstractContainerScreen<ShuManagementMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/dispenser.png");

    private Button swordBtn, movementBtn, magicBtn, universalBtn;

    public ShuManagementScreen(ShuManagementMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 200;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        int screenX = (width - imageWidth) / 2;
        int screenY = (height - imageHeight) / 2;

        // 分类按钮
        swordBtn = Button.builder(Component.literal("剑术"), button -> {
                    menu.setSelectedCategory(TechniqueType.SWORD);
                    refreshCategoryButtons();
                }).pos(screenX + 10, screenY + 28).size(36, 14).build();
        movementBtn = Button.builder(Component.literal("动作"), button -> {
                    menu.setSelectedCategory(TechniqueType.MOVEMENT);
                    refreshCategoryButtons();
                }).pos(screenX + 50, screenY + 28).size(36, 14).build();
        magicBtn = Button.builder(Component.literal("法术"), button -> {
                    menu.setSelectedCategory(TechniqueType.MAGIC);
                    refreshCategoryButtons();
                }).pos(screenX + 90, screenY + 28).size(36, 14).build();
        universalBtn = Button.builder(Component.literal("通用"), button -> {
                    menu.setSelectedCategory(TechniqueType.UNIVERSAL);
                    refreshCategoryButtons();
                }).pos(screenX + 130, screenY + 28).size(36, 14).build();

        addRenderableWidget(swordBtn);
        addRenderableWidget(movementBtn);
        addRenderableWidget(magicBtn);
        addRenderableWidget(universalBtn);

        refreshCategoryButtons();
    }

    private void refreshCategoryButtons() {
        TechniqueType sel = menu.getSelectedCategory();
        swordBtn.active = sel != TechniqueType.SWORD;
        movementBtn.active = sel != TechniqueType.MOVEMENT;
        magicBtn.active = sel != TechniqueType.MAGIC;
        universalBtn.active = sel != TechniqueType.UNIVERSAL;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int screenX = (width - imageWidth) / 2;
        int screenY = (height - imageHeight) / 2;
        // 绘制背景（使用原版贴图，拉伸到200x220）
        guiGraphics.blit(TEXTURE, screenX, screenY, 0, 0, imageWidth, 166);
        // 额外绘制下半部分（物品栏区域）
        guiGraphics.blit(TEXTURE, screenX, screenY + 166, 0, 0, imageWidth, 54);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题
        guiGraphics.drawString(font, Component.literal("术式图鉴"), titleLabelX, titleLabelY, 0x404040, false);

        // 剑术/法术等级显示
        int swordLv = menu.getSwordLevel();
        int magicLv = menu.getMagicLevel();
        guiGraphics.drawString(font, "§c剑术 Lv." + swordLv, 10, 4, 0xFFFFFF, false);
        guiGraphics.drawString(font, "§9法术 Lv." + magicLv, 100, 4, 0xFFFFFF, false);

        // 术列表
        List<Technique> techniques = menu.getTechniquesInCategory();
        int listY = 40;

        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            Technique technique = techniques.get(techniqueIndex);
            boolean unlocked = menu.isTechniqueUnlocked(technique);
            boolean learned = menu.isTechniqueLearned(technique.getId());
            int screenY = listY + techniqueIndex * 44; // 每个术占44像素高度

            if (unlocked && learned) {
                // 已学习 + 已解锁：显示详细信息
                int level = menu.getTechniqueLevel(technique.getId());
                float progress = menu.getTechniqueProgress(technique.getId());

                // 名称和等级
                guiGraphics.drawString(font, "§a✓ " + technique.getName(), 10, screenY, 0xFFFFFF, false);
                guiGraphics.drawString(font, "§eLv." + level, 100, screenY, 0xFFFF00, false);

                // 进度条
                renderProgressBar(guiGraphics, 130, screenY, 60, 8, progress);

                // 描述和属性
                guiGraphics.drawString(font, "§7" + technique.getDescription(), 10, screenY + 12, 0xAAAAAA, false);
                guiGraphics.drawString(font, "§c伤害:" + technique.getBaseDamage() + " §b冷却:" + technique.getBaseCooldown() + "t",
                        10, screenY + 24, 0xAAAAAA, false);
                guiGraphics.drawString(font, "§6属性:" + attributeName(technique.getAttribute()),
                        10, screenY + 36, 0xAAAAAA, false);
            } else if (unlocked) {
                // 已解锁但未学习（理论上不会出现，因为解锁=自动学习）
                guiGraphics.drawString(font, "§e○ " + technique.getName(), 10, screenY, 0xFFFFFF, false);
                guiGraphics.drawString(font, "§7" + technique.getDescription(), 10, screenY + 12, 0xAAAAAA, false);
            } else {
                // 未解锁：显示解锁条件
                guiGraphics.drawString(font, "§8🔒 " + technique.getName(), 10, screenY, 0x888888, false);
                String typeName = (technique.getType() == TechniqueType.SWORD) ? "剑术" : "法术";
                guiGraphics.drawString(font, "§c需要" + typeName + "等级 Lv." + technique.getUnlockLevel(),
                        10, screenY + 12, 0xFF5555, false);
                guiGraphics.drawString(font, "§7" + technique.getDescription(), 10, screenY + 24, 0x666666, false);
            }
        }

        // 如果当前分类没有术
        if (techniques.isEmpty()) {
            guiGraphics.drawString(font, "§7暂无此类型术", 10, listY, 0xAAAAAA, false);
        }
    }

    private String attributeName(ShuAttribute attribute) {
        return switch (attribute) {
            case NONE -> "无";
            case WOOD -> "木";
            case FIRE -> "火";
            case WATER -> "水";
            case EARTH -> "土";
            case METAL -> "金";
        };
    }

    /**
     * 绘制进度条
     */
    private void renderProgressBar(GuiGraphics guiGraphics, int screenX, int screenY, int width, int height, float progress) {
        // 背景
        guiGraphics.fill(screenX, screenY, screenX + width, screenY + height, 0xFF333333);
        // 进度
        int fillWidth = (int) (width * Math.min(1.0f, progress));
        if (fillWidth > 0) {
            guiGraphics.fill(screenX, screenY, screenX + fillWidth, screenY + height, 0xFF00AA00);
        }
        // 边框
        guiGraphics.fill(screenX, screenY, screenX + width, screenY + 1, 0xFF555555); // 上
        guiGraphics.fill(screenX, screenY + height - 1, screenX + width, screenY + height, 0xFF555555); // 下
        guiGraphics.fill(screenX, screenY, screenX + 1, screenY + height, 0xFF555555); // 左
        guiGraphics.fill(screenX + width - 1, screenY, screenX + width, screenY + height, 0xFF555555); // 右
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
