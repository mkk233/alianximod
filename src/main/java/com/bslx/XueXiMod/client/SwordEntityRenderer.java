package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * FIXED item model tip defaults to +Y. Map +Y to entity forward, then apply yaw/pitch
 * so the tip matches entity facing (hover/fly = look or trajectory; planted = embed dir).
 */
public class SwordEntityRenderer extends EntityRenderer<SwordEntity> {
    private final ItemRenderer itemRenderer;

    public SwordEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public boolean shouldRender(SwordEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return entity.distanceToSqr(camX, camY, camZ) <= 64.0 * 64.0
                && super.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public void render(SwordEntity entity, float entityYaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        float yRot = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        // Vertex order: straighten the diagonal sword sprite onto +Y, cancel FIXED's Y 180, lay that axis along entity forward.
        pose.mulPose(Axis.YP.rotationDegrees(-yRot));
        pose.mulPose(Axis.XP.rotationDegrees(xRot + 90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        this.itemRenderer.renderStatic(entity.swordItem(), ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SwordEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientRegistration {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ReleaseEntities.SWORD.get(), SwordEntityRenderer::new);
        }
    }
}