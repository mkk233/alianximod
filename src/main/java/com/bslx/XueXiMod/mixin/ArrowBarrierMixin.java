package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.barrier.ProjectileBarrierTracker;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 命中扣血之前先把这一段飞程推进到真实命中点。 */
@Mixin(AbstractArrow.class)
public class ArrowBarrierMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void xuexiArrowStart(CallbackInfo callback) {
        ProjectileBarrierTracker.noteStart((AbstractArrow) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void xuexiArrowEnd(CallbackInfo callback) {
        ProjectileBarrierTracker.noteEnd((AbstractArrow) (Object) this);
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void xuexiArrowHit(EntityHitResult impact, CallbackInfo callback) {
        ProjectileBarrierTracker.advanceToHit((AbstractArrow) (Object) this, impact.getLocation());
    }
}
