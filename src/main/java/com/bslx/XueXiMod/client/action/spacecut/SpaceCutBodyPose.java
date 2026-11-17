package com.bslx.XueXiMod.client.action.spacecut;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.client.action.VanillaEuler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.Player;

/** 把躯干、头和腿写进原版部件，并藏起单节手臂。分段臂由随后的身体绘制补上。 */
public final class SpaceCutBodyPose {
    private SpaceCutBodyPose() {
    }

    public static boolean apply(PlayerModel<?> model) {
        SpaceCutFrame frame = SpaceCutScope.current();
        if (frame == null || model == null || model.body == null) {
            return false;
        }
        boolean rightSleeve = shown(model.rightSleeve);
        boolean leftSleeve = shown(model.leftSleeve);
        frame.rememberBody(model, slimArms(frame), rightSleeve, leftSleeve);
        float[] neck = neckOf(frame.body());
        place(model.body, neck[0], neck[1], neck[2], frame.body().rotation());
        place(model.head, frame.head());
        copyPart(model.head, model.hat);
        place(model.rightLeg, frame.legRight());
        place(model.leftLeg, frame.legLeft());
        place(model.rightArm, frame.armRight());
        place(model.leftArm, frame.armLeft());
        hide(model.rightArm);
        hide(model.leftArm);
        hide(model.rightSleeve);
        hide(model.leftSleeve);
        return true;
    }

    /** {@code PlayerModel.slim} 是 private。皮肤模型名与 {@link AbstractClientPlayer#getModelName()} 相同。 */
    private static boolean slimArms(SpaceCutFrame frame) {
        Minecraft minecraft = Minecraft.getInstance();
        Player owner = minecraft == null || minecraft.level == null ? null : minecraft.level.getPlayerByUUID(frame.ownerId());
        if (owner instanceof AbstractClientPlayer clientPlayer) {
            return "slim".equals(clientPlayer.getModelName());
        }
        return "slim".equals(DefaultPlayerSkin.getSkinModelName(frame.ownerId()));
    }

    private static float[] neckOf(SpaceCutFrame.SpaceCutBone body) {
        OpenMatrix4f rotation = body.rotation();
        return new float[] {
                body.pivotX() - 12.0F * rotation.m10,
                body.pivotY() - 12.0F * rotation.m11,
                body.pivotZ() - 12.0F * rotation.m12
        };
    }

    private static void place(ModelPart part, SpaceCutFrame.SpaceCutBone bone) {
        if (bone == null) {
            return;
        }
        place(part, bone.pivotX(), bone.pivotY(), bone.pivotZ(), bone.rotation());
    }

    private static void place(ModelPart part, float pivotX, float pivotY, float pivotZ, OpenMatrix4f rotation) {
        if (part == null || rotation == null) {
            return;
        }
        part.x = pivotX;
        part.y = pivotY;
        part.z = pivotZ;
        try {
            VanillaEuler euler = VanillaEuler.from(rotation);
            part.xRot = euler.pitchRadians();
            part.yRot = euler.yawRadians();
            part.zRot = euler.rollRadians();
        } catch (IllegalArgumentException ignored) {
            part.xRot = 0.0F;
            part.yRot = 0.0F;
            part.zRot = 0.0F;
        }
    }

    private static boolean shown(ModelPart part) {
        return part != null && part.visible && !part.skipDraw;
    }

    private static void hide(ModelPart part) {
        if (part != null) {
            part.visible = false;
        }
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
