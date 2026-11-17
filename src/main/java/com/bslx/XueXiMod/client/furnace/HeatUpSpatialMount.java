package com.bslx.XueXiMod.client.furnace;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.NetherBlueFlame;
import com.bslx.XueXiMod.client.furnace.FurnaceBodyVisual.Playback;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/**
 * 升温 v3.1 的已确认卡片。Blockbench 坐标先换成部件局部，再除以 16 交给玩家骨骼。
 * 正背面用完整矩形，侧面只用记录里的窄外侧矩形。
 */
public final class HeatUpSpatialMount {
    public static final float MODEL_TO_BLOCK = 1.0F / 16.0F;
    private static final float FRAME_EDGE = 96.0F;
    private static final float FRAME_SPAN = 112.0F;
    private static final float NARROW_INSET = 0.5F;

    private static final ResourceLocation LOOP_WARMTH = sheet("loop_warmth.png");
    private static final ResourceLocation LOOP_HAND_REAR = sheet("loop_hand_rear.png");
    private static final ResourceLocation LOOP_LEG_REAR = sheet("loop_leg_rear.png");
    private static final ResourceLocation LOOP_TORSO = sheet("loop_torso.png");
    private static final ResourceLocation LOOP_HAND_FRONT = sheet("loop_hand_front.png");
    private static final ResourceLocation LOOP_LEG_FRONT = sheet("loop_leg_front.png");
    private static final ResourceLocation LOOP_EMBERS = sheet("loop_embers.png");
    private static final ResourceLocation ENTRY_WARMTH = sheet("entry_warmth.png");
    private static final ResourceLocation ENTRY_HAND_REAR = sheet("entry_hand_rear.png");
    private static final ResourceLocation ENTRY_LEG_REAR = sheet("entry_leg_rear.png");
    private static final ResourceLocation ENTRY_TORSO = sheet("entry_torso.png");
    private static final ResourceLocation ENTRY_HAND_FRONT = sheet("entry_hand_front.png");
    private static final ResourceLocation ENTRY_LEG_FRONT = sheet("entry_leg_front.png");
    private static final ResourceLocation ENTRY_EMBERS = sheet("entry_embers.png");
    private static final ResourceLocation BLUE_LOOP_WARMTH = blueSheet("furnace_loop_warmth.png");
    private static final ResourceLocation BLUE_LOOP_HAND_REAR = blueSheet("furnace_loop_hand_rear.png");
    private static final ResourceLocation BLUE_LOOP_LEG_REAR = blueSheet("furnace_loop_leg_rear.png");
    private static final ResourceLocation BLUE_LOOP_TORSO = blueSheet("furnace_loop_torso.png");
    private static final ResourceLocation BLUE_LOOP_HAND_FRONT = blueSheet("furnace_loop_hand_front.png");
    private static final ResourceLocation BLUE_LOOP_LEG_FRONT = blueSheet("furnace_loop_leg_front.png");
    private static final ResourceLocation BLUE_LOOP_EMBERS = blueSheet("furnace_loop_embers.png");
    private static final ResourceLocation BLUE_ENTRY_WARMTH = blueSheet("furnace_entry_warmth.png");
    private static final ResourceLocation BLUE_ENTRY_HAND_REAR = blueSheet("furnace_entry_hand_rear.png");
    private static final ResourceLocation BLUE_ENTRY_LEG_REAR = blueSheet("furnace_entry_leg_rear.png");
    private static final ResourceLocation BLUE_ENTRY_TORSO = blueSheet("furnace_entry_torso.png");
    private static final ResourceLocation BLUE_ENTRY_HAND_FRONT = blueSheet("furnace_entry_hand_front.png");
    private static final ResourceLocation BLUE_ENTRY_LEG_FRONT = blueSheet("furnace_entry_leg_front.png");
    private static final ResourceLocation BLUE_ENTRY_EMBERS = blueSheet("furnace_entry_embers.png");

    private static final Card[] BODY = {
            card("torso", false, 36, 40, 59, 69, -4.0F, 2.6666667F, -2.16F, 4.0F, 12.666667F, -2.16F),
            card("torso", false, 36, 40, 59, 69, -4.0F, 2.6666667F, 2.16F, 4.0F, 12.666667F, 2.16F),
            card("torso", false, 35, 55, 39, 69, -4.1F, 7.6666665F, -1.0F, -4.1F, 12.666667F, 0.6666667F),
            card("torso", false, 56, 55, 60, 69, 4.1F, 7.6666665F, -1.0F, 4.1F, 12.666667F, 0.6666667F)
    };
    private static final Card[] RIGHT_ARM = {
            card("hand_rear", true, 20, 46, 38, 73, -4.3333335F, 2.6666667F, 2.18F, 2.0F, 12.0F, 2.18F),
            card("hand_front", true, 20, 46, 38, 73, -4.3333335F, 2.6666667F, -2.18F, 2.0F, 12.0F, -2.18F),
            card("hand_rear", true, 20, 46, 29, 73, -3.16F, 2.6666667F, -2.0F, -3.16F, 12.0F, 1.3333334F),
            card("warmth", true, 20, 46, 38, 73, -4.3333335F, 2.6666667F, 2.12F, 2.0F, 12.0F, 2.12F),
            card("embers", true, 18, 40, 30, 75, -5.0F, 0.6666667F, -2.42F, -0.6666667F, 12.666667F, -2.42F),
            card("embers", true, 18, 40, 30, 75, -5.0F, 0.6666667F, 2.42F, -0.6666667F, 12.666667F, 2.42F)
    };
    private static final Card[] LEFT_ARM = {
            card("hand_rear", true, 57, 46, 76, 73, -2.0F, 2.6666667F, 2.18F, 4.6666665F, 12.0F, 2.18F),
            card("hand_front", true, 57, 46, 76, 73, -2.0F, 2.6666667F, -2.18F, 4.6666665F, 12.0F, -2.18F),
            card("hand_rear", true, 66, 46, 75, 73, 3.16F, 2.6666667F, -1.6666666F, 3.16F, 12.0F, 1.6666666F),
            card("warmth", true, 57, 46, 76, 73, -2.0F, 2.6666667F, 2.12F, 4.6666665F, 12.0F, 2.12F),
            card("embers", true, 66, 40, 80, 75, 1.0F, 0.6666667F, -2.42F, 6.0F, 12.666667F, -2.42F),
            card("embers", true, 66, 40, 80, 75, 1.0F, 0.6666667F, 2.42F, 6.0F, 12.666667F, 2.42F)
    };
    private static final Card[] RIGHT_LEG = {
            card("leg_rear", false, 30, 69, 47, 107, -4.0F, 0.33333334F, 2.18F, 2.0F, 13.333333F, 2.18F),
            card("leg_front", false, 30, 69, 47, 107, -4.0F, 0.33333334F, -2.18F, 2.0F, 13.333333F, -2.18F),
            card("leg_rear", false, 30, 69, 38, 107, -2.16F, 0.33333334F, -1.6666666F, -2.16F, 13.333333F, 1.3333334F),
            card("warmth", false, 30, 69, 47, 107, -4.0F, 0.33333334F, 2.12F, 2.0F, 13.333333F, 2.12F),
            card("embers", false, 29, 68, 44, 108, -4.3333335F, 0.0F, -2.42F, 1.0F, 13.666667F, -2.42F),
            card("embers", false, 29, 68, 44, 108, -4.3333335F, 0.0F, 2.42F, 1.0F, 13.666667F, 2.42F)
    };
    private static final Card[] LEFT_LEG = {
            card("leg_rear", false, 48, 69, 65, 107, -2.0F, 0.33333334F, 2.18F, 4.0F, 13.333333F, 2.18F),
            card("leg_front", false, 48, 69, 65, 107, -2.0F, 0.33333334F, -2.18F, 4.0F, 13.333333F, -2.18F),
            card("leg_rear", false, 57, 69, 65, 107, 2.16F, 0.33333334F, -1.6666666F, 2.16F, 13.333333F, 1.3333334F),
            card("warmth", false, 48, 69, 65, 107, -2.0F, 0.33333334F, 2.12F, 4.0F, 13.333333F, 2.12F),
            card("embers", false, 51, 68, 67, 108, -1.0F, 0.0F, -2.42F, 4.6666665F, 13.666667F, -2.42F),
            card("embers", false, 51, 68, 67, 108, -1.0F, 0.0F, 2.42F, 4.6666665F, 13.666667F, 2.42F)
    };
    private static final Card[] RIGHT_HAND = handCards(RIGHT_ARM);
    private static final Card[] LEFT_HAND = handCards(LEFT_ARM);

    private HeatUpSpatialMount() {
    }

    public static void drawBody(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback) {
        draw(poseStack, bufferSource, playback, BODY, 0.0F);
    }

    public static void drawRightArm(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback,
                                    boolean narrowArm) {
        draw(poseStack, bufferSource, playback, RIGHT_ARM, narrowArm ? NARROW_INSET : 0.0F);
    }

    public static void drawLeftArm(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback,
                                   boolean narrowArm) {
        draw(poseStack, bufferSource, playback, LEFT_ARM, narrowArm ? -NARROW_INSET : 0.0F);
    }

    public static void drawRightLeg(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback) {
        draw(poseStack, bufferSource, playback, RIGHT_LEG, 0.0F);
    }

    public static void drawLeftLeg(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback) {
        draw(poseStack, bufferSource, playback, LEFT_LEG, 0.0F);
    }

    public static void drawRightHand(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback,
                                     boolean narrowArm) {
        draw(poseStack, bufferSource, playback, RIGHT_HAND, narrowArm ? NARROW_INSET : 0.0F);
    }

    public static void drawLeftHand(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback,
                                    boolean narrowArm) {
        draw(poseStack, bufferSource, playback, LEFT_HAND, narrowArm ? -NARROW_INSET : 0.0F);
    }

    private static void draw(PoseStack poseStack, MultiBufferSource bufferSource, Playback playback, Card[] cards,
                             float insetX) {
        if (playback == null || !playback.present() || cards == null) {
            return;
        }
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int frameIndex = playback.frameIndex();
        int frameCount = playback.frameCount();
        boolean entryActive = playback.entryActive();
        String boundLayer = null;
        VertexConsumer consumer = null;
        for (Card card : cards) {
            if (!card.layer.equals(boundLayer)) {
                boundLayer = card.layer;
                consumer = FurnaceBodyVisual.emissive(bufferSource,
                        chosen(texture(card.layer, entryActive), blueTexture(card.layer, entryActive), playback));
            }
            paint(consumer, matrix, normal, card, frameIndex, frameCount, insetX);
        }
    }

    private static void paint(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, Card card,
                              int frameIndex, int frameCount, float insetX) {
        float lowerX = (Math.min(card.firstX, card.secondX) + insetX) * MODEL_TO_BLOCK;
        float upperX = (Math.max(card.firstX, card.secondX) + insetX) * MODEL_TO_BLOCK;
        float lowerY = Math.min(card.firstY, card.secondY) * MODEL_TO_BLOCK;
        float upperY = Math.max(card.firstY, card.secondY) * MODEL_TO_BLOCK;
        float lowerZ = Math.min(card.firstZ, card.secondZ) * MODEL_TO_BLOCK;
        float upperZ = Math.max(card.firstZ, card.secondZ) * MODEL_TO_BLOCK;
        float leftU = card.pixelLeft / FRAME_EDGE;
        float rightU = (card.pixelRight + 1) / FRAME_EDGE;
        float topV = (frameIndex + card.pixelTop / FRAME_SPAN) / frameCount;
        float bottomV = (frameIndex + (card.pixelBottom + 1) / FRAME_SPAN) / frameCount;
        if (card.firstX == card.secondX) {
            boolean positive = card.firstX > 0.0F;
            float planeX = positive ? upperX : lowerX;
            quad(consumer, matrix, normal,
                    planeX, lowerY, lowerZ, planeX, lowerY, upperZ, planeX, upperY, upperZ, planeX, upperY, lowerZ,
                    leftU, bottomV, rightU, bottomV, rightU, topV, leftU, topV,
                    positive ? 1.0F : -1.0F, 0.0F, 0.0F);
            quad(consumer, matrix, normal,
                    planeX, lowerY, upperZ, planeX, lowerY, lowerZ, planeX, upperY, lowerZ, planeX, upperY, upperZ,
                    rightU, bottomV, leftU, bottomV, leftU, topV, rightU, topV,
                    positive ? -1.0F : 1.0F, 0.0F, 0.0F);
            return;
        }
        boolean front = card.firstZ < 0.0F;
        float planeZ = front ? lowerZ : upperZ;
        quad(consumer, matrix, normal,
                lowerX, lowerY, planeZ, upperX, lowerY, planeZ, upperX, upperY, planeZ, lowerX, upperY, planeZ,
                leftU, bottomV, rightU, bottomV, rightU, topV, leftU, topV,
                0.0F, 0.0F, front ? -1.0F : 1.0F);
        quad(consumer, matrix, normal,
                upperX, lowerY, planeZ, lowerX, lowerY, planeZ, lowerX, upperY, planeZ, upperX, upperY, planeZ,
                rightU, bottomV, leftU, bottomV, leftU, topV, rightU, topV,
                0.0F, 0.0F, front ? 1.0F : -1.0F);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                             float firstX, float firstY, float firstZ,
                             float secondX, float secondY, float secondZ,
                             float thirdX, float thirdY, float thirdZ,
                             float fourthX, float fourthY, float fourthZ,
                             float firstU, float firstV, float secondU, float secondV,
                             float thirdU, float thirdV, float fourthU, float fourthV,
                             float normalX, float normalY, float normalZ) {
        vertex(consumer, matrix, normal, firstX, firstY, firstZ, firstU, firstV, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, secondX, secondY, secondZ, secondU, secondV, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, thirdX, thirdY, thirdZ, thirdU, thirdV, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, fourthX, fourthY, fourthZ, fourthU, fourthV, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                               float pointX, float pointY, float pointZ, float textureU, float textureV,
                               float normalX, float normalY, float normalZ) {
        FurnaceBodyVisual.emissiveVertex(consumer, matrix, normal, pointX, pointY, pointZ,
                textureU, textureV, normalX, normalY, normalZ);
    }

    private static ResourceLocation texture(String layer, boolean entryActive) {
        if ("warmth".equals(layer)) {
            return entryActive ? ENTRY_WARMTH : LOOP_WARMTH;
        }
        if ("hand_rear".equals(layer)) {
            return entryActive ? ENTRY_HAND_REAR : LOOP_HAND_REAR;
        }
        if ("leg_rear".equals(layer)) {
            return entryActive ? ENTRY_LEG_REAR : LOOP_LEG_REAR;
        }
        if ("torso".equals(layer)) {
            return entryActive ? ENTRY_TORSO : LOOP_TORSO;
        }
        if ("hand_front".equals(layer)) {
            return entryActive ? ENTRY_HAND_FRONT : LOOP_HAND_FRONT;
        }
        if ("leg_front".equals(layer)) {
            return entryActive ? ENTRY_LEG_FRONT : LOOP_LEG_FRONT;
        }
        return entryActive ? ENTRY_EMBERS : LOOP_EMBERS;
    }

    private static ResourceLocation blueTexture(String layer, boolean entryActive) {
        if ("warmth".equals(layer)) {
            return entryActive ? BLUE_ENTRY_WARMTH : BLUE_LOOP_WARMTH;
        }
        if ("hand_rear".equals(layer)) {
            return entryActive ? BLUE_ENTRY_HAND_REAR : BLUE_LOOP_HAND_REAR;
        }
        if ("leg_rear".equals(layer)) {
            return entryActive ? BLUE_ENTRY_LEG_REAR : BLUE_LOOP_LEG_REAR;
        }
        if ("torso".equals(layer)) {
            return entryActive ? BLUE_ENTRY_TORSO : BLUE_LOOP_TORSO;
        }
        if ("hand_front".equals(layer)) {
            return entryActive ? BLUE_ENTRY_HAND_FRONT : BLUE_LOOP_HAND_FRONT;
        }
        if ("leg_front".equals(layer)) {
            return entryActive ? BLUE_ENTRY_LEG_FRONT : BLUE_LOOP_LEG_FRONT;
        }
        return entryActive ? BLUE_ENTRY_EMBERS : BLUE_LOOP_EMBERS;
    }

    private static Card[] handCards(Card[] armCards) {
        int count = 0;
        for (Card card : armCards) {
            if (card.firstPerson) {
                count++;
            }
        }
        Card[] handCards = new Card[count];
        int cursor = 0;
        for (Card card : armCards) {
            if (card.firstPerson) {
                handCards[cursor++] = card;
            }
        }
        return handCards;
    }

    private static Card card(String layer, boolean firstPerson, int pixelLeft, int pixelTop, int pixelRight,
                             int pixelBottom, float firstX, float firstY, float firstZ,
                             float secondX, float secondY, float secondZ) {
        return new Card(layer, firstPerson, pixelLeft, pixelTop, pixelRight, pixelBottom,
                firstX, firstY, firstZ, secondX, secondY, secondZ);
    }

    private static ResourceLocation sheet(String fileName) {
        return new ResourceLocation(XueXiMod.MOD_ID, "textures/effect/furnace_v3/" + fileName);
    }

    private static ResourceLocation blueSheet(String fileName) {
        return NetherBlueFlame.sheet(fileName);
    }

    private static ResourceLocation chosen(ResourceLocation ordinary, ResourceLocation blue, Playback playback) {
        if (playback != null && playback.gloom()) {
            return blue;
        }
        return NetherBlueFlame.current(ordinary, blue);
    }

    private static final class Card {
        private final String layer;
        private final boolean firstPerson;
        private final int pixelLeft;
        private final int pixelTop;
        private final int pixelRight;
        private final int pixelBottom;
        private final float firstX;
        private final float firstY;
        private final float firstZ;
        private final float secondX;
        private final float secondY;
        private final float secondZ;

        private Card(String layer, boolean firstPerson, int pixelLeft, int pixelTop, int pixelRight, int pixelBottom,
                     float firstX, float firstY, float firstZ, float secondX, float secondY, float secondZ) {
            this.layer = layer;
            this.firstPerson = firstPerson;
            this.pixelLeft = pixelLeft;
            this.pixelTop = pixelTop;
            this.pixelRight = pixelRight;
            this.pixelBottom = pixelBottom;
            this.firstX = firstX;
            this.firstY = firstY;
            this.firstZ = firstZ;
            this.secondX = secondX;
            this.secondY = secondY;
            this.secondZ = secondZ;
        }
    }
}
