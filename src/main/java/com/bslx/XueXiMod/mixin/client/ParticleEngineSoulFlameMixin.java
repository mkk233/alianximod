package com.bslx.XueXiMod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.bslx.XueXiMod.client.NetherBlueFlame;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;

/** 客户端生成火焰粒子时按幽冥体积改用原版魂火粒子。离开领域仍是普通火焰。 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineSoulFlameMixin {
    @Shadow
    private ClientLevel level;

    @ModifyVariable(
            method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            argsOnly = true)
    private ParticleOptions xuexiSwapSoulFlame(ParticleOptions particleOptions,
                                              ParticleOptions originalParticleOptions, double worldX, double worldY,
                                              double worldZ, double velocityX, double velocityY, double velocityZ) {
        return NetherBlueFlame.flameParticle(this.level, worldX, worldY, worldZ, particleOptions);
    }
}
