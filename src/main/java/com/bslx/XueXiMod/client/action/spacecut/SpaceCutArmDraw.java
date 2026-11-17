package com.bslx.XueXiMod.client.action.spacecut;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;

/**
 * 用玩家皮肤或护甲贴图画上臂和前臂各 6 像素。立方体 UV 与 1.20.1 {@code ModelPart.Cube} 一致。
 * 只在左腰右拔这一次绘制里替换单节手臂。
 */
public final class SpaceCutArmDraw {
    private static final float SEGMENT = 6.0F;

    private SpaceCutArmDraw() {
    }

    public static void append(
            Object rendered,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float redAmount,
            float greenAmount,
            float blueAmount,
            float alphaAmount) {
        SpaceCutFrame frame = SpaceCutScope.current();
        if (frame == null || rendered == null || rendered != frame.renderedModel() || poseStack == null || consumer == null) {
            return;
        }
        int redByte = colorByte(redAmount);
        int greenByte = colorByte(greenAmount);
        int blueByte = colorByte(blueAmount);
        int alphaByte = colorByte(alphaAmount);
        drawSkinLimb(poseStack, consumer, frame, true, false, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        drawSkinLimb(poseStack, consumer, frame, false, false, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
    }

    public static boolean armorReplaces(Object model) {
        if (!(model instanceof HumanoidModel<?> humanoid) || model.getClass() != HumanoidModel.class) {
            return false;
        }
        if (SpaceCutScope.current() == null) {
            return false;
        }
        boolean rightShown = humanoid.rightArm != null && humanoid.rightArm.visible;
        boolean leftShown = humanoid.leftArm != null && humanoid.leftArm.visible;
        return rightShown || leftShown;
    }

    public static void armorLimbs(
            HumanoidModel<?> humanoid,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float redAmount,
            float greenAmount,
            float blueAmount,
            float alphaAmount) {
        SpaceCutFrame frame = SpaceCutScope.current();
        if (frame == null || humanoid == null || poseStack == null || consumer == null) {
            return;
        }
        int redByte = colorByte(redAmount);
        int greenByte = colorByte(greenAmount);
        int blueByte = colorByte(blueAmount);
        int alphaByte = colorByte(alphaAmount);
        if (humanoid.rightArm != null && humanoid.rightArm.visible) {
            drawArmorLimb(poseStack, consumer, frame, true, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        }
        if (humanoid.leftArm != null && humanoid.leftArm.visible) {
            drawArmorLimb(poseStack, consumer, frame, false, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        }
    }

    public static void drawFirstPersonLimb(
            PoseStack poseStack,
            VertexConsumer consumer,
            SpaceCutFrame frame,
            boolean rightSide,
            boolean slimArms,
            boolean sleeveShown,
            int packedLight,
            int packedOverlay) {
        if (poseStack == null || consumer == null || frame == null) {
            return;
        }
        drawSegments(
                poseStack,
                consumer,
                rightSide ? frame.armRight() : frame.armLeft(),
                rightSide ? frame.forearmRight() : frame.forearmLeft(),
                true,
                rightSide ? 40.0F : 32.0F,
                rightSide ? 16.0F : 48.0F,
                slimArms ? 3.0F : 4.0F,
                4.0F,
                0.0F,
                false,
                64.0F,
                64.0F,
                255,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
        if (!sleeveShown) {
            return;
        }
        drawSegments(
                poseStack,
                consumer,
                rightSide ? frame.armRight() : frame.armLeft(),
                rightSide ? frame.forearmRight() : frame.forearmLeft(),
                true,
                rightSide ? 40.0F : 48.0F,
                rightSide ? 32.0F : 48.0F,
                slimArms ? 3.0F : 4.0F,
                4.0F,
                0.25F,
                false,
                64.0F,
                64.0F,
                255,
                255,
                255,
                255,
                packedLight,
                packedOverlay);
    }

    private static void drawSkinLimb(
            PoseStack poseStack,
            VertexConsumer consumer,
            SpaceCutFrame frame,
            boolean rightSide,
            boolean cameraSpace,
            int redByte,
            int greenByte,
            int blueByte,
            int alphaByte,
            int packedLight,
            int packedOverlay) {
        float widthPixels = frame.slimArms() ? 3.0F : 4.0F;
        drawSegments(
                poseStack,
                consumer,
                rightSide ? frame.armRight() : frame.armLeft(),
                rightSide ? frame.forearmRight() : frame.forearmLeft(),
                cameraSpace,
                rightSide ? 40.0F : 32.0F,
                rightSide ? 16.0F : 48.0F,
                widthPixels,
                4.0F,
                0.0F,
                false,
                64.0F,
                64.0F,
                redByte,
                greenByte,
                blueByte,
                alphaByte,
                packedLight,
                packedOverlay);
        boolean sleeveShown = rightSide ? frame.rightSleeveShown() : frame.leftSleeveShown();
        if (!sleeveShown) {
            return;
        }
        drawSegments(
                poseStack,
                consumer,
                rightSide ? frame.armRight() : frame.armLeft(),
                rightSide ? frame.forearmRight() : frame.forearmLeft(),
                cameraSpace,
                rightSide ? 40.0F : 48.0F,
                rightSide ? 32.0F : 48.0F,
                widthPixels,
                4.0F,
                0.25F,
                false,
                64.0F,
                64.0F,
                redByte,
                greenByte,
                blueByte,
                alphaByte,
                packedLight,
                packedOverlay);
    }

    private static void drawArmorLimb(
            PoseStack poseStack,
            VertexConsumer consumer,
            SpaceCutFrame frame,
            boolean rightSide,
            int redByte,
            int greenByte,
            int blueByte,
            int alphaByte,
            int packedLight,
            int packedOverlay) {
        drawSegments(
                poseStack,
                consumer,
                rightSide ? frame.armRight() : frame.armLeft(),
                rightSide ? frame.forearmRight() : frame.forearmLeft(),
                false,
                40.0F,
                16.0F,
                4.0F,
                4.0F,
                1.0F,
                !rightSide,
                64.0F,
                32.0F,
                redByte,
                greenByte,
                blueByte,
                alphaByte,
                packedLight,
                packedOverlay);
    }

    private static void drawSegments(
            PoseStack poseStack,
            VertexConsumer consumer,
            SpaceCutFrame.SpaceCutBone upper,
            SpaceCutFrame.SpaceCutBone lower,
            boolean cameraSpace,
            float texU,
            float texV,
            float widthPixels,
            float depthPixels,
            float inflatePixels,
            boolean mirror,
            float textureWidth,
            float textureHeight,
            int redByte,
            int greenByte,
            int blueByte,
            int alphaByte,
            int packedLight,
            int packedOverlay) {
        emitSegment(poseStack, consumer, upper, cameraSpace, texU, texV, widthPixels, depthPixels, inflatePixels,
                mirror, 0.0F, textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        emitSegment(poseStack, consumer, lower, cameraSpace, texU, texV, widthPixels, depthPixels, inflatePixels,
                mirror, SEGMENT, textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
    }

    private static void emitSegment(
            PoseStack poseStack,
            VertexConsumer consumer,
            SpaceCutFrame.SpaceCutBone bone,
            boolean cameraSpace,
            float texU,
            float texV,
            float widthPixels,
            float depthPixels,
            float inflatePixels,
            boolean mirror,
            float sideBias,
            float textureWidth,
            float textureHeight,
            int redByte,
            int greenByte,
            int blueByte,
            int alphaByte,
            int packedLight,
            int packedOverlay) {
        if (bone == null) {
            return;
        }
        OpenMatrix4f placed = cameraSpace ? SpaceCutMatrices.placeCamera(bone) : SpaceCutMatrices.placeModel(bone);
        poseStack.pushPose();
        try {
            SpaceCutMatrices.apply(poseStack, placed);
            emitBox(poseStack.last(), consumer, texU, texV, widthPixels, depthPixels, inflatePixels, mirror, sideBias,
                    textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        } finally {
            poseStack.popPose();
        }
    }

    private static void emitBox(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float texU,
            float texV,
            float widthPixels,
            float depthPixels,
            float inflatePixels,
            boolean mirror,
            float sideBias,
            float textureWidth,
            float textureHeight,
            int redByte,
            int greenByte,
            int blueByte,
            int alphaByte,
            int packedLight,
            int packedOverlay) {
        float lowX = -widthPixels * 0.5F - inflatePixels;
        float highX = widthPixels * 0.5F + inflatePixels;
        float lowY = -inflatePixels;
        float highY = SEGMENT + inflatePixels;
        float lowZ = -depthPixels * 0.5F - inflatePixels;
        float highZ = depthPixels * 0.5F + inflatePixels;
        if (mirror) {
            float swapX = lowX;
            lowX = highX;
            highX = swapX;
        }
        float columnWest = texU;
        float columnNorth = texU + depthPixels;
        float columnEast = texU + depthPixels + widthPixels;
        float columnUp = texU + depthPixels + widthPixels + widthPixels;
        float columnSouth = texU + depthPixels + widthPixels + depthPixels;
        float columnEnd = texU + depthPixels + widthPixels + depthPixels + widthPixels;
        float capTop = texV;
        float capBottom = texV + depthPixels;
        float sideTop = texV + depthPixels + sideBias;
        float sideBottom = texV + depthPixels + SEGMENT + sideBias;
        face(pose, consumer, highX, lowY, highZ, lowX, lowY, highZ, lowX, lowY, lowZ, highX, lowY, lowZ,
                columnNorth, capTop, columnEast, capBottom, 0.0F, -1.0F, 0.0F, mirror,
                textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        face(pose, consumer, highX, highY, lowZ, lowX, highY, lowZ, lowX, highY, highZ, highX, highY, highZ,
                columnEast, capBottom, columnUp, capTop, 0.0F, 1.0F, 0.0F, mirror,
                textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        face(pose, consumer, lowX, lowY, lowZ, lowX, lowY, highZ, lowX, highY, highZ, lowX, highY, lowZ,
                columnWest, sideTop, columnNorth, sideBottom, -1.0F, 0.0F, 0.0F, mirror,
                textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        face(pose, consumer, highX, lowY, lowZ, lowX, lowY, lowZ, lowX, highY, lowZ, highX, highY, lowZ,
                columnNorth, sideTop, columnEast, sideBottom, 0.0F, 0.0F, -1.0F, mirror,
                textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        face(pose, consumer, highX, lowY, highZ, highX, lowY, lowZ, highX, highY, lowZ, highX, highY, highZ,
                columnEast, sideTop, columnSouth, sideBottom, 1.0F, 0.0F, 0.0F, mirror,
                textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        face(pose, consumer, lowX, lowY, highZ, highX, lowY, highZ, highX, highY, highZ, lowX, highY, highZ,
                columnSouth, sideTop, columnEnd, sideBottom, 0.0F, 0.0F, 1.0F, mirror,
                textureWidth, textureHeight, redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
    }

    private static void face(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float firstX, float firstY, float firstZ,
            float secondX, float secondY, float secondZ,
            float thirdX, float thirdY, float thirdZ,
            float fourthX, float fourthY, float fourthZ,
            float uLeft, float vTop, float uRight, float vBottom,
            float normalX, float normalY, float normalZ,
            boolean mirror,
            float textureWidth, float textureHeight,
            int redByte, int greenByte, int blueByte, int alphaByte,
            int packedLight, int packedOverlay) {
        float left = uLeft / textureWidth;
        float right = uRight / textureWidth;
        float top = vTop / textureHeight;
        float bottom = vBottom / textureHeight;
        if (mirror) {
            corner(pose, consumer, fourthX, fourthY, fourthZ, right, bottom, -normalX, normalY, normalZ,
                    redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
            corner(pose, consumer, thirdX, thirdY, thirdZ, left, bottom, -normalX, normalY, normalZ,
                    redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
            corner(pose, consumer, secondX, secondY, secondZ, left, top, -normalX, normalY, normalZ,
                    redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
            corner(pose, consumer, firstX, firstY, firstZ, right, top, -normalX, normalY, normalZ,
                    redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
            return;
        }
        corner(pose, consumer, firstX, firstY, firstZ, right, top, normalX, normalY, normalZ,
                redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        corner(pose, consumer, secondX, secondY, secondZ, left, top, normalX, normalY, normalZ,
                redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        corner(pose, consumer, thirdX, thirdY, thirdZ, left, bottom, normalX, normalY, normalZ,
                redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
        corner(pose, consumer, fourthX, fourthY, fourthZ, right, bottom, normalX, normalY, normalZ,
                redByte, greenByte, blueByte, alphaByte, packedLight, packedOverlay);
    }

    private static void corner(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float pixelX, float pixelY, float pixelZ,
            float textureU, float textureV,
            float normalX, float normalY, float normalZ,
            int redByte, int greenByte, int blueByte, int alphaByte,
            int packedLight, int packedOverlay) {
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        consumer.vertex(poseMatrix, pixelX * SpaceCutSampler.PIXEL, pixelY * SpaceCutSampler.PIXEL, pixelZ * SpaceCutSampler.PIXEL)
                .color(redByte, greenByte, blueByte, alphaByte)
                .uv(textureU, textureV)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }

    private static int colorByte(float amount) {
        float clamped = Mth.clamp(amount, 0.0F, 1.0F);
        return Math.round(clamped * 255.0F);
    }
}
