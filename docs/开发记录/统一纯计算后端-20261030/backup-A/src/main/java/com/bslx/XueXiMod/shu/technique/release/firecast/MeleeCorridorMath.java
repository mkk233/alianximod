package com.bslx.XueXiMod.shu.technique.release.firecast;

/** 剑前走廊的纯判定。前方从起点沿朝向伸出，不向后。 */
public final class MeleeCorridorMath {
    private MeleeCorridorMath() {
    }

    public static boolean blocked(double contactDistance, double obstacleDistance) {
        if (!Double.isFinite(contactDistance) || contactDistance < 0.0d) {
            return true;
        }
        if (!Double.isFinite(obstacleDistance)) {
            return false;
        }
        return obstacleDistance + 1.0e-4d < contactDistance;
    }

    /**
     * 朝向必须是单位向量。盒子用八个角的包围来近似相交：
     * 沿朝向的投影区间碰到 [0, length]，横向和竖向落在半宽半高内。
     */
    public static boolean overlaps(double startX, double startY, double startZ,
                                   double forwardX, double forwardY, double forwardZ,
                                   double length, double width, double height,
                                   double lowerX, double lowerY, double lowerZ,
                                   double upperX, double upperY, double upperZ) {
        if (!Double.isFinite(length) || length <= 0.0d || width < 0.0d || height < 0.0d) {
            return false;
        }
        double forwardLength = Math.sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ);
        if (forwardLength < 1.0e-8d) {
            return false;
        }
        double unitX = forwardX / forwardLength;
        double unitY = forwardY / forwardLength;
        double unitZ = forwardZ / forwardLength;
        double hintX = Math.abs(unitY) > 0.9d ? 1.0d : 0.0d;
        double hintY = Math.abs(unitY) > 0.9d ? 0.0d : 1.0d;
        double hintZ = 0.0d;
        double rightX = unitY * hintZ - unitZ * hintY;
        double rightY = unitZ * hintX - unitX * hintZ;
        double rightZ = unitX * hintY - unitY * hintX;
        double rightLength = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (rightLength < 1.0e-8d) {
            return false;
        }
        rightX /= rightLength;
        rightY /= rightLength;
        rightZ /= rightLength;
        double upX = rightY * unitZ - rightZ * unitY;
        double upY = rightZ * unitX - rightX * unitZ;
        double upZ = rightX * unitY - rightY * unitX;
        double centerX = (lowerX + upperX) * 0.5d;
        double centerY = (lowerY + upperY) * 0.5d;
        double centerZ = (lowerZ + upperZ) * 0.5d;
        double halfX = Math.abs(upperX - lowerX) * 0.5d;
        double halfY = Math.abs(upperY - lowerY) * 0.5d;
        double halfZ = Math.abs(upperZ - lowerZ) * 0.5d;
        double offsetX = centerX - startX;
        double offsetY = centerY - startY;
        double offsetZ = centerZ - startZ;
        double along = offsetX * unitX + offsetY * unitY + offsetZ * unitZ;
        double lateral = offsetX * rightX + offsetY * rightY + offsetZ * rightZ;
        double vertical = offsetX * upX + offsetY * upY + offsetZ * upZ;
        double alongPad = halfX * Math.abs(unitX) + halfY * Math.abs(unitY) + halfZ * Math.abs(unitZ);
        double lateralPad = halfX * Math.abs(rightX) + halfY * Math.abs(rightY) + halfZ * Math.abs(rightZ);
        double verticalPad = halfX * Math.abs(upX) + halfY * Math.abs(upY) + halfZ * Math.abs(upZ);
        boolean alongHit = along + alongPad >= 0.0d && along - alongPad <= length;
        boolean lateralHit = Math.abs(lateral) <= width * 0.5d + lateralPad;
        boolean verticalHit = Math.abs(vertical) <= height * 0.5d + verticalPad;
        return alongHit && lateralHit && verticalHit;
    }

    /** 目标靠近射线的一端。单位朝向。负值表示在起点后方。 */
    public static double contactDistance(double startX, double startY, double startZ,
                                         double unitX, double unitY, double unitZ,
                                         double centerX, double centerY, double centerZ,
                                         double alongPad) {
        double offsetX = centerX - startX;
        double offsetY = centerY - startY;
        double offsetZ = centerZ - startZ;
        double along = offsetX * unitX + offsetY * unitY + offsetZ * unitZ;
        double near = along - Math.max(0.0d, alongPad);
        return near < 0.0d ? 0.0d : near;
    }
}
