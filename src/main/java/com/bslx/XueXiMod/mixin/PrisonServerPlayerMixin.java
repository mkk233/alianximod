package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.prison.PrisonMotion;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 玩家死亡不走 LivingEntity.die。换维落点的 moveTo 所有者是 ServerPlayer。 */
@Mixin(ServerPlayer.class)
public abstract class PrisonServerPlayerMixin {
    @Redirect(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeHooks;onLivingDeath(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z",
                    remap = false))
    private boolean xuexiPrisonConfirmedDeath(LivingEntity entity, DamageSource source) {
        boolean cancelled = ForgeHooks.onLivingDeath(entity, source);
        if (!cancelled) {
            PrisonRuntime.onConfirmedDeath(entity);
        }
        return cancelled;
    }

    @Redirect(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;moveTo(DDDFF)V"))
    private void xuexiPrisonDimensionMove(ServerPlayer player, double worldX, double worldY, double worldZ, float yaw, float pitch) {
        PrisonMotion.relocate(player, worldX, worldY, worldZ, yaw, pitch);
    }

    @Inject(method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"), remap = false)
    private void xuexiPrisonEndCredits(ServerLevel destination, ITeleporter teleporter, CallbackInfoReturnable<Entity> callback) {
        if (callback.getReturnValue() != (Object) this || destination == null || teleporter == null || !teleporter.isVanilla()) {
            return;
        }
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.level() == null || destination.dimension() != Level.OVERWORLD || self.level().dimension() != Level.END) {
            return;
        }
        if (!self.isRemoved() || self.getRemovalReason() != Entity.RemovalReason.CHANGED_DIMENSION) {
            return;
        }
        PrisonRuntime.onVanillaEndReturn(self);
    }
}
