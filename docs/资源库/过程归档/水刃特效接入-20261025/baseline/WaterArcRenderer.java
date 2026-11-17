package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/** 用和碰撞相同的弧带采样画青蓝水带。不读取玩家当前朝向，也不播放动作。 */
public final class WaterArcRenderer extends RenderStateShard {
    private static final RenderType MESH = RenderType.create(
            "xueximod_water_arc",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            16384,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    private WaterArcRenderer() {
        super("xueximod_water_arc_state", () -> {
        }, () -> {
        });
    }

    public static void render(TechniqueHitEntity entity, float partialTicks, PoseStack pose, MultiBufferSource buffer) {
        ArcBandSnapshot snapshot = entity.arcBand();
        if (snapshot == null || snapshot.birthTick() < 0L) {
            return;
        }
        double age = (entity.level().getGameTime() - snapshot.birthTick()) + partialTicks;
        if (age < 0.0) {
            age = 0.0;
        }
        double active = Math.max(1, snapshot.activeTicks());
        double shown = Math.min(1.0, age / active);
        float alpha = 1.0F;
        if (age >= active) {
            double fade = 1.0 - (age - active) / Math.max(1.0, snapshot.tailTicks());
            alpha = (float) Math.max(0.0, fade);
        }
        if (alpha <= 0.01F || shown <= 1.0E-4) {
            return;
        }
        int pieces = Math.max(1, ProgressArcBand.segmentCount(snapshot, 0.0, shown));
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(MESH);
        ArcVec origin = snapshot.origin();
        ArcVec previousInner = bandPoint(snapshot, origin, 0.0, -1.0);
        ArcVec previousCore = bandPoint(snapshot, origin, 0.0, -0.72);
        ArcVec previousOuter = bandPoint(snapshot, origin, 0.0, 0.72);
        ArcVec previousRim = bandPoint(snapshot, origin, 0.0, 1.0);
        ArcVec crest = previousRim;
        for (int pieceIndex = 1; pieceIndex <= pieces; pieceIndex++) {
            double progress = pieceIndex == pieces ? shown : shown * pieceIndex / pieces;
            ArcVec inner = bandPoint(snapshot, origin, progress, -1.0);
            ArcVec core = bandPoint(snapshot, origin, progress, -0.72);
            ArcVec outer = bandPoint(snapshot, origin, progress, 0.72);
            ArcVec rim = bandPoint(snapshot, origin, progress, 1.0);
            quad(consumer, matrix, previousInner, previousCore, core, inner, 186, 236, 255, alpha * 0.9F);
            quad(consumer, matrix, previousCore, previousOuter, outer, core, 36, 150, 220, alpha);
            quad(consumer, matrix, previousOuter, previousRim, rim, outer, 186, 236, 255, alpha * 0.9F);
            if (rim.above() >= crest.above()) {
                crest = rim;
            }
            previousInner = inner;
            previousCore = core;
            previousOuter = outer;
            previousRim = rim;
        }
        ArcVec foamAcross = snapshot.crossAxis().scale(snapshot.halfWidth() * 0.35);
        ArcVec foamLift = snapshot.normalAxis().scale(snapshot.halfWidth() * 0.45);
        quad(consumer, matrix,
                crest.minus(foamAcross),
                crest.plus(foamLift),
                crest.plus(foamAcross),
                crest.minus(foamLift),
                255, 255, 255, alpha * 0.8F);
    }

    private static ArcVec bandPoint(ArcBandSnapshot snapshot, ArcVec origin, double progress, double widthFactor) {
        ArcVec radial = ProgressArcBand.direction(snapshot, progress);
        double distance = snapshot.radius() + snapshot.halfWidth() * widthFactor;
        return origin.plus(radial.scale(distance)).minus(origin);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, ArcVec first, ArcVec second, ArcVec third, ArcVec fourth,
                             int red, int green, int blue, float alpha) {
        int alphaByte = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        vertex(consumer, matrix, first, red, green, blue, alphaByte);
        vertex(consumer, matrix, second, red, green, blue, alphaByte);
        vertex(consumer, matrix, third, red, green, blue, alphaByte);
        vertex(consumer, matrix, fourth, red, green, blue, alphaByte);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, ArcVec point, int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, (float) point.east(), (float) point.above(), (float) point.south())
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
