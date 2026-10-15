package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ReleaseBatch {
    private static final ThreadLocal<ReleaseBatch> CURRENT = new ThreadLocal<>();
    private static final double ANCHOR_RANGE = 8.0;
    private static final double ANCHOR_FALLBACK = 4.0;

    private final ServerPlayer player;
    private Vec3 anchor;
    private boolean wantAnchor;

    private ReleaseBatch(ServerPlayer player) {
        this.player = player;
    }

    public static ReleaseBatch open(ServerPlayer player, Skill skill) {
        ReleaseBatch batch = new ReleaseBatch(player);
        if (skill != null) {
            for (var technique : skill.getTechniques()) {
                switch (technique.getId()) {
                    case "rule_cast_anchor" -> batch.wantAnchor = true;
                    default -> {
                    }
                }
            }
        }
        CURRENT.set(batch);
        return batch;
    }

    public static ReleaseBatch current() {
        return CURRENT.get();
    }

    public static void bind(ReleaseBatch batch) {
        if (batch == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(batch);
        }
    }

    public static void unbind() {
        CURRENT.remove();
    }

    public Vec3 resolveAnchor(ServerPlayer caster) {
        if (anchor != null) {
            return anchor;
        }
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        if (!wantAnchor) {
            anchor = eye.add(look.scale(ANCHOR_FALLBACK));
            return anchor;
        }
        Vec3 end = eye.add(look.scale(ANCHOR_RANGE));
        AABB search = new AABB(eye, end).inflate(1.5);
        List<LivingEntity> entities = caster.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != caster && entity.isAlive() && !entity.isSpectator());
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity entity : entities) {
            double distance = distanceToSegment(entity.getBoundingBox().getCenter(), eye, end);
            if (distance < best) {
                best = distance;
                nearest = entity;
            }
        }
        anchor = nearest != null ? nearest.getBoundingBox().getCenter() : eye.add(look.scale(ANCHOR_FALLBACK));
        return anchor;
    }

    public Tally openGroupTally() {
        return new Tally();
    }

    public final class Tally {
        private final Map<UUID, Integer> counts = new HashMap<>();
        private final Map<UUID, Float> damages = new HashMap<>();
        private final Map<UUID, LivingEntity> targets = new HashMap<>();
        private final Map<UUID, Set<Integer>> seenCopies = new HashMap<>();
        private int flushTick = Integer.MAX_VALUE;
        private boolean flushPending;

        private Tally() {
        }

        public void record(LivingEntity target, float damage, int copyIndex) {
            Set<Integer> seen = seenCopies.computeIfAbsent(target.getUUID(), key -> new HashSet<>());
            if (!seen.add(copyIndex)) {
                return;
            }
            counts.merge(target.getUUID(), 1, Integer::sum);
            damages.put(target.getUUID(), damage);
            targets.put(target.getUUID(), target);
            if (player.getServer() == null) {
                return;
            }
            int now = player.getServer().getTickCount();
            if (flushPending && flushTick >= now) {
                return;
            }
            flushTick = now + 6;
            flushPending = true;
            ChainScheduler.schedule(player.getServer(), flushTick, this::flush);
        }

        public int flushTick() {
            return flushTick;
        }

        private void flush() {
            if (!flushPending) {
                return;
            }
            flushPending = false;
            for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
                LivingEntity target = targets.get(entry.getKey());
                Float damage = damages.get(entry.getKey());
                if (target == null || !target.isAlive() || damage == null) {
                    continue;
                }
                target.hurt(target.level().damageSources().playerAttack(player), damage * entry.getValue());
            }
            counts.clear();
            damages.clear();
            targets.clear();
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 span = end.subtract(start);
        double length2 = span.lengthSqr();
        if (length2 < 1.0E-6) {
            return point.distanceTo(start);
        }
        double t = Math.max(0.0, Math.min(1.0, point.subtract(start).dot(span) / length2));
        return point.distanceTo(start.add(span.scale(t)));
    }
}
