package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainSight;
import com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainTint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * 幽冥魂火用 v2 竖条按 64×64 分 16 帧取紫外。
 * 魂火顶点保持白色，青色只来自贴图；启动时仍接上色调查询，供其他火焰使用。
 * 确认版只播放火舌帧，整簇宽高和朝向固定；每个切片另补 8 枚纯视觉余烬。
 */
public final class NetherDomainWisps {
    public static final ResourceLocation SHEET = new ResourceLocation(XueXiMod.MOD_ID,
            "textures/effect/nether_gloom_v2/nether_gloom.png");
    private static final int FRAME_COUNT = 16;
    private static final int TICKS_PER_FRAME = 2;
    private static final int CLUSTER_CAP = 256;
    private static final int EMBERS_PER_VOLUME = 8;
    private static final double SPACING_MIN = 0.9d;
    private static final double SPACING_SPAN = 0.2d;
    private static final double GROUND_LIFT = 0.15d;
    private static final double WIDTH_MIN = 0.6486d;
    private static final double WIDTH_SPAN = 0.3478d;
    private static final double HEIGHT_MIN = 0.58d;
    private static final double HEIGHT_SPAN = 0.6496d;
    private static final double EMBER_WIDTH_MIN = 0.028d;
    private static final double EMBER_WIDTH_SPAN = 0.020d;
    private static final double EMBER_HEIGHT_MIN = 0.042d;
    private static final double EMBER_HEIGHT_SPAN = 0.030d;
    private static final double EMBER_RISE_MIN = 0.71d;
    private static final double EMBER_RISE_SPAN = 0.94d;
    private static final double EMBER_DRIFT = 0.041d;
    private static final double EMBER_PERIOD = 1.6d;
    private static final double JITTER_FRACTION = 0.72d;
    private static final int WHITE = 255;
    private static final int ALPHA = 210;
    private static final int EMBER_RED = 204;
    private static final int EMBER_GREEN = 250;
    private static final int EMBER_BLUE = 245;
    private static RenderType sheetType;

    private NetherDomainWisps() {
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        private Setup() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            NetherDomainTint.bindClient(NetherDomainSight::probe);
        }
    }

    @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
    public static final class Draw {
        private Draw() {
        }

        @SubscribeEvent
        public static void onRender(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                NetherDomainSight.clear();
                return;
            }
            long gameTime = minecraft.level.getGameTime();
            float partialTick = event.getPartialTick();
            String dimensionId = minecraft.level.dimension().location().toString();
            PoseStack pose = event.getPoseStack();
            Vec3 camera = event.getCamera().getPosition();
            if (sheetType == null) {
                sheetType = RenderType.entityTranslucentEmissive(SHEET);
            }
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            VertexConsumer flameConsumer = buffers.getBuffer(sheetType);
            boolean drew = false;
            pose.pushPose();
            pose.translate(-camera.x, -camera.y, -camera.z);
            for (NetherDomainSight.Sight sight : NetherDomainSight.all()) {
                if (!sight.live(dimensionId, gameTime)) {
                    continue;
                }
                if (paint(pose, flameConsumer, sight, gameTime, partialTick)) {
                    drew = true;
                }
            }
            pose.popPose();
            if (drew) {
                buffers.endBatch(sheetType);
            }
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            NetherDomainSight.clear();
        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel() != null && event.getLevel().isClientSide()) {
                NetherDomainSight.clear();
            }
        }
    }

    private static boolean paint(PoseStack pose, VertexConsumer consumer, NetherDomainSight.Sight sight,
                                 long gameTime, float partialTick) {
        int count = sight.volumeCount();
        if (count <= 0) {
            return false;
        }
        double[] box = new double[6];
        int phase = Math.floorMod(sight.domainId().hashCode(), FRAME_COUNT);
        int ordinal = 0;
        boolean drew = false;
        while (ordinal < count) {
            if (sight.copyVolume(ordinal, box)
                    && paintBox(pose, consumer, sight.domainId(), ordinal, box,
                    gameTime, partialTick, phase)) {
                drew = true;
            }
            ordinal++;
        }
        return drew;
    }

    private static boolean paintBox(PoseStack pose, VertexConsumer consumer, UUID domainId, int volumeOrdinal,
                                    double[] box, long gameTime, float partialTick, int domainPhase) {
        double spanX = box[3] - box[0];
        double spanY = box[4] - box[1];
        double spanZ = box[5] - box[2];
        if (spanX < 0.05d || spanZ < 0.05d || spanY < 0.0d) {
            return false;
        }
        double spacing = SPACING_MIN + SPACING_SPAN * unit(domainId, volumeOrdinal, 7);
        int columns = Math.max(1, (int) Math.round(spanX / spacing));
        int rows = Math.max(1, (int) Math.round(spanZ / spacing));
        if ((long) columns * rows > CLUSTER_CAP) {
            int[] limited = limitClusters(columns, rows);
            columns = limited[0];
            rows = limited[1];
        }
        double stepX = spanX / columns;
        double stepZ = spanZ / rows;
        double groundY = box[1] + GROUND_LIFT;
        int column = 0;
        boolean drew = false;
        while (column < columns) {
            int row = 0;
            while (row < rows) {
                int index = row * columns + column;
                int identity = volumeOrdinal * 1024 + index;
                double worldX = box[0] + (column + 0.5d) * stepX
                        + (unit(domainId, identity, 1) - 0.5d) * stepX * JITTER_FRACTION;
                double worldZ = box[2] + (row + 0.5d) * stepZ
                        + (unit(domainId, identity, 2) - 0.5d) * stepZ * JITTER_FRACTION;
                double width = WIDTH_MIN + WIDTH_SPAN * unit(domainId, identity, 3);
                double height = HEIGHT_MIN + HEIGHT_SPAN * unit(domainId, identity, 4);
                float yaw = (float) (unit(domainId, identity, 5) * Math.PI);
                int clusterPhase = (index % 4) * 4;
                int frame = (int) Math.floorMod(gameTime / TICKS_PER_FRAME + domainPhase + clusterPhase,
                        (long) FRAME_COUNT);
                float nearV = frame / (float) FRAME_COUNT;
                float farV = (frame + 1) / (float) FRAME_COUNT;
                cross(pose, consumer, worldX, groundY, worldZ, width * 0.5d, height, yaw, nearV, farV,
                        WHITE, WHITE, WHITE, ALPHA);
                drew = true;
                row++;
            }
            column++;
        }
        if (embers(pose, consumer, domainId, volumeOrdinal, box, gameTime, partialTick)) {
            drew = true;
        }
        return drew;
    }

    private static boolean embers(PoseStack pose, VertexConsumer consumer, UUID domainId, int volumeOrdinal,
                                  double[] box, long gameTime, float partialTick) {
        double spanX = box[3] - box[0];
        double spanZ = box[5] - box[2];
        double seconds = (gameTime + partialTick) / 20.0d;
        int ember = 0;
        while (ember < EMBERS_PER_VOLUME) {
            int identity = volumeOrdinal * 64 + ember;
            double worldX = box[0] + spanX * unit(domainId, identity, 11);
            double worldZ = box[2] + spanZ * unit(domainId, identity, 12);
            double width = EMBER_WIDTH_MIN + EMBER_WIDTH_SPAN * unit(domainId, identity, 13);
            double height = EMBER_HEIGHT_MIN + EMBER_HEIGHT_SPAN * unit(domainId, identity, 14);
            double rise = EMBER_RISE_MIN + EMBER_RISE_SPAN * unit(domainId, identity, 15);
            double drift = (unit(domainId, identity, 16) - 0.5d) * 2.0d * EMBER_DRIFT;
            double phase = unit(domainId, identity, 17);
            double wave = 0.5d + 0.5d * Math.sin((Math.PI * 2.0d) * (seconds / EMBER_PERIOD + phase));
            float life = (float) Math.sin(Math.PI * wave);
            if (life > 0.02f) {
                int alpha = Math.max(1, Math.min(ALPHA, Math.round(ALPHA * life)));
                point(pose, consumer, worldX + drift * wave, box[1] + rise * wave, worldZ,
                        width * 0.5d * life, height * life, EMBER_RED, EMBER_GREEN, EMBER_BLUE, alpha);
            }
            ember++;
        }
        return true;
    }

    private static int[] limitClusters(int columns, int rows) {
        double aspect = columns / (double) rows;
        int bestColumns = 1;
        int bestRows = 1;
        double bestDistance = Double.POSITIVE_INFINITY;
        int columnTry = 1;
        while (columnTry <= CLUSTER_CAP) {
            int rowTry = CLUSTER_CAP / columnTry;
            double distance = Math.abs(Math.log((columnTry / (double) rowTry) / aspect));
            int used = columnTry * rowTry;
            int bestUsed = bestColumns * bestRows;
            if (distance < bestDistance - 1.0e-9d
                    || (Math.abs(distance - bestDistance) <= 1.0e-9d && used > bestUsed)) {
                bestDistance = distance;
                bestColumns = columnTry;
                bestRows = rowTry;
            }
            columnTry++;
        }
        return new int[] {bestColumns, bestRows};
    }

    private static double unit(UUID domainId, int index, int salt) {
        long mixed = domainId.getMostSignificantBits()
                ^ Long.rotateLeft(domainId.getLeastSignificantBits(), 21)
                ^ (index + 1L) * 0x9E3779B97F4A7C15L
                ^ (salt + 1L) * 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 11) * 0x1.0p-53;
    }

    private static void cross(PoseStack pose, VertexConsumer consumer, double worldX, double worldY, double worldZ,
                              double halfWidth, double height, float yaw, float nearV, float farV,
                              int red, int green, int blue, int alpha) {
        pose.pushPose();
        pose.translate(worldX, worldY, worldZ);
        pose.mulPose(Axis.YP.rotation(yaw));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        float left = (float) -halfWidth;
        float right = (float) halfWidth;
        float rise = (float) height;
        quad(consumer, matrix, normal, left, 0.0f, 0.0f, right, rise, 0.0f, nearV, farV, red, green, blue, alpha);
        quad(consumer, matrix, normal, 0.0f, 0.0f, left, 0.0f, rise, right, nearV, farV, red, green, blue, alpha);
        pose.popPose();
    }

    private static void point(PoseStack pose, VertexConsumer consumer, double worldX, double worldY, double worldZ,
                              double halfWidth, double height, int red, int green, int blue, int alpha) {
        pose.pushPose();
        pose.translate(worldX, worldY, worldZ);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        float left = (float) -halfWidth;
        float right = (float) halfWidth;
        float rise = (float) height;
        float clearU = 0.5f / 64.0f;
        float clearV = 0.5f / (FRAME_COUNT * 64.0f);
        quad(consumer, matrix, normal, left, 0.0f, 0.0f, right, rise, 0.0f,
                clearV, clearV, clearU, clearU, red, green, blue, alpha);
        quad(consumer, matrix, normal, 0.0f, 0.0f, left, 0.0f, rise, right,
                clearV, clearV, clearU, clearU, red, green, blue, alpha);
        pose.popPose();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float leftX, float leftY,
                             float leftZ, float rightX, float rightY, float rightZ, float nearV, float farV,
                             int red, int green, int blue, int alpha) {
        quad(consumer, matrix, normal, leftX, leftY, leftZ, rightX, rightY, rightZ,
                nearV, farV, 0.0f, 1.0f, red, green, blue, alpha);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float leftX, float leftY,
                             float leftZ, float rightX, float rightY, float rightZ, float nearV, float farV,
                             float leftU, float rightU, int red, int green, int blue, int alpha) {
        vertex(consumer, matrix, normal, leftX, leftY, leftZ, leftU, farV, red, green, blue, alpha);
        vertex(consumer, matrix, normal, rightX, leftY, rightZ, rightU, farV, red, green, blue, alpha);
        vertex(consumer, matrix, normal, rightX, rightY, rightZ, rightU, nearV, red, green, blue, alpha);
        vertex(consumer, matrix, normal, leftX, rightY, leftZ, leftU, nearV, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float pointX, float pointY,
                               float pointZ, float textureU, float textureV,
                               int red, int green, int blue, int alpha) {
        consumer.vertex(matrix, pointX, pointY, pointZ)
                .color(red, green, blue, alpha)
                .uv(textureU, textureV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0f, 1.0f, 0.0f)
                .endVertex();
    }
}
