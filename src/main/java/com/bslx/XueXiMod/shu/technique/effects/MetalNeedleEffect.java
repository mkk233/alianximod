package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MetalNeedleEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STRAIGHT;
        spec.length = 0.4;
        spec.width = 0.2;
        spec.height = 0.2;
        spec.speed = definition != null ? definition.speed() : 1.5;
        spec.lifetime = definition != null ? definition.lifetimeTicks() : 15;
        spec.lockThisCast = cast.lockThisCast();
        spec.dropThisCast = cast.dropThisCast();
        spec.visual = 1;
        spec.listener = new TechniqueHitSpec.HitListener() {
            @Override
            public void onHit(LivingEntity target) {
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(target.position());
                }
            }

            @Override
            public void onExpire(Vec3 position) {
                if (trigger != null && triggered.compareAndSet(false, true)) {
                    trigger.accept(position);
                }
            }
        };
        TechniqueHitSpec.spawn(serverLevel, player, spec);
    }
}
