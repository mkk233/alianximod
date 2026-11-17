package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.LifeDrain;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.shu.technique.release.payload.FlowSuccessPayload;
import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class TechniqueHitSettle {
    static boolean canHit(TechniqueHitEntity hitEntity, LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity.isSpectator()) {
            return false;
        }
        return hitEntity.casterUuid() == null || !hitEntity.casterUuid().equals(entity.getUUID());
    }

    static void tryHit(TechniqueHitEntity hitEntity, ServerLevel level, LivingEntity target) {
        long settleStart = PerformanceRecorder.begin();
        try {
        if (!acceptsTarget(hitEntity, target)) {
            return;
        }
        if (hitEntity.hitClock != null
                && hitEntity.hitClock.occupied(target.getUUID(), level.getGameTime(), hitEntity.hitInterval)) {
            return;
        }
        if (hitEntity.hitInterval <= 0) {
            if (!hitEntity.hitOnce.add(target.getUUID())) {
                return;
            }
        } else {
            long now = level.getGameTime();
            Long last = hitEntity.lastHitTick.get(target.getUUID());
            if (last != null && now - last < hitEntity.hitInterval) {
                return;
            }
            hitEntity.lastHitTick.put(target.getUUID(), now);
        }
        WetPressure.note(hitEntity.getUUID(), hitEntity.settleTally, target);
        if (!hitEntity.persistentFlow) {
            HitPayloads.apply(hitEntity, target, hitEntity.getCaster());
        }
        if (hitEntity.burstOnImpact) {
            if (hitEntity.detonated) {
                return;
            }
            hitEntity.detonated = true;
            if (hitEntity.listener != null) {
                try {
                    hitEntity.listener.onHit(target);
                } catch (Throwable throwable) {
                    DebugLog.log("释放实体命中回调异常：" + throwable);
                }
            }
            hitEntity.discard();
            HitPayloads.clear(hitEntity);
            return;
        }
        com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict sharedVerdict = null;
        if (!Float.isNaN(hitEntity.settleDamage)) {
            sharedVerdict = settle(hitEntity, level, target);
        }
        if (hitEntity.hitClock != null && sharedVerdict != null && sharedVerdict.continues()) {
            hitEntity.hitClock.tryMark(target.getUUID(), level.getGameTime(), hitEntity.hitInterval);
        }
        if (hitEntity.listener != null) {
            try {
                hitEntity.listener.onHit(target);
            } catch (Throwable throwable) {
                DebugLog.log("释放实体命中回调异常：" + throwable);
            }
        }
        } finally {
            PerformanceRecorder.end(PerfMetric.HIT_SETTLE, settleStart);
        }
    }

    static com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict settle(TechniqueHitEntity hitEntity, ServerLevel level, LivingEntity target) {
        if (hitEntity.persistentFlow) {
            settleFlow(hitEntity, target);
            return null;
        }
        if (hitEntity.settleBurnSeconds > 0) {
            target.setSecondsOnFire(hitEntity.settleBurnSeconds);
        } else if (hitEntity.settleEnchantments != null && hitEntity.settleEnchantments.has("fire")) {
            var fire = hitEntity.settleEnchantments.effect("fire");
            if (fire != null) {
                target.setSecondsOnFire(fire.durationSeconds());
            }
        }
        if (hitEntity.settleExtinguish) {
            target.clearFire();
        }
        if (hitEntity.settleControlTicks > 0) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, hitEntity.settleControlTicks, 4));
        }
        if (hitEntity.settleKnockback > 0.0F) {
            ServerPlayer caster = hitEntity.getCaster();
            Vec3 from = caster != null ? caster.position() : hitEntity.position();
            Vec3 delta = target.position().subtract(from);
            target.knockback(hitEntity.settleKnockback, delta.x, delta.z);
        }
        if (hitEntity.settleTally != null) {
            if (hitEntity.resourceCommit) {
                hitEntity.settleTally.armResource(hitEntity.rootCastId, hitEntity.resourceLoad,
                        hitEntity.bodyScaledBase, hitEntity.legacyFixedPoints);
            }
            hitEntity.settleTally.record(target, hitEntity.settleDamage, hitEntity.settleCopyIndex, hitEntity.hitInterval,
                    hitEntity.settleTide, null, hitEntity.settleSeedPoints);
            MinecraftServer server = level.getServer();
            if (server != null) {
                int feedbackTick = server.getTickCount() + hitEntity.settleCopyIndex * 2;
                if (feedbackTick < hitEntity.settleTally.flushTick()) {
                    ChainScheduler.schedule(server, feedbackTick, () -> {
                        if (!target.isAlive()) {
                            return;
                        }
                        long eventStart = PerformanceRecorder.begin();
                        try {
                            level.broadcastEntityEvent(target, (byte) 2);
                        } finally {
                            PerformanceRecorder.end(PerfMetric.EXPLICIT_SEND, eventStart);
                        }
                        if (PerformanceRecorder.isRecording()) {
                            PerformanceRecorder.add(PerfCounter.EXPLICIT_SEND_CALLS, 1L);
                        }
                        long soundStart = PerformanceRecorder.begin();
                        try {
                            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.6F,
                                    1.1F + 0.1F * hitEntity.settleCopyIndex);
                        } finally {
                            PerformanceRecorder.end(PerfMetric.EXPLICIT_SEND, soundStart);
                        }
                        if (PerformanceRecorder.isRecording()) {
                            PerformanceRecorder.add(PerfCounter.EXPLICIT_SEND_CALLS, 1L);
                        }
                        Vec3 direction = hitEntity.forward;
                        ServerPlayer caster = hitEntity.getCaster();
                        if (caster != null) {
                            Vec3 delta = target.position().subtract(caster.position());
                            if (delta.lengthSqr() > 1.0E-8) {
                                direction = delta.normalize();
                            }
                        }
                        target.knockback(0.08, direction.x, direction.z);
                        target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.02, 0.0));
                    });
                }
            }
            return com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.PENDING;
        } else {
            ServerPlayer caster = hitEntity.getCaster();
            if (caster != null && hitEntity.respectLanding) {
                com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict =
                        TechniqueHits.classifyRespecting(target, caster, hitEntity.settleDamage, hitEntity.settleTide);
                ResourceSettlement.afterVerdict(
                        hitEntity, target, caster, verdict);
                WetPressure.afterDamage(hitEntity.getUUID(), target, verdict.grantsSeeds());
                if (hitEntity.batchFollow != null && verdict != com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.PENDING) {
                    hitEntity.batchFollow.onVerdict(verdict, hitEntity.position(), hitEntity.forward, target);
                }
                return verdict;
            } else if (caster != null) {
                boolean landed = TechniqueHits.hurtRoot(target, caster, hitEntity.settleDamage, hitEntity.settleTide);
                com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict = landed
                        ? com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.DAMAGE_ACCEPTED
                        : com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.ESTABLISHED_NO_DAMAGE;
                ResourceSettlement.afterVerdict(hitEntity, target, caster, verdict);
                WetPressure.afterDamage(hitEntity.getUUID(), target, landed);
                return verdict;
            }
        }
        return null;
    }

    static boolean sweepEarliest(TechniqueHitEntity hitEntity, ServerLevel level) {
        Vec3 fromPoint = hitEntity.previousPosition();
        if (fromPoint == null) {
            return false;
        }
        com.bslx.XueXiMod.shu.technique.release.batch.ContactSweep.Contact contact =
                com.bslx.XueXiMod.shu.technique.release.batch.ContactSweep.earliest(
                        level, fromPoint, hitEntity.position(),
                        entity -> canHit(hitEntity, entity) && acceptsTarget(hitEntity, entity),
                        hitEntity.rejectedTargets);
        if (contact.kind() == com.bslx.XueXiMod.shu.technique.release.batch.ContactSweep.Kind.TERRAIN
                && (hitEntity.burstOnImpact || hitEntity.clipsSolids())) {
            expireTerrain(hitEntity, level, contact.point());
            return true;
        }
        if (contact.kind() != com.bslx.XueXiMod.shu.technique.release.batch.ContactSweep.Kind.ENTITY
                || contact.entity() == null) {
            return false;
        }
        LivingEntity target = contact.entity();
        ServerPlayer caster = hitEntity.getCaster();
        if (caster == null) {
            return false;
        }
        Vec3 sweepPoint = contact.point() == null ? hitEntity.position() : contact.point();
        com.bslx.XueXiMod.carrier.MeteorContactResult carrierContact = com.bslx.XueXiMod.carrier.BatchCarrierHooks.intercept(
                hitEntity, level, sweepPoint, target, com.bslx.XueXiMod.carrier.MeteorContactKind.ENTITY);
        if (carrierContact.handled()) {
            com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict carrierVerdict = carrierContact.verdict();
            if (hitEntity.batchFollow != null && carrierVerdict != null
                    && carrierVerdict != com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.INVALID_OR_CANCELLED
                    && carrierVerdict != com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.PENDING
                    && carrierVerdict.continues()) {
                hitEntity.batchFollow.onVerdict(carrierVerdict, sweepPoint, hitEntity.forward(), null);
            }
            if (hitEntity.burstOnImpact) {
                hitEntity.setPos(sweepPoint.x, sweepPoint.y, sweepPoint.z);
                onExpire(hitEntity, level);
                return true;
            }
            return false;
        }
        if (hitEntity.hitClock != null
                && hitEntity.hitClock.occupied(target.getUUID(), level.getGameTime(), hitEntity.hitInterval)) {
            hitEntity.rejectedTargets.add(target.getUUID());
            return sweepEarliest(hitEntity, level);
        }
        com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict =
                TechniqueHits.classifyRespecting(target, caster, hitEntity.settleDamage, hitEntity.settleTide);
        if (verdict == com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.INVALID_OR_CANCELLED
                || verdict == com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.PENDING) {
            if (verdict == com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.INVALID_OR_CANCELLED) {
                hitEntity.rejectedTargets.add(target.getUUID());
                return sweepEarliest(hitEntity, level);
            }
            return false;
        }
        if (hitEntity.hitClock != null) {
            hitEntity.hitClock.tryMark(target.getUUID(), level.getGameTime(), hitEntity.hitInterval);
        }
        if (hitEntity.hitInterval <= 0) {
            hitEntity.hitOnce.add(target.getUUID());
        }
        ResourceSettlement.afterVerdict(
                hitEntity, target, caster, verdict);
        WetPressure.afterDamage(hitEntity.getUUID(), target, verdict.grantsSeeds());
        Vec3 contactPoint = contact.point() == null ? hitEntity.position() : contact.point();
        if (hitEntity.batchFollow != null) {
            hitEntity.batchFollow.onVerdict(verdict, contactPoint, hitEntity.forward, target);
        }
        if (hitEntity.burstOnImpact) {
            hitEntity.setPos(contactPoint.x, contactPoint.y, contactPoint.z);
            onExpire(hitEntity, level);
            return true;
        }
        return false;
    }

    private static boolean acceptsTarget(TechniqueHitEntity hitEntity, LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (hitEntity.partition != null && hitEntity.partition.excludes(target.getUUID())) {
            return false;
        }
        return hitEntity.occlusion == null || hitEntity.occlusion.allows(target.getUUID());
    }

    private static void settleFlow(TechniqueHitEntity hitEntity, LivingEntity target) {
        long settleStart = PerformanceRecorder.begin();
        try {
        FlowSuccessPayload captured = strikePayload(hitEntity, target);
        if (hitEntity.settleTally != null) {
            hitEntity.settleTally.record(target, hitEntity.settleDamage, hitEntity.settleCopyIndex,
                    hitEntity.hitInterval, hitEntity.settleTide, captured, hitEntity.settleSeedPoints);
            return;
        }
        ServerPlayer caster = hitEntity.getCaster();
        if (!ownerReady(caster)) {
            return;
        }
        float mergedAmount = DomainAuraResolver.merge(hitEntity.settleDamage, caster,
                AttackEnchantmentContext.Kind.ROOT_TECHNIQUE);
        boolean precise = TechniqueHits.hurtConfirmed(target, caster, mergedAmount,
                TechniqueHits.Landing.RESPECT_INVULNERABILITY, hitEntity.settleTide);
        if (precise) {
            captured.apply(target);
        }
        ResourceSettlement.afterVerdict(hitEntity, target, caster,
                precise
                        ? com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.DAMAGE_ACCEPTED
                        : com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.ESTABLISHED_NO_DAMAGE);
        WetPressure.afterDamage(hitEntity.getUUID(), target, precise);
        } finally {
            PerformanceRecorder.end(PerfMetric.HIT_SETTLE, settleStart);
        }
    }

    private static boolean ownerReady(ServerPlayer caster) {
        return caster != null && caster.isAlive() && !caster.isRemoved();
    }

    private static FlowSuccessPayload strikePayload(TechniqueHitEntity hitEntity, LivingEntity target) {
        int burnSeconds = hitEntity.settleBurnSeconds;
        if (burnSeconds <= 0 && hitEntity.settleEnchantments != null && hitEntity.settleEnchantments.has("fire")) {
            var fireEffect = hitEntity.settleEnchantments.effect("fire");
            if (fireEffect != null) {
                burnSeconds = fireEffect.durationSeconds();
            }
        }
        ServerPlayer caster = hitEntity.getCaster();
        Vec3 fromPoint = caster != null ? caster.position() : hitEntity.position();
        Vec3 separation = target.position().subtract(fromPoint);
        return HitPayloads.capture(hitEntity).withStrike(burnSeconds, hitEntity.settleControlTicks,
                hitEntity.settleKnockback, separation.x, separation.z, hitEntity.settleExtinguish);
    }

    static void detonate(TechniqueHitEntity hitEntity, ServerLevel level) {
        hitEntity.detonated = true;
        AABB area = hitEntity.getBoundingBox().inflate(1.5);
        List<LivingEntity> victims = LivingCandidateQueries.entityQuery(level, area,
                entity -> canHit(hitEntity, entity));
        for (LivingEntity target : victims) {
            settle(hitEntity, level, target);
            com.bslx.XueXiMod.shu.technique.release.LifeDrain.start(level, hitEntity.getCaster(), target, 1.0F, 5, 12);
            if (hitEntity.listener != null) {
                try {
                    hitEntity.listener.onHit(target);
                } catch (Throwable throwable) {
                    DebugLog.log("释放实体命中回调异常：" + throwable);
                }
            }
        }
        long particleStart = PerformanceRecorder.begin();
        try {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        } finally {
            PerformanceRecorder.end(PerfMetric.EXPLICIT_SEND, particleStart);
        }
        if (PerformanceRecorder.isRecording()) {
            PerformanceRecorder.add(PerfCounter.EXPLICIT_SEND_CALLS, 1L);
        }
        hitEntity.discard();
        HitPayloads.clear(hitEntity);
        WetPressure.clear(hitEntity);
    }

    static void expireLifetime(TechniqueHitEntity hitEntity, ServerLevel level) {
        com.bslx.XueXiMod.carrier.BatchCarrierHooks.intercept(
                hitEntity, level, hitEntity.position(), null, com.bslx.XueXiMod.carrier.MeteorContactKind.TIMEOUT);
        onExpire(hitEntity, level);
    }

    static void expireTerrain(TechniqueHitEntity hitEntity, ServerLevel level, Vec3 hitPoint) {
        Vec3 sweepPoint = hitPoint == null ? hitEntity.position() : hitPoint;
        if (sweepPoint != null) {
            hitEntity.setPos(sweepPoint.x, sweepPoint.y, sweepPoint.z);
        }
        com.bslx.XueXiMod.carrier.MeteorContactResult carrierContact = com.bslx.XueXiMod.carrier.BatchCarrierHooks.intercept(
                hitEntity, level, sweepPoint, null, com.bslx.XueXiMod.carrier.MeteorContactKind.TERRAIN);
        com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict carrierVerdict = carrierContact.verdict();
        if (carrierContact.handled() && carrierContact.continueChildren() && hitEntity.batchFollow != null
                && carrierVerdict != null
                && carrierVerdict != com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.PENDING
                && carrierVerdict.continues()) {
            hitEntity.batchFollow.onVerdict(carrierVerdict, sweepPoint, hitEntity.forward(), null);
        }
        onExpire(hitEntity, level);
    }

    static void onExpire(TechniqueHitEntity hitEntity, ServerLevel level) {
        if (hitEntity.listener != null) {
            try {
                hitEntity.listener.onExpire(hitEntity.position());
            } catch (Throwable throwable) {
                DebugLog.log("释放实体过期回调异常：" + throwable);
            }
        }
        hitEntity.discard();
        HitPayloads.clear(hitEntity);
        WetPressure.clear(hitEntity);
    }
}
