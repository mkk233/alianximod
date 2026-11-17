package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Living 的位移和包围盒变化使空间候选失效。非 Living 的攻击体和飞剑不触发。 */
@Mixin(Entity.class)
public class LivingCandidateEntityMixin {
    static {
        LivingCandidateQueries.markEntityHooks();
    }

    @Inject(method = "setPosRaw", at = @At("HEAD"))
    private void xuexiCandidatePosHead(double nextX, double nextY, double nextZ, CallbackInfo callback) {
        LivingCandidateQueries.beginPositionChange((Entity) (Object) this, nextX, nextY, nextZ);
    }

    @Inject(method = "setPosRaw", at = @At("RETURN"))
    private void xuexiCandidatePosReturn(double nextX, double nextY, double nextZ, CallbackInfo callback) {
        LivingCandidateQueries.endPositionChange((Entity) (Object) this);
    }

    @Inject(method = "setBoundingBox", at = @At("HEAD"))
    private void xuexiCandidateBox(AABB replacement, CallbackInfo callback) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity)) {
            return;
        }
        AABB current = self.getBoundingBox();
        if (current != null && current.equals(replacement)) {
            return;
        }
        LivingCandidateQueries.invalidate(self.level());
    }
}
