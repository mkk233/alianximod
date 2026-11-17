package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantNumbers;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.FireSeedState;
import com.bslx.XueXiMod.carrier.BatchTriggerVisuals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 引爆只花当前共享余额。伤害是 0.2 × 余额 × (1 + 当时炉心热量)，资源增量是 0。
 * 去重键是 fire_detonate，不调用会写下跨效果禁产种标记的旧消费。
 * 打出的火伤帧已经标成炉心结算过，避免通用火伤再加一次热量。
 */
public final class DetonatePlan {
    private DetonatePlan() {
    }

    public static double damage(int balance, double furnaceBonus) {
        if (balance <= 0 || !Double.isFinite(furnaceBonus) || furnaceBonus < 0.0d) {
            return 0.0d;
        }
        return ResourceEnchantNumbers.DETONATE_FACTOR * (double) balance * (1.0d + furnaceBonus);
    }

    public static Outcome strike(LivingEntity target, FireCastContext cast) {
        return strike(target, cast, null);
    }

    /**
     * {@code hitAnchor} 只在调用方已经有载体接触点时传入。为空表示没有碰撞点，不在这里补一格高度。
     */
    public static Outcome strike(LivingEntity target, FireCastContext cast, Vec3 hitAnchor) {
        if (!ready(target, cast)) {
            return Outcome.notReady();
        }
        UUID targetId = target.getUUID();
        int balance = FireSeedState.value(target);
        double furnaceBonus = FireConsumption.frozenFurnaceBonus(target, cast.casterId());
        double planned = damage(balance, furnaceBonus);
        double settledDamage = 0.0d;
        if (balance > 0) {
            if (!Double.isFinite(planned) || planned <= 0.0d) {
                return Outcome.notReady();
            }
            Optional<Double> settled = SpecialBalance.settleConfigured(ResourceEffectKeys.DETONATE, planned);
            if (settled.isEmpty() || !FireLayer.acceptableSettled(settled.get())) {
                return Outcome.notReady();
            }
            settledDamage = settled.get();
        }
        if (!cast.claimEffect(targetId, ResourceEffectKeys.DETONATE)) {
            return Outcome.duplicate();
        }
        if (balance <= 0) {
            return Outcome.emptyBalance();
        }
        int spent = FireSeedState.draw(target, balance);
        double fireBase = ResourceEnchantNumbers.DETONATE_FACTOR * (double) balance;
        boolean struck = FireDamage.strikeOnce(target, cast.casterId(), settledDamage, fireBase, true);
        if (struck) {
            showSpent(target, spent, hitAnchor);
            return Outcome.struck(spent, settledDamage);
        }
        showSpent(target, spent, hitAnchor);
        return Outcome.spentUnstruck(spent, settledDamage);
    }

    /**
     * 只转发本次已经记下的消费。0、重复和未就绪不会到这里，也不会再次扣火种。
     * 有限的接触点按原坐标冻结。没有接触点时才用目标当前位置，那不是碰撞位置，也不另加 Y。
     */
    private static void showSpent(LivingEntity target, int spent, Vec3 hitAnchor) {
        if (spent <= 0 || target == null || !(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 frozenAnchor = finitePoint(hitAnchor);
        if (frozenAnchor == null) {
            frozenAnchor = finitePoint(target.position());
        }
        if (frozenAnchor == null) {
            return;
        }
        BatchTriggerVisuals.detonate(serverLevel, frozenAnchor, spent);
    }

    private static Vec3 finitePoint(Vec3 point) {
        if (point == null || !Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)) {
            return null;
        }
        return point;
    }

    private static boolean ready(LivingEntity target, FireCastContext cast) {
        if (target == null || cast == null || cast.castId() == null || target.getUUID() == null) {
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

    public static final class Outcome {
        public enum Kind {
            NOT_READY,
            DUPLICATE,
            EMPTY,
            STRUCK,
            SPENT_UNSTRUCK
        }

        private final Kind kind;
        private final int spent;
        private final double damage;

        private Outcome(Kind kind, int spent, double damage) {
            this.kind = kind;
            this.spent = spent;
            this.damage = damage;
        }

        static Outcome notReady() {
            return new Outcome(Kind.NOT_READY, 0, 0.0d);
        }

        static Outcome duplicate() {
            return new Outcome(Kind.DUPLICATE, 0, 0.0d);
        }

        static Outcome emptyBalance() {
            return new Outcome(Kind.EMPTY, 0, 0.0d);
        }

        static Outcome struck(int spent, double damage) {
            return new Outcome(Kind.STRUCK, spent, damage);
        }

        static Outcome spentUnstruck(int spent, double damage) {
            return new Outcome(Kind.SPENT_UNSTRUCK, spent, damage);
        }

        public Kind kind() {
            return this.kind;
        }

        public int spent() {
            return this.spent;
        }

        public double damage() {
            return this.damage;
        }

        public boolean struck() {
            return this.kind == Kind.STRUCK;
        }
    }
}
