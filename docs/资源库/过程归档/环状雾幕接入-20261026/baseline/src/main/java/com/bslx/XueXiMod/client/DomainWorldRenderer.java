package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** 非重压仍画六面水纹盒子。雾盒与细雨只写颜色；VOLUME 在雾批次结束后叠加细雨，重压保持原分支。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class DomainWorldRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/water_still.png");
    private static final RenderType FOG_MESH = DomainSheetMesh.of(TEXTURE);

    private DomainWorldRenderer() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        org.joml.Vector3f forward = event.getCamera().getLookVector();
        Vec3 look = new Vec3(forward.x, forward.y, forward.z);
        long gameTime = minecraft.level.getGameTime();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer fogConsumer = null;
        boolean drewPressure = false;
        for (DomainSightStore.Sight sight : DomainSightStore.all()) {
            if (sight.visual() == DomainVisual.PRESSURE) {
                drewPressure = true;
                continue;
            }
            if (fogConsumer == null) {
                fogConsumer = buffers.getBuffer(FOG_MESH);
            }
            AABB box = sight.bounds();
            pose.pushPose();
            pose.translate(box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z);
            Matrix4f matrix = pose.last().pose();
            float spanX = (float) (box.maxX - box.minX);
            float spanY = (float) (box.maxY - box.minY);
            float spanZ = (float) (box.maxZ - box.minZ);
            face(fogConsumer, matrix, 0f, 0f, spanZ, spanX, 0f, spanZ, spanX, spanY, spanZ, 0f, spanY, spanZ);
            face(fogConsumer, matrix, spanX, 0f, 0f, 0f, 0f, 0f, 0f, spanY, 0f, spanX, spanY, 0f);
            face(fogConsumer, matrix, 0f, 0f, 0f, 0f, 0f, spanZ, 0f, spanY, spanZ, 0f, spanY, 0f);
            face(fogConsumer, matrix, spanX, 0f, spanZ, spanX, 0f, 0f, spanX, spanY, 0f, spanX, spanY, spanZ);
            face(fogConsumer, matrix, 0f, spanY, spanZ, spanX, spanY, spanZ, spanX, spanY, 0f, 0f, spanY, 0f);
            face(fogConsumer, matrix, 0f, 0f, 0f, spanX, 0f, 0f, spanX, 0f, spanZ, 0f, 0f, spanZ);
            pose.popPose();
        }
        if (fogConsumer != null) {
            buffers.endBatch(FOG_MESH);
        }
        boolean drewRain = false;
        for (DomainSightStore.Sight sight : DomainSightStore.all()) {
            if (sight.visual() != DomainVisual.VOLUME) {
                continue;
            }
            if (DomainMistRainRenderer.draw(sight, pose, camera, buffers, gameTime)) {
                drewRain = true;
            }
        }
        if (drewRain) {
            DomainMistRainRenderer.end(buffers);
        }
        if (drewPressure) {
            for (DomainSightStore.Sight sight : DomainSightStore.all()) {
                if (sight.visual() != DomainVisual.PRESSURE) {
                    continue;
                }
                DomainPressureRenderer.draw(sight, pose, camera, look, buffers, gameTime);
            }
            DomainPressureRenderer.end(buffers);
        }
    }

    private static void face(VertexConsumer consumer, Matrix4f matrix,
                             float startX, float startY, float startZ,
                             float alongX, float alongY, float alongZ,
                             float farX, float farY, float farZ,
                             float closeX, float closeY, float closeZ) {
        vertex(consumer, matrix, startX, startY, startZ, 0f, 1f);
        vertex(consumer, matrix, alongX, alongY, alongZ, 1f, 1f);
        vertex(consumer, matrix, farX, farY, farZ, 1f, 0f);
        vertex(consumer, matrix, closeX, closeY, closeZ, 0f, 0f);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float pointX, float pointY, float pointZ,
                               float textureU, float textureV) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(90, 170, 255, 70)
                .uv(textureU, textureV).overlayCoords(0).uv2(0x00F000F0).normal(0f, 1f, 0f).endVertex();
    }
}
