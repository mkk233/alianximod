package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.entity.CohortLaneLayout;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortVisual;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端按实体走过的点拉一条水色带，用来看函数曲线。 */
public final class WaterRibbonRenderer {
    private static final ResourceLocation SPARK = new ResourceLocation("textures/entity/shulker/spark.png");
    private static final Map<Integer, Ribbon> RIBBONS = new ConcurrentHashMap<>();
    private static int frameSerial;

    private WaterRibbonRenderer() {}

    public static void render(TechniqueHitEntity entity, float partialTicks, PoseStack pose, MultiBufferSource buffer) {
        frameSerial++;
        Vec3 here = entity.getPosition(partialTicks);
        Ribbon ribbon = RIBBONS.computeIfAbsent(entity.getId(), key -> new Ribbon());
        ribbon.remember(here, frameSerial);
        if (ribbon.points.size() < 2) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(SPARK));
        Matrix4f matrix = pose.last().pose();
        int packedBright = LightTexture.FULL_BRIGHT;
        float ribbonScale = entity.getModelScale();
        if (!Float.isFinite(ribbonScale) || ribbonScale <= 0.001F) {
            ribbonScale = 1.0F;
        }
        List<Vec3> points = ribbon.points;
        CohortVisual visual = entity.cohortVisual();
        double crossSection = entity.width() > entity.height() ? entity.width() : entity.height();
        CohortLaneLayout.RibbonStrip strip = CohortLaneLayout.ribbonStrip(crossSection, 0.12 * ribbonScale, visual);
        for (int pointIndex = 0; pointIndex < points.size() - 1; pointIndex++) {
            boolean foam = pointIndex >= points.size() - 3;
            int red = foam ? 230 : 70;
            int green = foam ? 246 : 168;
            int blue = foam ? 255 : 214;
            int alpha = foam ? 210 : 150;
            ribbonQuad(consumer, matrix, packedBright, points.get(pointIndex).subtract(here), points.get(pointIndex + 1).subtract(here), red, green, blue, alpha, strip);
        }
        if ((frameSerial & 15) == 0) {
            int cutoff = frameSerial - 80;
            Iterator<Map.Entry<Integer, Ribbon>> entries = RIBBONS.entrySet().iterator();
            while (entries.hasNext()) {
                if (entries.next().getValue().lastFrame < cutoff) {
                    entries.remove();
                }
            }
        }
    }

    private static void ribbonQuad(VertexConsumer consumer, Matrix4f matrix, int packedLight,
                                   Vec3 from, Vec3 toward, int red, int green, int blue, int alpha,
                                   CohortLaneLayout.RibbonStrip strip) {
        Vec3 segment = toward.subtract(from);
        Vec3 side;
        if (strip.separated()) {
            CohortLaneLayout.LateralAxis lateral = CohortLaneLayout.stableLateral(segment.x, segment.y, segment.z);
            side = new Vec3(lateral.east(), lateral.above(), lateral.south());
        } else {
            side = segment.cross(new Vec3(0.0, 1.0, 0.0));
            if (side.lengthSqr() < 1.0E-6) {
                side = new Vec3(1.0, 0.0, 0.0);
            } else {
                side = side.normalize();
            }
        }
        Vec3 shift = side.scale(strip.center());
        side = side.scale(strip.halfSpan());
        Vec3 first = from.add(shift).add(side);
        Vec3 second = toward.add(shift).add(side);
        Vec3 third = toward.add(shift).subtract(side);
        Vec3 fourth = from.add(shift).subtract(side);
        ribbonVertex(consumer, matrix, packedLight, first, 0.0F, 0.0F, red, green, blue, alpha);
        ribbonVertex(consumer, matrix, packedLight, second, 1.0F, 0.0F, red, green, blue, alpha);
        ribbonVertex(consumer, matrix, packedLight, third, 1.0F, 1.0F, red, green, blue, alpha);
        ribbonVertex(consumer, matrix, packedLight, fourth, 0.0F, 1.0F, red, green, blue, alpha);
        ribbonVertex(consumer, matrix, packedLight, fourth, 0.0F, 1.0F, red, green, blue, alpha);
        ribbonVertex(consumer, matrix, packedLight, third, 1.0F, 1.0F, red, green, blue, alpha);
        ribbonVertex(consumer, matrix, packedLight, second, 1.0F, 0.0F, red, green, blue, alpha);
        ribbonVertex(consumer, matrix, packedLight, first, 0.0F, 0.0F, red, green, blue, alpha);
    }

    private static void ribbonVertex(VertexConsumer consumer, Matrix4f matrix, int packedLight, Vec3 point,
                                     float textureU, float textureV, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static final class Ribbon {
        private final List<Vec3> points = new ArrayList<>();
        private int lastFrame;

        private void remember(Vec3 here, int frame) {
            this.lastFrame = frame;
            if (!this.points.isEmpty()) {
                Vec3 last = this.points.get(this.points.size() - 1);
                if (last.distanceToSqr(here) > 16.0) {
                    this.points.clear();
                } else if (last.distanceToSqr(here) < 0.0025) {
                    return;
                }
            }
            this.points.add(here);
            if (this.points.size() > 48) {
                this.points.remove(0);
            }
        }
    }
}
