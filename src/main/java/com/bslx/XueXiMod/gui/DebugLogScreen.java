package com.bslx.XueXiMod.gui;

import com.bslx.XueXiMod.debug.DebugLog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 只显示调试记录，没有玩法操作。
 */
public class DebugLogScreen extends Screen {
    private static final int LINE_HEIGHT = 10;
    private int scroll;

    public DebugLogScreen() {
        super(Component.literal("调试日志"));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("清空"), button -> {
            DebugLog.clear();
            scroll = 0;
        }).bounds(width / 2 - 80, height - 28, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(width / 2 + 10, height - 28, 70, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, "调试日志", width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, DebugLog.isEnabled() ? "记录中" : "已关闭", width / 2, 20, 0xA0A0A0);

        List<String> lines = DebugLog.lines();
        int top = 36;
        int bottom = height - 36;
        int visible = Math.max(1, (bottom - top) / LINE_HEIGHT);
        int maxScroll = Math.max(0, lines.size() - visible);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        int start = Math.max(0, lines.size() - visible - scroll);
        int y = top;
        for (int i = start; i < lines.size() && y + LINE_HEIGHT <= bottom; i++) {
            graphics.drawString(font, lines.get(i), 12, y, 0xE0E0E0, false);
            y += LINE_HEIGHT;
        }
        if (lines.isEmpty()) {
            graphics.drawCenteredString(font, "还没有调试记录", width / 2, height / 2, 0x808080);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visible = Math.max(1, (height - 72) / LINE_HEIGHT);
        int maxScroll = Math.max(0, DebugLog.lines().size() - visible);
        if (delta > 0) {
            scroll = Math.min(maxScroll, scroll + 1);
        } else if (delta < 0) {
            scroll = Math.max(0, scroll - 1);
        }
        return true;
    }
}
