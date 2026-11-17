package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.carrier.BatchCarrierHooks;
import com.bslx.XueXiMod.carrier.MeteorContactKind;
import com.bslx.XueXiMod.carrier.MeteorContactResult;
import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.LifeDrain;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.batch.ContactSweep;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.shu.technique.release.payload.FlowSuccessPayload;
import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 命中体的去重和结算。调用前命中盒已经选好目标。
 * 间隔、一次性账本和共享命中钟在这里更新；无敌帧由所选的伤害入口决定，本类不直接清它。
 */
final class TechniqueHitSettle {
    /** 只排除死者、旁观者和施法者本人。共享钟、遮挡和分区不在这里判断。 */
    static boolean canHit(TechniqueHitEntity hitEntity, LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        return hitEntity.casterUuid() == null || !hitEntity.casterUuid().equals(entity.getUUID());
    }

    /**
     * 共享钟或本实体账本挡住时直接返回。持续接触按间隔记账，否则每个目标只进一次。
     * 爆裂命中只触发回调并移除；普通命中才进入伤害结算。
     */
    static void tryHit(TechniqueHitEntity hitEntity, ServerLevel level, LivingEntity target) {
        long settleStart = PerformanceRecorder.begin();
        try {
        if (HarmPolicy.denies(hitEntity.casterUuid(), target)) {
            return;
        }
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
            notifyHit(hitEntity, target);
            hitEntity.discard();
            HitPayloads.clear(hitEntity);
            return;
        }
        HitVerdict sharedVerdict = null;
        if (!Float.isNaN(hitEntity.settleDamage)) {
            sharedVerdict = settle(hitEntity, level, target);
        }
        if (hitEntity.hitClock != null && sharedVerdict != null && sharedVerdict.continues()) {
            hitEntity.hitClock.tryMark(target.getUUID(), level.getGameTime(), hitEntity.hitInterval);
        }
        notifyHit(hitEntity, target);
        if (sharedVerdict != null && sharedVerdict.continues() && hitEntity.consumesOnEntityHit()
                && (hitEntity.settleTally == null
                || sharedVerdict != HitVerdict.PENDING)) {
            onExpire(hitEntity, level);
        }
        } finally {
            PerformanceRecorder.end(PerfMetric.HIT_SETTLE, settleStart);
        }
    }

    /**
     * 持续流走确认伤害并尊重无敌帧。共享账本只记录，不在这里打出伤害。
     * 无账本时，尊重落点走新入口，其余走默认绕过无敌帧的根结算。
     */
    static HitVerdict settle(TechniqueHitEntity hitEntity, ServerLevel level, LivingEntity target) {
        if (HarmPolicy.denies(hitEntity.casterUuid(), target)) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        if (hitEntity.persistentFlow) {
            settleFlow(hitEntity, target);
            return null;
        }
        int burnSeconds = resolveBurnSeconds(hitEntity);
        if (burnSeconds > 0) {
            target.setSecondsOnFire(burnSeconds);
        }
        if (hitEntity.settleExtinguish) {
            target.clearFire();
        }
        if (hitEntity.settleControlTicks > 0) {
            com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime.suppressLegacyControl(
                    hitEntity.casterUuid(), target);
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
            return HitVerdict.PENDING;
        } else {
            ServerPlayer caster = hitEntity.getCaster();
            if (caster != null && hitEntity.respectLanding) {
                HitVerdict verdict = TechniqueHits.classifyRespecting(target, caster, hitEntity.settleDamage, hitEntity.settleTide);
                ResourceSettlement.afterVerdict(hitEntity, target, caster, verdict, null);
                WetPressure.afterDamage(hitEntity.getUUID(), target, verdict.grantsSeeds());
                offerSoloVine(hitEntity, caster, target, verdict);
                if (hitEntity.batchFollow != null && verdict != HitVerdict.PENDING) {
                    hitEntity.batchFollow.onVerdict(verdict, hitEntity.position(), hitEntity.forward, target);
                }
                return verdict;
            } else if (caster != null) {
                HitVerdict verdict = TechniqueHits.classifyRoot(target, caster, hitEntity.settleDamage, hitEntity.settleTide);
                ResourceSettlement.afterVerdict(hitEntity, target, caster, verdict, null);
                WetPressure.afterDamage(hitEntity.getUUID(), target, verdict.grantsSeeds());
                offerSoloVine(hitEntity, caster, target, verdict);
                return verdict;
            }
        }
        return null;
    }

    /**
     * 沿上一位置到当前位置找最早接触。无效目标会记入拒绝名单后继续找。
     * 伤害尊重无敌帧；地形、爆裂或已被处理的接触可以结束实体。
     */
    static boolean sweepEarliest(TechniqueHitEntity hitEntity, ServerLevel level) {
        Vec3 fromPoint = hitEntity.previousPosition();
        if (fromPoint == null) {
            return false;
        }
        ContactSweep.Contact contact = ContactSweep.earliest(
                level, fromPoint, hitEntity.position(),
                entity -> canHit(hitEntity, entity) && acceptsTarget(hitEntity, entity),
                hitEntity.rejectedTargets);
        if (contact.kind() == ContactSweep.Kind.TERRAIN
                && (hitEntity.burstOnImpact || hitEntity.clipsSolids())) {
            expireTerrain(hitEntity, level, contact.point());
            return true;
        }
        if (contact.kind() != ContactSweep.Kind.ENTITY || contact.entity() == null) {
            return false;
        }
        LivingEntity target = contact.entity();
        ServerPlayer caster = hitEntity.getCaster();
        if (caster == null) {
            return false;
        }
        if (HarmPolicy.denies(caster, target)) {
            hitEntity.rejectedTargets.add(target.getUUID());
            return sweepEarliest(hitEntity, level);
        }
        Vec3 contactPoint = contact.point() == null ? hitEntity.position() : contact.point();
        MeteorContactResult carrierContact = BatchCarrierHooks.intercept(
                hitEntity, level, contactPoint, target, MeteorContactKind.ENTITY);
        if (carrierContact.handled()) {
            HitVerdict carrierVerdict = carrierContact.verdict();
            if (hitEntity.batchFollow != null && carrierVerdict != null
                    && carrierVerdict != HitVerdict.INVALID_OR_CANCELLED
                    && carrierVerdict != HitVerdict.PENDING
                    && carrierVerdict.continues()) {
                hitEntity.batchFollow.onVerdict(carrierVerdict, contactPoint, hitEntity.forward(), null);
            }
            if (hitEntity.burstOnImpact) {
                hitEntity.setPos(contactPoint.x, contactPoint.y, contactPoint.z);
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
        HitVerdict verdict = TechniqueHits.classifyRespecting(target, caster, hitEntity.settleDamage, hitEntity.settleTide);
        if (verdict == HitVerdict.INVALID_OR_CANCELLED) {
            hitEntity.rejectedTargets.add(target.getUUID());
            return sweepEarliest(hitEntity, level);
        }
        if (verdict == HitVerdict.PENDING) {
            return false;
        }
        if (hitEntity.hitClock != null) {
            hitEntity.hitClock.tryMark(target.getUUID(), level.getGameTime(), hitEntity.hitInterval);
        }
        if (hitEntity.hitInterval <= 0) {
            hitEntity.hitOnce.add(target.getUUID());
        }
        ResourceSettlement.afterVerdict(hitEntity, target, caster, verdict, contactPoint);
        WetPressure.afterDamage(hitEntity.getUUID(), target, verdict.grantsSeeds());
        offerSoloVine(hitEntity, caster, target, verdict);
        if (hitEntity.batchFollow != null) {
            hitEntity.batchFollow.onVerdict(verdict, contactPoint, hitEntity.forward, target);
        }
        if (hitEntity.burstOnImpact) {
            hitEntity.setPos(contactPoint.x, contactPoint.y, contactPoint.z);
            onExpire(hitEntity, level);
            return true;
        }
        if (verdict.continues() && hitEntity.consumesOnEntityHit()) {
            hitEntity.setPos(contactPoint.x, contactPoint.y, contactPoint.z);
            onExpire(hitEntity, level);
            return true;
        }
        return false;
    }

    /** 分区排除优先。没有遮挡快照时其余目标都保留。 */
    private static boolean acceptsTarget(TechniqueHitEntity hitEntity, LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (hitEntity.partition != null && hitEntity.partition.excludes(target.getUUID())) {
            return false;
        }
        return hitEntity.occlusion == null || hitEntity.occlusion.allows(target.getUUID());
    }

    /** 有共享账本时只记录捕获的附带效果。没有账本才当场确认伤害，正伤被接受后才施加。 */
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
        HitVerdict verdict = TechniqueHits.classifyConfirmed(
                target, caster, mergedAmount, TechniqueHits.Landing.RESPECT_INVULNERABILITY, hitEntity.settleTide);
        if (verdict.grantsSeeds()) {
            captured.apply(target);
        }
        ResourceSettlement.afterVerdict(hitEntity, target, caster, verdict, null);
        WetPressure.afterDamage(hitEntity.getUUID(), target, verdict.grantsSeeds());
        offerSoloVine(hitEntity, caster, target, verdict);
        } finally {
            PerformanceRecorder.end(PerfMetric.HIT_SETTLE, settleStart);
        }
    }

    /** 单份剑术没有共享账本，伤害被接受后用实体上的冻结资格提交一次。 */
    private static void offerSoloVine(TechniqueHitEntity hitEntity, ServerPlayer caster,
                                      LivingEntity target, HitVerdict verdict) {
        if (!hitEntity.vineArmed || hitEntity.vineActionId == null || hitEntity.settleTally != null || verdict == null) {
            return;
        }
        com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime.offerQualifiedHit(
                caster, target, hitEntity.vineActionId,
                verdict == HitVerdict.INVALID_OR_CANCELLED,
                verdict == HitVerdict.DAMAGE_ACCEPTED,
                hitEntity.vineSpectrumEligible);
    }

    /** 施法者已死或已移除时，当场确认不能继续。 */
    private static boolean ownerReady(ServerPlayer caster) {
        return caster != null && caster.isAlive() && !caster.isRemoved();
    }

    /** 只捕获燃烧、减速、击退和灭火。这里不写目标状态，也不改账本。 */
    private static FlowSuccessPayload strikePayload(TechniqueHitEntity hitEntity, LivingEntity target) {
        int burnSeconds = resolveBurnSeconds(hitEntity);
        ServerPlayer caster = hitEntity.getCaster();
        Vec3 fromPoint = caster != null ? caster.position() : hitEntity.position();
        Vec3 separation = target.position().subtract(fromPoint);
        return HitPayloads.capture(hitEntity).withStrike(burnSeconds, hitEntity.settleControlTicks,
                hitEntity.settleKnockback, separation.x, separation.z, hitEntity.settleExtinguish);
    }

    /** 爆开范围内逐个结算，并给每个目标另排 5 次延迟扣血。随后移除命中体。 */
    static void detonate(TechniqueHitEntity hitEntity, ServerLevel level) {
        hitEntity.detonated = true;
        AABB area = hitEntity.getBoundingBox().inflate(1.5);
        List<LivingEntity> victims = LivingCandidateQueries.entityQuery(level, area,
                entity -> canHit(hitEntity, entity) && !HarmPolicy.denies(hitEntity.casterUuid(), entity));
        for (LivingEntity target : victims) {
            settle(hitEntity, level, target);
            LifeDrain.start(level, hitEntity.getCaster(), target, 1.0F, 5, 12);
            notifyHit(hitEntity, target);
        }
        long particleStart = PerformanceRecorder.begin();
        try {
            level.sendParticles(ParticleTypes.EXPLOSION, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
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

    /** 寿命结束先通知载体超时，再走统一移除。 */
    static void expireLifetime(TechniqueHitEntity hitEntity, ServerLevel level) {
        BatchCarrierHooks.intercept(hitEntity, level, hitEntity.position(), null, MeteorContactKind.TIMEOUT);
        onExpire(hitEntity, level);
    }

    /** 先把实体放到碰撞点。载体要求续发时才通知批次，无论如何都会移除。 */
    static void expireTerrain(TechniqueHitEntity hitEntity, ServerLevel level, Vec3 hitPoint) {
        Vec3 sweepPoint = hitPoint == null ? hitEntity.position() : hitPoint;
        hitEntity.setPos(sweepPoint.x, sweepPoint.y, sweepPoint.z);
        MeteorContactResult carrierContact = BatchCarrierHooks.intercept(
                hitEntity, level, sweepPoint, null, MeteorContactKind.TERRAIN);
        HitVerdict carrierVerdict = carrierContact.verdict();
        if (carrierContact.handled() && carrierContact.continueChildren() && hitEntity.batchFollow != null
                && carrierVerdict != null
                && carrierVerdict != HitVerdict.PENDING
                && carrierVerdict.continues()) {
            hitEntity.batchFollow.onVerdict(carrierVerdict, sweepPoint, hitEntity.forward(), null);
        }
        onExpire(hitEntity, level);
    }

    /** 过期回调、移除、附带效果和湿压都在这里收束。回调异常不阻止移除。 */
    static void onExpire(TechniqueHitEntity hitEntity, ServerLevel level) {
        notifyExpire(hitEntity);
        hitEntity.discard();
        HitPayloads.clear(hitEntity);
        WetPressure.clear(hitEntity);
    }

    private static int resolveBurnSeconds(TechniqueHitEntity hitEntity) {
        if (hitEntity.settleBurnSeconds > 0) {
            return hitEntity.settleBurnSeconds;
        }
        if (hitEntity.settleEnchantments != null && hitEntity.settleEnchantments.has("fire")) {
            var fireEffect = hitEntity.settleEnchantments.effect("fire");
            if (fireEffect != null) {
                return fireEffect.durationSeconds();
            }
        }
        return 0;
    }

    private static void notifyHit(TechniqueHitEntity hitEntity, LivingEntity target) {
        if (hitEntity.listener == null) {
            return;
        }
        try {
            hitEntity.listener.onHit(target);
        } catch (Throwable throwable) {
            DebugLog.log("释放实体命中回调异常：" + throwable);
        }
    }

    private static void notifyExpire(TechniqueHitEntity hitEntity) {
        if (hitEntity.listener == null) {
            return;
        }
        try {
            hitEntity.listener.onExpire(hitEntity.position());
        } catch (Throwable throwable) {
            DebugLog.log("释放实体过期回调异常：" + throwable);
        }
    }
}
