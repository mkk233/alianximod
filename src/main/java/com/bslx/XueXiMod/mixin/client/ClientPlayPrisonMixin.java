package com.bslx.XueXiMod.mixin.client;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bslx.XueXiMod.client.PrisonClientState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.entity.RelativeMovement;

/**
 * 服务端位置包到达时先看落点。离开壳就解除本地锁，不取消、不改写这个包。
 * handleMovePlayer(Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;)V 的开头。
 */
@Mixin(ClientPacketListener.class)
public class ClientPlayPrisonMixin {
    @Inject(method = "handleMovePlayer(Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;)V",
            at = @At("HEAD"), require = 1)
    private void xuexiNotePrisonTeleport(ClientboundPlayerPositionPacket packet, CallbackInfo callback) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || packet == null) {
            return;
        }
        Set<RelativeMovement> relative = packet.getRelativeArguments();
        double nextX = packet.getX();
        double nextY = packet.getY();
        double nextZ = packet.getZ();
        if (relative != null) {
            if (relative.contains(RelativeMovement.X)) {
                nextX = player.getX() + packet.getX();
            }
            if (relative.contains(RelativeMovement.Y)) {
                nextY = player.getY() + packet.getY();
            }
            if (relative.contains(RelativeMovement.Z)) {
                nextZ = player.getZ() + packet.getZ();
            }
        }
        PrisonClientState.noteServerTeleport(nextX, nextY, nextZ);
    }
}
