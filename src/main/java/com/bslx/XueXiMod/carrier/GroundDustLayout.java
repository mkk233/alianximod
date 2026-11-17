package com.bslx.XueXiMod.carrier;

/**
 * 崩地确认版 earth-ground-airborne-dust-v2r2-20261116 的纯视觉配方。
 * 九条尘流和十二颗粗粒都挂在撞击中心 C 上，不改坑形、伤害或地形。
 */
public final class GroundDustLayout {
    public static final String VERSION = "earth-ground-airborne-dust-v2r2-20261116";
    public static final int PLAY_TICKS = 28;
    public static final int DUST_COUNT = 9;
    public static final int GRIT_COUNT = 12;
    public static final int DUST_FRAMES = 24;
    public static final int DUST_BANKS = 3;
    public static final int GRIT_BANKS = 3;
    public static final int GRIT_ROWS = 4;
    public static final double ROOT_Y = 0.56D;
    public static final double DUST_LIFETIME_SECONDS = 1.2D;
    public static final double FRAME_SECONDS = 0.05D;

    private static final double[] DUST_X = {
            -0.82D, 0.48D, 0.10D, -1.38D, 1.26D, -0.26D, 0.16D, -0.54D, 0.82D
    };
    private static final double[] DUST_Z = {
            -0.20D, 0.28D, -1.05D, 0.48D, -0.36D, 1.32D, 0.02D, -0.72D, 0.86D
    };
    private static final double[] DUST_SIZE = {
            1.40D, 1.55D, 1.12D, 1.08D, 1.18D, 1.02D, 1.30D, 0.88D, 0.84D
    };
    private static final double[] DUST_DX = {
            -0.94D, 0.86D, 0.10D, -0.88D, 0.94D, -0.44D, -0.16D, -0.72D, 0.80D
    };
    private static final double[] DUST_DZ = {
            -0.30D, 0.64D, -1.06D, 0.38D, -0.56D, 0.82D, 0.26D, -0.80D, 0.92D
    };
    private static final double[] DUST_PEAK = {
            1.20D, 1.46D, 0.98D, 0.67D, 0.80D, 0.56D, 1.64D, 1.08D, 0.92D
    };
    private static final double[] DUST_DELAY = {
            0.00D, 0.05D, 0.00D, 0.10D, 0.05D, 0.10D, 0.00D, 0.10D, 0.05D
    };
    private static final int[] DUST_BANK = {
            0, 1, 2, 1, 0, 2, 2, 0, 1
    };

    private static final double[] GRIT_X = {
            -0.48D, 0.28D, 0.00D, -1.18D, 0.88D, -0.18D, 0.12D, -0.60D, 1.15D, -0.96D, 0.45D, 0.58D
    };
    private static final double[] GRIT_Z = {
            0.22D, 0.12D, -0.65D, -0.20D, -0.52D, 0.84D, -0.10D, -0.72D, 0.62D, 0.75D, -1.20D, 0.48D
    };
    private static final double[] GRIT_DX = {
            -1.38D, 1.70D, 0.35D, -1.50D, 1.50D, -0.75D, -0.80D, -0.80D, 1.30D, -0.92D, 0.75D, 0.92D
    };
    private static final double[] GRIT_DZ = {
            0.46D, 0.78D, -1.80D, -0.65D, -1.05D, 1.65D, 0.34D, -1.48D, 0.88D, 1.26D, -1.35D, 1.14D
    };
    private static final double[] GRIT_APEX = {
            1.80D, 2.14D, 1.45D, 0.94D, 1.28D, 1.10D, 2.35D, 1.62D, 0.82D, 1.32D, 0.70D, 1.76D
    };
    private static final double[] GRIT_FLIGHT = {
            0.90D, 1.05D, 0.85D, 0.70D, 0.80D, 0.75D, 1.10D, 0.95D, 0.65D, 0.85D, 0.65D, 0.95D
    };
    private static final double[] GRIT_SIZE = {
            0.13D, 0.16D, 0.10D, 0.12D, 0.09D, 0.14D, 0.10D, 0.07D, 0.08D, 0.09D, 0.12D, 0.07D
    };
    private static final double[] GRIT_DELAY = {
            0.00D, 0.00D, 0.05D, 0.05D, 0.00D, 0.05D, 0.00D, 0.10D, 0.05D, 0.10D, 0.05D, 0.10D
    };

    private static final double[] HORIZONTAL_TIME = {
            0.00D, 0.10D, 0.25D, 0.40D, 0.65D, 0.90D, 1.20D
    };
    private static final double[] HORIZONTAL_PROGRESS = {
            0.00D, 0.15D, 0.38D, 0.62D, 0.82D, 0.96D, 1.08D
    };
    private static final double[] RISE_PROGRESS = {
            0.00D, 0.46D, 0.88D, 1.00D, 0.94D, 0.84D, 0.77D
    };

    private GroundDustLayout() {
    }

    public static double dustAgeSeconds(int dustIndex, double elapsedTicks) {
        return elapsedSeconds(elapsedTicks) - DUST_DELAY[dustIndex];
    }

    public static boolean dustVisible(int dustIndex, double elapsedTicks) {
        double age = dustAgeSeconds(dustIndex, elapsedTicks);
        return age >= 0.0D && age < DUST_LIFETIME_SECONDS && dustFrame(dustIndex, elapsedTicks) < DUST_FRAMES - 1;
    }

    public static double dustOffsetX(int dustIndex, double elapsedTicks) {
        return DUST_X[dustIndex] + DUST_DX[dustIndex] * horizontalProgress(dustAgeSeconds(dustIndex, elapsedTicks));
    }

    public static double dustOffsetY(int dustIndex, double elapsedTicks) {
        return ROOT_Y + DUST_PEAK[dustIndex] * riseProgress(dustAgeSeconds(dustIndex, elapsedTicks));
    }

    public static double dustOffsetZ(int dustIndex, double elapsedTicks) {
        return DUST_Z[dustIndex] + DUST_DZ[dustIndex] * horizontalProgress(dustAgeSeconds(dustIndex, elapsedTicks));
    }

    public static double dustSize(int dustIndex) {
        return DUST_SIZE[dustIndex];
    }

    public static int dustBank(int dustIndex) {
        return DUST_BANK[dustIndex];
    }

    public static int dustFrame(int dustIndex, double elapsedTicks) {
        int frame = (int) Math.floor(dustAgeSeconds(dustIndex, elapsedTicks) / FRAME_SECONDS);
        if (frame < 0) {
            return 0;
        }
        if (frame >= DUST_FRAMES) {
            return DUST_FRAMES - 1;
        }
        return frame;
    }

    public static double gritAgeSeconds(int gritIndex, double elapsedTicks) {
        return elapsedSeconds(elapsedTicks) - GRIT_DELAY[gritIndex];
    }

    public static boolean gritVisible(int gritIndex, double elapsedTicks) {
        double age = gritAgeSeconds(gritIndex, elapsedTicks);
        return age >= 0.0D && age < GRIT_FLIGHT[gritIndex];
    }

    public static double gritOffsetX(int gritIndex, double elapsedTicks) {
        return GRIT_X[gritIndex] + GRIT_DX[gritIndex] * gritProgress(gritIndex, elapsedTicks);
    }

    public static double gritOffsetY(int gritIndex, double elapsedTicks) {
        double progress = gritProgress(gritIndex, elapsedTicks);
        return ROOT_Y + 4.0D * GRIT_APEX[gritIndex] * progress * (1.0D - progress);
    }

    public static double gritOffsetZ(int gritIndex, double elapsedTicks) {
        return GRIT_Z[gritIndex] + GRIT_DZ[gritIndex] * gritProgress(gritIndex, elapsedTicks);
    }

    public static double gritSize(int gritIndex) {
        return GRIT_SIZE[gritIndex];
    }

    public static int gritBank(int gritIndex) {
        return gritIndex % GRIT_BANKS;
    }

    public static int gritAlphaRow(int gritIndex, double elapsedTicks) {
        double remaining = GRIT_FLIGHT[gritIndex] - gritAgeSeconds(gritIndex, elapsedTicks);
        if (remaining <= 0.075D) {
            return 2;
        }
        if (remaining <= 0.15D) {
            return 1;
        }
        return 0;
    }

    public static float gritPitchDegrees(int gritIndex, double elapsedTicks) {
        return (float) ((90.0D + 13.0D * gritIndex) * gritProgress(gritIndex, elapsedTicks));
    }

    public static float gritYawDegrees(int gritIndex, double elapsedTicks) {
        return (float) ((145.0D - 5.0D * gritIndex) * gritProgress(gritIndex, elapsedTicks));
    }

    public static float gritRollDegrees(int gritIndex, double elapsedTicks) {
        return (float) ((35.0D + 11.0D * gritIndex) * gritProgress(gritIndex, elapsedTicks));
    }

    private static double gritProgress(int gritIndex, double elapsedTicks) {
        double flight = GRIT_FLIGHT[gritIndex];
        if (flight <= 0.0D) {
            return 0.0D;
        }
        double progress = gritAgeSeconds(gritIndex, elapsedTicks) / flight;
        if (progress < 0.0D) {
            return 0.0D;
        }
        if (progress > 1.0D) {
            return 1.0D;
        }
        return progress;
    }

    private static double horizontalProgress(double ageSeconds) {
        return sample(ageSeconds, HORIZONTAL_PROGRESS);
    }

    private static double riseProgress(double ageSeconds) {
        return sample(ageSeconds, RISE_PROGRESS);
    }

    private static double sample(double ageSeconds, double[] values) {
        if (ageSeconds <= HORIZONTAL_TIME[0]) {
            return values[0];
        }
        int last = HORIZONTAL_TIME.length - 1;
        if (ageSeconds >= HORIZONTAL_TIME[last]) {
            return values[last];
        }
        for (int index = 1; index <= last; index++) {
            if (ageSeconds <= HORIZONTAL_TIME[index]) {
                double span = HORIZONTAL_TIME[index] - HORIZONTAL_TIME[index - 1];
                double weight = span <= 0.0D ? 1.0D : (ageSeconds - HORIZONTAL_TIME[index - 1]) / span;
                return values[index - 1] + (values[index] - values[index - 1]) * weight;
            }
        }
        return values[last];
    }

    private static double elapsedSeconds(double elapsedTicks) {
        return Math.max(0.0D, elapsedTicks) / 20.0D;
    }
}
