package com.bslx.XueXiMod.client;

/**
 * 周身水环 v8 的摆放和贴图帧。顶点已去掉展示枢轴。
 * 次序是当地正 Y 旋转、静态 Z 倾斜、最小旋转对齐法线，再统一缩放。
 */
public final class WaterRingV8Math {
    public static final double MODEL_RADIUS = 11.775;
    public static final double TILT_DEGREES = 3.0;
    public static final int SPIN_TICKS = 48;
    public static final int FRAME_TICKS = 2;
    public static final int FRAME_COUNT = 16;
    public static final int LOOP_TICKS = 96;
    public static final int ATLAS_WIDTH = 768;
    public static final int ATLAS_HEIGHT = 640;
    public static final int LOGICAL_WIDTH = 256;
    private static final int ALIGN_SAME = 0;
    private static final int ALIGN_OPPOSITE = 1;
    private static final int ALIGN_AXIS = 2;

    private WaterRingV8Math() {
    }

    public static int frameIndex(double ageTicks) {
        double elapsed = ageTicks;
        if (Double.isNaN(elapsed) || elapsed < 0.0) {
            elapsed = 0.0;
        }
        long wholeTicks = (long) Math.floor(elapsed);
        return (int) Math.floorMod(wholeTicks / FRAME_TICKS, FRAME_COUNT);
    }

    public static double spinRadians(double ageTicks) {
        double elapsed = ageTicks;
        if (Double.isNaN(elapsed) || elapsed < 0.0) {
            elapsed = 0.0;
        }
        return elapsed / SPIN_TICKS * (Math.PI * 2.0);
    }

    /**
     * 把源图像素映射进合并图当前帧。内缩作用在整帧边界，不按单个四边形各自收缩。
     */
    public static void mapUv(int atlasWidth, int atlasHeight, int logicalWidth,
                              int atlasOriginX, int frameHeight, int frameSlot,
                              double sourceU, double sourceV, float[] destination) {
        int frame = Math.floorMod(frameSlot, FRAME_COUNT);
        double clampedU = clampUnit(sourceU, logicalWidth);
        double clampedV = clampUnit(sourceV, frameHeight);
        double pixelU = atlasOriginX + 0.5 + (clampedU / logicalWidth) * (logicalWidth - 1.0);
        double pixelV = frame * (double) frameHeight + 0.5 + (clampedV / frameHeight) * (frameHeight - 1.0);
        destination[0] = (float) (pixelU / atlasWidth);
        destination[1] = (float) (pixelV / atlasHeight);
    }

    public static int nearEyeAlpha(boolean fadeNearEye, double distance) {
        if (!fadeNearEye) {
            return 255;
        }
        if (distance < 0.35) {
            return 0;
        }
        if (distance < 0.7) {
            return (int) (255.0 * (distance - 0.35) / 0.35);
        }
        return 255;
    }

    public static float relativeToCamera(double world, double camera) {
        return (float) (world - camera);
    }

    private static double clampUnit(double sample, double upper) {
        if (sample < 0.0) {
            return 0.0;
        }
        if (sample > upper) {
            return upper;
        }
        return sample;
    }

    public static final class PlacedFrame {
        private final double spinCosine;
        private final double spinSine;
        private final double tiltCosine;
        private final double tiltSine;
        private final double scale;
        private final int alignKind;
        private final double axisX;
        private final double axisY;
        private final double axisZ;
        private final double alignRadians;

        private PlacedFrame(double spinCosine, double spinSine, double tiltCosine, double tiltSine, double scale,
                             int alignKind, double axisX, double axisY, double axisZ, double alignRadians) {
            this.spinCosine = spinCosine;
            this.spinSine = spinSine;
            this.tiltCosine = tiltCosine;
            this.tiltSine = tiltSine;
            this.scale = scale;
            this.alignKind = alignKind;
            this.axisX = axisX;
            this.axisY = axisY;
            this.axisZ = axisZ;
            this.alignRadians = alignRadians;
        }

        public static PlacedFrame prepare(double tiltDegrees, double spinRadians, double scale,
                                           double normalX, double normalY, double normalZ) {
            double spinCosine = Math.cos(spinRadians);
            double spinSine = Math.sin(spinRadians);
            double tiltRadians = Math.toRadians(tiltDegrees);
            double tiltCosine = Math.cos(tiltRadians);
            double tiltSine = Math.sin(tiltRadians);
            double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            double unitX;
            double unitY;
            double unitZ;
            if (normalLength < 1.0E-8) {
                unitX = 0.0;
                unitY = 1.0;
                unitZ = 0.0;
            } else {
                unitX = normalX / normalLength;
                unitY = normalY / normalLength;
                unitZ = normalZ / normalLength;
            }
            double rawAxisX = unitZ;
            double rawAxisY = 0.0;
            double rawAxisZ = -unitX;
            double sine = Math.sqrt(rawAxisX * rawAxisX + rawAxisY * rawAxisY + rawAxisZ * rawAxisZ);
            double cosine = unitY;
            if (sine < 1.0E-8) {
                if (cosine >= 0.0) {
                    return new PlacedFrame(spinCosine, spinSine, tiltCosine, tiltSine, scale,
                            ALIGN_SAME, 0.0, 0.0, 0.0, 0.0);
                }
                return new PlacedFrame(spinCosine, spinSine, tiltCosine, tiltSine, scale,
                        ALIGN_OPPOSITE, 1.0, 0.0, 0.0, Math.PI);
            }
            return new PlacedFrame(spinCosine, spinSine, tiltCosine, tiltSine, scale, ALIGN_AXIS,
                    rawAxisX / sine, rawAxisY / sine, rawAxisZ / sine, Math.atan2(sine, cosine));
        }

        public void apply(double localX, double localY, double localZ, double[] destination) {
            double spunX = localX * spinCosine + localZ * spinSine;
            double spunY = localY;
            double spunZ = -localX * spinSine + localZ * spinCosine;
            double tiltedX = spunX * tiltCosine - spunY * tiltSine;
            double tiltedY = spunX * tiltSine + spunY * tiltCosine;
            double tiltedZ = spunZ;
            if (alignKind == ALIGN_AXIS) {
                rotateAround(tiltedX, tiltedY, tiltedZ, axisX, axisY, axisZ, alignRadians, destination);
                destination[0] *= scale;
                destination[1] *= scale;
                destination[2] *= scale;
                return;
            }
            if (alignKind == ALIGN_OPPOSITE) {
                destination[0] = tiltedX * scale;
                destination[1] = -tiltedY * scale;
                destination[2] = -tiltedZ * scale;
                return;
            }
            destination[0] = tiltedX * scale;
            destination[1] = tiltedY * scale;
            destination[2] = tiltedZ * scale;
        }
    }

    private static void rotateAround(double pointX, double pointY, double pointZ,
                                      double axisX, double axisY, double axisZ, double radians,
                                      double[] destination) {
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        double dot = axisX * pointX + axisY * pointY + axisZ * pointZ;
        double crossX = axisY * pointZ - axisZ * pointY;
        double crossY = axisZ * pointX - axisX * pointZ;
        double crossZ = axisX * pointY - axisY * pointX;
        double preserved = 1.0 - cosine;
        destination[0] = pointX * cosine + crossX * sine + axisX * dot * preserved;
        destination[1] = pointY * cosine + crossY * sine + axisY * dot * preserved;
        destination[2] = pointZ * cosine + crossZ * sine + axisZ * dot * preserved;
    }
}
