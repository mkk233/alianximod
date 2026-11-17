package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** 低矮重压的向内条带和短弧。不画封闭盒子。 */
public final class DomainPressureRenderer {
    private static final ResourceLocation BANDS = new ResourceLocation(XueXiMod.MOD_ID, "textures/domain/deep_pressure_bands.png");
    private static final ResourceLocation ARCS = new ResourceLocation(XueXiMod.MOD_ID, "textures/domain/deep_pressure_arcs.png");
    private static final double FULL_REACH = 48.0;
    private static final double FAR_REACH = 64.0;

    private DomainPressureRenderer() {
    }

    public static void draw(DomainSightStore.Sight sight, PoseStack pose, Vec3 camera, Vec3 look,
                            MultiBufferSource.BufferSource buffers, long gameTime) {
        AABB box = sight.bounds();
        if (gameTime > sight.visualEnd()) {
            return;
        }
        Vec3 center = box.getCenter();
        Vec3 toward = center.subtract(camera);
        double reach = toward.length();
        if (reach > FAR_REACH) {
            return;
        }
        if (reach > 12.0 && toward.dot(look) < -0.2) {
            return;
        }
        float spanX = (float) (box.maxX - box.minX);
        float spanY = (float) (box.maxY - box.minY);
        float spanZ = (float) (box.maxZ - box.minZ);
        boolean simple = reach > FULL_REACH;
        int frame = (int) Math.floorMod(gameTime / 4L + sight.animSeed(), 4L);
        float textureV0 = frame * 0.25f;
        float textureV1 = textureV0 + 0.25f;
        float inset = 0.85f + frame * 0.12f;
        int alpha = cameraInside(camera, box) ? 48 : 160;
        pose.pushPose();
        pose.translate(box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer bands = buffers.getBuffer(RenderType.entityTranslucent(BANDS));
        VertexConsumer arcs = buffers.getBuffer(RenderType.entityTranslucent(ARCS));
        drawBands(bands, matrix, spanX, spanY, spanZ, inset, simple, alpha, textureV0, textureV1);
        drawArcs(arcs, matrix, spanX, spanY, spanZ, inset, alpha, textureV0, textureV1);
        if (!simple) {
            drawCorners(bands, matrix, spanX, spanZ, alpha, textureV0, textureV1);
        }
        pose.popPose();
    }

    public static void end(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(RenderType.entityTranslucent(BANDS));
        buffers.endBatch(RenderType.entityTranslucent(ARCS));
    }

    private static boolean cameraInside(Vec3 camera, AABB box) {
        return camera.x > box.minX && camera.x < box.maxX
                && camera.y > box.minY && camera.y < box.maxY
                && camera.z > box.minZ && camera.z < box.maxZ;
    }

    private static void drawBands(VertexConsumer consumer, Matrix4f matrix, float spanX, float spanY, float spanZ,
                                  float inset, boolean simple, int alpha, float textureV0, float textureV1) {
        float gap = 1.15f;
        float midX = spanX * 0.5f;
        float[] heights = simple ? new float[] {spanY * 0.82f} : new float[] {spanY * 0.58f, spanY * 0.74f, spanY * 0.9f};
        for (float height : heights) {
            sheetBoth(consumer, matrix, inset, inset, midX - gap * 0.5f, spanZ - inset, height, alpha, textureV0, textureV1);
            sheetBoth(consumer, matrix, midX + gap * 0.5f, inset, spanX - inset, spanZ - inset, height, alpha, textureV0, textureV1);
        }
    }

    private static void drawArcs(VertexConsumer consumer, Matrix4f matrix, float spanX, float spanY, float spanZ,
                                 float inset, int alpha, float textureV0, float textureV1) {
        float rise = Math.min(1.15f, spanY * 0.55f);
        float base = spanY * 0.22f;
        float wide = Math.min(1.7f, spanX * 0.28f);
        float deep = Math.min(1.7f, spanZ * 0.28f);
        wallXBoth(consumer, matrix, inset + 0.35f, base, spanZ * 0.5f - deep * 0.5f, rise, deep, alpha, textureV0, textureV1);
        wallXBoth(consumer, matrix, spanX - inset - 0.35f, base, spanZ * 0.5f - deep * 0.5f, rise, deep, alpha, textureV0, textureV1);
        wallZBoth(consumer, matrix, spanX * 0.5f - wide * 0.5f, base, inset + 0.35f, wide, rise, alpha, textureV0, textureV1);
        wallZBoth(consumer, matrix, spanX * 0.5f - wide * 0.5f, base, spanZ - inset - 0.35f, wide, rise, alpha, textureV0, textureV1);
    }

    private static void drawCorners(VertexConsumer consumer, Matrix4f matrix, float spanX, float spanZ,
                                    int alpha, float textureV0, float textureV1) {
        float mark = 0.72f;
        float lift = 0.06f;
        sheet(consumer, matrix, 0.35f, 0.35f, 0.35f + mark, 0.35f + mark, lift, alpha, textureV0, textureV1);
        sheet(consumer, matrix, spanX - 0.35f - mark, 0.35f, spanX - 0.35f, 0.35f + mark, lift, alpha, textureV0, textureV1);
        sheet(consumer, matrix, 0.35f, spanZ - 0.35f - mark, 0.35f + mark, spanZ - 0.35f, lift, alpha, textureV0, textureV1);
        sheet(consumer, matrix, spanX - 0.35f - mark, spanZ - 0.35f - mark, spanX - 0.35f, spanZ - 0.35f, lift, alpha, textureV0, textureV1);
    }

    private static void sheetBoth(VertexConsumer consumer, Matrix4f matrix, float lowX, float lowZ, float highX, float highZ,
                                  float height, int alpha, float textureV0, float textureV1) {
        sheet(consumer, matrix, lowX, lowZ, highX, highZ, height, alpha, textureV0, textureV1);
        if (highX - lowX < 0.2f || highZ - lowZ < 0.2f) {
            return;
        }
        vertex(consumer, matrix, lowX, height, lowZ, 0f, textureV0, alpha, 0f, -1f, 0f);
        vertex(consumer, matrix, highX, height, lowZ, 1f, textureV0, alpha, 0f, -1f, 0f);
        vertex(consumer, matrix, highX, height, highZ, 1f, textureV1, alpha, 0f, -1f, 0f);
        vertex(consumer, matrix, lowX, height, highZ, 0f, textureV1, alpha, 0f, -1f, 0f);
    }

    private static void sheet(VertexConsumer consumer, Matrix4f matrix, float lowX, float lowZ, float highX, float highZ,
                              float height, int alpha, float textureV0, float textureV1) {
        if (highX - lowX < 0.2f || highZ - lowZ < 0.2f) {
            return;
        }
        vertex(consumer, matrix, lowX, height, highZ, 0f, textureV1, alpha, 0f, 1f, 0f);
        vertex(consumer, matrix, highX, height, highZ, 1f, textureV1, alpha, 0f, 1f, 0f);
        vertex(consumer, matrix, highX, height, lowZ, 1f, textureV0, alpha, 0f, 1f, 0f);
        vertex(consumer, matrix, lowX, height, lowZ, 0f, textureV0, alpha, 0f, 1f, 0f);
    }

    private static void wallXBoth(VertexConsumer consumer, Matrix4f matrix, float planeX, float lowY, float lowZ,
                                  float rise, float deep, int alpha, float textureV0, float textureV1) {
        wallX(consumer, matrix, planeX, lowY, lowZ, rise, deep, alpha, textureV0, textureV1);
        vertex(consumer, matrix, planeX, lowY + rise, lowZ, 0f, textureV0, alpha, -1f, 0f, 0f);
        vertex(consumer, matrix, planeX, lowY + rise, lowZ + deep, 1f, textureV0, alpha, -1f, 0f, 0f);
        vertex(consumer, matrix, planeX, lowY, lowZ + deep, 1f, textureV1, alpha, -1f, 0f, 0f);
        vertex(consumer, matrix, planeX, lowY, lowZ, 0f, textureV1, alpha, -1f, 0f, 0f);
    }

    private static void wallZBoth(VertexConsumer consumer, Matrix4f matrix, float lowX, float lowY, float planeZ,
                                  float wide, float rise, int alpha, float textureV0, float textureV1) {
        wallZ(consumer, matrix, lowX, lowY, planeZ, wide, rise, alpha, textureV0, textureV1);
        vertex(consumer, matrix, lowX, lowY + rise, planeZ, 0f, textureV0, alpha, 0f, 0f, -1f);
        vertex(consumer, matrix, lowX + wide, lowY + rise, planeZ, 1f, textureV0, alpha, 0f, 0f, -1f);
        vertex(consumer, matrix, lowX + wide, lowY, planeZ, 1f, textureV1, alpha, 0f, 0f, -1f);
        vertex(consumer, matrix, lowX, lowY, planeZ, 0f, textureV1, alpha, 0f, 0f, -1f);
    }

    private static void wallX(VertexConsumer consumer, Matrix4f matrix, float planeX, float lowY, float lowZ,
                              float rise, float deep, int alpha, float textureV0, float textureV1) {
        vertex(consumer, matrix, planeX, lowY, lowZ, 0f, textureV1, alpha, 1f, 0f, 0f);
        vertex(consumer, matrix, planeX, lowY, lowZ + deep, 1f, textureV1, alpha, 1f, 0f, 0f);
        vertex(consumer, matrix, planeX, lowY + rise, lowZ + deep, 1f, textureV0, alpha, 1f, 0f, 0f);
        vertex(consumer, matrix, planeX, lowY + rise, lowZ, 0f, textureV0, alpha, 1f, 0f, 0f);
    }

    private static void wallZ(VertexConsumer consumer, Matrix4f matrix, float lowX, float lowY, float planeZ,
                              float wide, float rise, int alpha, float textureV0, float textureV1) {
        vertex(consumer, matrix, lowX, lowY, planeZ, 0f, textureV1, alpha, 0f, 0f, 1f);
        vertex(consumer, matrix, lowX + wide, lowY, planeZ, 1f, textureV1, alpha, 0f, 0f, 1f);
        vertex(consumer, matrix, lowX + wide, lowY + rise, planeZ, 1f, textureV0, alpha, 0f, 0f, 1f);
        vertex(consumer, matrix, lowX, lowY + rise, planeZ, 0f, textureV0, alpha, 0f, 0f, 1f);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float pointX, float pointY, float pointZ,
                               float textureU, float textureV, int alpha, float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(255, 255, 255, alpha)
                .uv(textureU, textureV).overlayCoords(0).uv2(0x00F000F0)
                .normal(normalX, normalY, normalZ).endVertex();
    }
}
