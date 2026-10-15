package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MagicArcaneMissileEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        boolean hasFire = cast.enchantments().has("fire");
        EnchantmentAccumulator fire = hasFire ? cast.enchantments().effect("fire") : null;

        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        float speed = definition != null ? definition.speed() : 1.5f;
        int lifetime = definition != null ? definition.lifetimeTicks() : 40;

        if (player.level() instanceof ServerLevel serverLevel) {
            Consumer<Vec3> trigger = cast.onFirstTrigger();
            AtomicBoolean triggered = new AtomicBoolean(false);
            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.techniqueId = technique.getId();
            spec.path = TechniqueHitSpec.Path.STRAIGHT;
            spec.length = 0.3;
            spec.width = 0.3;
            spec.height = 0.3;
            spec.speed = speed;
            spec.lifetime = lifetime;
            spec.lockThisCast = cast.lockThisCast();
            spec.dropThisCast = cast.dropThisCast();
            spec.visual = 1;
            spec.listener = new TechniqueHitSpec.HitListener() {
                @Override
                public void onHit(LivingEntity target) {
                    if (hasFire) {
                        int seconds = fire.durationSeconds();
                        AABB box = target.getBoundingBox().inflate(fire.radius());
                        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                                nearbyEntity -> nearbyEntity != target && nearbyEntity.isAlive());
                        for (LivingEntity nearbyEntity : nearby) {
                            nearbyEntity.setSecondsOnFire(seconds);
                        }
                    }
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
