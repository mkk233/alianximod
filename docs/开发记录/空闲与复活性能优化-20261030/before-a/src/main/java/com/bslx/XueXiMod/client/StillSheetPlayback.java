package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout;
import com.bslx.XueXiMod.shu.technique.release.entity.StillSheetCache;
import com.bslx.XueXiMod.shu.technique.release.entity.StillSheetClock;
import com.bslx.XueXiMod.shu.technique.release.entity.StillSheetLayout;
import com.bslx.XueXiMod.shu.technique.release.entity.StillSheetSnapshot;
import com.bslx.XueXiMod.shu.technique.release.entity.StillSheetVisual;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 静止斩面从正式激活画到出生后整段帧数。命中实体更早移除时，这份快照继续播完。
 * 捕获看客户端已加载实体，不看视锥。同一 UUID 只收一次。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class StillSheetPlayback {
    private static final double VIEW_REACH = 96.0D;
    private static final StillSheetCache RETAINED = new StillSheetCache();
    private static ClientLevel boundLevel;

    private StillSheetPlayback() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            forgetWorld();
            return;
        }
        alignWorld(clientLevel);
        captureLoaded(clientLevel);
        RETAINED.dropExpired(clientLevel.getGameTime(), 0.0F);
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            forgetWorld();
            return;
        }
        alignWorld(clientLevel);
        captureLoaded(clientLevel);
        float partialTick = event.getPartialTick();
        long gameTime = clientLevel.getGameTime();
        drawSheets(event, clientLevel, gameTime, partialTick);
        RETAINED.dropExpired(gameTime, partialTick);
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        forgetWorld();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() != null && event.getLevel().isClientSide()) {
            forgetWorld();
        }
    }

    private static void alignWorld(ClientLevel clientLevel) {
        if (boundLevel != clientLevel) {
            RETAINED.clear();
            boundLevel = clientLevel;
        }
        RETAINED.dropOtherWorlds(clientLevel.dimension().location().toString());
    }

    private static void forgetWorld() {
        RETAINED.clear();
        boundLevel = null;
    }

    private static void captureLoaded(ClientLevel clientLevel) {
        for (Entity entity : clientLevel.entitiesForRendering()) {
            if (entity instanceof TechniqueHitEntity hitEntity) {
                offerEntity(clientLevel, hitEntity);
            }
        }
    }

    private static void offerEntity(ClientLevel clientLevel, TechniqueHitEntity hitEntity) {
        StillSheetVisual visual = hitEntity.stillSheet();
        if (!visual.enabled()) {
            return;
        }
        if (hitEntity.stillSheetCancelled()) {
            RETAINED.cancel(hitEntity.getUUID());
            return;
        }
        long birthGameTime = hitEntity.visualBirth();
        if (birthGameTime < 0L) {
            return;
        }
        StillSheetSnapshot snapshot = new StillSheetSnapshot(
                hitEntity.getUUID(),
                clientLevel.dimension().location().toString(),
                hitEntity.getX(),
                hitEntity.getY(),
                hitEntity.getZ(),
                hitEntity.getYRot(),
                hitEntity.getXRot(),
                hitEntity.roll(),
                hitEntity.width(),
                hitEntity.length(),
                hitEntity.height(),
                hitEntity.cohortVisual(),
                hitEntity.texture(),
                visual.frameCount(),
                visual.ticksPerFrame(),
                birthGameTime);
        RETAINED.offer(snapshot);
    }

    private static void drawSheets(RenderLevelStageEvent event, ClientLevel clientLevel, long gameTime, float partialTick) {
        List<StillSheetSnapshot> retained = RETAINED.copyRetained();
        if (retained.isEmpty()) {
            return;
        }
        String worldIdentity = clientLevel.dimension().location().toString();
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        List<RenderType> drawnTypes = new ArrayList<>();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (StillSheetSnapshot snapshot : retained) {
            if (!worldIdentity.equals(snapshot.worldIdentity())) {
                continue;
            }
            int frame = StillSheetClock.frameIndex(gameTime, partialTick, snapshot.birthGameTime(),
                    snapshot.frameCount(), snapshot.ticksPerFrame());
            if (frame < 0) {
                continue;
            }
            double offsetEast = snapshot.worldX() - camera.x;
            double offsetAbove = snapshot.worldY() - camera.y;
            double offsetSouth = snapshot.worldZ() - camera.z;
            double reachSquared = offsetEast * offsetEast + offsetAbove * offsetAbove + offsetSouth * offsetSouth;
            if (reachSquared > VIEW_REACH * VIEW_REACH) {
                continue;
            }
            ResourceLocation sheetTexture = ResourceLocation.tryParse(snapshot.texturePath());
            if (sheetTexture == null) {
                continue;
            }
            CohortLaneLayout.FaceWindow faceWindow = StillSheetLayout.faceWindow(
                    snapshot.sheetWidth(), snapshot.sheetLength(), snapshot.sheetHeight(), snapshot.cohort());
            RenderType currentType = StillSheetMesh.of(sheetTexture);
            if (!drawnTypes.contains(currentType)) {
                drawnTypes.add(currentType);
            }
            pose.pushPose();
            pose.translate(snapshot.worldX(), snapshot.worldY(), snapshot.worldZ());
            pose.mulPose(Axis.YP.rotationDegrees(-snapshot.faceYaw()));
            if (Math.abs(snapshot.facePitch()) > 0.01F || Math.abs(snapshot.faceRoll()) > 0.01F) {
                pose.mulPose(Axis.XP.rotationDegrees(snapshot.facePitch()));
                pose.mulPose(Axis.ZP.rotationDegrees(snapshot.faceRoll()));
            }
            float topV = StillSheetClock.textureV(frame, 0.0F, snapshot.frameCount());
            float bottomV = StillSheetClock.textureV(frame, 1.0F, snapshot.frameCount());
            int packedLight = LevelRenderer.getLightColor(clientLevel,
                    BlockPos.containing(snapshot.worldX(), snapshot.worldY(), snapshot.worldZ()));
            VertexConsumer consumer = buffers.getBuffer(currentType);
            sheetQuad(consumer, pose.last().pose(), pose.last().normal(), packedLight, faceWindow, topV, bottomV);
            pose.popPose();
        }
        pose.popPose();
        for (RenderType drawnType : drawnTypes) {
            buffers.endBatch(drawnType);
        }
    }

    private static void sheetQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f sheetNormal, int packedLight,
                                  CohortLaneLayout.FaceWindow faceWindow, float topV, float bottomV) {
        float acrossLow = faceWindow.acrossLow();
        float acrossHigh = faceWindow.acrossHigh();
        float riseLow = faceWindow.riseLow();
        float riseHigh = faceWindow.riseHigh();
        consumer.vertex(matrix, acrossLow, riseLow, 0.0F).color(255, 255, 255, 255).uv(0.0F, bottomV)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(sheetNormal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, acrossHigh, riseLow, 0.0F).color(255, 255, 255, 255).uv(1.0F, bottomV)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(sheetNormal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, acrossHigh, riseHigh, 0.0F).color(255, 255, 255, 255).uv(1.0F, topV)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(sheetNormal, 0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, acrossLow, riseHigh, 0.0F).color(255, 255, 255, 255).uv(0.0F, topV)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(sheetNormal, 0.0F, 0.0F, 1.0F).endVertex();
    }
}
