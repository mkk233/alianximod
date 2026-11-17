package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 把确认版水线放进原版 Steve 的渲染变换。
 * 顺序与 LivingEntityRenderer 相同：先绕 Y 转 180 减身体朝向，再 X/Y 取反，再下移 1.501 格。
 * 网格已经是 1/16 格，不再把 32 模型单位硬拉成 1.8 格。
 */
public final class SteveWaterLineVisual extends RenderStateShard {
    static final float RESIDENCE_LOOP_SECONDS = 1.12F;
    static final float SCATTER_START_SECONDS = 1.12F;
    static final float SCATTER_END_SECONDS = 1.96F;
    static final float VANILLA_FOOT_LIFT = 1.501F;
    private static final ResourceLocation SKIN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/steve_water_lines.png");
    private static final ResourceLocation FLAKE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/water_flakes.png");
    private static final RenderType SKIN_TYPE = waterMesh("xueximod_steve_water_lines", SKIN_TEXTURE);
    private static final RenderType FLAKE_TYPE = waterMesh("xueximod_water_flakes", FLAKE_TEXTURE);

    private SteveWaterLineVisual() {
        super("xueximod_steve_water_line", () -> {
        }, () -> {
        });
    }

    public static RenderType skinType() {
        return SKIN_TYPE;
    }

    public static RenderType flakeType() {
        return FLAKE_TYPE;
    }

    public static void alignVanillaSteve(PoseStack pose, float bodyYaw) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -VANILLA_FOOT_LIFT, 0.0F);
    }

    public static float residencePhase(float elapsedSeconds) {
        float loopSeconds = RESIDENCE_LOOP_SECONDS;
        float wrapped = elapsedSeconds - loopSeconds * (float) Math.floor(elapsedSeconds / loopSeconds);
        if (wrapped < 0.0F) {
            wrapped += loopSeconds;
        }
        return wrapped;
    }

    public static float scatterPhase(float elapsedSeconds) {
        float phase = SCATTER_START_SECONDS + (elapsedSeconds < 0.0F ? 0.0F : elapsedSeconds);
        if (phase > SCATTER_END_SECONDS) {
            return SCATTER_END_SECONDS;
        }
        return phase;
    }

    public static boolean drawPhase(PoseStack pose, MultiBufferSource buffers, int packedLight, float timeSeconds) {
        SteveWaterLineMesh mesh = SteveWaterLineMesh.current();
        if (pose == null || buffers == null || mesh.isEmpty()) {
            return false;
        }
        // 两种 RenderType 共用 BufferSource 的同一个 BufferBuilder。
        // 必须先写完皮肤再取水花，否则 getBuffer 会先提交空的皮肤批次，皮肤顶点随后画进水花贴图。
        mesh.drawSkin(pose, buffers.getBuffer(SKIN_TYPE), timeSeconds);
        mesh.drawFlakes(pose, buffers.getBuffer(FLAKE_TYPE), timeSeconds);
        return true;
    }

    private static RenderType waterMesh(String meshName, ResourceLocation texture) {
        return RenderType.create(
                meshName,
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.QUADS,
                65536,
                false,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_TEX_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }
}
