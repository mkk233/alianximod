package com.bslx.XueXiMod.client.furnace;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.NetherBlueFlame;
import com.bslx.XueXiMod.client.furnace.FurnaceBodyVisual.Playback;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 第一人称升温仍走只会在空手和地图触发的 RenderArmEvent。
 * 燃烧改走每次画手都会触发的 RenderHandEvent，不取消事件，避免漏掉持剑、法杖，也不重复画原版手和物品。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class FurnaceArmFlame {
    private FurnaceArmFlame() {
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        Playback playback = FurnaceBodyVisual.playback(player, event.getPartialTick());
        if (!playback.present() || !playback.overdrive()) {
            return;
        }
        float partialTick = event.getPartialTick();
        boolean previousGloom = NetherBlueFlame.open(player.level(),
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()) + player.getBbHeight() * 0.5D,
                Mth.lerp(partialTick, player.zo, player.getZ()));
        try {
            BurnFirstPersonPose.draw(event.getPoseStack(), event.getMultiBufferSource(), player, event.getHand(),
                    event.getItemStack(), partialTick, event.getInterpolatedPitch(),
                    event.getSwingProgress(), event.getEquipProgress(), playback);
        } finally {
            NetherBlueFlame.close(previousGloom);
        }
    }

    @SubscribeEvent
    public static void onArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        if (player == null || player.isInvisible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
        if (!(renderer instanceof PlayerRenderer playerRenderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
        Playback playback = FurnaceBodyVisual.playback(player, minecraft.getFrameTime());
        if (!playback.present() || playback.overdrive()) {
            return;
        }
        float partialTick = minecraft.getFrameTime();
        boolean previousGloom = NetherBlueFlame.open(player.level(),
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()) + player.getBbHeight() * 0.5D,
                Mth.lerp(partialTick, player.zo, player.getZ()));
        boolean characterRight = event.getArm() == HumanoidArm.RIGHT;
        ModelPart arm = characterRight ? model.rightArm : model.leftArm;
        float keptX = arm.xRot;
        float keptY = arm.yRot;
        float keptZ = arm.zRot;
        arm.xRot = 0.0F;
        arm.yRot = 0.0F;
        arm.zRot = 0.0F;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            arm.translateAndRotate(poseStack);
            drawArm(poseStack, event.getMultiBufferSource(), playback, characterRight, FurnaceBodyVisual.narrowArm(player));
        } finally {
            poseStack.popPose();
            arm.xRot = keptX;
            arm.yRot = keptY;
            arm.zRot = keptZ;
            NetherBlueFlame.close(previousGloom);
        }
    }

    private static void drawArm(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback,
                                boolean characterRight, boolean narrowArm) {
        if (characterRight) {
            HeatUpSpatialMount.drawRightHand(poseStack, bufferSource, playback, narrowArm);
        } else {
            HeatUpSpatialMount.drawLeftHand(poseStack, bufferSource, playback, narrowArm);
        }
    }
}
