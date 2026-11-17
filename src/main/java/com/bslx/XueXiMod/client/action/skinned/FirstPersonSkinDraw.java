package com.bslx.XueXiMod.client.action.skinned;

import org.joml.Vector3f;

import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;
import com.bslx.XueXiMod.client.action.ActionVisualCatalog;
import com.bslx.XueXiMod.client.action.FirstPersonHandBasis;
import com.bslx.XueXiMod.client.action.mesh.CornerSink;
import com.bslx.XueXiMod.client.action.mesh.MeshRenderView;
import com.bslx.XueXiMod.client.action.mesh.PartVisibility;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinLease;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;
import com.bslx.XueXiMod.client.action.mesh.SkinWorkspacePool;
import com.bslx.XueXiMod.client.action.skinned.attachments.FirstPersonItemPlan;
import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;

/** 接管手画一次手臂、一次袖子、一次物品。不支持时调用方保持整手原路径。 */
public final class FirstPersonSkinDraw {
    private FirstPersonSkinDraw() {
    }

    public static boolean draw(
            AbstractClientPlayer player,
            InteractionHand hand,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        SkinSession session = SkinSession.top();
        if (session == null || player == null || hand == null || !session.owns(player) || session.bodyStarted()) {
            return false;
        }
        ActionSampleContext sample = session.sample();
        SkinAssets assets = session.assets();
        PlayerMeshKind kind = session.kind();
        if (sample == null || !sample.active() || assets == null || kind == null || !assets.usable(kind) || !assets.matches(sample.armature())) {
            return false;
        }
        boolean rightSide = handArm(player, hand) == HumanoidArm.RIGHT;
        PlayerSkinMesh mesh = assets.mesh(kind);
        if (HandFrameCalibration.supportDiagnostic(mesh, kind) != null) {
            return false;
        }
        OpenMatrix4f handFrame = HandFrameCalibration.handFrame(kind, rightSide, equipProgress);
        if (handFrame == null || sameElements(handFrame, AttachmentTransform.sourceToBody())) {
            return false;
        }
        PlaneDecision decision = planeDecision(sample, handFrame, kind, rightSide, null);
        if (decision.alignment == null) {
            return false;
        }
        if (!decision.unalignedReason.isEmpty()) {
            session.recordPlane(decision.unalignedReason);
        }
        OpenMatrix4f alignment = decision.alignment;
        Armature armature = sample.armature();
        Pose pose = sample.pose();
        if (armature == null || pose == null) {
            return false;
        }
        SkinPalette palette;
        ImmutableSkinFrame.Clock clock;
        try {
            palette = SkinPalette.prepare(armature, pose);
            clock = clock(sample);
        } catch (RuntimeException failure) {
            return false;
        }
        RenderPlan plan = RenderPlan.of(
                RenderPlan.Mode.SKINNED,
                player.getUUID(),
                player.getId(),
                RenderPlan.Perspective.FIRST_PERSON,
                rightSide ? RenderPlan.Hand.RIGHT : RenderPlan.Hand.LEFT,
                kind,
                assets.resourceGeneration(),
                "first-person-hand");
        ImmutableSkinFrame frame;
        try {
            frame = ImmutableSkinFrame.capture(plan, mesh, palette, alignment, handFrame, clock);
        } catch (RuntimeException failure) {
            return false;
        }
        OpenMatrix4f itemAnchor = HandFrameCalibration.itemAnchor(rightSide ? 1.0F : -1.0F, equipProgress);
        FirstPersonItemPlan itemPlan;
        try {
            itemPlan = SkinAttachmentBridge.prepareFirstPersonItem(frame, player, hand, stack, itemAnchor);
        } catch (RuntimeException failure) {
            return false;
        }
        if (itemPlan == null || !itemPlan.supported()) {
            return false;
        }
        SkinLease lease = SkinWorkspacePool.currentThread().open(mesh);
        session.hold(lease, frame);
        ResourceLocation skin = player.getSkinTextureLocation();
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        boolean invisible = player.isInvisible();
        boolean sleeve = !invisible && sleeveShown(player, rightSide);
        CornerSink armSink = invisible
                ? null
                : new PoseStackCornerSink(poseStack, buffers.getBuffer(RenderType.entitySolid(skin)), 1.0F, 1.0F, 1.0F, 1.0F);
        CornerSink sleeveSink = sleeve
                ? new PoseStackCornerSink(poseStack, buffers.getBuffer(RenderType.entityTranslucent(skin)), 1.0F, 1.0F, 1.0F, 1.0F)
                : null;
        completeHand(
                invisible,
                rightSide,
                sleeve,
                mesh,
                palette,
                lease,
                frame.copyMeshPlacement(),
                packedLight,
                overlay,
                armSink,
                sleeveSink,
                () -> SkinAttachmentBridge.renderFirstPersonItem(itemPlan, poseStack, buffers, packedLight));
        return true;
    }

    /** 隐身时不发手臂和袖子。物品回调仍执行一次。RenderType 由调用方保持原样。 */
    static int completeHand(
            boolean invisible,
            boolean rightSide,
            boolean sleeveShown,
            PlayerSkinMesh mesh,
            SkinPalette palette,
            SkinLease lease,
            OpenMatrix4f placement,
            int packedLight,
            int overlay,
            CornerSink armSink,
            CornerSink sleeveSink,
            Runnable itemOnce) {
        int corners = 0;
        if (!invisible) {
            PartVisibility arm = PartVisibility.noneShown().with(rightSide ? PlayerMeshPart.RIGHT_ARM : PlayerMeshPart.LEFT_ARM, true);
            corners += MeshRenderView.emit(mesh, palette, lease, arm, placement, packedLight, overlay, armSink);
            if (sleeveShown) {
                PartVisibility sleeve = PartVisibility.noneShown().with(
                        rightSide ? PlayerMeshPart.RIGHT_SLEEVE : PlayerMeshPart.LEFT_SLEEVE, true);
                corners += MeshRenderView.emit(mesh, palette, lease, sleeve, placement, packedLight, overlay, sleeveSink);
            }
        }
        if (itemOnce != null) {
            itemOnce.run();
        }
        return corners;
    }

    static PlaneDecision planeDecision(
            ActionSampleContext sample,
            OpenMatrix4f handFrame,
            PlayerMeshKind kind,
            boolean rightSide,
            OpenMatrix4f suppliedSpin) {
        if (sample == null || !sample.hasSlashFrame()) {
            return PlaneDecision.keep(new OpenMatrix4f());
        }
        ResourceLocation visualKey = sample.visualActionId();
        if (!ActionVisualCatalog.hasRecipe(visualKey)) {
            return PlaneDecision.plain("有挥斩帧但没有平面校准配方，保持普通蒙皮");
        }
        Vec3f referenceForward = ActionVisualCatalog.referenceForward(visualKey);
        Vec3f referenceNormal = ActionVisualCatalog.referenceNormal(visualKey);
        if (referenceForward == null || referenceNormal == null) {
            return PlaneDecision.blocked();
        }
        OpenMatrix4f spin = suppliedSpin != null ? suppliedSpin : cameraSpin(sample, referenceForward, referenceNormal);
        if (spin == null) {
            return PlaneDecision.blocked();
        }
        Vec3f shoulder = OpenMatrix4f.transform3v(handFrame, HandFrameCalibration.sourceShoulder(kind, rightSide), null);
        return PlaneDecision.keep(HandFrameCalibration.around(spin, shoulder.coordinateX, shoulder.coordinateY, shoulder.coordinateZ));
    }

    static final class PlaneDecision {
        final OpenMatrix4f alignment;
        final String unalignedReason;

        private PlaneDecision(OpenMatrix4f alignment, String unalignedReason) {
            this.alignment = alignment;
            this.unalignedReason = unalignedReason;
        }

        static PlaneDecision keep(OpenMatrix4f alignment) {
            return new PlaneDecision(alignment, "");
        }

        static PlaneDecision plain(String reason) {
            return new PlaneDecision(new OpenMatrix4f(), reason);
        }

        static PlaneDecision blocked() {
            return new PlaneDecision(null, "");
        }
    }

    private static OpenMatrix4f cameraSpin(ActionSampleContext sample, Vec3f referenceForward, Vec3f referenceNormal) {
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
        return FirstPersonHandBasis.cameraAlignment(
                sample.directionX(), sample.directionY(), sample.directionZ(),
                sample.planeX(), sample.planeY(), sample.planeZ(),
                lookVector.x(), lookVector.y(), lookVector.z(),
                upVector.x(), upVector.y(), upVector.z(),
                leftVector.x(), leftVector.y(), leftVector.z(),
                referenceForward,
                referenceNormal);
    }

    private static ImmutableSkinFrame.Clock clock(ActionSampleContext sample) {
        String actionName = "active-action";
        if (sample.visualActionId() != null) {
            actionName = sample.visualActionId().toString();
        } else if (sample.actionId() != null) {
            actionName = sample.actionId().toString();
        }
        float elapsed = sample.elapsedSeconds();
        float progress = sample.normalizedProgress();
        int revision = sample.revision();
        if (!Float.isFinite(elapsed) || elapsed < 0.0F || !Float.isFinite(progress) || progress < 0.0F || progress > 1.0F || revision < 0) {
            throw new IllegalArgumentException("first person clock is not usable");
        }
        return ImmutableSkinFrame.Clock.of(actionName, sample.startGameTick(), elapsed, progress, revision);
    }

    private static boolean sleeveShown(AbstractClientPlayer player, boolean rightSide) {
        if (rightSide) {
            return player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
        }
        return player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
    }

    private static HumanoidArm handArm(AbstractClientPlayer player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return player.getMainArm();
        }
        return player.getMainArm().getOpposite();
    }

    private static boolean sameElements(OpenMatrix4f left, OpenMatrix4f right) {
        return left.m00 == right.m00 && left.m01 == right.m01 && left.m02 == right.m02 && left.m03 == right.m03
                && left.m10 == right.m10 && left.m11 == right.m11 && left.m12 == right.m12 && left.m13 == right.m13
                && left.m20 == right.m20 && left.m21 == right.m21 && left.m22 == right.m22 && left.m23 == right.m23
                && left.m30 == right.m30 && left.m31 == right.m31 && left.m32 == right.m32 && left.m33 == right.m33;
    }
}
