package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkDamageScope;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
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

    /** 默认绕过无敌帧。返回值只表示正伤被接受，不记命中账本。 */
    public static boolean hurt(LivingEntity target, ServerPlayer player, float amount) {
        return classifyHit(target, player, amount).grantsSeeds();
    }

    /** 攻击术式的根归账。进入伤害之前并上当时的领域供给。布尔值只表示正伤被接受。 */
    public static boolean hurtRoot(LivingEntity target, ServerPlayer player, float amount) {
        return classifyRoot(target, player, amount).grantsSeeds();
    }

    public static boolean hurtRoot(LivingEntity target, ServerPlayer player, float amount, TideMarkSuccess tide) {
        return classifyRoot(target, player, amount, tide).grantsSeeds();
    }

    /** 调用方选择是否绕过无敌帧。不在这里合并领域供给。 */
    public static boolean hurt(LivingEntity target, ServerPlayer player, float amount, Landing landing) {
        return classifyHit(target, player, amount, landing).grantsSeeds();
    }

    /** 潮痕只随这次伤害记录。返回值不包含取消和免疫的区别。 */
    public static boolean hurt(LivingEntity target, ServerPlayer player, float amount, Landing landing, TideMarkSuccess tide) {
        return classifyHit(target, player, amount, landing, tide).grantsSeeds();
    }

    /** 不带落点选择时沿用绕过无敌帧。 */
    public static HitVerdict classifyHit(LivingEntity target, ServerPlayer player, float amount) {
        return classifyHit(target, player, amount, Landing.BYPASS_INVULNERABILITY);
    }

    /** 潮痕为空贡献。其余边界与带潮痕的入口相同。 */
    public static HitVerdict classifyHit(LivingEntity target, ServerPlayer player, float amount, Landing landing) {
        return classifyHit(target, player, amount, landing, TideMarkSuccess.NONE);
    }

    /**
     * 与 {@link #hurt(LivingEntity, ServerPlayer, float, Landing, TideMarkSuccess)} 同一条路径。
     * 有限且恰好为 0、权限成立时是成立但无正伤。负数、非有限和权限失败是无效。正伤仍走原来的无敌帧选择。
     */
    public static HitVerdict classifyHit(LivingEntity target, ServerPlayer player, float amount, Landing landing,
                                         TideMarkSuccess tide) {
        long damageStart = PerformanceRecorder.begin();
        try {
            if (!Float.isFinite(amount) || amount < 0f) {
                return HitVerdict.INVALID_OR_CANCELLED;
            }
            if (HarmPolicy.denies(player, target)) {
                return HitVerdict.INVALID_OR_CANCELLED;
            }
            if (amount == 0f) {
                return finiteZeroVerdict(target, player);
            }
            if (target == null || player == null) {
                return HitVerdict.INVALID_OR_CANCELLED;
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
            if (landed) {
                return HitVerdict.DAMAGE_ACCEPTED;
            }
            return HitVerdict.ESTABLISHED_NO_DAMAGE;
        } finally {
            PerformanceRecorder.end(PerfMetric.DAMAGE_APPLY, damageStart);
        }
    }

    /** 攻击术式的根归账。进入伤害之前并上当时的领域供给。 */
    public static HitVerdict classifyRoot(LivingEntity target, ServerPlayer player, float amount) {
        return classifyRoot(target, player, amount, TideMarkSuccess.NONE);
    }

    public static HitVerdict classifyRoot(LivingEntity target, ServerPlayer player, float amount, TideMarkSuccess tide) {
        float mergedAmount = com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver.merge(
                amount, player, com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext.Kind.ROOT_TECHNIQUE);
        return classifyHit(target, player, mergedAmount, Landing.BYPASS_INVULNERABILITY, tide);
    }

    /**
     * 新效果用这一条。不绕过无敌帧。取消、免疫和正伤分开。
     * 有限且恰好为 0 与 {@link #classifyHit} 同一边界：权限成立才是无正伤，负数和非有限是无效。
     */
    public static HitVerdict classifyRespecting(LivingEntity target, ServerPlayer player, float amount, TideMarkSuccess tide) {
        if (target == null || player == null || !target.isAlive() || target.isRemoved() || target.isSpectator()
                || player.getUUID().equals(target.getUUID())) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        if (!Float.isFinite(amount) || amount < 0f) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        if (HarmPolicy.denies(player, target)) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        if (amount == 0f) {
            return finiteZeroVerdict(target, player);
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

    /**
     * 持续接触的一次确认。只走原版无敌帧，不把无敌时间清掉。
     * 布尔值只表示最终正伤被接受。领域附加由调用方合并一次。
     * 只允许尊重无敌帧的确认进入原版伤害，其他落点选择直接无效。
     */
    public static boolean hurtConfirmed(LivingEntity target, ServerPlayer player, float amount, Landing landing,
                                        TideMarkSuccess tide) {
        return classifyConfirmed(target, player, amount, landing, tide).grantsSeeds();
    }

    /** 与 {@link #hurtConfirmed} 同一条确认。合法的恰好 0 不调用原版伤害。 */
    public static HitVerdict classifyConfirmed(LivingEntity target, ServerPlayer player, float amount, Landing landing,
                                               TideMarkSuccess tide) {
        long damageStart = PerformanceRecorder.begin();
        try {
            HitVerdict blocked = blockedConfirmation(target, player, amount, landing);
            if (blocked != null) {
                return blocked;
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
            if (precise) {
                return HitVerdict.DAMAGE_ACCEPTED;
            }
            return HitVerdict.ESTABLISHED_NO_DAMAGE;
        } finally {
            PerformanceRecorder.end(PerfMetric.DAMAGE_APPLY, damageStart);
        }
    }

    /**
     * 与 {@link #confirmTracked} 同一条跟踪。返回值是裁决，不是“可接续”布尔。
     * 没有结果对象时与 {@link #classifyConfirmed} 相同。
     */
    public static HitVerdict classifyConfirmed(LivingEntity target, ServerPlayer player, float amount, Landing landing,
                                               TideMarkSuccess tide, ConfirmedDamage outcome) {
        if (outcome == null) {
            return classifyConfirmed(target, player, amount, landing, tide);
        }
        HitVerdict blocked = blockedConfirmation(target, player, amount, landing);
        if (blocked != null) {
            outcome.record(false, false, false);
            return blocked;
        }
        int savedFrames = hurtFrameCount();
        AppliedWatch watch = new AppliedWatch(target, player);
        appliedWatches().addLast(watch);
        HitVerdict verdict = HitVerdict.INVALID_OR_CANCELLED;
        try {
            verdict = classifyConfirmed(target, player, amount, landing, tide);
            return verdict;
        } finally {
            outcome.record(verdict.grantsSeeds(), watch.healthApplied, watch.absorptionReduced);
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
     * 有限且恰好为 0 才区分权限。权限、无敌和不能伤害的玩家沿用 {@link DamagePermission}。
     * 合法目标不进入原版伤害，也不改无敌帧。
     */
    private static HitVerdict finiteZeroVerdict(LivingEntity target, ServerPlayer player) {
        if (!DamagePermission.allows(player, target)) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        return HitVerdict.ESTABLISHED_NO_DAMAGE;
    }

    /** 确认不能进入原版伤害时返回裁决。可以进入时返回 null。 */
    private static HitVerdict blockedConfirmation(LivingEntity target, ServerPlayer player, float amount, Landing landing) {
        if (!Float.isFinite(amount) || amount < 0f) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        if (amount == 0f) {
            return finiteZeroVerdict(target, player);
        }
        if (target == null || player == null || landing != Landing.RESPECT_INVULNERABILITY) {
            return HitVerdict.INVALID_OR_CANCELLED;
        }
        return null;
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
        return classifyConfirmed(target, player, amount, landing, tide, outcome).grantsSeeds();
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
