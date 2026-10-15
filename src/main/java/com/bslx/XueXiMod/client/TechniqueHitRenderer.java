package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.ShulkerBulletModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
    private final ShulkerBulletModel<TechniqueHitEntity> model;

    public TechniqueHitRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ShulkerBulletModel<>(context.bakeLayer(ModelLayers.SHULKER_BULLET));
    }

    @Override
    public boolean shouldRender(TechniqueHitEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return entity.distanceToSqr(camX, camY, camZ) <= 96.0 * 96.0
                && super.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public void render(TechniqueHitEntity entity, float entityYaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        String texture = entity.texture();
        if (texture != null && !texture.isEmpty()) {
            ResourceLocation sheet = ResourceLocation.tryParse(texture);
            if (sheet != null) {
                renderSheet(entity, pose, buffer, packedLight, sheet);
                return;
            }
        }
        int visual = entity.getVisual();
        if (visual == 1) {
            float scale = entity.getModelScale();
            float yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
            float xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            float spin = (float) entity.tickCount + partialTicks;
            pose.pushPose();
            pose.scale(scale, scale, scale);
            pose.translate(0.0F, 0.15F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(Mth.sin(spin * 0.1F) * 180.0F));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.cos(spin * 0.1F) * 180.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(spin * 0.15F) * 360.0F));
            pose.scale(-0.5F, -0.5F, 0.5F);
            this.model.setupAnim(entity, 0.0F, 0.0F, 0.0F, yRot, xRot);
            VertexConsumer consumer = buffer.getBuffer(this.model.renderType(TEXTURE));
            this.model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
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
