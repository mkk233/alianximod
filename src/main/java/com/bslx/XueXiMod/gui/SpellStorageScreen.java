package com.bslx.XueXiMod.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SpellStorageScreen extends AbstractContainerScreen<SpellStorageMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    public SpellStorageScreen(SpellStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int pointX = this.leftPos;
        int pointY = this.topPos;
        graphics.blit(TEXTURE, pointX, pointY, 0, 0, this.imageWidth, 71);
        graphics.blit(TEXTURE, pointX, pointY + 71, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
