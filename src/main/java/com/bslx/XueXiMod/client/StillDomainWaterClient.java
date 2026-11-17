package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.block.StillDomainFluids;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.function.Consumer;

/** 临时水沿用原版水的静止、流动和水面贴图，不接入未确认的旋流带。 */
public final class StillDomainWaterClient {
    private static final ResourceLocation STILL = new ResourceLocation("minecraft", "block/water_still");
    private static final ResourceLocation FLOWING = new ResourceLocation("minecraft", "block/water_flow");
    private static final ResourceLocation OVERLAY = new ResourceLocation("minecraft", "block/water_overlay");

    private StillDomainWaterClient() {
    }

    public static void attach(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY;
            }

            @Override
            public int getTintColor(net.minecraft.world.level.material.FluidState state,
                                    net.minecraft.world.level.BlockAndTintGetter level,
                                    net.minecraft.core.BlockPos cell) {
                return BiomeColors.getAverageWaterColor(level, cell) | 0xFF000000;
            }
        });
    }

    public static void renderLayers() {
        ItemBlockRenderTypes.setRenderLayer(StillDomainFluids.SOURCE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(StillDomainFluids.FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(StillDomainFluids.BLOCK.get(), RenderType.translucent());
    }
}
