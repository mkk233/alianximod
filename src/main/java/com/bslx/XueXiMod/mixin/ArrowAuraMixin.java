package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowBook;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 箭命中时按射手现在的位置决定供给，不看发射那一刻。 */
@Mixin(AbstractArrow.class)
public class ArrowAuraMixin {
    @ModifyArg(method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            index = 1)
    private float xuexiDomainArrow(float amount) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        if (ArrowBook.replacesHurt(arrow.getPersistentData(), arrow.getUUID())) {
            return amount;
        }
        Entity owner = arrow.getOwner();
        return DomainAuraResolver.merge(amount, owner, AttackEnchantmentContext.Kind.ROOT_ARROW);
    }
}
