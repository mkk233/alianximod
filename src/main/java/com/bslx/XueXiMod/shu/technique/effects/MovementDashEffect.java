package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class MovementDashEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        Vec3 look = cast.getLook();
        Vec3 pos = cast.getPos();
        float damage = cast.getDamage();

        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        float speed = definition != null ? definition.speed() : 2.0f;
        int duration = definition != null ? definition.lifetimeTicks() : 10;

        player.setDeltaMovement(look.scale(speed));
        player.hurtMarked = true;

        if (cast.enchantments().has("fire")) {
            EnchantmentAccumulator fire = cast.enchantments().effect("fire");
            Vec3 target = pos.add(look.scale(speed));
            AABB box = new AABB(pos, target).inflate(0.5);
            List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive());
            for (LivingEntity e : entities) {
                e.hurt(player.level().damageSources().playerAttack(player), damage);
                e.setSecondsOnFire(fire.durationSeconds());
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i <= 4; i++) {
                    double t = i / 4.0;
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                        pos.x + look.x * t * speed, pos.y + 0.5 + look.y * t * speed, pos.z + look.z * t * speed,
                        1, 0, 0, 0, 0);
                }
            }
        }

        Consumer<Vec3> onFirstTrigger = cast.onFirstTrigger();
        if (onFirstTrigger != null && player.level() instanceof ServerLevel serverLevel) {
            scheduleDashCollision(player, serverLevel, onFirstTrigger, 0, duration, new HashSet<>(), player.position());
        }
    }

    private void scheduleDashCollision(ServerPlayer player, ServerLevel level, Consumer<Vec3> onFirstTrigger,
                                       int tick, int duration, Set<UUID> already, Vec3 previous) {
        ChainScheduler.schedule(level.getServer(), level.getServer().getTickCount() + 1, () -> {
            if (!player.isAlive()) {
                return;
            }
            Vec3 now = player.position();
            AABB swept = new AABB(previous, now).inflate(0.5).minmax(player.getBoundingBox());
            List<LivingEntity> overlapping = level.getEntitiesOfClass(LivingEntity.class, swept,
                    e -> e != player && e.isAlive());
            for (LivingEntity entity : overlapping) {
                if (already.add(entity.getUUID())) {
                    onFirstTrigger.accept(entity.position());
                }
            }
            if (tick + 1 < duration) {
                scheduleDashCollision(player, level, onFirstTrigger, tick + 1, duration, already, now);
            }
        });
    }
}
