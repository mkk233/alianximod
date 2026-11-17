package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.shu.technique.release.payload.FlowSuccessPayload;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                var definition = ReleaseRegistry.get(technique.getId());
                if (definition != null && definition.role() == ChainRole.SEEK_SPACE) {
                    batch.wantAnchor = true;
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

    /** 弹丸的落点中心：准星打到的方块，光线上有生物则改用生物中心。不写入斩击锚点。 */
    public Vec3 crosshairCenter(ServerPlayer caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 far = eye.add(look.scale(32.0));
        BlockHitResult blockHit = caster.serverLevel().clip(new ClipContext(
                eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 limit = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : far;
        AABB search = new AABB(eye, limit).inflate(1.5);
        List<LivingEntity> entities = caster.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                entity -> entity != caster && entity.isAlive() && !entity.isSpectator());
        LivingEntity nearest = null;
        double bestDistance = 1.5;
        for (LivingEntity entity : entities) {
            double distance = distanceToSegment(entity.getBoundingBox().getCenter(), eye, limit);
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = entity;
            }
        }
        return nearest != null ? nearest.getBoundingBox().getCenter() : limit;
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
        private final FlowRoundLedger flowLedger = new FlowRoundLedger();
        private final Map<UUID, LivingEntity> targets = new HashMap<>();
        private int originTick;
        private boolean originReady;
        private int flushTick = Integer.MAX_VALUE;
        private boolean flushPending;

        private Tally() {
        }

        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval) {
            record(target, damage, copyIndex, hitInterval, TideMarkSuccess.NONE);
        }

        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide) {
            record(target, damage, copyIndex, hitInterval, tide, null);
        }

        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide,
                           FlowSuccessPayload flowPayload) {
            int roundIndex = 0;
            if (hitInterval > 0 && player.getServer() != null) {
                int nowTick = player.getServer().getTickCount();
                if (!originReady) {
                    originTick = nowTick;
                    originReady = true;
                }
                roundIndex = FlowRoundLedger.roundIndex(hitInterval, originTick, nowTick);
            }
            int marks = tide == null ? 0 : tide.marks();
            if (!this.flowLedger.accept(target.getUUID(), roundIndex, copyIndex, damage, marks, flowPayload)) {
                return;
            }
            targets.put(target.getUUID(), target);
            if (player.getServer() == null) {
                return;
            }
            int now = player.getServer().getTickCount();
            if (flushPending && flushTick >= now) {
                return;
            }
            flushTick = now + FlowRoundLedger.FLUSH_DELAY;
            flushPending = true;
            ChainScheduler.schedule(player.getServer(), flushTick, this::flush);
        }

        public int flushTick() {
            return flushTick;
        }

        private boolean ownerReady() {
            return player != null && player.isAlive() && !player.isRemoved();
        }

        private void flush() {
            if (!flushPending) {
                return;
            }
            flushPending = false;
            for (UUID targetId : this.flowLedger.targets()) {
                int marks = this.flowLedger.takeMarks(targetId);
                LivingEntity target = targets.get(targetId);
                Float damage = this.flowLedger.damage(targetId);
                if (target == null || !target.isAlive() || damage == null) {
                    continue;
                }
                float total = this.flowLedger.aggregatedDamage(targetId);
                TideMarkSuccess tide = TideMarkSuccess.of(marks, player.getUUID());
                FlowSuccessPayload flowPayload = this.flowLedger.payload(targetId);
                if (flowPayload != null) {
                    if (!this.ownerReady()) {
                        continue;
                    }
                    float mergedAmount = DomainAuraResolver.merge(total, player,
                            AttackEnchantmentContext.Kind.ROOT_TECHNIQUE);
                    boolean precise = TechniqueHits.hurtConfirmed(target, player, mergedAmount,
                            TechniqueHits.Landing.RESPECT_INVULNERABILITY, tide);
                    if (precise) {
                        flowPayload.apply(target);
                    }
                    com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, precise);
                } else {
                    boolean landed = TechniqueHits.hurtRoot(target, player, total, tide);
                    com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, landed);
                }
            }
            this.flowLedger.clearSettled();
            targets.clear();
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 span = end.subtract(start);
        double length2 = span.lengthSqr();
        if (length2 < 1.0E-6) {
            return point.distanceTo(start);
        }
        double scaleFactor = Math.max(0.0, Math.min(1.0, point.subtract(start).dot(span) / length2));
        return point.distanceTo(start.add(span.scale(scaleFactor)));
    }
}
