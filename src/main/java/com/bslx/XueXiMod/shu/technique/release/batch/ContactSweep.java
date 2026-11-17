package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** 同一段位移上比较最早碰到的实体和地形。 */
public final class ContactSweep {
    public enum Kind {
        NONE,
        ENTITY,
        TERRAIN
    }

    public record Contact(Kind kind, LivingEntity entity, Vec3 point, BlockHitResult blockHit) {
        public static Contact none() {
            return new Contact(Kind.NONE, null, null, null);
        }
    }

    private ContactSweep() {
    }

    /** 同一段上取更近的实体或方块。都没有时为空接触。 */
    public static Contact earliest(ServerLevel level, Vec3 fromPoint, Vec3 toPoint, Predicate<LivingEntity> allowed,
                                   Set<UUID> rejectedTargets) {
        if (level == null || fromPoint == null || toPoint == null) {
            return Contact.none();
        }
        Vec3 travel = toPoint.subtract(fromPoint);
        if (travel.lengthSqr() < 1.0E-8) {
            return Contact.none();
        }
        BlockHitResult blockHit = level.clip(new ClipContext(fromPoint, toPoint, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, null));
        double blockDistance = blockHit.getType() == HitResult.Type.BLOCK
                ? fromPoint.distanceTo(blockHit.getLocation())
                : Double.POSITIVE_INFINITY;
        AABB broad = new AABB(fromPoint, toPoint).inflate(1.0D);
        List<LivingEntity> candidates = LivingCandidateQueries.entityQuery(level, broad, candidate -> true);
        LivingEntity earliestEntity = null;
        double entityDistance = Double.POSITIVE_INFINITY;
        Vec3 entityPoint = null;
        for (LivingEntity candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (rejectedTargets != null && rejectedTargets.contains(candidate.getUUID())) {
                continue;
            }
            if (allowed != null && !allowed.test(candidate)) {
                continue;
            }
            Optional<Vec3> clipped = candidate.getBoundingBox().inflate(0.05D).clip(fromPoint, toPoint);
            if (clipped.isEmpty()) {
                continue;
            }
            double distance = fromPoint.distanceTo(clipped.get());
            if (distance < entityDistance) {
                entityDistance = distance;
                earliestEntity = candidate;
                entityPoint = clipped.get();
            }
        }
        if (earliestEntity == null && blockDistance == Double.POSITIVE_INFINITY) {
            return Contact.none();
        }
        if (earliestEntity != null && entityDistance <= blockDistance) {
            return new Contact(Kind.ENTITY, earliestEntity, entityPoint, null);
        }
        return new Contact(Kind.TERRAIN, null, blockHit.getLocation(), blockHit);
    }
}
