package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;
import com.bslx.XueXiMod.client.action.spacecut.SpaceCutItemMount;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 两次 renderArmWithItem 都带着实体和手。未蒙皮时原方法走一次。
 * 已蒙皮时不再进原方法，避免 translateToHand 和手再画一遍。
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandSkinMixin {
    @Shadow
    private ItemInHandRenderer itemInHandRenderer;

    @Invoker("renderArmWithItem")
    abstract void xuexiCallArm(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight);

    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/ItemInHandLayer;renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            require = 2)
    private void xuexiHeld(
            ItemInHandLayer<?, ?> layer,
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        ItemInHandSkinMixin mixin = (ItemInHandSkinMixin) (Object) layer;
        if (SpaceCutItemMount.redirectThirdPerson(
                mixin.itemInHandRenderer,
                entity,
                stack,
                arm,
                poseStack,
                buffers,
                packedLight)) {
            return;
        }
        EntityModel<?> model = layer.getParentModel();
        if (stack == null || stack.isEmpty() || !SkinAttachmentBridge.heldReplaces(entity, model, arm)) {
            mixin.xuexiCallArm(entity, stack, displayContext, arm, poseStack, buffers, packedLight);
            return;
        }
        poseStack.pushPose();
        try {
            SkinAttachmentBridge.applyHeld(entity, model, arm, poseStack);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean leftSide = arm == HumanoidArm.LEFT;
            poseStack.translate(leftSide ? -0.0625F : 0.0625F, 0.125F, -0.625F);
            mixin.itemInHandRenderer.renderItem(entity, stack, displayContext, leftSide, poseStack, buffers, packedLight);
        } finally {
            poseStack.popPose();
        }
    }
}
