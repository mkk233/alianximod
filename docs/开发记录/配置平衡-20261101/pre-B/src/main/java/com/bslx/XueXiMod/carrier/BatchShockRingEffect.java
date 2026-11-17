package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceHitCommit;
import com.bslx.XueXiMod.shu.technique.release.CastFrame;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.batch.BatchFollow;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout;
import com.bslx.XueXiMod.shu.technique.release.batch.HitClock;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.batch.ImpactPartition;
import com.bslx.XueXiMod.shu.technique.release.batch.OcclusionSnapshot;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 热波只判定一次。中空环，不先击退圆心再打环。父段的排除名单和遮挡快照直接用，不复制父附魔。
 * 宿主组要有命中体才算提交，所以另外放一颗不参与判定的 12 刻空实体。
 */
public final class BatchShockRingEffect implements TechniqueEffect {
    private static final int SILENT_READY_AGE = 1_000_000;

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BirthLayout.Slot slot = BirthLayout.current();
        CastFrame frame = CastFrame.current();
        Vec3 point = resolvePoint(player, cast, slot, frame);
        Vec3 facing = resolveFacing(player, cast, slot);
        ImpactPartition partition = slot == null || slot.partition == null ? ImpactPartition.none() : slot.partition;
        OcclusionSnapshot occlusion = slot == null ? null : slot.occlusion;
        HitClock hitClock = slot == null ? null : slot.hitClock;
        boolean resourceCommit = slot != null && slot.resourceCommit;
        ResourceEnchantLoad load = slot == null || slot.resourceLoad == null ? ResourceEnchantLoad.EMPTY : slot.resourceLoad;
        int bodyScaledBase = slot == null ? 0 : slot.bodyScaledBase;
        UUID rootCastId = slot == null ? null : slot.rootCastId;
        BatchFollow follow = slot == null ? null : slot.follow;
        int legacyPoints = frame == null ? 0 : Math.max(0, frame.seedPoints);
        TideMarkSuccess tide = frame == null || frame.tideSuccess == null ? TideMarkSuccess.NONE : frame.tideSuccess;
        float damage = cast.getDamage() > 0.0F ? cast.getDamage() : BatchCarrierIds.RING_DAMAGE;
        if (frame != null && frame.anchor != null) {
            BatchTriggerVisuals.surface(serverLevel, frame.anchor);
        }
        HitVerdict best = strike(serverLevel, player, point, damage, tide, partition, occlusion, hitClock,
                resourceCommit, load, bodyScaledBase, rootCastId, legacyPoints);
        BatchCarrierVisual.spawn(serverLevel, point, BatchVisualShape.RING, BatchVisualTexture.SHOCK,
                BatchCarrierIds.RING_LIFE_TICKS, (float) BatchCarrierIds.RING_OUTER_RADIUS, -1);
        spawnSilentBody(serverLevel, player, technique, cast);
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        if (trigger != null) {
            trigger.accept(point);
        }
        if (follow != null && best != null && best.continues()) {
            follow.onVerdict(best, point, facing, null);
        }
    }

    private static HitVerdict strike(ServerLevel level, ServerPlayer player, Vec3 point, float damage,
                                     TideMarkSuccess tide, ImpactPartition partition, OcclusionSnapshot occlusion,
                                     HitClock hitClock, boolean resourceCommit, ResourceEnchantLoad load,
                                     int bodyScaledBase, UUID rootCastId, int legacyPoints) {
        List<LivingEntity> targets = BatchVolumeQuery.collect(level, point, BatchCarrierIds.RING_INNER_RADIUS,
                BatchCarrierIds.RING_OUTER_RADIUS, BatchCarrierIds.RING_HEIGHT, partition, occlusion, player.getUUID());
        HitVerdict best = null;
        FireCastContext castContext = resourceCommit ? RootCastBindings.lookup(rootCastId) : null;
        for (int targetIndex = 0; targetIndex < targets.size(); targetIndex++) {
            LivingEntity target = targets.get(targetIndex);
            if (target == null) {
                continue;
            }
            if (hitClock != null && !hitClock.tryMark(target.getUUID(), level.getGameTime(), 0)) {
                continue;
            }
            Vec3 beforeKnock = target.getDeltaMovement();
            double keepX = beforeKnock.x;
            double keepY = beforeKnock.y;
            double keepZ = beforeKnock.z;
            HitVerdict verdict = TechniqueHits.classifyRespecting(target, player, damage, tide);
            target.setDeltaMovement(keepX, keepY, keepZ);
            if (verdict == HitVerdict.DAMAGE_ACCEPTED) {
                best = HitVerdict.DAMAGE_ACCEPTED;
            } else if (verdict != null && verdict.continues() && best == null) {
                best = verdict;
            }
            if (verdict != null && verdict.grantsSeeds()) {
                if (castContext != null) {
                    ResourceHitCommit.commit(target, castContext, player, load, Math.max(0, bodyScaledBase),
                            Math.max(0, legacyPoints), true);
                }
                BatchTriggerVisuals.onAccepted(level, target.position(), target.getUUID(), load, bodyScaledBase,
                        legacyPoints);
            }
        }
        return best;
    }

    private static void spawnSilentBody(ServerLevel level, ServerPlayer player, Technique technique,
                                        TechniqueCastContext cast) {
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STRAIGHT;
        spec.length = 0.2D;
        spec.width = 0.2D;
        spec.height = 0.2D;
        spec.speed = 0.0D;
        spec.lifetime = BatchCarrierIds.RING_LIFE_TICKS;
        spec.fixedLifetime = true;
        spec.seedPoints = 0;
        spec.visual = 0;
        spec.hitReadyAge = SILENT_READY_AGE;
        spec.suppressRootSettle = true;
        spec.clipsSolids = false;
        spec.burstOnImpact = false;
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast();
        TechniqueHitSpec.spawn(level, player, spec);
    }

    private static Vec3 resolvePoint(ServerPlayer player, TechniqueCastContext cast, BirthLayout.Slot slot,
                                     CastFrame frame) {
        if (slot != null && slot.forcedOrigin != null) {
            return slot.forcedOrigin;
        }
        if (frame != null && frame.anchor != null) {
            return frame.anchor;
        }
        if (frame != null && frame.origin != null) {
            return frame.origin;
        }
        if (cast.getPos() != null) {
            return cast.getPos();
        }
        return player.getEyePosition();
    }

    private static Vec3 resolveFacing(ServerPlayer player, TechniqueCastContext cast, BirthLayout.Slot slot) {
        if (slot != null && slot.forcedDirection != null && slot.forcedDirection.lengthSqr() > 1.0E-8D) {
            return slot.forcedDirection.normalize();
        }
        if (cast.getLook() != null && cast.getLook().lengthSqr() > 1.0E-8D) {
            return cast.getLook().normalize();
        }
        return player.getLookAngle();
    }
}
