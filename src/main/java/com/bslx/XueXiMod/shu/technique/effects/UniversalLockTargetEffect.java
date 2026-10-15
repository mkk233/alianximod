package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class UniversalLockTargetEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Vec3 pos = cast.getPos();
        Consumer<Vec3> trigger = cast.onFirstTrigger();

        LivingEntity target = LockTargetService.resolve(player);
        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
            if (trigger != null) {
                trigger.accept(target.position());
            }
        } else if (trigger != null) {
            trigger.accept(pos);
        }
    }
}
