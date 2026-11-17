package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.status.WetnessFlight;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 悦灵的 travel 不调用 LivingEntity.travel。 */
@Mixin(Allay.class)
public class WetnessAllayMixin {
    @Inject(method = "travel", at = @At("HEAD"))
    private void xuexiWetnessAllayTravel(Vec3 travelVector, CallbackInfo callback) {
        WetnessFlight.beforeTravel((LivingEntity) (Object) this);
    }
}
