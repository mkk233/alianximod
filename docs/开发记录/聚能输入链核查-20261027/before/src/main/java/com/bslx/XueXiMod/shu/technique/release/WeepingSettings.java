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
    public static final int RETAINED_MANA = 10;

    private WeepingSettings() {
    }
}
