package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.terrain.MeteoriteWreckageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 用陨石方块模型画无伤残骸。六面只走 32×32 的 meteorite_v1，不引用 64×1024 外观条带。
 * 不在这里登记渲染器。
 */
public class MeteoriteWreckageRenderer extends EntityRenderer<MeteoriteWreckageEntity> {
    public static final ResourceLocation BLOCK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "textures/block/meteorite_v1.png");
    private final BlockRenderDispatcher blockRenderer;

    public MeteoriteWreckageRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(MeteoriteWreckageEntity wreckage, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        if (!ModBlocks.METEORITE.isPresent()) {
            return;
        }
        pose.pushPose();
        pose.translate(-0.5D, 0.0D, -0.5D);
        this.blockRenderer.renderSingleBlock(
                ModBlocks.METEORITE.get().defaultBlockState(),
                pose,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(wreckage, entityYaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteoriteWreckageEntity wreckage) {
        return BLOCK_TEXTURE;
    }
}
