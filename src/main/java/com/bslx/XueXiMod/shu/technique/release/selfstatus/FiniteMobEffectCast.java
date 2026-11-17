package com.bslx.XueXiMod.shu.technique.release.selfstatus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 给玩家挂一个已经注册的有限药水效果。时长和等级由调用方传入，这里不规定唯一时长。
 * 不登记花谱效果，也不管理原版在更高阶时藏起来的旧等级。
 */
public final class FiniteMobEffectCast implements SelfStatusCast {
    public static final String REJECTED_TEXT = "§c自身状态没有改变";
    /** 客户端效果包把等级收成一个字节，超过后会被截断。 */
    public static final int HIGHEST_SYNCED_AMPLIFIER = 255;

    private final MobEffect effect;
    private final int durationTicks;
    private final int amplifier;
    private final PresencePolicy policy;

    public FiniteMobEffectCast(MobEffect effect, int durationTicks, int amplifier, PresencePolicy policy) {
        this.effect = effect;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.policy = policy;
    }

    /** 要安装的药水效果。 */
    public MobEffect effect() {
        return this.effect;
    }

    /** 调用方传入的有限时长。 */
    public int durationTicks() {
        return this.durationTicks;
    }

    /** 调用方传入的效果等级。 */
    public int amplifier() {
        return this.amplifier;
    }

    /** 已有同类效果时的处理方式。 */
    public PresencePolicy policy() {
        return this.policy;
    }

    /** 预检玩家、效果、时长和等级。不安装、不扣款。 */
    @Override
    public StatusPreparation prepare(ServerPlayer player) {
        String rejectionText = blockReason(player);
        if (rejectionText != null) {
            return StatusPreparation.reject(rejectionText);
        }
        return StatusPreparation.ready(new FinitePrepared(this));
    }

    private String blockReason(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return REJECTED_TEXT;
        }
        if (!effectAccepted() || !durationAccepted() || !amplifierAccepted() || this.policy == null) {
            return REJECTED_TEXT;
        }
        MobEffectInstance current = player.getEffect(this.effect);
        if (this.policy == PresencePolicy.REJECT_IF_PRESENT) {
            return current == null ? null : REJECTED_TEXT;
        }
        if (this.policy == PresencePolicy.REFRESH_SAME_LEVEL) {
            if (current == null) {
                return null;
            }
            if (current.isInfiniteDuration() || current.getAmplifier() != this.amplifier) {
                return REJECTED_TEXT;
            }
            if (current.getDuration() >= this.durationTicks) {
                return REJECTED_TEXT;
            }
            return null;
        }
        return REJECTED_TEXT;
    }

    private boolean effectAccepted() {
        return this.effect != null && ForgeRegistries.MOB_EFFECTS.getKey(this.effect) != null;
    }

    private boolean durationAccepted() {
        return this.durationTicks > 0 && this.durationTicks != MobEffectInstance.INFINITE_DURATION;
    }

    private boolean amplifierAccepted() {
        return this.amplifier >= 0 && this.amplifier <= HIGHEST_SYNCED_AMPLIFIER;
    }

    private boolean matchesRequest(ServerPlayer player) {
        MobEffectInstance applied = player.getEffect(this.effect);
        if (applied == null || applied.isInfiniteDuration() || applied.getAmplifier() != this.amplifier) {
            return false;
        }
        if (applied.getDuration() != this.durationTicks) {
            return false;
        }
        ResourceLocation appliedKey = ForgeRegistries.MOB_EFFECTS.getKey(applied.getEffect());
        ResourceLocation expectedKey = ForgeRegistries.MOB_EFFECTS.getKey(this.effect);
        return appliedKey != null && appliedKey.equals(expectedKey);
    }

    private boolean install(ServerPlayer player) {
        if (blockReason(player) != null) {
            return false;
        }
        boolean alreadyPresent = player.hasEffect(this.effect);
        MobEffectInstance incoming = new MobEffectInstance(this.effect, this.durationTicks, this.amplifier);
        boolean added = player.addEffect(incoming);
        if (added && matchesRequest(player)) {
            return true;
        }
        if (!alreadyPresent && player.hasEffect(this.effect)) {
            player.removeEffect(this.effect);
        }
        return false;
    }

    /**
     * 已有同一种效果时怎么处理。只看当前这一份实例的等级，不读写隐藏等级链。
     * 同级刷新只在新时长比剩余更长时交给 {@code addEffect}；相同或更短视为没有变化。
     */
    public enum PresencePolicy {
        /** 已经有同类效果就拒绝。 */
        REJECT_IF_PRESENT,
        /** 只在同级且新时长更长时允许刷新。 */
        REFRESH_SAME_LEVEL
    }

    private static final class FinitePrepared implements PreparedStatusChange {
        private final FiniteMobEffectCast cast;

        private FinitePrepared(FiniteMobEffectCast cast) {
            this.cast = cast;
        }

        @Override
        public String recheck(ServerPlayer player) {
            return this.cast.blockReason(player);
        }

        @Override
        public boolean commit(ServerPlayer player) {
            return this.cast.install(player);
        }

        @Override
        public void afterCommit(ServerPlayer player) {
        }

        @Override
        public String failureText() {
            return REJECTED_TEXT;
        }
    }
}
