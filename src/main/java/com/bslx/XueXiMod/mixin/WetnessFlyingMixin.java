package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.status.WetnessFlight;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 幻翼和恶魂的 travel 不调用 LivingEntity.travel。同一实体嵌套进入时由 helper 跳过。 */
@Mixin(FlyingMob.class)
public class WetnessFlyingMixin {
    @Inject(method = "travel", at = @At("HEAD"))
    private void xuexiWetnessBeforeFlyingTravel(Vec3 travelVector, CallbackInfo callback) {
        WetnessFlight.beforeTravel((LivingEntity) (Object) this);
    }
}
