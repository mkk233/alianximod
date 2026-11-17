package com.bslx.XueXiMod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 引爆 v2 单团材质。全亮、最近邻、硬透明，测试深度但不写深度。
 * 只给本术式的云团使用，其他载体视觉仍走各自材质。
 */
final class DetonateCloudMesh extends RenderStateShard {
    private DetonateCloudMesh() {
        super("xueximod_detonate_cloud", () -> {
        }, () -> {
        });
    }

    static RenderType of(ResourceLocation texture) {
        String meshName = "xueximod_detonate_cloud_" + texture.getNamespace() + "_"
                + texture.getPath().replace('/', '_').replace('.', '_');
        return RenderType.create(
                meshName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setOverlayState(OVERLAY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(true));
    }
}
