package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ParasiteSeedEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Consumer<Vec3> trigger = cast.onFirstTrigger();
        AtomicBoolean triggered = new AtomicBoolean(false);
        for (int i = 0; i < 3; i++) {
            double angle = i * Math.PI * 2.0 / 3.0;
            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.techniqueId = technique.getId();
            spec.path = TechniqueHitSpec.Path.DROP;
            spec.trap = true;
            spec.speed = 0.0;
            spec.length = 0.5;
            spec.width = 0.5;
            spec.height = 0.5;
            spec.lifetime = 160;
            spec.visual = 3;
            spec.localOffset = new Vec3(Math.cos(angle) * 1.2, 0.0, Math.sin(angle) * 1.2);
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
}
