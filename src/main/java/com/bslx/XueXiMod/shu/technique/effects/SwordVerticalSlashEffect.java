package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

public class SwordVerticalSlashEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Vec3 look = cast.getLook();
        Vec3 pos = cast.getPos();

        boolean hasFire = cast.enchantments().has("fire");
        EnchantmentAccumulator fire = hasFire ? cast.enchantments().effect("fire") : null;

        float range = hasFire ? 1.5f * fire.radiusMultiplier() : 1.5f;
        Consumer<Vec3> trigger = cast.onFirstTrigger();

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 effectPos = pos.add(look.scale(range));
            for (int i = 0; i < 12; i++) {
                double yOffset = 2.0 - (i * 0.2);
                double xOffset = (Math.random() - 0.5) * 0.15;
                double zOffset = (Math.random() - 0.5) * 0.15;

                serverLevel.sendParticles(ParticleTypes.CRIT,
                    effectPos.x + xOffset, effectPos.y + yOffset, effectPos.z + zOffset,
                    1, 0, 0, 0, 0.05);
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    effectPos.x + xOffset, effectPos.y + yOffset, effectPos.z + zOffset,
                    1, 0, 0, 0, 0.5);
            }
            for (int i = 0; i < 5; i++) {
                double burstX = (Math.random() - 0.5) * 0.8;
                double burstZ = (Math.random() - 0.5) * 0.8;
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    effectPos.x + burstX, effectPos.y + 0.1, effectPos.z + burstZ,
                    1, 0, 0.1, 0, 0.15);
            }
            if (hasFire) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                    effectPos.x, effectPos.y + 0.5, effectPos.z,
                    1, 0, 0.1, 0, 0.02);
            }
            serverLevel.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.8f);

            java.util.concurrent.atomic.AtomicBoolean triggered = new java.util.concurrent.atomic.AtomicBoolean(false);
            TechniqueHitSpec spec = new TechniqueHitSpec();
            spec.techniqueId = technique.getId();
            spec.path = TechniqueHitSpec.Path.STILL_AIM;
            spec.speed = 0.0;
            spec.length = range;
            spec.width = 1.6;
            spec.height = 2.0;
            spec.texture = "xueximod:textures/entity/xueximod/horizontal_slash.png";
            spec.lifetime = 4;
            spec.lockThisCast = false;
            spec.dropThisCast = false;
            spec.anchor = player.getEyePosition();
            spec.aim = look;
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
