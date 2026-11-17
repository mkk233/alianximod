package com.bslx.XueXiMod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.NetherBlueFlame;
import com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainTint;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.entity.player.Player;

/** 第一人称着火层沿用原版火层，只在眼睛处于幽冥内时换成魂火 1。 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectSoulFireMixin {
    @Redirect(
            method = "renderFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/Material;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"),
            require = 1)
    private static TextureAtlasSprite xuexiSoulFireOverlay(Material material, Minecraft minecraft, PoseStack poseStack) {
        Player player = minecraft.player;
        if (player != null && NetherDomainTint.gloom(player.level(), player.getX(), player.getEyeY(), player.getZ())) {
            return NetherBlueFlame.soulFireOne().sprite();
        }
        return material.sprite();
    }
}
