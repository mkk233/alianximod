package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.CapeLayer;

/** 在披风唯一的 0.125 平移之前乘一次胸部矩阵。披风几何、纹理和局部摆动保留。 */
@Mixin(CapeLayer.class)
public abstract class CapeSkinMixin {
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
            require = 1)
    private void xuexiCapeShift(PoseStack poseStack, float shiftX, float shiftY, float shiftZ) {
        EntityModel<?> model = ((CapeLayer) (Object) this).getParentModel();
        try {
            SkinAttachmentBridge.prependChest(model, poseStack);
            poseStack.translate(shiftX, shiftY, shiftZ);
        } catch (RuntimeException failure) {
            poseStack.popPose();
            throw failure;
        }
    }
}
