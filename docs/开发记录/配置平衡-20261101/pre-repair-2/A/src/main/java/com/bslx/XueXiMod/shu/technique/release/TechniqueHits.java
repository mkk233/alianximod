package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkDamageScope;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayDeque;

/**
 * 术式伤害。同一段释放里的多段命中都全额落下，不把目标对其他伤害的无敌时间清掉。
 */
public final class TechniqueHits {
    private static final Field LAST_HURT = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20898_");
    private static final ThreadLocal<ArrayDeque<AppliedWatch>> APPLIED_WATCHES = new ThreadLocal<>();
    private static final ThreadLocal<ArrayDeque<HurtFrame>> HURT_FRAMES = new ThreadLocal<>();

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
        long damageStart = PerformanceRecorder.begin();
        try {
            if (amount <= 0f) {
                return classifyNonPositive(target, player).continues();
            }
            if (target == null || player == null) {
                return false;
            }
            TideMarkDamageScope.Token token = TideMarkDamageScope.open(target, player, tide);
            boolean landed = false;
            try {
                if (landing == Landing.RESPECT_INVULNERABILITY) {
                    landed = target.hurt(target.level().damageSources().playerAttack(player), amount);
                    noteDamageOutcome(landed);
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
                    noteDamageOutcome(landed);
                }
            } finally {
                TideMarkDamageScope.finish(token, landed);
            }
            return landed;
        } finally {
            PerformanceRecorder.end(PerfMetric.DAMAGE_APPLY, damageStart);
        }
    }

    /**
     * 持续接触的一次确认。只走原版无敌帧，不把无敌时间清掉。
     * 返回的是最终正伤且事件没有取消，不只看 hurt 自己的布尔值。领域附加由调用方合并一次。
     */
    /**
     * 新效果用这一条。不绕过无敌帧。取消、免疫和正伤分开，不把 hurt 的布尔值当成全部。
     */
    public static HitVerdict classifyRespecting(LivingEntity target, ServerPlayer player, float amount, TideMarkSuccess tide) {
        if (target == null || player == null || !target.isAlive() || target.isRemoved() || target.isSpectator()
                || player.getUUID().equals(target.getUUID())) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        if (amount <= 0f) {
            return classifyNonPositive(target, player);
        }
        TideMarkSuccess contribution = tide == null ? TideMarkSuccess.NONE : tide;
        TideMarkDamageScope.Token token = TideMarkDamageScope.watch(target, player, contribution);
        boolean landed = false;
        TideMarkDamageScope.Reading reading;
        try {
            landed = target.hurt(target.level().damageSources().playerAttack(player), amount);
            noteDamageOutcome(landed);
        } finally {
            reading = TideMarkDamageScope.finishReading(token, landed);
        }
        if (reading.canceled()) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        if (reading.accepted()) {
            return HitVerdict.DAMAGE_ACCEPTED;
        }
        return HitVerdict.ESTABLISHED_NO_DAMAGE;
    }

    public static boolean hurtConfirmed(LivingEntity target, ServerPlayer player, float amount, Landing landing,
                                        TideMarkSuccess tide) {
        long damageStart = PerformanceRecorder.begin();
        try {
            if (amount <= 0f || target == null || player == null || landing != Landing.RESPECT_INVULNERABILITY) {
                return false;
            }
            TideMarkSuccess contribution = tide == null ? TideMarkSuccess.NONE : tide;
            TideMarkDamageScope.Token token = TideMarkDamageScope.watch(target, player, contribution);
            boolean landed = false;
            boolean precise = false;
            try {
                landed = target.hurt(target.level().damageSources().playerAttack(player), amount);
                noteDamageOutcome(landed);
            } finally {
                precise = TideMarkDamageScope.finish(token, landed);
            }
            return precise;
        } finally {
            PerformanceRecorder.end(PerfMetric.DAMAGE_APPLY, damageStart);
        }
    }

    /**
     * 已配置的非正数贡献。权限、无敌和不能伤害的玩家沿用 {@link DamagePermission}，不记成成立。
     * 合法目标不进入原版伤害，也不改无敌帧，结果是成立但无正伤。
     */
    private static HitVerdict classifyNonPositive(LivingEntity target, ServerPlayer player) {
        if (!DamagePermission.allows(player, target)) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        return HitVerdict.ESTABLISHED_NO_DAMAGE;
    }

    private static void noteDamageOutcome(boolean landed) {
        if (!PerformanceRecorder.isRecording()) {
            return;
        }
        PerformanceRecorder.add(PerfCounter.DAMAGE_ATTEMPTS, 1L);
        if (landed) {
            PerformanceRecorder.add(PerfCounter.DAMAGE_ACCEPTED, 1L);
        }
    }

    /**
     * 和 {@link #hurtConfirmed} 同一条正伤确认。正常返回时布尔值不变。
     * 调用中途抛出时，结果对象记下本次目标和来源是否已经从 setHealth 返回。
     * 没有活动跟踪时，hurt 上的包装直接调用原来的 actuallyHurt。
     */
    public static boolean confirmTracked(LivingEntity target, ServerPlayer player, float amount, Landing landing,
                                         TideMarkSuccess tide, ConfirmedDamage outcome) {
        if (outcome == null) {
            return hurtConfirmed(target, player, amount, landing, tide);
        }
        if (amount <= 0f || target == null || player == null || landing != Landing.RESPECT_INVULNERABILITY) {
            outcome.record(false, false, false);
            return false;
        }
        int savedFrames = hurtFrameCount();
        AppliedWatch watch = new AppliedWatch(target, player);
        appliedWatches().addLast(watch);
        boolean accepted = false;
        try {
            accepted = hurtConfirmed(target, player, amount, landing, tide);
            return accepted;
        } finally {
            outcome.record(accepted, watch.healthApplied, watch.absorptionReduced);
            while (hurtFrameCount() > savedFrames) {
                exitActuallyHurt(true);
            }
            ArrayDeque<AppliedWatch> watches = APPLIED_WATCHES.get();
            if (watches != null) {
                watches.remove(watch);
                if (watches.isEmpty()) {
                    APPLIED_WATCHES.remove();
                }
            }
        }
    }

    /**
     * 由 hurt 里的 actuallyHurt 包装在调用前进入。没有活动跟踪时不压栈。
     * 只把尚未绑定、或上一帧已经退出的最内层监视绑到当前帧。
     */
    public static boolean enterActuallyHurt(LivingEntity target, DamageSource damageSource) {
        ArrayDeque<AppliedWatch> watches = APPLIED_WATCHES.get();
        if (watches == null || watches.isEmpty() || target == null) {
            return false;
        }
        Entity attacker = damageSource == null ? null : damageSource.getEntity();
        ArrayDeque<HurtFrame> frames = hurtFrames();
        HurtFrame frame = new HurtFrame(target, attacker);
        frames.addLast(frame);
        AppliedWatch watch = watches.peekLast();
        if (watch.target == target && watch.attacker == attacker
                && (watch.boundFrame == null || !frames.contains(watch.boundFrame))) {
            watch.boundFrame = frame;
        }
        return true;
    }

    /** 包装的 finally 调用。entered 为假时不动栈。 */
    public static void exitActuallyHurt(boolean entered) {
        if (!entered) {
            return;
        }
        ArrayDeque<HurtFrame> frames = HURT_FRAMES.get();
        if (frames == null || frames.isEmpty()) {
            return;
        }
        frames.removeLast();
        if (frames.isEmpty()) {
            HURT_FRAMES.remove();
        }
    }

    /**
     * setHealth 返回之后由 mixin 调用。只给当前帧已绑定的最内层监视记扣血完成。
     * 不读取生命值差额，也不监听 ENTITY_DAMAGE。
     */
    public static void noteHealthCommitted(LivingEntity target, DamageSource damageSource) {
        AppliedWatch watch = matchedWatch(target, damageSource);
        if (watch == null) {
            return;
        }
        watch.healthApplied = true;
    }

    /** 本次吸收写入开始前记下当前值。没有匹配帧时不读、不记。 */
    public static void observeAbsorptionBefore(LivingEntity target, DamageSource damageSource) {
        AppliedWatch watch = matchedWatch(target, damageSource);
        if (watch == null) {
            return;
        }
        HurtFrame frame = watch.boundFrame;
        frame.previousAbsorption = target.getAbsorptionAmount();
        frame.absorptionPending = true;
    }

    /**
     * 本次 setAbsorptionAmount 已经返回后才比较。只有比写入前更小才记吸收已生效。
     * 等值、变大，或写入没有返回，都不记。
     */
    public static void observeAbsorptionAfter(LivingEntity target, DamageSource damageSource) {
        AppliedWatch watch = matchedWatch(target, damageSource);
        if (watch == null) {
            return;
        }
        HurtFrame frame = watch.boundFrame;
        if (!frame.absorptionPending) {
            return;
        }
        frame.absorptionPending = false;
        float writtenAbsorption = target.getAbsorptionAmount();
        if (writtenAbsorption < frame.previousAbsorption) {
            watch.absorptionReduced = true;
        }
    }

    private static AppliedWatch matchedWatch(LivingEntity target, DamageSource damageSource) {
        ArrayDeque<HurtFrame> frames = HURT_FRAMES.get();
        ArrayDeque<AppliedWatch> watches = APPLIED_WATCHES.get();
        if (frames == null || frames.isEmpty() || watches == null || watches.isEmpty() || target == null) {
            return null;
        }
        HurtFrame frame = frames.peekLast();
        AppliedWatch watch = watches.peekLast();
        Entity attacker = damageSource == null ? null : damageSource.getEntity();
        if (watch.boundFrame != frame || frame.target != target || frame.attacker != attacker
                || watch.target != target || watch.attacker != attacker) {
            return null;
        }
        return watch;
    }

    private static ArrayDeque<AppliedWatch> appliedWatches() {
        ArrayDeque<AppliedWatch> watches = APPLIED_WATCHES.get();
        if (watches == null) {
            watches = new ArrayDeque<>();
            APPLIED_WATCHES.set(watches);
        }
        return watches;
    }

    private static ArrayDeque<HurtFrame> hurtFrames() {
        ArrayDeque<HurtFrame> frames = HURT_FRAMES.get();
        if (frames == null) {
            frames = new ArrayDeque<>();
            HURT_FRAMES.set(frames);
        }
        return frames;
    }

    private static int hurtFrameCount() {
        ArrayDeque<HurtFrame> frames = HURT_FRAMES.get();
        if (frames == null) {
            return 0;
        }
        return frames.size();
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

    /** 一次确认调用的结果。正常路径看 accepted；异常退出后看生命提交或吸收实际减少。 */
    public static final class ConfirmedDamage {
        private boolean accepted;
        private boolean healthApplied;
        private boolean absorptionReduced;

        public boolean accepted() {
            return accepted;
        }

        /** 本次目标和来源的 setHealth 已经返回。早于 ENTITY_DAMAGE，也不看生命值差额。 */
        public boolean healthApplied() {
            return healthApplied;
        }

        /** 本次跟踪帧里有一次吸收写入已经返回，并且数值比该次写入前更小。 */
        public boolean absorptionReduced() {
            return absorptionReduced;
        }

        private void record(boolean acceptedResult, boolean appliedHealth, boolean reducedAbsorption) {
            this.accepted = acceptedResult;
            this.healthApplied = appliedHealth;
            this.absorptionReduced = reducedAbsorption;
        }
    }

    private static final class AppliedWatch {
        private final LivingEntity target;
        private final Entity attacker;
        private HurtFrame boundFrame;
        private boolean healthApplied;
        private boolean absorptionReduced;

        private AppliedWatch(LivingEntity target, Entity attacker) {
            this.target = target;
            this.attacker = attacker;
        }
    }

    private static final class HurtFrame {
        private final LivingEntity target;
        private final Entity attacker;
        private float previousAbsorption;
        private boolean absorptionPending;

        private HurtFrame(LivingEntity target, Entity attacker) {
            this.target = target;
            this.attacker = attacker;
        }
    }
}
