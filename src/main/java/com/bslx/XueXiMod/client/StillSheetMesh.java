package com.bslx.XueXiMod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** 静止斩面的透明网格。最近邻、无 mipmap，深度测试保留，只写颜色，双面但不画第二张反向面。 */
final class StillSheetMesh extends RenderStateShard {
    private StillSheetMesh() {
        super("xueximod_still_sheet", () -> {
        }, () -> {
        });
    }

    static RenderType of(ResourceLocation texture) {
        String sheetName = "xueximod_still_sheet_" + texture.getNamespace() + "_"
                + texture.getPath().replace('/', '_').replace('.', '_');
        return RenderType.create(
                sheetName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
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
}
