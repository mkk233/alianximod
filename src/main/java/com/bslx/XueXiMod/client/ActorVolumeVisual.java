package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceException;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.clone.core.VolumeVisualKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 断潮体积的纯客户端绘制。几何与 ActorVolumeGeometry 的中心、正交基和半尺寸一致。
 * 贴图用现成的 horizontal_slash，整张 UV，不生成命中实体。
 */
public final class ActorVolumeVisual {
    private static final ResourceLocation SLASH_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/horizontal_slash.png");
    private static final double TIDE_DEPTH_DEFAULT = 5.0d;
    private static final double TIDE_HEIGHT_DEFAULT = 3.0d;
    private static final double TIDE_WIDTH_DEFAULT = 0.4d;

    private ActorVolumeVisual() {
    }

    public static ResourceLocation texture() {
        return SLASH_TEXTURE;
    }

    public static RenderType slashType() {
        return RenderType.entityTranslucent(SLASH_TEXTURE);
    }

    /**
     * 与断潮报价同一读口：纵深、高度走专项，厚度走释放定义。
     * 读一次后由快照冻住。线程未绑定平衡侧时返回空，不另造一套尺寸。
     */
    public static double[] sampleTideSeverExtents() {
        try {
            double depthBlocks = SpecialBalance.number("tide_sever", BalanceKeys.BEAM_LENGTH, TIDE_DEPTH_DEFAULT);
            double heightBlocks = SpecialBalance.number("tide_sever", BalanceKeys.BOX_HEIGHT, TIDE_HEIGHT_DEFAULT);
            ReleaseDefinition definition = ReleaseRegistry.get("tide_sever");
            double widthBlocks = definition == null ? TIDE_WIDTH_DEFAULT : definition.thickness();
            if (!finiteSize(depthBlocks) || !finiteSize(heightBlocks) || !finiteSize(widthBlocks)) {
                return null;
            }
            return new double[] { depthBlocks, heightBlocks, widthBlocks };
        } catch (BalanceException failure) {
            return null;
        }
    }

    public static void draw(PoseStack pose, MultiBufferSource buffers, ClientLevel clientLevel,
                            CloneAfterimageSnapshot snapshot) {
        if (pose == null || buffers == null || clientLevel == null || snapshot == null
                || snapshot.volumeVisual() != VolumeVisualKind.TIDE_SLASH_FULL) {
            return;
        }
        if (!finiteSize(snapshot.depthBlocks()) || !finiteSize(snapshot.heightBlocks())
                || !finiteSize(snapshot.widthBlocks())) {
            return;
        }
        Vec3[] axes = basis(snapshot.lookUnit());
        if (axes == null) {
            return;
        }
        Vec3 forwardUnit = axes[0];
        Vec3 upUnit = axes[1];
        Vec3 rightUnit = axes[2];
        // 通知里的 VOLUME 坐标已经是体积中心。
        Vec3 center = snapshot.anchor();
        int packedLight = LevelRenderer.getLightColor(clientLevel, BlockPos.containing(center));
        VertexConsumer consumer = buffers.getBuffer(slashType());
        pose.pushPose();
        pose.translate(center.x, center.y, center.z);
        PoseStack.Pose transformed = pose.last();
        emitBox(consumer, transformed.pose(), transformed.normal(), packedLight, rightUnit, upUnit, forwardUnit,
                snapshot.widthBlocks() * 0.5d, snapshot.heightBlocks() * 0.5d, snapshot.depthBlocks() * 0.5d);
        pose.popPose();
    }

    /** 视线接近竖直时用固定备用右轴。否则视线叉乘世界上方向得到右轴，上轴再由右轴叉乘视线。 */
    static Vec3[] basis(Vec3 lookUnit) {
        if (lookUnit == null || !finiteVector(lookUnit)) {
            return null;
        }
        Vec3 forwardUnit = lookUnit;
        Vec3 rightUnit;
        if (Math.abs(forwardUnit.y) > 0.999d) {
            rightUnit = new Vec3(1.0d, 0.0d, 0.0d);
        } else {
            Vec3 crossed = forwardUnit.cross(new Vec3(0.0d, 1.0d, 0.0d));
            if (crossed.lengthSqr() < 1.0e-8d) {
                return null;
            }
            rightUnit = crossed.normalize();
        }
        Vec3 upUnit = rightUnit.cross(forwardUnit);
        if (upUnit.lengthSqr() < 1.0e-8d) {
            return null;
        }
        upUnit = upUnit.normalize();
        if (!finiteVector(rightUnit) || !finiteVector(upUnit)) {
            return null;
        }
        return new Vec3[] { forwardUnit, upUnit, rightUnit };
    }

    private static boolean finiteSize(double extent) {
        return Double.isFinite(extent) && extent > 0.0d && extent <= BalanceBound.SIZE_UPPER;
    }

    private static boolean finiteVector(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static void emitBox(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, int packedLight,
                                Vec3 rightUnit, Vec3 upUnit, Vec3 forwardUnit, double halfWidth, double halfHeight,
                                double halfDepth) {
        Vec3 rightSpan = rightUnit.scale(halfWidth);
        Vec3 upSpan = upUnit.scale(halfHeight);
        Vec3 forwardSpan = forwardUnit.scale(halfDepth);
        Vec3 rightEdge = rightSpan.scale(2.0d);
        Vec3 upEdge = upSpan.scale(2.0d);
        Vec3 forwardEdge = forwardSpan.scale(2.0d);
        emitFace(consumer, matrix, normalMatrix, packedLight,
                rightSpan.subtract(upSpan).subtract(forwardSpan), upEdge, forwardEdge);
        emitFace(consumer, matrix, normalMatrix, packedLight,
                rightSpan.scale(-1.0d).subtract(upSpan).subtract(forwardSpan), upEdge, forwardEdge);
        emitFace(consumer, matrix, normalMatrix, packedLight,
                upSpan.subtract(rightSpan).subtract(forwardSpan), rightEdge, forwardEdge);
        emitFace(consumer, matrix, normalMatrix, packedLight,
                upSpan.scale(-1.0d).subtract(rightSpan).subtract(forwardSpan), rightEdge, forwardEdge);
        emitFace(consumer, matrix, normalMatrix, packedLight,
                forwardSpan.subtract(rightSpan).subtract(upSpan), rightEdge, upEdge);
        emitFace(consumer, matrix, normalMatrix, packedLight,
                forwardSpan.scale(-1.0d).subtract(rightSpan).subtract(upSpan), rightEdge, upEdge);
    }

    private static void emitFace(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, int packedLight,
                                 Vec3 corner, Vec3 edgeAcross, Vec3 edgeRise) {
        float cornerX = (float) corner.x;
        float cornerY = (float) corner.y;
        float cornerZ = (float) corner.z;
        float alongX = (float) (corner.x + edgeAcross.x);
        float alongY = (float) (corner.y + edgeAcross.y);
        float alongZ = (float) (corner.z + edgeAcross.z);
        float oppositeX = (float) (corner.x + edgeAcross.x + edgeRise.x);
        float oppositeY = (float) (corner.y + edgeAcross.y + edgeRise.y);
        float oppositeZ = (float) (corner.z + edgeAcross.z + edgeRise.z);
        float riseX = (float) (corner.x + edgeRise.x);
        float riseY = (float) (corner.y + edgeRise.y);
        float riseZ = (float) (corner.z + edgeRise.z);
        float spanX = alongX - cornerX;
        float spanY = alongY - cornerY;
        float spanZ = alongZ - cornerZ;
        float sideX = riseX - cornerX;
        float sideY = riseY - cornerY;
        float sideZ = riseZ - cornerZ;
        float normalX = spanY * sideZ - spanZ * sideY;
        float normalY = spanZ * sideX - spanX * sideZ;
        float normalZ = spanX * sideY - spanY * sideX;
        float normalSpan = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (normalSpan < 1.0E-4F) {
            return;
        }
        normalX /= normalSpan;
        normalY /= normalSpan;
        normalZ /= normalSpan;
        vertex(consumer, matrix, normalMatrix, packedLight, cornerX, cornerY, cornerZ, 0.0F, 1.0F,
                normalX, normalY, normalZ);
        vertex(consumer, matrix, normalMatrix, packedLight, alongX, alongY, alongZ, 1.0F, 1.0F,
                normalX, normalY, normalZ);
        vertex(consumer, matrix, normalMatrix, packedLight, oppositeX, oppositeY, oppositeZ, 1.0F, 0.0F,
                normalX, normalY, normalZ);
        vertex(consumer, matrix, normalMatrix, packedLight, riseX, riseY, riseZ, 0.0F, 0.0F,
                normalX, normalY, normalZ);
        vertex(consumer, matrix, normalMatrix, packedLight, riseX, riseY, riseZ, 0.0F, 0.0F,
                -normalX, -normalY, -normalZ);
        vertex(consumer, matrix, normalMatrix, packedLight, oppositeX, oppositeY, oppositeZ, 1.0F, 0.0F,
                -normalX, -normalY, -normalZ);
        vertex(consumer, matrix, normalMatrix, packedLight, alongX, alongY, alongZ, 1.0F, 1.0F,
                -normalX, -normalY, -normalZ);
        vertex(consumer, matrix, normalMatrix, packedLight, cornerX, cornerY, cornerZ, 0.0F, 1.0F,
                -normalX, -normalY, -normalZ);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix, int packedLight,
                               float pointX, float pointY, float pointZ, float textureU, float textureV,
                               float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(255, 255, 255, 255).uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normalMatrix, normalX, normalY, normalZ).endVertex();
    }
}
