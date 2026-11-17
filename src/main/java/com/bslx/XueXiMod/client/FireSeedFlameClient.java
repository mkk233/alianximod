package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.network.FireSeedVisualBridge;
import com.bslx.XueXiMod.network.FireSeedVisualPacket;
import com.bslx.XueXiMod.status.fire.seed.FireSeedMath;
import com.bslx.XueXiMod.visual.facing.CameraFacingEffectId;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 火种余额的小火苗。图集手动切 8 帧，每帧 100 毫秒，不再配动画描述文件。
 * 只读服务端发来的余额，不改火种账。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
public final class FireSeedFlameClient {
    private static final int FRAME_COUNT = 8;
    private static final long FRAME_MILLIS = 100L;
    private static final int ABSENT_LIMIT = 40;
    private static final double VIEW_REACH = 96.0d;
    private static final ResourceLocation FLAME_SHEET =
            new ResourceLocation(XueXiMod.MOD_ID, "textures/effect/fire_seed/fire_seed_flame.png");
    private static final ResourceLocation BLUE_FLAME_SHEET = NetherBlueFlame.sheet("fire_seed.png");
    /** 相对身体宽和高的分散位置。最多 10 朵，对应火种上限 100。 */
    private static final double[] FLAME_RIGHT = {0.55d, -0.50d, 0.15d, -0.22d, 0.62d, -0.58d, 0.08d, -0.14d, 0.40d, -0.36d};
    private static final double[] FLAME_UP = {0.25d, 0.40d, 0.62d, 0.78d, 0.52d, 0.30d, 0.88d, 0.18d, 0.70d, 0.58d};
    private static final double[] FLAME_FORWARD = {0.15d, -0.20d, 0.55d, -0.48d, -0.10d, 0.42d, 0.22d, -0.55d, 0.35d, -0.38d};

    private static final Map<UUID, SeedSight> SIGHTS = new HashMap<>();

    private FireSeedFlameClient() {
    }

    public static void bind() {
        FireSeedVisualBridge.bind(FireSeedFlameClient::accept);
    }

    public static void accept(FireSeedVisualPacket packet) {
        if (packet == null || packet.entityUuid() == null) {
            return;
        }
        if (FireSeedMath.tiers(packet.balance()) <= 0) {
            SIGHTS.remove(packet.entityUuid());
            return;
        }
        SIGHTS.put(packet.entityUuid(), new SeedSight(packet.entityId(), packet.entityUuid(), packet.balance()));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || SIGHTS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            SIGHTS.clear();
            return;
        }
        float partialTick = event.getPartialTick();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType ordinaryType = RenderType.entityTranslucentEmissive(FLAME_SHEET);
        RenderType blueType = RenderType.entityTranslucentEmissive(BLUE_FLAME_SHEET);
        VertexConsumer ordinaryConsumer = buffers.getBuffer(ordinaryType);
        VertexConsumer blueConsumer = buffers.getBuffer(blueType);
        boolean drew = false;
        boolean drewBlue = false;
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        Iterator<SeedSight> sights = SIGHTS.values().iterator();
        while (sights.hasNext()) {
            SeedSight sight = sights.next();
            Entity entity = clientLevel.getEntity(sight.entityId);
            if (entity == null) {
                sight.absentTicks += 1;
                if (sight.absentTicks > ABSENT_LIMIT) {
                    sights.remove();
                }
                continue;
            }
            if (!sight.entityUuid.equals(entity.getUUID()) || !entity.isAlive()) {
                sights.remove();
                continue;
            }
            sight.absentTicks = 0;
            boolean blueFlame = NetherBlueFlame.covers(clientLevel, entity, partialTick);
            VertexConsumer consumer = blueFlame ? blueConsumer : ordinaryConsumer;
            if (paint(pose, event.getCamera(), consumer, entity, sight, partialTick, camera)) {
                if (blueFlame) {
                    drewBlue = true;
                } else {
                    drew = true;
                }
            }
        }
        pose.popPose();
        if (drew) {
            buffers.endBatch(ordinaryType);
        }
        if (drewBlue) {
            buffers.endBatch(blueType);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() != null && event.getLevel().isClientSide() && event.getEntity() != null) {
            SIGHTS.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SIGHTS.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() != null && event.getLevel().isClientSide()) {
            SIGHTS.clear();
        }
    }

    private static boolean paint(PoseStack pose, Camera camera, VertexConsumer consumer, Entity entity, SeedSight sight,
                                 float partialTick, Vec3 cameraPosition) {
        double anchorX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double anchorY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double anchorZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
        double east = anchorX - cameraPosition.x;
        double rise = anchorY - cameraPosition.y;
        double north = anchorZ - cameraPosition.z;
        if (east * east + rise * rise + north * north > VIEW_REACH * VIEW_REACH) {
            return false;
        }
        int flames = FireSeedMath.tiers(sight.balance);
        if (flames <= 0) {
            return false;
        }
        double span = entity.getBbWidth();
        if (span < 0.3d) {
            span = 0.3d;
        }
        double stature = entity.getBbHeight();
        if (stature < 0.3d) {
            stature = 0.3d;
        }
        double flameHalf = stature < 1.0d ? 0.11d * stature : 0.11d;
        if (flameHalf < 0.04d) {
            flameHalf = 0.04d;
        }
        float facing = entity instanceof Player player
                ? Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot)
                : Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        double yawRad = facing * (Math.PI / 180.0d);
        double sine = Math.sin(yawRad);
        double cosine = Math.cos(yawRad);
        int slot = 0;
        int limit = flames;
        if (limit > FLAME_RIGHT.length) {
            limit = FLAME_RIGHT.length;
        }
        while (slot < limit) {
            double right = FLAME_RIGHT[slot] * span;
            double forward = FLAME_FORWARD[slot] * span;
            double height = FLAME_UP[slot] * stature;
            double worldX = anchorX + right * cosine - forward * sine;
            double worldZ = anchorZ + right * sine + forward * cosine;
            billboard(pose, camera, consumer, worldX, anchorY + height, worldZ, flameHalf, flameFrame(slot));
            slot += 1;
        }
        return true;
    }

    /** 竖条从上往下第几帧。相位按朵错开，切帧间隔仍是 100 毫秒。 */
    private static int flameFrame(int slot) {
        long steps = System.currentTimeMillis() / FRAME_MILLIS;
        return (int) Math.floorMod(steps + slot, FRAME_COUNT);
    }

    private static void billboard(PoseStack pose, Camera camera, VertexConsumer consumer, double worldX, double centerY,
                                  double worldZ, double flameHalf, int frame) {
        float nearV = frame / (float) FRAME_COUNT;
        float farV = (frame + 1) / (float) FRAME_COUNT;
        float half = (float) flameHalf;
        pose.pushPose();
        pose.translate(worldX, centerY, worldZ);
        CameraFacingClient.orientDeclaredPlane(pose, CameraFacingEffectId.FIRE_SEED_ORBIT_FLAME, camera);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        tinted(consumer, matrix, normal, -half, -half, 0.0f, 0.0f, farV, 255, 255, 255);
        tinted(consumer, matrix, normal, half, -half, 0.0f, 1.0f, farV, 255, 255, 255);
        tinted(consumer, matrix, normal, half, half, 0.0f, 1.0f, nearV, 255, 255, 255);
        tinted(consumer, matrix, normal, -half, half, 0.0f, 0.0f, nearV, 255, 255, 255);
        pose.popPose();
    }

    private static void tinted(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float pointX, float pointY,
                               float pointZ, float textureU, float textureV, int red, int green, int blue) {
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(red, green, blue, 255)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0f, 0.0f, 1.0f)
                .endVertex();
    }

    private static final class SeedSight {
        private final int entityId;
        private final UUID entityUuid;
        private final int balance;
        private int absentTicks;

        private SeedSight(int entityId, UUID entityUuid, int balance) {
            this.entityId = entityId;
            this.entityUuid = entityUuid;
            this.balance = balance;
            this.absentTicks = 0;
        }
    }
}
