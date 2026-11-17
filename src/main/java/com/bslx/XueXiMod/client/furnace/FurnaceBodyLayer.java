package com.bslx.XueXiMod.client.furnace;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.NetherBlueFlame;
import com.bslx.XueXiMod.client.furnace.FurnaceBodyVisual.Playback;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把升温、燃烧贴在玩家骨骼上。燃烧用已确认的空间网格，升温用 v3.1 卡片。
 */
public final class FurnaceBodyLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<
        AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public FurnaceBodyLayer(PlayerRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (FurnaceBodyVisual.hidesBody(player)) {
            return;
        }
        Playback playback = FurnaceBodyVisual.playback(player, partialTick);
        if (!playback.present()) {
            return;
        }
        boolean previousGloom = NetherBlueFlame.open(player.level(),
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()) + player.getBbHeight() * 0.5D,
                Mth.lerp(partialTick, player.zo, player.getZ()));
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        boolean narrowArm = FurnaceBodyVisual.narrowArm(player);
        try {
            if (playback.overdrive()) {
                drawBurn(poseStack, bufferSource, model, playback);
            } else {
                drawHeat(poseStack, bufferSource, model, playback, narrowArm);
            }
        } finally {
            NetherBlueFlame.close(previousGloom);
        }
    }

    private static void drawBurn(PoseStack poseStack, MultiBufferSource bufferSource, PlayerModel<?> model,
                                 Playback playback) {
        onPart(poseStack, model.body, () -> BurnSpatialMount.draw(poseStack, bufferSource, playback, "body"));
        onPart(poseStack, model.rightLeg, () -> BurnSpatialMount.draw(poseStack, bufferSource, playback, "right_leg"));
        onPart(poseStack, model.leftLeg, () -> BurnSpatialMount.draw(poseStack, bufferSource, playback, "left_leg"));
        onPart(poseStack, model.rightArm, () -> BurnSpatialMount.draw(poseStack, bufferSource, playback, "right_arm"));
        onPart(poseStack, model.leftArm, () -> BurnSpatialMount.draw(poseStack, bufferSource, playback, "left_arm"));
    }

    private static void drawHeat(PoseStack poseStack, MultiBufferSource bufferSource, PlayerModel<?> model,
                                 Playback playback, boolean narrowArm) {
        onPart(poseStack, model.body, () -> {
            HeatUpSpatialMount.drawBody(poseStack, bufferSource, playback);
        });
        onPart(poseStack, model.rightArm, () -> HeatUpSpatialMount.drawRightArm(
                poseStack, bufferSource, playback, narrowArm));
        onPart(poseStack, model.leftArm, () -> HeatUpSpatialMount.drawLeftArm(
                poseStack, bufferSource, playback, narrowArm));
        onPart(poseStack, model.rightLeg, () -> HeatUpSpatialMount.drawRightLeg(poseStack, bufferSource, playback));
        onPart(poseStack, model.leftLeg, () -> HeatUpSpatialMount.drawLeftLeg(poseStack, bufferSource, playback));
    }

    private static void onPart(PoseStack poseStack, ModelPart part, Runnable drawer) {
        poseStack.pushPose();
        part.translateAndRotate(poseStack);
        try {
            drawer.run();
        } finally {
            poseStack.popPose();
        }
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (String skinName : event.getSkins()) {
                Object skinRenderer = event.getSkin(skinName);
                if (skinRenderer instanceof PlayerRenderer playerRenderer) {
                    playerRenderer.addLayer(new FurnaceBodyLayer(playerRenderer));
                }
            }
        }
    }
}
