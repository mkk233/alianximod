package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainMotion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在原版移动开始前裁切位移，结束时再核对一次最终位置。 */
@Mixin(Entity.class)
public class EntityMoveMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 xuexiDomainClip(Vec3 movement) {
        return DomainMotion.clip((Entity) (Object) this, movement);
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void xuexiDomainFinish(MoverType moverType, Vec3 movement, CallbackInfo callback) {
        DomainMotion.finish((Entity) (Object) this);
    }
}
