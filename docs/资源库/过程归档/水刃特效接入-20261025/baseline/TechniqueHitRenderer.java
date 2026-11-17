package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.ShulkerBulletModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class TechniqueHitRenderer extends EntityRenderer<TechniqueHitEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/shulker/spark.png");
    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");
    private static final float BEAM_CORE_RADIUS = 0.2F;
    private static final float BEAM_GLOW_RADIUS = 0.25F;
    private static final int BEAM_CORE_ALPHA = 255;
    private static final int BEAM_GLOW_ALPHA = 32;
    private final ShulkerBulletModel<TechniqueHitEntity> model;

    public TechniqueHitRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ShulkerBulletModel<>(context.bakeLayer(ModelLayers.SHULKER_BULLET));
    }

    @Override
    public boolean shouldRender(TechniqueHitEntity entity, Frustum camera, double camX, double camY, double camZ) {
        if (entity.hasArcBand()) {
            return camera.isVisible(entity.getBoundingBox().inflate(0.5));
        }
        if (entity.distanceToSqr(camX, camY, camZ) > 96.0 * 96.0) {
            return false;
        }
        if (entity.getVisual() == 6 || entity.getVisual() == 7 || entity.getVisual() == 8) {
            return camera.isVisible(entity.getBoundingBox().inflate(8.0));
        }
        return super.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public void render(TechniqueHitEntity entity, float entityYaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.hasArcBand()) {
            WaterArcRenderer.render(entity, partialTicks, pose, buffer);
            return;
        }
        int visual = entity.getVisual();
        if (visual == 2) {
            renderBeamColumn(entity, partialTicks, pose, buffer);
            return;
        }
        if (visual == 5) {
            WaterRibbonRenderer.render(entity, partialTicks, pose, buffer);
            return;
        }
        if (visual == 6) {
            WaterPartingRenderer.render(entity, partialTicks, pose, buffer);
            return;
        }
        if (visual == 7 || visual == 8) {
            WaterCurtainRenderer.render(entity, visual == 8, partialTicks, pose, buffer);
            return;
        }
        String texture = entity.texture();
        if (texture != null && !texture.isEmpty()) {
            ResourceLocation sheet = ResourceLocation.tryParse(texture);
            if (sheet != null) {
                renderSheet(entity, pose, buffer, packedLight, sheet);
                return;
            }
        }
        if (visual == 1 || visual == 4) {
            float scale = entity.getModelScale();
            float yRotation = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
            float xRotation = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            float spin = (float) entity.tickCount + partialTicks;
            pose.pushPose();
            pose.scale(scale, scale, scale);
            pose.translate(0.0F, 0.15F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(spin * 0.1F) * 180.0F));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.cos(spin * 0.1F) * 180.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(spin * 0.15F) * 360.0F));
            pose.scale(-0.5F, -0.5F, 0.5F);
            this.model.setupAnim(entity, 0.0F, 0.0F, 0.0F, yRotation, xRotation);
            VertexConsumer consumer = buffer.getBuffer(this.model.renderType(TEXTURE));
            this.model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
    }

    private void renderBeamColumn(TechniqueHitEntity entity, float partialTicks, PoseStack pose, MultiBufferSource buffer) {
        float yRotation = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        float xRotation = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        float beamLength = (float) entity.length();
        if (beamLength < 1.0F) {
            beamLength = 6.0F;
        }
        float nearDistance = 0.4F;
        float farDistance = Math.max(beamLength, nearDistance + 0.5F);
        float flow = (entity.tickCount + partialTicks) * 0.05F;
        float nearV = flow;
        float farV = flow + beamLength * 0.25F;
        float volumeScale = entity.getModelScale();
        if (!Float.isFinite(volumeScale) || volumeScale <= 0.001F) {
            volumeScale = 1.0F;
        }
        float coreRadius = BEAM_CORE_RADIUS * volumeScale;
        float glowRadius = BEAM_GLOW_RADIUS * volumeScale;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yRotation));
        pose.mulPose(Axis.XP.rotationDegrees(xRotation));
        Matrix4f matrix = pose.last().pose();
        org.joml.Matrix3f normalMatrix = pose.last().normal();
        int packedBright = LightTexture.FULL_BRIGHT;
        VertexConsumer glowConsumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, true));
        beamTube(glowConsumer, matrix, normalMatrix, packedBright, glowRadius, nearDistance, farDistance, nearV, farV, 255, 236, 196, BEAM_GLOW_ALPHA);
        beamCap(glowConsumer, matrix, normalMatrix, packedBright, glowRadius, nearDistance, 255, 236, 196, BEAM_GLOW_ALPHA);
        beamCap(glowConsumer, matrix, normalMatrix, packedBright, glowRadius, farDistance, 255, 236, 196, BEAM_GLOW_ALPHA);
        VertexConsumer coreConsumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_TEXTURE, false));
        beamTube(coreConsumer, matrix, normalMatrix, packedBright, coreRadius, nearDistance, farDistance, nearV, farV, 255, 236, 196, BEAM_CORE_ALPHA);
        beamCap(coreConsumer, matrix, normalMatrix, packedBright, coreRadius, nearDistance, 255, 236, 196, BEAM_CORE_ALPHA);
        beamCap(coreConsumer, matrix, normalMatrix, packedBright, coreRadius, farDistance, 255, 236, 196, BEAM_CORE_ALPHA);
        pose.popPose();
    }

    private static void beamTube(VertexConsumer consumer, Matrix4f matrix, org.joml.Matrix3f normalMatrix, int packedLight,
                                 float radius, float nearDistance, float farDistance, float nearV, float farV,
                                 int colorRed, int colorGreen, int colorBlue, int colorAlpha) {
        beamFace(consumer, matrix, normalMatrix, packedLight, radius, -radius, nearDistance, radius, -radius, farDistance, radius, radius, farDistance, radius, radius, nearDistance, nearV, farV, colorRed, colorGreen, colorBlue, colorAlpha);
        beamFace(consumer, matrix, normalMatrix, packedLight, -radius, radius, nearDistance, -radius, radius, farDistance, -radius, -radius, farDistance, -radius, -radius, nearDistance, nearV, farV, colorRed, colorGreen, colorBlue, colorAlpha);
        beamFace(consumer, matrix, normalMatrix, packedLight, -radius, radius, nearDistance, radius, radius, nearDistance, radius, radius, farDistance, -radius, radius, farDistance, nearV, farV, colorRed, colorGreen, colorBlue, colorAlpha);
        beamFace(consumer, matrix, normalMatrix, packedLight, radius, -radius, nearDistance, -radius, -radius, nearDistance, -radius, -radius, farDistance, radius, -radius, farDistance, nearV, farV, colorRed, colorGreen, colorBlue, colorAlpha);
    }

    private static void beamCap(VertexConsumer consumer, Matrix4f matrix, org.joml.Matrix3f normalMatrix, int packedLight,
                                float radius, float distance, int colorRed, int colorGreen, int colorBlue, int colorAlpha) {
        beamVertex(consumer, matrix, normalMatrix, packedLight, -radius, -radius, distance, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, radius, -radius, distance, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, radius, radius, distance, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, -radius, radius, distance, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, -radius, radius, distance, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, radius, radius, distance, 1.0F, 1.0F, 0.0F, 0.0F, -1.0F, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, radius, -radius, distance, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, -radius, -radius, distance, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, colorRed, colorGreen, colorBlue, colorAlpha);
    }

    private static void beamFace(VertexConsumer consumer, Matrix4f matrix, org.joml.Matrix3f normalMatrix, int packedLight,
                                 float firstX, float firstY, float firstZ,
                                 float secondX, float secondY, float secondZ,
                                 float thirdX, float thirdY, float thirdZ,
                                 float fourthX, float fourthY, float fourthZ,
                                 float nearV, float farV,
                                 int colorRed, int colorGreen, int colorBlue, int colorAlpha) {
        float spanX = secondX - firstX;
        float spanY = secondY - firstY;
        float spanZ = secondZ - firstZ;
        float sideX = fourthX - firstX;
        float sideY = fourthY - firstY;
        float sideZ = fourthZ - firstZ;
        float normalX = spanY * sideZ - spanZ * sideY;
        float normalY = spanZ * sideX - spanX * sideZ;
        float normalZ = spanX * sideY - spanY * sideX;
        float normalLength = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (normalLength < 1.0E-4F) {
            normalX = 0.0F;
            normalY = 1.0F;
            normalZ = 0.0F;
        } else {
            normalX /= normalLength;
            normalY /= normalLength;
            normalZ /= normalLength;
        }
        beamVertex(consumer, matrix, normalMatrix, packedLight, firstX, firstY, firstZ, 0.0F, nearV, normalX, normalY, normalZ, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, secondX, secondY, secondZ, 0.0F, farV, normalX, normalY, normalZ, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, thirdX, thirdY, thirdZ, 1.0F, farV, normalX, normalY, normalZ, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, fourthX, fourthY, fourthZ, 1.0F, nearV, normalX, normalY, normalZ, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, fourthX, fourthY, fourthZ, 1.0F, nearV, -normalX, -normalY, -normalZ, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, thirdX, thirdY, thirdZ, 1.0F, farV, -normalX, -normalY, -normalZ, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, secondX, secondY, secondZ, 0.0F, farV, -normalX, -normalY, -normalZ, colorRed, colorGreen, colorBlue, colorAlpha);
        beamVertex(consumer, matrix, normalMatrix, packedLight, firstX, firstY, firstZ, 0.0F, nearV, -normalX, -normalY, -normalZ, colorRed, colorGreen, colorBlue, colorAlpha);
    }

    private static void beamVertex(VertexConsumer consumer, Matrix4f matrix, org.joml.Matrix3f normalMatrix, int packedLight,
                                   float pointX, float pointY, float pointZ, float textureU, float textureV,
                                   float normalX, float normalY, float normalZ,
                                   int colorRed, int colorGreen, int colorBlue, int colorAlpha) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(colorRed, colorGreen, colorBlue, colorAlpha).uv(textureU, textureV).uv2(packedLight).normal(normalMatrix, normalX, normalY, normalZ).endVertex();
    }

    private void renderSheet(TechniqueHitEntity entity, PoseStack pose, MultiBufferSource buffer, int packedLight, ResourceLocation texture) {
        float halfWidth = (float) entity.width() * 0.5F;
        float halfHeight = entity.height() <= 0.05F ? (float) entity.length() * 0.5F : (float) entity.height() * 0.5F;
        if (halfHeight < 0.05F) {
            halfHeight = 1.5F;
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        if (Math.abs(entity.getXRot()) > 0.01F || Math.abs(entity.roll()) > 0.01F) {
            pose.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
            pose.mulPose(Axis.ZP.rotationDegrees(entity.roll()));
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        var matrix = pose.last().pose();
        var normal = pose.last().normal();
        consumer.vertex(matrix, -halfWidth, -halfHeight, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, halfWidth, -halfHeight, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, halfWidth, halfHeight, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, -halfWidth, halfHeight, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, -halfWidth, halfHeight, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, halfWidth, halfHeight, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, halfWidth, -halfHeight, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        consumer.vertex(matrix, -halfWidth, -halfHeight, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0.0F, 0.0F, -1.0F).endVertex();
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(TechniqueHitEntity entity) {
        return TEXTURE;
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientRegistration {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ReleaseEntities.TECHNIQUE_HIT.get(), TechniqueHitRenderer::new);
        }
    }
}
