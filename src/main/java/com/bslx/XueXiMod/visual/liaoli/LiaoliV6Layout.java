package com.bslx.XueXiMod.visual.liaoli;

/**
 * 燎里 v6 的空间关系。环、手持剑和分流仍按 64 像素身高换算。
 * 背后八剑用剑阵姿态 v7：32 模型单位对应 1.8 格，站立时绕柄点 +90° X，剑尖朝下。
 * 正深度朝玩家前方，负深度朝后方。横向正方向是面向 -Z 时的模型 +X，也就是玩家左侧。
 * 这里不生成实体，也不改伤害。
 */
public final class LiaoliV6Layout {
    public static final int VISUAL = 9;
    public static final int SWORD_COUNT = 8;
    public static final int RING_COUNT = 3;
    public static final int IGNITE_FRAMES = 12;
    public static final int FLOW_FRAMES = 24;
    public static final int STREAM_FRAMES = 24;
    public static final int RING_FRAMES = 24;
    /** 每一道从剑尖走到淡出结束的时长。不取模，走完就停。 */
    public static final double RING_SPAN = 20.0d;
    /** 三道依次晚出发，后两道不会被第一道的结束剪掉。 */
    public static final double RING_STAGGER = 6.0d;
    /** 环线先超过剑阵，这段之后才把透明度收到 0。 */
    public static final double RING_FADE_OPEN = 0.78d;
    /** 贴图环线大约在 96 像素画面的 32 像素处，方片半边用 48/32 才能让环线落在空间半径上。 */
    public static final double RING_QUAD_SCALE = 48.0d / 32.0d;
    /**
     * 环线半径。站立含剑身与火舌半宽的外缘约 2.89 格，横身俯冲约 2.76，蹲下俯冲约 2.97。
     * 先长到 3.80，淡出时再收到 4.10，仍然只增不减。
     */
    public static final double RING_LINE_START = 0.20d;
    public static final double RING_LINE_CLEAR = 3.80d;
    public static final double RING_LINE_END = 4.10d;
    /**
     * 环心前后。剑尖在 +2.053。剑阵最深处，站立火舌约 -1.78，俯冲约 -2.21。
     * 先退到 -3.40，淡出时再退到 -4.10。
     */
    public static final double RING_BACK_CLEAR = -3.40d;
    public static final double RING_BACK_END = -4.10d;
    public static final String SWORD_UNLIT = "textures/effect/liaoli_v6/sword_unlit.png";
    public static final String SWORD_HOT = "textures/effect/liaoli_v6/sword_hot.png";
    public static final String BACK_IGNITE = "textures/effect/liaoli_v6/back_ignite.png";
    public static final String BACK_FLOW = "textures/effect/liaoli_v6/back_flow.png";
    public static final String THRUST_STREAM = "textures/effect/liaoli_v6/thrust_backflow.png";
    public static final String REAR_RING = "textures/effect/liaoli_v6/rear_ring.png";

    private static final double[] FAN_DEGREES = {-180.0d, -154.0d, -128.0d, -102.0d, -78.0d, -52.0d, -26.0d, 0.0d};
    private static final double BLOCKS_PER_PIXEL = 1.8d / 64.0d;
    private static final double EYE_PIXELS = 58.0d;
    /** 确认模型的占位人体高 32，对应游戏身高 1.8 格。 */
    private static final double BLOCKS_PER_UNIT = 1.8d / 32.0d;
    private static final double STANDING_EYE_BLOCKS = 1.62d;
    private static final double HILT_RADIUS_UNITS = 7.0d;
    private static final double TIP_RADIUS_UNITS = 18.0d;
    private static final double HILT_BACK_UNITS = 5.5d;
    private static final double TIP_BACK_UNITS = 17.5d;
    private static final double FAN_CENTER_UNITS = 18.0d;
    private static final double WAIST_UNITS = 16.0d;
    private static final double DIVE_SHIFT_UNITS = -12.0d;
    /** 旧火焰比剑身多出的比例，用来让新剑尖之后仍留着火舌。 */
    private static final double FLAME_PAST_BLADE = (118.0d - 62.0d) / (62.0d - 23.0d);
    private static final double SWORD_PIXEL_WIDTH = 64.0d;
    private static final double SWORD_PIXEL_HEIGHT = 20.0d;
    private static final double FLAME_PIXEL_WIDTH = 128.0d;
    private static final double FLAME_PIXEL_HEIGHT = 48.0d;
    private static final double STREAM_PIXEL_WIDTH = 128.0d;
    private static final double STREAM_PIXEL_HEIGHT = 64.0d;

    private LiaoliV6Layout() {
    }

    /** 相对眼睛的方块偏移。横向、升高、前方。 */
    public static final class Mark {
        private final double lateral;
        private final double rise;
        private final double forward;

        private Mark(double lateral, double rise, double forward) {
            this.lateral = lateral;
            this.rise = rise;
            this.forward = forward;
        }

        public double lateral() {
            return this.lateral;
        }

        public double rise() {
            return this.rise;
        }

        public double forward() {
            return this.forward;
        }
    }

    public static double fanDegrees(int swordIndex) {
        if (swordIndex < 0 || swordIndex >= FAN_DEGREES.length) {
            return FAN_DEGREES[0];
        }
        return FAN_DEGREES[swordIndex];
    }

    /** 站立、眼睛离脚 1.62 格。旧检查仍调用这组入口。 */
    public static Mark swordHilt(int swordIndex) {
        return swordHilt(swordIndex, 0.0F, 0.0F, STANDING_EYE_BLOCKS);
    }

    public static Mark swordTip(int swordIndex) {
        return swordTip(swordIndex, 0.0F, 0.0F, STANDING_EYE_BLOCKS);
    }

    public static Mark swordHilt(int swordIndex, float pitchDegrees, float diveAmount, double eyeAboveFeet) {
        return posed(droppedHilt(swordIndex), pitchDegrees, diveAmount, eyeAboveFeet);
    }

    public static Mark swordTip(int swordIndex, float pitchDegrees, float diveAmount, double eyeAboveFeet) {
        return posed(droppedTip(swordIndex), pitchDegrees, diveAmount, eyeAboveFeet);
    }

    /** 火从身后中线起，不从剑尖起。 */
    public static Mark flameStart() {
        return flameStart(0.0F, 0.0F, STANDING_EYE_BLOCKS);
    }

    public static Mark flameEnd(int swordIndex) {
        return flameEnd(swordIndex, 0.0F, 0.0F, STANDING_EYE_BLOCKS);
    }

    public static Mark flameStart(float pitchDegrees, float diveAmount, double eyeAboveFeet) {
        return posed(new FanPoint(0.0d, FAN_CENTER_UNITS, HILT_BACK_UNITS), pitchDegrees, diveAmount, eyeAboveFeet);
    }

    public static Mark flameEnd(int swordIndex, float pitchDegrees, float diveAmount, double eyeAboveFeet) {
        FanPoint hilt = droppedHilt(swordIndex);
        FanPoint tip = droppedTip(swordIndex);
        return posed(span(hilt, tip, 1.0d + FLAME_PAST_BLADE), pitchDegrees, diveAmount, eyeAboveFeet);
    }

    /** 身体向前横置时从 0 收到 1。抬头或后仰不把剑阵送去腿部。 */
    public static float diveAmount(float pitchDegrees) {
        float portion = -pitchDegrees / 90.0F;
        if (portion < 0.0F) {
            return 0.0F;
        }
        if (portion > 1.0F) {
            return 1.0F;
        }
        return portion;
    }

    public static boolean flipFlame(int swordIndex) {
        return swordIndex >= 4;
    }

    /** 左右成对的剑共用同一段火焰相位。返回 1 到 4。 */
    public static int mirrorPair(int swordIndex) {
        int order = swordIndex + 1;
        int reflected = SWORD_COUNT - swordIndex;
        return order < reflected ? order : reflected;
    }

    public static Mark thrustHilt() {
        return fromPixels(12.0d, 20.0d, 10.0d);
    }

    public static Mark thrustTip() {
        return fromPixels(12.0d, 20.0d, 73.0d);
    }

    /** 贴图局部正横向的起点，靠近剑尖。 */
    public static Mark streamNearTip() {
        return fromPixels(12.0d, 20.0d, 63.0d);
    }

    /** 贴图局部正横向的终点，在玩家身后。 */
    public static Mark streamRear() {
        return fromPixels(12.0d, 20.0d, -60.0d);
    }

    /** 三道全部走完所需的拍数。尾迹比单道多留出两段错峰。 */
    public static double ringShowTicks() {
        return RING_SPAN + RING_STAGGER * (RING_COUNT - 1);
    }

    /** 这一道自己的年龄。还没到出发时刻时为负，不折回。 */
    public static double ringLocalAge(double ageTicks, int ringIndex) {
        double order = ringIndex;
        if (order < 0.0d) {
            order = 0.0d;
        }
        return ageTicks - order * RING_STAGGER;
    }

    /** 0 在剑尖，1 在背后淡出结束。年龄超出区间时停在端点，不取小数。 */
    public static double ringPortion(double localAge) {
        if (localAge <= 0.0d || RING_SPAN <= 0.0d) {
            return 0.0d;
        }
        if (localAge >= RING_SPAN) {
            return 1.0d;
        }
        return localAge / RING_SPAN;
    }

    public static double ringRadius(double portion) {
        double grown = RING_LINE_START + (RING_LINE_CLEAR - RING_LINE_START) * beforeFade(portion);
        return grown + (RING_LINE_END - RING_LINE_CLEAR) * afterFade(portion);
    }

    /** 起点与手持刺击剑尖相同，沿剑尖指向剑柄的深度继续退到身后。 */
    public static Mark ringCenter(double portion) {
        Mark tip = thrustTip();
        double depth = tip.forward() + (RING_BACK_CLEAR - tip.forward()) * beforeFade(portion);
        depth += (RING_BACK_END - RING_BACK_CLEAR) * afterFade(portion);
        return new Mark(tip.lateral(), tip.rise(), depth);
    }

    public static double ringAlpha(double portion) {
        double fading = afterFade(portion);
        double remain = 1.0d - fading;
        if (remain < 0.0d) {
            return 0.0d;
        }
        if (remain > 1.0d) {
            return 1.0d;
        }
        return remain;
    }

    private static double beforeFade(double portion) {
        double unit = unitRange(portion);
        if (unit >= RING_FADE_OPEN) {
            return 1.0d;
        }
        if (RING_FADE_OPEN <= 0.0d) {
            return 1.0d;
        }
        return unit / RING_FADE_OPEN;
    }

    private static double afterFade(double portion) {
        double unit = unitRange(portion);
        if (unit <= RING_FADE_OPEN || RING_FADE_OPEN >= 1.0d) {
            return 0.0d;
        }
        return (unit - RING_FADE_OPEN) / (1.0d - RING_FADE_OPEN);
    }

    private static double unitRange(double portion) {
        if (portion < 0.0d) {
            return 0.0d;
        }
        if (portion > 1.0d) {
            return 1.0d;
        }
        return portion;
    }

    /** 金剑从柄向尖覆盖的比例。12 拍后整柄点亮。 */
    public static double hotReach(double ageTicks) {
        double lived = ageTicks;
        if (lived < 0.0d) {
            lived = 0.0d;
        }
        if (lived > IGNITE_FRAMES) {
            lived = IGNITE_FRAMES;
        }
        double front = 127.0d * Math.pow(lived / IGNITE_FRAMES, 1.2d);
        double cutoff = (front - 23.0d) / 0.62d;
        double portion = cutoff / SWORD_PIXEL_WIDTH;
        if (portion < 0.0d) {
            return 0.0d;
        }
        if (portion > 1.0d) {
            return 1.0d;
        }
        return portion;
    }

    public static boolean igniting(double ageTicks) {
        return ageTicks < IGNITE_FRAMES;
    }

    public static int igniteFrame(double ageTicks) {
        int frame = (int) Math.floor(ageTicks);
        if (frame < 0) {
            return 0;
        }
        if (frame >= IGNITE_FRAMES) {
            return IGNITE_FRAMES - 1;
        }
        return frame;
    }

    public static int flowFrame(double ageTicks, int pairShift) {
        return loopFrame(Math.floor(ageTicks) + pairShift * 3.0d, FLOW_FRAMES);
    }

    public static int loopFrame(double ageTicks, int frameCount) {
        if (frameCount <= 0) {
            return 0;
        }
        int frame = (int) Math.floor(ageTicks) % frameCount;
        if (frame < 0) {
            frame += frameCount;
        }
        return frame;
    }

    public static Mark between(Mark start, Mark end, double portion) {
        double span = portion;
        if (span < 0.0d) {
            span = 0.0d;
        }
        if (span > 1.0d) {
            span = 1.0d;
        }
        return new Mark(
                start.lateral + (end.lateral - start.lateral) * span,
                start.rise + (end.rise - start.rise) * span,
                start.forward + (end.forward - start.forward) * span);
    }

    public static double swordHalfWidth(double length) {
        return ribbonHalfWidth(length, SWORD_PIXEL_HEIGHT, SWORD_PIXEL_WIDTH, 0.85d);
    }

    public static double flameHalfWidth(double length) {
        return ribbonHalfWidth(length, FLAME_PIXEL_HEIGHT, FLAME_PIXEL_WIDTH, 0.75d);
    }

    public static double streamHalfWidth(double length) {
        return ribbonHalfWidth(length, STREAM_PIXEL_HEIGHT, STREAM_PIXEL_WIDTH, 0.7d);
    }

    public static double heldHalfWidth(double length) {
        return ribbonHalfWidth(length, SWORD_PIXEL_HEIGHT, SWORD_PIXEL_WIDTH, 1.0d);
    }

    private static double ribbonHalfWidth(double length, double pixelHeight, double pixelWidth, double thickness) {
        if (length <= 0.0d || pixelWidth <= 0.0d) {
            return 0.0d;
        }
        return length * (pixelHeight / pixelWidth) * thickness * 0.5d;
    }

    private static FanPoint droppedHilt(int swordIndex) {
        double radians = Math.toRadians(fanDegrees(swordIndex));
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        return new FanPoint(
                cosine * HILT_RADIUS_UNITS,
                FAN_CENTER_UNITS + (-sine) * HILT_RADIUS_UNITS,
                HILT_BACK_UNITS);
    }

    /** 未旋转的尖在更远的 +Z。绕柄点 +90° X 后，站立时尖端朝下。 */
    private static FanPoint droppedTip(int swordIndex) {
        double radians = Math.toRadians(fanDegrees(swordIndex));
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        FanPoint hilt = droppedHilt(swordIndex);
        double tipAcross = cosine * TIP_RADIUS_UNITS;
        double tipUp = FAN_CENTER_UNITS + (-sine) * TIP_RADIUS_UNITS;
        double deltaAcross = tipAcross - hilt.across;
        double deltaUp = tipUp - hilt.upward;
        double deltaBack = TIP_BACK_UNITS - hilt.backward;
        return new FanPoint(hilt.across + deltaAcross, hilt.upward - deltaBack, hilt.backward + deltaUp);
    }

    private static FanPoint span(FanPoint start, FanPoint end, double scale) {
        return new FanPoint(
                start.across + (end.across - start.across) * scale,
                start.upward + (end.upward - start.upward) * scale,
                start.backward + (end.backward - start.backward) * scale);
    }

    /**
     * 先沿身体局部下移，再绕腰点做与原版相同的 X 旋转。
     * 模型 +Z 是背后，所以前方取负。
     */
    private static Mark posed(FanPoint point, float pitchDegrees, float diveAmount, double eyeAboveFeet) {
        double shiftedUp = point.upward + DIVE_SHIFT_UNITS * diveAmount;
        double offsetUp = shiftedUp - WAIST_UNITS;
        double radians = Math.toRadians(pitchDegrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        double turnedUp = offsetUp * cosine - point.backward * sine;
        double turnedBack = offsetUp * sine + point.backward * cosine;
        double feetUp = (WAIST_UNITS + turnedUp) * BLOCKS_PER_UNIT;
        double safeEye = eyeAboveFeet;
        if (!Double.isFinite(safeEye) || safeEye < 0.05d || safeEye > 2.0d) {
            safeEye = STANDING_EYE_BLOCKS;
        }
        return new Mark(point.across * BLOCKS_PER_UNIT, feetUp - safeEye, -(turnedBack * BLOCKS_PER_UNIT));
    }

    private static Mark fromPixels(double lateralPixels, double risePixels, double depthPixels) {
        return new Mark(
                lateralPixels * BLOCKS_PER_PIXEL,
                (risePixels - EYE_PIXELS) * BLOCKS_PER_PIXEL,
                depthPixels * BLOCKS_PER_PIXEL);
    }

    /** 确认模型里的一点。across 向玩家左侧，upward 向上，backward 朝背后。 */
    private static final class FanPoint {
        private final double across;
        private final double upward;
        private final double backward;

        private FanPoint(double across, double upward, double backward) {
            this.across = across;
            this.upward = upward;
            this.backward = backward;
        }
    }
}
