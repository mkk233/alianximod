package com.bslx.XueXiMod.client.furnace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceMobEffect;
import com.bslx.XueXiMod.visual.fire.FireVisualLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/**
 * 升温 v3.1 与燃烧的客户端取样。升温卡片在 HeatUpSpatialMount，燃烧网格在 BurnSpatialMount。
 * 只读炉心效果，不改服务端状态。冷却用 v4.1 的前后蒸汽和贴地冲击波。帧仍按 8×60 ms 入场、20×80 ms 循环取样。
 */
public final class FurnaceBodyVisual {
    public static final int ENTRY_FRAMES = 8;
    public static final int ENTRY_MILLIS = 60;
    public static final int LOOP_FRAMES = 20;
    public static final int LOOP_MILLIS = 80;
    public static final int FRAME_EDGE = 96;
    public static final int FRAME_SPAN = 112;

    private static final long ENTRY_SPAN = (long) ENTRY_FRAMES * ENTRY_MILLIS;
    private static final float MODEL_UNIT = 1.0F / 16.0F;
    private static final float COOL_CARD_LEFT = -21.33333F * MODEL_UNIT;
    private static final float COOL_CARD_RIGHT = 21.33333F * MODEL_UNIT;
    private static final float COOL_CARD_BOTTOM = -5.33333F * MODEL_UNIT;
    private static final float COOL_CARD_TOP = 37.33333F * MODEL_UNIT;
    private static final float COOL_REAR_DEPTH = -2.5F * MODEL_UNIT;
    private static final float COOL_FRONT_DEPTH = 2.7F * MODEL_UNIT;
    private static final float COOL_FRAGMENT_DEPTH = 2.9F * MODEL_UNIT;
    private static final float COOL_WAVE_HALF = 32.0F * MODEL_UNIT;
    private static final float COOL_WAVE_HEIGHT = 1.2F * MODEL_UNIT;
    private static final List<CoolLayer> COOL_LAYERS = List.of(
            new CoolLayer(sheet(FireVisualLayout.COOL_REAR_SHEET), COOL_REAR_DEPTH),
            new CoolLayer(sheet(FireVisualLayout.COOL_FRONT_SHEET), COOL_FRONT_DEPTH),
            new CoolLayer(sheet(FireVisualLayout.COOL_FRAGMENT_SHEET), COOL_FRAGMENT_DEPTH));

    private static final ResourceLocation COOL_WAVE = sheet(FireVisualLayout.COOL_WAVE_SHEET);

    private static final Map<Integer, CueMark> CUE_MARKS = new HashMap<>();
    private static final Playback ABSENT = new Playback(false, false, -1);

    private FurnaceBodyVisual() {
    }

    public static void noteCue(int entityId, int cueKind) {
        if (cueKind == 0) {
            return;
        }
        CUE_MARKS.put(entityId, new CueMark(cueKind, System.currentTimeMillis()));
    }

    public static void forget() {
        CUE_MARKS.clear();
    }

    public static boolean hidesBody(Player player) {
        if (player == null || player.isInvisible()) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == player && minecraft.options.getCameraType().isFirstPerson();
    }

    public static Playback playback(Player player, float partialTick) {
        if (player == null) {
            return ABSENT;
        }
        CueMark mark = CUE_MARKS.get(player.getId());
        if (mark != null && mark.cueKind == FireVisualLayout.COOL_ONCE) {
            return ABSENT;
        }
        MobEffectInstance effect = player.getEffect(FurnaceMobEffect.effect());
        if (!FurnaceMobEffect.isFurnace(effect)) {
            return ABSENT;
        }
        boolean overdrive = effect.getAmplifier() > 0;
        int expectedKind = overdrive ? FireVisualLayout.OVERDRIVE_LOOP : FireVisualLayout.HEAT_LOOP;
        if (mark != null && mark.cueKind == expectedKind) {
            return fromAnchor(overdrive, System.currentTimeMillis() - mark.anchorMillis);
        }
        float lived = player.tickCount + partialTick;
        int frameIndex = (int) (((long) (lived * 50.0F) / LOOP_MILLIS) % LOOP_FRAMES);
        if (frameIndex < 0) {
            frameIndex += LOOP_FRAMES;
        }
        return new Playback(overdrive, false, frameIndex);
    }

    public static void paintCool(ClientLevel clientLevel, PoseStack poseStack, Camera camera,
                                 MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn) {
        // 主动冷却的 v4.1 蒸汽与贴地冲击波。升温和燃烧不走这里。
        Minecraft minecraft = Minecraft.getInstance();
        paintCool(clientLevel, poseStack, camera, buffers, drawn, minecraft.getFrameTime(), minecraft.player);
    }

    private static void paintCool(ClientLevel clientLevel, PoseStack poseStack, Camera camera,
                                  MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                  float partialTick, Player localPlayer) {
        if (clientLevel == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Player player : clientLevel.players()) {
            CueMark mark = CUE_MARKS.get(player.getId());
            if (mark != null && mark.cueKind == FireVisualLayout.COOL_ONCE) {
                int coolIndex = FireVisualLayout.coolFrame(now - mark.anchorMillis);
                if (coolIndex < 0) {
                    CUE_MARKS.remove(player.getId());
                } else {
                    float sampledTick = player == localPlayer ? partialTick : 1.0F;
                    paintCoolBurst(poseStack, buffers, drawn, player, sampledTick, coolIndex);
                }
                continue;
            }
            MobEffectInstance effect = player.getEffect(FurnaceMobEffect.effect());
            if (mark != null && !FurnaceMobEffect.isFurnace(effect)) {
                CUE_MARKS.remove(player.getId());
            }
        }
    }

    public static boolean narrowArm(AbstractClientPlayer player) {
        return player != null && "slim".equals(player.getModelName());
    }

    public static VertexConsumer emissive(MultiBufferSource bufferSource, ResourceLocation texture) {
        return bufferSource.getBuffer(RenderType.entityTranslucentEmissive(texture));
    }

    private static Playback fromAnchor(boolean overdrive, long elapsed) {
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        if (elapsed < ENTRY_SPAN) {
            int frameIndex = (int) (elapsed / ENTRY_MILLIS);
            if (frameIndex >= ENTRY_FRAMES) {
                frameIndex = ENTRY_FRAMES - 1;
            }
            return new Playback(overdrive, true, frameIndex);
        }
        long loopElapsed = elapsed - ENTRY_SPAN;
        int frameIndex = (int) ((loopElapsed / LOOP_MILLIS) % LOOP_FRAMES);
        return new Playback(overdrive, false, frameIndex);
    }

    private static VertexConsumer emissive(MultiBufferSource.BufferSource buffers, ArrayList<RenderType> drawn,
                                           ResourceLocation texture) {
        RenderType renderType = RenderType.entityTranslucentEmissive(texture);
        if (!drawn.contains(renderType)) {
            drawn.add(renderType);
        }
        return buffers.getBuffer(renderType);
    }

    private static void paintCoolBurst(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                       ArrayList<RenderType> drawn, Player player, float partialTick,
                                       int frameIndex) {
        double footX = Mth.lerp(partialTick, player.xo, player.getX());
        double footY = Mth.lerp(partialTick, player.yo, player.getY());
        double footZ = Mth.lerp(partialTick, player.zo, player.getZ());
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        poseStack.pushPose();
        poseStack.translate(footX, footY, footZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        for (CoolLayer layer : COOL_LAYERS) {
            VertexConsumer consumer = emissive(buffers, drawn, layer.texture);
            paintFacing(consumer, matrix, normal, layer.depth, frameIndex);
        }
        VertexConsumer waveConsumer = emissive(buffers, drawn, COOL_WAVE);
        paintWave(waveConsumer, matrix, normal, frameIndex);
        poseStack.popPose();
    }

    private static void paintFacing(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float depth,
                                    int frameIndex) {
        float topV = frameIndex / (float) FireVisualLayout.FURNACE_FRAMES;
        float bottomV = (frameIndex + 1) / (float) FireVisualLayout.FURNACE_FRAMES;
        quad(consumer, matrix, normal,
                COOL_CARD_LEFT, COOL_CARD_BOTTOM, depth,
                COOL_CARD_RIGHT, COOL_CARD_BOTTOM, depth,
                COOL_CARD_RIGHT, COOL_CARD_TOP, depth,
                COOL_CARD_LEFT, COOL_CARD_TOP, depth,
                0.0F, bottomV, 1.0F, bottomV, 1.0F, topV, 0.0F, topV, 0.0F, 0.0F, 1.0F);
        quad(consumer, matrix, normal,
                COOL_CARD_LEFT, COOL_CARD_BOTTOM, depth,
                COOL_CARD_LEFT, COOL_CARD_TOP, depth,
                COOL_CARD_RIGHT, COOL_CARD_TOP, depth,
                COOL_CARD_RIGHT, COOL_CARD_BOTTOM, depth,
                0.0F, bottomV, 0.0F, topV, 1.0F, topV, 1.0F, bottomV, 0.0F, 0.0F, -1.0F);
    }

    private static void paintWave(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int frameIndex) {
        float topV = frameIndex / (float) FireVisualLayout.FURNACE_FRAMES;
        float bottomV = (frameIndex + 1) / (float) FireVisualLayout.FURNACE_FRAMES;
        quad(consumer, matrix, normal,
                -COOL_WAVE_HALF, COOL_WAVE_HEIGHT, -COOL_WAVE_HALF,
                COOL_WAVE_HALF, COOL_WAVE_HEIGHT, -COOL_WAVE_HALF,
                COOL_WAVE_HALF, COOL_WAVE_HEIGHT, COOL_WAVE_HALF,
                -COOL_WAVE_HALF, COOL_WAVE_HEIGHT, COOL_WAVE_HALF,
                0.0F, bottomV, 1.0F, bottomV, 1.0F, topV, 0.0F, topV, 0.0F, 1.0F, 0.0F);
        quad(consumer, matrix, normal,
                -COOL_WAVE_HALF, COOL_WAVE_HEIGHT, -COOL_WAVE_HALF,
                -COOL_WAVE_HALF, COOL_WAVE_HEIGHT, COOL_WAVE_HALF,
                COOL_WAVE_HALF, COOL_WAVE_HEIGHT, COOL_WAVE_HALF,
                COOL_WAVE_HALF, COOL_WAVE_HEIGHT, -COOL_WAVE_HALF,
                0.0F, bottomV, 0.0F, topV, 1.0F, topV, 1.0F, bottomV, 0.0F, -1.0F, 0.0F);
    }

    static void emissiveVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float pointX, float pointY, float pointZ, float textureU, float textureV,
                               float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(255, 255, 255, 255)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                             float firstX, float firstY, float firstZ,
                             float secondX, float secondY, float secondZ,
                             float thirdX, float thirdY, float thirdZ,
                             float fourthX, float fourthY, float fourthZ,
                             float firstU, float firstV, float secondU, float secondV,
                             float thirdU, float thirdV, float fourthU, float fourthV,
                             float normalX, float normalY, float normalZ) {
        emissiveVertex(consumer, matrix, normal, firstX, firstY, firstZ, firstU, firstV, normalX, normalY, normalZ);
        emissiveVertex(consumer, matrix, normal, secondX, secondY, secondZ, secondU, secondV, normalX, normalY, normalZ);
        emissiveVertex(consumer, matrix, normal, thirdX, thirdY, thirdZ, thirdU, thirdV, normalX, normalY, normalZ);
        emissiveVertex(consumer, matrix, normal, fourthX, fourthY, fourthZ, fourthU, fourthV, normalX, normalY, normalZ);
    }

    private static ResourceLocation sheet(String path) {
        return new ResourceLocation(XueXiMod.MOD_ID, path);
    }

    public static final class Playback {
        private final boolean overdrive;
        private final boolean entryActive;
        private final int frameIndex;
        private final boolean gloom;

        Playback(boolean overdrive, boolean entryActive, int frameIndex) {
            this(overdrive, entryActive, frameIndex, false);
        }

        Playback(boolean overdrive, boolean entryActive, int frameIndex, boolean gloom) {
            this.overdrive = overdrive;
            this.entryActive = entryActive;
            this.frameIndex = frameIndex;
            this.gloom = gloom;
        }

        public boolean present() {
            return this.frameIndex >= 0;
        }

        public boolean overdrive() {
            return this.overdrive;
        }

        public boolean gloom() {
            return this.gloom;
        }

        public Playback withGloom(boolean gloomActive) {
            if (this.gloom == gloomActive) {
                return this;
            }
            return new Playback(this.overdrive, this.entryActive, this.frameIndex, gloomActive);
        }

        public boolean entryActive() {
            return this.entryActive;
        }

        public int frameIndex() {
            return this.frameIndex;
        }

        public int frameCount() {
            return this.entryActive ? ENTRY_FRAMES : LOOP_FRAMES;
        }
    }

    private static final class CueMark {
        private final int cueKind;
        private final long anchorMillis;

        private CueMark(int cueKind, long anchorMillis) {
            this.cueKind = cueKind;
            this.anchorMillis = anchorMillis;
        }
    }

    private static final class CoolLayer {
        private final ResourceLocation texture;
        private final float depth;

        private CoolLayer(ResourceLocation texture, float depth) {
            this.texture = texture;
            this.depth = depth;
        }
    }
}
