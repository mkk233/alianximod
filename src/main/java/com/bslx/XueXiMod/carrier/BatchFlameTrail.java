package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.enchant.resource.TrailContract;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.List;

/**
 * 移动宿主上的留焰。碰撞是连续线段，表现点每 0.25 格一颗，上限 256，上限不扩大伤害。
 * 不产种、不引爆、不崩地、不放原版火，也不另开施法标识。
 */
public final class BatchFlameTrail {
    private static final double COLLISION_STRIDE = 0.05D;

    private BatchFlameTrail() {
    }

    public static void attach(BatchFlightRecord record) {
        if (record == null || record.body == null || record.hitClock == null || record.resourceLoad == null) {
            return;
        }
        if (!record.resourceLoad.trail() || !TrailContract.MOVING_HOSTS.contains(record.techniqueKey)) {
            return;
        }
        if (!(record.body.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!BatchCarrierTypes.VISUAL.isPresent()) {
            return;
        }
        boolean retainRoot = false;
        if (record.rootCastId != null && RootCastBindings.lookup(record.rootCastId) != null) {
            RootCastBindings.retain(record.rootCastId);
            retainRoot = true;
        }
        BatchCarrierVisual tracker = new BatchCarrierVisual(BatchCarrierTypes.VISUAL.get(), serverLevel);
        tracker.armTracker(record.rootCastId, record.ownerIdentifier, record.hitClock, record.damage, record.tide,
                retainRoot);
        if (BatchCarrierIds.SPARK.equals(record.techniqueKey)) {
            tracker.markSparkMeshAnchor();
            tracker.assignSparkLaunch(record.body.launchDirection());
        }
        tracker.assignHost(record.body.getId());
        tracker.setPos(record.body.getX(), record.body.getY(), record.body.getZ());
        tracker.moveLastPoint(record.body.position());
        if (!serverLevel.addFreshEntity(tracker)) {
            if (retainRoot) {
                RootCastBindings.release(record.rootCastId);
            }
        }
    }

    /**
     * 旧火球效果若要接留焰，在它自己的 publishedHook 里调用。本作者不改那个效果。
     * 调用时出生布局和命中钟必须还在，否则无法共用命中钟。
     */
    public static void attachLoose(TechniqueHitEntity host, BatchFlightRecord record) {
        if (host == null || record == null) {
            return;
        }
        record.body = host;
        attach(record);
    }

    static void tick(BatchCarrierVisual tracker, ServerLevel level) {
        if (tracker == null || level == null || !tracker.trackerArmed()) {
            return;
        }
        TechniqueHitEntity host = findHost(level, tracker.hostId());
        if (host == null || host.isRemoved()) {
            if (host != null && tracker.hasLastPoint()) {
                sweep(tracker, level, tracker.lastPoint(), host.position());
                tracker.clearLastPoint();
            }
            tracker.noteHostMissing();
            if (tracker.orphanTicks() >= TrailContract.LIFE_TICKS) {
                tracker.finishTracker();
            }
            return;
        }
        Vec3 current = host.position();
        if (!tracker.hasLastPoint()) {
            tracker.moveLastPoint(current);
            return;
        }
        Vec3 previous = tracker.lastPoint();
        sweep(tracker, level, previous, current);
        placeSamples(tracker, level, previous, current);
        tracker.moveLastPoint(current);
    }

    private static TechniqueHitEntity findHost(ServerLevel level, int hostId) {
        if (hostId < 0) {
            return null;
        }
        if (level.getEntity(hostId) instanceof TechniqueHitEntity host) {
            return host;
        }
        return null;
    }

    private static void sweep(BatchCarrierVisual tracker, ServerLevel level, Vec3 segmentStart, Vec3 segmentEnd) {
        if (segmentStart == null || segmentEnd == null || segmentStart.distanceToSqr(segmentEnd) < 1.0E-8D) {
            return;
        }
        ServerPlayer owner = onlineOwner(level, tracker.ownerIdentifier());
        if (owner == null || tracker.sharedClock() == null) {
            return;
        }
        AABB swept = new AABB(segmentStart, segmentEnd).inflate(TrailContract.DIAMETER);
        List<LivingEntity> candidates = com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries.entityQuery(
                level, swept, LivingEntity::isAlive);
        for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            LivingEntity candidate = candidates.get(candidateIndex);
            if (candidate == null || candidate.isSpectator() || owner.getUUID().equals(candidate.getUUID())) {
                continue;
            }
            if (!capsuleHits(segmentStart, segmentEnd, candidate)) {
                continue;
            }
            if (tracker.sharedClock().occupied(candidate.getUUID(), level.getGameTime(), 0)) {
                continue;
            }
            HitVerdict verdict = TechniqueHits.classifyRespecting(
                    candidate, owner, tracker.trailDamage(), tracker.trailTide());
            if (verdict == null || !verdict.continues()) {
                continue;
            }
            tracker.sharedClock().tryMark(candidate.getUUID(), level.getGameTime(), 0);
        }
    }

    private static boolean capsuleHits(Vec3 segmentStart, Vec3 segmentEnd, LivingEntity candidate) {
        double span = segmentStart.distanceTo(segmentEnd);
        int stepCount = (int) Math.ceil(span / COLLISION_STRIDE);
        if (stepCount < 1) {
            stepCount = 1;
        }
        if (stepCount > 400) {
            stepCount = 400;
        }
        AABB box = candidate.getBoundingBox();
        double radius = TrailContract.DIAMETER * 0.5D;
        for (int stepIndex = 0; stepIndex <= stepCount; stepIndex++) {
            double travel = stepIndex / (double) stepCount;
            Vec3 sample = new Vec3(
                    segmentStart.x + (segmentEnd.x - segmentStart.x) * travel,
                    segmentStart.y + (segmentEnd.y - segmentStart.y) * travel,
                    segmentStart.z + (segmentEnd.z - segmentStart.z) * travel);
            if (distanceToBox(box, sample) <= radius) {
                return true;
            }
        }
        return false;
    }

    private static double distanceToBox(AABB box, Vec3 point) {
        double clampedX = point.x < box.minX ? box.minX : (point.x > box.maxX ? box.maxX : point.x);
        double clampedY = point.y < box.minY ? box.minY : (point.y > box.maxY ? box.maxY : point.y);
        double clampedZ = point.z < box.minZ ? box.minZ : (point.z > box.maxZ ? box.maxZ : point.z);
        double deltaX = point.x - clampedX;
        double deltaY = point.y - clampedY;
        double deltaZ = point.z - clampedZ;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    private static void placeSamples(BatchCarrierVisual tracker, ServerLevel level, Vec3 segmentStart, Vec3 segmentEnd) {
        double span = segmentStart.distanceTo(segmentEnd);
        double pending = tracker.pendingDistance() + span;
        if (span < 1.0E-8D) {
            tracker.setPendingDistance(pending);
            return;
        }
        double cursor = TrailContract.SAMPLE_SPACING - tracker.pendingDistance();
        if (cursor < 0.0D) {
            cursor = 0.0D;
        }
        Vec3 tangent = segmentEnd.subtract(segmentStart).scale(1.0D / span);
        while (cursor <= span && tracker.activeSamples(level.getGameTime()) < TrailContract.ACTIVE_POINT_CAP) {
            double travel = cursor / span;
            Vec3 sample = new Vec3(
                    segmentStart.x + (segmentEnd.x - segmentStart.x) * travel,
                    segmentStart.y + (segmentEnd.y - segmentStart.y) * travel,
                    segmentStart.z + (segmentEnd.z - segmentStart.z) * travel);
            BatchCarrierVisual.spawnTrailSample(level, sample, tangent, tracker.nextSampleOrder(),
                    TrailContract.LIFE_TICKS, (float) (TrailContract.DIAMETER * 0.5D), tracker.sparkMeshAnchor(),
                    tracker.sparkLaunchX(), tracker.sparkLaunchY(), tracker.sparkLaunchZ());
            tracker.noteSample(level.getGameTime());
            cursor += TrailContract.SAMPLE_SPACING;
        }
        double consumed = cursor - TrailContract.SAMPLE_SPACING;
        if (consumed < 0.0D) {
            consumed = 0.0D;
        }
        if (consumed > span) {
            consumed = span;
        }
        double remainder = span - consumed;
        if (remainder < 0.0D) {
            remainder = 0.0D;
        }
        if (remainder >= TrailContract.SAMPLE_SPACING) {
            double whole = Math.floor(remainder / TrailContract.SAMPLE_SPACING);
            remainder = remainder - whole * TrailContract.SAMPLE_SPACING;
        }
        tracker.setPendingDistance(remainder);
    }

    private static ServerPlayer onlineOwner(ServerLevel level, java.util.UUID ownerIdentifier) {
        if (level == null || ownerIdentifier == null || level.getServer() == null) {
            return null;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerIdentifier);
        if (owner == null || owner.isRemoved() || !owner.isAlive() || owner instanceof FakePlayer) {
            return null;
        }
        if (owner.serverLevel() != level) {
            return null;
        }
        return owner;
    }
}
