package com.bslx.XueXiMod.shu.technique.enchant.resource;

import com.bslx.XueXiMod.shu.technique.release.fire.DetonatePlan;
import com.bslx.XueXiMod.shu.technique.release.fire.FireConsumption;
import com.bslx.XueXiMod.shu.technique.release.fire.ResourceEffectKeys;
import com.bslx.XueXiMod.shu.technique.release.fire.SharedSpec;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.SeedMerge;
import com.bslx.XueXiMod.status.fire.seed.SeedMergeResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * 已接受伤害上的资源顺序：合并产种，再挂引燃，再引爆。
 * 不收魔力，也不写武器账。留焰、幽冥和崩地只留在载荷上，由对应宿主执行。
 */
public final class ResourceHitCommit {
    private ResourceHitCommit() {
    }

    public static HitResourceResult commit(LivingEntity target, FireCastContext cast, LivingEntity caster,
                                           ResourceEnchantLoad load, int bodyScaledBase, int legacyFixedPoints,
                                           boolean damageAccepted) {
        return commit(target, cast, caster, load, bodyScaledBase, legacyFixedPoints, damageAccepted, null);
    }

    /**
     * {@code hitAnchor} 只传递已有接触点。为空的调用没有载体碰撞点，引爆视觉自行回退，不改变结算。
     */
    public static HitResourceResult commit(LivingEntity target, FireCastContext cast, LivingEntity caster,
                                           ResourceEnchantLoad load, int bodyScaledBase, int legacyFixedPoints,
                                           boolean damageAccepted, Vec3 hitAnchor) {
        ResourceEnchantLoad resolved = load == null ? ResourceEnchantLoad.EMPTY : load;
        int manaQuote = resolved.manaPrice();
        if (!damageAccepted) {
            return HitResourceResult.notAccepted(manaQuote);
        }
        if (target == null || cast == null || bodyScaledBase < 0 || legacyFixedPoints < 0) {
            return HitResourceResult.rejected(manaQuote);
        }
        int scaledBase = addBase(bodyScaledBase, resolved.scaledSeedBase());
        if (scaledBase < 0) {
            return HitResourceResult.rejected(manaQuote);
        }
        SeedMergeResult seeds = SeedMerge.commit(target, cast, caster, scaledBase, legacyFixedPoints);
        if (seeds.kind() == SeedMergeResult.Kind.REJECTED) {
            return HitResourceResult.rejected(manaQuote);
        }
        HitResourceResult.KindleStatus kindleStatus = HitResourceResult.KindleStatus.ABSENT;
        if (resolved.kindle()) {
            kindleStatus = mountKindle(target, cast);
        }
        DetonatePlan.Outcome detonate = null;
        if (resolved.detonate()) {
            detonate = DetonatePlan.strike(target, cast, hitAnchor);
        }
        return HitResourceResult.applied(manaQuote, seeds, kindleStatus, detonate);
    }

    private static HitResourceResult.KindleStatus mountKindle(LivingEntity target, FireCastContext cast) {
        if (cast.effectClaimed(target.getUUID(), ResourceEffectKeys.KINDLE)) {
            return HitResourceResult.KindleStatus.DUPLICATE;
        }
        Optional<SharedSpec> mounted = FireConsumption.scheduleKindle(target, cast);
        if (mounted.isPresent()) {
            return HitResourceResult.KindleStatus.MOUNTED;
        }
        if (cast.effectClaimed(target.getUUID(), ResourceEffectKeys.KINDLE)) {
            return HitResourceResult.KindleStatus.DUPLICATE;
        }
        return HitResourceResult.KindleStatus.REJECTED;
    }

    private static int addBase(int bodyScaledBase, int enchantScaledBase) {
        long combined = (long) bodyScaledBase + (long) enchantScaledBase;
        if (combined > Integer.MAX_VALUE || combined < 0L) {
            return -1;
        }
        return (int) combined;
    }
}
