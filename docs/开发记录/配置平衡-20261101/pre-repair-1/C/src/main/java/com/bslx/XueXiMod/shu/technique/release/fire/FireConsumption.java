package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.FireSeedSnapshot;
import com.bslx.XueXiMod.status.fire.seed.FireSeedState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/** 挂载只锁定参数。引爆在分量合格后一次扣完当时余额。按段燃料由燃烧推进扣除。 */
public final class FireConsumption {
    private FireConsumption() {
    }

    public interface SpecSource {
        Optional<SharedSpec> specify(int balance, double furnaceBonus);
    }

    public static Optional<SharedSpec> schedule(LivingEntity target, FireCastContext cast, String effectKey,
                                                SpecSource source) {
        return mountShared(target, cast, effectKey, source, false, false);
    }

    /** 挂上等待为 0 的共享层，本方法不结算。调用方应在同拍所有挂载之后集中 settleOpening。 */
    public static Optional<SharedSpec> scheduleOpening(LivingEntity target, FireCastContext cast, String effectKey,
                                                       SpecSource source) {
        return mountShared(target, cast, effectKey, source, true, false);
    }

    /**
     * 新引燃。首跳使用完整间隔等待，不走当拍首段。
     * 同一施法、目标和效果键成功挂上之后不再挂第二次。满 8 层或不足 1 档时不占燃料、也不占这次声明。
     */
    public static Optional<SharedSpec> scheduleKindle(LivingEntity target, FireCastContext cast) {
        if (!hostReady(target, cast)) {
            return Optional.empty();
        }
        UUID targetId = target.getUUID();
        if (cast.effectClaimed(targetId, ResourceEffectKeys.KINDLE)) {
            return Optional.empty();
        }
        BurnLedger existing = BurnCarrier.read(target);
        if (existing.duplicate(cast.castId(), targetId, ResourceEffectKeys.KINDLE)) {
            cast.claimEffect(targetId, ResourceEffectKeys.KINDLE);
            return Optional.empty();
        }
        if (existing.trackCount() >= FireLimits.PLAN_CAP) {
            return Optional.empty();
        }
        if (FireSeedState.value(target) / FireLimits.STANDARD_SEGMENT_DEMAND < 1) {
            return Optional.empty();
        }
        Optional<SharedSpec> mounted = mountShared(target, cast, ResourceEffectKeys.KINDLE, KindlePlan::specify,
                false, true);
        if (mounted.isPresent()) {
            cast.claimEffect(targetId, ResourceEffectKeys.KINDLE);
        }
        return mounted;
    }

    public static double frozenFurnaceBonus(LivingEntity target, UUID casterId) {
        return casterBonus(target, casterId);
    }

    private static Optional<SharedSpec> mountShared(LivingEntity target, FireCastContext cast, String effectKey,
                                                    SpecSource source, boolean openingDue, boolean rotateRemainder) {
        if (!hostReady(target, cast) || source == null || !BurnSchedule.validKey(effectKey)) {
            return Optional.empty();
        }
        int balance = FireSeedState.value(target);
        if (balance <= 0) {
            return Optional.empty();
        }
        double furnaceBonus = casterBonus(target, cast.casterId());
        int tiers = balance / 10;
        Optional<SharedSpec> specified = specify(source, balance, furnaceBonus);
        if (specified.isEmpty()) {
            return Optional.empty();
        }
        SharedSpec spec = specified.get();
        BurnLedger ledger = BurnCarrier.read(target);
        if (ledger.trackCount() >= FireLimits.PLAN_CAP
                || ledger.duplicate(cast.castId(), target.getUUID(), effectKey)) {
            return Optional.empty();
        }
        int remainder = balance - tiers * 10;
        boolean mounted;
        if (openingDue) {
            mounted = ledger.admitSharedOpening(effectKey, spec, cast.castId(), cast.casterId(), target.getUUID(),
                    balance, remainder, furnaceBonus, 0L);
        } else if (rotateRemainder) {
            mounted = ledger.admitSharedRotating(effectKey, spec, cast.castId(), cast.casterId(), target.getUUID(),
                    balance, remainder, furnaceBonus, 0L);
        } else {
            mounted = ledger.admitShared(effectKey, spec, cast.castId(), cast.casterId(), target.getUUID(),
                    balance, remainder, furnaceBonus, 0L);
        }
        if (!mounted) {
            return Optional.empty();
        }
        BurnCarrier.write(target, ledger);
        FireVisualRelay.publish(target);
        return Optional.of(spec);
    }

    public static Optional<FireSeedSnapshot> detonate(LivingEntity target, FireCastContext cast, int requested,
                                                      StrikePlanner planner) {
        if (!hostReady(target, cast) || planner == null || requested < 0) {
            return Optional.empty();
        }
        double furnaceBonus = casterBonus(target, cast.casterId());
        ChosenStrike chosen = new ChosenStrike();
        Optional<FireSeedSnapshot> taken = FireSeedState.consumePrepared(target, cast, requested, furnaceBonus,
                snapshot -> holdStrike(planner, snapshot, chosen));
        if (taken.isEmpty() || chosen.component == null) {
            return Optional.empty();
        }
        BurnComponent struck = chosen.component;
        double rawDamage = FireLayer.combine(struck.base(), struck.resourceAboveOne(), taken.get().furnaceBonus());
        double settledDamage = SpecialBalance.settle(SpecialBalance.LIAOLI, rawDamage);
        FireDamage.strikeOnce(target, taken.get().casterId(), settledDamage, struck.base(), true);
        return taken;
    }

    private static Optional<SharedSpec> specify(SpecSource source, int balance, double furnaceBonus) {
        try {
            Optional<SharedSpec> specified = source.specify(balance, furnaceBonus);
            if (specified == null) {
                return Optional.empty();
            }
            return specified;
        } catch (RuntimeException failure) {
            return Optional.empty();
        }
    }

    private static boolean holdStrike(StrikePlanner planner, FireSeedSnapshot snapshot, ChosenStrike chosen) {
        BurnComponent component = invokeStrike(planner, snapshot);
        if (!componentReady(component, snapshot.furnaceBonus())) {
            return false;
        }
        chosen.component = component;
        return true;
    }

    private static boolean hostReady(LivingEntity target, FireCastContext cast) {
        if (target == null || cast == null || cast.castId() == null) {
            return false;
        }
        if (!target.isAlive() || target.isSpectator() || target.isRemoved() || target.level().isClientSide()) {
            return false;
        }
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return serverLevel.getServer().isSameThread();
    }

    private static double casterBonus(LivingEntity target, UUID casterId) {
        ServerPlayer caster = OnlineCaster.find(target, casterId);
        if (caster == null) {
            return 0.0d;
        }
        double queried = FurnaceService.fireBonus(caster);
        if (!Double.isFinite(queried) || queried < 0.0d) {
            return 0.0d;
        }
        return queried;
    }

    private static boolean componentReady(BurnComponent component, double furnaceBonus) {
        if (component == null) {
            return false;
        }
        return FireLayer.acceptableDamage(FireLayer.combine(component.base(), component.resourceAboveOne(), furnaceBonus));
    }

    private static BurnComponent invokeStrike(StrikePlanner planner, FireSeedSnapshot snapshot) {
        try {
            Optional<BurnComponent> planned = planner.component(snapshot.amount(), snapshot.tiers(), snapshot.remainder());
            if (planned == null) {
                return null;
            }
            return planned.orElse(null);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static final class ChosenStrike {
        private BurnComponent component;
    }
}
