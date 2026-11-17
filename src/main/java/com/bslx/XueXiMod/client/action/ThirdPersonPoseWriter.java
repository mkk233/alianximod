package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.client.action.spacecut.SpaceCutBodyPose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 在原版 setupAnim 之后连接刚性部件。不调用 resetPose，不改实体字段，不写缩放、立方体或实体位置。
 * LEGACY 路径绕当帧 body 枢轴写六个部件，T = L * D_body，空槽继承 T。
 * UPPER_BODY_SLASH 路径绕腰点写 body、head 和双臂，腿保持原版；手臂只用 D×B，不乘 L。
 */
public final class ThirdPersonPoseWriter {
    private ThirdPersonPoseWriter() {
    }

    static boolean drives(RetargetedPose pose) {
        return pose != null && pose.anyDriven();
    }

    /** 当前栈顶是第三人称，并且模型、玩家都是这次调用时才写入。 */
    public static boolean applyScoped(PlayerModel<?> model, Object player, RetargetedPose pose) {
        if (!RenderPoseScope.allowsThirdPerson(model, player)) {
            return false;
        }
        apply(model, pose);
        return true;
    }

    public static void apply(PlayerModel<?> model, RetargetedPose pose) {
        if (SpaceCutBodyPose.apply(model)) {
            return;
        }
        if (model == null || !drives(pose) || model.body == null) {
            return;
        }
        if (pose.alignmentScope() == SlashAlignmentScope.UPPER_BODY_SLASH) {
            applyUpperBody(model, pose);
            return;
        }
        OpenMatrix4f alignment = pose.worldBasis();
        OpenMatrix4f trunk = MatrixFrames.product(alignment, pose.body());
        float centerAcross = model.body.x;
        float centerUpward = model.body.y;
        float centerAhead = model.body.z;
        placePart(model.body, centerAcross, centerUpward, centerAhead, alignment, pose.body(), trunk, true);
        placePart(model.head, centerAcross, centerUpward, centerAhead, alignment, pose.head(), trunk, false);
        placePart(model.rightArm, centerAcross, centerUpward, centerAhead, alignment, pose.rightArm(), trunk, false);
        placePart(model.leftArm, centerAcross, centerUpward, centerAhead, alignment, pose.leftArm(), trunk, false);
        placePart(model.rightLeg, centerAcross, centerUpward, centerAhead, alignment, pose.rightLeg(), trunk, false);
        placePart(model.leftLeg, centerAcross, centerUpward, centerAhead, alignment, pose.leftLeg(), trunk, false);
        copyPivotAndRotation(model.head, model.hat);
    }

    /**
     * 上身挥斩：旋转中心取腰部，而不是原版 body 枢轴（颈点）。
     * 绕颈点转会让头成为支点、腿被一起甩出去，水刃看着像整个人在翻。
     * 双腿完全不写，保留当帧原版站位。
     * UPPER_BODY_SLASH 不使用挥斩对齐 L：五骨骨架没有武器骨，L 在这里接近常量，
     * 乘到直臂上会把手臂拧向躯干造成穿模。手臂只按剪辑增量 D 和原版基准 B 播放。
     * L 仍保留给第一人称和 LEGACY 路径。
     */
    private static void applyUpperBody(PlayerModel<?> model, RetargetedPose pose) {
        OpenMatrix4f trunk = pose.body();
        // 必须在任何 placePart 改写 model.body 之前采样：腰点由当帧原版枢轴和欧拉角决定。
        float bodyAcross = model.body.x;
        float bodyUpward = model.body.y;
        float bodyAhead = model.body.z;
        OpenMatrix4f vanillaBody = VanillaEuler.zyx(model.body.xRot, model.body.yRot, model.body.zRot);
        // 原版 body 枢轴到腰 12 像素，+Y 向下。
        Vec3f waistOffset = MatrixFrames.apply(vanillaBody, 0.0F, 12.0F, 0.0F);
        float centerAcross = bodyAcross + waistOffset.coordinateX;
        float centerUpward = bodyUpward + waistOffset.coordinateY;
        float centerAhead = bodyAhead + waistOffset.coordinateZ;
        placePart(model.body, centerAcross, centerUpward, centerAhead, null, trunk, trunk, false);
        placePart(model.head, centerAcross, centerUpward, centerAhead, null, pose.head(), trunk, false);
        placePart(model.rightArm, centerAcross, centerUpward, centerAhead,
                null, pose.rightArm(), trunk, false);
        placePart(model.leftArm, centerAcross, centerUpward, centerAhead,
                null, pose.leftArm(), trunk, false);
        copyPivotAndRotation(model.head, model.hat);
    }

    private static void placePart(
            ModelPart part,
            float centerAcross,
            float centerUpward,
            float centerAhead,
            OpenMatrix4f alignment,
            OpenMatrix4f delta,
            OpenMatrix4f trunk,
            boolean bodyPivot) {
        if (part == null) {
            return;
        }
        RigidFallback.Sample vanilla = new RigidFallback.Sample(
                part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
        RigidFallback.Placed placed = RigidFallback.place(
                vanilla, centerAcross, centerUpward, centerAhead, alignment, delta, trunk, bodyPivot);
        part.x = placed.across();
        part.y = placed.upward();
        part.z = placed.ahead();
        part.xRot = placed.pitchRadians();
        part.yRot = placed.yawRadians();
        part.zRot = placed.rollRadians();
    }

    private static void copyPivotAndRotation(ModelPart source, ModelPart destination) {
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
