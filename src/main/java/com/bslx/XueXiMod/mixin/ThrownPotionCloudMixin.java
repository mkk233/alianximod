package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.ThrownPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 滞留药云加入世界前继承药水已确认的玩家来源。 */
@Mixin(ThrownPotion.class)
public class ThrownPotionCloudMixin {
    @Redirect(method = "makeAreaOfEffectCloud",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean xuexiInheritPotionOwner(net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity cloud) {
        if (cloud instanceof AreaEffectCloud area) {
            CombatOwnership.transfer(area, (ThrownPotion) (Object) this);
        }
        return level.addFreshEntity(cloud);
    }
}
