package com.bslx.XueXiMod.shu.technique.release.batch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * 视线 32 格内第一个向上的承托面。净空不够则整组拒绝，调用方不得扣费或进入冷却。
 */
public final class SurfaceAnchor {
    public static final double REACH = 32.0D;
    public static final double CLEARANCE_PAD = 0.125D;

    private SurfaceAnchor() {
    }

    public record Sample(Vec3 point, Vec3 direction, BlockPos support) {
    }

    /** 视线 32 格内第一个朝上的承托面。净空不够则整组不能用。 */
    public static Sample resolve(ServerPlayer player, double halfWidth, double halfHeight) {
        if (player == null) {
            return null;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-8) {
            return null;
        }
        Vec3 end = eye.add(look.normalize().scale(REACH));
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.BLOCK || blockHit.getDirection() != Direction.UP) {
            return null;
        }
        BlockPos support = blockHit.getBlockPos();
        BlockState supportState = level.getBlockState(support);
        if (supportState.getCollisionShape(level, support).isEmpty()) {
            return null;
        }
        Vec3 point = blockHit.getLocation();
        double width = Math.max(0.0D, halfWidth);
        double height = Math.max(0.0D, halfHeight) + CLEARANCE_PAD;
        if (!clear(level, player, point, width, height)) {
            return null;
        }
        return new Sample(point, look.normalize(), support);
    }

    private static boolean clear(ServerLevel level, ServerPlayer player, Vec3 point, double halfWidth, double height) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int horizontal = (int) Math.ceil(halfWidth);
        int vertical = (int) Math.ceil(height);
        for (int eastStep = -horizontal; eastStep <= horizontal; eastStep++) {
            for (int southStep = -horizontal; southStep <= horizontal; southStep++) {
                for (int aboveStep = 0; aboveStep < vertical; aboveStep++) {
                    cursor.set(point.x + eastStep, point.y + aboveStep, point.z + southStep);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.getCollisionShape(level, cursor, CollisionContext.of(player)).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
