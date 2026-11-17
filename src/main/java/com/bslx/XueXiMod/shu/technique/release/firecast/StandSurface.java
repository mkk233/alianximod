package com.bslx.XueXiMod.shu.technique.release.firecast;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;

/** 按碰撞箱取可站立顶面。顶面上方还被别的碰撞箱挡住就不算可见地表。不改方块。 */
public final class StandSurface {
    private StandSurface() {
    }

    public static double columnTop(BlockGetter level, double sampleX, double sampleZ, double originY, double slack) {
        return openTop(level, sampleX, sampleZ, originY, slack);
    }

    private static double openTop(BlockGetter level, double sampleX, double sampleZ, double originY, double slack) {
        if (level == null || !GroundFireMath.finite(sampleX) || !GroundFireMath.finite(sampleZ)
                || !GroundFireMath.finite(originY) || !GroundFireMath.finite(slack) || slack < 0.0d) {
            return Double.NaN;
        }
        int columnX = Mth.floor(sampleX);
        int columnZ = Mth.floor(sampleZ);
        int upper = Mth.floor(originY + slack);
        int lower = Mth.floor(originY - slack) - 1;
        ArrayList<GroundFireMath.Span> spans = new ArrayList<>();
        int cursorY = upper;
        while (cursorY >= lower) {
            BlockPos place = new BlockPos(columnX, cursorY, columnZ);
            VoxelShape shape = level.getBlockState(place).getCollisionShape(level, place, CollisionContext.empty());
            if (!shape.isEmpty()) {
                for (AABB bounds : shape.toAabbs()) {
                    spans.add(new GroundFireMath.Span(
                            place.getX() + bounds.minX, place.getY() + bounds.minY, place.getZ() + bounds.minZ,
                            place.getX() + bounds.maxX, place.getY() + bounds.maxY, place.getZ() + bounds.maxZ));
                }
            }
            cursorY -= 1;
        }
        return GroundFireMath.openTop(sampleX, sampleZ, originY, slack, spans);
    }
}
