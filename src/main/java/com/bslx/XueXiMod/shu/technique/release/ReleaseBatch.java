package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceHitCommit;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneSourceScope;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.SettlementGuard;
import com.bslx.XueXiMod.shu.technique.release.domain.AttackEnchantmentContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraResolver;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseInputMode;
import com.bslx.XueXiMod.shu.technique.release.payload.FlowSuccessPayload;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime;
import com.bslx.XueXiMod.shu.technique.release.payload.WetPressure;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 一次真实释放的线程内账本。保存落点、宿主配置和延迟结算。不拆链，也不直接生成命中体。 */
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

    /** 记下持续释放使用的槽。本文件不解释槽的来源。 */
    public void bindSustainSlot(int slot) {
        this.sustainSlot = slot;
    }

    /** 返回已记的持续槽。未记时返回 -1。 */
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

    /** 绑定当前线程正在使用的宿主配置。传入空则清除。 */
    public static void bindActiveOffer(ExplicitHostOffer offer) {
        if (offer == null) {
            ACTIVE_OFFER.remove();
        } else {
            ACTIVE_OFFER.set(offer);
        }
    }

    /** 返回当前线程的宿主配置。没有时返回空。 */
    public static ExplicitHostOffer activeOffer() {
        return ACTIVE_OFFER.get();
    }

    /** 返回当前宿主配置里的计划。没有配置时返回空。 */
    public static BoundReleasePlan activePlan() {
        ExplicitHostOffer offer = ACTIVE_OFFER.get();
        return offer == null ? null : offer.plan();
    }

    /** 返回当前宿主配置里的附魔快照。没有配置时返回空。 */
    public static EnchantmentSnapshot activeSnapshot() {
        ExplicitHostOffer offer = ACTIVE_OFFER.get();
        return offer == null ? null : offer.snapshot();
    }

    /** 打开账本。技能里有落点索敌时记锚点需求，并绑定到当前线程。 */
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

    /**
     * 领域和攻击共用这一次落点。已绑定的账本优先，避免再打一条准星射线。
     * 空间索敌才启用锚点射线；否则用同一套准星中心。
     */
    public static Vec3 sharedLanding(ServerPlayer caster, boolean spaceSeek) {
        ReleaseBatch bound = CURRENT.get();
        if (bound != null) {
            return spaceSeek ? bound.resolveAnchor(caster) : bound.crosshairCenter(caster);
        }
        ReleaseBatch probe = new ReleaseBatch(caster);
        if (spaceSeek) {
            probe.wantAnchor = true;
            return probe.resolveAnchor(caster);
        }
        return probe.crosshairCenter(caster);
    }

    /** 返回当前线程的账本。没有绑定时返回空。 */
    public static ReleaseBatch current() {
        return CURRENT.get();
    }

    /** 绑定账本到当前线程。传入空则清除。 */
    public static void bind(ReleaseBatch batch) {
        if (batch == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(batch);
        }
    }

    /** 清掉当前线程的账本。 */
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

    /** 自动来源直接返回本轮原点。否则在视线里找最近生物，找不到时用方块命中点或视线终点。 */
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

    /** 新建一份独立的延迟结算账。 */
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
        private UUID armedRoot;
        private ResourceEnchantLoad armedLoad = ResourceEnchantLoad.EMPTY;
        private int armedBody;
        private int armedLegacy;
        private Completion completion;
        private SettlementGuard settlementGuard;
        private boolean guardDropped;
        private final Map<UUID, Set<Integer>> copyIndexes = new HashMap<>();
        private final Set<String> notifiedPairs = new HashSet<>();
        private final Map<UUID, HitVerdict> settledVerdicts = new HashMap<>();
        public java.util.UUID vineActionId;
        public boolean vineSpectrumEligible;

        private Tally() {
        }

        /** 记一次命中。潮汐和水流载荷按无，火种点数按 0。 */
        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval) {
            record(target, damage, copyIndex, hitInterval, TideMarkSuccess.NONE);
        }

        /** 记一次带潮汐结果的命中。水流载荷为空，火种点数为 0。 */
        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide) {
            record(target, damage, copyIndex, hitInterval, tide, null, 0);
        }

        /** 记一次带水流载荷的命中。火种点数为 0。 */
        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide,
                           FlowSuccessPayload flowPayload) {
            record(target, damage, copyIndex, hitInterval, tide, flowPayload, 0);
        }

        /** 接受守卫和目标上限后入账。正命中间隔按轮次去重，并在固定延迟后安排结算。 */
        public void record(LivingEntity target, float damage, int copyIndex, int hitInterval, TideMarkSuccess tide,
                           FlowSuccessPayload flowPayload, int seedPoints) {
            long tallyRecordStart = PerformanceRecorder.begin();
            try {
            if (this.guardDropped) {
                return;
            }
            if (HarmPolicy.denies(player, target)) {
                return;
            }
            if (this.settlementGuard != null) {
                if (!(target.level() instanceof ServerLevel serverLevel)
                        || !this.settlementGuard.active(serverLevel)
                        || !this.settlementGuard.allows(serverLevel, target)) {
                    return;
                }
                if (!this.targets.containsKey(target.getUUID())
                        && this.targets.size() >= CooperativeStructureLimits.TALLY_TARGET_CAP) {
                    return;
                }
            }
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
            this.copyIndexes.computeIfAbsent(target.getUUID(), ignored -> new HashSet<>()).add(copyIndex);
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

        /** 标记这本账结算时走尊重落点的命中分类。 */
        public void respectLanding() {
            this.respectLanding = true;
        }

        /** 保存结算完成后的回调。后一次调用覆盖前一次。 */
        public void whenComplete(Completion completion) {
            this.completion = completion;
        }

        /** 挂上结算守卫。守卫失效或不允许目标时，记录和结算都会停下。 */
        public void bindSettlementGuard(SettlementGuard guard) {
            this.settlementGuard = guard;
        }

        /** 丢弃尚未结算的守卫账，后续记录直接返回。 */
        public void dropGuardedSettlement() {
            this.guardDropped = true;
            this.flushPending = false;
        }

        /** 给这本账装上资源附魔提交所需的根释放、载荷和点数。空载荷改记为空载荷。 */
        public void armResource(UUID rootCastId, ResourceEnchantLoad load,
                                int bodyScaledBase, int legacyFixedPoints) {
            this.resourceArmed = true;
            this.armedRoot = rootCastId;
            this.armedLoad = load == null ? ResourceEnchantLoad.EMPTY : load;
            this.armedBody = Math.max(0, bodyScaledBase);
            this.armedLegacy = Math.max(0, legacyFixedPoints);
        }

        /** 返回目标已结算的判定。目标为空或尚未写入时返回待定。 */
        public HitVerdict settledVerdict(UUID targetId) {
            return targetId == null ? HitVerdict.PENDING : settledVerdicts.getOrDefault(targetId, HitVerdict.PENDING);
        }

        /** 返回已安排的结算刻。尚未安排时返回最大 int。 */
        public int flushTick() {
            return flushTick;
        }

        /** 每个目标和复制序号结算一次的回调。 */
        public interface Completion {
            void onSettled(UUID targetId, LivingEntity target, HitVerdict verdict, int copyIndex);
        }

        private void notifyCopies(UUID targetId, LivingEntity target, HitVerdict verdict) {
            if (targetId == null || verdict == null) {
                return;
            }
            if (this.resourceArmed && target != null) {
                FireCastContext cast = RootCastBindings.lookup(this.armedRoot);
                if (cast != null) {
                    ResourceHitCommit.commit(
                            target, cast, player, this.armedLoad, this.armedBody, this.armedLegacy,
                            verdict.grantsSeeds());
                }
            }
            if (this.completion == null) {
                return;
            }
            Set<Integer> copies = this.copyIndexes.get(targetId);
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

        private void notifyOne(UUID targetId, LivingEntity target, HitVerdict verdict, int copyIndex) {
            String pairKey = targetId + ":" + copyIndex;
            if (!this.notifiedPairs.add(pairKey)) {
                return;
            }
            try {
                this.completion.onSettled(targetId, target, verdict, copyIndex);
            } catch (Throwable thrown) {
                DebugLog.log("账本完成回调异常：" + thrown);
            }
        }

        private boolean ownerReady() {
            return player != null && player.isAlive() && !player.isRemoved();
        }

        private void flush() {
            long tallyFlushStart = PerformanceRecorder.begin();
            try {
            if (!flushPending || this.guardDropped) {
                flushPending = false;
                return;
            }
            if (this.settlementGuard != null
                    && (player == null || player.serverLevel() == null
                    || !this.settlementGuard.active(player.serverLevel()))) {
                flushPending = false;
                return;
            }
            CloneSourceScope sourceScope = null;
            if (this.settlementGuard != null && player.getServer() != null) {
                sourceScope = CloneSourceScope.open(
                        player.getServer(),
                        CloneSourceScope.Origin.PLAYER_MANUAL,
                        player.getUUID(), 1);
            }
            try {
            flushPending = false;
            for (UUID targetId : this.flowLedger.targets()) {
                HitVerdict verdict = HitVerdict.INVALID_OR_CANCELLED;
                LivingEntity settledTarget = null;
                try {
                int marks = this.flowLedger.takeMarks(targetId);
                LivingEntity target = targets.get(targetId);
                Float damage = this.flowLedger.damage(targetId);
                if (target == null || !target.isAlive() || damage == null) {
                    this.retireSeeds(targetId, null, false);
                    continue;
                }
                if (HarmPolicy.denies(player, target)) {
                    this.retireSeeds(targetId, target, false);
                    continue;
                }
                if (this.settlementGuard != null
                        && (!(target.level() instanceof ServerLevel guardedLevel)
                        || !this.settlementGuard.allows(guardedLevel, target))) {
                    this.retireSeeds(targetId, target, false);
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
                        verdict = TechniqueHits.classifyConfirmed(target, player, mergedAmount,
                                TechniqueHits.Landing.RESPECT_INVULNERABILITY, tide);
                    }
                    if (verdict.grantsSeeds()) {
                        flowPayload.apply(target);
                    }
                } else if (this.respectLanding) {
                    verdict = TechniqueHits.classifyRespecting(target, player, total, tide);
                } else {
                    verdict = TechniqueHits.classifyRoot(target, player, total, tide);
                }
                this.retireSeeds(targetId, target, verdict.grantsSeeds());
                WetPressure.afterFlushed(this, target, verdict.grantsSeeds());
                if (this.vineActionId != null) {
                    boolean positive = verdict == HitVerdict.DAMAGE_ACCEPTED;
                    boolean canceled = verdict == HitVerdict.INVALID_OR_CANCELLED;
                    VineMarkRuntime.offerQualifiedHit(player, target, this.vineActionId, canceled, positive,
                            this.vineSpectrumEligible);
                }
                } finally {
                    this.settledVerdicts.put(targetId, verdict);
                    this.notifyCopies(targetId, settledTarget, verdict);
                }
            }
            this.flowLedger.clearSettled();
            targets.clear();
            } finally {
                if (sourceScope != null) {
                    sourceScope.close();
                }
            }
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
            Iterator<Map.Entry<String, Integer>> iterator = this.pendingSeeds.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Integer> entry = iterator.next();
                String seedKey = entry.getKey();
                if (seedKey == null || !seedKey.startsWith(prefix)) {
                    continue;
                }
                Integer stored = entry.getValue();
                if (!this.grantedSeeds.contains(seedKey) && stored != null && stored > points) {
                    points = stored;
                }
                iterator.remove();
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
