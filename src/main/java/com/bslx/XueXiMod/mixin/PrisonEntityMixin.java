package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.prison.PrisonMotion;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonMotionScope;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonRuntime;
import com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 服务端位移写入。普通 setPos 留在锚点，传送方法用 try/finally 作用域。 */
@Mixin(Entity.class)
public abstract class PrisonEntityMixin {
    @Shadow
    protected abstract void setPosRaw(double worldX, double worldY, double worldZ);

    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), cancellable = true)
    private void xuexiPrisonBlockMove(MoverType moverType, Vec3 movement, CallbackInfo callback) {
        Entity entity = (Entity) (Object) this;
        if (!PrisonMotion.locks(entity) || PrisonMotionScope.allowsDisplacement()) {
            return;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        callback.cancel();
    }

    @Redirect(method = "setPos(DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPosRaw(DDD)V"))
    private void xuexiPrisonSetPos(Entity entity, double worldX, double worldY, double worldZ) {
        Vec3 locked = PrisonMotion.displacementAnchor(entity);
        if (locked == null) {
            this.setPosRaw(worldX, worldY, worldZ);
            return;
        }
        this.setPosRaw(locked.x, locked.y, locked.z);
        entity.xo = locked.x;
        entity.yo = locked.y;
        entity.zo = locked.z;
        entity.setDeltaMovement(Vec3.ZERO);
    }

    @Redirect(method = "teleportTo(DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;moveTo(DDDFF)V"))
    private void xuexiPrisonTeleport(Entity entity, double worldX, double worldY, double worldZ, float yaw, float pitch) {
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            VineMarkRuntime.resetMove(living);
        }
        PrisonMotion.relocate(entity, worldX, worldY, worldZ, yaw, pitch);
    }

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("HEAD"), cancellable = true)
    private void xuexiPrisonDenyRide(Entity vehicle, boolean forceRide, CallbackInfoReturnable<Boolean> callback) {
        Entity rider = (Entity) (Object) this;
        if (PrisonMotion.locks(rider) || PrisonMotion.locks(vehicle)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
            at = @At("HEAD"), cancellable = true)
    private void xuexiPrisonHoldPassenger(Entity passenger, Entity.MoveFunction moveFunction, CallbackInfo callback) {
        if (PrisonMotion.locks(passenger)) {
            callback.cancel();
        }
    }

    @Inject(method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"), remap = false)
    private void xuexiPrisonCrossDimension(ServerLevel destination, ITeleporter teleporter, CallbackInfoReturnable<Entity> callback) {
        Entity arrived = callback.getReturnValue();
        if (arrived == null) {
            return;
        }
        PrisonRuntime.crossDimension((Entity) (Object) this, arrived);
    }
}
