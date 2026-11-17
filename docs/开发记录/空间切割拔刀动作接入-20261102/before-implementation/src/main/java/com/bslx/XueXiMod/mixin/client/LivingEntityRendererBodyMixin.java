package com.bslx.XueXiMod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.action.skinned.SkinnedBodyDraw;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** 只在当前玩家 scope 已是 SKINNED 时替换这一次身体绘制。名字、阴影和层仍走原方法。 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererBodyMixin {
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            require = 1)
    private void xuexiRenderBody(
            EntityModel<?> rendered,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        if (SkinnedBodyDraw.replace(rendered, poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha)) {
            return;
        }
        rendered.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
