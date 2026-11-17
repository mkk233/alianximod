package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.prison.PrisonMotion;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonRandomGate;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonRuntime;
import com.bslx.XueXiMod.registry.ModMobEffects;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 跳跃、击退、随机传送和湿度移除。随机传送在方法头包装，异常也离开作用域。 */
@Mixin(LivingEntity.class)
public abstract class PrisonLivingMixin {
    @Inject(method = "jumpFromGround()V", at = @At("HEAD"), cancellable = true)
    private void xuexiPrisonJump(CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(ModMobEffects.ROSE_BIND.get())) {
            callback.cancel();
            return;
        }
        if (!PrisonMotion.locks((Entity) (Object) this)) {
            return;
        }
        ((Entity) (Object) this).setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        callback.cancel();
    }

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void xuexiPrisonKnockback(double strength, double ratioX, double ratioZ, CallbackInfo callback) {
        if (!PrisonMotion.locks((Entity) (Object) this)) {
            return;
        }
        ((Entity) (Object) this).setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        callback.cancel();
    }

    @Inject(method = "jumpInLiquid(Lnet/minecraft/tags/TagKey;)V", at = @At("HEAD"), cancellable = true)
    private void xuexiPrisonJumpLiquid(TagKey<Fluid> fluidTag, CallbackInfo callback) {
        if (!PrisonMotion.locks((Entity) (Object) this)) {
            return;
        }
        ((Entity) (Object) this).setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        callback.cancel();
    }

    @Inject(method = "randomTeleport(DDDZ)Z", at = @At("HEAD"), cancellable = true)
    private void xuexiPrisonRandom(double targetX, double targetY, double targetZ, boolean broadcastTeleport,
                                   CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (PrisonRandomGate.consume(self)) {
            return;
        }
        callback.setReturnValue(PrisonRandomGate.wrap(self, targetX, targetY, targetZ, broadcastTeleport));
    }

    @Inject(method = "tickEffects()V", at = @At("HEAD"))
    private void xuexiPrisonEffectsEnter(CallbackInfo callback) {
        com.bslx.XueXiMod.status.AttributeStateService.enterEffectPass();
    }

    @Inject(method = "tickEffects()V", at = @At("RETURN"))
    private void xuexiPrisonEffectsLeave(CallbackInfo callback) {
        com.bslx.XueXiMod.status.AttributeStateService.leaveEffectPass();
    }

    @Inject(method = "onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("HEAD"))
    private void xuexiPrisonEffectRemoved(MobEffectInstance instance, CallbackInfo callback) {
        PrisonRuntime.onWetRemoved((LivingEntity) (Object) this, instance);
    }

    @Redirect(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeHooks;onLivingDeath(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z",
                    remap = false))
    private boolean xuexiPrisonConfirmedDeath(LivingEntity entity, DamageSource source) {
        boolean cancelled = ForgeHooks.onLivingDeath(entity, source);
        if (!cancelled) {
            PrisonRuntime.onConfirmedDeath(entity);
        }
        return cancelled;
    }
}
