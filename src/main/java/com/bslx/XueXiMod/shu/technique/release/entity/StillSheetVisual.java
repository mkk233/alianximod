package com.bslx.XueXiMod.shu.technique.release.entity;

/**
 * 静止冻结斩面的单次贴图。默认关闭。
 * 只有调用方放上有效描述才改视觉，不在通用路径里识别术式名字。
 */
public final class StillSheetVisual {
    public static final StillSheetVisual NONE = new StillSheetVisual("", 0, 1);
    /** 尚未正式激活。0 是合法的世界时间，不能拿来表示没出生。 */
    public static final long UNBORN = -1L;

    private final String texturePath;
    private final int frameCount;
    private final int ticksPerFrame;

    private StillSheetVisual(String texturePath, int frameCount, int ticksPerFrame) {
        this.texturePath = texturePath;
        this.frameCount = frameCount;
        this.ticksPerFrame = ticksPerFrame;
    }

    /** 贴图、帧数或每帧时长无效时返回关闭值。 */
    public static StillSheetVisual once(String texturePath, int frameCount, int ticksPerFrame) {
        if (texturePath == null || texturePath.isEmpty() || frameCount <= 0 || ticksPerFrame <= 0) {
            return NONE;
        }
        return new StillSheetVisual(texturePath, frameCount, ticksPerFrame);
    }

    /** 三项都有效才改视觉。 */
    public boolean enabled() {
        return this.frameCount > 0 && this.ticksPerFrame > 0 && this.texturePath != null && !this.texturePath.isEmpty();
    }

    public String texturePath() {
        return this.texturePath;
    }

    public int frameCount() {
        return this.frameCount;
    }

    public int ticksPerFrame() {
        return this.ticksPerFrame;
    }
}
