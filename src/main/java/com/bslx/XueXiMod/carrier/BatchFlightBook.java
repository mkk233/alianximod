package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceHitCommit;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.batch.ImpactPartition;
import com.bslx.XueXiMod.shu.technique.release.batch.ResolvedHit;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 火苗和陨岩出生后的记录。资源提交仍由 A 的结算做一次；这里只给中心范围里还没结算的目标补一次。
 */
public final class BatchFlightBook {
    private static final ThreadLocal<BatchFlightRecord> PENDING = new ThreadLocal<>();
    private static final Map<UUID, BatchFlightRecord> BY_BODY = new ConcurrentHashMap<>();
    private static final Map<UUID, List<BatchFlightRecord>> BY_CLOCK = new ConcurrentHashMap<>();

    private BatchFlightBook() {
    }

    public static TechniqueHitSpec.HitListener listener(String techniqueKey, Consumer<Vec3> trigger, AtomicBoolean gate) {
        BatchFlightRecord record = new BatchFlightRecord();
        record.techniqueKey = techniqueKey == null ? "" : techniqueKey;
        record.trigger = trigger;
        if (gate != null) {
            record.triggerGate = gate;
        }
        PENDING.set(record);
        return new TechniqueHitSpec.HitListener() {
            @Override
            public void onHit(LivingEntity target) {
            }

            @Override
            public void onExpire(Vec3 point) {
                expire(record, point);
            }
        };
    }

    public static void publish(TechniqueHitEntity body) {
        BatchFlightRecord record = PENDING.get();
        PENDING.remove();
        if (record == null || body == null) {
            return;
        }
        bind(record, body);
        BY_BODY.put(body.getUUID(), record);
        if (record.hitClock != null && record.hitClock.clockId() != null) {
            BY_CLOCK.computeIfAbsent(record.hitClock.clockId(), clockIdentifier -> new ArrayList<>()).add(record);
        }
        if (BatchCarrierIds.METEORITE.equals(record.techniqueKey)) {
            body.bindRemoval(BatchFlightBook::onBodyRemoved);
        }
        if (BatchCarrierIds.SPARK.equals(record.techniqueKey)) {
            body.bindRemoval(BatchFlightBook::onSparkRemoved);
        }
        if (body.level() instanceof ServerLevel serverLevel) {
            spawnBodyVisual(serverLevel, body, record.techniqueKey);
        }
        BatchFlameTrail.attach(record);
    }

    public static void onResolved(ResolvedHit hit) {
        if (hit == null || hit.verdict() == null || hit.handoff() == null || hit.owner() == null) {
            return;
        }
        if (hit.verdict() == HitVerdict.INVALID_OR_CANCELLED || hit.verdict() == HitVerdict.PENDING) {
            return;
        }
        confirmOpenStrike(hit);
        if (hit.verdict().grantsSeeds()) {
            Vec3 eventPoint = hit.handoff().eventPoint() != null ? hit.handoff().eventPoint() : hit.owner().position();
            UUID targetId = hit.target() == null ? null : hit.target().getUUID();
            BatchTriggerVisuals.onAccepted(hit.owner().serverLevel(), eventPoint, targetId, hit.resourceLoad(),
                    hit.bodyScaledBase(), hit.legacyFixedPoints());
        }
        if (hit.target() != null && hit.handoff().hitClock() != null) {
            List<BatchFlightRecord> matched = BY_CLOCK.get(hit.handoff().hitClock().clockId());
            if (matched != null) {
                List<BatchFlightRecord> snapshot = new ArrayList<>(matched);
                for (int recordIndex = 0; recordIndex < snapshot.size(); recordIndex++) {
                    noteResolved(snapshot.get(recordIndex), hit);
                }
            }
        }
    }

    public static MeteorContactResult intercept(TechniqueHitEntity body, ServerLevel level, Vec3 point,
                                                LivingEntity struck, MeteorContactKind kind) {
        BatchFlightRecord record = body == null ? null : BY_BODY.get(body.getUUID());
        if (record == null || level == null || point == null || kind == null
                || !BatchCarrierIds.METEORITE.equals(record.techniqueKey)) {
            return MeteorContactResult.notHandled();
        }
        if (kind == MeteorContactKind.TIMEOUT) {
            if (record.flyingAfterEntity) {
                record.followOwnedByCaller = true;
                record.followSent = true;
                return MeteorContactResult.handled(false, HitVerdict.ESTABLISHED_NO_DAMAGE);
            }
            declinePrevious(record, null);
            record.followOwnedByCaller = true;
            record.followSent = true;
            BatchMeteorLanding.finish(record, level, point, false);
            return MeteorContactResult.handled(false, HitVerdict.ESTABLISHED_NO_DAMAGE);
        }
        if (kind == MeteorContactKind.ENTITY) {
            if (struck == null) {
                return MeteorContactResult.notHandled();
            }
            if (record.landed || record.flyingAfterEntity || record.expanded) {
                record.followOwnedByCaller = true;
                return MeteorContactResult.handled(false, HitVerdict.INVALID_OR_CANCELLED);
            }
            UUID struckId = struck.getUUID();
            declinePrevious(record, struckId);
            record.openStrike = struckId;
            return MeteorContactResult.notHandled();
        }
        if (record.flyingAfterEntity) {
            record.followOwnedByCaller = true;
            record.followSent = true;
            BatchMeteorLanding.finish(record, level, point, true);
            return MeteorContactResult.handled(false, HitVerdict.ESTABLISHED_NO_DAMAGE);
        }
        declinePrevious(record, null);
        record.followOwnedByCaller = true;
        HitVerdict terrainVerdict = record.expanded
                ? HitVerdict.ESTABLISHED_NO_DAMAGE
                : expand(record, level, point, null, false);
        record.expanded = true;
        record.followSent = true;
        BatchMeteorLanding.finish(record, level, point, true);
        return MeteorContactResult.handled(true, terrainVerdict);
    }

    /** 上一个还没成立的候选换成下一次接触时，把它排除出中心圈。同一次候选不排除。 */
    private static void declinePrevious(BatchFlightRecord record, UUID nextIdentifier) {
        if (record.openStrike == null || record.contacted) {
            record.openStrike = null;
            return;
        }
        if (nextIdentifier != null && record.openStrike.equals(nextIdentifier)) {
            return;
        }
        record.declinedStrikes.add(record.openStrike);
        record.openStrike = null;
    }

    private static void confirmOpenStrike(ResolvedHit hit) {
        if (hit.target() == null || !hit.verdict().continues()) {
            return;
        }
        UUID targetId = hit.target().getUUID();
        for (BatchFlightRecord record : BY_BODY.values()) {
            if (record.openStrike != null && record.openStrike.equals(targetId)
                    && BatchCarrierIds.METEORITE.equals(record.techniqueKey)) {
                record.strikeConfirmed = true;
            }
        }
    }

    private static void noteResolved(BatchFlightRecord record, ResolvedHit hit) {
        if (record == null || hit == null || hit.target() == null) {
            return;
        }
        if (BatchCarrierIds.SPARK.equals(record.techniqueKey)
                && hit.target().getRemainingFireTicks() < BatchCarrierIds.SPARK_BURN_TICKS) {
            hit.target().setRemainingFireTicks(BatchCarrierIds.SPARK_BURN_TICKS);
        }
        if (!BatchCarrierIds.METEORITE.equals(record.techniqueKey)) {
            return;
        }
        if (record.openStrike == null || !record.openStrike.equals(hit.target().getUUID())) {
            return;
        }
        if (!(hit.owner().level() instanceof ServerLevel serverLevel)) {
            return;
        }
        record.contacted = true;
        record.followSent = true;
        if (record.expanded) {
            return;
        }
        record.expanded = true;
        record.openStrike = null;
        Vec3 point = hit.handoff().eventPoint() != null ? hit.handoff().eventPoint() : hit.target().position();
        expand(record, serverLevel, point, hit.target().getUUID(), hit.verdict().grantsSeeds());
    }

    private static void expire(BatchFlightRecord record, Vec3 point) {
        if (record == null) {
            return;
        }
        record.fireTrigger(point);
        ServerLevel level = record.body != null && record.body.level() instanceof ServerLevel serverLevel
                ? serverLevel : null;
        if (record.flyingAfterEntity && !record.landed) {
            forget(record);
            return;
        }
        if (level != null && BatchCarrierIds.METEORITE.equals(record.techniqueKey) && !record.landed) {
            if (!record.expanded && !record.resourceCommit && record.openStrike != null) {
                record.contacted = true;
                record.followSent = true;
                record.expanded = true;
                UUID settledStrike = record.openStrike;
                record.openStrike = null;
                expand(record, level, point, settledStrike, false);
            }
            if (record.openStrike != null && !record.contacted) {
                if (record.strikeConfirmed) {
                    record.contacted = true;
                    record.followSent = true;
                    if (!record.expanded) {
                        record.expanded = true;
                        UUID settledStrike = record.openStrike;
                        record.openStrike = null;
                        expand(record, level, point, settledStrike, true);
                    } else {
                        record.openStrike = null;
                    }
                }
                BatchMeteorLanding.finish(record, level, point, false);
            } else {
                boolean terrain = !record.contacted && BatchMeteorLanding.terrainContact(record, level, point);
                if (terrain && !record.expanded) {
                    HitVerdict verdict = expand(record, level, point, null, false);
                    record.expanded = true;
                    if (!record.followSent && !record.followOwnedByCaller && record.follow != null
                            && verdict.continues()) {
                        record.follow.onVerdict(verdict, point, record.facing == null ? Vec3.ZERO : record.facing, null);
                        record.followSent = true;
                    }
                    BatchMeteorLanding.finish(record, level, point, true);
                } else {
                    BatchMeteorLanding.finish(record, level, point, false);
                }
            }
        }
        forget(record);
    }

    private static HitVerdict expand(BatchFlightRecord record, ServerLevel level, Vec3 point, UUID skipIdentifier,
                                     boolean legacyAlreadySpent) {
        if (record == null || level == null || point == null) {
            return HitVerdict.ESTABLISHED_NO_DAMAGE;
        }
        ServerPlayer owner = BatchOwners.online(level, record.ownerIdentifier);
        if (owner == null) {
            return HitVerdict.ESTABLISHED_NO_DAMAGE;
        }
        List<LivingEntity> targets = BatchVolumeQuery.collect(level, point, 0.0D, BatchCarrierIds.METEOR_CENTER_RADIUS,
                BatchCarrierIds.METEOR_CENTER_HEIGHT, record.partition, record.occlusion, record.ownerIdentifier);
        HitVerdict best = HitVerdict.ESTABLISHED_NO_DAMAGE;
        boolean legacySpent = legacyAlreadySpent;
        for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
            LivingEntity target = targets.get(targetIndex);
            if (target == null || (skipIdentifier != null && skipIdentifier.equals(target.getUUID()))) {
                continue;
            }
            if (record.declinedStrikes.contains(target.getUUID())) {
                continue;
            }
            if (record.hitClock != null && record.hitClock.alreadySeen(target.getUUID())) {
                continue;
            }
            HitVerdict verdict = TechniqueHits.classifyRespecting(target, owner, record.damage, record.tide);
            if (verdict == null || verdict == HitVerdict.INVALID_OR_CANCELLED || verdict == HitVerdict.PENDING
                    || !verdict.continues()) {
                continue;
            }
            if (verdict == HitVerdict.DAMAGE_ACCEPTED) {
                best = HitVerdict.DAMAGE_ACCEPTED;
            }
            if (record.hitClock != null) {
                record.hitClock.tryMark(target.getUUID(), level.getGameTime(), 0);
            }
            if (verdict.grantsSeeds()) {
                int legacyPoints = 0;
                if (!legacySpent) {
                    legacyPoints = Math.max(0, record.legacyFixedPoints);
                    legacySpent = true;
                }
                commitExtra(record, owner, target, legacyPoints);
                BatchTriggerVisuals.onAccepted(level, target.position(), target.getUUID(), record.resourceLoad,
                        record.bodyScaledBase, legacyPoints);
            }
        }
        return best;
    }

    private static void commitExtra(BatchFlightRecord record, ServerPlayer owner, LivingEntity target, int legacyPoints) {
        if (!record.resourceCommit || record.rootCastId == null) {
            return;
        }
        FireCastContext cast = RootCastBindings.lookup(record.rootCastId);
        if (cast == null) {
            return;
        }
        ResourceEnchantLoad load = record.resourceLoad == null ? ResourceEnchantLoad.EMPTY : record.resourceLoad;
        ResourceHitCommit.commit(target, cast, owner, load, Math.max(0, record.bodyScaledBase),
                Math.max(0, legacyPoints), true);
    }

    private static void bind(BatchFlightRecord record, TechniqueHitEntity body) {
        record.body = body;
        if (body.getCaster() != null) {
            record.ownerIdentifier = body.getCaster().getUUID();
        }
        CastFrame frame = CastFrame.current();
        float fallback = BatchCarrierIds.METEORITE.equals(record.techniqueKey)
                ? BatchCarrierIds.METEOR_DAMAGE : BatchCarrierIds.SPARK_DAMAGE;
        record.damage = frame != null && frame.damage > 0.0F ? frame.damage : fallback;
        record.tide = frame == null || frame.tideSuccess == null ? TideMarkSuccess.NONE : frame.tideSuccess;
        record.legacyFixedPoints = frame == null ? 0 : Math.max(0, frame.seedPoints);
        record.facing = body.forward() == null ? new Vec3(0.0D, 0.0D, 1.0D) : body.forward();
        BirthLayout.Slot slot = BirthLayout.current();
        if (slot == null) {
            record.resourceLoad = ResourceEnchantLoad.EMPTY;
            record.partition = ImpactPartition.none();
            return;
        }
        record.hitClock = slot.hitClock;
        record.rootCastId = slot.rootCastId;
        record.resourceCommit = slot.resourceCommit;
        record.resourceLoad = slot.resourceLoad == null ? ResourceEnchantLoad.EMPTY : slot.resourceLoad;
        record.bodyScaledBase = slot.bodyScaledBase;
        record.breakGround = record.resourceLoad.breakGround();
        record.partition = slot.partition == null ? ImpactPartition.none() : slot.partition;
        record.occlusion = slot.occlusion;
        record.follow = slot.follow;
        if (slot.forcedDirection != null && slot.forcedDirection.lengthSqr() > 1.0E-8D) {
            record.facing = slot.forcedDirection;
        }
    }

    private static void spawnBodyVisual(ServerLevel level, TechniqueHitEntity body, String techniqueKey) {
        int duration = Math.max(1, body.lifetime());
        if (BatchCarrierIds.SPARK.equals(techniqueKey)) {
            BatchCarrierVisual.spawnSpark(level, body.position(), body.launchDirection(),
                    duration, 0.4F, body.getId(), body.getUUID());
            return;
        }
        if (BatchCarrierIds.METEORITE.equals(techniqueKey)) {
            BatchCarrierVisual.spawn(level, body.position(), BatchVisualShape.METEOR_BLOCK, BatchVisualTexture.NONE,
                    duration, 0.5F, body.getId(), true);
        }
    }

    /**
     * 扫掠已经放过一次合法实体后调用。补上原先随飞行体结束才做的那次中心，
     * 不生成残骸，也不再让后续实体或落地重做结算。
     */
    public static void holdAfterEntitySweep(TechniqueHitEntity body) {
        BatchFlightRecord record = body == null ? null : BY_BODY.get(body.getUUID());
        if (record == null || record.landed || record.flyingAfterEntity
                || !BatchCarrierIds.METEORITE.equals(record.techniqueKey)) {
            return;
        }
        boolean legal = record.contacted || record.expanded || record.strikeConfirmed
                || (record.openStrike != null && body.remembersStrike(record.openStrike));
        if (!legal) {
            return;
        }
        ServerLevel level = body.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        Vec3 point = body.position();
        if (!record.expanded && !record.resourceCommit && record.openStrike != null && level != null) {
            record.contacted = true;
            record.followSent = true;
            record.expanded = true;
            UUID settledStrike = record.openStrike;
            record.openStrike = null;
            expand(record, level, point, settledStrike, false);
        }
        if (record.openStrike != null && !record.contacted && record.strikeConfirmed && level != null) {
            record.contacted = true;
            record.followSent = true;
            if (!record.expanded) {
                record.expanded = true;
                UUID settledStrike = record.openStrike;
                record.openStrike = null;
                expand(record, level, point, settledStrike, true);
            } else {
                record.openStrike = null;
            }
        }
        record.flyingAfterEntity = true;
        record.contacted = true;
        record.followSent = true;
        record.fireTrigger(point);
        body.holdFlight();
        releaseArmedTrail(body);
    }

    public static void onBodyRemoved(TechniqueHitEntity body) {
        if (body == null) {
            return;
        }
        BatchFlightRecord record = BY_BODY.get(body.getUUID());
        if (record == null) {
            return;
        }
        forget(record);
    }

    /** 火苗宿主正常移除时只收自己的跟随视觉。陨岩继续走原移除回调。 */
    private static void onSparkRemoved(TechniqueHitEntity body) {
        if (body == null || !(body.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BatchCarrierVisual.discardForHost(serverLevel, body.getUUID());
        BatchFlightRecord record = BY_BODY.get(body.getUUID());
        if (record != null) {
            forget(record);
        }
    }

    private static void releaseArmedTrail(TechniqueHitEntity body) {
        if (body == null || !(body.level() instanceof ServerLevel level)) {
            return;
        }
        int hostId = body.getId();
        List<BatchCarrierVisual> armed = new ArrayList<>();
        for (Entity worldEntity : level.getAllEntities()) {
            if (worldEntity instanceof BatchCarrierVisual visual && visual.trackerArmed() && visual.hostId() == hostId) {
                armed.add(visual);
            }
        }
        for (int visualIndex = 0; visualIndex < armed.size(); visualIndex++) {
            armed.get(visualIndex).finishTracker();
        }
    }

    private static void forget(BatchFlightRecord record) {
        if (record.body != null) {
            BY_BODY.remove(record.body.getUUID());
        }
        if (record.hitClock == null || record.hitClock.clockId() == null) {
            return;
        }
        List<BatchFlightRecord> matched = BY_CLOCK.get(record.hitClock.clockId());
        if (matched == null) {
            return;
        }
        matched.remove(record);
        if (matched.isEmpty()) {
            BY_CLOCK.remove(record.hitClock.clockId());
        }
    }
}
