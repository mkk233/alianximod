package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.deflection.melee.NonPhysicalMeleeScope;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * GuardianAttackGoal.tick 里两次 LivingEntity.hurt 描述符相同。
 * 字节码 186 是 indirectMagic，序号 0；字节码 216 是守卫者自己的 mobAttack，序号 1。
 * 只包住第二次。原 hurt 仍调用一次，并原样返回；射线伤害不取消。
 * 远古守卫者复用这个内部目标类，因此同一段射线同样排除。真实近战不在这个调用点里。
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Guardian$GuardianAttackGoal")
public class GuardianBeamMeleeScopeMixin {
    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    ordinal = 1))
    private boolean xuexiGuardianBeamMobAttack(LivingEntity victim, DamageSource damageSource, float amount) {
        NonPhysicalMeleeScope.Token beamToken = NonPhysicalMeleeScope.open(victim, damageSource);
        try {
            return victim.hurt(damageSource, amount);
        } finally {
            beamToken.close();
        }
    }
}
