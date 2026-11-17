package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainVisual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 水雾领域内部的竖直细雨。只画 VOLUME，与雾盒共用只写颜色的透明网格，重压不在这里处理。
 * 雨幕沿世界 X-Y、Z-Y 交叉，每格保持正方形，32 帧各自映射，不随相机翻倒。
 */
public final class DomainMistRainRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(XueXiMod.MOD_ID, "textures/domain/mist_rain_80.png");
    private static final RenderType MESH = DomainSheetMesh.of(TEXTURE);
    private static final float CELL_SIZE = 2.0f;
    private static final float EDGE_INSET = 0.08f;
    private static final float LEAST_CELL_SPAN = 0.02f;
    private static final float FRAME_TEXEL_PAD = 0.5f / 64.0f;
    private static final double FULL_REACH = 48.0;
    private static final double FAR_REACH = 64.0;
    private static final float[] NEAR_FRACTIONS = {0.28f, 0.50f, 0.72f};
    private static final int[] NEAR_SLOTS = {0, 1, 2};
    private static final float[] FAR_FRACTIONS = {0.50f};
    private static final int[] FAR_SLOTS = {1};
    private static final int GROUP_ACROSS = 0;
    private static final int GROUP_ALONG = 16;
    private static final int PLANE_ACROSS = 1;
    private static final int PLANE_ALONG = 2;

    private DomainMistRainRenderer() {
    }

    public static boolean draw(DomainSightStore.Sight sight, PoseStack pose, Vec3 camera,
                               MultiBufferSource.BufferSource buffers, long gameTime) {
        if (sight.visual() != DomainVisual.VOLUME) {
            return false;
        }
        if (gameTime < sight.visualOrigin() || gameTime >= sight.visualEnd()) {
            return false;
        }
        AABB box = sight.bounds();
        Vec3 toward = box.getCenter().subtract(camera);
        double reach = toward.length();
        if (reach > FAR_REACH) {
            return false;
        }
        float spanX = (float) (box.maxX - box.minX);
        float spanY = (float) (box.maxY - box.minY);
        float spanZ = (float) (box.maxZ - box.minZ);
        float neededSpan = EDGE_INSET * 2.0f + LEAST_CELL_SPAN;
        if (spanX < neededSpan || spanY < neededSpan || spanZ < neededSpan) {
            return false;
        }
        boolean fewerSheets = reach > FULL_REACH;
        pose.pushPose();
        pose.translate(box.minX - camera.x, box.minY - camera.y, box.minZ - camera.z);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffers.getBuffer(MESH);
        drawGroup(consumer, matrix, PLANE_ACROSS, GROUP_ACROSS, sight.animSeed(),
                spanX, spanY, spanZ, gameTime, sight.visualOrigin(), fewerSheets);
        drawGroup(consumer, matrix, PLANE_ALONG, GROUP_ALONG, sight.animSeed(),
                spanZ, spanY, spanX, gameTime, sight.visualOrigin(), fewerSheets);
        pose.popPose();
        return true;
    }

    public static void end(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(MESH);
    }

    private static void drawGroup(VertexConsumer consumer, Matrix4f matrix, int planeKind, int group, int animSeed,
                                  float spanAcross, float spanUp, float spanFixed,
                                  long gameTime, long visualOrigin, boolean fewerSheets) {
        float[] fractions = fewerSheets ? FAR_FRACTIONS : NEAR_FRACTIONS;
        int[] slots = fewerSheets ? FAR_SLOTS : NEAR_SLOTS;
        for (int slice = 0; slice < fractions.length; slice++) {
            float planeFixed = spanFixed * fractions[slice];
            int frame = frameOf(gameTime, visualOrigin, phaseOf(animSeed, group, slots[slice]));
            tilePlane(consumer, matrix, planeKind, planeFixed, spanAcross, spanUp, frame);
        }
    }

    private static void tilePlane(VertexConsumer consumer, Matrix4f matrix, int planeKind, float planeFixed,
                                  float spanAcross, float spanUp, int frame) {
        float acrossLimit = spanAcross - EDGE_INSET;
        float upLimit = spanUp - EDGE_INSET;
        float acrossCursor = EDGE_INSET;
        while (acrossCursor < acrossLimit - 1.0E-4f) {
            float acrossNext = Math.min(acrossLimit, acrossCursor + CELL_SIZE);
            if (acrossNext <= acrossCursor) {
                break;
            }
            float upCursor = EDGE_INSET;
            while (upCursor < upLimit - 1.0E-4f) {
                float upNext = Math.min(upLimit, upCursor + CELL_SIZE);
                if (upNext <= upCursor) {
                    break;
                }
                float coveredAcross = (acrossNext - acrossCursor) / CELL_SIZE;
                float coveredUp = (upNext - upCursor) / CELL_SIZE;
                float leftU = insetEdge(0.0f);
                float rightU = insetEdge(coveredAcross);
                float topV = insetEdge(1.0f - coveredUp);
                float bottomV = insetEdge(1.0f);
                if (acrossNext - acrossCursor >= LEAST_CELL_SPAN
                        && upNext - upCursor >= LEAST_CELL_SPAN
                        && rightU > leftU
                        && bottomV > topV) {
                    emitCell(consumer, matrix, planeKind, planeFixed,
                            acrossCursor, acrossNext, upCursor, upNext,
                            leftU, rightU,
                            frameV(frame, topV), frameV(frame, bottomV));
                }
                upCursor = upNext;
            }
            acrossCursor = acrossNext;
        }
    }

    private static void emitCell(VertexConsumer consumer, Matrix4f matrix, int planeKind, float planeFixed,
                                 float lowAcross, float highAcross, float lowUp, float highUp,
                                 float leftU, float rightU, float topV, float bottomV) {
        if (planeKind == PLANE_ACROSS) {
            vertex(consumer, matrix, lowAcross, highUp, planeFixed, leftU, topV, 0f, 0f, 1f);
            vertex(consumer, matrix, highAcross, highUp, planeFixed, rightU, topV, 0f, 0f, 1f);
            vertex(consumer, matrix, highAcross, lowUp, planeFixed, rightU, bottomV, 0f, 0f, 1f);
            vertex(consumer, matrix, lowAcross, lowUp, planeFixed, leftU, bottomV, 0f, 0f, 1f);
            return;
        }
        vertex(consumer, matrix, planeFixed, highUp, lowAcross, leftU, topV, 1f, 0f, 0f);
        vertex(consumer, matrix, planeFixed, highUp, highAcross, rightU, topV, 1f, 0f, 0f);
        vertex(consumer, matrix, planeFixed, lowUp, highAcross, rightU, bottomV, 1f, 0f, 0f);
        vertex(consumer, matrix, planeFixed, lowUp, lowAcross, leftU, bottomV, 1f, 0f, 0f);
    }

    private static int phaseOf(int animSeed, int group, int slot) {
        return Math.floorMod(animSeed + (group + slot) * 5, 32);
    }

    private static int frameOf(long gameTime, long visualOrigin, int phase) {
        return (int) Math.floorMod(gameTime - visualOrigin + phase, 32L);
    }

    private static float frameV(int frame, float localSpan) {
        return (frame + localSpan) / 32.0f;
    }

    /** 最近邻在 0 和 1 上会采到相邻帧或对侧，完整边收半个纹素。裁切中的局部坐标保持原值。 */
    private static float insetEdge(float unitSpan) {
        if (unitSpan <= 0.0f) {
            return FRAME_TEXEL_PAD;
        }
        if (unitSpan >= 1.0f) {
            return 1.0f - FRAME_TEXEL_PAD;
        }
        return unitSpan;
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float pointX, float pointY, float pointZ,
                               float textureU, float textureV, float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(255, 255, 255, 255)
                .uv(textureU, textureV).overlayCoords(0).uv2(0x00F000F0)
                .normal(normalX, normalY, normalZ).endVertex();
    }
}
