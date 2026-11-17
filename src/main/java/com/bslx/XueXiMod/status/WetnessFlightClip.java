package com.bslx.XueXiMod.status;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** 潮湿追加下降的纯裁短。这里调用原版 {@link Shapes#collide}。 */
public final class WetnessFlightClip {
    private WetnessFlightClip() {}

    /** 把向下的追加量裁到碰不上这些形状为止。向上或零追加不裁；起始盒已经相交时整段取消。 */
    public static double clipExtra(AABB startBox, double extraDown, Iterable<VoxelShape> shapes) {
        if (startBox == null || extraDown >= 0.0D) {
            return 0.0D;
        }
        List<VoxelShape> gathered = new ArrayList<>();
        if (shapes != null) {
            for (VoxelShape shape : shapes) {
                if (shape != null && !shape.isEmpty()) {
                    gathered.add(shape);
                }
            }
        }
        if (gathered.isEmpty()) {
            return extraDown;
        }
        if (strictOverlap(startBox, gathered)) {
            return 0.0D;
        }
        double allowed = Shapes.collide(Direction.Axis.Y, startBox, gathered, extraDown);
        if (allowed < extraDown) {
            allowed = extraDown;
        }
        if (allowed > 0.0D) {
            allowed = 0.0D;
        }
        return allowed;
    }

    /** 真实体素相交。外包围盒不算。只贴着表面时这里为假，交给 {@link Shapes#collide}。 */
    private static boolean strictOverlap(AABB startBox, List<VoxelShape> gathered) {
        VoxelShape occupied = Shapes.create(startBox);
        for (VoxelShape shape : gathered) {
            if (Shapes.joinIsNotEmpty(occupied, shape, BooleanOp.AND)) {
                return true;
            }
        }
        return false;
    }

    /** 多个部件里允许下降更少的那个。数值都小于等于 0。 */
    public static double strictestExtra(double requestedDown, double... allowedDown) {
        double strictest = requestedDown;
        if (allowedDown == null) {
            return strictest;
        }
        for (double candidate : allowedDown) {
            if (candidate > strictest) {
                strictest = candidate;
            }
        }
        return strictest;
    }

    /** 请求下降减去实际允许下降。两者都小于等于 0。 */
    public static double rejectedDown(double requestedDown, double allowedDown) {
        return requestedDown - allowedDown;
    }

    /** 龙下降要躲的方块碰撞，再加龙不能穿过的实心方块整格。 */
    public static List<VoxelShape> dragonObstacles(Level level, Entity mover, AABB swept) {
        List<VoxelShape> shapes = new ArrayList<>();
        if (level == null || swept == null) {
            return shapes;
        }
        for (VoxelShape shape : level.getBlockCollisions(mover, swept)) {
            shapes.add(shape);
        }
        int west = Mth.floor(swept.minX);
        int east = Mth.floor(swept.maxX);
        int low = Mth.floor(swept.minY);
        int high = Mth.floor(swept.maxY);
        int north = Mth.floor(swept.minZ);
        int south = Mth.floor(swept.maxZ);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int cursorX = west; cursorX <= east; cursorX++) {
            for (int cursorY = low; cursorY <= high; cursorY++) {
                for (int cursorZ = north; cursorZ <= south; cursorZ++) {
                    cursor.set(cursorX, cursorY, cursorZ);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || state.is(BlockTags.DRAGON_TRANSPARENT)) {
                        continue;
                    }
                    shapes.add(Shapes.block().move(cursorX, cursorY, cursorZ));
                }
            }
        }
        return shapes;
    }
}
