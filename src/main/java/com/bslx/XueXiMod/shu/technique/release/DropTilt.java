package com.bslx.XueXiMod.shu.technique.release;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 显式垂落的一次抽样。竖直位移仍走公共二次下落，这里只把该位移铺到固定锚点。
 * 没有足够高度，或落柱所在区块未加载时，调用方保持原来的竖直路径。
 */
public final class DropTilt {
    /** 确认版上限。没有单独的角度配置时用这一值。 */
    public static final double TILT_LIMIT_DEGREES = 30.0D;
    /** 已经贴在承托面上时不再横移，避免把接触点误判成还要斜着汇合。 */
    private static final double SURFACE_SLACK = 0.05D;

    private DropTilt() {
    }

    public record Placement(Vec3 start, Vec3 anchor, Vec3 heading) {
    }

    public record Heading(Vec3 horizontalUnit, double tiltRadians) {
    }

    /** 同一发的方位和倾角只由种子决定，飞行中不再抽。 */
    public static Heading choose(long sampleSeed, int copyOrdinal, int spawnOrdinal) {
        RandomSource random = RandomSource.create(mixOrdinal(sampleSeed, copyOrdinal, spawnOrdinal));
        double tiltRadians = Math.toRadians(random.nextDouble() * TILT_LIMIT_DEGREES);
        double azimuthTurn = random.nextDouble() * Math.PI * 2.0D;
        return new Heading(new Vec3(Math.cos(azimuthTurn), 0.0D, Math.sin(azimuthTurn)), tiltRadians);
    }

    /**
     * 沿旧的竖直落柱找第一个碰撞顶面。只读已加载区块。
     * 找不到承托面时锚在扫描下沿，使斜线仍汇向这条柱，而不是眼高。
     */
    public static Placement placement(ServerLevel level, Vec3 columnStart, long sampleSeed, int copyOrdinal, int spawnOrdinal) {
        if (level == null || columnStart == null || !finite(columnStart)) {
            return null;
        }
        Vec3 anchorPoint = columnAnchor(level, columnStart);
        double verticalSpan = columnStart.y - anchorPoint.y;
        if (!Double.isFinite(verticalSpan) || verticalSpan < SURFACE_SLACK) {
            return null;
        }
        long columnSalt = Double.doubleToLongBits(columnStart.x)
                ^ Long.rotateLeft(Double.doubleToLongBits(columnStart.z), 17);
        Heading chosen = choose(sampleSeed ^ columnSalt, copyOrdinal, spawnOrdinal);
        double offsetDistance = verticalSpan * Math.tan(chosen.tiltRadians());
        Vec3 startPoint = new Vec3(
                anchorPoint.x + chosen.horizontalUnit().x * offsetDistance,
                columnStart.y,
                anchorPoint.z + chosen.horizontalUnit().z * offsetDistance);
        return new Placement(startPoint, anchorPoint, unitToward(startPoint, anchorPoint));
    }

    /** 竖直下落距离与公共公式相同，水平分量按该距离占整段高度的比例收回锚点。 */
    public static Vec3 pointAtFall(Vec3 startPoint, Vec3 anchorPoint, double verticalFall) {
        if (startPoint == null) {
            return anchorPoint;
        }
        double fallen = verticalFall < 0.0D ? 0.0D : verticalFall;
        if (anchorPoint == null) {
            return new Vec3(startPoint.x, startPoint.y - fallen, startPoint.z);
        }
        double verticalSpan = startPoint.y - anchorPoint.y;
        if (!Double.isFinite(verticalSpan) || verticalSpan < SURFACE_SLACK) {
            return new Vec3(startPoint.x, startPoint.y - fallen, startPoint.z);
        }
        double progress = fallen / verticalSpan;
        return new Vec3(
                startPoint.x + (anchorPoint.x - startPoint.x) * progress,
                startPoint.y - fallen,
                startPoint.z + (anchorPoint.z - startPoint.z) * progress);
    }

    public static Vec3 unitToward(Vec3 startPoint, Vec3 anchorPoint) {
        if (startPoint == null || anchorPoint == null) {
            return new Vec3(0.0D, -1.0D, 0.0D);
        }
        Vec3 towardAnchor = anchorPoint.subtract(startPoint);
        if (towardAnchor.lengthSqr() < 1.0E-8D) {
            return new Vec3(0.0D, -1.0D, 0.0D);
        }
        return towardAnchor.normalize();
    }

    /** 陷阱每拍仍下降固定竖直距离，斜向步长只补上对应的水平分量。 */
    public static Vec3 fallStep(Vec3 heading, double verticalStep) {
        double downward = heading == null ? 1.0D : -heading.y;
        if (heading == null || downward < 1.0E-6D) {
            return new Vec3(0.0D, -verticalStep, 0.0D);
        }
        return heading.normalize().scale(verticalStep / downward);
    }

    private static Vec3 columnAnchor(ServerLevel level, Vec3 columnStart) {
        BlockPos columnBlock = BlockPos.containing(columnStart.x, columnStart.y, columnStart.z);
        if (!level.hasChunkAt(columnBlock)) {
            return columnStart;
        }
        int highestBlock = columnBlock.getY();
        int buildCeiling = level.getMaxBuildHeight() - 1;
        if (highestBlock > buildCeiling) {
            highestBlock = buildCeiling;
        }
        int lowestBuild = level.getMinBuildHeight();
        if (highestBlock < lowestBuild) {
            return columnStart;
        }
        BlockPos.MutableBlockPos columnCursor = new BlockPos.MutableBlockPos();
        for (int blockAbove = highestBlock; blockAbove >= lowestBuild; blockAbove--) {
            columnCursor.set(columnBlock.getX(), blockAbove, columnBlock.getZ());
            if (!level.hasChunkAt(columnCursor)) {
                return new Vec3(columnStart.x, blockAbove + 1.0D, columnStart.z);
            }
            BlockState state = level.getBlockState(columnCursor);
            VoxelShape shape = state.getCollisionShape(level, columnCursor, CollisionContext.empty());
            if (shape.isEmpty()) {
                continue;
            }
            boolean insideShape = false;
            double hitCeiling = Double.NaN;
            for (AABB box : shape.toAabbs()) {
                double west = columnCursor.getX() + box.minX;
                double east = columnCursor.getX() + box.maxX;
                double north = columnCursor.getZ() + box.minZ;
                double south = columnCursor.getZ() + box.maxZ;
                if (columnStart.x < west - 1.0E-6D || columnStart.x > east + 1.0E-6D
                        || columnStart.z < north - 1.0E-6D || columnStart.z > south + 1.0E-6D) {
                    continue;
                }
                double boxFloor = columnCursor.getY() + box.minY;
                double boxCeiling = columnCursor.getY() + box.maxY;
                if (columnStart.y <= boxCeiling && columnStart.y >= boxFloor) {
                    insideShape = true;
                    break;
                }
                if (boxCeiling < columnStart.y && (Double.isNaN(hitCeiling) || boxCeiling > hitCeiling)) {
                    hitCeiling = boxCeiling;
                }
            }
            if (insideShape) {
                return columnStart;
            }
            if (!Double.isNaN(hitCeiling)) {
                return new Vec3(columnStart.x, hitCeiling, columnStart.z);
            }
        }
        return new Vec3(columnStart.x, lowestBuild, columnStart.z);
    }

    private static boolean finite(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static long mixOrdinal(long sampleSeed, int copyOrdinal, int spawnOrdinal) {
        long mixed = sampleSeed
                ^ (copyOrdinal * 0x9E3779B97F4A7C15L)
                ^ (spawnOrdinal * 0xBF58476D1CE4E5B9L);
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}
