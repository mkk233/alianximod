package com.bslx.XueXiMod.client.action.skinned.attachments;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.client.action.RenderPoseScope;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;
import com.bslx.XueXiMod.client.action.skinned.ImmutableSkinFrame;
import com.bslx.XueXiMod.client.action.skinned.RenderPlan;
import com.bslx.XueXiMod.client.action.skinned.SkinRenderAccess;
import com.bslx.XueXiMod.client.action.skinned.SkinSession;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 附件路对外的三个冻结入口，以及各原版层的替换函数。
 * 主路登记 {@link #MIXIN_CLASS_NAMES}，本类不写 mixins.json，也不建立主路占位。
 */
public final class SkinAttachmentBridge {
    public static final String[] MIXIN_CLASS_NAMES = {
            "client.skin.HumanoidArmorSkinMixin",
            "client.skin.HumanoidArmorLayerAccess",
            "client.skin.LivingRendererLayersAccessor",
            "client.skin.ItemInHandSkinMixin",
            "client.skin.CustomHeadSkinMixin",
            "client.skin.CapeSkinMixin",
            "client.skin.ElytraSkinMixin",
            "client.skin.StuckInBodySkinMixin",
            "client.skin.PlayerSkinOverlayAccessor",
            "client.skin.StuckLayerScopeMixin"
    };

    private static ItemDraw itemDraw = SkinAttachmentBridge::drawVanillaItem;

    private SkinAttachmentBridge() {
    }

    public static AttachmentSupport preflight(
            AbstractClientPlayer player,
            PlayerRenderer renderer,
            com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind kind,
            long resourceGeneration) {
        return ArmorPreflight.inspect(player, renderer, kind, resourceGeneration);
    }

    public static FirstPersonItemPlan prepareFirstPersonItem(
            ImmutableSkinFrame frame,
            AbstractClientPlayer player,
            InteractionHand hand,
            ItemStack stack,
            OpenMatrix4f neutralItemFrame) {
        if (frame == null) {
            return FirstPersonItemPlan.blocked("frame-missing");
        }
        if (player == null) {
            return FirstPersonItemPlan.blocked("player-missing");
        }
        if (hand == null || frame.plan().hand() == RenderPlan.Hand.NONE) {
            return FirstPersonItemPlan.blocked("hand-missing");
        }
        if (stack == null || stack.isEmpty()) {
            return FirstPersonItemPlan.blocked("item-empty");
        }
        if (neutralItemFrame == null) {
            return FirstPersonItemPlan.blocked("item-frame-missing");
        }
        if (frame.plan().mode() != RenderPlan.Mode.SKINNED) {
            return FirstPersonItemPlan.blocked("frame-mode");
        }
        if (frame.plan().perspective() != RenderPlan.Perspective.FIRST_PERSON) {
            return FirstPersonItemPlan.blocked("perspective");
        }
        if (!player.getUUID().equals(frame.plan().ownerId()) || player.getId() != frame.plan().entityId()) {
            return FirstPersonItemPlan.blocked("owner");
        }
        boolean leftHand = frame.plan().hand() == RenderPlan.Hand.LEFT;
        BindJointTable table = BindJointTable.published(frame.plan().resourceGeneration());
        if (table == null) {
            return FirstPersonItemPlan.blocked("bind-armature-missing");
        }
        String jointName = leftHand ? "Tool_L" : "Tool_R";
        if (!table.has(jointName)) {
            jointName = leftHand ? "Hand_L" : "Hand_R";
        }
        if (!table.has(jointName)) {
            return FirstPersonItemPlan.blocked("bind-joint-missing");
        }
        try {
            OpenMatrix4f matrix = AttachmentMotion.attachmentLocal(frame, table, jointName, new OpenMatrix4f(neutralItemFrame));
            return FirstPersonItemPlan.ready(frame, player, hand, stack.copy(), matrix, leftHand);
        } catch (SkinDiagnostic diagnostic) {
            return FirstPersonItemPlan.blocked(diagnostic.reasonCode());
        }
    }

    public static void renderFirstPersonItem(
            FirstPersonItemPlan plan,
            PoseStack entryStack,
            MultiBufferSource buffers,
            int packedLight) {
        if (plan == null || !plan.supported() || entryStack == null || plan.itemMatrix() == null) {
            return;
        }
        entryStack.pushPose();
        try {
            AffinePose.apply(entryStack, plan.itemMatrix());
            itemDraw.draw(plan.player(), plan.stack(), plan.displayContext(), plan.leftHand(), entryStack, buffers, packedLight);
        } finally {
            entryStack.popPose();
        }
    }

    /**
     * 主路在 {@code SkinPalette.prepare} 之后、选定 SKINNED 和打开身体租约之前调用。
     * 返回空表示调色板盖住了已准备的护甲网格；非空则整次角色走既有回退，身体不要输出蒙皮顶点。
     * 不重算 FK。预检不支持时返回空，原因仍在 {@link AttachmentSupport#reason()}。
     */
    public static String armorPaletteReason(AttachmentSupport support, SkinPalette palette) {
        if (support == null) {
            return null;
        }
        return support.paletteReason(palette);
    }

    public static Model cachedArmorModel(LivingEntity entity, EquipmentSlot slot) {
        if (entity == null) {
            return null;
        }
        AttachmentSupport support = SkinRenderAccess.attachmentSupport(entity);
        if (support == null) {
            return null;
        }
        return support.hookedModel(slot);
    }

    public static ArmorToken openArmorPiece(LivingEntity entity, EntityModel<?> model) {
        LayerHold.Subject subject = LayerHold.push(entity, model);
        ArmorDraw.Piece piece;
        try {
            piece = ArmorDraw.push();
        } catch (RuntimeException failure) {
            LayerHold.pop(subject);
            throw failure;
        }
        return new ArmorToken(subject, piece);
    }

    public static boolean emitArmorOrPass(
            Model model,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float redAmount,
            float greenAmount,
            float blueAmount,
            float alphaAmount) {
        if (ArmorDraw.current() == null) {
            return false;
        }
        LayerHold.Subject subject = LayerHold.current();
        if (subject == null || subject.entity == null) {
            throw new SkinDiagnostic("armor-layer", "护甲件缺少层上下文");
        }
        ImmutableSkinFrame frame = SkinRenderAccess.currentThirdPerson(subject.entity, subject.model);
        if (!acceptsFrame(frame, subject.entity)) {
            return false;
        }
        return ArmorDraw.emit(
                SkinRenderAccess.attachmentSupport(subject.entity),
                frame,
                model,
                poseStack,
                consumer,
                packedLight,
                packedOverlay,
                redAmount,
                greenAmount,
                blueAmount,
                alphaAmount);
    }

    public static boolean heldReplaces(LivingEntity entity, EntityModel<?> model, HumanoidArm arm) {
        if (arm == null || entity == null || !(model instanceof HumanoidModel<?>)) {
            return false;
        }
        ImmutableSkinFrame frame = SkinRenderAccess.currentThirdPerson(entity, model);
        if (!acceptsFrame(frame, entity)) {
            return false;
        }
        requireSupport(entity);
        requireTable(frame);
        return true;
    }

    public static void applyHeld(LivingEntity entity, EntityModel<?> model, HumanoidArm arm, PoseStack poseStack) {
        if (!(model instanceof HumanoidModel<?> humanoid) || arm == null || poseStack == null || entity == null) {
            throw new SkinDiagnostic("held-context", "手持变换缺少模型或栈");
        }
        ImmutableSkinFrame frame = SkinRenderAccess.currentThirdPerson(entity, model);
        if (!acceptsFrame(frame, entity)) {
            throw new SkinDiagnostic("held-frame", "手持变换没有已蒙皮的帧");
        }
        requireSupport(entity);
        BindJointTable table = requireTable(frame);
        boolean leftSide = arm == HumanoidArm.LEFT;
        ModelPart armPart = leftSide ? humanoid.leftArm : humanoid.rightArm;
        if (armPart == null) {
            throw new SkinDiagnostic("held-part", "手持变换缺少手臂部件");
        }
        OpenMatrix4f local = AttachmentMotion.heldLocal(frame, table, leftSide, PartRest.fromInitial(armPart), AttachmentMotion.HELD_GRIP);
        AffinePose.apply(poseStack, local);
    }

    public static boolean replaceHead(EntityModel<?> model, ModelPart headPart, PoseStack poseStack) {
        LivingEntity entity = entityFor(model);
        if (entity == null || poseStack == null) {
            return false;
        }
        ImmutableSkinFrame frame = SkinRenderAccess.currentThirdPerson(entity, model);
        if (!acceptsFrame(frame, entity)) {
            return false;
        }
        if (headPart == null) {
            throw new SkinDiagnostic("head-part", "头部变换缺少部件");
        }
        requireSupport(entity);
        BindJointTable table = requireTable(frame);
        OpenMatrix4f local = AttachmentMotion.attachmentLocal(frame, table, "Head", PartRest.fromInitial(headPart));
        AffinePose.apply(poseStack, local);
        return true;
    }

    /** 披风和鞘翅共用：在原版 0.125 局部平移之前乘一次胸部矩阵。未蒙皮时不改栈。 */
    public static void prependChest(EntityModel<?> model, PoseStack poseStack) {
        LivingEntity entity = entityFor(model);
        if (entity == null || poseStack == null) {
            return;
        }
        ImmutableSkinFrame frame = SkinRenderAccess.currentThirdPerson(entity, model);
        if (!acceptsFrame(frame, entity)) {
            return;
        }
        requireSupport(entity);
        BindJointTable table = requireTable(frame);
        AffinePose.apply(poseStack, AttachmentMotion.chestLocal(frame, table));
    }

    public static StuckToken openStuck(LivingEntity entity, EntityModel<?> model) {
        LayerHold.Subject subject = LayerHold.push(entity, model);
        StuckMarks.Mark mark;
        try {
            mark = StuckMarks.push();
        } catch (RuntimeException failure) {
            LayerHold.pop(subject);
            throw failure;
        }
        return new StuckToken(subject, mark);
    }

    public static void noteStuckPart(ModelPart part) {
        StuckMarks.clearTop();
        LayerHold.Subject subject = LayerHold.current();
        if (subject == null || !(subject.model instanceof HumanoidModel<?> humanoid) || part == null) {
            return;
        }
        ImmutableSkinFrame frame = SkinRenderAccess.currentThirdPerson(subject.entity, subject.model);
        if (!acceptsFrame(frame, subject.entity)) {
            return;
        }
        try {
            requireSupport(subject.entity);
        } catch (SkinDiagnostic diagnostic) {
            StuckMarks.fail(diagnostic);
            return;
        }
        ArmorRegion region = PlayerPartRegions.regionOf(humanoid, part);
        if (region == null) {
            return;
        }
        StuckMarks.remember(region, PartRest.fromInitial(part));
    }

    public static boolean replaceStuckRotate() {
        SkinDiagnostic failure = StuckMarks.pendingFailure();
        if (failure != null) {
            throw failure;
        }
        return StuckMarks.handled();
    }

    public static boolean replaceStuckPoint(PoseStack poseStack, float localX, float localY, float localZ) {
        SkinDiagnostic failure = StuckMarks.pendingFailure();
        if (failure != null) {
            throw failure;
        }
        if (!StuckMarks.handled() || poseStack == null) {
            return false;
        }
        LayerHold.Subject subject = LayerHold.current();
        if (subject == null) {
            throw new SkinDiagnostic("stuck-layer", "插体点缺少层上下文");
        }
        ImmutableSkinFrame frame = SkinRenderAccess.currentThirdPerson(subject.entity, subject.model);
        if (!acceptsFrame(frame, subject.entity)) {
            throw new SkinDiagnostic("stuck-frame", "插体点缺少已蒙皮的帧");
        }
        BindJointTable table = requireTable(frame);
        OpenMatrix4f local = AttachmentMotion.stuckLocal(frame, table, StuckMarks.region(), StuckMarks.partRest(), localX, localY, localZ);
        AffinePose.apply(poseStack, local);
        return true;
    }

    static boolean acceptsFrame(ImmutableSkinFrame frame, LivingEntity entity) {
        if (frame == null || entity == null || frame.plan() == null) {
            return false;
        }
        if (frame.plan().mode() != RenderPlan.Mode.SKINNED) {
            return false;
        }
        if (frame.plan().perspective() != RenderPlan.Perspective.THIRD_PERSON) {
            return false;
        }
        return entity.getUUID().equals(frame.plan().ownerId()) && entity.getId() == frame.plan().entityId();
    }

    static void replaceItemDraw(ItemDraw replacement) {
        itemDraw = replacement == null ? SkinAttachmentBridge::drawVanillaItem : replacement;
    }

    private static LivingEntity entityFor(EntityModel<?> model) {
        if (model == null) {
            return null;
        }
        SkinSession session = SkinSession.top();
        if (session == null || session.model() != model || session.view() != RenderPoseScope.View.THIRD_PERSON) {
            return null;
        }
        Object player = session.player();
        if (player instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static void requireSupport(LivingEntity entity) {
        AttachmentSupport support = SkinRenderAccess.attachmentSupport(entity);
        if (support == null || !support.supported()) {
            String reason = support == null || support.reason() == null || support.reason().isBlank()
                    ? "attachment-unsupported"
                    : support.reason();
            throw new SkinDiagnostic(reason, "已蒙皮的附件预检不可用");
        }
    }

    private static BindJointTable requireTable(ImmutableSkinFrame frame) {
        BindJointTable table = BindJointTable.published(frame.plan().resourceGeneration());
        if (table == null) {
            throw new SkinDiagnostic("bind-armature-missing", "已蒙皮的附件找不到绑定骨架");
        }
        return table;
    }

    private static void drawVanillaItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        if (entity == null || stack == null || stack.isEmpty() || displayContext == null || poseStack == null || buffers == null) {
            return;
        }
        int seed = entity.getId() + displayContext.ordinal();
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity,
                stack,
                displayContext,
                leftHand,
                poseStack,
                buffers,
                entity.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                seed);
    }

    public static final class ArmorToken {
        private final LayerHold.Subject subject;
        private final ArmorDraw.Piece piece;
        private boolean closed;

        private ArmorToken(LayerHold.Subject subject, ArmorDraw.Piece piece) {
            this.subject = subject;
            this.piece = piece;
        }

        public void close() {
            if (this.closed) {
                return;
            }
            ArmorDraw.pop(this.piece);
            LayerHold.pop(this.subject);
            this.closed = true;
        }
    }

    public static final class StuckToken {
        private final LayerHold.Subject subject;
        private final StuckMarks.Mark mark;
        private boolean closed;

        private StuckToken(LayerHold.Subject subject, StuckMarks.Mark mark) {
            this.subject = subject;
            this.mark = mark;
        }

        public void close() {
            if (this.closed) {
                return;
            }
            StuckMarks.pop(this.mark);
            LayerHold.pop(this.subject);
            this.closed = true;
        }
    }

    interface ItemDraw {
        void draw(
                LivingEntity entity,
                ItemStack stack,
                ItemDisplayContext displayContext,
                boolean leftHand,
                PoseStack poseStack,
                MultiBufferSource buffers,
                int packedLight);
    }
}
