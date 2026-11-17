package com.bslx.XueXiMod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** 雾雨领域这一帧的雾环和雨格。远的先画，连续同一纹理合并，切换时只结束上一批。 */
final class DomainVolumeBatch {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int CORNER_STRIDE = 5;
    private final List<Quad> quads = new ArrayList<>();

    void addQuad(RenderType mesh,
                 float firstX, float firstY, float firstZ, float firstU, float firstV,
                 float secondX, float secondY, float secondZ, float secondU, float secondV,
                 float thirdX, float thirdY, float thirdZ, float thirdU, float thirdV,
                 float fourthX, float fourthY, float fourthZ, float fourthU, float fourthV,
                 float normalX, float normalY, float normalZ) {
        float[] corners = new float[] {
                firstX, firstY, firstZ, firstU, firstV,
                secondX, secondY, secondZ, secondU, secondV,
                thirdX, thirdY, thirdZ, thirdU, thirdV,
                fourthX, fourthY, fourthZ, fourthU, fourthV
        };
        float centroidX = (firstX + secondX + thirdX + fourthX) * 0.25f;
        float centroidY = (firstY + secondY + thirdY + fourthY) * 0.25f;
        float centroidZ = (firstZ + secondZ + thirdZ + fourthZ) * 0.25f;
        float distanceSquared = centroidX * centroidX + centroidY * centroidY + centroidZ * centroidZ;
        quads.add(new Quad(mesh, distanceSquared, normalX, normalY, normalZ, corners));
    }

    void submit(PoseStack pose, MultiBufferSource.BufferSource buffers) {
        if (quads.isEmpty()) {
            return;
        }
        quads.sort((Quad left, Quad right) -> Float.compare(right.distanceSquared, left.distanceSquared));
        Matrix4f matrix = pose.last().pose();
        RenderType activeMesh = null;
        VertexConsumer consumer = null;
        for (Quad quad : quads) {
            if (quad.mesh != activeMesh) {
                if (activeMesh != null) {
                    buffers.endBatch(activeMesh);
                }
                activeMesh = quad.mesh;
                consumer = buffers.getBuffer(activeMesh);
            }
            quad.write(consumer, matrix);
        }
        if (activeMesh != null) {
            buffers.endBatch(activeMesh);
        }
        quads.clear();
    }

    private static final class Quad {
        private final RenderType mesh;
        private final float distanceSquared;
        private final float normalX;
        private final float normalY;
        private final float normalZ;
        private final float[] corners;

        private Quad(RenderType mesh, float distanceSquared,
                     float normalX, float normalY, float normalZ, float[] corners) {
            this.mesh = mesh;
            this.distanceSquared = distanceSquared;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
            this.corners = corners;
        }

        private void write(VertexConsumer consumer, Matrix4f matrix) {
            for (int corner = 0; corner < 4; corner++) {
                int offset = corner * CORNER_STRIDE;
                consumer.vertex(matrix, corners[offset], corners[offset + 1], corners[offset + 2])
                        .color(255, 255, 255, 255)
                        .uv(corners[offset + 3], corners[offset + 4])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(FULL_BRIGHT)
                        .normal(normalX, normalY, normalZ)
                        .endVertex();
            }
        }
    }
}
