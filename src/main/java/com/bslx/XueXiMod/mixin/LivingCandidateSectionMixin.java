package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Living 进出 section，以及 section 可访问性切换，都使该世界的空间候选失效。 */
@Mixin(EntitySection.class)
public class LivingCandidateSectionMixin {
    static {
        LivingCandidateQueries.markSectionHooks();
    }

    @Shadow
    private Visibility chunkStatus;

    @Inject(method = "add", at = @At("HEAD"))
    private void xuexiCandidateAddHead(EntityAccess stored, CallbackInfo callback) {
        LivingCandidateQueries.invalidateLiving(stored);
    }

    @Inject(method = "add", at = @At("RETURN"))
    private void xuexiCandidateAddReturn(EntityAccess stored, CallbackInfo callback) {
        LivingCandidateQueries.invalidateLiving(stored);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void xuexiCandidateRemoveHead(EntityAccess stored, CallbackInfoReturnable<Boolean> callback) {
        LivingCandidateQueries.invalidateLiving(stored);
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void xuexiCandidateRemoveReturn(EntityAccess stored, CallbackInfoReturnable<Boolean> callback) {
        LivingCandidateQueries.invalidateLiving(stored);
    }

    @Inject(method = "updateChunkStatus", at = @At("HEAD"))
    private void xuexiCandidateStatus(Visibility next, CallbackInfoReturnable<Visibility> callback) {
        if (next == null || this.chunkStatus == null || next.isAccessible() == this.chunkStatus.isAccessible()) {
            return;
        }
        LivingCandidateQueries.invalidateSection((EntitySection<?>) (Object) this);
    }
}
