package com.bslx.XueXiMod.shu.technique.release.payload;

import com.bslx.XueXiMod.status.WetnessState;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 一次持续接触先记下、等真实正伤后再提交的载荷。本身不可变。
 * 基础潮湿、燃烧、缓慢和击退都不在记下的时候生效。
 * 合并时同一种强度或时长取较大的一份，击退和牵引方向留第一次收下的那份。
 * 基础潮湿只保留合并后的那一份，不按复制份数相乘。潮印不在这里。
 */
public final class FlowSuccessPayload {
    private static final FlowSuccessPayload EMPTY = new FlowSuccessPayload(
            0, 0, 0, 0f, 0.0, 0.0, false, 0f, 0f, 0f, null, null);

    private final int wetness;
    private final int burnSeconds;
    private final int slowTicks;
    private final float knockStrength;
    private final double pushAcross;
    private final double pushAlong;
    private final boolean purify;
    private final float healAmount;
    private final float liftStrength;
    private final float pullStrength;
    private final Vec3 pullAnchor;
    private final UUID actorId;

    private FlowSuccessPayload(int wetness, int burnSeconds, int slowTicks, float knockStrength,
                               double pushAcross, double pushAlong, boolean purify, float healAmount,
                               float liftStrength, float pullStrength, Vec3 pullAnchor, UUID actorId) {
        this.wetness = wetness;
        this.burnSeconds = burnSeconds;
        this.slowTicks = slowTicks;
        this.knockStrength = knockStrength;
        this.pushAcross = pushAcross;
        this.pushAlong = pushAlong;
        this.purify = purify;
        this.healAmount = healAmount;
        this.liftStrength = liftStrength;
        this.pullStrength = pullStrength;
        this.pullAnchor = pullAnchor;
        this.actorId = actorId;
    }

    public static FlowSuccessPayload none() {
        return EMPTY;
    }

    public static FlowSuccessPayload attached(int wetness, float healAmount, float pullStrength, float liftStrength,
                                              boolean purify, Vec3 pullAnchor) {
        return attached(wetness, healAmount, pullStrength, liftStrength, purify, pullAnchor, null);
    }

    public static FlowSuccessPayload attached(int wetness, float healAmount, float pullStrength, float liftStrength,
                                              boolean purify, Vec3 pullAnchor, UUID actorId) {
        return new FlowSuccessPayload(Math.max(0, wetness), 0, 0, 0f, 0.0, 0.0, purify,
                Math.max(0f, healAmount), Math.max(0f, liftStrength), Math.max(0f, pullStrength), pullAnchor, actorId);
    }

    public FlowSuccessPayload withStrike(int burnSeconds, int slowTicks, float knockStrength,
                                          double pushAcross, double pushAlong, boolean extinguish) {
        return new FlowSuccessPayload(this.wetness, Math.max(this.burnSeconds, Math.max(0, burnSeconds)),
                Math.max(this.slowTicks, Math.max(0, slowTicks)),
                Math.max(this.knockStrength, Math.max(0f, knockStrength)),
                pushAcross, pushAlong, this.purify || extinguish, this.healAmount, this.liftStrength,
                this.pullStrength, this.pullAnchor, this.actorId);
    }

    /** 已收下的这份在前。后来的份只抬高强度，不改已经记下的方向。 */
    public FlowSuccessPayload mergedWith(FlowSuccessPayload incoming) {
        if (incoming == null) {
            return this;
        }
        return new FlowSuccessPayload(
                Math.max(this.wetness, incoming.wetness),
                Math.max(this.burnSeconds, incoming.burnSeconds),
                Math.max(this.slowTicks, incoming.slowTicks),
                Math.max(this.knockStrength, incoming.knockStrength),
                this.pushAcross,
                this.pushAlong,
                this.purify || incoming.purify,
                Math.max(this.healAmount, incoming.healAmount),
                Math.max(this.liftStrength, incoming.liftStrength),
                Math.max(this.pullStrength, incoming.pullStrength),
                this.pullAnchor,
                this.actorId != null ? this.actorId : incoming.actorId);
    }

    public void apply(LivingEntity target) {
        if (target == null) {
            return;
        }
        if (HarmPolicy.denies(this.actorId, target)) {
            return;
        }
        if (this.wetness > 0 && target.isAlive()) {
            WetnessState.grant(target, this.wetness);
        }
        if (this.burnSeconds > 0) {
            target.setSecondsOnFire(this.burnSeconds);
        }
        if (this.purify) {
            target.clearFire();
        }
        if (this.slowTicks > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.slowTicks, 4));
        }
        if (this.knockStrength > 0f) {
            target.knockback(this.knockStrength, this.pushAcross, this.pushAlong);
        }
        if (this.healAmount > 0f) {
            target.heal(this.healAmount);
        }
        if (this.liftStrength > 0f) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, this.liftStrength, 0.0));
            target.hurtMarked = true;
        }
        if (this.pullStrength > 0f && this.pullAnchor != null) {
            Vec3 separation = this.pullAnchor.subtract(target.position());
            if (separation.lengthSqr() > 1.0E-6) {
                Vec3 pullStep = separation.normalize().scale(this.pullStrength);
                target.setDeltaMovement(target.getDeltaMovement().add(pullStep.x, pullStep.y * 0.25, pullStep.z));
                target.hurtMarked = true;
            }
        }
    }

    public int wetness() {
        return this.wetness;
    }

    public int burnSeconds() {
        return this.burnSeconds;
    }

    public int slowTicks() {
        return this.slowTicks;
    }

    public float knockStrength() {
        return this.knockStrength;
    }

    public double pushAcross() {
        return this.pushAcross;
    }

    public double pushAlong() {
        return this.pushAlong;
    }

    public boolean purify() {
        return this.purify;
    }

    public float healAmount() {
        return this.healAmount;
    }

    public float liftStrength() {
        return this.liftStrength;
    }

    public float pullStrength() {
        return this.pullStrength;
    }

    public Vec3 pullAnchor() {
        return this.pullAnchor;
    }
}
