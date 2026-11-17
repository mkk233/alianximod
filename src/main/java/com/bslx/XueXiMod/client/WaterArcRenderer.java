package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.client.WaterArcSheetMap.SheetCorner;
import com.bslx.XueXiMod.shu.technique.release.entity.CohortVisual;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** 沿冻结弧带贴弧形水斩。不画旧程序色带，不读相机，也不延长攻击。 */
public final class WaterArcRenderer extends RenderStateShard {
    private static final ResourceLocation SHEET = ResourceLocation.fromNamespaceAndPath(
            "xueximod", "textures/entity/xueximod/water_arc_slash.png");
    private static final RenderType SHEET_MESH = RenderType.create(
            "xueximod_water_arc_sheet",
            DefaultVertexFormat.POSITION_COLOR_TEX,
            VertexFormat.Mode.QUADS,
            65536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_TEX_SHADER)
                    .setTextureState(new TextureStateShard(SHEET, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
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
        if (age < 0.0 || WaterArcSheetMap.frameIndex(age, snapshot.activeTicks(), snapshot.tailTicks()) < 0) {
            return;
        }
        if (WaterArcSheetMap.reveal(age, snapshot.activeTicks()) <= 1.0E-8) {
            return;
        }
        double fade = WaterArcSheetMap.fadeAlpha(age, snapshot.activeTicks(), snapshot.tailTicks());
        if (fade <= 0.01) {
            return;
        }
        int alphaByte = Math.max(0, Math.min(255, (int) Math.round(fade * 255.0)));
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(SHEET_MESH);
        CohortVisual visual = entity.cohortVisual();
        WaterArcSheetMap.emit(snapshot, age, (startInner, startOuter, endOuter, endInner) -> {
            submit(consumer, matrix, startInner, alphaByte);
            submit(consumer, matrix, startOuter, alphaByte);
            submit(consumer, matrix, endOuter, alphaByte);
            submit(consumer, matrix, endInner, alphaByte);
        }, visual);
    }

    private static void submit(VertexConsumer consumer, Matrix4f matrix, SheetCorner corner, int alphaByte) {
        consumer.vertex(matrix, (float) corner.eastOffset(), (float) corner.aboveOffset(), (float) corner.southOffset())
                .color(255, 255, 255, alphaByte)
                .uv((float) corner.textureU(), (float) corner.textureV())
                .endVertex();
    }
}
