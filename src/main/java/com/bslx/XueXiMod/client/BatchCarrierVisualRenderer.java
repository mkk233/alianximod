package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.carrier.BatchCarrierIds;
import com.bslx.XueXiMod.carrier.BatchCarrierVisual;
import com.bslx.XueXiMod.carrier.BatchVisualShape;
import com.bslx.XueXiMod.carrier.BatchVisualTexture;
import com.bslx.XueXiMod.carrier.DetonateCloudLayout;
import com.bslx.XueXiMod.carrier.GroundDustLayout;
import com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainTint;
import com.bslx.XueXiMod.visual.facing.CameraFacingRegistry;
import com.bslx.XueXiMod.visual.facing.CameraFacingTag;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 16 帧竖条按 V 分帧。不把 1024 高拉成一个四边形，也不在这里登记渲染器。
 * 火焰条带在幽冥体积内换确认版青蓝图，顶点保持白色。离开领域恢复原图。
 * 土尘、地表锚点和陨岩方块模型不换蓝图，也不乘旧的暗黄绿色。
 * 只有登记为持续面向玩家的贴图才读取相机朝向。引爆各团单独朝向，世界位置不整组旋转。
 */
public class BatchCarrierVisualRenderer extends EntityRenderer<BatchCarrierVisual> {
    public static final ResourceLocation METEOR_BLOCK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "textures/block/meteorite_v1.png");
    private static final double INV_SQRT_2 = 0.7071067811865476D;
    private static final double SPARK_FIRST_PERSON_DROP = 0.245D;
    private final BlockRenderDispatcher blockRenderer;

    public BatchCarrierVisualRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0F;
    }

    /**
     * 实体上存过的平色。绘制不再读它，避免一次写入就把火焰永久染蓝。
     * 当前帧的火色由 {@link #flameChannels(Level, double, double, double, BatchVisualTexture)} 决定。
     */
    public static int flatTintArgb(BatchCarrierVisual visual) {
        if (visual == null) {
            return 0xFFFFFF;
        }
        return visual.flatTintPacked() & 0xFFFFFF;
    }

    /**
     * 火焰条带的顶点色。青蓝图和原图都用白色，避免把 90,160,255 再乘一次。
     * 土尘和地表锚点固定白色。
     */
    public static NetherDomainTint.Sample flameChannels(Level level, double worldX, double worldY, double worldZ,
                                                        BatchVisualTexture texture) {
        if (!acceptsFlameTint(texture) || !NetherDomainTint.gloom(level, worldX, worldY, worldZ)) {
            return NetherDomainTint.Sample.plain();
        }
        return NetherDomainTint.Sample.soulWhite();
    }

    public static ResourceLocation flameSheet(Level level, double worldX, double worldY, double worldZ,
                                              BatchVisualTexture texture) {
        if (texture == null || texture.texturePath().isEmpty()) {
            return METEOR_BLOCK_TEXTURE;
        }
        ResourceLocation ordinary = ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, texture.texturePath());
        if (!acceptsFlameTint(texture)) {
            return ordinary;
        }
        return NetherBlueFlame.choose(level, worldX, worldY, worldZ, ordinary, blueSheet(texture));
    }

    private static ResourceLocation blueSheet(BatchVisualTexture texture) {
        if (texture == BatchVisualTexture.SPARK) {
            return NetherBlueFlame.sheet("fire_spark_v3.png");
        }
        if (texture == BatchVisualTexture.TRAIL) {
            return NetherBlueFlame.sheet("flame_trail_v3.png");
        }
        if (texture == BatchVisualTexture.SHOCK) {
            return NetherBlueFlame.sheet("shock_ring_v1.png");
        }
        if (texture == BatchVisualTexture.SEED) {
            return NetherBlueFlame.sheet("seed_sow_v3.png");
        }
        if (texture == BatchVisualTexture.KINDLE) {
            return NetherBlueFlame.sheet("kindle_v1.png");
        }
        if (texture == BatchVisualTexture.DETONATE) {
            return detonateSheet("explosion_puff_soul.png");
        }
        return null;
    }

    private static ResourceLocation detonateSheet(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "textures/effect/detonate_v2_3d/" + fileName);
    }

    public static boolean acceptsFlameTint(BatchVisualTexture texture) {
        return texture == BatchVisualTexture.SPARK
                || texture == BatchVisualTexture.TRAIL
                || texture == BatchVisualTexture.SHOCK
                || texture == BatchVisualTexture.SEED
                || texture == BatchVisualTexture.KINDLE
                || texture == BatchVisualTexture.DETONATE;
    }

    public static int frameIndex(int ageTicks, float partialTick, int durationTicks) {
        if (durationTicks <= 0) {
            return 0;
        }
        float elapsed = ageTicks + partialTick;
        if (elapsed < 0.0F) {
            elapsed = 0.0F;
        }
        int frame = (int) Math.floor(elapsed * BatchCarrierIds.STRIP_FRAMES / (float) durationTicks);
        if (frame < 0) {
            return 0;
        }
        if (frame >= BatchCarrierIds.STRIP_FRAMES) {
            return BatchCarrierIds.STRIP_FRAMES - 1;
        }
        return frame;
    }

    @Override
    public boolean shouldRender(BatchCarrierVisual visual, Frustum camera, double camX, double camY, double camZ) {
        if (visual.shape() == BatchVisualShape.HIDDEN) {
            return false;
        }
        double reach = Math.max(1.0D, visual.halfSpan());
        if (visual.textureKind() == BatchVisualTexture.DETONATE
                || visual.textureKind() == BatchVisualTexture.DUST) {
            reach = Math.max(reach, 3.0D);
        }
        return camera.isVisible(visual.getBoundingBox().inflate(reach));
    }

    @Override
    public void render(BatchCarrierVisual visual, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        BatchVisualShape shape = visual.shape();
        if (shape == BatchVisualShape.HIDDEN) {
            return;
        }
        if (shape == BatchVisualShape.METEOR_BLOCK) {
            renderMeteorBlock(pose, buffer, packedLight);
            return;
        }
        BatchVisualTexture texture = visual.textureKind();
        if (texture == BatchVisualTexture.NONE || texture.texturePath().isEmpty()) {
            return;
        }
        if (texture == BatchVisualTexture.DETONATE) {
            drawDetonateCloud(visual, partialTick, pose, buffer);
            return;
        }
        if (texture == BatchVisualTexture.DUST) {
            drawGroundDust(visual, partialTick, pose, buffer, packedLight);
            return;
        }
        int frame = frameIndex(visual.tickCount, partialTick, visual.durationTicks());
        float topV = frame / (float) BatchCarrierIds.STRIP_FRAMES;
        float bottomV = (frame + 1) / (float) BatchCarrierIds.STRIP_FRAMES;
        double worldX = visual.xOld + (visual.getX() - visual.xOld) * partialTick;
        double worldY = visual.yOld + (visual.getY() - visual.yOld) * partialTick;
        double worldZ = visual.zOld + (visual.getZ() - visual.zOld) * partialTick;
        if (shape == BatchVisualShape.RING) {
            worldY += 0.05D;
        }
        ResourceLocation sheet = flameSheet(visual.level(), worldX, worldY, worldZ, texture);
        NetherDomainTint.Sample tint = flameChannels(visual.level(), worldX, worldY, worldZ, texture);
        int redChannel = tint.red();
        int greenChannel = tint.green();
        int blueChannel = tint.blue();
        int light = LightTexture.FULL_BRIGHT;
        float half = visual.halfSpan();
        CameraFacingTag facingTag = CameraFacingRegistry.tagOf(texture);
        pose.pushPose();
        if (shape == BatchVisualShape.RING) {
            pose.translate(0.0D, 0.05D, 0.0D);
            drawHorizontal(pose, buffer, sheet, half, topV, bottomV, redChannel, greenChannel, blueChannel, light);
        } else if (texture == BatchVisualTexture.TRAIL && facingTag == CameraFacingTag.NONE) {
            drawTrailRibbon(pose, buffer, visual, sheet, half, topV, bottomV, redChannel, greenChannel, blueChannel, light);
        } else if (texture == BatchVisualTexture.SPARK && facingTag == CameraFacingTag.NONE) {
            drawSparkCross(pose, buffer, visual, sheet, half, topV, bottomV, redChannel, greenChannel, blueChannel, light);
        } else if (texture == BatchVisualTexture.SURFACE && facingTag == CameraFacingTag.NONE) {
            drawSurfaceAnchor(pose, buffer, sheet, half, topV, bottomV, redChannel, greenChannel, blueChannel);
        } else if (facingTag == CameraFacingTag.CAMERA_PLANE) {
            CameraFacingClient.orientDeclaredPlane(
                    pose, texture, this.entityRenderDispatcher.cameraOrientation());
            drawBillboard(pose, buffer, sheet, half, topV, bottomV, redChannel, greenChannel, blueChannel, light);
        } else {
            drawBillboard(pose, buffer, sheet, half, topV, bottomV, redChannel, greenChannel, blueChannel, light);
        }
        pose.popPose();
    }

    /**
     * 确认版九条尘流加十二颗粗粒。世界位移不跟相机转，尘流卡片才朝向镜头。
     * 粗粒是有体积的小立方体，不画成平面卡片。
     */
    private void drawGroundDust(BatchCarrierVisual visual, float partialTick, PoseStack pose,
                                MultiBufferSource buffer, int packedLight) {
        double elapsedTicks = visual.tickCount + partialTick;
        if (elapsedTicks < 0.0D) {
            elapsedTicks = 0.0D;
        }
        double anchorX = visual.xOld + (visual.getX() - visual.xOld) * partialTick;
        double anchorY = visual.yOld + (visual.getY() - visual.yOld) * partialTick;
        double anchorZ = visual.zOld + (visual.getZ() - visual.zOld) * partialTick;
        ResourceLocation dustSheet = ResourceLocation.fromNamespaceAndPath(
                XueXiMod.MOD_ID, visual.textureKind().texturePath());
        ResourceLocation gritSheet = ResourceLocation.fromNamespaceAndPath(
                XueXiMod.MOD_ID, "textures/effect/ground_dust_v2r2/visual_grit.png");
        Vec3 cameraPosition = this.entityRenderDispatcher.camera.getPosition();
        Quaternionf cameraFacing = this.entityRenderDispatcher.cameraOrientation();
        VertexConsumer dustConsumer = buffer.getBuffer(GroundDustMesh.of(dustSheet));
        int[] dustOrder = groundDustOrder(elapsedTicks, cameraPosition, anchorX, anchorY, anchorZ);
        for (int orderIndex = 0; orderIndex < dustOrder.length; orderIndex++) {
            int dustIndex = dustOrder[orderIndex];
            if (dustIndex >= 0) {
                drawDustStream(pose, dustConsumer, cameraFacing, dustIndex, elapsedTicks, packedLight);
            }
        }
        VertexConsumer gritConsumer = buffer.getBuffer(GroundDustMesh.of(gritSheet));
        int[] gritOrder = groundGritOrder(elapsedTicks, cameraPosition, anchorX, anchorY, anchorZ);
        for (int orderIndex = 0; orderIndex < gritOrder.length; orderIndex++) {
            int gritIndex = gritOrder[orderIndex];
            if (gritIndex >= 0) {
                drawGritCube(pose, gritConsumer, gritIndex, elapsedTicks, packedLight);
            }
        }
    }

    private static int[] groundDustOrder(double elapsedTicks, Vec3 cameraPosition,
                                         double anchorX, double anchorY, double anchorZ) {
        int[] order = new int[GroundDustLayout.DUST_COUNT];
        double[] depth = new double[GroundDustLayout.DUST_COUNT];
        int visibleCount = 0;
        double viewX = cameraPosition.x - anchorX;
        double viewY = cameraPosition.y - anchorY;
        double viewZ = cameraPosition.z - anchorZ;
        for (int dustIndex = 0; dustIndex < GroundDustLayout.DUST_COUNT; dustIndex++) {
            order[dustIndex] = -1;
            if (!GroundDustLayout.dustVisible(dustIndex, elapsedTicks)) {
                continue;
            }
            order[visibleCount] = dustIndex;
            depth[visibleCount] = GroundDustLayout.dustOffsetX(dustIndex, elapsedTicks) * viewX
                    + GroundDustLayout.dustOffsetY(dustIndex, elapsedTicks) * viewY
                    + GroundDustLayout.dustOffsetZ(dustIndex, elapsedTicks) * viewZ;
            visibleCount++;
        }
        sortFarToNear(order, depth, visibleCount);
        return order;
    }

    private static int[] groundGritOrder(double elapsedTicks, Vec3 cameraPosition,
                                         double anchorX, double anchorY, double anchorZ) {
        int[] order = new int[GroundDustLayout.GRIT_COUNT];
        double[] depth = new double[GroundDustLayout.GRIT_COUNT];
        int visibleCount = 0;
        double viewX = cameraPosition.x - anchorX;
        double viewY = cameraPosition.y - anchorY;
        double viewZ = cameraPosition.z - anchorZ;
        for (int gritIndex = 0; gritIndex < GroundDustLayout.GRIT_COUNT; gritIndex++) {
            order[gritIndex] = -1;
            if (!GroundDustLayout.gritVisible(gritIndex, elapsedTicks)) {
                continue;
            }
            order[visibleCount] = gritIndex;
            depth[visibleCount] = GroundDustLayout.gritOffsetX(gritIndex, elapsedTicks) * viewX
                    + GroundDustLayout.gritOffsetY(gritIndex, elapsedTicks) * viewY
                    + GroundDustLayout.gritOffsetZ(gritIndex, elapsedTicks) * viewZ;
            visibleCount++;
        }
        sortFarToNear(order, depth, visibleCount);
        return order;
    }

    private static void sortFarToNear(int[] order, double[] depth, int visibleCount) {
        for (int leftIndex = 1; leftIndex < visibleCount; leftIndex++) {
            int held = order[leftIndex];
            double heldDepth = depth[leftIndex];
            int insertionIndex = leftIndex;
            while (insertionIndex > 0 && depth[insertionIndex - 1] > heldDepth) {
                order[insertionIndex] = order[insertionIndex - 1];
                depth[insertionIndex] = depth[insertionIndex - 1];
                insertionIndex--;
            }
            order[insertionIndex] = held;
            depth[insertionIndex] = heldDepth;
        }
    }

    private static void drawDustStream(PoseStack pose, VertexConsumer consumer, Quaternionf cameraFacing,
                                       int dustIndex, double elapsedTicks, int packedLight) {
        double size = GroundDustLayout.dustSize(dustIndex);
        double halfWidth = size * 0.5D;
        double bottom = -size / 12.0D;
        double top = size * 11.0D / 12.0D;
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f upAxis = new Vector3f(0.0F, 1.0F, 0.0F);
        cameraFacing.transform(right);
        cameraFacing.transform(upAxis);
        float centerX = (float) GroundDustLayout.dustOffsetX(dustIndex, elapsedTicks);
        float centerY = (float) GroundDustLayout.dustOffsetY(dustIndex, elapsedTicks);
        float centerZ = (float) GroundDustLayout.dustOffsetZ(dustIndex, elapsedTicks);
        float leftX = centerX - right.x * (float) halfWidth;
        float leftY = centerY - right.y * (float) halfWidth;
        float leftZ = centerZ - right.z * (float) halfWidth;
        float rightX = centerX + right.x * (float) halfWidth;
        float rightY = centerY + right.y * (float) halfWidth;
        float rightZ = centerZ + right.z * (float) halfWidth;
        int bank = GroundDustLayout.dustBank(dustIndex);
        int frame = GroundDustLayout.dustFrame(dustIndex, elapsedTicks);
        float leftU = bank / (float) GroundDustLayout.DUST_BANKS;
        float rightU = (bank + 1) / (float) GroundDustLayout.DUST_BANKS;
        float topV = frame / (float) GroundDustLayout.DUST_FRAMES;
        float bottomV = (frame + 1) / (float) GroundDustLayout.DUST_FRAMES;
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        putGroundDustVertex(consumer, matrix, normal,
                leftX + upAxis.x * (float) bottom, leftY + upAxis.y * (float) bottom, leftZ + upAxis.z * (float) bottom,
                leftU, bottomV, packedLight);
        putGroundDustVertex(consumer, matrix, normal,
                rightX + upAxis.x * (float) bottom, rightY + upAxis.y * (float) bottom, rightZ + upAxis.z * (float) bottom,
                rightU, bottomV, packedLight);
        putGroundDustVertex(consumer, matrix, normal,
                rightX + upAxis.x * (float) top, rightY + upAxis.y * (float) top, rightZ + upAxis.z * (float) top,
                rightU, topV, packedLight);
        putGroundDustVertex(consumer, matrix, normal,
                leftX + upAxis.x * (float) top, leftY + upAxis.y * (float) top, leftZ + upAxis.z * (float) top,
                leftU, topV, packedLight);
    }

    private static void drawGritCube(PoseStack pose, VertexConsumer consumer, int gritIndex,
                                     double elapsedTicks, int packedLight) {
        float half = (float) (GroundDustLayout.gritSize(gritIndex) * 0.5D);
        Quaternionf rotation = new Quaternionf()
                .rotateYXZ(
                        (float) Math.toRadians(GroundDustLayout.gritYawDegrees(gritIndex, elapsedTicks)),
                        (float) Math.toRadians(GroundDustLayout.gritPitchDegrees(gritIndex, elapsedTicks)),
                        (float) Math.toRadians(GroundDustLayout.gritRollDegrees(gritIndex, elapsedTicks)));
        Vector3f axisX = new Vector3f(half, 0.0F, 0.0F);
        Vector3f axisY = new Vector3f(0.0F, half, 0.0F);
        Vector3f axisZ = new Vector3f(0.0F, 0.0F, half);
        rotation.transform(axisX);
        rotation.transform(axisY);
        rotation.transform(axisZ);
        float centerX = (float) GroundDustLayout.gritOffsetX(gritIndex, elapsedTicks);
        float centerY = (float) GroundDustLayout.gritOffsetY(gritIndex, elapsedTicks);
        float centerZ = (float) GroundDustLayout.gritOffsetZ(gritIndex, elapsedTicks);
        int bank = GroundDustLayout.gritBank(gritIndex);
        int row = GroundDustLayout.gritAlphaRow(gritIndex, elapsedTicks);
        float leftU = bank / (float) GroundDustLayout.GRIT_BANKS;
        float rightU = (bank + 1) / (float) GroundDustLayout.GRIT_BANKS;
        float topV = row / (float) GroundDustLayout.GRIT_ROWS;
        float bottomV = (row + 1) / (float) GroundDustLayout.GRIT_ROWS;
        Matrix4f matrix = pose.last().pose();
        Matrix3f normalMatrix = pose.last().normal();
        putGritFace(consumer, matrix, normalMatrix, centerX, centerY, centerZ, axisZ, axisX, axisY,
                leftU, rightU, topV, bottomV, packedLight);
        putGritFace(consumer, matrix, normalMatrix, centerX, centerY, centerZ, axisZ.negate(new Vector3f()),
                axisX.negate(new Vector3f()), axisY, leftU, rightU, topV, bottomV, packedLight);
        putGritFace(consumer, matrix, normalMatrix, centerX, centerY, centerZ, axisX, axisZ.negate(new Vector3f()), axisY,
                leftU, rightU, topV, bottomV, packedLight);
        putGritFace(consumer, matrix, normalMatrix, centerX, centerY, centerZ, axisX.negate(new Vector3f()), axisZ, axisY,
                leftU, rightU, topV, bottomV, packedLight);
        putGritFace(consumer, matrix, normalMatrix, centerX, centerY, centerZ, axisY, axisX, axisZ.negate(new Vector3f()),
                leftU, rightU, topV, bottomV, packedLight);
        putGritFace(consumer, matrix, normalMatrix, centerX, centerY, centerZ, axisY.negate(new Vector3f()), axisX, axisZ,
                leftU, rightU, topV, bottomV, packedLight);
    }

    private static void putGritFace(VertexConsumer consumer, Matrix4f matrix, Matrix3f normalMatrix,
                                    float centerX, float centerY, float centerZ,
                                    Vector3f outward, Vector3f right, Vector3f upAxis,
                                    float leftU, float rightU, float topV, float bottomV, int packedLight) {
        Vector3f normal = outward.normalize(new Vector3f());
        putGroundDustVertex(consumer, matrix, normalMatrix,
                centerX - right.x - upAxis.x + outward.x, centerY - right.y - upAxis.y + outward.y, centerZ - right.z - upAxis.z + outward.z,
                leftU, bottomV, normal.x, normal.y, normal.z, packedLight);
        putGroundDustVertex(consumer, matrix, normalMatrix,
                centerX + right.x - upAxis.x + outward.x, centerY + right.y - upAxis.y + outward.y, centerZ + right.z - upAxis.z + outward.z,
                rightU, bottomV, normal.x, normal.y, normal.z, packedLight);
        putGroundDustVertex(consumer, matrix, normalMatrix,
                centerX + right.x + upAxis.x + outward.x, centerY + right.y + upAxis.y + outward.y, centerZ + right.z + upAxis.z + outward.z,
                rightU, topV, normal.x, normal.y, normal.z, packedLight);
        putGroundDustVertex(consumer, matrix, normalMatrix,
                centerX - right.x + upAxis.x + outward.x, centerY - right.y + upAxis.y + outward.y, centerZ - right.z + upAxis.z + outward.z,
                leftU, topV, normal.x, normal.y, normal.z, packedLight);
    }

    private static void putGroundDustVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                           float pointX, float pointY, float pointZ,
                                           float textureU, float textureV, int packedLight) {
        putGroundDustVertex(consumer, matrix, normal, pointX, pointY, pointZ, textureU, textureV,
                0.0F, 0.0F, 1.0F, packedLight);
    }

    private static void putGroundDustVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                           float pointX, float pointY, float pointZ,
                                           float textureU, float textureV,
                                           float normalX, float normalY, float normalZ, int packedLight) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(255, 255, 255, 255)
                .uv(textureU, textureV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ).endVertex();
    }

    /**
     * 一个视觉载体展开确认版的 26 团。各团有独立世界中心、寿命、atlas 帧和滚转。
     * 先画远离相机的团。尺寸只读取已同步的视觉比例，不读取或改变伤害半径。
     */
    private void drawDetonateCloud(BatchCarrierVisual visual, float partialTick, PoseStack pose,
                                   MultiBufferSource buffer) {
        double elapsedTicks = visual.tickCount + partialTick;
        if (elapsedTicks < 0.0D) {
            elapsedTicks = 0.0D;
        }
        double visualScale = visual.halfSpan();
        if (!(visualScale > 0.0D) || !Double.isFinite(visualScale)) {
            return;
        }
        double anchorX = visual.xOld + (visual.getX() - visual.xOld) * partialTick;
        double anchorY = visual.yOld + (visual.getY() - visual.yOld) * partialTick;
        double anchorZ = visual.zOld + (visual.getZ() - visual.zOld) * partialTick;
        Vec3 cameraPosition = this.entityRenderDispatcher.camera.getPosition();
        Quaternionf cameraFacing = this.entityRenderDispatcher.cameraOrientation();
        ResourceLocation sheet = flameSheet(visual.level(), anchorX, anchorY, anchorZ, BatchVisualTexture.DETONATE);
        NetherDomainTint.Sample tint = flameChannels(visual.level(), anchorX, anchorY, anchorZ, BatchVisualTexture.DETONATE);
        VertexConsumer consumer = buffer.getBuffer(DetonateCloudMesh.of(sheet));
        int[] cloudOrder = detonateOrder(anchorX, anchorY, anchorZ, elapsedTicks, visualScale, cameraPosition);
        for (int orderIndex = 0; orderIndex < cloudOrder.length; orderIndex++) {
            int cloudIndex = cloudOrder[orderIndex];
            if (cloudIndex < 0) {
                continue;
            }
            drawDetonatePuff(pose, consumer, cameraFacing, cloudIndex,
                    elapsedTicks, visualScale, tint.red(), tint.green(), tint.blue());
        }
    }

    private static int[] detonateOrder(double anchorX, double anchorY, double anchorZ, double elapsedTicks,
                                       double visualScale, Vec3 cameraPosition) {
        int[] cloudOrder = new int[DetonateCloudLayout.CLOUD_COUNT];
        double[] cameraDepth = new double[DetonateCloudLayout.CLOUD_COUNT];
        int visibleCount = 0;
        double cameraDeltaX = cameraPosition.x - anchorX;
        double cameraDeltaY = cameraPosition.y - anchorY;
        double cameraDeltaZ = cameraPosition.z - anchorZ;
        double viewX = cameraDeltaX;
        double viewY = cameraDeltaY;
        double viewZ = cameraDeltaZ;
        for (int cloudIndex = 0; cloudIndex < DetonateCloudLayout.CLOUD_COUNT; cloudIndex++) {
            cloudOrder[cloudIndex] = -1;
            double ageTicks = DetonateCloudLayout.ageTicks(cloudIndex, elapsedTicks);
            if (!DetonateCloudLayout.visible(cloudIndex, ageTicks)) {
                continue;
            }
            double travelBlocks = DetonateCloudLayout.travelBlocks(cloudIndex, ageTicks, visualScale);
            double offsetX = DetonateCloudLayout.directionX(cloudIndex) * travelBlocks;
            double offsetY = DetonateCloudLayout.directionY(cloudIndex) * travelBlocks
                    + DetonateCloudLayout.liftBlocks(ageTicks, visualScale);
            double offsetZ = DetonateCloudLayout.directionZ(cloudIndex) * travelBlocks;
            cloudOrder[visibleCount] = cloudIndex;
            cameraDepth[visibleCount] = offsetX * viewX + offsetY * viewY + offsetZ * viewZ;
            visibleCount++;
        }
        for (int leftIndex = 1; leftIndex < visibleCount; leftIndex++) {
            int heldCloud = cloudOrder[leftIndex];
            double heldDepth = cameraDepth[leftIndex];
            int insertionIndex = leftIndex;
            while (insertionIndex > 0 && cameraDepth[insertionIndex - 1] > heldDepth) {
                cloudOrder[insertionIndex] = cloudOrder[insertionIndex - 1];
                cameraDepth[insertionIndex] = cameraDepth[insertionIndex - 1];
                insertionIndex--;
            }
            cloudOrder[insertionIndex] = heldCloud;
            cameraDepth[insertionIndex] = heldDepth;
        }
        return cloudOrder;
    }

    private static void drawDetonatePuff(PoseStack pose, VertexConsumer consumer, Quaternionf cameraFacing,
                                         int cloudIndex, double elapsedTicks, double visualScale,
                                         int redChannel, int greenChannel, int blueChannel) {
        double ageTicks = DetonateCloudLayout.ageTicks(cloudIndex, elapsedTicks);
        double travelBlocks = DetonateCloudLayout.travelBlocks(cloudIndex, ageTicks, visualScale);
        double localX = DetonateCloudLayout.directionX(cloudIndex) * travelBlocks;
        double localY = DetonateCloudLayout.directionY(cloudIndex) * travelBlocks
                + DetonateCloudLayout.liftBlocks(ageTicks, visualScale);
        double localZ = DetonateCloudLayout.directionZ(cloudIndex) * travelBlocks;
        float halfSpan = DetonateCloudLayout.halfSpanBlocks(cloudIndex, visualScale);
        int atlasFrame = DetonateCloudLayout.atlasFrame(cloudIndex, ageTicks);
        float topV = atlasFrame * DetonateCloudLayout.FRAME_HEIGHT;
        float bottomV = (atlasFrame + 1) * DetonateCloudLayout.FRAME_HEIGHT;
        Quaternionf puffFacing = new Quaternionf(cameraFacing);
        puffFacing.rotateZ((float) Math.toRadians(DetonateCloudLayout.rollDegrees(cloudIndex)));
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F);
        Vector3f upAxis = new Vector3f(0.0F, 1.0F, 0.0F);
        puffFacing.transform(right);
        puffFacing.transform(upAxis);
        right.mul(halfSpan);
        upAxis.mul(halfSpan);
        float leftX = (float) localX - right.x;
        float leftY = (float) localY - right.y;
        float leftZ = (float) localZ - right.z;
        float rightX = (float) localX + right.x;
        float rightY = (float) localY + right.y;
        float rightZ = (float) localZ + right.z;
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        putDetonateVertex(consumer, matrix, normal, leftX - upAxis.x, leftY - upAxis.y, leftZ - upAxis.z,
                0.0F, bottomV, redChannel, greenChannel, blueChannel);
        putDetonateVertex(consumer, matrix, normal, rightX - upAxis.x, rightY - upAxis.y, rightZ - upAxis.z,
                1.0F, bottomV, redChannel, greenChannel, blueChannel);
        putDetonateVertex(consumer, matrix, normal, rightX + upAxis.x, rightY + upAxis.y, rightZ + upAxis.z,
                1.0F, topV, redChannel, greenChannel, blueChannel);
        putDetonateVertex(consumer, matrix, normal, leftX + upAxis.x, leftY + upAxis.y, leftZ + upAxis.z,
                0.0F, topV, redChannel, greenChannel, blueChannel);
    }

    private static void putDetonateVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                          float pointX, float pointY, float pointZ, float textureU, float textureV,
                                          int redChannel, int greenChannel, int blueChannel) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(textureU, textureV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
    }

    private void renderMeteorBlock(PoseStack pose, MultiBufferSource buffer, int packedLight) {
        if (!ModBlocks.METEORITE.isPresent()) {
            return;
        }
        pose.pushPose();
        pose.translate(-0.5D, 0.0D, -0.5D);
        this.blockRenderer.renderSingleBlock(
                ModBlocks.METEORITE.get().defaultBlockState(),
                pose,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /**
     * 留焰沿世界弹道切向直立。贴图顶部朝天空，双侧单面各偏一微层，较新采样压在较旧采样前面。
     * 火苗宿主的采样再套用火苗面片的视觉锚点，不另加偏移。
     */
    private static void drawTrailRibbon(PoseStack pose, MultiBufferSource buffer, BatchCarrierVisual visual,
                                        ResourceLocation sheet, float half, float topV, float bottomV,
                                        int redChannel, int greenChannel, int blueChannel, int packedLight) {
        Vec3 tangent = unitTangent(visual.tangentX(), visual.tangentY(), visual.tangentZ());
        Vec3 referenceUp = Math.abs(tangent.y) < 0.999D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        double tangentDotUp = referenceUp.dot(tangent);
        Vec3 upAxis = unitTangent(
                referenceUp.x - tangent.x * tangentDotUp,
                referenceUp.y - tangent.y * tangentDotUp,
                referenceUp.z - tangent.z * tangentDotUp);
        Vec3 sideNormal = unitTangent(
                tangent.y * upAxis.z - tangent.z * upAxis.y,
                tangent.z * upAxis.x - tangent.x * upAxis.z,
                tangent.x * upAxis.y - tangent.y * upAxis.x);
        if (visual.sparkMeshAnchor()) {
            Vec3 lockedLaunch = unitLaunch(visual.sparkLaunchX(), visual.sparkLaunchY(), visual.sparkLaunchZ());
            translateSparkMeshAnchor(pose, sparkAnchorUp(lockedLaunch));
        }
        float sideOffset = ((visual.sampleOrder() & 31) + 1) * 0.00025F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(sheet));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        putTrailFace(consumer, matrix, normal, tangent, upAxis, sideNormal, half, sideOffset, true,
                topV, bottomV, redChannel, greenChannel, blueChannel, packedLight);
        putTrailFace(consumer, matrix, normal, tangent, upAxis, sideNormal, half, sideOffset, false,
                topV, bottomV, redChannel, greenChannel, blueChannel, packedLight);
    }

    private static void putTrailFace(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, Vec3 tangent,
                                     Vec3 upAxis, Vec3 sideNormal, float half, float sideOffset, boolean positiveSide,
                                     float topV, float bottomV, int redChannel, int greenChannel, int blueChannel,
                                     int packedLight) {
        float layerOffset = positiveSide ? sideOffset : -sideOffset;
        float normalX = positiveSide ? (float) sideNormal.x : (float) -sideNormal.x;
        float normalY = positiveSide ? (float) sideNormal.y : (float) -sideNormal.y;
        float normalZ = positiveSide ? (float) sideNormal.z : (float) -sideNormal.z;
        float tailBottomX = trailPoint(-half, -half, layerOffset, tangent, upAxis, sideNormal, 0);
        float tailBottomY = trailPoint(-half, -half, layerOffset, tangent, upAxis, sideNormal, 1);
        float tailBottomZ = trailPoint(-half, -half, layerOffset, tangent, upAxis, sideNormal, 2);
        float headBottomX = trailPoint(half, -half, layerOffset, tangent, upAxis, sideNormal, 0);
        float headBottomY = trailPoint(half, -half, layerOffset, tangent, upAxis, sideNormal, 1);
        float headBottomZ = trailPoint(half, -half, layerOffset, tangent, upAxis, sideNormal, 2);
        float headTopX = trailPoint(half, half, layerOffset, tangent, upAxis, sideNormal, 0);
        float headTopY = trailPoint(half, half, layerOffset, tangent, upAxis, sideNormal, 1);
        float headTopZ = trailPoint(half, half, layerOffset, tangent, upAxis, sideNormal, 2);
        float tailTopX = trailPoint(-half, half, layerOffset, tangent, upAxis, sideNormal, 0);
        float tailTopY = trailPoint(-half, half, layerOffset, tangent, upAxis, sideNormal, 1);
        float tailTopZ = trailPoint(-half, half, layerOffset, tangent, upAxis, sideNormal, 2);
        if (positiveSide) {
            putTrailVertex(consumer, matrix, normal, tailBottomX, tailBottomY, tailBottomZ, 0.0F, bottomV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putTrailVertex(consumer, matrix, normal, headBottomX, headBottomY, headBottomZ, 1.0F, bottomV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putTrailVertex(consumer, matrix, normal, headTopX, headTopY, headTopZ, 1.0F, topV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putTrailVertex(consumer, matrix, normal, tailTopX, tailTopY, tailTopZ, 0.0F, topV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
        } else {
            putTrailVertex(consumer, matrix, normal, tailTopX, tailTopY, tailTopZ, 0.0F, topV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putTrailVertex(consumer, matrix, normal, headTopX, headTopY, headTopZ, 1.0F, topV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putTrailVertex(consumer, matrix, normal, headBottomX, headBottomY, headBottomZ, 1.0F, bottomV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putTrailVertex(consumer, matrix, normal, tailBottomX, tailBottomY, tailBottomZ, 0.0F, bottomV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
        }
    }

    private static float trailPoint(float tangentScale, float upScale, float sideScale, Vec3 tangent, Vec3 upAxis,
                                    Vec3 sideNormal, int axisIndex) {
        double component = tangentScale * tangentComponent(tangent, axisIndex)
                + upScale * tangentComponent(upAxis, axisIndex)
                + sideScale * tangentComponent(sideNormal, axisIndex);
        return (float) component;
    }

    private static double tangentComponent(Vec3 axis, int axisIndex) {
        if (axisIndex == 0) {
            return axis.x;
        }
        if (axisIndex == 1) {
            return axis.y;
        }
        return axis.z;
    }

    private static Vec3 unitTangent(double tangentX, double tangentY, double tangentZ) {
        double lengthSquared = tangentX * tangentX + tangentY * tangentY + tangentZ * tangentZ;
        if (lengthSquared <= 1.0E-8D || !Double.isFinite(lengthSquared)) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        double length = Math.sqrt(lengthSquared);
        return new Vec3(tangentX / length, tangentY / length, tangentZ / length);
    }

    /**
     * 两片翼面绕发射方向锁定在 ±45°。火头在前方，火尾在后方，不跟相机转。
     */
    private static void drawSparkCross(PoseStack pose, MultiBufferSource buffer, BatchCarrierVisual visual,
                                       ResourceLocation sheet, float half, float topV, float bottomV,
                                       int redChannel, int greenChannel, int blueChannel, int packedLight) {
        Vec3 tangent = unitLaunch(visual.tangentX(), visual.tangentY(), visual.tangentZ());
        Vec3 upAxis = sparkAnchorUp(tangent);
        Vec3 sideAxis = unitLaunch(
                tangent.y * upAxis.z - tangent.z * upAxis.y,
                tangent.z * upAxis.x - tangent.x * upAxis.z,
                tangent.x * upAxis.y - tangent.y * upAxis.x);
        Vec3 firstFin = upAxis.add(sideAxis).scale(INV_SQRT_2);
        Vec3 secondFin = upAxis.subtract(sideAxis).scale(INV_SQRT_2);
        pose.pushPose();
        try {
            translateSparkMeshAnchor(pose, upAxis);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(sheet));
            Matrix4f matrix = pose.last().pose();
            Matrix3f normal = pose.last().normal();
            putSparkFin(consumer, matrix, normal, tangent, firstFin, secondFin, half,
                    topV, bottomV, redChannel, greenChannel, blueChannel, packedLight);
            putSparkFin(consumer, matrix, normal, tangent, secondFin, firstFin.scale(-1.0D), half,
                    topV, bottomV, redChannel, greenChannel, blueChannel, packedLight);
        } finally {
            pose.popPose();
        }
    }

    private static void translateSparkMeshAnchor(PoseStack pose, Vec3 upAxis) {
        pose.translate(
                -upAxis.x * SPARK_FIRST_PERSON_DROP,
                -upAxis.y * SPARK_FIRST_PERSON_DROP,
                -upAxis.z * SPARK_FIRST_PERSON_DROP);
    }

    private static Vec3 sparkAnchorUp(Vec3 launch) {
        Vec3 referenceUp = Math.abs(launch.y) < 0.999D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        double launchDotUp = referenceUp.dot(launch);
        return unitLaunch(
                referenceUp.x - launch.x * launchDotUp,
                referenceUp.y - launch.y * launchDotUp,
                referenceUp.z - launch.z * launchDotUp);
    }

    private static void putSparkFin(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, Vec3 tangent,
                                    Vec3 finAxis, Vec3 faceNormal, float half,
                                    float topV, float bottomV, int redChannel, int greenChannel, int blueChannel,
                                    int packedLight) {
        float headTopX = sparkPoint(half, half, tangent, finAxis, 0);
        float headTopY = sparkPoint(half, half, tangent, finAxis, 1);
        float headTopZ = sparkPoint(half, half, tangent, finAxis, 2);
        float headBottomX = sparkPoint(half, -half, tangent, finAxis, 0);
        float headBottomY = sparkPoint(half, -half, tangent, finAxis, 1);
        float headBottomZ = sparkPoint(half, -half, tangent, finAxis, 2);
        float tailBottomX = sparkPoint(-half, -half, tangent, finAxis, 0);
        float tailBottomY = sparkPoint(-half, -half, tangent, finAxis, 1);
        float tailBottomZ = sparkPoint(-half, -half, tangent, finAxis, 2);
        float tailTopX = sparkPoint(-half, half, tangent, finAxis, 0);
        float tailTopY = sparkPoint(-half, half, tangent, finAxis, 1);
        float tailTopZ = sparkPoint(-half, half, tangent, finAxis, 2);
        putSparkSide(consumer, matrix, normal,
                headTopX, headTopY, headTopZ,
                headBottomX, headBottomY, headBottomZ,
                tailBottomX, tailBottomY, tailBottomZ,
                tailTopX, tailTopY, tailTopZ,
                faceNormal, true, topV, bottomV, redChannel, greenChannel, blueChannel, packedLight);
        putSparkSide(consumer, matrix, normal,
                tailTopX, tailTopY, tailTopZ,
                tailBottomX, tailBottomY, tailBottomZ,
                headBottomX, headBottomY, headBottomZ,
                headTopX, headTopY, headTopZ,
                faceNormal, false, topV, bottomV, redChannel, greenChannel, blueChannel, packedLight);
    }

    private static void putSparkSide(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                     float headTopX, float headTopY, float headTopZ,
                                     float headBottomX, float headBottomY, float headBottomZ,
                                     float tailBottomX, float tailBottomY, float tailBottomZ,
                                     float tailTopX, float tailTopY, float tailTopZ,
                                     Vec3 faceNormal, boolean positiveNormal,
                                     float topV, float bottomV, int redChannel, int greenChannel, int blueChannel,
                                     int packedLight) {
        float normalX = positiveNormal ? (float) faceNormal.x : (float) -faceNormal.x;
        float normalY = positiveNormal ? (float) faceNormal.y : (float) -faceNormal.y;
        float normalZ = positiveNormal ? (float) faceNormal.z : (float) -faceNormal.z;
        if (positiveNormal) {
            putSparkVertex(consumer, matrix, normal, tailTopX, tailTopY, tailTopZ, 0.0F, topV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putSparkVertex(consumer, matrix, normal, headTopX, headTopY, headTopZ, 1.0F, topV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putSparkVertex(consumer, matrix, normal, headBottomX, headBottomY, headBottomZ, 1.0F, bottomV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putSparkVertex(consumer, matrix, normal, tailBottomX, tailBottomY, tailBottomZ, 0.0F, bottomV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
        } else {
            putSparkVertex(consumer, matrix, normal, tailBottomX, tailBottomY, tailBottomZ, 0.0F, bottomV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putSparkVertex(consumer, matrix, normal, headBottomX, headBottomY, headBottomZ, 1.0F, bottomV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putSparkVertex(consumer, matrix, normal, headTopX, headTopY, headTopZ, 1.0F, topV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
            putSparkVertex(consumer, matrix, normal, tailTopX, tailTopY, tailTopZ, 0.0F, topV,
                    normalX, normalY, normalZ, redChannel, greenChannel, blueChannel, packedLight);
        }
    }

    private static float sparkPoint(float tangentScale, float finScale, Vec3 tangent, Vec3 finAxis, int axisIndex) {
        double component = tangentScale * tangentComponent(tangent, axisIndex)
                + finScale * tangentComponent(finAxis, axisIndex);
        return (float) component;
    }

    private static Vec3 unitLaunch(double launchX, double launchY, double launchZ) {
        double lengthSquared = launchX * launchX + launchY * launchY + launchZ * launchZ;
        if (lengthSquared <= 1.0E-8D || !Double.isFinite(lengthSquared)) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        double length = Math.sqrt(lengthSquared);
        return new Vec3(launchX / length, launchY / length, launchZ / length);
    }

    private static void putSparkVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                       float pointX, float pointY, float pointZ, float textureU, float textureV,
                                       float normalX, float normalY, float normalZ,
                                       int redChannel, int greenChannel, int blueChannel, int packedLight) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(textureU, textureV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ).endVertex();
    }

    private static void putTrailVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                       float pointX, float pointY, float pointZ, float textureU, float textureV,
                                       float normalX, float normalY, float normalZ,
                                       int redChannel, int greenChannel, int blueChannel, int packedLight) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(textureU, textureV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ).endVertex();
    }

    /**
     * 地表定位悬在选中方块顶面上方。两张竖直面绕世界 +Y 对角正交，不跟镜头转。
     * 宽顶在上，尖端朝下指锚点。
     */
    private static void drawSurfaceAnchor(PoseStack pose, MultiBufferSource buffer, ResourceLocation sheet,
                                          float halfSpan, float topV, float bottomV,
                                          int redChannel, int greenChannel, int blueChannel) {
        float yBottom = 0.04F;
        float yTop = 0.04F + halfSpan * 2.0F;
        float diagonal = halfSpan * (float) INV_SQRT_2;
        float axis = (float) INV_SQRT_2;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(sheet));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        putSurfacePlane(consumer, matrix, normal,
                -diagonal, yBottom, -diagonal,
                diagonal, yBottom, diagonal,
                diagonal, yTop, diagonal,
                -diagonal, yTop, -diagonal,
                -axis, 0.0F, axis,
                topV, bottomV, redChannel, greenChannel, blueChannel);
        putSurfacePlane(consumer, matrix, normal,
                -diagonal, yBottom, diagonal,
                diagonal, yBottom, -diagonal,
                diagonal, yTop, -diagonal,
                -diagonal, yTop, diagonal,
                axis, 0.0F, axis,
                topV, bottomV, redChannel, greenChannel, blueChannel);
    }

    private static void putSurfacePlane(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                        float leftBottomX, float leftBottomY, float leftBottomZ,
                                        float rightBottomX, float rightBottomY, float rightBottomZ,
                                        float rightTopX, float rightTopY, float rightTopZ,
                                        float leftTopX, float leftTopY, float leftTopZ,
                                        float normalX, float normalY, float normalZ,
                                        float topV, float bottomV,
                                        int redChannel, int greenChannel, int blueChannel) {
        putSurfaceVertex(consumer, matrix, normal, leftBottomX, leftBottomY, leftBottomZ, 0.0F, bottomV,
                normalX, normalY, normalZ, redChannel, greenChannel, blueChannel);
        putSurfaceVertex(consumer, matrix, normal, rightBottomX, rightBottomY, rightBottomZ, 1.0F, bottomV,
                normalX, normalY, normalZ, redChannel, greenChannel, blueChannel);
        putSurfaceVertex(consumer, matrix, normal, rightTopX, rightTopY, rightTopZ, 1.0F, topV,
                normalX, normalY, normalZ, redChannel, greenChannel, blueChannel);
        putSurfaceVertex(consumer, matrix, normal, leftTopX, leftTopY, leftTopZ, 0.0F, topV,
                normalX, normalY, normalZ, redChannel, greenChannel, blueChannel);
        putSurfaceVertex(consumer, matrix, normal, leftTopX, leftTopY, leftTopZ, 0.0F, topV,
                -normalX, -normalY, -normalZ, redChannel, greenChannel, blueChannel);
        putSurfaceVertex(consumer, matrix, normal, rightTopX, rightTopY, rightTopZ, 1.0F, topV,
                -normalX, -normalY, -normalZ, redChannel, greenChannel, blueChannel);
        putSurfaceVertex(consumer, matrix, normal, rightBottomX, rightBottomY, rightBottomZ, 1.0F, bottomV,
                -normalX, -normalY, -normalZ, redChannel, greenChannel, blueChannel);
        putSurfaceVertex(consumer, matrix, normal, leftBottomX, leftBottomY, leftBottomZ, 0.0F, bottomV,
                -normalX, -normalY, -normalZ, redChannel, greenChannel, blueChannel);
    }

    private static void putSurfaceVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                         float pointX, float pointY, float pointZ, float textureU, float textureV,
                                         float normalX, float normalY, float normalZ,
                                         int redChannel, int greenChannel, int blueChannel) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(textureU, textureV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ).endVertex();
    }

    private static void drawBillboard(PoseStack pose, MultiBufferSource buffer, ResourceLocation sheet, float half,
                                      float topV, float bottomV, int redChannel, int greenChannel, int blueChannel,
                                      int packedLight) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(sheet));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        putQuad(consumer, matrix, normal,
                -half, -half, 0.0F,
                half, -half, 0.0F,
                half, half, 0.0F,
                -half, half, 0.0F,
                topV, bottomV, redChannel, greenChannel, blueChannel, packedLight);
    }

    private static void drawHorizontal(PoseStack pose, MultiBufferSource buffer, ResourceLocation sheet, float half,
                                       float topV, float bottomV, int redChannel, int greenChannel, int blueChannel,
                                       int packedLight) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(sheet));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        putQuad(consumer, matrix, normal, -half, 0.0F, -half, half, 0.0F, -half, half, 0.0F, half, -half, 0.0F, half,
                topV, bottomV, redChannel, greenChannel, blueChannel, packedLight);
    }

    private static void putQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                float firstX, float firstY, float firstZ,
                                float secondX, float secondY, float secondZ,
                                float thirdX, float thirdY, float thirdZ,
                                float fourthX, float fourthY, float fourthZ,
                                float topV, float bottomV,
                                int redChannel, int greenChannel, int blueChannel, int packedLight) {
        consumer.vertex(matrix, firstX, firstY, firstZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(0.0F, bottomV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, secondX, secondY, secondZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(1.0F, bottomV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, thirdX, thirdY, thirdZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(1.0F, topV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, fourthX, fourthY, fourthZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(0.0F, topV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, fourthX, fourthY, fourthZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(0.0F, topV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, thirdX, thirdY, thirdZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(1.0F, topV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, secondX, secondY, secondZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(1.0F, bottomV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, firstX, firstY, firstZ).color(redChannel, greenChannel, blueChannel, 255)
                .uv(0.0F, bottomV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(BatchCarrierVisual visual) {
        if (visual.shape() == BatchVisualShape.METEOR_BLOCK) {
            return METEOR_BLOCK_TEXTURE;
        }
        BatchVisualTexture texture = visual.textureKind();
        if (texture.texturePath().isEmpty()) {
            return METEOR_BLOCK_TEXTURE;
        }
        return flameSheet(visual.level(), visual.getX(), visual.getY(), visual.getZ(), texture);
    }
}
