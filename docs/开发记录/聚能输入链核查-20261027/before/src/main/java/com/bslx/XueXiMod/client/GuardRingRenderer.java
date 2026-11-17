package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.GuardSyncPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** 水环护体的 v8 水膜。环心和姿态仍跟同步视图，网格三层共用一次变换。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class GuardRingRenderer {
    private GuardRingRenderer() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (GuardSyncPacket.ClientRings.all().isEmpty()) {
            return;
        }
        WaterRingV8Mesh mesh = WaterRingV8Mesh.current();
        if (mesh == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        long gameTime = minecraft.level.getGameTime();
        Vec3 camera = event.getCamera().getPosition();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        RenderType meshType = WaterRingV8Material.mesh();
        VertexConsumer consumer = buffer.getBuffer(meshType);
        double[] placedPoint = new double[3];
        double[] offsetX = new double[4];
        double[] offsetY = new double[4];
        double[] offsetZ = new double[4];
        float[] atlasUv = new float[2];
        for (GuardSyncPacket.ClientRings.View view : GuardSyncPacket.ClientRings.all()) {
            Player wearer = minecraft.level.getPlayerByUUID(view.ownerId);
            if (wearer == null || !Double.isFinite(view.radius) || view.radius <= 0.0f) {
                continue;
            }
            double centerX = Mth.lerp(partialTick, wearer.xo, wearer.getX());
            double centerY = Mth.lerp(partialTick, wearer.yo, wearer.getY()) + wearer.getBbHeight() * view.bodyHeight;
            double centerZ = Mth.lerp(partialTick, wearer.zo, wearer.getZ());
            if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(centerZ)) {
                continue;
            }
            Vec3 normal = blendedNormal(view);
            double age = view.animationAge(gameTime, partialTick);
            WaterRingV8Math.PlacedFrame placed = WaterRingV8Math.PlacedFrame.prepare(
                    mesh.tiltDegrees,
                    WaterRingV8Math.spinRadians(age),
                    view.radius / WaterRingV8Math.MODEL_RADIUS,
                    normal.x, normal.y, normal.z);
            boolean fadeNearEye = firstPerson && wearer == minecraft.player;
            pose.pushPose();
            pose.translate(
                    WaterRingV8Math.relativeToCamera(centerX, camera.x),
                    WaterRingV8Math.relativeToCamera(centerY, camera.y),
                    WaterRingV8Math.relativeToCamera(centerZ, camera.z));
            drawRing(consumer, pose.last().pose(), mesh, placed, WaterRingV8Math.frameIndex(age),
                    centerX, centerY, centerZ, camera, fadeNearEye,
                    placedPoint, offsetX, offsetY, offsetZ, atlasUv);
            pose.popPose();
        }
        buffer.endBatch(meshType);
    }

    private static Vec3 blendedNormal(GuardSyncPacket.ClientRings.View view) {
        float span = Math.max(1, view.poseTicks) * 50f;
        float progress = Mth.clamp((System.currentTimeMillis() - view.poseStartMillis) / span, 0f, 1f);
        double normalX = Mth.lerp(progress, view.fromX, view.towardX);
        double normalY = Mth.lerp(progress, view.fromY, view.towardY);
        double normalZ = Mth.lerp(progress, view.fromZ, view.towardZ);
        Vec3 normal = new Vec3(normalX, normalY, normalZ);
        if (normal.lengthSqr() < 1.0E-6) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return normal.normalize();
    }

    private static void drawRing(VertexConsumer consumer, Matrix4f matrix, WaterRingV8Mesh mesh,
                                  WaterRingV8Math.PlacedFrame placed, int frame,
                                  double centerX, double centerY, double centerZ, Vec3 camera, boolean fadeNearEye,
                                  double[] placedPoint, double[] offsetX, double[] offsetY, double[] offsetZ,
                                  float[] atlasUv) {
        for (WaterRingV8Mesh.Layer layer : mesh.layers) {
            for (int quadIndex = 0; quadIndex < layer.quadCount; quadIndex++) {
                for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                    int corner = quadIndex * 4 + vertexIndex;
                    placed.apply(layer.localX[corner], layer.localY[corner], layer.localZ[corner], placedPoint);
                    offsetX[vertexIndex] = placedPoint[0];
                    offsetY[vertexIndex] = placedPoint[1];
                    offsetZ[vertexIndex] = placedPoint[2];
                }
                int alpha = faceAlpha(fadeNearEye, centerX, centerY, centerZ, camera, offsetX, offsetY, offsetZ);
                if (alpha <= 0) {
                    continue;
                }
                for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                    int corner = quadIndex * 4 + vertexIndex;
                    WaterRingV8Math.mapUv(WaterRingV8Math.ATLAS_WIDTH, WaterRingV8Math.ATLAS_HEIGHT,
                            WaterRingV8Math.LOGICAL_WIDTH, layer.atlasOriginX, layer.frameHeight, frame,
                            layer.sourceU[corner], layer.sourceV[corner], atlasUv);
                    consumer.vertex(matrix, (float) offsetX[vertexIndex], (float) offsetY[vertexIndex], (float) offsetZ[vertexIndex])
                            .color(255, 255, 255, alpha)
                            .uv(atlasUv[0], atlasUv[1])
                            .endVertex();
                }
            }
        }
    }

    private static int faceAlpha(boolean fadeNearEye, double centerX, double centerY, double centerZ, Vec3 camera,
                                  double[] offsetX, double[] offsetY, double[] offsetZ) {
        double midX = centerX + (offsetX[0] + offsetX[1] + offsetX[2] + offsetX[3]) * 0.25;
        double midY = centerY + (offsetY[0] + offsetY[1] + offsetY[2] + offsetY[3]) * 0.25;
        double midZ = centerZ + (offsetZ[0] + offsetZ[1] + offsetZ[2] + offsetZ[3]) * 0.25;
        double deltaX = midX - camera.x;
        double deltaY = midY - camera.y;
        double deltaZ = midZ - camera.z;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        return WaterRingV8Math.nearEyeAlpha(fadeNearEye, distance);
    }
}
