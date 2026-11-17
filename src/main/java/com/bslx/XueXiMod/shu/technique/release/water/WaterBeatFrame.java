package com.bslx.XueXiMod.shu.technique.release.water;

/** 从最终弧快照抄出的世界前向和法线。东、上、南对应动作帧的 X、Y、Z。 */
public final class WaterBeatFrame {
    private final double forwardEast;
    private final double forwardAbove;
    private final double forwardSouth;
    private final double normalEast;
    private final double normalAbove;
    private final double normalSouth;

    private WaterBeatFrame(double forwardEast, double forwardAbove, double forwardSouth,
                           double normalEast, double normalAbove, double normalSouth) {
        this.forwardEast = forwardEast;
        this.forwardAbove = forwardAbove;
        this.forwardSouth = forwardSouth;
        this.normalEast = normalEast;
        this.normalAbove = normalAbove;
        this.normalSouth = normalSouth;
    }

    public static WaterBeatFrame copy(ArcBandSnapshot snapshot) {
        if (snapshot == null || snapshot.forwardAxis() == null || snapshot.normalAxis() == null) {
            return null;
        }
        ArcVec forward = snapshot.forwardAxis();
        ArcVec normal = snapshot.normalAxis();
        return new WaterBeatFrame(forward.east(), forward.above(), forward.south(),
                normal.east(), normal.above(), normal.south());
    }

    public double forwardEast() {
        return forwardEast;
    }

    public double forwardAbove() {
        return forwardAbove;
    }

    public double forwardSouth() {
        return forwardSouth;
    }

    public double normalEast() {
        return normalEast;
    }

    public double normalAbove() {
        return normalAbove;
    }

    public double normalSouth() {
        return normalSouth;
    }
}
