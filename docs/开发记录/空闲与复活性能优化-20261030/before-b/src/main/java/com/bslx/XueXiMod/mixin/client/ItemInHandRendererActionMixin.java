package com.bslx.XueXiMod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bslx.XueXiMod.client.action.ActionRenderQueries;
import com.bslx.XueXiMod.client.action.ModelPartSnapshot;
import com.bslx.XueXiMod.client.action.PoseStackGuard;
import com.bslx.XueXiMod.client.action.RenderPoseScope;
import com.bslx.XueXiMod.client.action.skinned.FirstPersonSkinDraw;
import com.bslx.XueXiMod.client.action.skinned.SkinSession;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 两个调用点都在 Forge 取消判断之后。
 * 接管的剑手把这一次挥砍进度清零，装备进度保持原值。
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererActionMixin {
    @Shadow
    private void renderArmWithItem(
            AbstractClientPlayer player,
            float partialTick,
            float interpolatedPitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        throw new AssertionError();
    }

    @Redirect(
            method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            require = 2)
    private void xuexiRenderArm(
            ItemInHandRenderer renderer,
            AbstractClientPlayer player,
            float partialTick,
            float interpolatedPitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        if (renderer != (ItemInHandRenderer) (Object) this) {
            throw new IllegalStateException("hand renderer receiver changed");
        }
        PlayerModel<?> model = firstPersonModel(player);
        int entryDepth = PoseStackGuard.depth(poseStack);
        ModelPartSnapshot snapshot = ModelPartSnapshot.capturePlayer(player);
        RenderPoseScope.runFirstPerson(model, player, () -> {
            try {
                SkinSession.openFirstPerson(model, player, partialTick);
                boolean takeHand = ActionRenderQueries.takeFirstPerson(player, hand, stack, partialTick);
                if (takeHand && FirstPersonSkinDraw.draw(player, hand, stack, equipProgress, poseStack, buffer, packedLight)) {
                    return;
                }
                float swingUsed = takeHand ? 0.0F : swingProgress;
                this.renderArmWithItem(
                        player,
                        partialTick,
                        interpolatedPitch,
                        hand,
                        swingUsed,
                        stack,
                        equipProgress,
                        poseStack,
                        buffer,
                        packedLight);
            } finally {
                try {
                    snapshot.restore();
                } finally {
                    PoseStackGuard.restore(poseStack, entryDepth);
                }
            }
        });
    }

    private static PlayerModel<?> firstPersonModel(AbstractClientPlayer player) {
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

    @Inject(
            method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
                    shift = At.Shift.AFTER),
            require = 1)
    private void xuexiAimHeldItem(
            AbstractClientPlayer player,
            float partialTick,
            float interpolatedPitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callback) {
        Quaternionf held = ActionRenderQueries.firstPersonQuaternion(player, hand, stack, partialTick);
        if (held == null) {
            return;
        }
        poseStack.mulPose(held);
    }
}
