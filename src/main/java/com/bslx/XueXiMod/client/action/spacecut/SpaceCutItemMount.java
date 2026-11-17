package com.bslx.XueXiMod.client.action.spacecut;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * 真实剑只在动作期挂到右手刀骨。主手在左时仍由右手渲染，结束绘制后不改库存和主手设置。
 * 没有剑就不生成示意刀。另一只手的非剑物品留在那只前臂腕部。
 */
public final class SpaceCutItemMount {
    /** 居中后的棕色缠把：(3.5, 3.5, 8) / 16 再减 0.5。方块中心不是这个点。 */
    private static final float GRIP_CENTER_X = 3.5F / 16.0F - 0.5F;
    private static final float GRIP_CENTER_Y = 3.5F / 16.0F - 0.5F;
    private static final float GRIP_CENTER_Z = 0.0F;
    /** 生成剑刀身 normalize(1, 1, 0)，扁平面法线 +Z，刃宽是刀身叉法线。 */
    private static final float MESH_BLADE_X = 0.7071067811865476F;
    private static final float MESH_BLADE_Y = 0.7071067811865476F;
    private static final float MESH_BLADE_Z = 0.0F;
    private static final float MESH_FLAT_X = 0.0F;
    private static final float MESH_FLAT_Y = 0.0F;
    private static final float MESH_FLAT_Z = 1.0F;
    private static final float MESH_EDGE_X = 0.7071067811865476F;
    private static final float MESH_EDGE_Y = -0.7071067811865476F;
    private static final float MESH_EDGE_Z = 0.0F;
    /** 右手 handheld 的 rotationXYZ 作用在刃宽和斜向刀身上。法线经同一旋转后是 (-1, 0, 0)。 */
    private static final float[] THIRD_EDGE = {0.0F, 0.1736481777F, 0.9848077530F};
    private static final float[] THIRD_BLADE = {0.0F, 0.9848077530F, -0.1736481777F};
    private static final float[] FIRST_EDGE = {0.0F, -0.3420201433F, 0.9396926208F};
    private static final float[] FIRST_BLADE = {0.0F, 0.9396926208F, 0.3420201433F};
    /** 原版右手 handheld 展示平移，单位是方块。renderItem 先平移再旋转再缩放。 */
    private static final float[] THIRD_GRIP = {0.0F, 4.0F / 16.0F, 0.5F / 16.0F};
    private static final float[] FIRST_GRIP = {1.13F / 16.0F, 3.2F / 16.0F, 1.13F / 16.0F};
    private static final float THIRD_SCALE = 0.85F;
    private static final float FIRST_SCALE = 0.68F;
    private static final float WRIST = 6.0F * SpaceCutSampler.PIXEL;

    private SpaceCutItemMount() {
    }

    public static void drawFirstPerson(
            ItemInHandRenderer renderer,
            AbstractClientPlayer player,
            float partialTick,
            InteractionHand hand,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        SpaceCutFrame frame = SpaceCutScope.current();
        if (renderer == null || player == null || frame == null || poseStack == null || buffers == null) {
            return;
        }
        HumanoidArm arm = armOf(player, hand);
        boolean rightSide = arm == HumanoidArm.RIGHT;
        poseStack.pushPose();
        try {
            float lookPitch = player.getViewXRot(partialTick);
            float lookYaw = player.getViewYRot(partialTick);
            float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            float pitchBob = lookPitch;
            float yawBob = lookYaw;
            if (player instanceof LocalPlayer localPlayer) {
                pitchBob = Mth.lerp(partialTick, localPlayer.xBobO, localPlayer.xBob);
                yawBob = Mth.lerp(partialTick, localPlayer.yBobO, localPlayer.yBob);
            }
            float pitchResidual = (lookPitch - pitchBob) * 0.1F;
            float yawResidual = (lookYaw - yawBob) * 0.1F;
            poseStack.mulPose(Axis.YP.rotationDegrees(-yawResidual));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitchResidual));
            poseStack.mulPose(Axis.XP.rotationDegrees(lookPitch));
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.wrapDegrees(bodyYaw - lookYaw)));
            if (!player.isInvisible()) {
                ResourceLocation skin = player.getSkinTextureLocation();
                boolean slimArms = "slim".equals(player.getModelName());
                PlayerModelPart sleevePart = rightSide ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE;
                SpaceCutArmDraw.drawFirstPersonLimb(
                        poseStack,
                        buffers.getBuffer(RenderType.entityCutoutNoCull(skin)),
                        frame,
                        rightSide,
                        slimArms,
                        player.isModelPartShown(sleevePart),
                        packedLight,
                        OverlayTexture.NO_OVERLAY);
            }
            InteractionHand sourceHand = swordHand(player);
            if (stack != null && !stack.isEmpty() && sourceHand != hand) {
                drawWrist(renderer, player, frame, rightSide, stack, true, poseStack, buffers, packedLight);
            }
            if (rightSide && sourceHand != null) {
                ItemStack mounted = sourceHand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
                drawSword(renderer, player, frame, mounted, true, poseStack, buffers, packedLight);
            }
        } finally {
            poseStack.popPose();
        }
    }

    public static boolean redirectThirdPerson(
            ItemInHandRenderer renderer,
            LivingEntity entity,
            ItemStack stack,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        SpaceCutFrame frame = SpaceCutScope.current();
        if (renderer == null || frame == null || !(entity instanceof Player player) || arm == null) {
            return false;
        }
        if (!player.getUUID().equals(frame.ownerId())) {
            return false;
        }
        boolean rightSide = arm == HumanoidArm.RIGHT;
        InteractionHand sourceHand = swordHand(player);
        HumanoidArm sourceArm = sourceHand == null ? null : armOf(player, sourceHand);
        if (stack != null && !stack.isEmpty() && sourceArm != arm) {
            drawWrist(renderer, player, frame, rightSide, stack, false, poseStack, buffers, packedLight);
        }
        if (rightSide && sourceHand != null) {
            ItemStack mounted = sourceHand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
            drawSword(renderer, player, frame, mounted, false, poseStack, buffers, packedLight);
        }
        return true;
    }

    private static void drawSword(
            ItemInHandRenderer renderer,
            Player player,
            SpaceCutFrame frame,
            ItemStack mounted,
            boolean cameraSpace,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        if (mounted == null || mounted.isEmpty() || frame.swordHand() == null) {
            return;
        }
        OpenMatrix4f mount = mountMatrix(frame, cameraSpace, cameraSpace);
        if (mount == null) {
            return;
        }
        poseStack.pushPose();
        try {
            SpaceCutMatrices.apply(poseStack, mount);
            renderer.renderItem(
                    player,
                    mounted,
                    cameraSpace ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    false,
                    poseStack,
                    buffers,
                    packedLight);
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawWrist(
            ItemInHandRenderer renderer,
            Player player,
            SpaceCutFrame frame,
            boolean rightSide,
            ItemStack stack,
            boolean cameraSpace,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        SpaceCutFrame.SpaceCutBone forearm = rightSide ? frame.forearmRight() : frame.forearmLeft();
        if (forearm == null) {
            return;
        }
        OpenMatrix4f placed = cameraSpace ? SpaceCutMatrices.placeCamera(forearm) : SpaceCutMatrices.placeModel(forearm);
        ItemDisplayContext display = wristContext(cameraSpace, rightSide);
        poseStack.pushPose();
        try {
            SpaceCutMatrices.apply(poseStack, placed);
            poseStack.translate(0.0F, WRIST, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(rightSide ? 0.0625F : -0.0625F, 0.125F, -0.625F);
            renderer.renderItem(player, stack, display, !rightSide, poseStack, buffers, packedLight);
        } finally {
            poseStack.popPose();
        }
    }

    private static ItemDisplayContext wristContext(boolean cameraSpace, boolean rightSide) {
        if (cameraSpace) {
            return rightSide ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }
        return rightSide ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    private static OpenMatrix4f mountMatrix(SpaceCutFrame frame, boolean cameraSpace, boolean firstPersonDisplay) {
        float bladeX = frame.bladeX();
        float bladeY = frame.bladeY();
        float bladeZ = frame.bladeZ();
        OpenMatrix4f sword = frame.swordHand().rotation();
        float sideX = sword.m00;
        float sideY = sword.m01;
        float sideZ = sword.m02;
        if (cameraSpace) {
            float[] blade = new float[3];
            float[] side = new float[3];
            SpaceCutMatrices.flipCamera(bladeX, bladeY, bladeZ, blade);
            SpaceCutMatrices.flipCamera(sideX, sideY, sideZ, side);
            bladeX = blade[0];
            bladeY = blade[1];
            bladeZ = blade[2];
            sideX = side[0];
            sideY = side[1];
            sideZ = side[2];
        }
        float bladeLength = length(bladeX, bladeY, bladeZ);
        if (!(bladeLength > 1.0E-4F)) {
            return null;
        }
        bladeX /= bladeLength;
        bladeY /= bladeLength;
        bladeZ /= bladeLength;
        float along = sideX * bladeX + sideY * bladeY + sideZ * bladeZ;
        sideX -= along * bladeX;
        sideY -= along * bladeY;
        sideZ -= along * bladeZ;
        float sideLength = length(sideX, sideY, sideZ);
        if (!(sideLength > 1.0E-4F)) {
            return null;
        }
        sideX /= sideLength;
        sideY /= sideLength;
        sideZ /= sideLength;
        float thirdX = sideY * bladeZ - sideZ * bladeY;
        float thirdY = sideZ * bladeX - sideX * bladeZ;
        float thirdZ = sideX * bladeY - sideY * bladeX;
        float[] sourceSide = firstPersonDisplay ? FIRST_EDGE : THIRD_EDGE;
        float[] sourceBlade = firstPersonDisplay ? FIRST_BLADE : THIRD_BLADE;
        float sourceThirdX = sourceSide[1] * sourceBlade[2] - sourceSide[2] * sourceBlade[1];
        float sourceThirdY = sourceSide[2] * sourceBlade[0] - sourceSide[0] * sourceBlade[2];
        float sourceThirdZ = sourceSide[0] * sourceBlade[1] - sourceSide[1] * sourceBlade[0];
        float[] targetSide = {sideX, sideY, sideZ};
        float[] targetBlade = {bladeX, bladeY, bladeZ};
        float[] targetThird = {thirdX, thirdY, thirdZ};
        float[] sourceThird = {sourceThirdX, sourceThirdY, sourceThirdZ};
        OpenMatrix4f mount = new OpenMatrix4f();
        writeMapped(mount, 0, targetSide, targetBlade, targetThird, sourceSide, sourceBlade, sourceThird);
        writeMapped(mount, 1, targetSide, targetBlade, targetThird, sourceSide, sourceBlade, sourceThird);
        writeMapped(mount, 2, targetSide, targetBlade, targetThird, sourceSide, sourceBlade, sourceThird);
        float[] displayShift = firstPersonDisplay ? FIRST_GRIP : THIRD_GRIP;
        float displayScale = firstPersonDisplay ? FIRST_SCALE : THIRD_SCALE;
        float gripOnEdge = GRIP_CENTER_X * MESH_EDGE_X + GRIP_CENTER_Y * MESH_EDGE_Y + GRIP_CENTER_Z * MESH_EDGE_Z;
        float gripOnBlade = GRIP_CENTER_X * MESH_BLADE_X + GRIP_CENTER_Y * MESH_BLADE_Y + GRIP_CENTER_Z * MESH_BLADE_Z;
        float gripOnFlat = GRIP_CENTER_X * MESH_FLAT_X + GRIP_CENTER_Y * MESH_FLAT_Y + GRIP_CENTER_Z * MESH_FLAT_Z;
        float carriedX = mount.m00 * displayShift[0] + mount.m10 * displayShift[1] + mount.m20 * displayShift[2];
        float carriedY = mount.m01 * displayShift[0] + mount.m11 * displayShift[1] + mount.m21 * displayShift[2];
        float carriedZ = mount.m02 * displayShift[0] + mount.m12 * displayShift[1] + mount.m22 * displayShift[2];
        carriedX += displayScale * (gripOnEdge * targetSide[0] + gripOnBlade * targetBlade[0] + gripOnFlat * targetThird[0]);
        carriedY += displayScale * (gripOnEdge * targetSide[1] + gripOnBlade * targetBlade[1] + gripOnFlat * targetThird[1]);
        carriedZ += displayScale * (gripOnEdge * targetSide[2] + gripOnBlade * targetBlade[2] + gripOnFlat * targetThird[2]);
        SpaceCutFrame.SpaceCutBone hand = frame.swordHand();
        if (cameraSpace) {
            float[] camera = SpaceCutMatrices.cameraPoint(hand.pivotX(), hand.pivotY(), hand.pivotZ());
            mount.m30 = camera[0] - carriedX;
            mount.m31 = camera[1] - carriedY;
            mount.m32 = camera[2] - carriedZ;
        } else {
            mount.m30 = hand.pivotX() * SpaceCutSampler.PIXEL - carriedX;
            mount.m31 = hand.pivotY() * SpaceCutSampler.PIXEL - carriedY;
            mount.m32 = hand.pivotZ() * SpaceCutSampler.PIXEL - carriedZ;
        }
        return mount;
    }

    private static void writeMapped(
            OpenMatrix4f mount,
            int row,
            float[] targetSide,
            float[] targetBlade,
            float[] targetThird,
            float[] sourceSide,
            float[] sourceBlade,
            float[] sourceThird) {
        // 这一行把展示空间里的刃宽、斜向刀身和 +Z 法线送上 SwordHand。renderItem 再乘一次展示旋转后，刀轴只剩一次。
        float columnX = targetSide[row] * sourceSide[0] + targetBlade[row] * sourceBlade[0] + targetThird[row] * sourceThird[0];
        float columnY = targetSide[row] * sourceSide[1] + targetBlade[row] * sourceBlade[1] + targetThird[row] * sourceThird[1];
        float columnZ = targetSide[row] * sourceSide[2] + targetBlade[row] * sourceBlade[2] + targetThird[row] * sourceThird[2];
        if (row == 0) {
            mount.m00 = columnX;
            mount.m10 = columnY;
            mount.m20 = columnZ;
        } else if (row == 1) {
            mount.m01 = columnX;
            mount.m11 = columnY;
            mount.m21 = columnZ;
        } else {
            mount.m02 = columnX;
            mount.m12 = columnY;
            mount.m22 = columnZ;
        }
    }

    private static InteractionHand swordHand(Player player) {
        if (isSword(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (isSword(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isSword(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
    }

    private static HumanoidArm armOf(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return player.getMainArm();
        }
        return player.getMainArm().getOpposite();
    }

    private static float length(float coordinateX, float coordinateY, float coordinateZ) {
        return (float) Math.sqrt(coordinateX * coordinateX + coordinateY * coordinateY + coordinateZ * coordinateZ);
    }
}
