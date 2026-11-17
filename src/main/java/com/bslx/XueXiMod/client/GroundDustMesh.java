package com.bslx.XueXiMod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 崩地 v2r2 土尘材质。世界光照、最近邻、硬透明，不发光。
 * 只给确认版尘流和粗粒使用。
 */
final class GroundDustMesh extends RenderStateShard {
    private GroundDustMesh() {
        super("xueximod_ground_dust_v2r2", () -> {
        }, () -> {
        });
    }

    static RenderType of(ResourceLocation texture) {
        String meshName = "xueximod_ground_dust_v2r2_" + texture.getNamespace() + "_"
                + texture.getPath().replace('/', '_').replace('.', '_');
        return RenderType.create(
                meshName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
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
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(true));
    }
}
