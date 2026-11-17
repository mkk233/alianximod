package com.bslx.XueXiMod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bslx.XueXiMod.client.NetherBlueFlame;
import com.bslx.XueXiMod.client.FireVisualClient;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** 实体着火覆盖层在幽冥内改用原版魂火 0/1。同一图集只换精灵，不另画一层橙火。引燃活跃且原版未着火时，也只借这一次火焰。 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherSoulFireMixin {
    @Shadow
    private Level level;

    private float xuexiFlamePartialTick;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void xuexiRememberFlamePartial(Entity entity, double cameraX, double cameraY, double cameraZ, float bodyYaw,
                                           float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, CallbackInfo callback) {
        this.xuexiFlamePartialTick = partialTick;
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;displayFireAnimation()Z"))
    private boolean xuexiKindleUsesVanillaFlame(Entity entity) {
        return entity.displayFireAnimation() || FireVisualClient.showsKindleFlame(entity);
    }

    @Redirect(
            method = "renderFlame",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/Material;sprite()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"),
            require = 2)
    private TextureAtlasSprite xuexiSoulFlameSprite(Material material, PoseStack poseStack, MultiBufferSource bufferSource,
                                                   Entity burningEntity) {
        if (NetherBlueFlame.covers(this.level, burningEntity, this.xuexiFlamePartialTick)) {
            if (material == ModelBakery.FIRE_0) {
                return NetherBlueFlame.soulFireZero().sprite();
            }
            if (material == ModelBakery.FIRE_1) {
                return NetherBlueFlame.soulFireOne().sprite();
            }
        }
        return material.sprite();
    }
}
