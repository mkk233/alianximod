package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceHitCommit;
import com.bslx.XueXiMod.shu.technique.release.batch.CarrierHooks;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.batch.HostHandoff;
import com.bslx.XueXiMod.shu.technique.release.batch.ResolvedHit;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 新链把本体基数、旧固定点数和附魔载荷收成一次提交。
 * 旧链仍走原来的 deposit。这里不调用产种账本的 claim，避免和上下文里的生产标记各扣一次。
 */
final class ResourceSettlement {
    private ResourceSettlement() {
    }

    /**
     * 旧链只在正伤时按固定点数产种。新链把基数、旧点数和载荷收成一次，并通知载体。
     * {@code hitAnchor} 只转交引爆视觉；为空不改交接事件点。
     */
    static void afterVerdict(TechniqueHitEntity hitEntity, LivingEntity target, ServerPlayer caster, HitVerdict verdict,
                             Vec3 hitAnchor) {
        if (hitEntity == null || target == null || caster == null) {
            return;
        }
        boolean accepted = verdict != null && verdict.grantsSeeds();
        if (!hitEntity.resourceCommit) {
            if (accepted) {
                FireSeedGrant.deposit(target, caster, hitEntity.settleSeedPoints);
            }
            return;
        }
        FireCastContext cast = RootCastBindings.lookup(hitEntity.rootCastId);
        ResourceEnchantLoad load = hitEntity.resourceLoad == null ? ResourceEnchantLoad.EMPTY : hitEntity.resourceLoad;
        if (cast != null) {
            ResourceHitCommit.commit(target, cast, caster, load, Math.max(0, hitEntity.bodyScaledBase),
                    Math.max(0, hitEntity.legacyFixedPoints), accepted, hitAnchor);
        }
        HostHandoff handoff = new HostHandoff(hitEntity.rootCastId, caster, hitEntity.branchOrdinal,
                hitEntity.settleCopyIndex, hitEntity.hitClock, hitEntity.position(), hitEntity.forward(),
                hitEntity.childInstance);
        CarrierHooks.notifyResolved(new ResolvedHit(handoff, target, caster, verdict, load, cast,
                hitEntity.bodyScaledBase, hitEntity.legacyFixedPoints, hitEntity.settleDamage));
    }
}
