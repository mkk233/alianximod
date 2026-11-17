package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.DropTilt;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 陷阱下落。每拍最多下降 0.35 格，碰到碰撞体或落点承托面后停住。
 * 已经停住就不再移动。不改无敌帧或账本。
 */
public final class TrapFallMotion implements TechniqueMotion {
    private static final double VERTICAL_STEP = 0.35D;

    @Override
    /** 没有落点锚点时竖直掉。有锚点时朝锚点斜落，最后一步缩短到锚点高度。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        if (entity.planted()) {
            return;
        }
        Vec3 landing = entity.dropLanding();
        if (landing == null) {
            fallStraight(entity, level);
            return;
        }
        Vec3 heading = entity.forward();
        if (heading == null || heading.y >= -1.0E-6D) {
            heading = DropTilt.unitToward(entity.origin(), landing);
        }
        Vec3 position = entity.position();
        double remaining = position.y - landing.y;
        double usedStep = VERTICAL_STEP;
        if (remaining > 1.0E-4D && remaining < usedStep) {
            usedStep = remaining;
        }
        Vec3 next = position.add(DropTilt.fallStep(heading, usedStep));
        BlockHitResult blockHit = level.clip(new ClipContext(
                position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            entity.placeAt(blockHit.getLocation(), heading);
            entity.setPlanted(true);
            return;
        }
        if (plantedOnSupport(level, next)) {
            entity.placeAt(next, heading);
            entity.setPlanted(true);
            return;
        }
        entity.placeAt(next, heading);
    }

    private static void fallStraight(TechniqueHitEntity entity, ServerLevel level) {
        Vec3 position = entity.position();
        Vec3 next = position.add(0.0, -VERTICAL_STEP, 0.0);
        BlockPos below = BlockPos.containing(next.x, next.y - 0.05, next.z);
        if (next.y <= level.getMinBuildHeight() || !level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
            double top = below.getY() + 1.0;
            entity.placeAt(new Vec3(position.x, Math.max(top, next.y), position.z), new Vec3(0.0, 1.0, 0.0));
            entity.setPlanted(true);
            return;
        }
        entity.placeAt(next, new Vec3(0.0, -1.0, 0.0));
    }

    private static boolean plantedOnSupport(ServerLevel level, Vec3 next) {
        if (next.y <= level.getMinBuildHeight()) {
            return true;
        }
        BlockPos below = BlockPos.containing(next.x, next.y - 0.05, next.z);
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }
}
