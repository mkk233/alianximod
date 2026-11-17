package com.bslx.XueXiMod.client.action.skinned;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.client.action.mesh.MeshRenderView;
import com.bslx.XueXiMod.client.action.mesh.PartVisibility;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** 只替换当前第三人称 scope 里已经定为 SKINNED 的身体。其余调用原样一次。 */
public final class SkinnedBodyDraw {
    private SkinnedBodyDraw() {
    }

    public static boolean replace(
            EntityModel<?> rendered,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        SkinSession session = SkinSession.top();
        if (session == null || !session.drawsBody(rendered)) {
            return false;
        }
        PlayerModel<?> playerModel = (PlayerModel<?>) rendered;
        session.installBody(playerModel);
        if (session.frame() == null) {
            return false;
        }
        Player wearer = session.player() instanceof Player player ? player : null;
        PartVisibility visibility = fromModel(playerModel, wearer);
        MeshRenderView.emit(
                session.frame().mesh(),
                session.frame().palette(),
                session.lease(),
                visibility,
                session.frame().copyMeshPlacement(),
                packedLight,
                packedOverlay,
                new PoseStackCornerSink(poseStack, consumer, red, green, blue, alpha));
        return true;
    }

    /** 读取这次渲染已经写好的 12 个部件可见性。外层还要过玩家开关，模型关掉的部位不再打开。 */
    public static PartVisibility fromModel(PlayerModel<?> model, Player wearer) {
        return fromModel(
                model,
                wearer == null || wearer.isModelPartShown(PlayerModelPart.HAT),
                wearer == null || wearer.isModelPartShown(PlayerModelPart.JACKET),
                wearer == null || wearer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE),
                wearer == null || wearer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE),
                wearer == null || wearer.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG),
                wearer == null || wearer.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG));
    }

    public static PartVisibility fromModel(
            PlayerModel<?> model,
            boolean hatShown,
            boolean jacketShown,
            boolean leftSleeveShown,
            boolean rightSleeveShown,
            boolean leftPantsShown,
            boolean rightPantsShown) {
        PartVisibility visibility = PartVisibility.noneShown();
        if (model == null) {
            return visibility;
        }
        visibility = visibility.with(PlayerMeshPart.HEAD, partVisible(model.head));
        visibility = visibility.with(PlayerMeshPart.HAT, partVisible(model.hat) && hatShown);
        visibility = visibility.with(PlayerMeshPart.TORSO, partVisible(model.body));
        visibility = visibility.with(PlayerMeshPart.JACKET, partVisible(model.jacket) && jacketShown);
        visibility = visibility.with(PlayerMeshPart.RIGHT_ARM, partVisible(model.rightArm));
        visibility = visibility.with(PlayerMeshPart.RIGHT_SLEEVE, partVisible(model.rightSleeve) && rightSleeveShown);
        visibility = visibility.with(PlayerMeshPart.LEFT_ARM, partVisible(model.leftArm));
        visibility = visibility.with(PlayerMeshPart.LEFT_SLEEVE, partVisible(model.leftSleeve) && leftSleeveShown);
        visibility = visibility.with(PlayerMeshPart.RIGHT_LEG, partVisible(model.rightLeg));
        visibility = visibility.with(PlayerMeshPart.RIGHT_PANTS, partVisible(model.rightPants) && rightPantsShown);
        visibility = visibility.with(PlayerMeshPart.LEFT_LEG, partVisible(model.leftLeg));
        visibility = visibility.with(PlayerMeshPart.LEFT_PANTS, partVisible(model.leftPants) && leftPantsShown);
        return visibility;
    }

    public static PartVisibility fromPlayer(Player player) {
        return parts(
                player.isModelPartShown(PlayerModelPart.HAT),
                player.isModelPartShown(PlayerModelPart.JACKET),
                player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE),
                player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE),
                player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG),
                player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG));
    }

    public static PartVisibility parts(
            boolean hat,
            boolean jacket,
            boolean leftSleeve,
            boolean rightSleeve,
            boolean leftPants,
            boolean rightPants) {
        PartVisibility visibility = PartVisibility.allShown();
        visibility = visibility.with(PlayerMeshPart.HAT, hat);
        visibility = visibility.with(PlayerMeshPart.JACKET, jacket);
        visibility = visibility.with(PlayerMeshPart.LEFT_SLEEVE, leftSleeve);
        visibility = visibility.with(PlayerMeshPart.RIGHT_SLEEVE, rightSleeve);
        visibility = visibility.with(PlayerMeshPart.LEFT_PANTS, leftPants);
        visibility = visibility.with(PlayerMeshPart.RIGHT_PANTS, rightPants);
        return visibility;
    }

    private static boolean partVisible(ModelPart part) {
        return part != null && part.visible;
    }

    public static OpenMatrix4f bodyAlignment(OpenMatrix4f spin, float centerX, float centerY, float centerZ) {
        return HandFrameCalibration.around(spin, centerX, centerY, centerZ);
    }
}
