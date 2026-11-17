package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.ActionWindow;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class MovementDashEffect implements TechniqueEffect {
    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        PublicationAttempt attempt = PublicationAttempt.current();
        if (attempt != null && !attempt.allowsPublication()) {
            return;
        }
        Vec3 look = cast.getLook();
        Vec3 start = cast.getPos();
        float damage = cast.getDamage();

        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        float speed = definition != null ? definition.speed() : 2.0f;
        int duration = definition != null ? definition.lifetimeTicks() : 10;

        player.setDeltaMovement(look.scale(speed));
        player.hurtMarked = true;
        if (attempt != null) {
            attempt.reportAccepted();
        }

        if (cast.enchantments().has("fire")) {
            EnchantmentAccumulator fire = cast.enchantments().effect("fire");
            Vec3 target = start.add(look.scale(speed));
            AABB box = new AABB(start, target).inflate(0.5);
            List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity != player && entity.isAlive());
            for (LivingEntity entity : entities) {
                TechniqueHits.hurtRoot(entity, player, damage);
                entity.setSecondsOnFire(fire.durationSeconds());
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                for (int segmentIndex = 0; segmentIndex <= 4; segmentIndex++) {
                    double progress = segmentIndex / 4.0;
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                        start.x + look.x * progress * speed, start.y + 0.5 + look.y * progress * speed, start.z + look.z * progress * speed,
                        1, 0, 0, 0, 0);
                }
            }
        }

        ActionWindow.Sink sink = cast.actionSink();
        if (sink != null && player.level() instanceof ServerLevel serverLevel) {
            if (duration <= 0) {
                finishWindow(sink);
            } else {
                scheduleSweep(player, serverLevel, sink, 0, duration, new HashSet<>(), player.position(), player.getBoundingBox());
            }
            return;
        }
        Consumer<Vec3> onFirstTrigger = cast.onFirstTrigger();
        if (onFirstTrigger != null && player.level() instanceof ServerLevel serverLevel) {
            scheduleDashCollision(player, serverLevel, onFirstTrigger, 0, duration, new HashSet<>(), player.position());
        }
    }

    private void scheduleSweep(ServerPlayer player, ServerLevel level, ActionWindow.Sink sink, int tick, int duration,
                               Set<UUID> already, Vec3 previous, AABB previousBox) {
        ChainScheduler.schedule(level.getServer(), level.getServer().getTickCount() + 1, () -> {
            if (sink.closed() || !player.isAlive() || player.isRemoved() || player.hasDisconnected() || player.level() != level) {
                finishWindow(sink);
                return;
            }
            Vec3 now = player.position();
            AABB currentBox = player.getBoundingBox();
            Vec3 displacement = now.subtract(previous);
            boolean jumped = displacement.lengthSqr() > 64.0;
            if (!jumped && displacement.lengthSqr() > 1.0E-8) {
                AABB swept = previousBox.minmax(currentBox).inflate(0.05);
                List<LivingEntity> overlapping = level.getEntitiesOfClass(LivingEntity.class, swept,
                        entity -> ActionWindow.hostileTo(player, entity));
                List<SweepHit> hits = new ArrayList<>();
                for (LivingEntity entity : overlapping) {
                    if (already.contains(entity.getUUID())) {
                        continue;
                    }
                    double entryTime = BodySweep.entryAlong(previousBox, displacement, entity.getBoundingBox());
                    if (entryTime < 0.0) {
                        continue;
                    }
                    Vec3 center = previousBox.getCenter().add(displacement.scale(entryTime));
                    hits.add(new SweepHit(entity, entryTime, BodySweep.closest(entity.getBoundingBox(), center)));
                }
                hits.sort(Comparator.comparingDouble((SweepHit sweepHit) -> sweepHit.entryTime)
                        .thenComparing(sweepHit -> sweepHit.entity.getUUID()));
                Vec3 direction = displacement.normalize();
                for (SweepHit sweepHit : hits) {
                    if (already.add(sweepHit.entity.getUUID())) {
                        sink.onContact(sweepHit.entity.getUUID(), sweepHit.point, direction);
                    }
                }
            }
            if (tick + 1 < duration) {
                scheduleSweep(player, level, sink, tick + 1, duration, already, now, currentBox);
            } else {
                finishWindow(sink);
            }
        });
    }

    private static void finishWindow(ActionWindow.Sink sink) {
        if (sink == null) {
            return;
        }
        sink.onEnded();
        sink.onFinished();
    }

    private record SweepHit(LivingEntity entity, double entryTime, Vec3 point) {
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
                    entity -> entity != player && entity.isAlive());
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
