package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 有效上升时临时视为无重力。不写 DATA_NO_GRAVITY。 */
@Mixin(Entity.class)
public class FlowGravityMixin {
    @Inject(method = "isNoGravity", at = @At("RETURN"), cancellable = true)
    private void xuexiFlowRise(CallbackInfoReturnable<Boolean> callback) {
        if (Boolean.TRUE.equals(callback.getReturnValue())) {
            return;
        }
        Entity self = (Entity) (Object) this;
        Entity root = self.getRootVehicle();
        if (FlowMotionIntents.hasEffectiveRise(root)) {
            callback.setReturnValue(true);
        }
    }
}
