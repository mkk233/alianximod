package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.carrier.BatchTriggerVisuals;
import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.player.PlayerBoundSkills;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantCatalog;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout;
import com.bslx.XueXiMod.shu.technique.release.batch.CancelToken;
import com.bslx.XueXiMod.shu.technique.release.batch.CarrierHooks;
import com.bslx.XueXiMod.shu.technique.release.batch.HitClock;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.batch.HostHandoff;
import com.bslx.XueXiMod.shu.technique.release.batch.ImpactPartition;
import com.bslx.XueXiMod.shu.technique.release.batch.OcclusionSnapshot;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.shu.technique.release.batch.SurfaceAnchor;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSpawnInheritance;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 新链的费用和出生调度。伤害仍走现有宿主提交和命中结算，不另建一套攻击。
 * 松键不取消。死亡、下线、换维、换手或装配变化只停未出生的份和未触发的子段。
 */
public final class BatchCastSession {
    private static final ConcurrentHashMap<String, BatchCastSession> OPEN = new ConcurrentHashMap<>();

    private final ServerPlayer owner;
    private final ItemStack carrier;
    private final int slot;
    private final String carrierKey;
    private final String signature;
    private final UUID staffPayId;
    private final ServerLevel castLevel;
    private final List<Technique> techniques;
    private final Skill skill;
    private final BatchReleasePlan plan;
    private final float weaponBonus;
    private final Double attributeBonus;
    private final GroupBudget budget;
    private final HostCast hostCast;
    private final CancelToken cancelToken = new CancelToken();
    private final UUID rootCastId = UUID.randomUUID();
    private final Set<String> continued = ConcurrentHashMap.newKeySet();
    private final Set<Integer> childRetained = ConcurrentHashMap.newKeySet();
    private final Vec3 frozenPoint;
    private final Vec3 frozenDirection;
    private final Vec3 parallelHeading;
    private int pendingParents;
    private int pendingChildren;
    private int openLatches;
    private boolean finished;
    private boolean aborted;

    private BatchCastSession(ServerPlayer owner, ItemStack carrier, int slot, String carrierKey, String signature,
                             UUID staffPayId, List<Technique> techniques, Skill skill,
                             BatchReleasePlan plan, float weaponBonus, Double attributeBonus, GroupBudget budget,
                             HostCast hostCast, Vec3 frozenPoint, Vec3 frozenDirection, Vec3 parallelHeading) {
        this.owner = owner;
        this.carrier = carrier;
        this.slot = slot;
        this.carrierKey = carrierKey;
        this.signature = signature;
        this.staffPayId = staffPayId;
        this.castLevel = owner.serverLevel();
        this.techniques = techniques;
        this.skill = skill;
        this.plan = plan;
        this.weaponBonus = weaponBonus;
        this.attributeBonus = attributeBonus;
        this.budget = budget;
        this.hostCast = hostCast;
        this.frozenPoint = frozenPoint;
        this.frozenDirection = frozenDirection;
        this.parallelHeading = parallelHeading;
    }

    static CastTiming open(ServerPlayer owner, ItemStack carrier, int slot, com.bslx.XueXiMod.skill.Skill skill,
                           BatchReleasePlan plan, float weaponBonus, Double attributeBonus) {
        Vec3 castLook = owner.getLookAngle();
        Vec3 parallelHeading = horizontalCastHeading(castLook, owner.getYRot());
        Vec3 frozenPoint = null;
        Vec3 frozenDirection = castLook;
        if (plan.needsSurface) {
            SurfaceAnchor.Sample sample = SurfaceAnchor.resolve(owner, plan.clearanceHorizontal, plan.clearanceVertical);
            if (sample == null) {
                owner.displayClientMessage(Component.translatable("gui.xueximod.batch.surface_rejected"), true);
                return CastTiming.rejected();
            }
            frozenPoint = sample.point();
            frozenDirection = sample.direction();
            BatchTriggerVisuals.surface(owner.serverLevel(), sample.point());
        }
        GroupBudget budget = GroupBudget.reserve(owner, plan.reservedTotal, true);
        if (budget == null) {
            return CastTiming.insufficient();
        }
        String carrierKey = CastCooldown.carrierKey(owner, carrier, slot);
        if (!CastCooldown.acquire(carrierKey)) {
            budget.settle(owner);
            return CastTiming.rejected();
        }
        HostCast hostCast = HostCast.open(owner.getUUID());
        hostCast.hold();
        UUID staffPayId = StaffItem.holds(carrier) ? StaffItem.payIdentity(carrier) : null;
        String signature = StaffItem.holds(carrier)
                ? StaffSkills.signature(carrier)
                : (skill == null ? "" : ChainSteps.signature(skill));
        BatchCastSession session = new BatchCastSession(owner, carrier, slot, carrierKey, signature, staffPayId,
                skill.getTechniques(), skill, plan, weaponBonus, attributeBonus, budget, hostCast, frozenPoint, frozenDirection,
                parallelHeading);
        RootCastBindings.open(session.rootCastId, owner.getUUID());
        OPEN.put(carrierKey, session);
        session.pendingParents = plan.parents.size();
        int nowTick = owner.getServer().getTickCount();
        if (plan.parents.isEmpty()) {
            session.aborted = true;
            session.finish();
            return CastTiming.rejected();
        }
        for (BatchReleasePlan.ParentBirth birth : plan.parents) {
            BatchReleasePlan.ParentBirth captured = birth;
            ChainScheduler.schedule(owner.getServer(), nowTick + birth.birthDelay, () -> session.spawnParent(captured));
        }
        return CastTiming.deferred(nowTick, plan.fired);
    }

    /** 这名玩家当前唯一未结束的批量释放根。没有，或同时有多本，就返回 null。 */
    public static UUID currentRootId(ServerPlayer owner) {
        if (owner == null) {
            return null;
        }
        UUID found = null;
        for (BatchCastSession session : OPEN.values()) {
            if (session.finished || session.owner == null || !owner.getUUID().equals(session.owner.getUUID())) {
                continue;
            }
            if (found != null && !found.equals(session.rootCastId)) {
                return null;
            }
            found = session.rootCastId;
        }
        return found;
    }

    public static void cancelOwned(ServerPlayer owner) {
        if (owner == null) {
            return;
        }
        for (BatchCastSession session : List.copyOf(OPEN.values())) {
            if (owner.getUUID().equals(session.owner.getUUID())) {
                session.cancel();
            }
        }
    }

    public static void cancelAll() {
        for (BatchCastSession session : List.copyOf(OPEN.values())) {
            session.cancel();
        }
    }

    private void spawnParent(BatchReleasePlan.ParentBirth birth) {
        boolean armedLatch = false;
        boolean retainLatch = false;
        String latchKey = birth.branchOrdinal + ":" + birth.copyOrdinal;
        try {
            if (!stillValid()) {
                return;
            }
            HitClock hitClock = new HitClock();
            BirthLayout.Slot slot = layout(birth.copyOrdinal, birth.spreadCount, birth.branchOrdinal, hitClock,
                    birth.earliestSweep, birth.child == null ? 0.0D : birth.child.radius, false, null, null);
            slot.follow = (verdict, point, direction, target) ->
                    onParentVerdict(birth, hitClock, verdict, point, direction, target);
            slot.upwellThisCast = birth.upwellPath;
            slot.parallelThisCast = birth.parallelPath;
            if (birth.parallelPath) {
                slot.parallelHeading = parallelHeading;
            }
            if (frozenPoint != null) {
                slot.forcedOrigin = frozenPoint;
                slot.forcedDirection = frozenDirection;
            }
            if (birth.child != null) {
                openLatches += 1;
                armedLatch = true;
                int timeoutTick = owner.getServer().getTickCount() + birth.lifetimeTicks;
                ChainScheduler.schedule(owner.getServer(), timeoutTick, () -> expireLatch(birth));
            }
            BirthLayout.install(slot);
            ReleaseOutcome outcome = spawn(birth.segment, birth.entityLock, birth.dropPath, frozenPoint, frozenDirection);
            if (outcome != null && outcome.committed()) {
                budget.commit(birth.hostPrice);
                if (birth.retainedPrice > 0) {
                    budget.commit(birth.retainedPrice);
                }
                CarrierHooks.notifyBorn(handoff(birth.branchOrdinal, birth.copyOrdinal, hitClock, frozenPoint, frozenDirection, false));
                retainLatch = birth.child != null;
            } else if (armedLatch) {
                releaseUncontinued(birth);
                armedLatch = false;
            }
        } finally {
            if (armedLatch && !retainLatch && !continued.contains(latchKey)) {
                releaseUncontinued(birth);
            }
            BirthLayout.clear();
            noteParentFinished();
        }
    }

    private void releaseUncontinued(BatchReleasePlan.ParentBirth birth) {
        String latchKey = birth.branchOrdinal + ":" + birth.copyOrdinal;
        if (!continued.add(latchKey)) {
            return;
        }
        openLatches = Math.max(0, openLatches - 1);
    }

    private void onParentVerdict(BatchReleasePlan.ParentBirth birth, HitClock hitClock, HitVerdict verdict,
                                 Vec3 point, Vec3 direction, LivingEntity target) {
        if (birth.child == null || verdict == null || !verdict.continues()
                || verdict == HitVerdict.PENDING) {
            return;
        }
        String latchKey = birth.branchOrdinal + ":" + birth.copyOrdinal;
        if (!continued.add(latchKey)) {
            return;
        }
        openLatches = Math.max(0, openLatches - 1);
        ImpactPartition partition = ImpactPartition.excluding(target == null ? null : target.getUUID());
        OcclusionSnapshot snapshot = OcclusionSnapshot.capture(castLevel, point, direction, birth.child.radius, partition);
        int nowTick = owner.getServer().getTickCount();
        for (int childOrdinal = 0; childOrdinal < birth.child.ownCopies; childOrdinal++) {
            pendingChildren += 1;
            int delay = birth.child.stagger ? BatchReleasePlan.STAGGER_GAP * childOrdinal : 0;
            int capturedOrdinal = childOrdinal;
            if (delay <= 0) {
                spawnChild(birth, snapshot, partition, point, direction, capturedOrdinal);
                continue;
            }
            RootCastBindings.retain(rootCastId);
            ChainScheduler.schedule(owner.getServer(), nowTick + delay, () -> {
                try {
                    spawnChild(birth, snapshot, partition, point, direction, capturedOrdinal);
                } finally {
                    RootCastBindings.release(rootCastId);
                }
            });
        }
        maybeFinish();
    }

    private void expireLatch(BatchReleasePlan.ParentBirth birth) {
        if (finished || birth.child == null) {
            return;
        }
        String latchKey = birth.branchOrdinal + ":" + birth.copyOrdinal;
        if (!continued.add(latchKey)) {
            return;
        }
        openLatches = Math.max(0, openLatches - 1);
        maybeFinish();
    }

    private void spawnChild(BatchReleasePlan.ParentBirth birth, OcclusionSnapshot snapshot, ImpactPartition partition,
                            Vec3 point, Vec3 direction, int childOrdinal) {
        try {
            if (!stillValid() || birth.child == null) {
                return;
            }
            HitClock hitClock = new HitClock();
            BirthLayout.Slot slot = layout(childOrdinal, birth.child.ownCopies, birth.branchOrdinal, hitClock,
                    false, 0.0D, true, snapshot, partition);
            slot.upwellThisCast = birth.child.upwellPath;
            slot.parallelThisCast = birth.child.parallelPath;
            slot.forcedOrigin = point;
            slot.forcedDirection = direction;
            if (birth.child.parallelPath) {
                slot.parallelHeading = parallelHeading;
            }
            BirthLayout.install(slot);
            ReleaseOutcome outcome = spawn(birth.child.segment, birth.child.entityLock, birth.child.dropPath, point, direction);
            if (outcome != null && outcome.committed()) {
                budget.commit(birth.child.hostPrice);
                if (childRetained.add(birth.branchOrdinal) && birth.child.retainedPrice > 0) {
                    budget.commit(birth.child.retainedPrice);
                }
                CarrierHooks.notifyBorn(handoff(birth.branchOrdinal, childOrdinal, hitClock, point, direction, true));
            }
        } finally {
            BirthLayout.clear();
            noteChildFinished();
        }
    }

    private BirthLayout.Slot layout(int copyOrdinal, int spreadCount, int branchOrdinal, HitClock hitClock,
                                    boolean earliestSweep, double childRadius, boolean childInstance,
                                    OcclusionSnapshot snapshot, ImpactPartition partition) {
        BirthLayout.Slot slot = new BirthLayout.Slot();
        slot.copyOrdinal = copyOrdinal;
        slot.spreadCount = spreadCount;
        slot.respectLanding = true;
        slot.earliestSweep = earliestSweep;
        slot.hitClock = hitClock;
        slot.rootCastId = rootCastId;
        slot.branchOrdinal = branchOrdinal;
        slot.childRadius = childRadius;
        slot.childInstance = childInstance;
        slot.occlusion = snapshot;
        slot.partition = partition == null ? ImpactPartition.none() : partition;
        return slot;
    }

    private static Vec3 horizontalCastHeading(Vec3 look, float yawDegrees) {
        if (look != null) {
            double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
            if (horizontalLength >= 1.0E-4) {
                return new Vec3(look.x / horizontalLength, 0.0, look.z / horizontalLength);
            }
        }
        double yawRadians = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
    }

    private ReleaseOutcome spawn(ChainSegment segment, boolean entityLock, boolean dropPath, Vec3 point, Vec3 direction) {
        Technique action = techniques.get(segment.actionIndex());
        BirthLayout.Slot laid = BirthLayout.current();
        if (laid != null) {
            laid.resourceCommit = true;
            laid.resourceLoad = ResourceEnchantCatalog.collect(skill, segment.enchantIndexes());
            ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
            laid.bodyScaledBase = definition == null ? 0 : definition.bodyScaledSeedBase();
            DomainSpawnInheritance.install(laid, ReleaseExecutionContext.current(), action);
        }
        EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, segment);
        EnchantBonus bonus = EnchantBonus.resolve(skill, segment.enchantIndexes(), action.getBaseDamage(), 1);
        bonus = DomainSpawnInheritance.finish(owner, action, skill, segment.enchantIndexes(), bonus);
        float damage = SkillSegmentMath.configuredSegmentDamage(action, segment, weaponBonus,
                SkillSegmentMath.segmentMagicDamage(skill, segment), attributeBonus, bonus);
        boolean carries = !segment.enchantIndexes().isEmpty()
                || (bonus.resourceLoad != null && !bonus.resourceLoad.empty());
        Vec3 look = direction == null ? owner.getLookAngle() : direction;
        Vec3 place = point == null ? owner.position() : point;
        ReleaseBatch previousBatch = ReleaseBatch.current();
        ReleaseBatch geometryBatch = null;
        if (point == null) {
            geometryBatch = ReleaseBatch.open(owner, skill);
            ReleaseBatch.bind(previousBatch);
        }
        try {
            return SkillSegmentRunner.commitHostGroup(owner, action, techniques, segment.actionIndex(), carrier,
                    damage, carries, enchantments, entityLock, dropPath, null, 1, geometryBatch, bonus, segment.actionIndex(),
                    look, place, point, look, hostCast, UUID.randomUUID(), null);
        } finally {
            if (geometryBatch != null) {
                ReleaseBatch.bind(previousBatch);
            }
        }
    }

    private HostHandoff handoff(int branchOrdinal, int copyOrdinal, HitClock hitClock, Vec3 point, Vec3 direction,
                                boolean childInstance) {
        return new HostHandoff(rootCastId, owner, branchOrdinal, copyOrdinal, hitClock, point, direction, childInstance);
    }

    private boolean stillValid() {
        if (finished || cancelToken.cancelled() || owner == null || !owner.isAlive() || owner.isRemoved()) {
            return false;
        }
        if (owner.serverLevel() != castLevel) {
            return false;
        }
        ItemStack hand = owner.getMainHandItem();
        if (hand.getItem() != carrier.getItem()) {
            return false;
        }
        if (StaffItem.holds(carrier)) {
            if (staffPayId != null && !staffPayId.equals(StaffItem.payIdentity(hand))) {
                return false;
            }
            return signature.equals(StaffSkills.signature(hand));
        }
        Skill boundSkill = PlayerBoundSkills.get(owner, slot);
        String liveSignature = boundSkill == null ? "" : ChainSteps.signature(boundSkill);
        return signature.equals(liveSignature);
    }

    private void noteParentFinished() {
        if (finished) {
            return;
        }
        pendingParents = Math.max(0, pendingParents - 1);
        maybeFinish();
    }

    private void noteChildFinished() {
        if (finished) {
            return;
        }
        pendingChildren = Math.max(0, pendingChildren - 1);
        maybeFinish();
    }

    private void maybeFinish() {
        if (!finished && pendingParents == 0 && pendingChildren == 0 && openLatches == 0) {
            finish();
        }
    }

    private void cancel() {
        cancelToken.cancel();
        if (finished) {
            return;
        }
        aborted = true;
        pendingParents = 0;
        pendingChildren = 0;
        openLatches = 0;
        finish();
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        budget.settle(owner);
        if (!aborted) {
            int nowTick = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
            CastCooldown.mark(owner, carrier, slot, nowTick + Math.max(plan.scaledCooldown, 1));
        }
        CastCooldown.release(carrierKey);
        hostCast.release();
        RootCastBindings.release(rootCastId);
        OPEN.remove(carrierKey);
    }
}
