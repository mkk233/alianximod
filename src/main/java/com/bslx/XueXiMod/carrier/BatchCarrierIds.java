package com.bslx.XueXiMod.carrier;

/**
 * 本批载体使用的术式标识和已批准数值。火苗本体产种基数 2 由释放登记提供，这里不把它写进 seedPoints。
 */
public final class BatchCarrierIds {
    public static final String SPARK = "fire_spark";
    public static final String SHOCK_RING = "fire_shock_ring";
    public static final String METEORITE = "fire_meteorite";
    public static final String SURFACE_ANCHOR = "rule_surface_anchor";
    public static final String UPWELL = "fire_upwell";
    public static final String STAGGER = "rule_stagger";
    public static final String HIT_CONTINUE = "rule_hit_continue";
    public static final String FLAME_TRAIL = "fire_flame_trail";
    public static final String SEED_SOW = "fire_seed_sow";
    public static final String KINDLE = "fire_kindle";
    public static final String DETONATE = "fire_detonate";
    public static final String BREAK_GROUND = "earth_break_ground";

    public static final float SPARK_DAMAGE = 1.0F;
    public static final int SPARK_LIFE_TICKS = 30;
    public static final double SPARK_SPEED = 0.8D;
    public static final double SPARK_SIZE = 0.25D;
    public static final int SPARK_BURN_TICKS = 20;

    public static final float RING_DAMAGE = 4.0F;
    public static final int RING_LIFE_TICKS = 12;
    public static final double RING_INNER_RADIUS = 1.5D;
    public static final double RING_OUTER_RADIUS = 5.0D;
    public static final double RING_HEIGHT = 3.0D;

    public static final float METEOR_DAMAGE = 24.0F;
    public static final int METEOR_LIFE_TICKS = 100;
    public static final double METEOR_SPEED = 0.8D;
    public static final double METEOR_SIZE = 1.0D;
    public static final double METEOR_CENTER_RADIUS = 1.5D;
    public static final double METEOR_CENTER_HEIGHT = 3.0D;

    public static final int STRIP_FRAMES = 16;

    private BatchCarrierIds() {
    }
}
