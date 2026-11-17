package com.bslx.XueXiMod.mixin.client;

import com.bslx.XueXiMod.client.NetherDomainAtmosphere;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 只改当前帧天空色返回值。权重为零时保留原版计算结果。 */
@Mixin(ClientLevel.class)
public class ClientLevelGloomSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void xueximod$tintGloomSky(Vec3 position, float partialTick, CallbackInfoReturnable<Vec3> callback) {
        Vec3 original = callback.getReturnValue();
        callback.setReturnValue(NetherDomainAtmosphere.sky(original));
    }
}
