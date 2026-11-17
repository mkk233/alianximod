package com.bslx.XueXiMod.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** 领域透明网格。细雨和旧水纹盒用原版实体透明 shader；雾环改用只丢弃零 alpha 的专用 shader。 */
final class DomainSheetMesh extends RenderStateShard {
    private static final ShaderStateShard MIST_CURTAIN_SHADER = new ShaderStateShard(DomainMistShaders::curtain);

    private DomainSheetMesh() {
        super("xueximod_domain_sheet", () -> {
        }, () -> {
        });
    }

    static RenderType mistCurtain(ResourceLocation texture) {
        String sheetName = "xueximod_domain_mist_curtain_" + texture.getNamespace() + "_"
                + texture.getPath().replace('/', '_').replace('.', '_');
        return sheet(sheetName, texture, MIST_CURTAIN_SHADER);
    }

    static RenderType of(ResourceLocation texture) {
        String sheetName = "xueximod_domain_sheet_" + texture.getNamespace() + "_"
                + texture.getPath().replace('/', '_').replace('.', '_');
        return sheet(sheetName, texture, RENDERTYPE_ENTITY_TRANSLUCENT_SHADER);
    }

    private static RenderType sheet(String sheetName, ResourceLocation texture, ShaderStateShard shader) {
        return RenderType.create(
                sheetName,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                true,
                RenderType.CompositeState.builder()
                        .setShaderState(shader)
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
