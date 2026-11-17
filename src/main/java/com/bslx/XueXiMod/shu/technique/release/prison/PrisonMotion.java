package com.bslx.XueXiMod.shu.technique.release.prison;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 普通定位拉回锚点。真正传送在作用域里放行，完成后才按坐标或维度变化破裂。 */
public final class PrisonMotion {
    private static final double MOVE_EPSILON_SQR = 1.0E-8D;

    private PrisonMotion() {
    }

    /** 这个实体是否被活动水牢锁在锚点。 */
    public static boolean locks(Entity entity) {
        return PrisonRuntime.locks(entity);
    }

    /** 当前锁定的世界锚点。没有锁定时为空。 */
    public static Vec3 anchor(Entity entity) {
        return PrisonRuntime.anchor(entity);
    }

    /** 没有传送作用域时返回锚点，调用方改写这次定位。允许传送时返回空。 */
    public static Vec3 displacementAnchor(Entity entity) {
        if (!locks(entity) || PrisonMotionScope.allowsDisplacement()) {
            return null;
        }
        return anchor(entity);
    }

    /** 普通定位留在锚点。真正移动后，活动牢按传送结束。 */
    public static void relocate(Entity entity, double worldX, double worldY, double worldZ, float yaw, float pitch) {
        if (entity == null) {
            return;
        }
        if (!locks(entity) || PrisonMotionScope.defersBreak()) {
            entity.moveTo(worldX, worldY, worldZ, yaw, pitch);
            return;
        }
        Vec3 before = entity.position();
        ResourceKey<Level> dimension = entity.level().dimension();
        try (PrisonMotionScope.Open motionOpen = PrisonMotionScope.open(PrisonMotionScope.Kind.TELEPORT)) {
            entity.moveTo(worldX, worldY, worldZ, yaw, pitch);
        }
        if (entity instanceof LivingEntity living && moved(entity, before, dimension)) {
            PrisonRuntime.endTarget(living, EndReason.TELEPORT, living);
        }
    }

    /** 接受连接定位。没有实际位移或换维时不破牢。 */
    public static void acceptConnection(ServerPlayer player, double worldX, double worldY, double worldZ, float yaw, float pitch) {
        if (player == null) {
            return;
        }
        if (!locks(player) || PrisonMotionScope.defersBreak()) {
            player.absMoveTo(worldX, worldY, worldZ, yaw, pitch);
            return;
        }
        Vec3 before = player.position();
        ResourceKey<Level> dimension = player.level().dimension();
        try (PrisonMotionScope.Open motionOpen = PrisonMotionScope.open(PrisonMotionScope.Kind.TELEPORT)) {
            player.absMoveTo(worldX, worldY, worldZ, yaw, pitch);
        }
        if (moved(player, before, dimension)) {
            PrisonRuntime.endTarget(player, EndReason.TELEPORT, player);
        }
    }

    /** 比较位置或维度是否真正变了。 */
    public static boolean moved(Entity entity, Vec3 before, ResourceKey<Level> dimension) {
        if (entity == null || before == null || dimension == null) {
            return false;
        }
        if (entity.level().dimension() != dimension) {
            return true;
        }
        return entity.position().distanceToSqr(before) > MOVE_EPSILON_SQR;
    }
}
