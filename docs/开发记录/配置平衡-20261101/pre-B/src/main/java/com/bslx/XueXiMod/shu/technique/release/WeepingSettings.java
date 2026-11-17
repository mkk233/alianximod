package com.bslx.XueXiMod.shu.technique.release;

/**
 * 垂丝这一轮的试调默认值。几何、结算和注册都读这里，不在各处再写一套。
 * 改前底伤是 5，魔力按 {@code ceil(底伤 * 2)} 得到 10。底伤改成 2 之后仍显式保留 10，冷却仍是 20。
 */
public final class WeepingSettings {
    public static final float BASE_DAMAGE = 2.0f;
    public static final int WETNESS = 3;
    public static final int HIT_INTERVAL = 10;
    public static final int LIFETIME = 40;
    public static final int FILL_TICKS = 8;
    public static final int DRAIN_START = 32;
    /** 水帘竖直方向允许的最大下落距离。 */
    public static final double FALL_DISTANCE_LIMIT = 6.0;
    /** 原版基础宽度，按 4+4+4 像素铺法。 */
    public static final double SHEET_ACROSS = 0.5;
    public static final int RETAINED_MANA = 10;

    private WeepingSettings() {
    }

    /** 基准寿命减去基准排空起点，排空窗口固定 8 拍。 */
    public static int drainSpan() {
        return LIFETIME - DRAIN_START;
    }

    /** 实例总寿命减去固定排空拍数。填充长度不随总寿命改变。 */
    public static int drainStartFor(int lifeTicks) {
        return lifeTicks - drainSpan();
    }
}
