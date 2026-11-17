package com.bslx.XueXiMod.client.action.skinned.attachments;

import com.bslx.XueXiMod.mixin.client.skin.PlayerSkinOverlayAccessor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/** 只认原版玩家部件。耳朵和未知部件返回空，调用方保留原版附着。 */
final class PlayerPartRegions {
    private PlayerPartRegions() {
    }

    static ArmorRegion regionOf(HumanoidModel<?> model, ModelPart part) {
        if (model == null || part == null) {
            return null;
        }
        if (part == model.head || part == model.hat) {
            return ArmorRegion.HEAD;
        }
        if (part == model.body) {
            return ArmorRegion.TORSO;
        }
        if (part == model.rightArm) {
            return ArmorRegion.ARM_RIGHT;
        }
        if (part == model.leftArm) {
            return ArmorRegion.ARM_LEFT;
        }
        if (part == model.rightLeg) {
            return ArmorRegion.LEG_RIGHT;
        }
        if (part == model.leftLeg) {
            return ArmorRegion.LEG_LEFT;
        }
        if (!(model instanceof PlayerModel<?> playerModel) || !(playerModel instanceof PlayerSkinOverlayAccessor overlay)) {
            return null;
        }
        if (part == overlay.xuexiJacket() || part == overlay.xuexiCloak()) {
            return ArmorRegion.TORSO;
        }
        if (part == overlay.xuexiRightSleeve()) {
            return ArmorRegion.ARM_RIGHT;
        }
        if (part == overlay.xuexiLeftSleeve()) {
            return ArmorRegion.ARM_LEFT;
        }
        if (part == overlay.xuexiRightPants()) {
            return ArmorRegion.LEG_RIGHT;
        }
        if (part == overlay.xuexiLeftPants()) {
            return ArmorRegion.LEG_LEFT;
        }
        return null;
    }
}
