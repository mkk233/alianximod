package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.debug.performance.PerfCounter;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
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

import com.bslx.XueXiMod.status.fire.seed.FireCastContext;

import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;

import java.util.ArrayList;
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
    private FireCastContext fireContext;
    private boolean surfaceClaimed;
    private int sustainSlot = -1;

    private ReleaseBatch(ServerPlayer player) {
        this.player = player;
    }

    /** 同一次真实释放的复制份共用这个对象，不按标识另造账本。 */
    public FireCastContext fireContext() {
        if (this.fireContext == null) {
            this.fireContext = new FireCastContext(UUID.randomUUID(), this.player.getUUID());
        }
        return this.fireContext;
    }

    /** 同一本账只铺一片地表火场。 */
    public boolean claimSurfaceFire() {
        if (this.surfaceClaimed) {
            return false;
        }
        this.surfaceClaimed = true;
        return true;
    }

    public void bindSustainSlot(int slot) {
        this.sustainSlot = slot;
    }

    public int sustainSlot() {
        return this.sustainSlot;
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
        long targetQueryStart = PerformanceRecorder.begin();
        try {
            BlockHitResult blockHit = caster.serverLevel().clip(new ClipContext(
                    eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            Vec3 limit = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : far;
            AABB search = new AABB(eye, limit).inflate(1.5);
            List<LivingEntity> entities = caster.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                    entity -> entity != caster && entity.isAlive() && !entity.isSpectator());
            if (PerformanceRecorder.isRecording()) {
                PerformanceRecorder.add(PerfCounter.TARGET_QUERY_CALLS, 1L);
                PerformanceRecorder.add(PerfCounter.TARGET_CANDIDATES, entities.size());
            }
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
        } finally {
            PerformanceRecorder.end(PerfMetric.TARGET_QUERY, targetQueryStart);
        }
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
        long targetQueryStart = PerformanceRecorder.begin();
        try {
            Vec3 end = eye.add(look.scale(ANCHOR_RANGE));
            AABB search = new AABB(eye, end).inflate(1.5);
            List<LivingEntity> entities = caster.serverLevel().getEntitiesOfClass(LivingEntity.class, search,
                    entity -> entity != caster && entity.isAlive() && !entity.isSpectator());
            if (PerformanceRecorder.isRecording()) {
                PerformanceRecorder.add(PerfCounter.TARGET_QUERY_CALLS, 1L);
                PerformanceRecorder.add(PerfCounter.TARGET_CANDIDATES, entities.size());
            }
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
        } finally {
            PerformanceRecorder.end(PerfMetric.TARGET_QUERY, targetQueryStart);
        }
    }

    public Tally openGroupTally() {
        return new Tally();
    }

    public final class Tally {
        private final FlowRoundLedger flowLedger = new FlowRoundLedger();
        private final Map<UUID, LivingEntity> targets = new HashMap<>();
        private final Map<String, Integer> pendingSeeds = new HashMap<>();
        private final Set<String> grantedSeeds = new HashSet<>();
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
            record(target, damage, copyIndex, hitInterval, tide, null, 0);
        }

        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide,
                           FlowSuccessPayload flowPayload) {
            record(target, damage, copyIndex, hitInterval, tide, flowPayload, 0);
        }

        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide,
                           FlowSuccessPayload flowPayload, int seedPoints) {
            long tallyRecordStart = PerformanceRecorder.begin();
            try {
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
            this.rememberSeed(target.getUUID(), roundIndex, seedPoints);
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
            } finally {
                PerformanceRecorder.end(PerfMetric.TALLY_RECORD, tallyRecordStart);
            }
        }

        public int flushTick() {
            return flushTick;
        }

        private boolean ownerReady() {
            return player != null && player.isAlive() && !player.isRemoved();
        }

        private void flush() {
            long tallyFlushStart = PerformanceRecorder.begin();
            try {
            if (!flushPending) {
                return;
            }
            flushPending = false;
            for (UUID targetId : this.flowLedger.targets()) {
                int marks = this.flowLedger.takeMarks(targetId);
                LivingEntity target = targets.get(targetId);
                Float damage = this.flowLedger.damage(targetId);
                if (target == null || !target.isAlive() || damage == null) {
                    this.retireSeeds(targetId, null, false);
                    continue;
                }
                float total = this.flowLedger.aggregatedDamage(targetId);
                TideMarkSuccess tide = TideMarkSuccess.of(marks, player.getUUID());
                FlowSuccessPayload flowPayload = this.flowLedger.payload(targetId);
                if (flowPayload != null) {
                    if (!this.ownerReady()) {
                        this.retireSeeds(targetId, target, false);
                        continue;
                    }
                    float mergedAmount = DomainAuraResolver.merge(total, player,
                            AttackEnchantmentContext.Kind.ROOT_TECHNIQUE);
                    boolean precise = TechniqueHits.hurtConfirmed(target, player, mergedAmount,
                            TechniqueHits.Landing.RESPECT_INVULNERABILITY, tide);
                    if (precise) {
                        flowPayload.apply(target);
                    }
                    this.retireSeeds(targetId, target, precise);
                    com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, precise);
                } else {
                    boolean landed = TechniqueHits.hurtRoot(target, player, total, tide);
                    this.retireSeeds(targetId, target, landed);
                    com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, landed);
                }
            }
            this.flowLedger.clearSettled();
            targets.clear();
            } finally {
                PerformanceRecorder.end(PerfMetric.TALLY_FLUSH, tallyFlushStart);
            }
        }

        private void rememberSeed(UUID targetId, int roundIndex, int seedPoints) {
            if (targetId == null || seedPoints <= 0) {
                return;
            }
            String seedKey = targetId + "#" + roundIndex;
            if (this.grantedSeeds.contains(seedKey)) {
                return;
            }
            this.pendingSeeds.putIfAbsent(seedKey, seedPoints);
        }

        private void retireSeeds(UUID targetId, LivingEntity target, boolean accepted) {
            if (targetId == null) {
                return;
            }
            String prefix = targetId.toString() + "#";
            int points = 0;
            ArrayList<String> matched = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : this.pendingSeeds.entrySet()) {
                String seedKey = entry.getKey();
                if (seedKey == null || !seedKey.startsWith(prefix)) {
                    continue;
                }
                matched.add(seedKey);
                Integer stored = entry.getValue();
                if (!this.grantedSeeds.contains(seedKey) && stored != null && stored > points) {
                    points = stored;
                }
            }
            for (String seedKey : matched) {
                this.pendingSeeds.remove(seedKey);
                this.grantedSeeds.add(seedKey);
            }
            if (accepted && points > 0) {
                FireSeedGrant.deposit(target, player, points);
            }
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
