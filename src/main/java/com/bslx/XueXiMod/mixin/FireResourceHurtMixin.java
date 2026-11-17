package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.fire.FireCooldownPolicy;
import com.bslx.XueXiMod.shu.technique.release.fire.FireDamage;
import com.bslx.XueXiMod.status.wood.vine.VineMarkDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * lastHurt 的第一处 PUTFIELD 是差额分支，第二处才是 else。
 * invulnerableTime 在映射字节码里的 owner 是 LivingEntity，本方法只有这一处写入。
 * 两个重定向在实例和新值之后直接接收本次 hurt 的 DamageSource 与 float，不读元素栈。
 */
@Mixin(LivingEntity.class)
public abstract class FireResourceHurtMixin {
    @Shadow
    protected float lastHurt;

    @Redirect(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/LivingEntity;lastHurt:F",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 1
            ),
            require = 1
    )
    private void xuexiSkipFireResourceLastHurt(LivingEntity entity, float replacement, DamageSource source,
                                               float amount) {
        if (FireCooldownPolicy.keepExistingHurt(FireDamage.matches(source)) || VineMarkDamage.matches(source)) {
            return;
        }
        this.lastHurt = replacement;
    }

    @Redirect(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0
            ),
            require = 1
    )
    private void xuexiSkipFireResourceInvulnerableTime(LivingEntity entity, int replacement, DamageSource source,
                                                       float amount) {
        if (FireCooldownPolicy.keepExistingHurt(FireDamage.matches(source)) || VineMarkDamage.matches(source)) {
            return;
        }
        entity.invulnerableTime = replacement;
    }
}
