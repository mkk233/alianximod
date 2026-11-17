package com.bslx.XueXiMod.status.fire.furnace;

import com.bslx.XueXiMod.mixin.FurnaceEffectAccess;
import com.bslx.XueXiMod.shu.technique.release.fire.SteamBurst;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 炉心查询与转换。运行态在玩家持久数据，效果和属性由炉心事件维护。
 * 这里只提供热量、产种倍率和水冰倍率，不读写火种余额。
 * 火伤和水冰承伤由火伤作者在伤害事件里调用查询，这里不改那些结算。
 */
public final class FurnaceService {
    private FurnaceService() {
    }

    /** 当前炉心热量。非玩家、不在服务端线程或没有炉心时是 0。 */
    public static double fireBonus(LivingEntity entity) {
        FurnaceStore.Snapshot snapshot = serverSnapshot(entity);
        return snapshot.tier() == null ? 0.0D : snapshot.bonus();
    }

    /** 当前炉心给出的火种生产倍率。没有炉心时是 1，这里不改火种余额。 */
    public static double seedMultiplier(LivingEntity entity) {
        FurnaceStore.Snapshot snapshot = serverSnapshot(entity);
        return FurnaceMath.seedMultiplier(snapshot.tier(), snapshot.bonus());
    }

    /** 服务端线程上的玩家是不是有炉心。 */
    public static boolean active(LivingEntity entity) {
        return serverSnapshot(entity).tier() != null;
    }

    /** 当前档位的水冰承伤倍率。没有炉心时是 1。 */
    public static double waterIceMultiplier(LivingEntity entity) {
        FurnaceStore.Snapshot snapshot = serverSnapshot(entity);
        return FurnaceMath.waterIceMultiplier(snapshot.tier());
    }

    /** 玩家身上有效果时，自身抗性提升不算减伤。非玩家、没有炉心效果时不干预。 */
    public static boolean suppressesOwnResistance(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        return FurnaceMobEffect.isFurnace(player.getEffect(FurnaceMobEffect.effect()));
    }

    /**
     * 只预览转换，不调用进入、升级或冷却。
     * 与正式入口一样会先对账；损坏档的修复是既有读路径。
     */
    public static FurnaceChange preview(Player player, FurnaceMath.Action action) {
        FurnaceChange rejected = gate(player);
        if (rejected != null) {
            return rejected;
        }
        if (action == null) {
            return new FurnaceChange(FurnaceMath.Outcome.REJECT_NULL);
        }
        FurnaceRuntime.reconcile(player);
        FurnaceStore.Snapshot snapshot = FurnaceStore.snapshot(player);
        FurnaceMath.Transition transition = FurnaceMath.transition(
                snapshot.tier(), snapshot.bonus(), snapshot.lastTick(), action);
        return new FurnaceChange(transition.outcome());
    }

    /** 进入普通炉心。 */
    public static FurnaceChange enterNormal(Player player) {
        return enter(player, FurnaceMath.Action.ENTER_NORMAL, 0);
    }

    /** 进入过载；已经是普通时按转换规则升级。 */
    public static FurnaceChange enterOverdrive(Player player) {
        return enter(player, FurnaceMath.Action.ENTER_OVERDRIVE, 1);
    }

    /** 去掉炉心效果、运行态和属性。不释放蒸汽。 */
    public static FurnaceChange endPassive(Player player) {
        FurnaceChange rejected = gate(player);
        if (rejected != null) {
            return rejected;
        }
        FurnaceRuntime.reconcile(player);
        FurnaceStore.Snapshot snapshot = FurnaceStore.snapshot(player);
        FurnaceMath.Transition transition = FurnaceMath.transition(
                snapshot.tier(), snapshot.bonus(), snapshot.lastTick(), FurnaceMath.Action.END);
        if (!transition.outcome().succeeded()) {
            return new FurnaceChange(transition.outcome());
        }
        drop(player);
        return new FurnaceChange(FurnaceMath.Outcome.ENDED);
    }

    /** 先被动结束，成功后再释放蒸汽。 */
    public static FurnaceChange cool(Player player) {
        FurnaceChange ended = endPassive(player);
        if (!ended.succeeded()) {
            return ended;
        }
        SteamBurst.release(player);
        return new FurnaceChange(FurnaceMath.Outcome.COOLED);
    }

    private static FurnaceChange enter(Player player, FurnaceMath.Action action, int amplifier) {
        FurnaceChange rejected = gate(player);
        if (rejected != null) {
            return rejected;
        }
        FurnaceRuntime.reconcile(player);
        FurnaceStore.Snapshot snapshot = FurnaceStore.snapshot(player);
        FurnaceMath.Transition transition = FurnaceMath.transition(
                snapshot.tier(), snapshot.bonus(), snapshot.lastTick(), action);
        if (!transition.outcome().succeeded()) {
            return new FurnaceChange(transition.outcome());
        }
        MobEffectInstance incoming = FurnaceMobEffect.infinite(amplifier);
        if (!FurnaceInstall.accepted(FurnaceMobEffect.isFurnace(incoming), incoming.getAmplifier(),
                incoming.isInfiniteDuration(), amplifier)) {
            return new FurnaceChange(FurnaceMath.Outcome.REJECT_BLOCKED);
        }
        CompoundTag storeBefore = FurnaceStore.copyTag(player);
        MobEffectInstance effectBefore = player.getEffect(FurnaceMobEffect.effect());
        int durationBefore = effectBefore == null ? 0 : effectBefore.getDuration();
        int amplifierBefore = effectBefore == null ? 0 : effectBefore.getAmplifier();
        try {
            player.forceAddEffect(incoming, player);
            MobEffectInstance applied = player.getEffect(FurnaceMobEffect.effect());
            boolean furnace = FurnaceMobEffect.isFurnace(applied);
            int appliedAmplifier = applied == null ? 0 : applied.getAmplifier();
            boolean infinite = applied != null && applied.isInfiniteDuration();
            FurnaceInstall.Plan plan = FurnaceInstall.plan(furnace, appliedAmplifier, infinite, amplifier,
                    effectBefore != null, applied == effectBefore, applied != null);
            if (plan == FurnaceInstall.Plan.ACCEPT) {
                return new FurnaceChange(transition.outcome());
            }
            restore(player, effectBefore, durationBefore, amplifierBefore, storeBefore, plan);
            return new FurnaceChange(FurnaceMath.Outcome.REJECT_BLOCKED);
        } catch (RuntimeException failure) {
            FurnaceInstall.Plan undo = effectBefore == null
                    ? FurnaceInstall.Plan.REMOVE_CURRENT
                    : FurnaceInstall.Plan.REINSTALL_PREVIOUS;
            restore(player, effectBefore, durationBefore, amplifierBefore, storeBefore, undo);
            throw failure;
        }
    }

    /** 只放回进入前的效果和热量。不走冷却，也不走被动结束。 */
    private static void restore(Player player, MobEffectInstance effectBefore, int durationBefore,
                                int amplifierBefore, CompoundTag storeBefore, FurnaceInstall.Plan plan) {
        if (effectBefore != null && plan != FurnaceInstall.Plan.REMOVE_CURRENT) {
            FurnaceEffectAccess access = (FurnaceEffectAccess) (Object) effectBefore;
            access.xuexiSetDuration(durationBefore);
            access.xuexiSetAmplifier(amplifierBefore);
            player.forceAddEffect(effectBefore, player);
        } else if (plan == FurnaceInstall.Plan.REMOVE_CURRENT) {
            player.removeEffect(FurnaceMobEffect.effect());
        }
        FurnaceStore.replaceTag(player, storeBefore);
    }

    /** 去掉炉心效果、运行态和属性。 */
    private static void drop(Player player) {
        if (player.hasEffect(FurnaceMobEffect.effect())) {
            player.removeEffect(FurnaceMobEffect.effect());
        }
        if (FurnaceStore.present(player)) {
            FurnaceStore.clear(player);
        }
        if (FurnaceAttributes.wearing(player)) {
            FurnaceAttributes.detach(player);
        }
    }

    /** 空玩家、客户端或非服务器线程直接拒绝。 */
    private static FurnaceChange gate(Player player) {
        if (player == null) {
            return new FurnaceChange(FurnaceMath.Outcome.REJECT_NULL);
        }
        if (player.level().isClientSide()) {
            return new FurnaceChange(FurnaceMath.Outcome.REJECT_CLIENT);
        }
        if (!FurnaceThreadGate.onServerThread(player)) {
            return new FurnaceChange(FurnaceMath.Outcome.REJECT_THREAD);
        }
        return null;
    }

    /** 先对账再读运行态。不是服务端线程上的玩家时视为没有炉心。 */
    private static FurnaceStore.Snapshot serverSnapshot(LivingEntity entity) {
        if (!(entity instanceof Player player) || !FurnaceThreadGate.onServerThread(player)) {
            return FurnaceStore.Snapshot.inactive();
        }
        FurnaceRuntime.reconcile(player);
        return FurnaceStore.snapshot(player);
    }
}
