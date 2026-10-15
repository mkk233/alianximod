package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class LifeDrain {
    private LifeDrain() {}

    public static void start(ServerLevel level, ServerPlayer caster, LivingEntity target, float damage, int times, int interval) {
        if (level.getServer() == null || caster == null || target == null) {
            return;
        }
        schedule(level, caster, target, damage, times, interval, 0);
    }

    private static void schedule(ServerLevel level, ServerPlayer caster, LivingEntity target, float damage, int times, int interval, int index) {
        if (index >= times) {
            return;
        }
        int runAt = level.getServer().getTickCount() + interval;
        ChainScheduler.schedule(level.getServer(), runAt, () -> {
            if (!target.isAlive()) {
                return;
            }
            target.hurt(target.level().damageSources().playerAttack(caster), damage);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            schedule(level, caster, target, damage, times, interval, index + 1);
        });
    }
}
