package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 种火、引爆和地表、土尘的条带。引燃改走燃烧轨道，这里不再放一次性点火片。只播放，不提交资源账。 */
public final class BatchTriggerVisuals {
    private static long flashStamp = Long.MIN_VALUE;
    private static final Set<String> FLASHED = new HashSet<>();
    private static long surfaceStamp = Long.MIN_VALUE;
    private static int surfaceX;
    private static int surfaceY;
    private static int surfaceZ;

    private BatchTriggerVisuals() {
    }

    public static void onAccepted(ServerLevel level, Vec3 point, UUID targetId, ResourceEnchantLoad load,
                                  int bodyScaledBase, int legacyFixedPoints) {
        if (level == null || point == null || load == null) {
            return;
        }
        long stamp = level.getGameTime();
        if (stamp != flashStamp) {
            FLASHED.clear();
            flashStamp = stamp;
        }
        int seedBase = bodyScaledBase + load.scaledSeedBase() + Math.max(0, legacyFixedPoints);
        if (seedBase > 0) {
            spawnOnce(level, point, targetId, BatchVisualTexture.SEED, stamp);
        }
    }

    /**
     * 一次真实消费对应一团三维云。26 个面只在客户端展开，这里不新增结算或伤害实体。
     * 位置保持调用方给出的命中点。
     */
    public static void detonate(ServerLevel level, Vec3 hitPoint, int spentSeeds) {
        if (level == null || hitPoint == null || spentSeeds <= 0) {
            return;
        }
        if (!Double.isFinite(hitPoint.x) || !Double.isFinite(hitPoint.y) || !Double.isFinite(hitPoint.z)) {
            return;
        }
        float visualScale = (float) DetonateCloudLayout.visualScale(spentSeeds);
        if (!(visualScale > 0.0F) || !Float.isFinite(visualScale)) {
            return;
        }
        BatchCarrierVisual.spawn(level, hitPoint, BatchVisualShape.BILLBOARD, BatchVisualTexture.DETONATE,
                DetonateCloudLayout.PLAY_TICKS, visualScale, -1);
    }

    public static void surface(ServerLevel level, Vec3 point) {
        if (level == null || point == null) {
            return;
        }
        BlockPos cell = BlockPos.containing(point);
        long stamp = level.getGameTime();
        if (stamp == surfaceStamp && cell.getX() == surfaceX && cell.getY() == surfaceY && cell.getZ() == surfaceZ) {
            return;
        }
        surfaceStamp = stamp;
        surfaceX = cell.getX();
        surfaceY = cell.getY();
        surfaceZ = cell.getZ();
        BatchCarrierVisual.spawn(level, point, BatchVisualShape.BILLBOARD, BatchVisualTexture.SURFACE,
                BatchCarrierIds.STRIP_FRAMES, 0.40F, -1);
    }

    public static void dust(ServerLevel level, Vec3 point) {
        if (level == null || point == null) {
            return;
        }
        if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z)) {
            return;
        }
        BatchCarrierVisual.spawn(level, point, BatchVisualShape.BILLBOARD, BatchVisualTexture.DUST,
                GroundDustLayout.PLAY_TICKS, 1.0F, -1);
    }

    private static void spawnOnce(ServerLevel level, Vec3 point, UUID targetId, BatchVisualTexture texture, long stamp) {
        String marker = texture.name() + ":" + (targetId == null ? point.toString() : targetId.toString()) + ":" + stamp;
        if (!FLASHED.add(marker)) {
            return;
        }
        Vec3 raised = point.add(0.0D, 1.0D, 0.0D);
        if (texture == BatchVisualTexture.SEED) {
            spawnSeed(level, point, targetId);
            return;
        }
        BatchCarrierVisual.spawn(level, raised, BatchVisualShape.BILLBOARD, texture, BatchCarrierIds.STRIP_FRAMES, 0.6F, -1);
    }

    /**
     * 种火中心贴在身体中心，面的边长覆盖碰撞高度。没有目标时用事件点上方 0.875 格的 1.75 格正方形。
     * 向镜头拉近只在客户端画 SEED，不写进这里的世界坐标。
     */
    private static void spawnSeed(ServerLevel level, Vec3 point, UUID targetId) {
        Entity target = targetId == null ? null : level.getEntity(targetId);
        Vec3 center = point.add(0.0D, 0.875D, 0.0D);
        float halfSpan = 0.875F;
        if (target != null && !target.isRemoved()) {
            AABB body = target.getBoundingBox();
            double height = body.getYsize();
            if (Double.isFinite(height) && height > 0.0D) {
                center = body.getCenter();
                halfSpan = (float) (height * 0.5D);
            }
        }
        BatchCarrierVisual.spawn(level, center, BatchVisualShape.BILLBOARD, BatchVisualTexture.SEED,
                BatchCarrierIds.STRIP_FRAMES, halfSpan, -1);
    }
}
