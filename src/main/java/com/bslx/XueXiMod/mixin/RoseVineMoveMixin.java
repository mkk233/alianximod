package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 非玩家的自身移动在结算后计入缠枝刺伤。玩家位置由移动包逐段记录。 */
@Mixin(LivingEntity.class)
public abstract class RoseVineMoveMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void xuexiRoseBeforeStep(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide() && !(entity instanceof ServerPlayer)) {
            VineMarkRuntime.captureMove(entity);
        }
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void xuexiRoseAfterStep(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide() || entity instanceof ServerPlayer) {
            return;
        }
        VineMarkRuntime.finishMove(entity);
    }
}
