package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.enchant.WeaponEnchant;
import com.bslx.XueXiMod.shu.technique.release.deflection.melee.MeleeDeflection;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSpawnInheritance;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import com.bslx.XueXiMod.shu.technique.effects.SwordGuardEffect;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.wood.vine.VineMarkActions;
import com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 普通近战在进入主目标 hurt 之前加上领域供给。含水武器印记只加在这一刀上。 */
@Mixin(Player.class)
public class PlayerAttackMixin {
    @Inject(method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getFireAspect(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private void xuexiBeforePlayerFire(Entity target, CallbackInfo callback) {
        if ((Object) this instanceof LivingEntity attacker) {
            MeleeDeflection.arm(attacker, target, false);
        }
    }

    @ModifyArg(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            index = 1)
    private float xuexiDomainMelee(float amount) {
        Entity attacker = (Entity) (Object) this;
        if (!attacker.level().isClientSide() && attacker instanceof Player player) {
            amount = WeaponEnchant.includeTideFlat(player, amount);
        }
        return DomainAuraResolver.merge(amount, attacker, AttackEnchantmentContext.Kind.ROOT_MELEE);
    }

    @Redirect(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean xuexiMainHurt(Entity target, DamageSource source, float amount) {
        Entity attacker = (Entity) (Object) this;
        WeaponEnchant.SwingTide swing = WeaponEnchant.captureSwing(attacker, target);
        FireCastContext domainCast = attacker instanceof ServerPlayer player
                ? new FireCastContext(java.util.UUID.randomUUID(), player.getUUID()) : null;
        boolean landed = false;
        try {
            if (MeleeDeflection.cancelCommitted(attacker, target)
                    || SwordGuardEffect.parryActualMelee(attacker, target)) {
                landed = false;
            } else {
                landed = target.hurt(source, amount);
                VineMarkActions.Action vineAction = VineMarkActions.current();
                if (vineAction != null && target instanceof LivingEntity living && attacker instanceof ServerPlayer serverPlayer) {
                    VineMarkActions.notePrimary(living);
                    VineMarkRuntime.offerQualifiedHit(serverPlayer, living, vineAction.actionId(),
                            false, landed, vineAction.spectrumEligible());
                }
            }
        } finally {
            swing.finish(landed);
            if (attacker instanceof ServerPlayer player && target instanceof LivingEntity living) {
                DomainSpawnInheritance.grantVanilla(player, living, domainCast, landed);
            }
            MeleeDeflection.close(attacker, target);
        }
        return landed;
    }

    @Redirect(method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V"))
    private void xuexiSkipFriendlyIgnite(Entity target, int seconds) {
        if (!(target instanceof LivingEntity living) || !HarmPolicy.denies((Entity) (Object) this, living)) {
            target.setSecondsOnFire(seconds);
        }
    }

    @Inject(method = "attack", at = @At("RETURN"))
    private void xuexiPlayerAttackEnd(Entity target, CallbackInfo callback) {
        MeleeDeflection.close((Entity) (Object) this, target);
        VineMarkActions.close(VineMarkActions.current());
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void xuexiRoseAttackBegin(Entity target, CallbackInfo callback) {
        if ((Object) this instanceof ServerPlayer player && !player.level().isClientSide()) {
            VineMarkActions.openMelee(player);
        }
    }

    @ModifyArg(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            index = 1,
            require = 0)
    private float xuexiDomainSweep(float amount) {
        return DomainAuraResolver.merge(amount, (Entity) (Object) this, AttackEnchantmentContext.Kind.ROOT_MELEE);
    }

    @Redirect(method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V",
                    ordinal = 1),
            require = 0)
    private void xuexiSkipFriendlySweepKnockback(LivingEntity target, double strength, double ratioX, double ratioZ) {
        if (!HarmPolicy.denies((Entity) (Object) this, target)) {
            target.knockback(strength, ratioX, ratioZ);
        }
    }

    @Redirect(method = "attack",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            require = 0)
    private boolean xuexiSkipFriendlySweepHurt(LivingEntity target, DamageSource source, float amount) {
        if (HarmPolicy.denies((Entity) (Object) this, target)) {
            return false;
        }
        boolean landed = target.hurt(source, amount);
        VineMarkActions.Action action = VineMarkActions.current();
        if (landed && VineMarkActions.sweepArmed() && action != null
                && (Object) this instanceof ServerPlayer player) {
            VineMarkRuntime.offerQualifiedHit(player, target, action.actionId(), false, true,
                    action.spectrumEligible());
        }
        return landed;
    }
}
