package com.bslx.XueXiMod.client.action.cooperative;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

/**
 * 把确认曲线写进共享人体模型。根偏航只转模型，不移动实体脚点，也不改相机。
 * Blockbench 的 Y 朝上、面向 -Z；模型部件在 Y 朝下的空间里使用相反的三个欧拉角。
 * 根偏航不经过矩阵反解，完整 360 度曲线按原角度交给渲染栈。
 */
public final class CooperativePoseApplier {
    private static final float ARM_OUTSET = 5.0F;
    private static final float ARM_DROP = 2.0F;
    private static final float LEG_OUTSET = 1.9F;
    private static final float LEG_DROP = 12.0F;
    private static HumanoidModel<?> pendingModel;
    private static float pendingSpinRadians;

    private CooperativePoseApplier() {
    }

    public static float rootSpinRadians(CooperativePose pose) {
        if (pose == null) {
            return 0.0F;
        }
        return (float) Math.toRadians(-pose.rootYawDegrees() + pose.facingYawDegrees());
    }

    public static void apply(HumanoidModel<?> model, LivingEntity living, float partialTick) {
        if (model == null) {
            return;
        }
        CooperativePose pose = CooperativeActionViews.pose(living, partialTick);
        if (pose == null) {
            discardPending(model);
            return;
        }
        pendingModel = model;
        pendingSpinRadians = rootSpinRadians(pose);
        place(model.body, 0.0F, 0.0F, 0.0F, pose.bodyX(), pose.bodyY(), pose.bodyZ());
        place(model.head, 0.0F, 0.0F, 0.0F, pose.headX(), pose.headY(), pose.headZ());
        place(model.hat, 0.0F, 0.0F, 0.0F, pose.headX(), pose.headY(), pose.headZ());
        place(model.rightArm, -ARM_OUTSET, ARM_DROP, 0.0F, pose.armRightX(), pose.armRightY(), pose.armRightZ());
        place(model.leftArm, ARM_OUTSET, ARM_DROP, 0.0F, pose.armLeftX(), pose.armLeftY(), pose.armLeftZ());
        place(model.rightLeg, -LEG_OUTSET, LEG_DROP, 0.0F, pose.legRightX(), pose.legRightY(), pose.legRightZ());
        place(model.leftLeg, LEG_OUTSET, LEG_DROP, 0.0F, pose.legLeftX(), pose.legLeftY(), pose.legLeftZ());
        if (model instanceof PlayerModel<?> playerModel) {
            copyPart(playerModel.body, playerModel.jacket);
            copyPart(playerModel.rightArm, playerModel.rightSleeve);
            copyPart(playerModel.leftArm, playerModel.leftSleeve);
            copyPart(playerModel.rightLeg, playerModel.rightPants);
            copyPart(playerModel.leftLeg, playerModel.leftPants);
        }
    }

    public static void spinSubmittedModel(Object rendered, PoseStack poseStack) {
        if (pendingModel == null || rendered != pendingModel || poseStack == null) {
            return;
        }
        pendingModel = null;
        poseStack.mulPose(Axis.YP.rotation(pendingSpinRadians));
    }

    public static void discardPending(Object rendered) {
        if (pendingModel == rendered) {
            pendingModel = null;
        }
    }

    private static void place(
            ModelPart part,
            float bindAcross,
            float bindUp,
            float bindAhead,
            float sourceX,
            float sourceY,
            float sourceZ) {
        if (part == null) {
            return;
        }
        part.x = bindAcross;
        part.y = bindUp;
        part.z = bindAhead;
        part.xRot = (float) Math.toRadians(-sourceX);
        part.yRot = (float) Math.toRadians(-sourceY);
        part.zRot = (float) Math.toRadians(-sourceZ);
    }

    private static void copyPart(ModelPart source, ModelPart destination) {
        if (source == null || destination == null) {
            return;
        }
        destination.x = source.x;
        destination.y = source.y;
        destination.z = source.z;
        destination.xRot = source.xRot;
        destination.yRot = source.yRot;
        destination.zRot = source.zRot;
    }
}
