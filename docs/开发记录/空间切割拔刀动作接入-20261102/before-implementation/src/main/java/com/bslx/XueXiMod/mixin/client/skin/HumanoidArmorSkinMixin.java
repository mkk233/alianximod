package com.bslx.XueXiMod.mixin.client.skin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * 在 render 的四次 renderArmorPiece 上逐件 try/finally。
 * 染色、overlay、trim、箔和 Forge 纹理选择仍走原方法，只替换三个 Model.renderToBuffer。
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorSkinMixin {
    @Invoker("renderArmorPiece")
    abstract void xuexiCallArmorPiece(
            PoseStack poseStack,
            MultiBufferSource buffers,
            LivingEntity entity,
            EquipmentSlot slot,
            int packedLight,
            HumanoidModel<?> humanoid);

    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"),
            require = 4)
    private void xuexiArmorPiece(
            HumanoidArmorLayer<?, ?, ?> layer,
            PoseStack poseStack,
            MultiBufferSource buffers,
            LivingEntity entity,
            EquipmentSlot slot,
            int packedLight,
            HumanoidModel<?> humanoid) {
        EntityModel<?> parentModel = layer.getParentModel();
        if (!SkinAttachmentBridge.armorPieceNeeded(entity, parentModel)) {
            ((HumanoidArmorSkinMixin) (Object) layer).xuexiCallArmorPiece(
                    poseStack, buffers, entity, slot, packedLight, humanoid);
            return;
        }
        SkinAttachmentBridge.ArmorToken token = SkinAttachmentBridge.openArmorPiece(entity, parentModel);
        try {
            ((HumanoidArmorSkinMixin) (Object) layer).xuexiCallArmorPiece(
                    poseStack, buffers, entity, slot, packedLight, humanoid);
        } finally {
            token.close();
        }
    }

    @Redirect(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;getArmorModelHook(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/client/model/HumanoidModel;)Lnet/minecraft/client/model/Model;",
                    remap = false))
    private Model xuexiArmorHook(
            HumanoidArmorLayer<?, ?, ?> layer,
            LivingEntity entity,
            ItemStack stack,
            EquipmentSlot slot,
            HumanoidModel<?> humanoid) {
        Model cached = SkinAttachmentBridge.cachedArmorModel(entity, slot);
        if (cached != null) {
            return cached;
        }
        return ForgeHooksClient.getArmorModel(entity, stack, slot, humanoid);
    }

    @Redirect(
            method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/ArmorItem;Lnet/minecraft/client/model/Model;ZFFFLnet/minecraft/resources/ResourceLocation;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void xuexiArmorBase(
            Model model,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float redAmount,
            float greenAmount,
            float blueAmount,
            float alphaAmount) {
        this.xuexiArmorLeaf(model, poseStack, consumer, packedLight, packedOverlay, redAmount, greenAmount, blueAmount, alphaAmount);
    }

    @Redirect(
            method = "renderTrim(Lnet/minecraft/world/item/ArmorMaterial;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/Model;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void xuexiArmorTrim(
            Model model,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float redAmount,
            float greenAmount,
            float blueAmount,
            float alphaAmount) {
        this.xuexiArmorLeaf(model, poseStack, consumer, packedLight, packedOverlay, redAmount, greenAmount, blueAmount, alphaAmount);
    }

    @Redirect(
            method = "renderGlint(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
    private void xuexiArmorGlint(
            Model model,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float redAmount,
            float greenAmount,
            float blueAmount,
            float alphaAmount) {
        this.xuexiArmorLeaf(model, poseStack, consumer, packedLight, packedOverlay, redAmount, greenAmount, blueAmount, alphaAmount);
    }

    private void xuexiArmorLeaf(
            Model model,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float redAmount,
            float greenAmount,
            float blueAmount,
            float alphaAmount) {
        if (!SkinAttachmentBridge.emitArmorOrPass(model, poseStack, consumer, packedLight, packedOverlay, redAmount, greenAmount, blueAmount, alphaAmount)) {
            model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay, redAmount, greenAmount, blueAmount, alphaAmount);
        }
    }
}
