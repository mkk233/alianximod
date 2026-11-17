package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.deflection.melee.MeleeDeflection;
import com.bslx.XueXiMod.shu.technique.effects.SwordGuardEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 怪物近战在火焰附魔写入之前预留。取消时把着火刻退回预置前，不调用 clearFire。 */
@Mixin(Mob.class)
public abstract class MobMeleeDeflectionMixin {
    @Inject(method = "doHurtTarget",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getFireAspect(Lnet/minecraft/world/entity/LivingEntity;)I"))
    private void xuexiBeforeMobFire(Entity target, CallbackInfoReturnable<Boolean> callback) {
        MeleeDeflection.arm((LivingEntity) (Object) this, target, true);
    }

    @Redirect(method = "doHurtTarget",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean xuexiMobHurt(Entity target, DamageSource source, float amount) {
        LivingEntity attacker = (LivingEntity) (Object) this;
        try {
            if (MeleeDeflection.cancelCommitted(attacker, target)
                    || SwordGuardEffect.parryActualMelee(attacker, target)) {
                return false;
            }
            return target.hurt(source, amount);
        } finally {
            MeleeDeflection.close(attacker, target);
        }
    }

    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void xuexiMobEnd(Entity target, CallbackInfoReturnable<Boolean> callback) {
        MeleeDeflection.close((LivingEntity) (Object) this, target);
    }
}
