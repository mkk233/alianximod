package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** 垂丝水帘。尺寸来自已经同步的盒子，不再把弹头轨迹当成碰撞。 */
public final class WaterCurtainRenderer {
    private static final ResourceLocation SPARK = new ResourceLocation("textures/entity/shulker/spark.png");

    private WaterCurtainRenderer() {
    }

    public static void render(TechniqueHitEntity entity, boolean heavy, float partialTicks, PoseStack pose, MultiBufferSource buffer) {
        float turn = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) * ((float) Math.PI / 180.0F);
        float aheadX = -Mth.sin(turn);
        float aheadZ = Mth.cos(turn);
        float rightX = aheadZ;
        float rightZ = -aheadX;
        float halfAcross = (float) entity.width() * 0.5F;
        float halfSpan = (float) entity.height() * 0.5F;
        float halfThick = (float) entity.length() * 0.5F;
        float flow = -((entity.tickCount + partialTicks) * 0.12F);
        int red = heavy ? 18 : 64;
        int green = heavy ? 78 : 156;
        int blue = heavy ? 138 : 214;
        int alpha = heavy ? 190 : 150;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(SPARK));
        Matrix4f matrix = pose.last().pose();
        int packedBright = LightTexture.FULL_BRIGHT;
        sheet(consumer, matrix, packedBright, rightX, rightZ, halfAcross, halfSpan, 0.0F, red, green, blue, alpha, flow);
        sheet(consumer, matrix, packedBright, aheadX, aheadZ, halfThick, halfSpan, halfAcross * 0.15F, red, green, blue, alpha / 2, flow);
    }

    private static void sheet(VertexConsumer consumer, Matrix4f matrix, int packedLight,
                              float axisX, float axisZ, float halfWide, float halfTall, float push,
                              int red, int green, int blue, int alpha, float flow) {
        float leftX = -axisX * halfWide;
        float leftZ = -axisZ * halfWide;
        float rightX = axisX * halfWide;
        float rightZ = axisZ * halfWide;
        float faceX = axisZ;
        float faceZ = -axisX;
        quad(consumer, matrix, packedLight,
                leftX + faceX * push, halfTall, leftZ + faceZ * push,
                rightX + faceX * push, halfTall, rightZ + faceZ * push,
                rightX + faceX * push, -halfTall, rightZ + faceZ * push,
                leftX + faceX * push, -halfTall, leftZ + faceZ * push,
                faceX, faceZ, red, green, blue, alpha, flow);
        quad(consumer, matrix, packedLight,
                rightX - faceX * push, halfTall, rightZ - faceZ * push,
                leftX - faceX * push, halfTall, leftZ - faceZ * push,
                leftX - faceX * push, -halfTall, leftZ - faceZ * push,
                rightX - faceX * push, -halfTall, rightZ - faceZ * push,
                -faceX, -faceZ, red, green, blue, alpha, flow);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, int packedLight,
                             float firstX, float firstY, float firstZ, float secondX, float secondY, float secondZ,
                             float thirdX, float thirdY, float thirdZ, float fourthX, float fourthY, float fourthZ,
                             float normalX, float normalZ, int red, int green, int blue, int alpha, float flow) {
        vertex(consumer, matrix, packedLight, firstX, firstY, firstZ, 0.0F, flow, normalX, normalZ, red, green, blue, alpha);
        vertex(consumer, matrix, packedLight, secondX, secondY, secondZ, 1.0F, flow, normalX, normalZ, red, green, blue, alpha);
        vertex(consumer, matrix, packedLight, thirdX, thirdY, thirdZ, 1.0F, flow + 1.0F, normalX, normalZ, red, green, blue, alpha);
        vertex(consumer, matrix, packedLight, fourthX, fourthY, fourthZ, 0.0F, flow + 1.0F, normalX, normalZ, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, int packedLight,
                               float pointX, float pointY, float pointZ, float textureU, float textureV,
                               float normalX, float normalZ, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(red, green, blue, alpha)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalX, 0.0F, normalZ)
                .endVertex();
    }
}
