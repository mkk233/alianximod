package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.release.LifeDrain;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.shu.technique.release.payload.FlowSuccessPayload;
import com.bslx.XueXiMod.shu.technique.release.payload.HitPayloads;
import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
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
        if (!Float.isNaN(hitEntity.settleDamage)) {
            settle(hitEntity, level, target);
        }
        if (hitEntity.listener != null) {
            try {
                hitEntity.listener.onHit(target);
            } catch (Throwable throwable) {
                DebugLog.log("释放实体命中回调异常：" + throwable);
            }
        }
    }

    static void settle(TechniqueHitEntity hitEntity, ServerLevel level, LivingEntity target) {
        if (hitEntity.persistentFlow) {
            settleFlow(hitEntity, target);
            return;
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
                        level.broadcastEntityEvent(target, (byte) 2);
                        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.6F,
                                1.1F + 0.1F * hitEntity.settleCopyIndex);
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
        } else {
            ServerPlayer caster = hitEntity.getCaster();
            if (caster != null) {
                boolean landed = TechniqueHits.hurtRoot(target, caster, hitEntity.settleDamage, hitEntity.settleTide);
                if (landed) {
                    FireSeedGrant.deposit(target, caster, hitEntity.settleSeedPoints);
                }
                WetPressure.afterDamage(hitEntity.getUUID(), target, landed);
            }
        }
    }

    private static void settleFlow(TechniqueHitEntity hitEntity, LivingEntity target) {
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
            FireSeedGrant.deposit(target, caster, hitEntity.settleSeedPoints);
        }
        WetPressure.afterDamage(hitEntity.getUUID(), target, precise);
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
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area, entity -> canHit(hitEntity, entity));
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
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        hitEntity.discard();
        HitPayloads.clear(hitEntity);
        WetPressure.clear(hitEntity);
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
