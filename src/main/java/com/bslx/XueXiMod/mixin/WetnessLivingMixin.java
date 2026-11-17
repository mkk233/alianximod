package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.status.WetnessFlight;
import com.bslx.XueXiMod.status.WetnessSensitivity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 移动控制之后、本拍 travel 使用速度之前夹下降。怕水扣血放在 aiStep 末尾。 */
@Mixin(LivingEntity.class)
public class WetnessLivingMixin {
    @Inject(method = "travel", at = @At("HEAD"))
    private void xuexiWetnessBeforeTravel(Vec3 travelVector, CallbackInfo callback) {
        WetnessFlight.beforeTravel((LivingEntity) (Object) this);
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void xuexiWetnessAfterAi(CallbackInfo callback) {
        WetnessSensitivity.afterAiStep((LivingEntity) (Object) this);
    }
}
