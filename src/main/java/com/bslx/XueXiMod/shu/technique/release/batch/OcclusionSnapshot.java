package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 父事件当时能看见的目标。子段只认这份名单，不把后来挖开的墙后目标补进来。
 */
public final class OcclusionSnapshot {
    private final Vec3 eventPoint;
    private final Vec3 direction;
    private final Set<UUID> visibleTargets;

    private OcclusionSnapshot(Vec3 eventPoint, Vec3 direction, Set<UUID> visibleTargets) {
        this.eventPoint = eventPoint;
        this.direction = direction;
        this.visibleTargets = visibleTargets;
    }

    /** 记下当时半径内没有被方块挡住的目标。 */
    public static OcclusionSnapshot capture(ServerLevel level, Vec3 eventPoint, Vec3 direction, double radius,
                                            ImpactPartition partition) {
        Vec3 frozenPoint = eventPoint == null ? Vec3.ZERO : eventPoint;
        Vec3 frozenDirection = direction == null || direction.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : direction;
        if (level == null || radius <= 0.0D) {
            return new OcclusionSnapshot(frozenPoint, frozenDirection, Set.of());
        }
        AABB volume = new AABB(frozenPoint, frozenPoint).inflate(radius);
        List<LivingEntity> candidates = LivingCandidateQueries.entityQuery(level, volume, LivingEntity::isAlive);
        Set<UUID> visible = new HashSet<>();
        for (LivingEntity candidate : candidates) {
            if (candidate == null || !candidate.isAlive()) {
                continue;
            }
            if (partition != null && partition.excludes(candidate.getUUID())) {
                continue;
            }
            Vec3 center = candidate.getBoundingBox().getCenter();
            if (blocked(level, frozenPoint, center)) {
                continue;
            }
            visible.add(candidate.getUUID());
        }
        return new OcclusionSnapshot(frozenPoint, frozenDirection, Set.copyOf(visible));
    }

    public Vec3 eventPoint() {
        return eventPoint;
    }

    public Vec3 direction() {
        return direction;
    }

    /** 子段只接受快照里的目标。 */
    public boolean allows(UUID targetId) {
        return targetId != null && visibleTargets.contains(targetId);
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
