package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;

/** 只替换头部 translateAndRotate。婴儿缩放和头颅物品局部变换仍留在原方法里。 */
@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadSkinMixin {
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V"),
            require = 1)
    private void xuexiHeadRotate(ModelPart headPart, PoseStack poseStack) {
        EntityModel<?> model = ((CustomHeadLayer<?, ?>) (Object) this).getParentModel();
        try {
            if (!SkinAttachmentBridge.replaceHead(model, headPart, poseStack)) {
                headPart.translateAndRotate(poseStack);
            }
        } catch (RuntimeException failure) {
            poseStack.popPose();
            throw failure;
        }
    }
}
