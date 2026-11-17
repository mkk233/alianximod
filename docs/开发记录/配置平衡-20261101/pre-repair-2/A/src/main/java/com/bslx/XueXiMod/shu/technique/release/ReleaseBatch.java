package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseInputMode;
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
    private final Map<String, ExplicitHostOffer> explicitHosts = new HashMap<>();
    private String hostLane = "";
    private static final ThreadLocal<ExplicitHostOffer> ACTIVE_OFFER = new ThreadLocal<>();

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

    /**
     * 按通道加槽位记下宿主配置。读取不删除，同一槽的复制份都能拿到。
     * 通道分开普通链和条件组，避免两边的局部下标撞在一起。
     */
    public void rememberExplicitHost(String lane, int actionIndex, BoundReleasePlan plan, EnchantmentSnapshot snapshot) {
        if (lane == null || lane.isEmpty() || actionIndex < 0) {
            return;
        }
        if (this.hostLane.isEmpty()) {
            this.hostLane = lane;
        }
        this.explicitHosts.put(lane + "#" + actionIndex, new ExplicitHostOffer(plan, snapshot));
    }

    /** 当前这本账的通道上，这一槽的配置。没有就返回空，不取出。 */
    public ExplicitHostOffer explicitOffer(int actionIndex) {
        if (actionIndex < 0 || this.hostLane.isEmpty()) {
            return null;
        }
        return this.explicitHosts.get(this.hostLane + "#" + actionIndex);
    }

    public static void bindActiveOffer(ExplicitHostOffer offer) {
        if (offer == null) {
            ACTIVE_OFFER.remove();
        } else {
            ACTIVE_OFFER.set(offer);
        }
    }

    public static ExplicitHostOffer activeOffer() {
        return ACTIVE_OFFER.get();
    }

    public static BoundReleasePlan activePlan() {
        ExplicitHostOffer offer = ACTIVE_OFFER.get();
        return offer == null ? null : offer.plan();
    }

    public static EnchantmentSnapshot activeSnapshot() {
        ExplicitHostOffer offer = ACTIVE_OFFER.get();
        return offer == null ? null : offer.snapshot();
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
        Vec3 forcedOrigin = automaticOrigin();
        if (forcedOrigin != null) {
            return forcedOrigin;
        }
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 far = eye.add(look.scale(32.0));
        BlockHitResult blockHit = caster.serverLevel().clip(new ClipContext(
                eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 limit = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : far;
        AABB search = new AABB(eye, limit).inflate(1.5);
        List<LivingEntity> entities = LivingCandidateQueries.targetQuery(caster.serverLevel(), search,
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
        Vec3 forcedOrigin = automaticOrigin();
        if (forcedOrigin != null) {
            return forcedOrigin;
        }
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
        List<LivingEntity> entities = LivingCandidateQueries.targetQuery(caster.serverLevel(), search,
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

    /** 自动轮直接用本轮原点，不射线、不写玩家标记，也不改手动锚点缓存。 */
    private static Vec3 automaticOrigin() {
        ReleaseExecutionContext currentContext = ReleaseExecutionContext.current();
        if (currentContext == null || currentContext.mode() != ReleaseInputMode.DOMAIN_AUTOMATIC) {
            return null;
        }
        return currentContext.origin();
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
        private boolean respectLanding;
        private boolean resourceArmed;
        private java.util.UUID armedRoot;
        private com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad armedLoad =
                com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad.EMPTY;
        private int armedBody;
        private int armedLegacy;
        private Completion completion;
        private final java.util.Map<UUID, java.util.Set<Integer>> copyIndexes = new java.util.HashMap<>();
        private final java.util.Set<String> notifiedPairs = new java.util.HashSet<>();
        private final java.util.Map<UUID, com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict> settledVerdicts =
                new java.util.HashMap<>();

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
            this.copyIndexes.computeIfAbsent(target.getUUID(), ignored -> new java.util.HashSet<>()).add(copyIndex);
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

        public void respectLanding() {
            this.respectLanding = true;
        }

        public void whenComplete(Completion completion) {
            this.completion = completion;
        }

        public void armResource(java.util.UUID rootCastId,
                                com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad load,
                                int bodyScaledBase, int legacyFixedPoints) {
            this.resourceArmed = true;
            this.armedRoot = rootCastId;
            this.armedLoad = load == null
                    ? com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad.EMPTY
                    : load;
            this.armedBody = Math.max(0, bodyScaledBase);
            this.armedLegacy = Math.max(0, legacyFixedPoints);
        }

        public com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict settledVerdict(UUID targetId) {
            if (targetId == null || !settledVerdicts.containsKey(targetId)) {
                return com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.PENDING;
            }
            return settledVerdicts.get(targetId);
        }

        public int flushTick() {
            return flushTick;
        }

        public interface Completion {
            void onSettled(UUID targetId, LivingEntity target,
                           com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict, int copyIndex);
        }

        private void notifyCopies(UUID targetId, LivingEntity target,
                                  com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict) {
            if (targetId == null || verdict == null) {
                return;
            }
            if (this.resourceArmed && target != null) {
                com.bslx.XueXiMod.status.fire.seed.FireCastContext cast =
                        com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings.lookup(this.armedRoot);
                if (cast != null) {
                    com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceHitCommit.commit(
                            target, cast, player, this.armedLoad, this.armedBody, this.armedLegacy,
                            verdict.grantsSeeds());
                }
            }
            if (this.completion == null) {
                return;
            }
            java.util.Set<Integer> copies = this.copyIndexes.get(targetId);
            if (copies == null || copies.isEmpty()) {
                this.notifyOne(targetId, target, verdict, 0);
                return;
            }
            for (Integer copyIndex : copies) {
                if (copyIndex != null) {
                    this.notifyOne(targetId, target, verdict, copyIndex);
                }
            }
        }

        private void notifyOne(UUID targetId, LivingEntity target,
                               com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict, int copyIndex) {
            String pairKey = targetId + ":" + copyIndex;
            if (!this.notifiedPairs.add(pairKey)) {
                return;
            }
            try {
                this.completion.onSettled(targetId, target, verdict, copyIndex);
            } catch (Throwable thrown) {
                com.bslx.XueXiMod.debug.DebugLog.log("账本完成回调异常：" + thrown);
            }
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
                com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict verdict =
                        com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.INVALID_OR_CANCELLED;
                LivingEntity settledTarget = null;
                try {
                int marks = this.flowLedger.takeMarks(targetId);
                LivingEntity target = targets.get(targetId);
                Float damage = this.flowLedger.damage(targetId);
                if (target == null || !target.isAlive() || damage == null) {
                    this.retireSeeds(targetId, null, false);
                    continue;
                }
                settledTarget = target;
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
                    if (this.respectLanding) {
                        verdict = TechniqueHits.classifyRespecting(target, player, mergedAmount, tide);
                    } else {
                        boolean precise = TechniqueHits.hurtConfirmed(target, player, mergedAmount,
                                TechniqueHits.Landing.RESPECT_INVULNERABILITY, tide);
                        verdict = precise
                                ? com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.DAMAGE_ACCEPTED
                                : com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.ESTABLISHED_NO_DAMAGE;
                    }
                    if (verdict.grantsSeeds()) {
                        flowPayload.apply(target);
                    }
                    this.retireSeeds(targetId, target, verdict.grantsSeeds());
                    com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, verdict.grantsSeeds());
                } else if (this.respectLanding) {
                    verdict = TechniqueHits.classifyRespecting(target, player, total, tide);
                    this.retireSeeds(targetId, target, verdict.grantsSeeds());
                    com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, verdict.grantsSeeds());
                } else {
                    boolean landed = TechniqueHits.hurtRoot(target, player, total, tide);
                    verdict = landed
                            ? com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.DAMAGE_ACCEPTED
                            : com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict.ESTABLISHED_NO_DAMAGE;
                    this.retireSeeds(targetId, target, landed);
                    com.bslx.XueXiMod.shu.technique.release.payload.WetPressure.afterFlushed(this, target, landed);
                }
                } finally {
                    this.settledVerdicts.put(targetId, verdict);
                    this.notifyCopies(targetId, settledTarget, verdict);
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
            if (accepted && points > 0 && !this.resourceArmed) {
                FireSeedGrant.deposit(target, player, points);
            }
        }
    }

    public record ExplicitHostOffer(BoundReleasePlan plan, EnchantmentSnapshot snapshot) {
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
