package com.bslx.XueXiMod.client.furnace;

import org.joml.Quaternionf;

import com.bslx.XueXiMod.client.furnace.FurnaceBodyVisual.Playback;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * 第一人称手焰跟原版 ItemInHandRenderer 的同一套姿态。
 * 不取消 RenderHandEvent，因此原版手臂和物品仍由原版绘制。
 * 空手、地图走手臂网格空间；其他持物把拳心锚到物品手部原点。披风和身火不进第一人称。
 */
public final class BurnFirstPersonPose {
    private BurnFirstPersonPose() {
    }

    public static void draw(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource,
                            AbstractClientPlayer player, InteractionHand hand, ItemStack stack,
                            float partialTick, float viewPitch, float swingProgress, float equipProgress,
                            Playback playback) {
        if (player == null || player.isInvisible() || player.isScoping() || stack == null) {
            return;
        }
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        HumanoidArm armSide = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        if (stack.isEmpty()) {
            if (!mainHand) {
                return;
            }
            poseStack.pushPose();
            try {
                applyEmptyArm(poseStack, equipProgress, swingProgress, armSide);
                applyArmPivot(poseStack, armSide);
                drawArm(poseStack, bufferSource, playback, armSide);
            } finally {
                poseStack.popPose();
            }
            return;
        }
        if (stack.is(Items.FILLED_MAP)) {
            poseStack.pushPose();
            try {
                if (mainHand && player.getOffhandItem().isEmpty()) {
                    applyTwoHandedMap(poseStack, viewPitch, equipProgress, swingProgress);
                    poseStack.pushPose();
                    try {
                        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                        drawMapHand(poseStack, bufferSource, playback, HumanoidArm.RIGHT);
                        drawMapHand(poseStack, bufferSource, playback, HumanoidArm.LEFT);
                    } finally {
                        poseStack.popPose();
                    }
                } else {
                    applyOneHandedMap(poseStack, equipProgress, swingProgress, armSide);
                    applyArmPivot(poseStack, armSide);
                    drawArm(poseStack, bufferSource, playback, armSide);
                }
            } finally {
                poseStack.popPose();
            }
            return;
        }
        poseStack.pushPose();
        try {
            applyHeldItem(poseStack, player, stack, partialTick, swingProgress, equipProgress, armSide, mainHand);
            BurnSpatialMount.translateGrip(poseStack, armSide == HumanoidArm.RIGHT);
            drawArm(poseStack, bufferSource, playback, armSide);
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawArm(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                Playback playback, HumanoidArm armSide) {
        BurnSpatialMount.draw(poseStack, bufferSource, playback,
                armSide == HumanoidArm.RIGHT ? "right_arm" : "left_arm");
    }

    private static void drawMapHand(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                    Playback playback, HumanoidArm armSide) {
        poseStack.pushPose();
        try {
            float sideSign = sideSign(armSide);
            poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * -41.0F));
            poseStack.translate(sideSign * 0.3F, -1.1F, 0.45F);
            applyArmPivot(poseStack, armSide);
            drawArm(poseStack, bufferSource, playback, armSide);
        } finally {
            poseStack.popPose();
        }
    }

    /** 与 PlayerRenderer.renderHand 在年龄 0、空手或持物姿态下强制 xRot=0 后的手臂枢轴一致。 */
    private static void applyArmPivot(PoseStack poseStack, HumanoidArm armSide) {
        float sideSign = sideSign(armSide);
        poseStack.translate(-sideSign * 5.0F * BurnSpatialMount.MODEL_TO_BLOCK,
                2.0F * BurnSpatialMount.MODEL_TO_BLOCK, 0.0F);
        poseStack.mulPose(new Quaternionf().rotationZYX(sideSign * 0.1F, 0.0F, 0.0F));
    }

    private static void applyEmptyArm(PoseStack poseStack, float equipProgress, float swingProgress, HumanoidArm armSide) {
        float sideSign = sideSign(armSide);
        float swingRoot = Mth.sqrt(swingProgress);
        float swayAcross = -0.3F * Mth.sin(swingRoot * (float) Math.PI);
        float swayLift = 0.4F * Mth.sin(swingRoot * ((float) Math.PI * 2.0F));
        float swayForward = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(sideSign * (swayAcross + 0.64000005F),
                swayLift + -0.6F + equipProgress * -0.6F, swayForward + -0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * 45.0F));
        float swingSquare = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float swingSine = Mth.sin(swingRoot * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * swingSine * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * swingSquare * -20.0F));
        poseStack.translate(sideSign * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * -135.0F));
        poseStack.translate(sideSign * 5.6F, 0.0F, 0.0F);
    }

    private static void applyOneHandedMap(PoseStack poseStack, float equipProgress, float swingProgress, HumanoidArm armSide) {
        float sideSign = sideSign(armSide);
        poseStack.translate(sideSign * 0.125F, -0.125F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * 10.0F));
        applyEmptyArm(poseStack, equipProgress, swingProgress, armSide);
    }

    private static void applyTwoHandedMap(PoseStack poseStack, float viewPitch, float equipProgress, float swingProgress) {
        float swingRoot = Mth.sqrt(swingProgress);
        float dip = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        float push = -0.4F * Mth.sin(swingRoot * (float) Math.PI);
        poseStack.translate(0.0F, -dip / 2.0F, push);
        float mapTilt = mapTilt(viewPitch);
        poseStack.translate(0.0F, 0.04F + equipProgress * -1.2F + mapTilt * -0.5F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(mapTilt * -85.0F));
    }

    private static float mapTilt(float viewPitch) {
        float tilt = 1.0F - viewPitch / 45.0F + 0.1F;
        tilt = Mth.clamp(tilt, 0.0F, 1.0F);
        return -Mth.cos(tilt * (float) Math.PI) * 0.5F + 0.5F;
    }

    private static void applyHeldItem(PoseStack poseStack, AbstractClientPlayer player, ItemStack stack,
                                      float partialTick, float swingProgress, float equipProgress,
                                      HumanoidArm armSide, boolean mainHand) {
        if (stack.getItem() instanceof CrossbowItem) {
            applyCrossbow(poseStack, player, stack, partialTick, swingProgress, equipProgress, armSide, mainHand);
            return;
        }
        boolean customPose = false;
        if (player instanceof LocalPlayer localPlayer) {
            customPose = IClientItemExtensions.of(stack).applyForgeHandTransform(
                    poseStack, localPlayer, armSide, stack, partialTick, equipProgress, swingProgress);
        }
        if (customPose) {
            return;
        }
        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == (mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)) {
            applyUse(poseStack, player, stack, partialTick, equipProgress, armSide);
            return;
        }
        if (player.isAutoSpinAttack()) {
            applyItemArm(poseStack, equipProgress, armSide);
            float sideSign = sideSign(armSide);
            poseStack.translate(sideSign * -0.4F, 0.8F, 0.3F);
            poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * 65.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * -85.0F));
            return;
        }
        float swingRoot = Mth.sqrt(swingProgress);
        float sideSign = sideSign(armSide);
        float swayAcross = -0.4F * Mth.sin(swingRoot * (float) Math.PI);
        float swayLift = 0.2F * Mth.sin(swingRoot * ((float) Math.PI * 2.0F));
        float swayForward = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(sideSign * swayAcross, swayLift, swayForward);
        applyItemArm(poseStack, equipProgress, armSide);
        applyItemAttack(poseStack, swingProgress, armSide);
    }

    private static void applyCrossbow(PoseStack poseStack, AbstractClientPlayer player, ItemStack stack,
                                      float partialTick, float swingProgress, float equipProgress,
                                      HumanoidArm armSide, boolean mainHand) {
        boolean charged = CrossbowItem.isCharged(stack);
        float sideSign = sideSign(armSide);
        if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == (mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)) {
            applyItemArm(poseStack, equipProgress, armSide);
            poseStack.translate(sideSign * -0.4785682F, -0.094387F, 0.05731531F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-11.935F));
            poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * 65.3F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * -9.785F));
            float chargeTicks = (float) stack.getUseDuration()
                    - ((float) player.getUseItemRemainingTicks() - partialTick + 1.0F);
            float chargeRatio = chargeTicks / (float) CrossbowItem.getChargeDuration(stack);
            if (chargeRatio > 1.0F) {
                chargeRatio = 1.0F;
            }
            if (chargeRatio > 0.1F) {
                float chargeShake = Mth.sin((chargeTicks - 0.1F) * 1.3F);
                float chargeSpan = chargeRatio - 0.1F;
                float chargeJolt = chargeShake * chargeSpan;
                poseStack.translate(chargeJolt * 0.0F, chargeJolt * 0.004F, chargeJolt * 0.0F);
            }
            poseStack.translate(chargeRatio * 0.0F, chargeRatio * 0.0F, chargeRatio * 0.04F);
            poseStack.scale(1.0F, 1.0F, 1.0F + chargeRatio * 0.2F);
            poseStack.mulPose(Axis.YN.rotationDegrees(sideSign * 45.0F));
            return;
        }
        float swingRoot = Mth.sqrt(swingProgress);
        float swayAcross = -0.4F * Mth.sin(swingRoot * (float) Math.PI);
        float swayLift = 0.2F * Mth.sin(swingRoot * ((float) Math.PI * 2.0F));
        float swayForward = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(sideSign * swayAcross, swayLift, swayForward);
        applyItemArm(poseStack, equipProgress, armSide);
        applyItemAttack(poseStack, swingProgress, armSide);
        if (charged && swingProgress < 0.001F && mainHand) {
            poseStack.translate(sideSign * -0.641864F, 0.0F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * 10.0F));
        }
    }

    private static void applyUse(PoseStack poseStack, AbstractClientPlayer player, ItemStack stack,
                                 float partialTick, float equipProgress, HumanoidArm armSide) {
        float sideSign = sideSign(armSide);
        UseAnim useAnim = stack.getUseAnimation();
        if (useAnim == UseAnim.NONE || useAnim == UseAnim.BLOCK) {
            applyItemArm(poseStack, equipProgress, armSide);
            return;
        }
        if (useAnim == UseAnim.EAT || useAnim == UseAnim.DRINK) {
            applyEat(poseStack, player, stack, partialTick, armSide);
            applyItemArm(poseStack, equipProgress, armSide);
            return;
        }
        if (useAnim == UseAnim.BOW) {
            applyItemArm(poseStack, equipProgress, armSide);
            poseStack.translate(sideSign * -0.2785682F, 0.18344387F, 0.15731531F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));
            poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * 35.3F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * -9.785F));
            float drawTicks = (float) stack.getUseDuration()
                    - ((float) player.getUseItemRemainingTicks() - partialTick + 1.0F);
            float drawRatio = drawTicks / 20.0F;
            drawRatio = (drawRatio * drawRatio + drawRatio * 2.0F) / 3.0F;
            if (drawRatio > 1.0F) {
                drawRatio = 1.0F;
            }
            if (drawRatio > 0.1F) {
                float drawShake = Mth.sin((drawTicks - 0.1F) * 1.3F);
                float drawSpan = drawRatio - 0.1F;
                float drawJolt = drawShake * drawSpan;
                poseStack.translate(drawJolt * 0.0F, drawJolt * 0.004F, drawJolt * 0.0F);
            }
            poseStack.translate(drawRatio * 0.0F, drawRatio * 0.0F, drawRatio * 0.04F);
            poseStack.scale(1.0F, 1.0F, 1.0F + drawRatio * 0.2F);
            poseStack.mulPose(Axis.YN.rotationDegrees(sideSign * 45.0F));
            return;
        }
        if (useAnim == UseAnim.SPEAR) {
            applyItemArm(poseStack, equipProgress, armSide);
            poseStack.translate(sideSign * -0.5F, 0.7F, 0.1F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-55.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * 35.3F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * -9.785F));
            float throwTicks = (float) stack.getUseDuration()
                    - ((float) player.getUseItemRemainingTicks() - partialTick + 1.0F);
            float throwRatio = throwTicks / 10.0F;
            if (throwRatio > 1.0F) {
                throwRatio = 1.0F;
            }
            if (throwRatio > 0.1F) {
                float throwShake = Mth.sin((throwTicks - 0.1F) * 1.3F);
                float throwSpan = throwRatio - 0.1F;
                float throwJolt = throwShake * throwSpan;
                poseStack.translate(throwJolt * 0.0F, throwJolt * 0.004F, throwJolt * 0.0F);
            }
            poseStack.translate(0.0F, 0.0F, throwRatio * 0.2F);
            poseStack.scale(1.0F, 1.0F, 1.0F + throwRatio * 0.2F);
            poseStack.mulPose(Axis.YN.rotationDegrees(sideSign * 45.0F));
            return;
        }
        if (useAnim == UseAnim.BRUSH) {
            applyBrush(poseStack, player, partialTick, equipProgress, armSide);
        }
    }

    private static void applyEat(PoseStack poseStack, AbstractClientPlayer player, ItemStack stack,
                                 float partialTick, HumanoidArm armSide) {
        float remaining = (float) player.getUseItemRemainingTicks() - partialTick + 1.0F;
        float remainingRatio = remaining / (float) stack.getUseDuration();
        if (remainingRatio < 0.8F) {
            float bob = Mth.abs(Mth.cos(remaining / 4.0F * (float) Math.PI) * 0.1F);
            poseStack.translate(0.0F, bob, 0.0F);
        }
        float bite = 1.0F - (float) Math.pow(remainingRatio, 27.0D);
        float sideSign = sideSign(armSide);
        poseStack.translate(bite * 0.6F * sideSign, bite * -0.5F, bite * 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * bite * 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(bite * 10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * bite * 30.0F));
    }

    private static void applyBrush(PoseStack poseStack, AbstractClientPlayer player, float partialTick,
                                   float equipProgress, HumanoidArm armSide) {
        applyItemArm(poseStack, equipProgress, armSide);
        float brushTick = (float) (player.getUseItemRemainingTicks() % 10);
        float brushRemaining = brushTick - partialTick + 1.0F;
        float brushRatio = 1.0F - brushRemaining / 10.0F;
        float brushSweep = -15.0F + 75.0F * Mth.cos(brushRatio * 2.0F * (float) Math.PI);
        if (armSide != HumanoidArm.RIGHT) {
            poseStack.translate(0.1D, 0.83D, 0.35D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(brushSweep));
            poseStack.translate(-0.3D, 0.22D, 0.35D);
            return;
        }
        poseStack.translate(-0.25D, 0.22D, 0.35D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(0.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(brushSweep));
    }

    private static void applyItemAttack(PoseStack poseStack, float swingProgress, HumanoidArm armSide) {
        float sideSign = sideSign(armSide);
        float swingSquare = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * (45.0F + swingSquare * -20.0F)));
        float swingSine = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.mulPose(Axis.ZP.rotationDegrees(sideSign * swingSine * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(swingSine * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(sideSign * -45.0F));
    }

    private static void applyItemArm(PoseStack poseStack, float equipProgress, HumanoidArm armSide) {
        float sideSign = sideSign(armSide);
        poseStack.translate(sideSign * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private static float sideSign(HumanoidArm armSide) {
        return armSide == HumanoidArm.RIGHT ? 1.0F : -1.0F;
    }
}
