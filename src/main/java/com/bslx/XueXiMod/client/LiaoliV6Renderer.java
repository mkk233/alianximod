package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.client.NetherBlueFlame;
import com.bslx.XueXiMod.client.action.ActionRenderQueries;
import com.bslx.XueXiMod.client.action.RetargetedPose;
import com.bslx.XueXiMod.client.action.VanillaEuler;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.visual.liaoli.LiaoliV6Layout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

/** 燎里 v6 的空间透明片。剑阵跟着已绑定的施法者。后环改由尾迹从剑尖播完。 */
public final class LiaoliV6Renderer {
    private static final ResourceLocation SWORD_UNLIT = new ResourceLocation(XueXiMod.MOD_ID, LiaoliV6Layout.SWORD_UNLIT);
    private static final ResourceLocation SWORD_HOT = new ResourceLocation(XueXiMod.MOD_ID, LiaoliV6Layout.SWORD_HOT);
    private static final ResourceLocation BACK_IGNITE = new ResourceLocation(XueXiMod.MOD_ID, LiaoliV6Layout.BACK_IGNITE);
    private static final ResourceLocation BACK_FLOW = new ResourceLocation(XueXiMod.MOD_ID, LiaoliV6Layout.BACK_FLOW);
    private static final ResourceLocation THRUST_STREAM = new ResourceLocation(XueXiMod.MOD_ID, LiaoliV6Layout.THRUST_STREAM);
    private static final ResourceLocation REAR_RING = new ResourceLocation(XueXiMod.MOD_ID, LiaoliV6Layout.REAR_RING);
    private static final ResourceLocation BLUE_BACK_IGNITE = NetherBlueFlame.sheet("liaoli_back_ignite.png");
    private static final ResourceLocation BLUE_BACK_FLOW = NetherBlueFlame.sheet("liaoli_back_flow.png");
    private static final ResourceLocation BLUE_THRUST_STREAM = NetherBlueFlame.sheet("liaoli_thrust_backflow.png");
    private static final ResourceLocation BLUE_REAR_RING = NetherBlueFlame.sheet("liaoli_rear_ring.png");

    private LiaoliV6Renderer() {
    }

    public static void render(TechniqueHitEntity entity, float partialTicks, PoseStack pose, MultiBufferSource buffer) {
        pose.pushPose();
        Player caster = boundPlayer(entity);
        if (caster != null) {
            alignToCaster(pose, entity, caster, partialTicks);
        }
        float lookTurn = lookTurn(entity, caster, partialTicks);
        Basis lookBasis = Basis.fromYaw(lookTurn);
        BackPose backPose = backPose(caster, partialTicks, lookBasis);
        double lived = entity.tickCount + partialTicks;
        Matrix4f matrix = pose.last().pose();
        double sampleX;
        double sampleY;
        double sampleZ;
        if (caster != null) {
            Vec3 eye = caster.getEyePosition(partialTicks);
            sampleX = eye.x;
            sampleY = eye.y;
            sampleZ = eye.z;
        } else {
            sampleX = Mth.lerp(partialTicks, entity.xo, entity.getX());
            sampleY = Mth.lerp(partialTicks, entity.yo, entity.getY());
            sampleZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
        }
        boolean previousGloom = NetherBlueFlame.open(entity.level(), sampleX, sampleY, sampleZ);
        long ringBirth = entity.rearRingBirth();
        if (ringBirth >= 0L && caster != null) {
            LiaoliRingTrail.remember(caster.getUUID(), ringBirth);
        }
        try {
            drawBackFlames(buffer, matrix, backPose, lived);
            drawBackSwords(buffer, matrix, backPose, lived);
            drawThrust(buffer, matrix, lookBasis, lived);
        } finally {
            NetherBlueFlame.close(previousGloom);
        }
        pose.popPose();
    }

    /** 渲染原点改到施法者这一帧的眼睛，不用滞后的命中体坐标。 */
    private static void alignToCaster(PoseStack pose, TechniqueHitEntity entity, Player caster, float partialTicks) {
        double hereX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double hereY = Mth.lerp(partialTicks, entity.yo, entity.getY());
        double hereZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());
        Vec3 eye = caster.getEyePosition(partialTicks);
        pose.translate(eye.x - hereX, eye.y - hereY, eye.z - hereZ);
    }

    private static float lookTurn(TechniqueHitEntity entity, Player caster, float partialTicks) {
        float yawDegrees = caster == null
                ? Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot())
                : Mth.rotLerp(partialTicks, caster.yRotO, caster.getYRot());
        return yawDegrees * ((float) Math.PI / 180.0F);
    }

    /** 在已经对准施法者眼睛的姿态上画三道后环。进度不折回。 */
    static void paintRearRings(MultiBufferSource buffer, Matrix4f matrix, float lookYawRadians, double ageTicks) {
        drawRings(buffer, matrix, Basis.fromYaw(lookYawRadians), ageTicks);
    }

    private static void drawRings(MultiBufferSource buffer, Matrix4f matrix, Basis basis, double lived) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(
                NetherBlueFlame.current(REAR_RING, BLUE_REAR_RING)));
        int frame = LiaoliV6Layout.loopFrame(lived, LiaoliV6Layout.RING_FRAMES);
        float nearV = frame / (float) LiaoliV6Layout.RING_FRAMES;
        float farV = (frame + 1) / (float) LiaoliV6Layout.RING_FRAMES;
        int ringIndex = 0;
        while (ringIndex < LiaoliV6Layout.RING_COUNT) {
            double localAge = LiaoliV6Layout.ringLocalAge(lived, ringIndex);
            if (localAge < 0.0d || localAge >= LiaoliV6Layout.RING_SPAN) {
                ringIndex += 1;
                continue;
            }
            double portion = LiaoliV6Layout.ringPortion(localAge);
            double opacity = LiaoliV6Layout.ringAlpha(portion);
            if (opacity <= 0.0d) {
                ringIndex += 1;
                continue;
            }
            LiaoliV6Layout.Mark center = LiaoliV6Layout.ringCenter(portion);
            double radius = LiaoliV6Layout.ringRadius(portion) * LiaoliV6Layout.RING_QUAD_SCALE;
            int alpha = (int) Math.round(opacity * 255.0d);
            if (alpha <= 0) {
                ringIndex += 1;
                continue;
            }
            WorldPoint origin = basis.place(center);
            WorldPoint lateral = basis.lateralStep(radius);
            WorldPoint upward = new WorldPoint(0.0d, radius, 0.0d);
            ribbon(consumer, matrix,
                    origin.add(lateral).add(upward),
                    origin.add(lateral.negate()).add(upward),
                    origin.add(lateral.negate()).add(upward.negate()),
                    origin.add(lateral).add(upward.negate()),
                    0.0f, 1.0f, nearV, farV, alpha);
            ringIndex += 1;
        }
    }

    private static void drawBackFlames(MultiBufferSource buffer, Matrix4f matrix, BackPose backPose, double lived) {
        boolean igniting = LiaoliV6Layout.igniting(lived);
        ResourceLocation texture = NetherBlueFlame.current(
                igniting ? BACK_IGNITE : BACK_FLOW, igniting ? BLUE_BACK_IGNITE : BLUE_BACK_FLOW);
        int frameCount = igniting ? LiaoliV6Layout.IGNITE_FRAMES : LiaoliV6Layout.FLOW_FRAMES;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
        WorldPoint forwardHint = backPose.basis.forwardStep(1.0d);
        LiaoliV6Layout.Mark start = LiaoliV6Layout.flameStart(backPose.pitchDegrees, backPose.diveAmount, backPose.eyeAboveFeet);
        int swordIndex = 0;
        while (swordIndex < LiaoliV6Layout.SWORD_COUNT) {
            int frame = igniting
                    ? LiaoliV6Layout.igniteFrame(lived)
                    : LiaoliV6Layout.flowFrame(lived, LiaoliV6Layout.mirrorPair(swordIndex));
            float nearV = frame / (float) frameCount;
            float farV = (frame + 1) / (float) frameCount;
            if (LiaoliV6Layout.flipFlame(swordIndex)) {
                float swapped = nearV;
                nearV = farV;
                farV = swapped;
            }
            WorldPoint from = backPose.basis.place(start);
            WorldPoint toward = backPose.basis.place(LiaoliV6Layout.flameEnd(
                    swordIndex, backPose.pitchDegrees, backPose.diveAmount, backPose.eyeAboveFeet));
            ribbonAlong(consumer, matrix, from, toward, forwardHint, LiaoliV6Layout.flameHalfWidth(from.distance(toward)),
                    0.0f, 1.0f, nearV, farV, 255);
            swordIndex += 1;
        }
    }

    private static void drawBackSwords(MultiBufferSource buffer, Matrix4f matrix, BackPose backPose, double lived) {
        VertexConsumer unlit = buffer.getBuffer(RenderType.entityTranslucentEmissive(SWORD_UNLIT));
        VertexConsumer hot = buffer.getBuffer(RenderType.entityTranslucentEmissive(SWORD_HOT));
        WorldPoint forwardHint = backPose.basis.forwardStep(1.0d);
        double reach = LiaoliV6Layout.hotReach(lived);
        int swordIndex = 0;
        while (swordIndex < LiaoliV6Layout.SWORD_COUNT) {
            LiaoliV6Layout.Mark hilt = LiaoliV6Layout.swordHilt(
                    swordIndex, backPose.pitchDegrees, backPose.diveAmount, backPose.eyeAboveFeet);
            LiaoliV6Layout.Mark tip = LiaoliV6Layout.swordTip(
                    swordIndex, backPose.pitchDegrees, backPose.diveAmount, backPose.eyeAboveFeet);
            WorldPoint hiltPoint = backPose.basis.place(hilt);
            double halfWidth = LiaoliV6Layout.swordHalfWidth(hiltPoint.distance(backPose.basis.place(tip)));
            boolean flip = LiaoliV6Layout.flipFlame(swordIndex);
            if (reach <= 0.0d) {
                drawBlade(unlit, matrix, backPose.basis, forwardHint, hilt, tip, halfWidth, 0.0f, 1.0f, flip);
            } else if (reach >= 1.0d) {
                drawBlade(hot, matrix, backPose.basis, forwardHint, hilt, tip, halfWidth, 0.0f, 1.0f, flip);
            } else {
                LiaoliV6Layout.Mark joint = LiaoliV6Layout.between(hilt, tip, reach);
                drawBlade(hot, matrix, backPose.basis, forwardHint, hilt, joint, halfWidth, 0.0f, (float) reach, flip);
                drawBlade(unlit, matrix, backPose.basis, forwardHint, joint, tip, halfWidth, (float) reach, 1.0f, flip);
            }
            swordIndex += 1;
        }
    }

    private static void drawBlade(VertexConsumer consumer, Matrix4f matrix, Basis basis, WorldPoint forwardHint,
                                  LiaoliV6Layout.Mark fromMark, LiaoliV6Layout.Mark toMark, double halfWidth,
                                  float nearU, float farU, boolean flip) {
        WorldPoint from = basis.place(fromMark);
        WorldPoint toward = basis.place(toMark);
        float nearV = flip ? 1.0f : 0.0f;
        float farV = flip ? 0.0f : 1.0f;
        ribbonAlong(consumer, matrix, from, toward, forwardHint, halfWidth, nearU, farU, nearV, farV, 255);
    }

    private static void drawThrust(MultiBufferSource buffer, Matrix4f matrix, Basis basis, double lived) {
        WorldPoint upward = new WorldPoint(0.0d, 1.0d, 0.0d);
        WorldPoint lateral = basis.lateralStep(1.0d);
        WorldPoint hilt = basis.place(LiaoliV6Layout.thrustHilt());
        WorldPoint tip = basis.place(LiaoliV6Layout.thrustTip());
        VertexConsumer sword = buffer.getBuffer(RenderType.entityTranslucentEmissive(SWORD_HOT));
        ribbonAlong(sword, matrix, hilt, tip, upward, LiaoliV6Layout.heldHalfWidth(hilt.distance(tip)),
                0.0f, 1.0f, 0.0f, 1.0f, 255);
        int frame = LiaoliV6Layout.loopFrame(lived, LiaoliV6Layout.STREAM_FRAMES);
        float nearV = frame / (float) LiaoliV6Layout.STREAM_FRAMES;
        float farV = (frame + 1) / (float) LiaoliV6Layout.STREAM_FRAMES;
        WorldPoint nearTip = basis.place(LiaoliV6Layout.streamNearTip());
        WorldPoint rear = basis.place(LiaoliV6Layout.streamRear());
        VertexConsumer stream = buffer.getBuffer(RenderType.entityTranslucentEmissive(
                NetherBlueFlame.current(THRUST_STREAM, BLUE_THRUST_STREAM)));
        ribbonAlong(stream, matrix, nearTip, rear, lateral, LiaoliV6Layout.streamHalfWidth(nearTip.distance(rear)),
                0.0f, 1.0f, nearV, farV, 255);
    }

    private static void ribbonAlong(VertexConsumer consumer, Matrix4f matrix, WorldPoint from, WorldPoint toward,
                                    WorldPoint axis, double halfWidth, float nearU, float farU,
                                    float nearV, float farV, int alpha) {
        WorldPoint across = from.crossWidth(toward, axis, halfWidth);
        ribbon(consumer, matrix, from.add(across), toward.add(across), toward.add(across.negate()), from.add(across.negate()),
                nearU, farU, farV, nearV, alpha);
    }

    private static void ribbon(VertexConsumer consumer, Matrix4f matrix, WorldPoint first, WorldPoint second,
                               WorldPoint third, WorldPoint fourth, float nearU, float farU,
                               float nearV, float farV, int alpha) {
        vertex(consumer, matrix, first, nearU, farV, alpha);
        vertex(consumer, matrix, second, farU, farV, alpha);
        vertex(consumer, matrix, third, farU, nearV, alpha);
        vertex(consumer, matrix, fourth, nearU, nearV, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, WorldPoint point, float textureU, float textureV, int alpha) {
        consumer.vertex(matrix, (float) point.pointX, (float) point.pointY, (float) point.pointZ)
                .color(255, 255, 255, alpha)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0f, 1.0f, 0.0f)
                .endVertex();
    }

    /**
     * 刺击跟着施法者视线偏航。背后剑阵改跟身体偏航。
     * 横置角度与 PlayerRenderer.setupRotations 一致：游泳、滑翔、激流。
     * 这些都没有时，才读动作剪辑写到身体上的俯仰。单纯抬头低头不移动剑阵。
     */
    private static BackPose backPose(Player caster, float partialTicks, Basis lookBasis) {
        if (caster == null) {
            return new BackPose(lookBasis, 0.0F, 0.0F, 1.62D);
        }
        float bodyYaw = Mth.rotLerp(partialTicks, caster.yBodyRotO, caster.yBodyRot);
        float turn = bodyYaw * ((float) Math.PI / 180.0F);
        float pitchDegrees = bodyPitchDegrees(caster, partialTicks);
        double feetUp = Mth.lerp(partialTicks, caster.yo, caster.getY());
        double eyeUp = caster.getEyePosition(partialTicks).y;
        return new BackPose(Basis.fromYaw(turn), pitchDegrees, LiaoliV6Layout.diveAmount(pitchDegrees), eyeUp - feetUp);
    }

    private static float bodyPitchDegrees(Player player, float partialTicks) {
        if (player.isAutoSpinAttack()) {
            return -90.0F - player.getXRot();
        }
        if (player.isFallFlying()) {
            float flyingTicks = player.getFallFlyingTicks() + partialTicks;
            float opened = Mth.clamp(flyingTicks * flyingTicks / 100.0F, 0.0F, 1.0F);
            return opened * (-90.0F - player.getXRot());
        }
        float swimAmount = player.getSwimAmount(partialTicks);
        if (swimAmount > 0.0F) {
            float levelPitch = swimsInFluid(player) ? -90.0F - player.getXRot() : -90.0F;
            return Mth.lerp(swimAmount, 0.0F, levelPitch);
        }
        return actionPitchDegrees(player, partialTicks);
    }

    private static boolean swimsInFluid(Player player) {
        return player.isInWater()
                || player.isInFluidType((fluidKind, surfaceHeight) -> player.canSwimInFluidType(fluidKind));
    }

    private static float actionPitchDegrees(Player player, float partialTicks) {
        RetargetedPose sampled = ActionRenderQueries.thirdPersonPose(player, partialTicks);
        if (sampled == null || !sampled.anyDriven() || sampled.body() == null) {
            return 0.0F;
        }
        try {
            float modelPitch = VanillaEuler.from(sampled.body()).pitchRadians() * (180.0F / (float) Math.PI);
            return -modelPitch;
        } catch (IllegalArgumentException rejected) {
            return 0.0F;
        }
    }

    /** 只认同步来的施法者编号。对不上就没有人，不用距离去猜旁边的玩家。 */
    private static Player boundPlayer(TechniqueHitEntity entity) {
        if (entity.level() == null) {
            return null;
        }
        UUID casterId = entity.casterUuid();
        if (casterId == null) {
            return null;
        }
        for (Player player : entity.level().players()) {
            if (player != null && casterId.equals(player.getUUID())) {
                return player;
            }
        }
        return null;
    }

    private static final class BackPose {
        private final Basis basis;
        private final float pitchDegrees;
        private final float diveAmount;
        private final double eyeAboveFeet;

        private BackPose(Basis basis, float pitchDegrees, float diveAmount, double eyeAboveFeet) {
            this.basis = basis;
            this.pitchDegrees = pitchDegrees;
            this.diveAmount = diveAmount;
            this.eyeAboveFeet = eyeAboveFeet;
        }
    }

    private static final class Basis {
        private final double lateralX;
        private final double lateralZ;
        private final double forwardX;
        private final double forwardZ;

        private Basis(double lateralX, double lateralZ, double forwardX, double forwardZ) {
            this.lateralX = lateralX;
            this.lateralZ = lateralZ;
            this.forwardX = forwardX;
            this.forwardZ = forwardZ;
        }

        private static Basis fromYaw(float turn) {
            double aheadX = -Math.sin(turn);
            double aheadZ = Math.cos(turn);
            return new Basis(aheadZ, -aheadX, aheadX, aheadZ);
        }

        private WorldPoint place(LiaoliV6Layout.Mark mark) {
            return new WorldPoint(
                    this.lateralX * mark.lateral() + this.forwardX * mark.forward(),
                    mark.rise(),
                    this.lateralZ * mark.lateral() + this.forwardZ * mark.forward());
        }

        private WorldPoint lateralStep(double distance) {
            return new WorldPoint(this.lateralX * distance, 0.0d, this.lateralZ * distance);
        }

        private WorldPoint forwardStep(double distance) {
            return new WorldPoint(this.forwardX * distance, 0.0d, this.forwardZ * distance);
        }
    }

    private static final class WorldPoint {
        private final double pointX;
        private final double pointY;
        private final double pointZ;

        private WorldPoint(double pointX, double pointY, double pointZ) {
            this.pointX = pointX;
            this.pointY = pointY;
            this.pointZ = pointZ;
        }

        private WorldPoint add(WorldPoint other) {
            return new WorldPoint(this.pointX + other.pointX, this.pointY + other.pointY, this.pointZ + other.pointZ);
        }

        private WorldPoint negate() {
            return new WorldPoint(-this.pointX, -this.pointY, -this.pointZ);
        }

        private double distance(WorldPoint other) {
            double east = this.pointX - other.pointX;
            double rise = this.pointY - other.pointY;
            double north = this.pointZ - other.pointZ;
            return Math.sqrt(east * east + rise * rise + north * north);
        }

        private WorldPoint crossWidth(WorldPoint toward, WorldPoint axis, double halfWidth) {
            double edgeX = toward.pointX - this.pointX;
            double edgeY = toward.pointY - this.pointY;
            double edgeZ = toward.pointZ - this.pointZ;
            double spanX = edgeY * axis.pointZ - edgeZ * axis.pointY;
            double spanY = edgeZ * axis.pointX - edgeX * axis.pointZ;
            double spanZ = edgeX * axis.pointY - edgeY * axis.pointX;
            double span = Math.sqrt(spanX * spanX + spanY * spanY + spanZ * spanZ);
            if (span < 1.0e-6d) {
                return new WorldPoint(halfWidth, 0.0d, 0.0d);
            }
            double scale = halfWidth / span;
            return new WorldPoint(spanX * scale, spanY * scale, spanZ * scale);
        }
    }
}
