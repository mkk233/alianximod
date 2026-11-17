package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LivingEntity.hurt 的两条 actuallyHurt 互斥，描述符相同，都要包住。
 * 原方法仍调用一次。setHealth 返回后才记扣血完成，不改写生命值。
 * 玩家覆盖了 actuallyHurt，不会走进这里的 setHealth。
 */
@Mixin(LivingEntity.class)
public abstract class LivingDomainDamageCommitMixin {
    @Shadow
    protected abstract void actuallyHurt(DamageSource damageSource, float amount);

    @Redirect(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"
            ),
            require = 2
    )
    private void xuexiTrackActuallyHurt(LivingEntity living, DamageSource damageSource, float amount) {
        boolean entered = TechniqueHits.enterActuallyHurt(living, damageSource);
        try {
            this.actuallyHurt(damageSource, amount);
        } finally {
            TechniqueHits.exitActuallyHurt(entered);
        }
    }

    @Inject(
            method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void xuexiAfterLivingHealthCommit(DamageSource damageSource, float amount, CallbackInfo callback) {
        TechniqueHits.noteHealthCommitted((LivingEntity) (Object) this, damageSource);
    }

    @Inject(
            method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V",
                    shift = At.Shift.BEFORE
            ),
            require = 2
    )
    private void xuexiBeforeLivingAbsorption(DamageSource damageSource, float amount, CallbackInfo callback) {
        TechniqueHits.observeAbsorptionBefore((LivingEntity) (Object) this, damageSource);
    }

    @Inject(
            method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V",
                    shift = At.Shift.AFTER
            ),
            require = 2
    )
    private void xuexiAfterLivingAbsorption(DamageSource damageSource, float amount, CallbackInfo callback) {
        TechniqueHits.observeAbsorptionAfter((LivingEntity) (Object) this, damageSource);
    }
}
