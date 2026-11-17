package com.bslx.XueXiMod.client.action.cooperative;

import org.joml.Quaternionf;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * 手持物四元数。第一人称确认姿态由 {@link CooperativeFirstPerson} 画手臂和握柄。
 * 这里只在仍走到原版手持物变换时兜底，不另生成物品实体。
 */
public final class CooperativeGrip {
    private CooperativeGrip() {
    }

    public static Quaternionf heldTurn(Player player, InteractionHand hand, float partialTick) {
        CooperativePose pose = CooperativeActionViews.pose(player, partialTick);
        if (pose == null || hand == null) {
            return null;
        }
        float sourceX = hand == InteractionHand.MAIN_HAND ? pose.armRightX() : pose.armLeftX();
        float sourceY = hand == InteractionHand.MAIN_HAND ? pose.armRightY() : pose.armLeftY();
        float sourceZ = hand == InteractionHand.MAIN_HAND ? pose.armRightZ() : pose.armLeftZ();
        float spinRadians = (float) Math.toRadians(-pose.rootYawDegrees() + pose.facingYawDegrees());
        return new Quaternionf()
                .rotateZ((float) Math.toRadians(-sourceZ))
                .rotateY((float) Math.toRadians(-sourceY) + spinRadians)
                .rotateX((float) Math.toRadians(-sourceX));
    }
}
