package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.registry.ModMobEffects;
import com.bslx.XueXiMod.status.WetnessFlight;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * move 与 checkWalls 的字节码 owner 都是 EnderDragon。
 * 两次飞行 move 在部件更新之前，三处 checkWalls 在全部 tickPart 之后。
 */
@Mixin(EnderDragon.class)
public abstract class WetnessDragonMixin extends Mob {
    protected WetnessDragonMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public EnderDragonPart head;
    @Shadow
    private EnderDragonPart neck;
    @Shadow
    private EnderDragonPart body;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void xuexiWetnessDragonBegin(CallbackInfo callback) {
        WetnessFlight.beginDragonStep((EnderDragon) (Object) this);
    }

    @Redirect(method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            require = 2)
    private void xuexiWetnessDragonMove(EnderDragon receiver, MoverType type, Vec3 movement) {
        Vec3 adjusted = WetnessFlight.adjustDragonMove(receiver, type, movement);
        receiver.move(type, adjusted);
    }

    @ModifyArg(method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;checkWalls(Lnet/minecraft/world/phys/AABB;)Z",
                    ordinal = 0),
            index = 0)
    private AABB xuexiWetnessDragonWalls(AABB preparedHead) {
        return WetnessFlight.correctDragonDescent(
                (EnderDragon) (Object) this, this.head, this.neck, this.body, preparedHead);
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void xuexiWetnessDragonEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback) {
        if (effect != null && effect.getEffect() == ModMobEffects.WETNESS.get()) {
            callback.setReturnValue(super.addEffect(effect, source));
        }
    }
}
