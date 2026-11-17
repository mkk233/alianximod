package com.bslx.XueXiMod.mixin.client;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowClientBridge;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 本地驾驶根每步开始清消费标记，结束时若还没走过原生 move 就补一次。 */
@Mixin(Entity.class)
public class FlowClientTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void xuexiFlowBegin(CallbackInfo callback) {
        Entity self = (Entity) (Object) this;
        if (drives(self)) {
            FlowClientBridge.beginStep(self);
        }
    }

    private static boolean drives(Entity mover) {
        return mover.level().isClientSide() && FlowMotionIntents.clientDriven(mover) && !(mover instanceof AbstractMinecart);
    }
}
