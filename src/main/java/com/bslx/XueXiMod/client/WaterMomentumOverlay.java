package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.network.WaterMomentumNotice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.List;

/** 水势整组默认隐藏。只有结余变化的浮动行还在时，才和水势数字一起短暂显示。 */
public final class WaterMomentumOverlay {
    private WaterMomentumOverlay() {
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        List<Float> changes = WaterMomentumNotice.ClientNotice.freshChanges();
        if (changes.isEmpty()) {
            return;
        }
        float balance = WaterMomentumNotice.ClientNotice.balance();
        int shown = Math.round(balance);
        ResourceBarLayout.drawWater(graphics, gui.getFont(), balance, shown + "/100", 0xFFFFFF);
        int lineHeight = gui.getFont().lineHeight + 1;
        int cursorY = ResourceBarLayout.waterChangeTop(gui.getFont());
        for (Float change : changes) {
            int color = change >= 0f ? 0x7AD7FF : 0xF2C14E;
            graphics.drawString(gui.getFont(), signed(change), 20, cursorY, color);
            cursorY += lineHeight;
        }
    }

    private static String signed(float change) {
        return (change > 0f ? "+" : "-") + plain(Math.abs(change));
    }

    private static String plain(float amount) {
        float tenths = Math.round(amount * 10f) / 10f;
        int whole = Math.round(tenths);
        if (Math.abs(tenths - whole) < 0.05f) {
            return Integer.toString(Math.abs(whole));
        }
        int tenthDigit = Math.abs(Math.round(tenths * 10f)) % 10;
        return Math.abs(whole) + "." + tenthDigit;
    }
}
