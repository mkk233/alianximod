package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.effects.WaterYieldingEffect;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortVisual;
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

/** 让锋的水面：先铺平，再从中间裂开，两侧抬成浪。斩击只画在缝中间。 */
public final class WaterPartingRenderer {
    private static final ResourceLocation SPARK = new ResourceLocation("textures/entity/shulker/spark.png");
    private static final int PART_TICKS = 14;

    private WaterPartingRenderer() {}

    public static void render(TechniqueHitEntity entity, float partialTicks, PoseStack pose, MultiBufferSource buffer) {
        float scale = entity.getModelScale();
        if (!Float.isFinite(scale) || scale <= 0.001F) {
            scale = 1.0F;
        }
        float lived = entity.tickCount + partialTicks;
        float reveal = Mth.clamp(lived / 8.0F, 0.0F, 1.0F);
        float part = Mth.clamp((lived - WaterYieldingEffect.SURFACE_TICKS) / PART_TICKS, 0.0F, 1.0F);
        float eased = part * part * (3.0F - 2.0F * part);
        float turn = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) * ((float) Math.PI / 180.0F);
        float aheadX = -Mth.sin(turn);
        float aheadZ = Mth.cos(turn);
        float rightX = aheadZ;
        float rightZ = -aheadX;
        float halfReach = (float) entity.length() * 0.5F;
        float sheet = 4.0F * scale;
        float gap = eased * 2.6F * scale;
        float drift = eased * 0.9F * scale;
        float lift = eased * 1.35F * scale;
        float surface = -1.0F * scale;
        int bodyAlpha = (int) (175.0F * reveal);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(SPARK));
        Matrix4f matrix = pose.last().pose();
        int packedBright = LightTexture.FULL_BRIGHT;
        waterFace(consumer, matrix, packedBright, -(gap + drift), -(gap + drift + sheet),
                -halfReach, halfReach, surface, surface + lift, rightX, rightZ, aheadX, aheadZ,
                36, 118, 176, 186, 228, 246, bodyAlpha);
        waterFace(consumer, matrix, packedBright, gap + drift, gap + drift + sheet,
                -halfReach, halfReach, surface, surface + lift, rightX, rightZ, aheadX, aheadZ,
                36, 118, 176, 186, 228, 246, bodyAlpha);
        float cut = Mth.clamp((lived - WaterYieldingEffect.SURFACE_TICKS) / 6.0F, 0.0F, 1.0F);
        if (cut <= 0.001F) {
            return;
        }
        float seam = 0.18F * scale;
        float seamY = surface + 0.08F;
        int seamAlpha = (int) (210.0F * cut);
        waterFace(consumer, matrix, packedBright, -seam, seam, -halfReach, halfReach, seamY, seamY,
                rightX, rightZ, aheadX, aheadZ, 220, 246, 255, 220, 246, 255, seamAlpha);
        float halfHeight = (float) entity.height() * 0.5F;
        float flashAlong = Mth.lerp(cut, -halfReach, halfReach);
        int slashAlpha = (int) (235.0F * (1.0F - part * 0.35F));
        int glowAlpha = (int) (90.0F * (1.0F - part * 0.45F));
        CohortVisual visual = entity.cohortVisual();
        float glowHalf = 0.85F * scale;
        float coreHalf = 0.22F * scale;
        if (!visual.forDrawing().laneSplit()) {
            slashFace(consumer, matrix, packedBright, flashAlong, -glowHalf, glowHalf, -halfHeight, halfHeight,
                    rightX, rightZ, aheadX, aheadZ, 150, 214, 255, glowAlpha);
            slashFace(consumer, matrix, packedBright, flashAlong, -coreHalf, coreHalf, -halfHeight, halfHeight,
                    rightX, rightZ, aheadX, aheadZ, 240, 252, 255, slashAlpha);
            return;
        }
        CohortLaneLayout.SlashInterval glow = CohortLaneLayout.partingSlash(
                entity.width(), entity.height(), glowHalf, halfHeight, visual, false);
        CohortLaneLayout.SlashInterval core = CohortLaneLayout.partingSlash(
                entity.width(), entity.height(), coreHalf, halfHeight, visual, true);
        slashFace(consumer, matrix, packedBright, flashAlong, glow.sideLow(), glow.sideHigh(), glow.riseLow(), glow.riseHigh(),
                rightX, rightZ, aheadX, aheadZ, 150, 214, 255, glowAlpha);
        slashFace(consumer, matrix, packedBright, flashAlong, core.sideLow(), core.sideHigh(), core.riseLow(), core.riseHigh(),
                rightX, rightZ, aheadX, aheadZ, 240, 252, 255, slashAlpha);
    }

    private static void waterFace(VertexConsumer consumer, Matrix4f matrix, int packedLight,
                                  float inner, float outer, float near, float far,
                                  float innerY, float outerY,
                                  float rightX, float rightZ, float aheadX, float aheadZ,
                                  int innerRed, int innerGreen, int innerBlue,
                                  int outerRed, int outerGreen, int outerBlue, int alpha) {
        emit(consumer, matrix, packedLight, corner(inner, near, innerY, rightX, rightZ, aheadX, aheadZ),
                innerRed, innerGreen, innerBlue, alpha, 0.0F, 0.0F);
        emit(consumer, matrix, packedLight, corner(inner, far, innerY, rightX, rightZ, aheadX, aheadZ),
                innerRed, innerGreen, innerBlue, alpha, 0.0F, 1.0F);
        emit(consumer, matrix, packedLight, corner(outer, far, outerY, rightX, rightZ, aheadX, aheadZ),
                outerRed, outerGreen, outerBlue, alpha, 1.0F, 1.0F);
        emit(consumer, matrix, packedLight, corner(outer, near, outerY, rightX, rightZ, aheadX, aheadZ),
                outerRed, outerGreen, outerBlue, alpha, 1.0F, 0.0F);
        emit(consumer, matrix, packedLight, corner(outer, near, outerY, rightX, rightZ, aheadX, aheadZ),
                outerRed, outerGreen, outerBlue, alpha, 1.0F, 0.0F);
        emit(consumer, matrix, packedLight, corner(outer, far, outerY, rightX, rightZ, aheadX, aheadZ),
                outerRed, outerGreen, outerBlue, alpha, 1.0F, 1.0F);
        emit(consumer, matrix, packedLight, corner(inner, far, innerY, rightX, rightZ, aheadX, aheadZ),
                innerRed, innerGreen, innerBlue, alpha, 0.0F, 1.0F);
        emit(consumer, matrix, packedLight, corner(inner, near, innerY, rightX, rightZ, aheadX, aheadZ),
                innerRed, innerGreen, innerBlue, alpha, 0.0F, 0.0F);
    }

    private static void slashFace(VertexConsumer consumer, Matrix4f matrix, int packedLight,
                                  float along, float sideLow, float sideHigh, float low, float high,
                                  float rightX, float rightZ, float aheadX, float aheadZ,
                                  int red, int green, int blue, int alpha) {
        emit(consumer, matrix, packedLight, corner(sideLow, along, low, rightX, rightZ, aheadX, aheadZ), red, green, blue, alpha, 0.0F, 1.0F);
        emit(consumer, matrix, packedLight, corner(sideHigh, along, low, rightX, rightZ, aheadX, aheadZ), red, green, blue, alpha, 1.0F, 1.0F);
        emit(consumer, matrix, packedLight, corner(sideHigh, along, high, rightX, rightZ, aheadX, aheadZ), red, green, blue, alpha, 1.0F, 0.0F);
        emit(consumer, matrix, packedLight, corner(sideLow, along, high, rightX, rightZ, aheadX, aheadZ), red, green, blue, alpha, 0.0F, 0.0F);
        emit(consumer, matrix, packedLight, corner(sideLow, along, high, rightX, rightZ, aheadX, aheadZ), red, green, blue, alpha, 0.0F, 0.0F);
        emit(consumer, matrix, packedLight, corner(sideHigh, along, high, rightX, rightZ, aheadX, aheadZ), red, green, blue, alpha, 1.0F, 0.0F);
        emit(consumer, matrix, packedLight, corner(sideHigh, along, low, rightX, rightZ, aheadX, aheadZ), red, green, blue, alpha, 1.0F, 1.0F);
        emit(consumer, matrix, packedLight, corner(sideLow, along, low, rightX, rightZ, aheadX, aheadZ), red, green, blue, alpha, 0.0F, 1.0F);
    }

    private static float[] corner(float side, float along, float height,
                                  float rightX, float rightZ, float aheadX, float aheadZ) {
        return new float[]{
                rightX * side + aheadX * along,
                height,
                rightZ * side + aheadZ * along
        };
    }

    private static void emit(VertexConsumer consumer, Matrix4f matrix, int packedLight, float[] point,
                             int red, int green, int blue, int alpha, float textureU, float textureV) {
        consumer.vertex(matrix, point[0], point[1], point[2])
                .color(red, green, blue, alpha)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
