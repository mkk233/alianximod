package com.bslx.XueXiMod.client;

/**
 * 雾雨领域的内接椭圆环。只产生 16 个首尾相接的侧面，没有顶面和底面。
 * 周向 U 按累计边长连续增加，整圈收成整数次纹理重复。
 */
final class DomainMistRingLayout {
    static final int SIDE_COUNT = 16;
    static final int FRAME_COUNT = 32;
    static final int HORIZONTAL_CAPS = 0;
    static final float FRAME_EDGE_PAD = 0.5f / 64.0f;

    /** 相对水平中心的局部坐标，不把世界平移写进顶点。 */
    private final double[] pointX = new double[SIDE_COUNT];
    private final double[] pointZ = new double[SIDE_COUNT];
    private final float[] outwardX = new float[SIDE_COUNT];
    private final float[] outwardZ = new float[SIDE_COUNT];
    private final double[] cumulative = new double[SIDE_COUNT + 1];
    private final double lowerY;
    private final double upperY;
    private final double centerX;
    private final double centerZ;
    private int textureTurns = 1;

    private DomainMistRingLayout(double lowerY, double upperY, double centerX, double centerZ) {
        this.lowerY = lowerY;
        this.upperY = upperY;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    static DomainMistRingLayout tryBuild(double lowX, double lowY, double lowZ, double highX, double highY, double highZ) {
        double spanX = highX - lowX;
        double spanY = highY - lowY;
        double spanZ = highZ - lowZ;
        if (spanX <= 1.0E-4d || spanY <= 1.0E-4d || spanZ <= 1.0E-4d) {
            return null;
        }
        double centerX = (lowX + highX) * 0.5d;
        double centerZ = (lowZ + highZ) * 0.5d;
        double radiusX = spanX * 0.5d;
        double radiusZ = spanZ * 0.5d;
        DomainMistRingLayout layout = new DomainMistRingLayout(lowY, highY, centerX, centerZ);
        for (int side = 0; side < SIDE_COUNT; side++) {
            double turn = (Math.PI * 2.0d * side) / SIDE_COUNT;
            layout.pointX[side] = radiusX * Math.cos(turn);
            layout.pointZ[side] = radiusZ * Math.sin(turn);
        }
        for (int side = 0; side < SIDE_COUNT; side++) {
            int next = (side + 1) % SIDE_COUNT;
            double edgeX = layout.pointX[next] - layout.pointX[side];
            double edgeZ = layout.pointZ[next] - layout.pointZ[side];
            double edgeLength = Math.sqrt(edgeX * edgeX + edgeZ * edgeZ);
            if (edgeLength <= 1.0E-4d) {
                return null;
            }
            layout.cumulative[side + 1] = layout.cumulative[side] + edgeLength;
            double rawX = edgeZ;
            double rawZ = -edgeX;
            double normalLength = Math.sqrt(rawX * rawX + rawZ * rawZ);
            if (normalLength <= 1.0E-4d) {
                return null;
            }
            layout.outwardX[side] = (float) (rawX / normalLength);
            layout.outwardZ[side] = (float) (rawZ / normalLength);
        }
        double perimeter = layout.cumulative[SIDE_COUNT];
        double aspect = perimeter / (spanY * 2.0d);
        long rounded = Math.round(aspect);
        if (rounded > 1L && rounded <= Integer.MAX_VALUE) {
            layout.textureTurns = (int) rounded;
        }
        return layout;
    }

    int quadCount() {
        return SIDE_COUNT;
    }

    int textureTurns() {
        return textureTurns;
    }

    double lowerY() {
        return lowerY;
    }

    double upperY() {
        return upperY;
    }

    double centerX() {
        return centerX;
    }

    double centerZ() {
        return centerZ;
    }

    double pointX(int side) {
        return pointX[side];
    }

    double pointZ(int side) {
        return pointZ[side];
    }

    /** 先用 double 减去相机，再加上局部偏移，最后才转 float。 */
    float relativeX(int side, double cameraX) {
        return (float) ((centerX - cameraX) + pointX[side]);
    }

    float relativeZ(int side, double cameraZ) {
        return (float) ((centerZ - cameraZ) + pointZ[side]);
    }

    float relativeLowerY(double cameraY) {
        return (float) (lowerY - cameraY);
    }

    float relativeUpperY(double cameraY) {
        return (float) (upperY - cameraY);
    }

    float outwardX(int side) {
        return outwardX[side];
    }

    float outwardZ(int side) {
        return outwardZ[side];
    }

    /** 点号可取到侧面数量，最后一点的 U 是整数圈，不折回 0。 */
    float uAt(int point) {
        return (float) (cumulative[point] / cumulative[SIDE_COUNT] * textureTurns);
    }

    static float frameV(int frame, float localV) {
        return (frame + localV) / (float) FRAME_COUNT;
    }

    public static void main(String[] commandLine) {
        if (commandLine.length != 0) {
            throw new IllegalStateException("环状布局自检不接受参数");
        }
        if (HORIZONTAL_CAPS != 0 || SIDE_COUNT != 16 || FRAME_COUNT != 32) {
            throw new IllegalStateException("环状布局常量偏离裁决");
        }
        DomainMistRingLayout sample = tryBuild(0.0d, 0.0d, 0.0d, 7.0d, 8.0d, 7.0d);
        require(sample != null, "7x7x8 应能生成");
        require(sample.textureTurns() == 1, "7x7x8 应为一圈纹理，实际 " + sample.textureTurns());
        require(sample.centerX() + sample.pointX(0) == 7.0d && sample.centerZ() + sample.pointZ(0) == 3.5d,
                "角度 0 应落在宽边中点而不是方角");
        require(sample.lowerY() == 0.0d && sample.upperY() == 8.0d, "上下沿应贴着盒子");
        checkClosed(sample);
        DomainMistRingLayout wide = tryBuild(0.0d, 0.0d, 0.0d, 40.0d, 2.0d, 40.0d);
        require(wide != null && wide.textureTurns() == 31, "40x2x40 最近整数圈应为 31，实际 " + (wide == null ? "null" : wide.textureTurns()));
        checkClosed(wide);
        DomainMistRingLayout tall = tryBuild(0.0d, 0.0d, 0.0d, 7.0d, 40.0d, 7.0d);
        require(tall != null && tall.textureTurns() == 1, "高盒子不足一圈时仍应保留 1 圈");
        checkClosed(tall);
        DomainMistRingLayout ellipse = tryBuild(0.0d, 0.0d, 0.0d, 10.0d, 8.0d, 4.0d);
        require(ellipse != null, "椭圆环应能生成");
        checkClosed(ellipse);
        require(tryBuild(0.0d, 0.0d, 0.0d, 0.0d, 8.0d, 7.0d) == null, "零宽度不能生成环");
        checkFarShift(30_000_000.0d, 30_000_000.0d, 30_000_000.0d);
        checkFarShift(-30_000_000.0d, -30_000_000.0d, -30_000_000.0d);
        checkFarShift(30_000_000.0d, -30_000_000.0d, -30_000_000.0d);
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            float topV = frameV(frame, FRAME_EDGE_PAD);
            float bottomV = frameV(frame, 1.0f - FRAME_EDGE_PAD);
            float frameStart = frame / (float) FRAME_COUNT;
            float frameEnd = (frame + 1) / (float) FRAME_COUNT;
            require(topV > frameStart && bottomV < frameEnd && topV < bottomV, "帧 " + frame + " 的纵向 UV 串帧");
        }
        System.out.println("RING_LAYOUT_OK turns=" + sample.textureTurns()
                + " wideTurns=" + wide.textureTurns()
                + " ellipseTurns=" + ellipse.textureTurns()
                + " far=30000000");
    }

    private static void checkFarShift(double shiftX, double shiftY, double shiftZ) {
        DomainMistRingLayout origin = tryBuild(0.0d, 0.0d, 0.0d, 7.0d, 8.0d, 7.0d);
        DomainMistRingLayout shifted = tryBuild(
                shiftX, shiftY, shiftZ, shiftX + 7.0d, shiftY + 8.0d, shiftZ + 7.0d);
        require(origin != null && shifted != null, "三千万附近的环不应退化消失");
        checkClosed(shifted);
        require(shifted.quadCount() == SIDE_COUNT, "远坐标仍应是 16 面");
        require(shifted.textureTurns() == origin.textureTurns(), "平移后圈数应一致");
        double cameraX = 1.25d;
        double cameraY = 2.5d;
        double cameraZ = -0.75d;
        require(shifted.relativeLowerY(cameraY + shiftY) == origin.relativeLowerY(cameraY), "下沿相对相机应稳定");
        require(shifted.relativeUpperY(cameraY + shiftY) == origin.relativeUpperY(cameraY), "上沿相对相机应稳定");
        for (int side = 0; side < SIDE_COUNT; side++) {
            int next = (side + 1) % SIDE_COUNT;
            float originX = origin.relativeX(side, cameraX);
            float originZ = origin.relativeZ(side, cameraZ);
            float shiftedX = shifted.relativeX(side, cameraX + shiftX);
            float shiftedZ = shifted.relativeZ(side, cameraZ + shiftZ);
            require(shiftedX == originX && shiftedZ == originZ, "相对相机的顶点应与原点布局一致");
            require(shifted.uAt(side) == origin.uAt(side), "周向 U 不应随平移改变");
            require(shifted.outwardX(side) == origin.outwardX(side)
                    && shifted.outwardZ(side) == origin.outwardZ(side), "法线不应随平移改变");
            float edgeX = origin.relativeX(next, cameraX) - originX;
            float edgeZ = origin.relativeZ(next, cameraZ) - originZ;
            float shiftedEdgeX = shifted.relativeX(next, cameraX + shiftX) - shiftedX;
            float shiftedEdgeZ = shifted.relativeZ(next, cameraZ + shiftZ) - shiftedZ;
            require(shiftedEdgeX == edgeX && shiftedEdgeZ == edgeZ, "平移后的边应与原点一致");
            require(edgeX * edgeX + edgeZ * edgeZ > 0.25f, "16 条边都不应退化");
        }
    }

    private static void checkClosed(DomainMistRingLayout layout) {
        require(layout.quadCount() == SIDE_COUNT, "侧面数量应为 16");
        require(layout.uAt(0) == 0.0f, "周向 U 应从 0 开始");
        double firstX = layout.pointX(0);
        double firstZ = layout.pointZ(0);
        double previousX = firstX;
        double previousZ = firstZ;
        float previousEndU = layout.uAt(0);
        for (int side = 0; side < layout.quadCount(); side++) {
            int next = (side + 1) % layout.quadCount();
            double startX = layout.pointX(side);
            double startZ = layout.pointZ(side);
            double endX = layout.pointX(next);
            double endZ = layout.pointZ(next);
            require(startX == previousX && startZ == previousZ, "相邻面应共享端点");
            float startU = layout.uAt(side);
            float endU = layout.uAt(side + 1);
            require(endU > startU, "周向 U 应沿边长增加");
            require(startU == previousEndU, "相邻面应共享周向 U");
            if (side > 0) {
                require(startU != 0.0f, "后续面不能把 U 重置为 0");
            }
            double midX = (startX + endX) * 0.5d;
            double midZ = (startZ + endZ) * 0.5d;
            double outward = layout.outwardX(side) * midX + layout.outwardZ(side) * midZ;
            require(outward > 0.0d, "法线应指向环外");
            previousX = endX;
            previousZ = endZ;
            previousEndU = endU;
        }
        require(previousX == firstX && previousZ == firstZ, "最后一面应回到首点");
        require(previousEndU == (float) layout.textureTurns(), "周向 U 应在整数圈闭合");
    }

    private static void require(boolean passed, String detail) {
        if (!passed) {
            throw new IllegalStateException(detail);
        }
    }
}
