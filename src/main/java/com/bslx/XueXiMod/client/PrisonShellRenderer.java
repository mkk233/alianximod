package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.visual.facing.CameraFacingEffectId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.UUID;

/**
 * 水牢等比公告板。单张面朝向当前客户端相机，含俯仰。
 * 相机在包络内时不画，避免第一人称被整张面挡住。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class PrisonShellRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            XueXiMod.MOD_ID, "textures/entity/xueximod/water_prison_billboard.png");
    private static final int FRAME_COUNT = 16;
    private static final double FRAME_EDGE = 96.0D;
    private static final double CONTENT_WIDTH = 70.0D;
    private static final double CONTENT_HEIGHT = 74.0D;
    private static final double COVER_SCALE = 1.04D;
    private static final double DEPTH_PULL = 0.08D;
    private static final double NEAR_CLEARANCE = 0.05D;
    private static final double FALLBACK_NEAR = 0.05D;
    private static final double CLIP_EPSILON = 1.0E-4D;
    private static final double DEGENERATE_SPAN = 1.0E-4D;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private PrisonShellRenderer() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        List<PrisonClientState.PrisonClientSnapshot> shown = PrisonClientState.visible();
        if (shown.isEmpty()) {
            return;
        }
        if (!CameraFacingClient.acceptsCameraPlane(CameraFacingEffectId.WATER_PRISON_SHELL)) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        Matrix4f rotation = new Matrix4f(pose.last().pose());
        Vector3f look = new Vector3f(event.getCamera().getLookVector());
        Vector3f upAxis = new Vector3f(event.getCamera().getUpVector());
        Vector3f leftAxis = new Vector3f(event.getCamera().getLeftVector());
        Vector3f forwardProbe = new Vector3f(look);
        rotation.transformPosition(forwardProbe);
        if (!Float.isFinite(forwardProbe.z) || Math.abs(forwardProbe.z) < 1.0E-4F) {
            return;
        }
        Matrix4f projection = event.getProjectionMatrix();
        RenderType sheetType = StillSheetMesh.of(TEXTURE);
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(sheetType);
        long gameTime = minecraft.level.getGameTime();
        float partialTick = event.getPartialTick();
        for (PrisonClientState.PrisonClientSnapshot snapshot : shown) {
            AABB envelope = envelopeOf(minecraft, snapshot, partialTick);
            if (envelope == null || cameraInside(envelope, camera)) {
                continue;
            }
            int frame = frameOf(gameTime, snapshot.birthTick(), partialTick);
            float topV = frame / (float) FRAME_COUNT;
            float bottomV = (frame + 1) / (float) FRAME_COUNT;
            submitBillboard(consumer, rotation, projection, camera, look, upAxis, leftAxis,
                    forwardProbe, envelope, topV, bottomV);
        }
        bufferSource.endBatch(sheetType);
    }

    private static AABB envelopeOf(Minecraft minecraft, PrisonClientState.PrisonClientSnapshot snapshot,
                                   float partialTick) {
        AABB frozen = snapshot.bounds();
        if (degenerateBox(frozen)) {
            return null;
        }
        ClientLevel level = minecraft.level;
        UUID targetId = snapshot.targetId();
        if (level == null || targetId == null) {
            return frozen;
        }
        Entity tracked = entityByUuid(level, targetId);
        if (tracked == null && snapshot.targetNetworkId() >= 0) {
            Entity backup = level.getEntity(snapshot.targetNetworkId());
            if (backup != null && targetId.equals(backup.getUUID())) {
                tracked = backup;
            }
        }
        if (tracked == null) {
            return frozen;
        }
        if (!tracked.isAlive() || tracked.isRemoved()) {
            return null;
        }
        if (minecraft.options.getCameraType().isFirstPerson() && tracked == minecraft.getCameraEntity()) {
            return null;
        }
        AABB body = interpolatedBody(tracked, partialTick);
        if (degenerateBox(body)) {
            return frozen;
        }
        return frozen.minmax(body);
    }

    private static Entity entityByUuid(ClientLevel level, UUID targetId) {
        for (Entity candidate : level.entitiesForRendering()) {
            if (candidate != null && targetId.equals(candidate.getUUID())) {
                return candidate;
            }
        }
        return null;
    }

    private static AABB interpolatedBody(Entity tracked, float partialTick) {
        double shiftX = Mth.lerp(partialTick, tracked.xo, tracked.getX()) - tracked.getX();
        double shiftY = Mth.lerp(partialTick, tracked.yo, tracked.getY()) - tracked.getY();
        double shiftZ = Mth.lerp(partialTick, tracked.zo, tracked.getZ()) - tracked.getZ();
        return tracked.getBoundingBox().move(shiftX, shiftY, shiftZ);
    }

    private static boolean degenerateBox(AABB box) {
        if (box == null) {
            return true;
        }
        if (!Double.isFinite(box.minX) || !Double.isFinite(box.minY) || !Double.isFinite(box.minZ)
                || !Double.isFinite(box.maxX) || !Double.isFinite(box.maxY) || !Double.isFinite(box.maxZ)) {
            return true;
        }
        return box.getXsize() < DEGENERATE_SPAN || box.getYsize() < DEGENERATE_SPAN || box.getZsize() < DEGENERATE_SPAN;
    }

    private static boolean cameraInside(AABB box, Vec3 camera) {
        return camera.x >= box.minX && camera.x <= box.maxX
                && camera.y >= box.minY && camera.y <= box.maxY
                && camera.z >= box.minZ && camera.z <= box.maxZ;
    }

    private static int frameOf(long gameTime, long birthTick, float partialTick) {
        double age = (double) (gameTime - birthTick) + partialTick;
        if (!Double.isFinite(age) || age < 0.0D) {
            return 0;
        }
        long stepped = (long) Math.floor(age / 2.0D);
        if (stepped < 0L) {
            return 0;
        }
        return (int) Math.floorMod(stepped, (long) FRAME_COUNT);
    }

    private static void submitBillboard(VertexConsumer consumer, Matrix4f rotation,
                                        Matrix4f projection, Vec3 camera, Vector3f look, Vector3f upAxis,
                                        Vector3f leftAxis, Vector3f forwardProbe, AABB envelope,
                                        float topV, float bottomV) {
        double scaleX = projection.m00();
        double scaleY = projection.m11();
        if (!(scaleX > 0.0D) || !(scaleY > 0.0D) || !Double.isFinite(scaleX) || !Double.isFinite(scaleY)) {
            return;
        }
        double nearPlane = (double) projection.m32() / ((double) projection.m22() - 1.0D);
        if (!Double.isFinite(nearPlane) || !(nearPlane > 0.0D)) {
            nearPlane = FALLBACK_NEAR;
        }
        double lowNdcX = Double.POSITIVE_INFINITY;
        double highNdcX = Double.NEGATIVE_INFINITY;
        double lowNdcY = Double.POSITIVE_INFINITY;
        double highNdcY = Double.NEGATIVE_INFINITY;
        double nearestDepth = Double.POSITIVE_INFINITY;
        int kept = 0;
        Vector3f viewPoint = new Vector3f();
        Vector4f clip = new Vector4f();
        for (int corner = 0; corner < 8; corner++) {
            double cornerX = (corner & 1) == 0 ? envelope.minX : envelope.maxX;
            double cornerY = (corner & 2) == 0 ? envelope.minY : envelope.maxY;
            double cornerZ = (corner & 4) == 0 ? envelope.minZ : envelope.maxZ;
            double depth = (cornerX - camera.x) * look.x + (cornerY - camera.y) * look.y
                    + (cornerZ - camera.z) * look.z;
            if (!Double.isFinite(depth) || depth <= 0.0D) {
                continue;
            }
            viewPoint.set((float) (cornerX - camera.x), (float) (cornerY - camera.y), (float) (cornerZ - camera.z));
            rotation.transformPosition(viewPoint);
            clip.set(viewPoint.x, viewPoint.y, viewPoint.z, 1.0F);
            projection.transform(clip);
            if (!Float.isFinite(clip.w) || clip.w <= CLIP_EPSILON || !Float.isFinite(clip.x) || !Float.isFinite(clip.y)) {
                continue;
            }
            double ndcX = clip.x / clip.w;
            double ndcY = clip.y / clip.w;
            if (!Double.isFinite(ndcX) || !Double.isFinite(ndcY)) {
                continue;
            }
            kept++;
            if (ndcX < lowNdcX) {
                lowNdcX = ndcX;
            }
            if (ndcX > highNdcX) {
                highNdcX = ndcX;
            }
            if (ndcY < lowNdcY) {
                lowNdcY = ndcY;
            }
            if (ndcY > highNdcY) {
                highNdcY = ndcY;
            }
            if (depth < nearestDepth) {
                nearestDepth = depth;
            }
        }
        if (kept < 1 || !Double.isFinite(nearestDepth)) {
            return;
        }
        double spanX = highNdcX - lowNdcX;
        double spanY = highNdcY - lowNdcY;
        if (!(spanX > 0.0D) || !(spanY > 0.0D) || !Double.isFinite(spanX) || !Double.isFinite(spanY)) {
            return;
        }
        double planeDepth = Math.max(nearPlane + NEAR_CLEARANCE, nearestDepth - DEPTH_PULL);
        if (!Double.isFinite(planeDepth) || !(planeDepth > 0.0D)) {
            return;
        }
        double bodyWidth = spanX * (planeDepth / scaleX);
        double bodyHeight = spanY * (planeDepth / scaleY);
        if (!(bodyWidth > 0.0D) || !(bodyHeight > 0.0D) || !Double.isFinite(bodyWidth) || !Double.isFinite(bodyHeight)) {
            return;
        }
        double side = Math.hypot(bodyWidth * (FRAME_EDGE / CONTENT_WIDTH), bodyHeight * (FRAME_EDGE / CONTENT_HEIGHT))
                * COVER_SCALE;
        if (!Double.isFinite(side) || !(side > 0.0D)) {
            return;
        }
        double centerNdcX = (lowNdcX + highNdcX) * 0.5D;
        double centerNdcY = (lowNdcY + highNdcY) * 0.5D;
        double viewX = centerNdcX * (planeDepth / scaleX) - projection.m30() / scaleX;
        double viewY = centerNdcY * (planeDepth / scaleY) - projection.m31() / scaleY;
        double viewZ = Math.copySign(planeDepth, (double) forwardProbe.z);
        if (!Double.isFinite(viewX) || !Double.isFinite(viewY) || !Double.isFinite(viewZ)) {
            return;
        }
        Vector3f worldOffset = new Vector3f((float) viewX, (float) viewY, (float) viewZ);
        new Matrix3f(rotation).transpose().transform(worldOffset);
        double centerX = camera.x + worldOffset.x;
        double centerY = camera.y + worldOffset.y;
        double centerZ = camera.z + worldOffset.z;
        if (!Double.isFinite(centerX) || !Double.isFinite(centerY) || !Double.isFinite(centerZ)) {
            return;
        }
        double halfEdge = side * 0.5D;
        double rightX = -leftAxis.x;
        double rightY = -leftAxis.y;
        double rightZ = -leftAxis.z;
        double upX = upAxis.x;
        double upY = upAxis.y;
        double upZ = upAxis.z;
        float normalX = -forwardProbe.x;
        float normalY = -forwardProbe.y;
        float normalZ = -forwardProbe.z;
        putVertex(consumer, rotation, camera, centerX - rightX * halfEdge - upX * halfEdge,
                centerY - rightY * halfEdge - upY * halfEdge, centerZ - rightZ * halfEdge - upZ * halfEdge,
                0.0F, bottomV, normalX, normalY, normalZ);
        putVertex(consumer, rotation, camera, centerX + rightX * halfEdge - upX * halfEdge,
                centerY + rightY * halfEdge - upY * halfEdge, centerZ + rightZ * halfEdge - upZ * halfEdge,
                1.0F, bottomV, normalX, normalY, normalZ);
        putVertex(consumer, rotation, camera, centerX + rightX * halfEdge + upX * halfEdge,
                centerY + rightY * halfEdge + upY * halfEdge, centerZ + rightZ * halfEdge + upZ * halfEdge,
                1.0F, topV, normalX, normalY, normalZ);
        putVertex(consumer, rotation, camera, centerX - rightX * halfEdge + upX * halfEdge,
                centerY - rightY * halfEdge + upY * halfEdge, centerZ - rightZ * halfEdge + upZ * halfEdge,
                0.0F, topV, normalX, normalY, normalZ);
    }

    private static void putVertex(VertexConsumer consumer, Matrix4f rotation, Vec3 camera, double pointX, double pointY,
                                  double pointZ, float textureU, float textureV, float normalX, float normalY,
                                  float normalZ) {
        consumer.vertex(rotation, (float) (pointX - camera.x), (float) (pointY - camera.y), (float) (pointZ - camera.z))
                .color(255, 255, 255, 255)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(normalX, normalY, normalZ)
                .endVertex();
    }
}
