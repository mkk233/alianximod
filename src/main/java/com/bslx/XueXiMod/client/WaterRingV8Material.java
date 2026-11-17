package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** 三层水环共用这一份无光照透明批次。不写深度，不剔除背面。 */
public final class WaterRingV8Material extends RenderStateShard {
    static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(
            XueXiMod.MOD_ID, "textures/entity/xueximod/water_ring_v8.png");
    private static final RenderType MESH = RenderType.create(
            "xueximod_water_ring_v8",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            1048576,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_TEX_SHADER)
                    .setTextureState(new TextureStateShard(ATLAS, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private WaterRingV8Material() {
        super("xueximod_water_ring_v8_state", () -> {
        }, () -> {
        });
    }

    static RenderType mesh() {
        return MESH;
    }
}
