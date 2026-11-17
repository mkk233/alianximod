package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 投射物所有者变化时同步或清除玩家来源标记。 */
@Mixin(Projectile.class)
public class ProjectileOwnerMixin {
    @Inject(method = "setOwner", at = @At("RETURN"))
    private void xuexiTrackProjectileOwner(Entity owner, CallbackInfo callback) {
        CombatOwnership.transfer((Projectile) (Object) this, owner);
    }
}
