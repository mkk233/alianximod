package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.util.RandomSource;

/**
 * 层内只保留选件和变换。打开和关闭由 LivingEntityRenderer 上的独占包装负责。
 * 未知部件仍走原调用。
 */
@Mixin(StuckInBodyLayer.class)
public abstract class StuckInBodySkinMixin {
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/PlayerModel;getRandomModelPart(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/client/model/geom/ModelPart;"))
    private ModelPart xuexiStuckPart(PlayerModel<?> model, RandomSource random) {
        ModelPart part = model.getRandomModelPart(random);
        SkinAttachmentBridge.noteStuckPart(part);
        return part;
    }

    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/geom/ModelPart;translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void xuexiStuckRotate(ModelPart part, PoseStack poseStack) {
        try {
            if (!SkinAttachmentBridge.replaceStuckRotate()) {
                part.translateAndRotate(poseStack);
            }
        } catch (RuntimeException failure) {
            poseStack.popPose();
            throw failure;
        }
    }

    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void xuexiStuckShift(PoseStack poseStack, float shiftX, float shiftY, float shiftZ) {
        try {
            if (!SkinAttachmentBridge.replaceStuckPoint(poseStack, shiftX, shiftY, shiftZ)) {
                poseStack.translate(shiftX, shiftY, shiftZ);
            }
        } catch (RuntimeException failure) {
            poseStack.popPose();
            throw failure;
        }
    }
}
