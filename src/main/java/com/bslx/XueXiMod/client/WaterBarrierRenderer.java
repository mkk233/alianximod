package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierBody;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** 固定薄水墙。不使用垂丝的 WaterCurtainRenderer。 */
public class WaterBarrierRenderer extends EntityRenderer<BarrierBody> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/water_still.png");

    public WaterBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BarrierBody wall, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        AABB box = wall.getBoundingBox();
        float lowX = (float) (box.minX - wall.getX());
        float lowY = (float) (box.minY - wall.getY());
        float lowZ = (float) (box.minZ - wall.getZ());
        float highX = (float) (box.maxX - wall.getX());
        float highY = (float) (box.maxY - wall.getY());
        float highZ = (float) (box.maxZ - wall.getZ());
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f matrix = pose.last().pose();
        int light = 0x00F000F0;
        face(consumer, matrix, lowX, lowY, highZ, highX, lowY, highZ, highX, highY, highZ, lowX, highY, highZ, light);
        face(consumer, matrix, highX, lowY, lowZ, lowX, lowY, lowZ, lowX, highY, lowZ, highX, highY, lowZ, light);
        face(consumer, matrix, lowX, lowY, lowZ, lowX, lowY, highZ, lowX, highY, highZ, lowX, highY, lowZ, light);
        face(consumer, matrix, highX, lowY, highZ, highX, lowY, lowZ, highX, highY, lowZ, highX, highY, highZ, light);
        face(consumer, matrix, lowX, highY, highZ, highX, highY, highZ, highX, highY, lowZ, lowX, highY, lowZ, light);
        face(consumer, matrix, lowX, lowY, lowZ, highX, lowY, lowZ, highX, lowY, highZ, lowX, lowY, highZ, light);
        super.render(wall, yaw, partialTick, pose, buffers, packedLight);
    }

    private static void face(VertexConsumer consumer, Matrix4f matrix,
                             float startX, float startY, float startZ,
                             float alongX, float alongY, float alongZ,
                             float farX, float farY, float farZ,
                             float closeX, float closeY, float closeZ, int light) {
        vertex(consumer, matrix, startX, startY, startZ, 0f, 1f, light);
        vertex(consumer, matrix, alongX, alongY, alongZ, 1f, 1f, light);
        vertex(consumer, matrix, farX, farY, farZ, 1f, 0f, light);
        vertex(consumer, matrix, closeX, closeY, closeZ, 0f, 0f, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float pointX, float pointY, float pointZ,
                               float textureU, float textureV, int light) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(80, 160, 255, 140)
                .uv(textureU, textureV).overlayCoords(0).uv2(light).normal(0f, 1f, 0f).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(BarrierBody wall) {
        return TEXTURE;
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientRegistration {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ReleaseEntities.BARRIER.get(), WaterBarrierRenderer::new);
        }
    }
}
