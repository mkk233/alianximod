package com.bslx.XueXiMod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** 雾盒和细雨的实体透明网格。只写颜色，深度测试保持 LEQUAL，不改全局深度遮罩。 */
final class DomainSheetMesh extends RenderStateShard {
    private DomainSheetMesh() {
        super("xueximod_domain_sheet", () -> {
        }, () -> {
        });
    }

    static RenderType of(ResourceLocation texture) {
        String sheetName = "xueximod_domain_sheet_" + texture.getNamespace() + "_"
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
