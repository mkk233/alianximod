package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.enchant.TideMarkDamageScope;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * 术式伤害。同一段释放里的多段命中都全额落下，不把目标对其他伤害的无敌时间清掉。
 */
public final class TechniqueHits {
    private static final Field LAST_HURT = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20898_");

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
}
