package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;

/** 在鞘翅唯一的 0.125 平移之前乘一次胸部矩阵。setupAnim 和原翼几何保留。 */
@Mixin(ElytraLayer.class)
public abstract class ElytraSkinMixin {
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
            require = 1)
    private void xuexiElytraShift(PoseStack poseStack, float shiftX, float shiftY, float shiftZ) {
        EntityModel<?> model = ((ElytraLayer<?, ?>) (Object) this).getParentModel();
        try {
            SkinAttachmentBridge.prependChest(model, poseStack);
            poseStack.translate(shiftX, shiftY, shiftZ);
        } catch (RuntimeException failure) {
            poseStack.popPose();
            throw failure;
        }
    }
}
