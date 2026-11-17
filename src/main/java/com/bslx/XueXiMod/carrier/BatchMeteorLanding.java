package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.terrain.MeteoriteWreckageEntity;
import com.bslx.XueXiMod.terrain.TerrainMutationService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 陨岩结束后的唯一落点。没碰到实体的超时只生成无伤残骸，不记成撞地。
 * 真正撞块时先崩地再安放；安放失败才把残骸留下重试。
 */
public final class BatchMeteorLanding {
    private BatchMeteorLanding() {
    }

    public static boolean terrainContact(BatchFlightRecord record, ServerLevel level, Vec3 point) {
        if (record == null || record.body == null || level == null || point == null) {
            return false;
        }
        Vec3 fromPoint = record.body.previousPosition();
        if (fromPoint == null || fromPoint.distanceToSqr(point) < 1.0E-8D) {
            return false;
        }
        BlockHitResult blockHit = level.clip(new ClipContext(fromPoint, point, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, record.body));
        return blockHit.getType() == HitResult.Type.BLOCK;
    }

    public static void finish(BatchFlightRecord record, ServerLevel level, Vec3 point, boolean terrainImpact) {
        if (record == null || record.landed || level == null || point == null) {
            return;
        }
        if (!BatchCarrierIds.METEORITE.equals(record.techniqueKey)) {
            record.landed = true;
            return;
        }
        record.landed = true;
        if (record.ownerIdentifier == null) {
            return;
        }
        if (!terrainImpact) {
            MeteoriteWreckageEntity.spawnAfterSettlement(level, record.ownerIdentifier, point, Vec3.ZERO,
                    record.breakGround);
            return;
        }
        BlockPos impactCell = BlockPos.containing(point);
        boolean marked = false;
        if (record.breakGround) {
            boolean[] markHolder = new boolean[1];
            TerrainMutationService.BreakGroundResult breakResult = TerrainMutationService.breakExistingOnce(
                    level, record.ownerIdentifier, impactCell, true, false, () -> markHolder[0] = true);
            marked = markHolder[0];
            if (breakResult == TerrainMutationService.BreakGroundResult.EXECUTED) {
                BatchTriggerVisuals.dust(level, Vec3.atCenterOf(impactCell));
            }
        }
        TerrainMutationService.PlaceOutcome placeOutcome = TerrainMutationService.placeMeteorite(
                level, record.ownerIdentifier, impactCell);
        if (placeOutcome != null && placeOutcome.placeResult() == TerrainMutationService.PlaceResult.PLACED) {
            return;
        }
        boolean allowLaterBreak = record.breakGround && !marked;
        MeteoriteWreckageEntity.spawnAfterSettlement(level, record.ownerIdentifier, point, Vec3.ZERO, allowLaterBreak);
    }
}
