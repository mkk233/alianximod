package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.batch.ImpactPartition;
import com.bslx.XueXiMod.shu.technique.release.batch.OcclusionSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 环和陨岩中心共用的圆柱查询。子段只认父遮挡快照，父段按撞击点实时挡墙。 */
public final class BatchVolumeQuery {
    private BatchVolumeQuery() {
    }

    public static List<LivingEntity> collect(ServerLevel level, Vec3 point, double innerRadius, double outerRadius,
                                             double totalHeight, ImpactPartition partition, OcclusionSnapshot occlusion,
                                             UUID ownerIdentifier) {
        List<LivingEntity> accepted = new ArrayList<>();
        if (level == null || point == null || outerRadius <= 0.0D || totalHeight <= 0.0D) {
            return accepted;
        }
        double halfHeight = totalHeight * 0.5D;
        double bandLow = point.y - halfHeight;
        double bandHigh = point.y + halfHeight;
        AABB volume = new AABB(point.x - outerRadius, bandLow, point.z - outerRadius,
                point.x + outerRadius, bandHigh, point.z + outerRadius);
        List<LivingEntity> candidates = LivingCandidateQueries.entityQuery(level, volume, LivingEntity::isAlive);
        for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            LivingEntity candidate = candidates.get(candidateIndex);
            if (!included(level, point, candidate, innerRadius, outerRadius, bandLow, bandHigh, partition, occlusion,
                    ownerIdentifier)) {
                continue;
            }
            accepted.add(candidate);
        }
        return accepted;
    }

    private static boolean included(ServerLevel level, Vec3 point, LivingEntity candidate, double innerRadius,
                                    double outerRadius, double bandLow, double bandHigh, ImpactPartition partition,
                                    OcclusionSnapshot occlusion, UUID ownerIdentifier) {
        if (candidate == null || !candidate.isAlive() || candidate.isRemoved() || candidate.isSpectator()) {
            return false;
        }
        if (ownerIdentifier != null && ownerIdentifier.equals(candidate.getUUID())) {
            return false;
        }
        if (partition != null && partition.excludes(candidate.getUUID())) {
            return false;
        }
        AABB box = candidate.getBoundingBox();
        if (box.maxY < bandLow || box.minY > bandHigh) {
            return false;
        }
        double offsetX = candidate.getX() - point.x;
        double offsetZ = candidate.getZ() - point.z;
        double horizontal = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        if (horizontal < innerRadius || horizontal > outerRadius) {
            return false;
        }
        if (occlusion != null) {
            return occlusion.allows(candidate.getUUID());
        }
        return !blocked(level, point, box.getCenter());
    }

    private static boolean blocked(ServerLevel level, Vec3 fromPoint, Vec3 toPoint) {
        BlockHitResult blockHit = level.clip(new ClipContext(fromPoint, toPoint, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, null));
        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        double blockDistance = fromPoint.distanceTo(blockHit.getLocation());
        double targetDistance = fromPoint.distanceTo(toPoint);
        return blockDistance + 0.05D < targetDistance;
    }
}
