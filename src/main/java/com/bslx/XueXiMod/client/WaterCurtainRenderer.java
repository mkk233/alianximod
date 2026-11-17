package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.WaterCurtainLayout.Panel;
import com.bslx.XueXiMod.client.WaterCurtainLayout.Piece;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 垂丝主面。宽度和高只读已经同步的盒子，不再乘聚能，也不画厚度方向的交叉片。
 * 一份面配 NO_CULL，正反都看得见。碰撞厚度仍由水段逻辑维护。
 */
public final class WaterCurtainRenderer extends RenderStateShard {
    private static final ResourceLocation LEFT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/waterfall_runtime_left.png");
    private static final ResourceLocation MIDDLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/waterfall_runtime_middle.png");
    private static final ResourceLocation RIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/waterfall_runtime_right.png");
    private static final ResourceLocation THIN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/waterfall_thin.png");
    private static final RenderType LEFT_MESH = curtainMesh("xueximod_waterfall_runtime_left", LEFT_TEXTURE);
    private static final RenderType MIDDLE_MESH = curtainMesh("xueximod_waterfall_runtime_middle", MIDDLE_TEXTURE);
    private static final RenderType RIGHT_MESH = curtainMesh("xueximod_waterfall_runtime_right", RIGHT_TEXTURE);
    private static final RenderType THIN_MESH = curtainMesh("xueximod_waterfall_thin", THIN_TEXTURE);
    private static final int HEAVY_RED = 210;
    private static final int HEAVY_GREEN = 232;
    private static final int HEAVY_BLUE = 255;

    private WaterCurtainRenderer() {
        super("xueximod_waterfall_curtain", () -> {
        }, () -> {
        });
    }

    public static void render(TechniqueHitEntity entity, boolean heavy, float partialTicks, PoseStack pose, MultiBufferSource buffer) {
        double curtainWidth = entity.width();
        double curtainHeight = entity.height();
        int phaseTicks = Math.floorMod(entity.getId(), WaterCurtainLayout.FRAME_COUNT);
        int frameIndex = WaterCurtainLayout.flowFrame(entity.tickCount, partialTicks, phaseTicks);
        int drainStart = WaterCurtainLayout.drainStart(entity.lifetime());
        List<Panel> panels = WaterCurtainLayout.panels(curtainWidth, curtainHeight, frameIndex, drainStart);
        if (panels.isEmpty()) {
            return;
        }
        float turn = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot()) * ((float) Math.PI / 180.0F);
        float aheadX = -Mth.sin(turn);
        float aheadZ = Mth.cos(turn);
        float rightX = aheadZ;
        float rightZ = -aheadX;
        float normalX = -aheadX;
        float normalZ = -aheadZ;
        int colorRed = heavy ? HEAVY_RED : 255;
        int colorGreen = heavy ? HEAVY_GREEN : 255;
        int colorBlue = heavy ? HEAVY_BLUE : 255;
        Matrix4f matrix = pose.last().pose();
        int packedLight = LightTexture.FULL_BRIGHT;
        for (Panel panel : panels) {
            VertexConsumer consumer = buffer.getBuffer(meshFor(panel.piece));
            submit(consumer, matrix, packedLight, panel, rightX, rightZ, normalX, normalZ,
                    colorRed, colorGreen, colorBlue);
        }
    }

    private static RenderType meshFor(Piece piece) {
        return switch (piece) {
            case LEFT -> LEFT_MESH;
            case MIDDLE -> MIDDLE_MESH;
            case RIGHT -> RIGHT_MESH;
            case THIN -> THIN_MESH;
        };
    }

    private static RenderType curtainMesh(String meshName, ResourceLocation texture) {
        return RenderType.create(
                meshName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                65536,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(true));
    }

    private static void submit(VertexConsumer consumer, Matrix4f matrix, int packedLight, Panel panel,
                               float rightX, float rightZ, float normalX, float normalZ,
                               int colorRed, int colorGreen, int colorBlue) {
        vertex(consumer, matrix, packedLight, panel.alongStart, panel.topOffset, rightX, rightZ,
                panel.textureUStart, panel.textureVStart, normalX, normalZ, colorRed, colorGreen, colorBlue);
        vertex(consumer, matrix, packedLight, panel.alongEnd, panel.topOffset, rightX, rightZ,
                panel.textureUEnd, panel.textureVStart, normalX, normalZ, colorRed, colorGreen, colorBlue);
        vertex(consumer, matrix, packedLight, panel.alongEnd, panel.bottomOffset, rightX, rightZ,
                panel.textureUEnd, panel.textureVEnd, normalX, normalZ, colorRed, colorGreen, colorBlue);
        vertex(consumer, matrix, packedLight, panel.alongStart, panel.bottomOffset, rightX, rightZ,
                panel.textureUStart, panel.textureVEnd, normalX, normalZ, colorRed, colorGreen, colorBlue);
    }

    /**
     * 顶点是实体中心的局部偏移。实体渲染调度器已经用 double 做过实体坐标减相机，
     * 再把这个小差值写入姿态。这里只在相乘之后转 float，不把世界坐标直接转 float。
     */
    private static void vertex(VertexConsumer consumer, Matrix4f matrix, int packedLight,
                               double along, double vertical, float rightX, float rightZ,
                               double textureU, double textureV,
                               float normalX, float normalZ,
                               int colorRed, int colorGreen, int colorBlue) {
        float pointX = (float) (along * rightX);
        float pointY = (float) vertical;
        float pointZ = (float) (along * rightZ);
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(colorRed, colorGreen, colorBlue, 255)
                .uv((float) textureU, (float) textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalX, 0.0F, normalZ)
                .endVertex();
    }
}
