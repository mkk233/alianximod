package com.bslx.XueXiMod.client.action.cooperative;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 第一人称画出确认曲线的左右臂和握柄，不转动玩家相机，也不改玩家位置。
 * 根偏航仍是未归一的整圈角度，出刀和收势用同一套骨骼欧拉角。
 */
public final class CooperativeFirstPerson {
    private static final float VIEWMODEL_DROP = 0.35F;
    private static final float VIEWMODEL_FORWARD = 0.45F;

    private CooperativeFirstPerson() {
    }

    public static void draw(
            AbstractClientPlayer player,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (player == null || poseStack == null || buffer == null) {
            return;
        }
        CooperativePose pose = CooperativeActionViews.pose(player, partialTick);
        PlayerModel<?> model = playerModel(player);
        if (pose == null || model == null) {
            return;
        }
        CooperativePoseApplier.apply(model, player, partialTick);
        poseStack.pushPose();
        try {
            poseStack.translate(0.0F, -VIEWMODEL_DROP, -VIEWMODEL_FORWARD);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.mulPose(Axis.YP.rotation(CooperativePoseApplier.rootSpinRadians(pose)));
            if (!player.isInvisible()) {
                VertexConsumer skin = buffer.getBuffer(RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));
                drawLimb(model.rightArm, poseStack, skin, packedLight);
                if (player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE)) {
                    drawLimb(model.rightSleeve, poseStack, skin, packedLight);
                }
                drawLimb(model.leftArm, poseStack, skin, packedLight);
                if (player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE)) {
                    drawLimb(model.leftSleeve, poseStack, skin, packedLight);
                }
            }
            boolean mainOnRight = player.getMainArm() != HumanoidArm.LEFT;
            drawHeld(player, mainOnRight ? model.rightArm : model.leftArm, mainOnRight,
                    player.getMainHandItem(), poseStack, buffer, packedLight);
            drawHeld(player, mainOnRight ? model.leftArm : model.rightArm, !mainOnRight,
                    player.getOffhandItem(), poseStack, buffer, packedLight);
        } finally {
            poseStack.popPose();
            CooperativePoseApplier.discardPending(model);
        }
    }

    private static void drawLimb(ModelPart limb, PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        if (limb == null || poseStack == null || consumer == null) {
            return;
        }
        limb.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawHeld(
            AbstractClientPlayer player,
            ModelPart arm,
            boolean rightSide,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (player == null || arm == null || stack == null || stack.isEmpty() || poseStack == null || buffer == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        poseStack.pushPose();
        try {
            CooperativeWeaponCopy.appendHeldItem(poseStack, arm, rightSide);
            itemRenderer.renderStatic(
                    stack,
                    rightSide ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    poseStack,
                    buffer,
                    player.level(),
                    player.getId());
        } finally {
            poseStack.popPose();
        }
    }

    private static PlayerModel<?> playerModel(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft == null || minecraft.getEntityRenderDispatcher() == null) {
            return null;
        }
        EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (renderer instanceof PlayerRenderer playerRenderer) {
            return playerRenderer.getModel();
        }
        return null;
    }
}
