package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.npc.daozhang.DaozhangEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 原版人形和流浪者贴图，只作功能占位。 */
public class DaozhangRenderer extends HumanoidMobRenderer<DaozhangEntity, HumanoidModel<DaozhangEntity>> {
    private static final ResourceLocation PLACEHOLDER =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/wandering_trader.png");

    public DaozhangRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(DaozhangEntity entity) {
        return PLACEHOLDER;
    }
}
