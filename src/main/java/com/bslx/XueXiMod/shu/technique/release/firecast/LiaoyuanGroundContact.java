package com.bslx.XueXiMod.shu.technique.release.firecast;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * 燎原自己的方形贴地采样。不改 {@link SurfaceContactField#HALF_EXTENT}，也不改灼地。
 * 每列只记可见顶面；展开裁剪和碰撞盒相交留到检测时做。
 */
public final class LiaoyuanGroundContact {
    private static final double EDGE_EPSILON = 1.0E-4d;

    private LiaoyuanGroundContact() {
    }

    public static List<ColumnSample> collect(ServerLevel level, double originX, double originY, double originZ,
                                             double halfWidth) {
        if (level == null || !GroundFireMath.finite(originX) || !GroundFireMath.finite(originY)
                || !GroundFireMath.finite(originZ) || !GroundFireMath.finite(halfWidth) || halfWidth <= 0.0d) {
            return List.of();
        }
        double lowX = originX - halfWidth;
        double highX = originX + halfWidth;
        double lowZ = originZ - halfWidth;
        double highZ = originZ + halfWidth;
        int columnStartX = Mth.floor(lowX);
        int columnEndX = Mth.ceil(highX) - 1;
        int columnStartZ = Mth.floor(lowZ);
        int columnEndZ = Mth.ceil(highZ) - 1;
        ArrayList<ColumnSample> gathered = new ArrayList<>();
        int columnX = columnStartX;
        while (columnX <= columnEndX) {
            int columnZ = columnStartZ;
            while (columnZ <= columnEndZ) {
                BlockPos column = new BlockPos(columnX, Mth.floor(originY), columnZ);
                if (level.hasChunkAt(column)) {
                    double overlapLowX = Math.max(columnX, lowX);
                    double overlapHighX = Math.min(columnX + 1.0d, highX);
                    double overlapLowZ = Math.max(columnZ, lowZ);
                    double overlapHighZ = Math.min(columnZ + 1.0d, highZ);
                    if (overlapHighX - overlapLowX > EDGE_EPSILON && overlapHighZ - overlapLowZ > EDGE_EPSILON) {
                        double sampleX = (overlapLowX + overlapHighX) * 0.5d;
                        double sampleZ = (overlapLowZ + overlapHighZ) * 0.5d;
                        double surfaceTop = StandSurface.columnTop(level, sampleX, sampleZ, originY,
                                GroundFireFields.VERTICAL_SLACK);
                        if (!Double.isNaN(surfaceTop)) {
                            gathered.add(new ColumnSample(columnX, columnZ, surfaceTop));
                        }
                    }
                }
                columnZ += 1;
            }
            columnX += 1;
        }
        return List.copyOf(gathered);
    }

    /** 一列已经采样的地表。检测时再裁到当前展开方形里。 */
    public static final class ColumnSample {
        private final int columnX;
        private final int columnZ;
        private final double surfaceTop;

        private ColumnSample(int columnX, int columnZ, double surfaceTop) {
            this.columnX = columnX;
            this.columnZ = columnZ;
            this.surfaceTop = surfaceTop;
        }

        public boolean meets(double originX, double originZ, double halfWidth, double layerHeight, AABB body) {
            if (body == null || !GroundFireMath.finite(halfWidth) || halfWidth <= 0.0d
                    || !GroundFireMath.finite(layerHeight) || layerHeight <= 0.0d) {
                return false;
            }
            double lowX = Math.max(this.columnX, originX - halfWidth);
            double highX = Math.min(this.columnX + 1.0d, originX + halfWidth);
            double lowZ = Math.max(this.columnZ, originZ - halfWidth);
            double highZ = Math.min(this.columnZ + 1.0d, originZ + halfWidth);
            if (highX - lowX <= EDGE_EPSILON || highZ - lowZ <= EDGE_EPSILON) {
                return false;
            }
            if (body.maxX <= lowX || body.minX >= highX || body.maxZ <= lowZ || body.minZ >= highZ) {
                return false;
            }
            SurfaceContactField.Slice slice = new SurfaceContactField.Slice(lowX, this.surfaceTop, lowZ,
                    highX, this.surfaceTop + layerHeight, highZ);
            return slice.meets(body);
        }
    }
}
