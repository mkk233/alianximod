package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 服务端自行模拟的实体每步只消费一次流场，没发生 move 时补一次。 */
@Mixin(ServerLevel.class)
public class FlowStepMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void xuexiFlowBegin(Entity entity, CallbackInfo callback) {
        FlowMotionIntents.beginStep(entity);
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void xuexiFlowFinish(Entity entity, CallbackInfo callback) {
        FlowMotionIntents.finishStep(entity);
    }
}
