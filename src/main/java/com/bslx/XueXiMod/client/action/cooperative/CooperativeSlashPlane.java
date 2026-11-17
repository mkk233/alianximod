package com.bslx.XueXiMod.client.action.cooperative;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 已确认薄壳。中心是斩击实体原点，宽、纵深和倾角来自同步几何。
 * 上下各四片、左右侧边各四片，共 16 个四边形、32 个三角形。贴图透明，每片只写一次。
 */
public final class CooperativeSlashPlane {
    private static final float[] PHASE_SECONDS = {0.0F, 0.04F, 0.14F, 0.2F};
    private static final float[] PHASE_SCALE = {0.92F, 1.0F, 1.0F, 0.001F};
    private static final float AUTHORED_WIDTH = 5.0F;
    private static final float AUTHORED_DEPTH = 3.0F;
    private static final float AUTHORED_LOGICAL_THICKNESS = 0.5F;
    private static final float AUTHORED_PEAK_HALF = 0.16F;
    private static final float FLAT_PEAK_HALF = 0.04F;
    /** 相对实体中心的纵深折点。后端停在 -1.15，不把壳补回眼点。 */
    private static final float[] ALONG_STATIONS = {-1.15F, -0.75F, -0.25F, 0.50F, 1.50F};
    private static final float[] HALF_STATIONS = {0.0F, 0.08F, 0.16F, 0.10F, 0.0F};

    private CooperativeSlashPlane() {
    }

    public static boolean matches(TechniqueHitEntity entity) {
        if (entity == null || CooperativeTechniqueIds.HORIZONTAL_SLASH.equals(entity.getTechniqueId())) {
            return false;
        }
        return CooperativeSlashPresentations.matchesTechnique(entity.getTechniqueId())
                || CooperativeSlashPresentations.matchesTexture(entity.texture());
    }

    public static void draw(TechniqueHitEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
        if (!matches(entity) || poseStack == null || buffer == null) {
            return;
        }
        int lifetimeTicks = entity.lifetime();
        if (lifetimeTicks < 1) {
            return;
        }
        double faceWidth = entity.width();
        double faceDepth = entity.length();
        double logicalThickness = entity.height();
        if (!Double.isFinite(faceWidth) || !Double.isFinite(faceDepth) || !Double.isFinite(logicalThickness)
                || faceWidth <= 0.0D || faceDepth <= 0.0D || logicalThickness < 0.0D) {
            return;
        }
        float seenSeconds = (entity.tickCount + partialTick) / 20.0F;
        float windowSeconds = lifetimeTicks / 20.0F;
        float phaseSeconds = seenSeconds / windowSeconds * CooperativeSlashPresentations.AUTHORED_WINDOW_SECONDS;
        float scale = phaseScale(phaseSeconds);
        if (scale < CooperativeSlashPresentations.PHASE_END_SCALE) {
            return;
        }
        float widthRatio = (float) (faceWidth / AUTHORED_WIDTH);
        float depthRatio = (float) (faceDepth / AUTHORED_DEPTH);
        float thicknessRatio = logicalThickness == 0.0D
                ? FLAT_PEAK_HALF / AUTHORED_PEAK_HALF
                : (float) (logicalThickness / AUTHORED_LOGICAL_THICKNESS);
        if (!Float.isFinite(widthRatio) || !Float.isFinite(depthRatio) || !Float.isFinite(thicknessRatio)) {
            return;
        }
        float across = (float) (faceWidth * 0.5D * scale);
        float alongScale = depthRatio * scale;
        float halfScale = tightest(widthRatio, depthRatio, thicknessRatio) * scale;
        if (!Float.isFinite(across) || !Float.isFinite(alongScale) || !Float.isFinite(halfScale)) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.roll()));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(CooperativeSlashPresentations.TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        int packedLight = LightTexture.FULL_BRIGHT;
        for (int stationIndex = 1; stationIndex < ALONG_STATIONS.length; stationIndex++) {
            writeStation(
                    consumer,
                    matrix,
                    normalMatrix,
                    packedLight,
                    across,
                    alongScale,
                    halfScale,
                    stationIndex - 1,
                    stationIndex);
        }
        poseStack.popPose();
    }

    private static float phaseScale(float phaseSeconds) {
        if (phaseSeconds <= PHASE_SECONDS[0]) {
            return PHASE_SCALE[0];
        }
        if (phaseSeconds >= PHASE_SECONDS[PHASE_SECONDS.length - 1]) {
            return PHASE_SCALE[PHASE_SCALE.length - 1];
        }
        for (int phaseIndex = 1; phaseIndex < PHASE_SECONDS.length; phaseIndex++) {
            if (phaseSeconds <= PHASE_SECONDS[phaseIndex]) {
                float span = PHASE_SECONDS[phaseIndex] - PHASE_SECONDS[phaseIndex - 1];
                float ratio = span <= 1.0E-6F ? 0.0F : (phaseSeconds - PHASE_SECONDS[phaseIndex - 1]) / span;
                return PHASE_SCALE[phaseIndex - 1] + (PHASE_SCALE[phaseIndex] - PHASE_SCALE[phaseIndex - 1]) * ratio;
            }
        }
        return PHASE_SCALE[PHASE_SCALE.length - 1];
    }

    private static float tightest(float widthRatio, float depthRatio, float thicknessRatio) {
        float tight = widthRatio;
        if (depthRatio < tight) {
            tight = depthRatio;
        }
        if (thicknessRatio < tight) {
            tight = thicknessRatio;
        }
        if (tight > 1.0F) {
            return 1.0F;
        }
        if (tight < 0.0F) {
            return 0.0F;
        }
        return tight;
    }

    /** 确认视口的贴图 Y 以 PNG 顶部为 0：0.5 - 纵深/3。后端裁剪只少取一段，不把整张图拉开。 */
    private static float textureAhead(float authoredAlong) {
        float textureRow = 0.5F - authoredAlong / AUTHORED_DEPTH;
        if (textureRow < 0.0F) {
            return 0.0F;
        }
        if (textureRow > 1.0F) {
            return 1.0F;
        }
        return textureRow;
    }

    private static void writeStation(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normalMatrix,
            int packedLight,
            float across,
            float alongScale,
            float halfScale,
            int nearIndex,
            int farIndex) {
        float authoredNear = ALONG_STATIONS[nearIndex];
        float authoredFar = ALONG_STATIONS[farIndex];
        float alongNear = authoredNear * alongScale;
        float alongFar = authoredFar * alongScale;
        float halfNear = HALF_STATIONS[nearIndex] * halfScale;
        float halfFar = HALF_STATIONS[farIndex] * halfScale;
        float textureNear = textureAhead(authoredNear);
        float textureFar = textureAhead(authoredFar);
        float deltaAlong = alongFar - alongNear;
        float deltaHalf = halfFar - halfNear;
        float rise = deltaAlong;
        float slope = -deltaHalf;
        float squared = rise * rise + slope * slope;
        float normalY = 1.0F;
        float normalZ = 0.0F;
        if (squared > 1.0E-8F) {
            float inverse = 1.0F / (float) Math.sqrt(squared);
            normalY = rise * inverse;
            normalZ = slope * inverse;
        }
        writeCorner(consumer, matrix, normalMatrix, packedLight, -across, halfNear, alongNear, 0.0F, textureNear, 0.0F, normalY, normalZ);
        writeCorner(consumer, matrix, normalMatrix, packedLight, across, halfNear, alongNear, 1.0F, textureNear, 0.0F, normalY, normalZ);
        writeCorner(consumer, matrix, normalMatrix, packedLight, across, halfFar, alongFar, 1.0F, textureFar, 0.0F, normalY, normalZ);
        writeCorner(consumer, matrix, normalMatrix, packedLight, -across, halfFar, alongFar, 0.0F, textureFar, 0.0F, normalY, normalZ);
        writeCorner(consumer, matrix, normalMatrix, packedLight, across, -halfNear, alongNear, 1.0F, textureNear, 0.0F, -normalY, -normalZ);
        writeCorner(consumer, matrix, normalMatrix, packedLight, -across, -halfNear, alongNear, 0.0F, textureNear, 0.0F, -normalY, -normalZ);
        writeCorner(consumer, matrix, normalMatrix, packedLight, -across, -halfFar, alongFar, 0.0F, textureFar, 0.0F, -normalY, -normalZ);
        writeCorner(consumer, matrix, normalMatrix, packedLight, across, -halfFar, alongFar, 1.0F, textureFar, 0.0F, -normalY, -normalZ);
        writeCorner(consumer, matrix, normalMatrix, packedLight, -across, halfNear, alongNear, 0.0F, textureNear, -1.0F, 0.0F, 0.0F);
        writeCorner(consumer, matrix, normalMatrix, packedLight, -across, halfFar, alongFar, 0.0F, textureFar, -1.0F, 0.0F, 0.0F);
        writeCorner(consumer, matrix, normalMatrix, packedLight, -across, -halfFar, alongFar, 0.0F, textureFar, -1.0F, 0.0F, 0.0F);
        writeCorner(consumer, matrix, normalMatrix, packedLight, -across, -halfNear, alongNear, 0.0F, textureNear, -1.0F, 0.0F, 0.0F);
        writeCorner(consumer, matrix, normalMatrix, packedLight, across, halfNear, alongNear, 1.0F, textureNear, 1.0F, 0.0F, 0.0F);
        writeCorner(consumer, matrix, normalMatrix, packedLight, across, -halfNear, alongNear, 1.0F, textureNear, 1.0F, 0.0F, 0.0F);
        writeCorner(consumer, matrix, normalMatrix, packedLight, across, -halfFar, alongFar, 1.0F, textureFar, 1.0F, 0.0F, 0.0F);
        writeCorner(consumer, matrix, normalMatrix, packedLight, across, halfFar, alongFar, 1.0F, textureFar, 1.0F, 0.0F, 0.0F);
    }

    private static void writeCorner(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normalMatrix,
            int packedLight,
            float pointX,
            float pointY,
            float pointZ,
            float textureU,
            float textureV,
            float normalX,
            float normalY,
            float normalZ) {
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(255, 255, 255, 255)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }
}
