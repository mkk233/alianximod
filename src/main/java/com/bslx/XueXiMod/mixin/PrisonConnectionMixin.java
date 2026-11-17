package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.prison.PrisonMotion;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonMotionScope;
import com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.20.1 字节码：handleMovePlayer 里 ServerPlayer.absMoveTo(DDDFF) 两次，第二次才是封包落点。
 * 同方法里的 teleport(DDDFF) 共四次，都是纠偏或重发。
 * 原版移动只有 ServerPlayer.move(MoverType, Vec3) 一次，不是 Entity.move。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PrisonConnectionMixin {
    private Vec3 xuexiRoseSample;

    @Redirect(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void xuexiPrisonCorrect(ServerGamePacketListenerImpl listener, double worldX, double worldY, double worldZ,
                                    float yaw, float pitch) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
        try (PrisonMotionScope.Open motionOpen = PrisonMotionScope.open(PrisonMotionScope.Kind.CORRECTION)) {
            VineMarkRuntime.resetMove(player);
            this.xuexiRoseSample = null;
            listener.teleport(worldX, worldY, worldZ, yaw, pitch);
        }
    }

    @Redirect(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void xuexiRoseBeforeVanillaMove(ServerPlayer player, net.minecraft.world.entity.MoverType moverType, Vec3 movement) {
        VineMarkRuntime.captureMove(player);
        this.xuexiRoseSample = player.position();
        player.move(moverType, movement);
    }

    @Redirect(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;absMoveTo(DDDFF)V",
                    ordinal = 1))
    private void xuexiPrisonPacket(ServerPlayer player, double worldX, double worldY, double worldZ, float yaw, float pitch) {
        Vec3 roseStart = this.xuexiRoseSample;
        this.xuexiRoseSample = null;
        if (!PrisonMotion.locks(player)) {
            player.absMoveTo(worldX, worldY, worldZ, yaw, pitch);
            if (roseStart != null) {
                VineMarkRuntime.acceptMove(player, roseStart, player.position(), false);
            }
            return;
        }
        Vec3 anchor = PrisonMotion.anchor(player);
        if (anchor == null || anchor.distanceToSqr(worldX, worldY, worldZ) <= 1.0E-8D) {
            player.absMoveTo(worldX, worldY, worldZ, yaw, pitch);
            if (roseStart != null) {
                VineMarkRuntime.acceptMove(player, roseStart, player.position(), false);
            }
            return;
        }
        try (PrisonMotionScope.Open motionOpen = PrisonMotionScope.open(PrisonMotionScope.Kind.CORRECTION)) {
            VineMarkRuntime.resetMove(player);
            player.connection.teleport(anchor.x, anchor.y, anchor.z, yaw, pitch);
        }
    }

    @Redirect(method = "handleMoveVehicle(Lnet/minecraft/network/protocol/game/ServerboundMoveVehiclePacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;absMoveTo(DDDFF)V"))
    private void xuexiPrisonVehicle(Entity vehicle, double worldX, double worldY, double worldZ, float yaw, float pitch) {
        if (!PrisonMotion.locks(vehicle)) {
            vehicle.absMoveTo(worldX, worldY, worldZ, yaw, pitch);
            return;
        }
        Vec3 anchor = PrisonMotion.anchor(vehicle);
        if (anchor == null) {
            vehicle.absMoveTo(worldX, worldY, worldZ, yaw, pitch);
            return;
        }
        vehicle.absMoveTo(anchor.x, anchor.y, anchor.z, yaw, pitch);
    }

    @Redirect(method = "teleport(DDDFFLjava/util/Set;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;absMoveTo(DDDFF)V"))
    private void xuexiPrisonConnection(ServerPlayer player, double worldX, double worldY, double worldZ, float yaw, float pitch) {
        PrisonMotion.acceptConnection(player, worldX, worldY, worldZ, yaw, pitch);
    }
}
