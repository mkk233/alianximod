package com.bslx.XueXiMod.shu.technique.release.entity;

/**
 * 单次贴图的帧号。出生时间是服务器 gameTime，不取模，播完即停。
 */
public final class StillSheetClock {
    /** 出生时间异常超前时丢掉残留。正常激活从 0 开始，不会落到这里。 */
    public static final int STRANDED_BEFORE_TICKS = 100;

    private StillSheetClock() {
    }

    /** 还没开始或已经播完时返回 -1。 */
    public static int frameIndex(long gameTime, float partialTick, long birthGameTime, int frameCount, int ticksPerFrame) {
        if (!playable(partialTick, birthGameTime, frameCount, ticksPerFrame)) {
            return -1;
        }
        double elapsedTicks = ageTicks(gameTime, partialTick, birthGameTime);
        double durationTicks = (double) frameCount * (double) ticksPerFrame;
        if (elapsedTicks < 0.0D || elapsedTicks >= durationTicks) {
            return -1;
        }
        int frame = (int) Math.floor(elapsedTicks / ticksPerFrame);
        if (frame < 0 || frame >= frameCount) {
            return -1;
        }
        return frame;
    }

    /** 已经超过总帧时长。参数不能播时不算过期。 */
    public static boolean expired(long gameTime, float partialTick, long birthGameTime, int frameCount, int ticksPerFrame) {
        if (!playable(partialTick, birthGameTime, frameCount, ticksPerFrame)) {
            return false;
        }
        double elapsedTicks = ageTicks(gameTime, partialTick, birthGameTime);
        return elapsedTicks >= (double) frameCount * (double) ticksPerFrame;
    }

    /** 出生时间比当前超前超过残留阈值。 */
    public static boolean stranded(long gameTime, float partialTick, long birthGameTime) {
        if (birthGameTime < 0L || !Float.isFinite(partialTick)) {
            return false;
        }
        return ageTicks(gameTime, partialTick, birthGameTime) < -STRANDED_BEFORE_TICKS;
    }

    /** 顶部 localV 为 0，底部为 1。一整帧铺满，不按槽把贴图切成三段。 */
    public static float textureV(int frame, float localV, int frameCount) {
        if (frameCount <= 0 || !Float.isFinite(localV)) {
            return 0.0F;
        }
        return (frame + localV) / (float) frameCount;
    }

    private static boolean playable(float partialTick, long birthGameTime, int frameCount, int ticksPerFrame) {
        return birthGameTime >= 0L && frameCount > 0 && ticksPerFrame > 0 && Float.isFinite(partialTick);
    }

    private static double ageTicks(long gameTime, float partialTick, long birthGameTime) {
        return (double) (gameTime - birthGameTime) + partialTick;
    }
}
