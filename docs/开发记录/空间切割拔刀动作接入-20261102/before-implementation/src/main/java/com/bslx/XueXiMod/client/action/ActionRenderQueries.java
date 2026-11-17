package com.bslx.XueXiMod.client.action;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;

/** 只读同一客户端时钟上的采样，并决定哪只手接管。不推进播放，不改游戏字段。 */
public final class ActionRenderQueries {
    private ActionRenderQueries() {
    }

    public static ActionSampleContext inspect(Player player, float partialTick) {
        if (player != null) {
            ActionSampleContext frozen = RenderPoseScope.frozenSample(player.getUUID());
            if (frozen != null) {
                return frozen;
            }
        }
        return inspectLive(player, partialTick);
    }

    public static ActionSampleContext inspectLive(Player player, float partialTick) {
        if (player == null) {
            return ActionSampleContext.inactive(null);
        }
        if (player.level() == null) {
            return ActionSampleContext.inactive(player.getUUID());
        }
        float safePartial = Float.isFinite(partialTick) ? partialTick : 0.0F;
        return ActionClientPlayback.inspect(player.getUUID(), player.level().getGameTime(), safePartial);
    }

    public static RetargetedPose thirdPersonPose(Player player, float partialTick) {
        ActionSampleContext context = inspect(player, partialTick);
        if (!context.active()) {
            return RetargetedPose.empty();
        }
        ResourceLocation visualId = context.visualActionId();
        SlashAlignmentScope scope = ActionVisualCatalog.alignmentScope(visualId);
        RetargetedPose local = localPose(context).withAlignmentScope(scope);
        if (!context.hasSlashFrame() || !ActionVisualCatalog.blade(visualId) || ActionVisualCatalog.referenceForward(visualId) == null) {
            return local;
        }
        boolean rightSword = swordArm(player, HumanoidArm.RIGHT) && ActionVisualCatalog.usesHand(visualId, true);
        boolean leftSword = swordArm(player, HumanoidArm.LEFT) && ActionVisualCatalog.usesHand(visualId, false);
        if (!rightSword && !leftSword) {
            return RetargetedPose.empty();
        }
        OpenMatrix4f alignment = SlashPlaneAlign.modelAlignment(
                context.directionX(), context.directionY(), context.directionZ(),
                context.planeX(), context.planeY(), context.planeZ(),
                bodyYawDegrees(player, partialTick),
                RenderSpaceCapture.worldToModel(),
                ActionVisualCatalog.referenceForward(visualId),
                ActionVisualCatalog.referenceNormal(visualId));
        RetargetedPose aimed = RigidPoseRetarget.aim(local, alignment);
        if (!rightSword) {
            aimed = aimed.withoutRightArm();
        }
        if (!leftSword) {
            aimed = aimed.withoutLeftArm();
        }
        return aimed;
    }

    public static float partialTick(LivingEntity living, float ageInTicks) {
        float elapsed = living == null ? 0.0F : ageInTicks - living.tickCount;
        float fallback = elapsed >= 0.0F && elapsed <= 1.0F ? elapsed : 0.0F;
        return ActionRenderClock.partialOr(fallback);
    }

    /**
     * 与 thirdPersonPose 的手臂矩阵一致：usesHand、主副手和 useOwns 都先反映在那个姿态里。
     * 不按术式 id 单独分支。非玩家模型、没有活动、挥砍手未被接管时保持原 attackTime。
     */
    public static boolean suppressVanillaSwing(EntityModel<?> model, LivingEntity living, float ageInTicks) {
        if (!(model instanceof PlayerModel) || !(living instanceof Player player)) {
            return false;
        }
        RetargetedPose pose = thirdPersonPose(player, partialTick(player, ageInTicks));
        return ownedSwing(pose, player.swingingArm, player.getMainArm());
    }

    public static boolean ownedSwing(RetargetedPose pose, InteractionHand swingingHand, HumanoidArm mainArm) {
        if (pose == null || swingingHand == null || mainArm == null || !pose.anyDriven()) {
            return false;
        }
        HumanoidArm swingingArm = swingingHand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
        if (swingingArm == null) {
            return false;
        }
        OpenMatrix4f owned = swingingArm == HumanoidArm.RIGHT ? pose.rightArm() : pose.leftArm();
        return owned != null;
    }

    public static float keepAttackTime(
            boolean playerModel,
            boolean playerEntity,
            RetargetedPose pose,
            InteractionHand swingingHand,
            HumanoidArm mainArm,
            float actual) {
        if (!playerModel || !playerEntity || !ownedSwing(pose, swingingHand, mainArm)) {
            return actual;
        }
        return 0.0F;
    }

    public static OpenMatrix4f firstPersonHeld(Player player, InteractionHand hand, ItemStack stack, float partialTick) {
        if (!swordHand(player, hand, stack)) {
            return null;
        }
        ActionSampleContext context = inspect(player, partialTick);
        if (!context.active()) {
            return null;
        }
        ResourceLocation visualId = context.visualActionId();
        if (!ActionVisualCatalog.blade(visualId)) {
            return null;
        }
        HumanoidArm arm = handArm(player, hand);
        if (!ActionVisualCatalog.usesHand(visualId, arm == HumanoidArm.RIGHT)) {
            return null;
        }
        RetargetedPose local = localPose(context);
        OpenMatrix4f modelArm = arm == HumanoidArm.RIGHT ? local.rightArm() : local.leftArm();
        if (modelArm == null) {
            return null;
        }
        OpenMatrix4f aim = null;
        if (context.hasSlashFrame() && ActionVisualCatalog.referenceForward(visualId) != null) {
            aim = cameraAim(context, visualId);
        }
        return FirstPersonHandBasis.heldRotation(modelArm, aim);
    }

    public static Quaternionf firstPersonQuaternion(Player player, InteractionHand hand, ItemStack stack, float partialTick) {
        OpenMatrix4f held = firstPersonHeld(player, hand, stack, partialTick);
        if (held == null) {
            return null;
        }
        return FirstPersonHandBasis.quaternion(held);
    }

    public static boolean takeFirstPerson(Player player, InteractionHand hand, ItemStack stack, float partialTick) {
        return firstPersonHeld(player, hand, stack, partialTick) != null;
    }

    private static RetargetedPose localPose(ActionSampleContext context) {
        Armature armature = context.armature();
        RetargetProfile profile = RetargetProfile.forArmature(armature);
        Pose pose = context.pose();
        if (profile == null || pose == null) {
            return RetargetedPose.empty();
        }
        return RigidPoseRetarget.adapt(armature, pose, profile);
    }

    private static OpenMatrix4f cameraAim(ActionSampleContext context, ResourceLocation visualId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gameRenderer == null) {
            return null;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) {
            return null;
        }
        Vector3f lookVector = camera.getLookVector();
        Vector3f upVector = camera.getUpVector();
        Vector3f leftVector = camera.getLeftVector();
        if (lookVector == null || upVector == null || leftVector == null) {
            return null;
        }
        Vec3f referenceForward = ActionVisualCatalog.referenceForward(visualId);
        Vec3f referenceNormal = ActionVisualCatalog.referenceNormal(visualId);
        return FirstPersonHandBasis.cameraAlignment(
                context.directionX(), context.directionY(), context.directionZ(),
                context.planeX(), context.planeY(), context.planeZ(),
                lookVector.x(), lookVector.y(), lookVector.z(),
                upVector.x(), upVector.y(), upVector.z(),
                leftVector.x(), leftVector.y(), leftVector.z(),
                referenceForward,
                referenceNormal);
    }

    private static boolean swordArm(Player player, HumanoidArm arm) {
        if (player == null || arm == null || useOwns(player, arm)) {
            return false;
        }
        ItemStack stack = player.getMainArm() == arm ? player.getMainHandItem() : player.getOffhandItem();
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
    }

    private static boolean swordHand(Player player, InteractionHand hand, ItemStack stack) {
        if (player == null || hand == null || stack == null || stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) {
            return false;
        }
        return !useOwns(player, handArm(player, hand));
    }

    private static boolean useOwns(Player player, HumanoidArm arm) {
        if (player == null || arm == null || !player.isUsingItem()) {
            return false;
        }
        if (handArm(player, player.getUsedItemHand()) != arm) {
            return false;
        }
        ItemStack used = player.getUseItem();
        if (used == null || used.isEmpty()) {
            return false;
        }
        return used.getUseAnimation() != UseAnim.NONE;
    }

    private static HumanoidArm handArm(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return player.getMainArm();
        }
        return player.getMainArm().getOpposite();
    }

    private static float bodyYawDegrees(Player player, float partialTick) {
        if (player == null) {
            return 0.0F;
        }
        float safePartial = Float.isFinite(partialTick) ? Mth.clamp(partialTick, 0.0F, 1.0F) : 0.0F;
        return Mth.rotLerp(safePartial, player.yBodyRotO, player.yBodyRot);
    }
}
