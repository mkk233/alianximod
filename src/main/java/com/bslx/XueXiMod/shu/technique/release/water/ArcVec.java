package com.bslx.XueXiMod.shu.technique.release.water;

/** 不依赖游戏的三维向量。东、上、南对应世界的 X、Y、Z。 */
public final class ArcVec {
    private final double east;
    private final double above;
    private final double south;

    public ArcVec(double east, double above, double south) {
        this.east = east;
        this.above = above;
        this.south = south;
    }

    public double east() {
        return east;
    }

    public double above() {
        return above;
    }

    public double south() {
        return south;
    }

    public ArcVec plus(ArcVec other) {
        return new ArcVec(east + other.east, above + other.above, south + other.south);
    }

    public ArcVec minus(ArcVec other) {
        return new ArcVec(east - other.east, above - other.above, south - other.south);
    }

    public ArcVec scale(double factor) {
        return new ArcVec(east * factor, above * factor, south * factor);
    }

    public double dot(ArcVec other) {
        return east * other.east + above * other.above + south * other.south;
    }

    public ArcVec cross(ArcVec other) {
        return new ArcVec(
                above * other.south - south * other.above,
                south * other.east - east * other.south,
                east * other.above - above * other.east);
    }

    public double lengthSquared() {
        return east * east + above * above + south * south;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public ArcVec normalize() {
        double magnitude = length();
        if (magnitude < 1.0E-12) {
            return new ArcVec(0.0, 0.0, 0.0);
        }
        return scale(1.0 / magnitude);
    }

    public boolean finite() {
        return Double.isFinite(east) && Double.isFinite(above) && Double.isFinite(south);
    }
}
