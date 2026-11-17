package com.bslx.XueXiMod.shu.technique.release.firecast;

import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定世界坐标的方形贴地检测。只读碰撞顶面，不放方块，不改地形，也不加载未加载区块。
 * 中心向四面各伸出 {@link #HALF_EXTENT} 格，所以边长是 4 格。同一水平位置上、参考高度上下 3 格内的每一片合法顶面都保留。
 */
public final class SurfaceContactField {
    public static final double HALF_EXTENT = 2.0d;

    public static double configuredHalfExtent() {
        return BalanceService.number(SpecialBalance.SCORCH_FIELD, SpecialBalance.HALF_EXTENT);
    }
    public static final double FORWARD_STEP = 1.0d;
    public static final double VERTICAL_SLACK = 3.0d;
    public static final double CLEARANCE = 1.0d;
    private static final double AREA_EPSILON = 1.0E-4d;

    private SurfaceContactField() {
    }

    public static List<Slice> collect(ServerLevel level, double centerX, double centerZ, double referenceY) {
        if (level == null || !GroundFireMath.finite(centerX) || !GroundFireMath.finite(centerZ) || !GroundFireMath.finite(referenceY)) {
            return List.of();
        }
        double halfExtent = configuredHalfExtent();
        double lowX = centerX - halfExtent;
        double highX = centerX + halfExtent;
        double lowZ = centerZ - halfExtent;
        double highZ = centerZ + halfExtent;
        int lowColumnX = Mth.floor(lowX);
        int highColumnX = Mth.ceil(highX) - 1;
        int lowColumnZ = Mth.floor(lowZ);
        int highColumnZ = Mth.ceil(highZ) - 1;
        int lowLayer = Mth.floor(referenceY - VERTICAL_SLACK) - 1;
        int highLayer = Mth.floor(referenceY + VERTICAL_SLACK);
        ArrayList<Volume> volumes = new ArrayList<>();
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
        int columnX = lowColumnX;
        while (columnX <= highColumnX) {
            int columnZ = lowColumnZ;
            while (columnZ <= highColumnZ) {
                column.set(columnX, lowLayer, columnZ);
                if (level.hasChunkAt(column)) {
                    int layerY = lowLayer;
                    while (layerY <= highLayer) {
                        column.set(columnX, layerY, columnZ);
                        BlockState state = level.getBlockState(column);
                        VoxelShape shape = state.getCollisionShape(level, column, CollisionContext.empty());
                        if (!shape.isEmpty()) {
                            for (AABB box : shape.toAabbs()) {
                                if (box == null) {
                                    continue;
                                }
                                AABB worldBox = box.move(columnX, layerY, columnZ);
                                Volume clipped = clip(worldBox, lowX, highX, lowZ, highZ);
                                if (clipped != null) {
                                    volumes.add(clipped);
                                }
                            }
                        }
                        layerY++;
                    }
                }
                columnZ++;
            }
            columnX++;
        }
        ArrayList<Slice> gathered = new ArrayList<>();
        for (int volumeIndex = 0; volumeIndex < volumes.size(); volumeIndex++) {
            Volume volume = volumes.get(volumeIndex);
            if (Math.abs(volume.highY - referenceY) > VERTICAL_SLACK) {
                continue;
            }
            if (volume.highX - volume.lowX <= AREA_EPSILON || volume.highZ - volume.lowZ <= AREA_EPSILON) {
                continue;
            }
            List<Flat> remnants = new ArrayList<>();
            remnants.add(new Flat(volume.lowX, volume.lowZ, volume.highX, volume.highZ));
            for (int other = 0; other < volumes.size(); other++) {
                if (other == volumeIndex || remnants.isEmpty()) {
                    continue;
                }
                Volume blocker = volumes.get(other);
                if (!blocksClearance(blocker, volume.highY)) {
                    continue;
                }
                Flat cut = new Flat(blocker.lowX, blocker.lowZ, blocker.highX, blocker.highZ);
                ArrayList<Flat> next = new ArrayList<>();
                for (Flat remnant : remnants) {
                    next.addAll(subtract(remnant, cut));
                }
                remnants = next;
            }
            for (Flat remnant : remnants) {
                if (remnant.highX - remnant.lowX <= AREA_EPSILON || remnant.highZ - remnant.lowZ <= AREA_EPSILON) {
                    continue;
                }
                gathered.add(new Slice(remnant.lowX, volume.highY, remnant.lowZ,
                        remnant.highX, volume.highY + CLEARANCE, remnant.highZ));
            }
        }
        return mergeEquivalent(gathered);
    }

    /** 同高度、共边且并集仍是矩形的片合成一片。面积不减，重叠的不同片保留。 */
    private static List<Slice> mergeEquivalent(List<Slice> gathered) {
        if (gathered == null || gathered.isEmpty()) {
            return List.of();
        }
        ArrayList<Slice> pending = new ArrayList<>();
        for (Slice slice : gathered) {
            if (slice == null || !absorb(pending, slice)) {
                if (slice != null) {
                    pending.add(slice);
                }
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            int left = 0;
            while (left < pending.size()) {
                int right = left + 1;
                while (right < pending.size()) {
                    Slice union = unite(pending.get(left), pending.get(right));
                    if (union != null) {
                        pending.set(left, union);
                        pending.remove(right);
                        changed = true;
                        break;
                    }
                    right++;
                }
                if (changed) {
                    break;
                }
                left++;
            }
        }
        return List.copyOf(pending);
    }

    private static boolean absorb(List<Slice> pending, Slice slice) {
        int slot = 0;
        while (slot < pending.size()) {
            Slice union = unite(pending.get(slot), slice);
            if (union != null) {
                pending.set(slot, union);
                return true;
            }
            slot++;
        }
        return false;
    }

    private static Slice unite(Slice left, Slice right) {
        if (left == null || right == null) {
            return null;
        }
        if (Math.abs(left.lowY() - right.lowY()) > AREA_EPSILON || Math.abs(left.highY() - right.highY()) > AREA_EPSILON) {
            return null;
        }
        boolean sameX = Math.abs(left.lowX() - right.lowX()) <= AREA_EPSILON && Math.abs(left.highX() - right.highX()) <= AREA_EPSILON;
        boolean sameZ = Math.abs(left.lowZ() - right.lowZ()) <= AREA_EPSILON && Math.abs(left.highZ() - right.highZ()) <= AREA_EPSILON;
        if (sameX && sameZ) {
            return left;
        }
        boolean touchX = Math.abs(left.highX() - right.lowX()) <= AREA_EPSILON || Math.abs(right.highX() - left.lowX()) <= AREA_EPSILON;
        boolean touchZ = Math.abs(left.highZ() - right.lowZ()) <= AREA_EPSILON || Math.abs(right.highZ() - left.lowZ()) <= AREA_EPSILON;
        boolean overlapX = left.lowX() < right.highX() - AREA_EPSILON && right.lowX() < left.highX() - AREA_EPSILON;
        boolean overlapZ = left.lowZ() < right.highZ() - AREA_EPSILON && right.lowZ() < left.highZ() - AREA_EPSILON;
        if (sameZ && touchX && !overlapX) {
            double unionLowX = Math.min(left.lowX(), right.lowX());
            double unionHighX = Math.max(left.highX(), right.highX());
            return new Slice(unionLowX, left.lowY(), left.lowZ(), unionHighX, left.highY(), left.highZ());
        }
        if (sameX && touchZ && !overlapZ) {
            double unionLowZ = Math.min(left.lowZ(), right.lowZ());
            double unionHighZ = Math.max(left.highZ(), right.highZ());
            return new Slice(left.lowX(), left.lowY(), unionLowZ, left.highX(), left.highY(), unionHighZ);
        }
        return null;
    }

    private static boolean blocksClearance(Volume other, double topY) {
        return other.highY > topY + AREA_EPSILON && other.lowY < topY + CLEARANCE - AREA_EPSILON;
    }

    private static Volume clip(AABB box, double lowX, double highX, double lowZ, double highZ) {
        if (box == null || !GroundFireMath.finite(box.minX) || !GroundFireMath.finite(box.maxX)
                || !GroundFireMath.finite(box.minY) || !GroundFireMath.finite(box.maxY)
                || !GroundFireMath.finite(box.minZ) || !GroundFireMath.finite(box.maxZ)) {
            return null;
        }
        double clippedLowX = Math.max(box.minX, lowX);
        double clippedHighX = Math.min(box.maxX, highX);
        double clippedLowZ = Math.max(box.minZ, lowZ);
        double clippedHighZ = Math.min(box.maxZ, highZ);
        if (clippedHighX - clippedLowX <= AREA_EPSILON || clippedHighZ - clippedLowZ <= AREA_EPSILON) {
            return null;
        }
        return new Volume(clippedLowX, box.minY, clippedLowZ, clippedHighX, box.maxY, clippedHighZ);
    }

    private static List<Flat> subtract(Flat source, Flat cut) {
        double cutLowX = Math.max(source.lowX, cut.lowX);
        double cutHighX = Math.min(source.highX, cut.highX);
        double cutLowZ = Math.max(source.lowZ, cut.lowZ);
        double cutHighZ = Math.min(source.highZ, cut.highZ);
        if (cutHighX - cutLowX <= AREA_EPSILON || cutHighZ - cutLowZ <= AREA_EPSILON) {
            return List.of(source);
        }
        ArrayList<Flat> kept = new ArrayList<>(4);
        if (cutLowX - source.lowX > AREA_EPSILON) {
            kept.add(new Flat(source.lowX, source.lowZ, cutLowX, source.highZ));
        }
        if (source.highX - cutHighX > AREA_EPSILON) {
            kept.add(new Flat(cutHighX, source.lowZ, source.highX, source.highZ));
        }
        if (cutLowZ - source.lowZ > AREA_EPSILON) {
            kept.add(new Flat(cutLowX, source.lowZ, cutHighX, cutLowZ));
        }
        if (source.highZ - cutHighZ > AREA_EPSILON) {
            kept.add(new Flat(cutLowX, cutHighZ, cutHighX, source.highZ));
        }
        return kept.isEmpty() ? List.of() : kept;
    }

    public static final class Slice {
        private final double lowX;
        private final double lowY;
        private final double lowZ;
        private final double highX;
        private final double highY;
        private final double highZ;

        public Slice(double lowX, double lowY, double lowZ, double highX, double highY, double highZ) {
            this.lowX = lowX;
            this.lowY = lowY;
            this.lowZ = lowZ;
            this.highX = highX;
            this.highY = highY;
            this.highZ = highZ;
        }

        public double lowX() {
            return lowX;
        }

        public double lowY() {
            return lowY;
        }

        public double lowZ() {
            return lowZ;
        }

        public double highX() {
            return highX;
        }

        public double highY() {
            return highY;
        }

        public double highZ() {
            return highZ;
        }

        public double area() {
            return (highX - lowX) * (highZ - lowZ);
        }

        public AABB box() {
            return new AABB(lowX, lowY, lowZ, highX, highY, highZ);
        }

        public boolean meets(AABB body) {
            return body != null && box().intersects(body);
        }
    }

    private record Volume(double lowX, double lowY, double lowZ, double highX, double highY, double highZ) {
    }

    private record Flat(double lowX, double lowZ, double highX, double highZ) {
    }
}
