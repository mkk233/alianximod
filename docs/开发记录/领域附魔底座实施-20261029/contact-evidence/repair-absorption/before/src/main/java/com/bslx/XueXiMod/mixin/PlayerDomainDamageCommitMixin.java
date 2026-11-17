package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player.actuallyHurt 不调用 LivingEntity.actuallyHurt。
 * 字节码里这次 setHealth 的 owner 是 Player。返回后才记扣血完成，不改写生命值。
 */
@Mixin(Player.class)
public abstract class PlayerDomainDamageCommitMixin {
    @Inject(
            method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V",
                    shift = At.Shift.AFTER
            ),
            require = 1
    )
    private void xuexiAfterPlayerHealthCommit(DamageSource damageSource, float amount, CallbackInfo callback) {
        TechniqueHits.noteHealthCommitted((LivingEntity) (Object) this, damageSource);
    }
}
