package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.registry.ModMobEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 输入生成之后、运动计算之前清掉蔷薇束缚的主动输入。 */
@Mixin(LivingEntity.class)
public abstract class RoseLivingInputMixin {
    @Accessor("xxa")
    public abstract void xuexiSetStrafeInput(float strafeInput);

    @Accessor("yya")
    public abstract void xuexiSetVerticalInput(float verticalInput);

    @Accessor("zza")
    public abstract void xuexiSetForwardInput(float forwardInput);

    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;serverAiStep()V",
            shift = At.Shift.AFTER))
    private void xuexiRoseAfterServerInput(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.hasEffect(ModMobEffects.ROSE_BIND.get()) || entity instanceof LocalPlayer) {
            return;
        }
        xuexiRoseClearVoluntary(entity);
    }

    private static void xuexiRoseClearVoluntary(LivingEntity entity) {
        RoseLivingInputMixin input = (RoseLivingInputMixin) (Object) entity;
        input.xuexiSetStrafeInput(0.0f);
        input.xuexiSetVerticalInput(0.0f);
        input.xuexiSetForwardInput(0.0f);
        entity.setJumping(false);
        entity.setSprinting(false);
        if (entity instanceof Mob mob) {
            mob.setXxa(0.0f);
            mob.setYya(0.0f);
            mob.setZza(0.0f);
        }
    }
}
