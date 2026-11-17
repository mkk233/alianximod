package com.bslx.XueXiMod.shu.technique.enchant.resource;

import java.util.List;

/**
 * 留焰宿主合同。这里只有已批准参数，不生成轨迹、不改命中钟、不产种。
 * 连续扫掠和表现点合并由载体作者执行。
 */
public final class TrailContract {
    public static final int LIFE_TICKS = 40;
    public static final double DIAMETER = 0.5d;
    /** 表现采样上限。碰撞仍是连续扫掠线段，采样限额不扩大伤害区。 */
    public static final double SAMPLE_SPACING = 0.25d;
    public static final int ACTIVE_POINT_CAP = 256;
    public static final boolean PRODUCES_SEEDS = false;
    public static final boolean CONSUMES_SEEDS = false;
    public static final boolean PLACES_VANILLA_FIRE = false;
    public static final boolean BREAKS_TERRAIN = false;
    public static final boolean SHARES_HOST_HIT_CLOCK = true;
    /** 第一版只声明这些移动命中体。剑实体、光束、领域和固定斩击不在此列。 */
    public static final List<String> MOVING_HOSTS = List.of("fire_spark", "fire_orb", "fire_meteorite");

    private TrailContract() {
    }
}
