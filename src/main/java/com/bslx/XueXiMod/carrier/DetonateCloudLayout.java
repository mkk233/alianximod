package com.bslx.XueXiMod.carrier;

/**
 * 引爆 v2 三维云的确认视觉常量。只描述 26 团的位置、尺寸和时序，不参与伤害、火种消费或目标去重。
 * 世界偏移在命中点冻结，不随相机旋转整组。
 */
public final class DetonateCloudLayout {
    public static final int CLOUD_COUNT = 26;
    public static final int PLAY_TICKS = 20;
    public static final int ATLAS_FRAMES = 16;
    public static final float FRAME_HEIGHT = 1.0F / ATLAS_FRAMES;
    /** 确认版模型单位换算：16 单位 = 1 方块。 */
    public static final double MODEL_UNITS_PER_BLOCK = 16.0D;
    /** 视觉尺寸上限只对应已展示的 S100。不限制火种消费或伤害。 */
    public static final int VISUAL_SPENT_CAP = 100;

    private static final double[] DIRECTION_X = new double[CLOUD_COUNT];
    private static final double[] DIRECTION_Y = new double[CLOUD_COUNT];
    private static final double[] DIRECTION_Z = new double[CLOUD_COUNT];
    private static final int[] ONSET_TICKS = new int[CLOUD_COUNT];
    private static final int[] LIFETIME_TICKS = new int[CLOUD_COUNT];
    private static final double[] TRAVEL_UNITS = new double[CLOUD_COUNT];
    private static final double[] SPAN_UNITS = new double[CLOUD_COUNT];
    private static final float[] ROLL_DEGREES = new float[CLOUD_COUNT];

    static {
        for (int cloudIndex = 0; cloudIndex < 24; cloudIndex++) {
            double vertical = 1.0D - 2.0D * (cloudIndex + 0.5D) / 24.0D;
            double angle = cloudIndex * 2.3999632297D + 0.23D * Math.sin(1.7D * cloudIndex);
            double radial = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
            DIRECTION_X[cloudIndex] = Math.cos(angle) * radial;
            DIRECTION_Y[cloudIndex] = vertical;
            DIRECTION_Z[cloudIndex] = Math.sin(angle) * radial;
            ONSET_TICKS[cloudIndex] = cloudIndex % 4;
            LIFETIME_TICKS[cloudIndex] = 13 + cloudIndex % 4;
            TRAVEL_UNITS[cloudIndex] = 10.6D + ((cloudIndex * 7) % 9) * 0.49D;
            SPAN_UNITS[cloudIndex] = 16.5D + ((cloudIndex * 5) % 7);
            ROLL_DEGREES[cloudIndex] = (float) ((cloudIndex * 137.5D) % 360.0D);
        }
        DIRECTION_X[24] = 0.0D;
        DIRECTION_Y[24] = 0.13D;
        DIRECTION_Z[24] = 0.0D;
        ONSET_TICKS[24] = 0;
        LIFETIME_TICKS[24] = 9;
        TRAVEL_UNITS[24] = 1.0D;
        SPAN_UNITS[24] = 27.0D;
        ROLL_DEGREES[24] = 8.0F;

        DIRECTION_X[25] = 0.2D;
        DIRECTION_Y[25] = -0.2D;
        DIRECTION_Z[25] = 0.4D;
        ONSET_TICKS[25] = 1;
        LIFETIME_TICKS[25] = 10;
        TRAVEL_UNITS[25] = 2.0D;
        SPAN_UNITS[25] = 23.0D;
        ROLL_DEGREES[25] = 147.0F;
    }

    private DetonateCloudLayout() {
    }

    /**
     * 确认版连续映射。1–9 非零；超过已展示的 100 点后视觉固定 1.35。
     * 0 和负数返回 0，调用方仍须先判断是否真实成功消费。
     */
    public static double visualScale(int spentSeeds) {
        if (spentSeeds <= 0) {
            return 0.0D;
        }
        int visualSeeds = Math.min(spentSeeds, VISUAL_SPENT_CAP);
        if (visualSeeds <= 10) {
            return 0.40D + 0.025D * visualSeeds;
        }
        if (visualSeeds <= 50) {
            return 0.65D + 0.00875D * (visualSeeds - 10);
        }
        return 1.0D + 0.007D * (visualSeeds - 50);
    }

    public static double directionX(int cloudIndex) {
        return DIRECTION_X[cloudIndex];
    }

    public static double directionY(int cloudIndex) {
        return DIRECTION_Y[cloudIndex];
    }

    public static double directionZ(int cloudIndex) {
        return DIRECTION_Z[cloudIndex];
    }

    public static int onsetTicks(int cloudIndex) {
        return ONSET_TICKS[cloudIndex];
    }

    public static int lifetimeTicks(int cloudIndex) {
        return LIFETIME_TICKS[cloudIndex];
    }

    public static double travelUnits(int cloudIndex) {
        return TRAVEL_UNITS[cloudIndex];
    }

    public static double spanUnits(int cloudIndex) {
        return SPAN_UNITS[cloudIndex];
    }

    public static float rollDegrees(int cloudIndex) {
        return ROLL_DEGREES[cloudIndex];
    }

    /** 局部年龄小于 0 或已达寿命时不显示。 */
    public static boolean visible(int cloudIndex, double ageTicks) {
        return ageTicks >= 0.0D && ageTicks < LIFETIME_TICKS[cloudIndex];
    }

    public static double ageTicks(int cloudIndex, double elapsedTicks) {
        return elapsedTicks - ONSET_TICKS[cloudIndex];
    }

    public static double travelBlocks(int cloudIndex, double ageTicks, double visualScale) {
        if (ageTicks <= 0.0D || visualScale <= 0.0D) {
            return 0.0D;
        }
        double eased = 1.0D - Math.exp(-0.32D * ageTicks);
        return TRAVEL_UNITS[cloudIndex] * eased * visualScale / MODEL_UNITS_PER_BLOCK;
    }

    public static double liftBlocks(double ageTicks, double visualScale) {
        if (ageTicks <= 0.0D || visualScale <= 0.0D) {
            return 0.0D;
        }
        return ageTicks * 0.055D * visualScale / MODEL_UNITS_PER_BLOCK;
    }

    public static float halfSpanBlocks(int cloudIndex, double visualScale) {
        if (visualScale <= 0.0D) {
            return 0.0F;
        }
        return (float) (SPAN_UNITS[cloudIndex] * visualScale / (2.0D * MODEL_UNITS_PER_BLOCK));
    }

    /** 零基 atlas 帧；调用方须先确认该团当前可见。 */
    public static int atlasFrame(int cloudIndex, double ageTicks) {
        int lifetime = LIFETIME_TICKS[cloudIndex];
        double progress = ageTicks / (double) (lifetime - 1);
        if (progress < 0.0D) {
            progress = 0.0D;
        } else if (progress > 1.0D) {
            progress = 1.0D;
        }
        int frame = (int) Math.floor(progress * (ATLAS_FRAMES - 1));
        if (frame < 0) {
            return 0;
        }
        if (frame >= ATLAS_FRAMES) {
            return ATLAS_FRAMES - 1;
        }
        return frame;
    }
}
