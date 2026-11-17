package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.DomainMotionPreview;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.FunnelOutline;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.MotionFrame;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.VolumeMotionKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;
import java.util.UUID;

/** 确认版水带和气泡膜。未抬升独立旋流是三层倾斜回旋；组合漏斗、水下汇入、气泡和局部携带保持原形。半径、高度和受控根只读同步快照。 */
public final class DomainFlowRenderer {
    private static final ResourceLocation FLOW = new ResourceLocation(XueXiMod.MOD_ID, "textures/domain/water_flow_v3_1.png");
    private static final ResourceLocation BUBBLE = new ResourceLocation(XueXiMod.MOD_ID, "textures/domain/water_bubble_v3_1.png");
    private static final double FAR_REACH = 64.0D;
    private static final float RIBBON = 0.5625F;
    private static final double REFERENCE_BODY = 2.5D;

    private DomainFlowRenderer() {
    }

    public static boolean wants(DomainSightStore.Sight sight) {
        MotionFrame frame = DomainMotionPreview.frame(sight.instanceId());
        return frame.kind() == VolumeMotionKind.VORTEX_HOST || frame.kind() == VolumeMotionKind.UPDRAFT;
    }

    public static void draw(DomainSightStore.Sight sight, PoseStack pose, Vec3 camera, Vec3 look,
                            MultiBufferSource.BufferSource buffers, ClientLevel level, long gameTime) {
        MotionFrame frame = DomainMotionPreview.frame(sight.instanceId());
        AABB box = sight.bounds();
        Vec3 center = box.getCenter();
        if (center.subtract(camera).lengthSqr() > FAR_REACH * FAR_REACH) {
            return;
        }
        if (center.subtract(camera).dot(look) < -0.2D && center.subtract(camera).lengthSqr() > 144.0D) {
            return;
        }
        double axisX = (box.minX + box.maxX) * 0.5D;
        double axisZ = (box.minZ + box.maxZ) * 0.5D;
        double bottom = box.minY;
        double columnHeight = Math.max(0.5D, box.maxY - box.minY);
        double bodyRadius = frame.bodyRadius() > 0.0D ? frame.bodyRadius() : (box.maxX - box.minX) * 0.5D;
        pose.pushPose();
        pose.translate(axisX - camera.x, bottom - camera.y, axisZ - camera.z);
        Matrix4f matrix = pose.last().pose();
        if (frame.kind() == VolumeMotionKind.UPDRAFT) {
            if (DomainMotionPreview.mountedHost(sight.instanceId()) == null) {
                VertexConsumer film = buffers.getBuffer(RenderType.entityTranslucent(BUBBLE));
                drawBubbleFilm(film, matrix, level, axisX, axisZ, bottom, columnHeight, bodyRadius, gameTime);
            }
            pose.popPose();
            return;
        }
        VertexConsumer ribbon = buffers.getBuffer(RenderType.entityTranslucent(FLOW));
        float spin = (gameTime + sight.animSeed()) * 0.08F;
        if (frame.lifted()) {
            drawFunnel(ribbon, matrix, bodyRadius, columnHeight, spin);
            drawUnderwater(ribbon, matrix, level, axisX, axisZ, bottom, bodyRadius, spin);
            drawCarry(ribbon, matrix, level, axisX, bottom, axisZ, bodyRadius, columnHeight, sight.instanceId());
            if (drawsMountedBubble(sight.instanceId())) {
                VertexConsumer film = buffers.getBuffer(RenderType.entityTranslucent(BUBBLE));
                drawBubbleFilm(film, matrix, level, axisX, axisZ, bottom, columnHeight, bodyRadius, gameTime);
            }
        } else {
            double attraction = frame.attraction() > 0.0D ? frame.attraction() : 8.0D;
            drawStandalone(ribbon, matrix, level, axisX, axisZ, bottom, attraction, spin);
        }
        pose.popPose();
    }

    public static void end(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(RenderType.entityTranslucent(FLOW));
        buffers.endBatch(RenderType.entityTranslucent(BUBBLE));
    }

    /** 未抬升独立旋流。三层各两条，48 段向中心收束；半径按吸引范围相对默认 8 格缩放。 */
    private static void drawStandalone(VertexConsumer consumer, Matrix4f matrix, ClientLevel level,
                                      double axisX, double axisZ, double bottom, double attraction, float spin) {
        double radialScale = attraction / 8.0D;
        float scroll = spin * 0.15F;
        for (int layerIndex = 0; layerIndex < 3; layerIndex++) {
            for (int bandIndex = 0; bandIndex < 2; bandIndex++) {
                float phase = spin + layerIndex * ((float) Math.PI / 3.0F) + bandIndex * (float) Math.PI;
                layStandaloneBand(consumer, matrix, level, axisX, axisZ, bottom, layerIndex, phase, radialScale, scroll);
            }
        }
    }

    private static void layStandaloneBand(VertexConsumer consumer, Matrix4f matrix, ClientLevel level,
                                         double axisX, double axisZ, double bottom, int layerIndex,
                                         float phase, double radialScale, float scroll) {
        int segmentCount = 48;
        float[] innerX = new float[segmentCount + 1];
        float[] innerY = new float[segmentCount + 1];
        float[] innerZ = new float[segmentCount + 1];
        float[] outerX = new float[segmentCount + 1];
        float[] outerY = new float[segmentCount + 1];
        float[] outerZ = new float[segmentCount + 1];
        double innerCenter = 1.35D + layerIndex * 0.18D;
        for (int stepIndex = 0; stepIndex <= segmentCount; stepIndex++) {
            double fraction = stepIndex / (double) segmentCount;
            double centerRadius = (7.42D + (innerCenter - 7.42D) * fraction) * radialScale;
            double angle = phase + 6.15D * fraction;
            double centerHeight = 0.44D + layerIndex * 0.75D
                    + 0.12D * Math.sin(Math.PI * 2.0D * fraction + layerIndex * 0.6D);
            double halfWidth = 0.58D * (0.7D + 0.3D * Math.sin(Math.PI * fraction)) * radialScale;
            float cosine = (float) Math.cos(angle);
            float sine = (float) Math.sin(angle);
            float innerReach = (float) (centerRadius - halfWidth);
            float outerReach = (float) (centerRadius + halfWidth);
            innerX[stepIndex] = cosine * innerReach;
            innerY[stepIndex] = (float) (centerHeight - 0.19D);
            innerZ[stepIndex] = sine * innerReach;
            outerX[stepIndex] = cosine * outerReach;
            outerY[stepIndex] = (float) (centerHeight + 0.19D);
            outerZ[stepIndex] = sine * outerReach;
        }
        for (int stepIndex = 0; stepIndex < segmentCount; stepIndex++) {
            float sampleX = (innerX[stepIndex] + outerX[stepIndex] + innerX[stepIndex + 1] + outerX[stepIndex + 1]) * 0.25F;
            float sampleY = (innerY[stepIndex] + outerY[stepIndex] + innerY[stepIndex + 1] + outerY[stepIndex + 1]) * 0.25F;
            float sampleZ = (innerZ[stepIndex] + outerZ[stepIndex] + innerZ[stepIndex + 1] + outerZ[stepIndex + 1]) * 0.25F;
            if (solidBlocked(level, axisX + sampleX, bottom + sampleY, axisZ + sampleZ)) {
                continue;
            }
            float slice = (stepIndex % 8) / 8.0F;
            float textureStart = wrapUnit(scroll + slice);
            float textureEnd = textureStart + 0.125F;
            if (textureEnd > 1.0F) {
                textureStart = 0.0F;
                textureEnd = 0.125F;
            }
            bandQuad(consumer, matrix,
                    outerX[stepIndex], outerY[stepIndex], outerZ[stepIndex],
                    outerX[stepIndex + 1], outerY[stepIndex + 1], outerZ[stepIndex + 1],
                    innerX[stepIndex + 1], innerY[stepIndex + 1], innerZ[stepIndex + 1],
                    innerX[stepIndex], innerY[stepIndex], innerZ[stepIndex],
                    textureStart, textureEnd, 150);
        }
    }

    private static void drawFunnel(VertexConsumer consumer, Matrix4f matrix, double bodyRadius, double columnHeight, float spin) {
        for (int band = 0; band < 6; band++) {
            float startAngle = spin + band * ((float) Math.PI / 3.0F);
            layFunnel(consumer, matrix, bodyRadius, columnHeight, startAngle, 160);
        }
    }

    private static void drawUnderwater(VertexConsumer consumer, Matrix4f matrix, ClientLevel level, double axisX,
                                       double axisZ, double bottom, double bodyRadius, float spin) {
        double scale = bodyRadius / REFERENCE_BODY;
        double[] lifts = {0.44D, 1.19D, 1.94D};
        double[] mouths = {0.75D, 0.94D, 1.125D};
        for (int layer = 0; layer < lifts.length; layer++) {
            float mouth = (float) (mouths[layer] * scale);
            float outer = (float) (3.25D * scale);
            float deck = (float) lifts[layer];
            for (int band = 0; band < 2; band++) {
                float startAngle = spin * 1.4F + layer + band * (float) Math.PI;
                laySpiral(consumer, matrix, deck, mouth, outer, startAngle, 0.979F, 0.37F, 16, spin * 0.15F, 130,
                        0.0F, 0.0F, level, axisX, axisZ, bottom, true);
            }
        }
    }

    private static void drawBubbleFilm(VertexConsumer consumer, Matrix4f matrix, ClientLevel level, double axisX,
                                       double axisZ, double bottom, double columnHeight, double radius, long gameTime) {
        float filmY = (float) ((gameTime % 40L) / 40.0D * columnHeight);
        float filmRadius = (float) Math.max(0.35D, radius * 0.72D);
        for (int band = 0; band < 4; band++) {
            float angle = band * ((float) Math.PI / 2.0F) + filmY;
            float centerX = (float) Math.cos(angle) * filmRadius;
            float centerZ = (float) Math.sin(angle) * filmRadius;
            if (!waterOpen(level, axisX + centerX, bottom + filmY, axisZ + centerZ)) {
                continue;
            }
            float span = 0.42F;
            bandQuad(consumer, matrix,
                    centerX - span, filmY, centerZ - span,
                    centerX + span, filmY, centerZ - span,
                    centerX + span, filmY, centerZ + span,
                    centerX - span, filmY, centerZ + span,
                    0.0F, 1.0F, 170);
        }
    }

    private static void drawCarry(VertexConsumer consumer, Matrix4f matrix, ClientLevel level,
                                  double axisX, double bottom, double axisZ, double bodyRadius, double columnHeight,
                                  UUID instanceId) {
        List<UUID> roots = DomainMotionPreview.controlledRoots(instanceId);
        if (roots.isEmpty()) {
            return;
        }
        AABB query = new AABB(axisX - 16.0D, bottom - 4.0D, axisZ - 16.0D,
                axisX + 16.0D, bottom + columnHeight + 4.0D, axisZ + 16.0D);
        List<Entity> found = level.getEntities((Entity) null, query, candidate -> roots.contains(candidate.getUUID()));
        for (Entity carried : found) {
            Vec3 body = carried.getBoundingBox().getCenter();
            float localX = (float) (body.x - axisX);
            float localY = (float) (body.y - bottom);
            float localZ = (float) (body.z - axisZ);
            float reach = (float) Math.sqrt(localX * localX + localZ * localZ);
            if (reach < 0.2F) {
                continue;
            }
            float angle = (float) Math.atan2(localZ, localX);
            float arcRadius = Math.max(0.55F, carried.getBbWidth() * 0.55F);
            laySpiral(consumer, matrix, localY, arcRadius, arcRadius, angle, 260.0F / 360.0F, 0.28F, 12,
                    angle * 0.1F, 120, localX, localZ, null, 0.0D, 0.0D, 0.0D, false);
            float mouth = (float) FunnelOutline.radiusAt(bodyRadius, 0.0D, columnHeight, Math.max(0.0D, localY));
            float mouthX = (float) Math.cos(angle) * mouth;
            float mouthZ = (float) Math.sin(angle) * mouth;
            float edgeX = localX - (float) Math.cos(angle) * arcRadius;
            float edgeZ = localZ - (float) Math.sin(angle) * arcRadius;
            straightBand(consumer, matrix, mouthX, localY, mouthZ, edgeX, localY, edgeZ, 0.22F, 120);
        }
    }

    private static boolean drawsMountedBubble(UUID hostId) {
        for (DomainSightStore.Sight sight : DomainSightStore.all()) {
            MotionFrame frame = DomainMotionPreview.frame(sight.instanceId());
            if (frame.kind() == VolumeMotionKind.UPDRAFT && hostId.equals(DomainMotionPreview.mountedHost(sight.instanceId()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean solidBlocked(ClientLevel level, double worldX, double worldY, double worldZ) {
        BlockPos spot = BlockPos.containing(worldX, worldY, worldZ);
        return level.getBlockState(spot).isSolidRender(level, spot);
    }

    private static boolean waterOpen(ClientLevel level, double axisX, double sampleY, double axisZ) {
        BlockPos spot = BlockPos.containing(axisX, sampleY, axisZ);
        if (level.getBlockState(spot).isSolidRender(level, spot)) {
            return false;
        }
        return level.getFluidState(spot).is(FluidTags.WATER);
    }

    private static void laySpiral(VertexConsumer consumer, Matrix4f matrix, float height, float innerRadius,
                                  float outerRadius, float startAngle, float turnCount, float halfWidth, int segments,
                                  float scroll, int alpha, float originX, float originZ, ClientLevel level,
                                  double axisX, double axisZ, double bottom, boolean clipWater) {
        float[] pointX = new float[segments + 1];
        float[] pointZ = new float[segments + 1];
        for (int step = 0; step <= segments; step++) {
            float fraction = step / (float) segments;
            float radius = outerRadius + (innerRadius - outerRadius) * fraction;
            float angle = startAngle + turnCount * ((float) Math.PI * 2.0F) * fraction;
            pointX[step] = originX + (float) Math.cos(angle) * radius;
            pointZ[step] = originZ + (float) Math.sin(angle) * radius;
        }
        for (int step = 0; step < segments; step++) {
            float fromX = pointX[step];
            float fromZ = pointZ[step];
            float toX = pointX[step + 1];
            float toZ = pointZ[step + 1];
            if (clipWater) {
                float midX = (fromX + toX) * 0.5F;
                float midZ = (fromZ + toZ) * 0.5F;
                if (!waterOpen(level, axisX + midX, bottom + height, axisZ + midZ)) {
                    continue;
                }
            }
            float deltaX = toX - fromX;
            float deltaZ = toZ - fromZ;
            float span = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (span < 1.0E-4F) {
                continue;
            }
            float sideX = -deltaZ / span * halfWidth;
            float sideZ = deltaX / span * halfWidth;
            float textureU = wrapUnit(scroll + step / (float) segments);
            float textureEnd = textureU + 1.0F / segments;
            if (textureEnd > 1.0F) {
                textureU = 0.0F;
                textureEnd = 1.0F / segments;
            }
            bandQuad(consumer, matrix,
                    fromX + sideX, height, fromZ + sideZ,
                    toX + sideX, height, toZ + sideZ,
                    toX - sideX, height, toZ - sideZ,
                    fromX - sideX, height, fromZ - sideZ,
                    textureU, textureEnd, alpha);
        }
    }

    private static void layFunnel(VertexConsumer consumer, Matrix4f matrix, double bodyRadius, double columnHeight,
                                  float startAngle, int alpha) {
        int segments = 32;
        float halfRise = RIBBON * 0.5F;
        float[] pointX = new float[segments + 1];
        float[] pointY = new float[segments + 1];
        float[] pointZ = new float[segments + 1];
        for (int step = 0; step <= segments; step++) {
            float fraction = step / (float) segments;
            float rise = (float) (fraction * columnHeight);
            float radius = (float) FunnelOutline.radiusAt(bodyRadius, 0.0D, columnHeight, rise);
            float angle = startAngle + fraction * ((float) Math.PI * 4.0F);
            pointX[step] = (float) Math.cos(angle) * radius;
            pointY[step] = rise;
            pointZ[step] = (float) Math.sin(angle) * radius;
        }
        for (int step = 0; step < segments; step++) {
            float textureU = wrapUnit(startAngle * 0.05F + step / (float) segments);
            float textureEnd = textureU + 1.0F / segments;
            if (textureEnd > 1.0F) {
                textureU = 0.0F;
                textureEnd = 1.0F / segments;
            }
            bandQuad(consumer, matrix,
                    pointX[step], pointY[step] - halfRise, pointZ[step],
                    pointX[step + 1], pointY[step + 1] - halfRise, pointZ[step + 1],
                    pointX[step + 1], pointY[step + 1] + halfRise, pointZ[step + 1],
                    pointX[step], pointY[step] + halfRise, pointZ[step],
                    textureU, textureEnd, alpha);
        }
    }

    private static void straightBand(VertexConsumer consumer, Matrix4f matrix, float fromX, float fromY, float fromZ,
                                     float toX, float toY, float toZ, float halfWidth, int alpha) {
        float deltaX = toX - fromX;
        float deltaZ = toZ - fromZ;
        float span = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (span < 1.0E-4F) {
            return;
        }
        float sideX = -deltaZ / span * halfWidth;
        float sideZ = deltaX / span * halfWidth;
        bandQuad(consumer, matrix,
                fromX + sideX, fromY, fromZ + sideZ,
                toX + sideX, toY, toZ + sideZ,
                toX - sideX, toY, toZ - sideZ,
                fromX - sideX, fromY, fromZ - sideZ,
                0.0F, 1.0F, alpha);
    }

    private static float wrapUnit(float unit) {
        return unit - (float) Math.floor(unit);
    }

    private static void bandQuad(VertexConsumer consumer, Matrix4f matrix,
                                 float firstX, float firstY, float firstZ,
                                 float secondX, float secondY, float secondZ,
                                 float thirdX, float thirdY, float thirdZ,
                                 float fourthX, float fourthY, float fourthZ,
                                 float textureStart, float textureEnd, int alpha) {
        vertex(consumer, matrix, firstX, firstY, firstZ, textureStart, 1F, alpha);
        vertex(consumer, matrix, secondX, secondY, secondZ, textureEnd, 1F, alpha);
        vertex(consumer, matrix, thirdX, thirdY, thirdZ, textureEnd, 0F, alpha);
        vertex(consumer, matrix, fourthX, fourthY, fourthZ, textureStart, 0F, alpha);
        vertex(consumer, matrix, fourthX, fourthY, fourthZ, textureStart, 0F, alpha);
        vertex(consumer, matrix, thirdX, thirdY, thirdZ, textureEnd, 0F, alpha);
        vertex(consumer, matrix, secondX, secondY, secondZ, textureEnd, 1F, alpha);
        vertex(consumer, matrix, firstX, firstY, firstZ, textureStart, 1F, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, float pointX, float pointY, float pointZ,
                               float textureU, float textureV, int alpha) {
        consumer.vertex(matrix, pointX, pointY, pointZ).color(170, 220, 255, alpha)
                .uv(textureU, textureV).overlayCoords(0).uv2(0x00F000F0).normal(0F, 1F, 0F).endVertex();
    }
}
