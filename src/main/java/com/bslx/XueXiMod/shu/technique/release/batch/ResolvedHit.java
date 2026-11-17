package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 一次已经结算的命中。资源和载体从这里拿目标、主人、结果、载荷和根上下文，不用按距离猜。
 */
public final class ResolvedHit {
    private final HostHandoff handoff;
    private final LivingEntity target;
    private final ServerPlayer owner;
    private final HitVerdict verdict;
    private final ResourceEnchantLoad resourceLoad;
    private final FireCastContext cast;
    private final int bodyScaledBase;
    private final int legacyFixedPoints;
    private final float hostBase;

    public ResolvedHit(HostHandoff handoff, LivingEntity target, ServerPlayer owner, HitVerdict verdict,
                       ResourceEnchantLoad resourceLoad, FireCastContext cast, int bodyScaledBase,
                       int legacyFixedPoints, float hostBase) {
        this.handoff = handoff;
        this.target = target;
        this.owner = owner;
        this.verdict = verdict;
        this.resourceLoad = resourceLoad == null ? ResourceEnchantLoad.EMPTY : resourceLoad;
        this.cast = cast;
        this.bodyScaledBase = bodyScaledBase;
        this.legacyFixedPoints = legacyFixedPoints;
        this.hostBase = hostBase;
    }

    public HostHandoff handoff() {
        return handoff;
    }

    public LivingEntity target() {
        return target;
    }

    public ServerPlayer owner() {
        return owner;
    }

    public HitVerdict verdict() {
        return verdict;
    }

    public ResourceEnchantLoad resourceLoad() {
        return resourceLoad;
    }

    public FireCastContext cast() {
        return cast;
    }

    public int bodyScaledBase() {
        return bodyScaledBase;
    }

    public int legacyFixedPoints() {
        return legacyFixedPoints;
    }

    public float hostBase() {
        return hostBase;
    }
}
