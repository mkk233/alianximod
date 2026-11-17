package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.domain.PressureBreath;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 只包住这次溺水 hurt。取消或没有调用时不会留下水势票据。 */
@Mixin(ForgeHooks.class)
public class PressureDrownMixin {
    @Inject(method = "onLivingBreathe",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static void xuexiPressureBeforeHurt(LivingEntity entity, int consumeAir, int refillAir, CallbackInfo callback) {
        PressureBreath.openDrownScope(entity);
    }

    @Inject(method = "onLivingBreathe",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    shift = At.Shift.AFTER))
    private static void xuexiPressureAfterHurt(LivingEntity entity, int consumeAir, int refillAir, CallbackInfo callback) {
        PressureBreath.closeDrownScope(entity);
    }
}
