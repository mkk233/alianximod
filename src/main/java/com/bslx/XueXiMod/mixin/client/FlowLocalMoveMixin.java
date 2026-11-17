package com.bslx.XueXiMod.mixin.client;

import com.bslx.XueXiMod.client.FlowClientMotion;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 有流场回执时，用一条带回执的移动包替换原版玩家或载具包。 */
@Mixin(LocalPlayer.class)
public class FlowLocalMoveMixin {
    @Redirect(method = {"tick", "sendPosition"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void xuexiFlowSend(ClientPacketListener listener, Packet<?> packet) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (FlowClientMotion.wrapSend(player, packet)) {
            return;
        }
        listener.send(packet);
    }
}
