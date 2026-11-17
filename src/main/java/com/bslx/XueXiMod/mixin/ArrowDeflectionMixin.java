package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.deflection.MitigationCreditScope;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowBook;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowCaptureAccess;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowCaptureBook;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowFlight;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowSupport;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowTickState;
import com.bslx.XueXiMod.shu.technique.effects.SwordGuardEffect;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 tick 在方块裁剪之后、findHitEntity 的唯一调用点之前查询水体。
 * 只有已经接管的箭才取消这一拍剩余运动，避免穿透循环反复选中同一目标。
 */
@Mixin(AbstractArrow.class)
public abstract class ArrowDeflectionMixin implements ArrowCaptureAccess {
    @Unique
    private static final EntityDataAccessor<Boolean> XUEXI_CAPTURED =
            SynchedEntityData.defineId(AbstractArrow.class, EntityDataSerializers.BOOLEAN);

    @Shadow
    protected abstract boolean canHitEntity(Entity target);

    @Override
    @Unique
    public void xuexiSetCaptured(boolean captured) {
        ((AbstractArrow) (Object) this).getEntityData().set(XUEXI_CAPTURED, captured);
    }

    @Override
    @Unique
    public boolean xuexiIsCaptured() {
        return ((AbstractArrow) (Object) this).getEntityData().get(XUEXI_CAPTURED);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void xuexiDefineCaptured(CallbackInfo callback) {
        ((AbstractArrow) (Object) this).getEntityData().define(XUEXI_CAPTURED, Boolean.FALSE);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void xuexiHeldTick(CallbackInfo callback) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (!arrow.level().isClientSide() && ArrowSupport.supported(arrow)) {
            ArrowFlight.refreshExit(arrow);
        }
        if (arrow.level().isClientSide()) {
            if (this.xuexiIsCaptured()) {
                callback.cancel();
            }
            return;
        }
        if (this.xuexiIsCaptured() || ArrowBook.captured(arrow.getPersistentData())) {
            if (ArrowCaptureBook.holding(arrow.getUUID())) {
                ArrowCaptureBook.advance(arrow);
            } else if (ArrowBook.captured(arrow.getPersistentData())) {
                ArrowCaptureBook.resumeLoaded(arrow);
            } else {
                this.xuexiSetCaptured(false);
            }
            callback.cancel();
            return;
        }
        if (ArrowSupport.supported(arrow)) {
            ArrowTickState.beginTick(arrow.getUUID());
        }
    }

    @Inject(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;findHitEntity(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/EntityHitResult;"),
            cancellable = true)
    private void xuexiBeforeHitSearch(CallbackInfo callback) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (arrow.level().isClientSide() || !ArrowSupport.supported(arrow) || arrow.isNoPhysics() || arrow.isRemoved()) {
            return;
        }
        if (this.xuexiIsCaptured() || ArrowCaptureBook.holding(arrow.getUUID())) {
            callback.cancel();
            return;
        }
        Vec3 motion = arrow.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-8D) {
            return;
        }
        Vec3 from = arrow.position();
        Vec3 toward = from.add(motion);
        boolean blockLimited = false;
        BlockHitResult clip = arrow.level().clip(new ClipContext(from, toward, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, arrow));
        if (clip.getType() != HitResult.Type.MISS) {
            toward = clip.getLocation();
            blockLimited = true;
        }
        EntityHitResult legal = ProjectileUtil.getEntityHitResult(arrow.level(), arrow, from, toward,
                arrow.getBoundingBox().expandTowards(motion).inflate(1.0D), this::xuexiAccept);
        RandomSource randomSource = ((EntityRandomAccessor) (Object) this).xuexiRandom();
        if (ArrowFlight.consider(arrow, from, toward, blockLimited, legal, randomSource::nextInt)) {
            callback.cancel();
        }
    }

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void xuexiPassExcluded(Entity target, CallbackInfoReturnable<Boolean> callback) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (target != null && ArrowBook.excludes(arrow.getPersistentData(), target.getUUID())) {
            callback.setReturnValue(false);
        }
    }

    @Redirect(method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int xuexiSkipRepeatCrit(RandomSource source, int bound) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (ArrowBook.replacesHurt(arrow.getPersistentData(), arrow.getUUID())) {
            return 0;
        }
        return source.nextInt(bound);
    }

    @ModifyArg(method = "onHitEntity",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            index = 1)
    private float xuexiFrozenHurt(float amount) {
        return ArrowFlight.hurtAmount((AbstractArrow) (Object) this, amount);
    }

    @Redirect(method = "onHitEntity",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean xuexiArrowHurt(Entity target, DamageSource source, float amount) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (SwordGuardEffect.parryDirectArrow(arrow, target)) {
            return false;
        }
        boolean partial = ArrowTickState.consumeSuppress(arrow.getUUID());
        MitigationCreditScope.Token token = partial
                ? MitigationCreditScope.open(target, source, true)
                : MitigationCreditScope.Token.NONE;
        try {
            return target.hurt(source, amount);
        } finally {
            token.close();
            ArrowBook.consumePending(arrow.getUUID());
        }
    }

    @Unique
    private boolean xuexiAccept(Entity candidate) {
        if (candidate == null || !this.canHitEntity(candidate)) {
            return false;
        }
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (candidate instanceof Player victim && arrow.getOwner() instanceof Player shooter) {
            return shooter.canHarmPlayer(victim);
        }
        return true;
    }
}
