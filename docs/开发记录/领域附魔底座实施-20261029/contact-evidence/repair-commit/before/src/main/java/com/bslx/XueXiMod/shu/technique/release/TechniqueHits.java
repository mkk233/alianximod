package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.enchant.TideMarkDamageScope;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayDeque;

/**
 * 术式伤害。同一段释放里的多段命中都全额落下，不把目标对其他伤害的无敌时间清掉。
 */
public final class TechniqueHits {
    private static final Field LAST_HURT = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20898_");
    private static final ThreadLocal<ArrayDeque<AppliedWatch>> APPLIED_WATCHES = new ThreadLocal<>();
    private static volatile boolean listenerReady;

    private TechniqueHits() {
    }

    public static boolean hurt(LivingEntity target, ServerPlayer player, float amount) {
        return hurt(target, player, amount, Landing.BYPASS_INVULNERABILITY);
    }

    /** 攻击术式的根归账。进入 hurt 之前并上当时的领域供给。 */
    public static boolean hurtRoot(LivingEntity target, ServerPlayer player, float amount) {
        return hurtRoot(target, player, amount, TideMarkSuccess.NONE);
    }

    public static boolean hurtRoot(LivingEntity target, ServerPlayer player, float amount, TideMarkSuccess tide) {
        float merged = com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver.merge(
                amount, player, com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext.Kind.ROOT_TECHNIQUE);
        return hurt(target, player, merged, Landing.BYPASS_INVULNERABILITY, tide);
    }

    public static boolean hurt(LivingEntity target, ServerPlayer player, float amount, Landing landing) {
        return hurt(target, player, amount, landing, TideMarkSuccess.NONE);
    }

    public static boolean hurt(LivingEntity target, ServerPlayer player, float amount, Landing landing, TideMarkSuccess tide) {
        if (amount <= 0f || target == null || player == null) {
            return false;
        }
        TideMarkDamageScope.Token token = TideMarkDamageScope.open(target, player, tide);
        boolean landed = false;
        try {
            if (landing == Landing.RESPECT_INVULNERABILITY) {
                landed = target.hurt(target.level().damageSources().playerAttack(player), amount);
            } else {
                int savedTime = target.invulnerableTime;
                float savedHurt = readLastHurt(target);
                target.invulnerableTime = 0;
                try {
                    landed = target.hurt(target.level().damageSources().playerAttack(player), amount);
                } finally {
                    target.invulnerableTime = Math.max(savedTime, target.invulnerableTime);
                    if (savedTime > 10) {
                        writeLastHurt(target, Math.max(savedHurt, readLastHurt(target)));
                    }
                }
            }
        } finally {
            TideMarkDamageScope.finish(token, landed);
        }
        return landed;
    }

    /**
     * 持续接触的一次确认。只走原版无敌帧，不把无敌时间清掉。
     * 返回的是最终正伤且事件没有取消，不只看 hurt 自己的布尔值。领域附加由调用方合并一次。
     */
    public static boolean hurtConfirmed(LivingEntity target, ServerPlayer player, float amount, Landing landing,
                                        TideMarkSuccess tide) {
        if (amount <= 0f || target == null || player == null || landing != Landing.RESPECT_INVULNERABILITY) {
            return false;
        }
        TideMarkSuccess contribution = tide == null ? TideMarkSuccess.NONE : tide;
        TideMarkDamageScope.Token token = TideMarkDamageScope.watch(target, player, contribution);
        boolean landed = false;
        boolean precise = false;
        try {
            landed = target.hurt(target.level().damageSources().playerAttack(player), amount);
        } finally {
            precise = TideMarkDamageScope.finish(token, landed);
        }
        return precise;
    }

    /**
     * 和 {@link #hurtConfirmed} 同一条正伤确认。正常返回时布尔值不变。
     * 调用中途抛出时，结果对象仍记下这次是否已经走过扣血后的 ENTITY_DAMAGE。
     * 其他调用者继续用 {@link #hurtConfirmed}，不会推入这个监听。
     */
    public static boolean confirmTracked(LivingEntity target, ServerPlayer player, float amount, Landing landing,
                                         TideMarkSuccess tide, ConfirmedDamage outcome) {
        if (outcome == null) {
            return hurtConfirmed(target, player, amount, landing, tide);
        }
        if (amount <= 0f || target == null || player == null || landing != Landing.RESPECT_INVULNERABILITY) {
            outcome.record(false, false);
            return false;
        }
        ensureAppliedListener();
        AppliedWatch watch = new AppliedWatch(target);
        appliedWatches().addLast(watch);
        boolean accepted = false;
        try {
            accepted = hurtConfirmed(target, player, amount, landing, tide);
            return accepted;
        } finally {
            outcome.record(accepted, watch.healthApplied);
            ArrayDeque<AppliedWatch> watches = APPLIED_WATCHES.get();
            if (watches != null) {
                watches.remove(watch);
                if (watches.isEmpty()) {
                    APPLIED_WATCHES.remove();
                }
            }
        }
    }

    private static void ensureAppliedListener() {
        if (listenerReady) {
            return;
        }
        synchronized (TechniqueHits.class) {
            if (listenerReady) {
                return;
            }
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, VanillaGameEvent.class,
                    TechniqueHits::noteHealthApplied);
            listenerReady = true;
        }
    }

    /**
     * 玩家和其它生物的 actuallyHurt 都是 setHealth 返回之后才 gameEvent(ENTITY_DAMAGE)。
     * 这个事件发出时，本次非零伤害的扣血已经返回。LivingDamageEvent 还在扣血之前，不能当作已生效。
     */
    private static void noteHealthApplied(VanillaGameEvent event) {
        if (event == null || event.getVanillaEvent() != GameEvent.ENTITY_DAMAGE) {
            return;
        }
        ArrayDeque<AppliedWatch> watches = APPLIED_WATCHES.get();
        if (watches == null || watches.isEmpty()) {
            return;
        }
        AppliedWatch watch = watches.peekLast();
        Entity cause = event.getCause();
        if (watch.target == cause) {
            watch.healthApplied = true;
        }
    }

    private static ArrayDeque<AppliedWatch> appliedWatches() {
        ArrayDeque<AppliedWatch> watches = APPLIED_WATCHES.get();
        if (watches == null) {
            watches = new ArrayDeque<>();
            APPLIED_WATCHES.set(watches);
        }
        return watches;
    }

    private static float readLastHurt(LivingEntity target) {
        try {
            return LAST_HURT.getFloat(target);
        } catch (IllegalAccessException exception) {
            return 0f;
        }
    }

    private static void writeLastHurt(LivingEntity target, float value) {
        try {
            LAST_HURT.setFloat(target, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    /** 旧攻击绕过无敌帧。接触附魔必须用 RESPECT_INVULNERABILITY。 */
    public enum Landing {
        BYPASS_INVULNERABILITY,
        RESPECT_INVULNERABILITY
    }

    /** 一次确认调用的结果。正常路径看 accepted；异常退出后还可看 healthApplied。 */
    public static final class ConfirmedDamage {
        private boolean accepted;
        private boolean healthApplied;

        public boolean accepted() {
            return accepted;
        }

        /** 原版已经在 setHealth 之后发出本次目标的 ENTITY_DAMAGE。 */
        public boolean healthApplied() {
            return healthApplied;
        }

        private void record(boolean acceptedResult, boolean appliedHealth) {
            this.accepted = acceptedResult;
            this.healthApplied = appliedHealth;
        }
    }

    private static final class AppliedWatch {
        private final LivingEntity target;
        private boolean healthApplied;

        private AppliedWatch(LivingEntity target) {
            this.target = target;
        }
    }
}
