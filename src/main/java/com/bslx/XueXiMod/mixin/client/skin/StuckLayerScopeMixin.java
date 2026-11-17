package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 只包装 StuckInBodyLayer 的那一次 RenderLayer.render。其他层原样调用一次。 */
@Mixin(LivingEntityRenderer.class)
public abstract class StuckLayerScopeMixin {
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"),
            require = 1)
    private void xuexiStuckScope(
            RenderLayer<?, ?> layer,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (layer instanceof StuckInBodyLayer && entity instanceof LivingEntity living) {
            EntityModel<?> bodyModel = ((LivingEntityRenderer<?, ?>) (Object) this).getModel();
            if (!SkinAttachmentBridge.stuckPieceNeeded(living, bodyModel)) {
                renderLayer(layer, poseStack, buffers, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
                return;
            }
            SkinAttachmentBridge.StuckToken token = SkinAttachmentBridge.openStuck(living, bodyModel);
            try {
                renderLayer(layer, poseStack, buffers, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            } finally {
                token.close();
            }
            return;
        }
        renderLayer(layer, poseStack, buffers, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
    }

    @SuppressWarnings("unchecked")
    private static <TEntity extends Entity> void renderLayer(
            RenderLayer<TEntity, ?> layer,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            Entity entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        layer.render(poseStack, buffers, packedLight, (TEntity) entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
    }
}
