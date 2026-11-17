package com.bslx.XueXiMod.visual.fire;

/**
 * 燎原、引燃、焚天和炉心的纯视觉取样。不读世界，不改伤害。
 * 旧竖条帧号从 1 计；焚天全身火焰使用 0 起的图集行。
 */
public final class FireVisualLayout {
    public static final int SPREAD = 1;
    public static final int SKY = 2;
    public static final int KINDLE = 3;
    public static final int HEAT_LOOP = 1;
    public static final int OVERDRIVE_LOOP = 2;
    public static final int COOL_ONCE = 3;

    public static final int WAVE_FRAMES = 12;
    public static final int FLAME_FRAMES = 24;
    public static final int FIELD_LIFE_TICKS = 200;
    public static final int SKY_FRAMES = 52;
    public static final int PULSE_LAST = 40;
    public static final int EMBER_FIRST = 41;
    public static final int EMBER_FRAMES = 12;
    public static final int KINDLE_FRAMES = 52;
    public static final int KINDLE_LOOP_FRAMES = 40;
    public static final int KINDLE_EMBER_FIRST = 41;
    public static final int KINDLE_EMBER_FRAMES = 12;
    public static final int BODY_FRAMES = 19;
    public static final int BODY_LOOP_FRAMES = 10;
    public static final int BODY_PULSE_FIRST = 10;
    public static final int BODY_PULSE_FRAMES = 3;
    public static final int BODY_END_FIRST = 13;
    public static final int BODY_END_FRAMES = 6;
    public static final double BODY_CAMERA_CLEARANCE = 0.04d;
    public static final int FURNACE_FRAMES = 16;
    public static final int FURNACE_FRAME_MILLIS = 75;

    public static final String GROUND_FLAME = "textures/effect/liaoyuan_v2/ground_flame.png";
    public static final String GROUND_EMBERS = "textures/effect/liaoyuan_v2/ground_embers.png";
    public static final String SPREAD_WAVE = "textures/effect/liaoyuan_v2/spread_wave.png";
    /** 地表灰烬四变体。各一张 16×16 静帧，按方块坐标选用，不按 24 帧切片。 */
    public static final String GROUND_ASH = "textures/effect/liaoyuan_v3/ground_ash.png";
    public static final String GROUND_ASH_B = "textures/effect/liaoyuan_v3/ground_ash_b.png";
    public static final String GROUND_ASH_C = "textures/effect/liaoyuan_v3/ground_ash_c.png";
    public static final String GROUND_ASH_D = "textures/effect/liaoyuan_v3/ground_ash_d.png";
    public static final int ASH_VARIANTS = 4;
    /** 地表余烬竖条。16×384，24 帧，每帧 16×16、50ms。 */
    public static final String GROUND_ASH_EMBERS = "textures/effect/liaoyuan_v3/ground_embers.png";
    public static final int ASH_EMBER_FRAMES = 24;
    public static final String CONFLAGRATION = "textures/effect/fentian_v2/conflagration.png";
    public static final String KINDLE_SHEET = "textures/effect/kindle_v4/kindle.png";
    public static final String BODY_FLAME = "textures/effect/fentian_v3/conflagration.png";
    public static final String HEAT_SHEET = "textures/effect/furnace_v1/heat_up.png";
    public static final String OVERDRIVE_SHEET = "textures/effect/furnace_v1/overdrive.png";
    /** 冷却 v4.1 竖条。蒸汽三层各 64×1024，冲击波 96×1536，16 帧自上而下。 */
    public static final String COOL_REAR_SHEET = "textures/effect/cool_v41/cool_v4_1_rear_strip.png";
    public static final String COOL_FRONT_SHEET = "textures/effect/cool_v41/cool_v4_1_front_strip.png";
    public static final String COOL_FRAGMENT_SHEET = "textures/effect/cool_v41/cool_v4_1_fragments_strip.png";
    public static final String COOL_WAVE_SHEET = "textures/effect/cool_v41/cool_v4_1_shockwave_strip.png";

    private FireVisualLayout() {
    }

    public static long burnToken(int entityId, int burnKind) {
        return ((long) entityId << 4) | (burnKind & 15);
    }

    /** 引燃在真实轨道存活期间循环 1–40 帧。进度来自轨道的间隔、段号和剩余等待。 */
    public static int kindleSustainFrame(int interval, int cursor, int wait) {
        int progress = progressTicks(interval, cursor, wait);
        if (progress < 0) {
            progress = 0;
        }
        return (progress % KINDLE_LOOP_FRAMES) + 1;
    }

    /** 自然结束才进入 41–52 余烬。传入负数或越界表示不再画。 */
    public static int kindleEmberFrame(int emberTicks) {
        if (emberTicks < 0 || emberTicks >= KINDLE_EMBER_FRAMES) {
            return 0;
        }
        return KINDLE_EMBER_FIRST + emberTicks;
    }

    /** 焚天持续段循环 0–9 帧。结算当拍改由短亮覆盖，不在这里推进。 */
    public static int bodyLoopFrame(int interval, int cursor, int wait) {
        int progress = progressTicks(interval, cursor, wait);
        if (progress < 0) {
            progress = 0;
        }
        return progress % BODY_LOOP_FRAMES;
    }

    /** 真实结算节拍的三帧短亮，索引 10–12。越界返回 -1。 */
    public static int bodyPulseFrame(int pulseTicks) {
        if (pulseTicks < 0 || pulseTicks >= BODY_PULSE_FRAMES) {
            return -1;
        }
        return BODY_PULSE_FIRST + pulseTicks;
    }

    /** 终止后的六帧单次收束，索引 13–18。越界返回 -1。 */
    public static int bodyEndFrame(int endTicks) {
        if (endTicks < 0 || endTicks >= BODY_END_FRAMES) {
            return -1;
        }
        return BODY_END_FIRST + endTicks;
    }

    /** 焚天背景在真实轨道存活期间循环旧条 1–40 帧。 */
    public static int skySustainFrame(int interval, int cursor, int wait) {
        int progress = progressTicks(interval, cursor, wait);
        if (progress < 0) {
            progress = 0;
        }
        return (progress % PULSE_LAST) + 1;
    }

    /** 焚天背景只在自然结束后单次播放 41–52 帧。越界返回 0。 */
    public static int skyEmberFrame(int emberTicks) {
        return emberFrame(emberTicks);
    }

    public static final int SKY_END_FRAMES = BODY_PULSE_FRAMES + BODY_END_FRAMES;
    public static final int SKY_HELD_FALLBACK = PULSE_LAST - 1;
    private static final double[] SKY_HELD_ALPHA = {
            1.0d, 0.94d, 0.82d, 0.66d, 0.48d, 0.30d, 0.15d, 0.05d, 0.0d};
    private static final double[] SKY_EMBER_ALPHA = {
            0.0d, 0.18d, 0.36d, 0.55d, 0.72d, 0.82d, 0.65d, 0.35d, 0.0d};
    private static final int[] SKY_EMBER_INDEX = {40, 41, 42, 44, 45, 46, 48, 50, 51};

    public static int skyHeldIndex(int interval, int cursor, int wait) {
        int row = textureRow(skySustainFrame(interval, cursor, wait), SKY_FRAMES);
        if (row < 0 || row >= PULSE_LAST) {
            return SKY_HELD_FALLBACK;
        }
        return row;
    }

    public static int skyEndTick(double ageTicks) {
        if (!Double.isFinite(ageTicks) || ageTicks < 0.0d) {
            return -1;
        }
        int tick = (int) Math.floor(ageTicks);
        if (tick < 0 || tick >= SKY_END_FRAMES) {
            return -1;
        }
        return tick;
    }

    public static int skyEndEmberIndex(int tick) {
        if (tick < 0 || tick >= SKY_EMBER_INDEX.length) {
            return -1;
        }
        return SKY_EMBER_INDEX[tick];
    }

    public static double skyEndHeldAlpha(double ageTicks) {
        return skyEndAlpha(SKY_HELD_ALPHA, ageTicks);
    }

    public static double skyEndEmberAlpha(double ageTicks) {
        return skyEndAlpha(SKY_EMBER_ALPHA, ageTicks);
    }

    public static int skyEndOpacity(double alpha) {
        if (!Double.isFinite(alpha) || alpha <= 0.0d) {
            return 0;
        }
        if (alpha >= 1.0d) {
            return 255;
        }
        return (int) Math.round(alpha * 255.0d);
    }

    private static double skyEndAlpha(double[] keys, double ageTicks) {
        if (keys == null || keys.length == 0 || !Double.isFinite(ageTicks) || ageTicks < 0.0d
                || ageTicks >= keys.length) {
            return 0.0d;
        }
        int tick = (int) Math.floor(ageTicks);
        if (tick >= keys.length - 1) {
            return keys[keys.length - 1];
        }
        double fraction = ageTicks - tick;
        return keys[tick] + (keys[tick + 1] - keys[tick]) * fraction;
    }

    /** 火浪向外走完之前，火海只铺到已经过的半径。之后停在真实半径上。 */
    public static double coveredRadius(double radius, double ageTicks) {
        if (!Double.isFinite(radius) || radius <= 0.0d || !Double.isFinite(ageTicks) || ageTicks <= 0.0d) {
            return 0.0d;
        }
        double grown = ageTicks / WAVE_FRAMES;
        if (grown > 1.0d) {
            grown = 1.0d;
        }
        return radius * grown;
    }

    public static boolean waveVisible(double ageTicks) {
        return Double.isFinite(ageTicks) && ageTicks >= 0.0d && ageTicks < WAVE_FRAMES;
    }

    public static int stripFrame(double ageTicks, int frameCount) {
        if (frameCount <= 0 || !Double.isFinite(ageTicks)) {
            return 0;
        }
        int frame = (int) Math.floor(ageTicks) % frameCount;
        if (frame < 0) {
            frame += frameCount;
        }
        return frame;
    }

    /** 附着火焰的强弱来自消费快照，不跟焚天的十拍。 */
    public static double clingScale(int tiers, int remainder) {
        double strength = tiers;
        if (remainder > 0) {
            strength += remainder / 10.0d;
        }
        if (strength < 0.0d) {
            strength = 0.0d;
        }
        if (strength > 10.0d) {
            strength = 10.0d;
        }
        return 0.45d + 0.55d * (strength / 10.0d);
    }

    public static int progressTicks(int interval, int cursor, int wait) {
        if (interval <= 0 || cursor < 0) {
            return 0;
        }
        int spent = interval - wait;
        if (spent < 0) {
            spent = 0;
        }
        if (spent > interval) {
            spent = interval;
        }
        return cursor * interval + spent;
    }

    /**
     * 按这条计划自己的段数取样。低火种停在自己的拍上，不读到第 40 帧。
     * 进度 1–3 是聚火，之后每 4 拍一爆。
     */
    public static int pulseFrame(int segmentCount, int interval, int cursor, int wait) {
        if (segmentCount <= 0 || interval <= 0) {
            return 0;
        }
        int progress = progressTicks(interval, cursor, wait);
        int limit = segmentCount * interval;
        if (limit > PULSE_LAST) {
            limit = PULSE_LAST;
        }
        if (progress < 0) {
            progress = 0;
        }
        if (progress > limit) {
            progress = limit;
        }
        if (progress <= 0) {
            return 1;
        }
        return progress;
    }

    public static int finalPulseFrame(int segmentCount, int interval) {
        if (segmentCount <= 0 || interval <= 0) {
            return 0;
        }
        int frame = segmentCount * interval;
        if (frame > PULSE_LAST) {
            frame = PULSE_LAST;
        }
        if (frame < 1) {
            frame = 1;
        }
        return frame;
    }

    public static int emberFrame(int emberTicks) {
        if (emberTicks < 0 || emberTicks >= EMBER_FRAMES) {
            return 0;
        }
        return EMBER_FIRST + emberTicks;
    }

    public static int textureRow(int frame, int frameCount) {
        if (frame <= 0 || frameCount <= 0) {
            return 0;
        }
        int row = frame - 1;
        if (row >= frameCount) {
            return frameCount - 1;
        }
        return row;
    }

    /** 余量段比完整段小。前面的完整段保持原大。 */
    public static double skyScale(int segmentCount, int cursor, int remainder) {
        if (remainder <= 0 || segmentCount <= 0 || cursor < segmentCount - 1) {
            return 1.0d;
        }
        double portion = remainder / 10.0d;
        if (portion < 0.35d) {
            portion = 0.35d;
        }
        if (portion > 1.0d) {
            portion = 1.0d;
        }
        return portion;
    }

    /** 升级和直接进入过载都走强火焰。冷却是另一条单次。 */
    public static int furnaceLoop(boolean enteredOverdrive, boolean upgraded, boolean cooled) {
        if (cooled) {
            return COOL_ONCE;
        }
        if (enteredOverdrive || upgraded) {
            return OVERDRIVE_LOOP;
        }
        return HEAT_LOOP;
    }

    /** 把相对火场原点的水平偏移收成竖条某一帧里的贴图坐标。中心是 0.5。 */
    public static double discCoordinate(double offset, double radius) {
        if (!Double.isFinite(offset) || !Double.isFinite(radius) || radius <= 0.0d) {
            return Double.NaN;
        }
        return 0.5d + offset / (radius * 2.0d);
    }

    public static int loopFrame(long elapsedMillis) {
        if (elapsedMillis < 0L) {
            elapsedMillis = 0L;
        }
        return (int) ((elapsedMillis / FURNACE_FRAME_MILLIS) % FURNACE_FRAMES);
    }

    /** 播完 16 帧就停。返回负数表示这次蒸汽已经结束。 */
    public static int coolFrame(long elapsedMillis) {
        if (elapsedMillis < 0L) {
            return -1;
        }
        int frame = (int) (elapsedMillis / FURNACE_FRAME_MILLIS);
        if (frame >= FURNACE_FRAMES) {
            return -1;
        }
        return frame;
    }
}
