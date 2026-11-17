import com.bslx.XueXiMod.client.WaterRingV8Math;

/** 纯数学核对，不启动 Gradle。数字来自 blockbench 金标准减枢轴。 */
public final class WaterRingRenderCheck {
    private WaterRingRenderCheck() {
    }

    public static void main(String[] programArguments) {
        double[] placed = new double[3];
        WaterRingV8Math.PlacedFrame resting = WaterRingV8Math.PlacedFrame.prepare(
                3.0, WaterRingV8Math.spinRadians(0), 1.0, 0.0, 1.0, 0.0);
        resting.apply(11.8, 1.5, 0.0, placed);
        expectNear("tick0", placed, 11.705324575739557, 2.1155085857986, 0.0);

        WaterRingV8Math.PlacedFrame quarter = WaterRingV8Math.PlacedFrame.prepare(
                3.0, WaterRingV8Math.spinRadians(12), 1.0, 0.0, 1.0, 0.0);
        quarter.apply(11.8, 1.5, 0.0, placed);
        expectNear("tick12", placed, -0.07850393436441305, 1.497944302131863, -11.8);

        WaterRingV8Math.PlacedFrame closed = WaterRingV8Math.PlacedFrame.prepare(
                3.0, WaterRingV8Math.spinRadians(96), 1.0, 0.0, 1.0, 0.0);
        double[] again = new double[3];
        resting.apply(9.96187744909645, 0.816405745722356, -8.736332050075912, placed);
        closed.apply(9.96187744909645, 0.816405745722356, -8.736332050075912, again);
        if (Math.abs(placed[0] - again[0]) > 1.0e-8
                || Math.abs(placed[1] - again[1]) > 1.0e-8
                || Math.abs(placed[2] - again[2]) > 1.0e-8) {
            throw new IllegalStateException("0 与 96 tick 不一致");
        }
        if (WaterRingV8Math.frameIndex(0) != 0 || WaterRingV8Math.frameIndex(96) != 0
                || WaterRingV8Math.frameIndex(32) != 0 || WaterRingV8Math.frameIndex(30) != 15) {
            throw new IllegalStateException("帧序号不对");
        }

        float[] texture = new float[2];
        WaterRingV8Math.mapUv(768, 640, 256, 0, 24, 0, 0.0, 24.0, texture);
        expectUv("body-bottom", texture[1], 23.5 / 640.0);
        WaterRingV8Math.mapUv(768, 640, 256, 256, 40, 1, 0.0, 0.0, texture);
        expectUv("outer-frame", texture[1], 40.5 / 640.0);
        WaterRingV8Math.mapUv(768, 640, 256, 512, 40, 15, 256.0, 40.0, texture);
        expectUv("inner-last", texture[0], 767.5 / 768.0);
        expectUv("inner-last-v", texture[1], (15 * 40 + 39.5) / 640.0);

        if (WaterRingV8Math.nearEyeAlpha(true, 0.2) != 0
                || WaterRingV8Math.nearEyeAlpha(true, 0.525) != 127
                || WaterRingV8Math.nearEyeAlpha(false, 0.1) != 255) {
            throw new IllegalStateException("近眼透明度不对");
        }
        float relative = WaterRingV8Math.relativeToCamera(30000000.25, 29999999.5);
        float broken = (float) 30000000.25 - (float) 29999999.5;
        if (Math.abs(relative - 0.75f) > 1.0e-6f || Math.abs(broken - relative) < 1.0e-3f) {
            throw new IllegalStateException("远坐标精度不对 relative=" + relative + " broken=" + broken);
        }

        double[] lowImage = new double[3];
        double[] highImage = new double[3];
        imageAt(0.89, lowImage);
        imageAt(0.91, highImage);
        double dot = lowImage[0] * highImage[0] + lowImage[1] * highImage[1] + lowImage[2] * highImage[2];
        if (dot < 0.99) {
            throw new IllegalStateException("越过 0.9 时新对齐跳变 dot=" + dot);
        }
        WaterRingV8Math.PlacedFrame scaled = WaterRingV8Math.PlacedFrame.prepare(
                0.0, 0.0, 0.9 / WaterRingV8Math.MODEL_RADIUS, 0.0, 1.0, 0.0);
        scaled.apply(11.775, 0.0, 0.0, placed);
        double reach = Math.sqrt(placed[0] * placed[0] + placed[1] * placed[1] + placed[2] * placed[2]);
        if (Math.abs(reach - 0.9) > 1.0e-9) {
            throw new IllegalStateException("缩放没有把模型半径变成护体半径 " + reach);
        }
        System.out.println("java-render-check-ok");
    }

    private static void imageAt(double above, double[] destination) {
        double horizontal = Math.sqrt(1.0 - above * above);
        WaterRingV8Math.PlacedFrame frame = WaterRingV8Math.PlacedFrame.prepare(
                0.0, 0.0, 1.0, horizontal, above, 0.0);
        frame.apply(1.0, 0.0, 0.0, destination);
    }

    private static void expectNear(String label, double[] actual, double expectedX, double expectedY, double expectedZ) {
        if (Math.abs(actual[0] - expectedX) > 1.0e-9
                || Math.abs(actual[1] - expectedY) > 1.0e-9
                || Math.abs(actual[2] - expectedZ) > 1.0e-9) {
            throw new IllegalStateException(label + " 实际 "
                    + actual[0] + " " + actual[1] + " " + actual[2]);
        }
    }

    private static void expectUv(String label, float actual, double expected) {
        if (Math.abs(actual - (float) expected) > 1.0e-6) {
            throw new IllegalStateException(label + " uv " + actual + " != " + expected);
        }
    }
}
