package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 命中结算之后的延迟扣血。不生成规格、不移动命中体，也不维护去重或账本。
 * 每次都走 {@link TechniqueHits#hurt}，默认绕过无敌帧。
 */
public final class LifeDrain {
    private LifeDrain() {}

    /** 目标、施法者或服务器缺一个就不排程。不在这里检查目标是否还活着。 */
    public static void start(ServerLevel level, ServerPlayer caster, LivingEntity target, float damage, int times, int interval) {
        if (level.getServer() == null || caster == null || target == null) {
            return;
        }
        schedule(level, caster, target, damage, times, interval, 0);
    }

    /** 用服务器 tick 排下一次。到点时目标已死就停，否则扣血后再排下一次。 */
    private static void schedule(ServerLevel level, ServerPlayer caster, LivingEntity target, float damage, int times, int interval, int index) {
        if (index >= times) {
            return;
        }
        int runAt = level.getServer().getTickCount() + interval;
        ChainScheduler.schedule(level.getServer(), runAt, () -> {
            if (!target.isAlive()) {
                return;
            }
            TechniqueHits.hurt(target, caster, damage);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            schedule(level, caster, target, damage, times, interval, index + 1);
        });
    }
}
