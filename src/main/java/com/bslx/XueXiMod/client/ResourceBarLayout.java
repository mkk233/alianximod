package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** 左上角生存资源条 v6。部件在 0.5 倍矩阵里按 2x 坐标画，数字在还原后画。 */
public final class ResourceBarLayout {
    public static final int FILL_WIDTH = 180;
    public static final int WATER_UPPER_BOUND = 100;

    private static final int FRAME_COUNT = 16;
    private static final long FRAME_MILLIS = 90L;
    private static final double LOW_MANA_RATIO = 0.22D;
    private static final int FILL_HEIGHT = 10;
    private static final int TRACK_WIDTH = 204;
    private static final int TRACK_HEIGHT = 20;
    private static final int MANA_ICON_WIDTH = 68;
    private static final int MANA_ICON_HEIGHT = 58;
    private static final int WATER_ICON_WIDTH = 48;
    private static final int WATER_ICON_HEIGHT = 40;
    private static final float WATER_TIER_STREAM = 20.0F;
    private static final float WATER_TIER_TORRENT = 50.0F;
    private static final int MANA_TEXT_X = 137;
    private static final int MANA_TEXT_Y = 10;
    private static final int WATER_TEXT_X = 137;
    private static final int WATER_TEXT_Y = 37;
    private static final int WATER_CHANGE_TOP = 52;

    private static final ResourceLocation MANA_ICON_EMPTY = texture("mana_icon_empty.png");
    private static final ResourceLocation MANA_ICON_STRIP = texture("mana_icon_strip.png");
    private static final ResourceLocation MANA_ICON_FULL_STRIP = texture("mana_icon_full_strip.png");
    private static final ResourceLocation MANA_TRACK = texture("mana_track.png");
    private static final ResourceLocation MANA_FILL_LOW = texture("mana_fill_low.png");
    private static final ResourceLocation MANA_FILL_STRIP = texture("mana_fill_strip.png");
    private static final ResourceLocation MANA_FILL_FULL_STRIP = texture("mana_fill_full_strip.png");
    private static final ResourceLocation WATER_ICON_IDLE = texture("water_icon_idle.png");
    private static final ResourceLocation WATER_ICON = texture("water_icon.png");
    private static final ResourceLocation WATER_ICON_FULL_STRIP = texture("water_icon_full_strip.png");
    private static final ResourceLocation WATER_TRACK_STATES = texture("water_track_states.png");
    private static final ResourceLocation WATER_FILL_TIER1 = texture("water_fill_tier1_trickle_strip.png");
    private static final ResourceLocation WATER_FILL_TIER2 = texture("water_fill_tier2_stream_strip.png");
    private static final ResourceLocation WATER_FILL_TIER3 = texture("water_fill_tier3_torrent_strip.png");

    private ResourceBarLayout() {
    }

    public static void drawMana(GuiGraphics graphics, Font font, int current, int upperBound, String amountText, int textColor) {
        int frame = frameIndex();
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        blitWhole(graphics, MANA_TRACK, 62, 18, TRACK_WIDTH, TRACK_HEIGHT);
        if (current <= 0) {
            blitWhole(graphics, MANA_ICON_EMPTY, 8, 3, MANA_ICON_WIDTH, MANA_ICON_HEIGHT);
        } else if (current >= upperBound) {
            blitStrip(graphics, MANA_FILL_FULL_STRIP, 80, 23, FILL_WIDTH, frame, FILL_HEIGHT, FILL_WIDTH);
            blitStrip(graphics, MANA_ICON_FULL_STRIP, 8, 3, MANA_ICON_WIDTH, frame, MANA_ICON_HEIGHT, MANA_ICON_WIDTH);
        } else {
            int width = filledWidth(current, upperBound);
            if (width > 0 && (double) current / (double) upperBound <= LOW_MANA_RATIO) {
                blit(graphics, MANA_FILL_LOW, 80, 23, width, FILL_HEIGHT, 0.0F, 0.0F, width, FILL_HEIGHT, FILL_WIDTH, FILL_HEIGHT);
            } else if (width > 0) {
                blitStrip(graphics, MANA_FILL_STRIP, 80, 23, width, frame, FILL_HEIGHT, FILL_WIDTH);
            }
            blitStrip(graphics, MANA_ICON_STRIP, 8, 3, MANA_ICON_WIDTH, frame, MANA_ICON_HEIGHT, MANA_ICON_WIDTH);
        }
        graphics.pose().popPose();
        graphics.drawString(font, amountText, MANA_TEXT_X, MANA_TEXT_Y, textColor);
    }

    public static void drawWater(GuiGraphics graphics, Font font, float balance, String amountText, int textColor) {
        int frame = frameIndex();
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        if (balance <= 0.0F) {
            blitTrack(graphics, 0);
            blitWhole(graphics, WATER_ICON_IDLE, 16, 62, WATER_ICON_WIDTH, WATER_ICON_HEIGHT);
        } else if (balance < WATER_TIER_STREAM) {
            blitTrack(graphics, 0);
            blitFill(graphics, WATER_FILL_TIER1, filledWidth(balance, WATER_UPPER_BOUND), frame);
            blitWhole(graphics, WATER_ICON, 16, 62, WATER_ICON_WIDTH, WATER_ICON_HEIGHT);
        } else if (balance < WATER_TIER_TORRENT) {
            blitTrack(graphics, TRACK_HEIGHT);
            blitFill(graphics, WATER_FILL_TIER2, filledWidth(balance, WATER_UPPER_BOUND), frame);
            blitWhole(graphics, WATER_ICON, 16, 62, WATER_ICON_WIDTH, WATER_ICON_HEIGHT);
        } else if (balance < WATER_UPPER_BOUND) {
            blitTrack(graphics, TRACK_HEIGHT * 2);
            blitFill(graphics, WATER_FILL_TIER3, filledWidth(balance, WATER_UPPER_BOUND), frame);
            blitWhole(graphics, WATER_ICON, 16, 62, WATER_ICON_WIDTH, WATER_ICON_HEIGHT);
        } else {
            blitTrack(graphics, TRACK_HEIGHT * 2);
            blitFill(graphics, WATER_FILL_TIER3, FILL_WIDTH, frame);
            blitStrip(graphics, WATER_ICON_FULL_STRIP, 16, 62, WATER_ICON_WIDTH, frame, WATER_ICON_HEIGHT, WATER_ICON_WIDTH);
        }
        graphics.pose().popPose();
        graphics.drawString(font, amountText, WATER_TEXT_X, WATER_TEXT_Y, textColor);
    }

    /** 水势图标逻辑下沿是 51。变化行从 52 起，并且不盖住 y=37 的水势数字。 */
    public static int waterChangeTop(Font font) {
        return Math.max(WATER_CHANGE_TOP, WATER_TEXT_Y + font.lineHeight + 1);
    }

    public static int filledWidth(double current, double upperBound) {
        if (current <= 0.0 || upperBound <= 0.0) {
            return 0;
        }
        if (current >= upperBound) {
            return FILL_WIDTH;
        }
        int rounded = (int) Math.floor(current / upperBound * FILL_WIDTH + 0.5);
        return Math.max(1, Math.min(FILL_WIDTH, rounded));
    }

    private static void blitTrack(GuiGraphics graphics, int v) {
        blit(graphics, WATER_TRACK_STATES, 62, 72, TRACK_WIDTH, TRACK_HEIGHT, 0.0F, v, TRACK_WIDTH, TRACK_HEIGHT, TRACK_WIDTH, TRACK_HEIGHT * 3);
    }

    private static void blitFill(GuiGraphics graphics, ResourceLocation texture, int width, int frame) {
        if (width <= 0) {
            return;
        }
        blitStrip(graphics, texture, 80, 77, width, frame, FILL_HEIGHT, FILL_WIDTH);
    }

    private static void blitStrip(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int frame, int frameHeight, int textureWidth) {
        blit(graphics, texture, x, y, width, frameHeight, 0.0F, frame * frameHeight, width, frameHeight, textureWidth, frameHeight * FRAME_COUNT);
    }

    private static void blitWhole(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        blit(graphics, texture, x, y, width, height, 0.0F, 0.0F, width, height, width, height);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                             float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    private static int frameIndex() {
        return (int) ((System.currentTimeMillis() / FRAME_MILLIS) % FRAME_COUNT);
    }

    private static ResourceLocation texture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "textures/gui/resource_bars_v6/" + fileName);
    }
}
