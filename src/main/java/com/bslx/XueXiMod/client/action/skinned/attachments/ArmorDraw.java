package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.util.ArrayDeque;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.bslx.XueXiMod.client.action.mesh.MeshRenderView;
import com.bslx.XueXiMod.client.action.mesh.PartVisibility;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshPart;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;
import com.bslx.XueXiMod.client.action.mesh.SkinLease;
import com.bslx.XueXiMod.client.action.mesh.SkinWorkspacePool;
import com.bslx.XueXiMod.client.action.skinned.ImmutableSkinFrame;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;

/** 同一护甲件的多次材质 pass 共用一条租约。关闭只弹出本次件，不碰外层租约或身体租约。 */
final class ArmorDraw {
    static int drawCount;

    private static final ThreadLocal<ArrayDeque<Piece>> PIECES = ThreadLocal.withInitial(ArrayDeque::new);

    private ArmorDraw() {
    }

    static Piece push() {
        Piece piece = new Piece();
        PIECES.get().addLast(piece);
        return piece;
    }

    static void pop(Piece piece) {
        ArrayDeque<Piece> stack = PIECES.get();
        if (stack.isEmpty() || stack.peekLast() != piece) {
            if (!stack.contains(piece)) {
                return;
            }
            throw new SkinDiagnostic("armor-order", "护甲件关闭顺序和打开顺序不一致");
        }
        stack.removeLast();
        if (piece.lease != null) {
            piece.lease.close();
        }
    }

    static Piece current() {
        ArrayDeque<Piece> stack = PIECES.get();
        return stack.isEmpty() ? null : stack.peekLast();
    }

    static int depth() {
        return PIECES.get().size();
    }

    static void discardThread() {
        ArrayDeque<Piece> stack = PIECES.get();
        while (!stack.isEmpty()) {
            Piece piece = stack.removeLast();
            if (piece.lease != null && piece.lease.isOpen()) {
                piece.lease.close();
            }
        }
    }

    static boolean pieceLeaseOpen() {
        Piece piece = current();
        return piece != null && piece.lease != null && piece.lease.isOpen();
    }

    static boolean emit(
            AttachmentSupport support,
            ImmutableSkinFrame frame,
            Model model,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            float redAmount,
            float greenAmount,
            float blueAmount,
            float alphaAmount) {
        Piece piece = current();
        if (piece == null) {
            throw new SkinDiagnostic("armor-piece", "护甲绘制没有打开的件");
        }
        if (support == null || !support.supported()) {
            throw new SkinDiagnostic(reasonOf(support), "已蒙皮的护甲预检不可用");
        }
        if (frame == null || model == null || poseStack == null || consumer == null) {
            throw new SkinDiagnostic("armor-context", "护甲绘制缺少帧或缓冲");
        }
        if (!(model instanceof HumanoidModel<?> humanoid)) {
            throw new SkinDiagnostic("armor-model", "护甲模型不是人形");
        }
        PlayerSkinMesh mesh = support.armorMesh(model);
        if (mesh == null) {
            throw new SkinDiagnostic("armor-mesh", "预检没有这个护甲网格");
        }
        if (frame.palette() == null) {
            throw new SkinDiagnostic("palette-missing", "护甲调色板缺失");
        }
        if (frame.palette().jointCount() < mesh.jointLimit()) {
            throw new SkinDiagnostic("palette", "调色板关节数小于护甲网格");
        }
        if (!frame.copyMeshPlacement().hasFiniteElements()) {
            throw new SkinDiagnostic("armor-placement", "护甲放置矩阵不是有限数");
        }
        SkinLease lease = piece.lease;
        if (lease == null || piece.mesh != mesh) {
            if (lease != null) {
                lease.close();
            }
            lease = SkinWorkspacePool.currentThread().open(mesh);
            piece.lease = lease;
            piece.mesh = mesh;
        }
        MeshRenderView.emit(
                mesh,
                frame.palette(),
                lease,
                visibility(humanoid, mesh),
                frame.copyMeshPlacement(),
                packedLight,
                packedOverlay,
                new PoseSink(poseStack, consumer, redAmount, greenAmount, blueAmount, alphaAmount));
        drawCount++;
        return true;
    }

    private static String reasonOf(AttachmentSupport support) {
        if (support == null || support.reason() == null || support.reason().isBlank()) {
            return "armor-unsupported";
        }
        return support.reason();
    }

    private static PartVisibility visibility(HumanoidModel<?> humanoid, PlayerSkinMesh mesh) {
        PartVisibility visibility = PartVisibility.noneShown();
        visibility = show(visibility, mesh, PlayerMeshPart.HEAD, humanoid.head);
        visibility = show(visibility, mesh, PlayerMeshPart.HAT, humanoid.hat);
        visibility = show(visibility, mesh, PlayerMeshPart.TORSO, humanoid.body);
        visibility = show(visibility, mesh, PlayerMeshPart.RIGHT_ARM, humanoid.rightArm);
        visibility = show(visibility, mesh, PlayerMeshPart.LEFT_ARM, humanoid.leftArm);
        visibility = show(visibility, mesh, PlayerMeshPart.RIGHT_LEG, humanoid.rightLeg);
        visibility = show(visibility, mesh, PlayerMeshPart.LEFT_LEG, humanoid.leftLeg);
        return visibility;
    }

    private static PartVisibility show(PartVisibility visibility, PlayerSkinMesh mesh, PlayerMeshPart meshPart, ModelPart part) {
        if (mesh.hasPart(meshPart) && part != null && part.visible && !part.skipDraw) {
            return visibility.with(meshPart, true);
        }
        return visibility;
    }

    static final class Piece {
        private SkinLease lease;
        private PlayerSkinMesh mesh;
    }

    private static final class PoseSink implements com.bslx.XueXiMod.client.action.mesh.CornerSink {
        private final Matrix4f pose;
        private final Matrix3f normalMatrix;
        private final VertexConsumer consumer;
        private final float redAmount;
        private final float greenAmount;
        private final float blueAmount;
        private final float alphaAmount;
        private final Vector4f point = new Vector4f();
        private final Vector3f direction = new Vector3f();

        PoseSink(
                PoseStack poseStack,
                VertexConsumer consumer,
                float redAmount,
                float greenAmount,
                float blueAmount,
                float alphaAmount) {
            this.pose = poseStack.last().pose();
            this.normalMatrix = poseStack.last().normal();
            this.consumer = consumer;
            this.redAmount = redAmount;
            this.greenAmount = greenAmount;
            this.blueAmount = blueAmount;
            this.alphaAmount = alphaAmount;
        }

        @Override
        public void corner(
                float positionX,
                float positionY,
                float positionZ,
                float normalX,
                float normalY,
                float normalZ,
                float textureU,
                float textureV,
                int packedLight,
                int packedOverlay) {
            this.point.set(positionX, positionY, positionZ, 1.0F);
            this.pose.transform(this.point);
            this.direction.set(normalX, normalY, normalZ);
            this.normalMatrix.transform(this.direction);
            float lengthSquared = this.direction.lengthSquared();
            if (lengthSquared > 1.0E-8F) {
                this.direction.mul(1.0F / (float) Math.sqrt(lengthSquared));
            }
            this.consumer.vertex(
                    this.point.x,
                    this.point.y,
                    this.point.z,
                    this.redAmount,
                    this.greenAmount,
                    this.blueAmount,
                    this.alphaAmount,
                    textureU,
                    textureV,
                    packedOverlay,
                    packedLight,
                    this.direction.x,
                    this.direction.y,
                    this.direction.z);
        }
    }
}
