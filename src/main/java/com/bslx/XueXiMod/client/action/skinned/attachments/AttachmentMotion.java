package com.bslx.XueXiMod.client.action.skinned.attachments;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.client.action.skinned.AttachmentTransform;
import com.bslx.XueXiMod.client.action.skinned.ImmutableSkinFrame;

/**
 * 附件局部矩阵。握点校正默认是单位阵：原版层在 translateToHand 之后仍会应用自己的物品朝向。
 * 测试可以传入别的握点，确认它只乘一次。
 */
final class AttachmentMotion {
    static final OpenMatrix4f HELD_GRIP = new OpenMatrix4f();

    private AttachmentMotion() {
    }

    static OpenMatrix4f attachmentLocal(
            ImmutableSkinFrame frame,
            BindJointTable table,
            String jointName,
            OpenMatrix4f restTarget) {
        if (frame == null || table == null) {
            throw new SkinDiagnostic("attachment", "帧或绑定缺失");
        }
        OpenMatrix4f currentGlobal = frame.copyGlobalFrame(jointName);
        OpenMatrix4f bindGlobal = table.global(jointName);
        OpenMatrix4f correction = AttachmentTransform.runtimeCorrection(bindGlobal, frame.copySourceToView(), restTarget);
        return AttachmentTransform.localDraw(frame.copyAlignment(), frame.copySourceToView(), currentGlobal, correction);
    }

    static OpenMatrix4f heldLocal(
            ImmutableSkinFrame frame,
            BindJointTable table,
            boolean leftSide,
            OpenMatrix4f armRest,
            OpenMatrix4f grip) {
        String toolName = leftSide ? "Tool_L" : "Tool_R";
        String handName = leftSide ? "Hand_L" : "Hand_R";
        String jointName = table.has(toolName) ? toolName : handName;
        OpenMatrix4f restTarget = OpenMatrix4f.mul(armRest, grip == null ? HELD_GRIP : grip, null);
        return attachmentLocal(frame, table, jointName, restTarget);
    }

    static OpenMatrix4f chestLocal(ImmutableSkinFrame frame, BindJointTable table) {
        return attachmentLocal(frame, table, "Chest", new OpenMatrix4f());
    }

    static OpenMatrix4f stuckLocal(
            ImmutableSkinFrame frame,
            BindJointTable table,
            ArmorRegion region,
            OpenMatrix4f partRest,
            float localX,
            float localY,
            float localZ) {
        OpenMatrix4f localShift = OpenMatrix4f.createTranslation(localX, localY, localZ);
        OpenMatrix4f restTarget = OpenMatrix4f.mul(partRest, localShift, null);
        Vec3f modelPoint = OpenMatrix4f.transform3v(restTarget, new Vec3f(0.0F, 0.0F, 0.0F), null);
        OpenMatrix4f sourceFromModel = OpenMatrix4f.invert(AttachmentTransform.sourceToBody(), null);
        Vec3f sourcePoint = OpenMatrix4f.transform3v(sourceFromModel, modelPoint, null);
        String jointName = dominantJoint(region, sourcePoint, table);
        return attachmentLocal(frame, table, jointName, restTarget);
    }

    static String dominantJoint(ArmorRegion region, Vec3f sourcePoint, BindJointTable table) {
        ArmorWeights.Influences influences = ArmorWeights.weigh(
                region,
                sourcePoint.coordinateX,
                sourcePoint.coordinateY,
                sourcePoint.coordinateZ,
                table,
                0.0F,
                1.0F);
        int bestSlot = 0;
        for (int weightIndex = 1; weightIndex < influences.weights.length; weightIndex++) {
            if (influences.weights[weightIndex] > influences.weights[bestSlot]) {
                bestSlot = weightIndex;
            }
        }
        int jointId = influences.jointIds[bestSlot];
        String[] names = regionJoints(region);
        for (String jointName : names) {
            if (table.jointId(jointName) == jointId) {
                return jointName;
            }
        }
        return names[0];
    }

    private static String[] regionJoints(ArmorRegion region) {
        if (region == ArmorRegion.HEAD) {
            return new String[] {"Head"};
        }
        if (region == ArmorRegion.TORSO) {
            return new String[] {"Torso", "Chest"};
        }
        if (region == ArmorRegion.ARM_RIGHT) {
            return new String[] {"Arm_R", "Hand_R", "Elbow_R"};
        }
        if (region == ArmorRegion.ARM_LEFT) {
            return new String[] {"Arm_L", "Hand_L", "Elbow_L"};
        }
        if (region == ArmorRegion.LEG_RIGHT) {
            return new String[] {"Thigh_R", "Leg_R", "Knee_R"};
        }
        return new String[] {"Thigh_L", "Leg_L", "Knee_L"};
    }
}
