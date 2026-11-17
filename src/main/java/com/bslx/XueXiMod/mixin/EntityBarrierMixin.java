package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierBody;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 在移动者自己的碰撞判断里决定要不要带上这面墙。 */
@Mixin(Entity.class)
public class EntityBarrierMixin {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void xuexiBarrierPass(Entity other, CallbackInfoReturnable<Boolean> callback) {
        if (!(other instanceof BarrierBody wall)) {
            return;
        }
        Entity mover = (Entity) (Object) this;
        if (ContactPolicy.mayBodyPass(mover, wall)) {
            callback.setReturnValue(false);
        }
    }
}
