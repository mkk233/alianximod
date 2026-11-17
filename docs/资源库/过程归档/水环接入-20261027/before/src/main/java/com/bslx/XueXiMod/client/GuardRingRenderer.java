package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.GuardSyncPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** 水环跟着身体画。气泡图只贴在环带上，不生成气泡。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class GuardRingRenderer {
    private static final ResourceLocation BUBBLE = new ResourceLocation("minecraft", "textures/particle/bubble.png");
    private static final int SEGMENTS = 32;

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
        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(BUBBLE));
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = pose.last().pose();
        for (GuardSyncPacket.ClientRings.View view : GuardSyncPacket.ClientRings.all()) {
            Player wearer = minecraft.level.getPlayerByUUID(view.ownerId);
            if (wearer == null) {
                continue;
            }
            double centerX = Mth.lerp(partialTick, wearer.xo, wearer.getX());
            double centerY = Mth.lerp(partialTick, wearer.yo, wearer.getY()) + wearer.getBbHeight() * view.bodyHeight;
            double centerZ = Mth.lerp(partialTick, wearer.zo, wearer.getZ());
            Vec3 normal = blendedNormal(view);
            boolean fadeNearEye = firstPerson && wearer == minecraft.player;
            ring(consumer, matrix, camera, centerX, centerY, centerZ, normal, view.radius, view.band, fadeNearEye);
        }
        pose.popPose();
        buffer.endBatch(RenderType.entityTranslucent(BUBBLE));
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

    private static void ring(VertexConsumer consumer, Matrix4f matrix, Vec3 camera,
                             double centerX, double centerY, double centerZ, Vec3 normal,
                             float radius, float band, boolean fadeNearEye) {
        Vec3 helper = Math.abs(normal.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 side = helper.cross(normal);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1.0, 0.0, 0.0);
        }
        side = side.normalize();
        Vec3 other = normal.cross(side).normalize();
        float outer = radius + band * 0.5f;
        float inner = Math.max(0.05f, radius - band * 0.5f);
        int packedLight = LightTexture.FULL_BRIGHT;
        for (int segmentIndex = 0; segmentIndex < SEGMENTS; segmentIndex++) {
            double startAngle = segmentIndex * Math.PI * 2.0 / SEGMENTS;
            double endAngle = (segmentIndex + 1) * Math.PI * 2.0 / SEGMENTS;
            Vec3 startOuter = place(centerX, centerY, centerZ, side, other, startAngle, outer);
            Vec3 endOuter = place(centerX, centerY, centerZ, side, other, endAngle, outer);
            Vec3 endInner = place(centerX, centerY, centerZ, side, other, endAngle, inner);
            Vec3 startInner = place(centerX, centerY, centerZ, side, other, startAngle, inner);
            int alpha = 170;
            if (fadeNearEye) {
                Vec3 middle = startOuter.add(endInner).scale(0.5);
                double distance = middle.distanceTo(camera);
                if (distance < 0.35) {
                    continue;
                }
                if (distance < 0.7) {
                    alpha = (int) (170 * (distance - 0.35) / 0.35);
                }
            }
            float textureU = segmentIndex / (float) SEGMENTS;
            float nextU = (segmentIndex + 1) / (float) SEGMENTS;
            quad(consumer, matrix, packedLight, startOuter, endOuter, endInner, startInner,
                    (float) normal.x, (float) normal.y, (float) normal.z, alpha, textureU, nextU);
            Vec3 offset = normal.scale(0.002);
            quad(consumer, matrix, packedLight,
                    endOuter.subtract(offset), startOuter.subtract(offset), startInner.subtract(offset), endInner.subtract(offset),
                    (float) -normal.x, (float) -normal.y, (float) -normal.z, alpha, nextU, textureU);
        }
    }

    private static Vec3 place(double centerX, double centerY, double centerZ, Vec3 side, Vec3 other, double angle, float reach) {
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        return new Vec3(
                centerX + side.x * cosine * reach + other.x * sine * reach,
                centerY + side.y * cosine * reach + other.y * sine * reach,
                centerZ + side.z * cosine * reach + other.z * sine * reach);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, int packedLight,
                             Vec3 first, Vec3 second, Vec3 third, Vec3 fourth,
                             float normalX, float normalY, float normalZ, int alpha, float leftU, float rightU) {
        vertex(consumer, matrix, packedLight, first, leftU, 0f, normalX, normalY, normalZ, alpha);
        vertex(consumer, matrix, packedLight, second, rightU, 0f, normalX, normalY, normalZ, alpha);
        vertex(consumer, matrix, packedLight, third, rightU, 1f, normalX, normalY, normalZ, alpha);
        vertex(consumer, matrix, packedLight, fourth, leftU, 1f, normalX, normalY, normalZ, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, int packedLight, Vec3 point,
                               float textureU, float textureV, float normalX, float normalY, float normalZ, int alpha) {
        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(90, 180, 220, alpha)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalX, normalY, normalZ)
                .endVertex();
    }
}
