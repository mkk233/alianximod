package com.bslx.XueXiMod.shu.technique.release.payload;

import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership;
import com.bslx.XueXiMod.status.WetnessState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 挂在这发命中体上的治疗、牵引、潮湿和灭火。同一发只结算一份。 */
public final class HitPayloads {
    private static final ConcurrentHashMap<UUID, Payload> ATTACHED = new ConcurrentHashMap<>();

    private HitPayloads() {
    }

    public static void attach(Entity entity, CastFrame frame) {
        if (entity == null || frame == null) {
            return;
        }
        if (frame.healAmount <= 0f && frame.pullStrength <= 0f && frame.wetnessGrant <= 0 && !frame.extinguish
                && frame.liftStrength <= 0f) {
            return;
        }
        Payload payload = new Payload(frame.healAmount, frame.healCaster, frame.pullStrength, frame.wetnessGrant,
                frame.extinguish, frame.pullAnchor, frame.liftStrength);
        ATTACHED.put(entity.getUUID(), payload);
    }

    /** 读出已经挂上的治疗、牵引、潮湿和灭火，不在这里施加。没有挂载时是空载荷。 */
    public static FlowSuccessPayload capture(Entity entity) {
        if (entity == null) {
            return FlowSuccessPayload.none();
        }
        Payload payload = ATTACHED.get(entity.getUUID());
        if (payload == null) {
            return FlowSuccessPayload.none();
        }
        return FlowSuccessPayload.attached(payload.wetnessGrant, payload.healAmount, payload.pullStrength,
                payload.liftStrength, payload.purify, payload.pullAnchor, entity instanceof TechniqueHitEntity hit
                        ? hit.casterUuid() : CombatOwnership.playerOwner(entity));
    }

    public static void apply(Entity entity, LivingEntity target, ServerPlayer caster) {
        if (entity == null || target == null) {
            return;
        }
        if (com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy.denies(caster, target)) {
            return;
        }
        Payload payload = ATTACHED.get(entity.getUUID());
        if (payload == null) {
            return;
        }
        if (payload.purify) {
            target.clearFire();
        }
        if (payload.wetnessGrant > 0) {
            WetnessState.grant(target, payload.wetnessGrant);
        }
        if (payload.healAmount > 0f && !payload.healCaster) {
            target.heal(payload.healAmount);
        }
        if (payload.pullStrength > 0f) {
            Vec3 toward = payload.pullAnchor != null ? payload.pullAnchor : (caster != null ? caster.position() : null);
            if (toward != null) {
                Vec3 delta = toward.subtract(target.position());
                if (delta.lengthSqr() > 1.0E-6) {
                    Vec3 pull = delta.normalize().scale(payload.pullStrength);
                    target.setDeltaMovement(target.getDeltaMovement().add(pull.x, pull.y * 0.25, pull.z));
                    target.hurtMarked = true;
                }
            }
        }
        if (payload.liftStrength > 0f) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, payload.liftStrength, 0.0));
            target.hurtMarked = true;
        }
    }

    public static void applyCaster(ServerPlayer caster, CastFrame frame) {
        if (caster == null || frame == null || !frame.healCaster || frame.healAmount <= 0f) {
            return;
        }
        caster.heal(frame.healAmount);
    }

    public static void clear(Entity entity) {
        if (entity != null) {
            ATTACHED.remove(entity.getUUID());
        }
    }

    private static final class Payload {
        final float healAmount;
        final boolean healCaster;
        final float pullStrength;
        final int wetnessGrant;
        final boolean purify;
        final Vec3 pullAnchor;
        final float liftStrength;

        Payload(float healAmount, boolean healCaster, float pullStrength, int wetnessGrant, boolean purify,
                Vec3 pullAnchor, float liftStrength) {
            this.healAmount = healAmount;
            this.healCaster = healCaster;
            this.pullStrength = pullStrength;
            this.wetnessGrant = wetnessGrant;
            this.purify = purify;
            this.pullAnchor = pullAnchor;
            this.liftStrength = liftStrength;
        }
    }
}
