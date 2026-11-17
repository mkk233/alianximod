package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.ActionWindow;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalIntent;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupIds;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多重释放的父会话。成员按展开的间隔排队，同一成员的复制同一刻提交。
 * 执行锁和正式冷却分开。游标和冷却只在全部分组终结时各做一次。
 */
public final class CastExecutionSession implements ActionWindow.Sink {
    static final int STEP_GAP = MulticastExpansion.MEMBER_GAP_TICKS;

    private static final ConcurrentHashMap<String, CastExecutionSession> OPEN = new ConcurrentHashMap<>();

    private final ServerPlayer owner;
    private final ItemStack carrier;
    private final int slot;
    private final CastSource castSource;
    private final String carrierKey;
    private final Skill skill;
    private final List<Technique> techniques;
    private final List<ChainSegment> segments;
    private final CompiledReleaseStep plan;
    private final float weaponBonus;
    private final String signature;
    private final int stepCount;
    private final CastPlanContext planContext;
    private final DomainOpening domainOpening;
    private final ServerLevel castLevel;
    private final int castEpoch;
    private final Set<UUID> processedTargets = new HashSet<>();
    private final List<Group> groups = new ArrayList<>();

    private ManaPaymentReceipt actionReceipt;
    private boolean accepted;
    private boolean actionEnded;
    private boolean legalCollision;
    private boolean terminalAlready;
    private boolean closed;
    private boolean reserveFailed;
    private int pending;
    private HostCast hostCast;
    private final Set<String> conditionalUsage = new HashSet<>();

    private CastExecutionSession(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, String carrierKey, Skill skill,
                                 List<Technique> techniques, List<ChainSegment> segments, CompiledReleaseStep plan,
                                 float weaponBonus, String signature, int stepCount, CastPlanContext planContext,
                                 DomainOpening domainOpening) {
        this.owner = owner;
        this.carrier = carrier;
        this.slot = slot;
        this.castSource = castSource;
        this.carrierKey = carrierKey;
        this.skill = skill;
        this.techniques = techniques;
        this.segments = segments;
        this.plan = plan;
        this.weaponBonus = weaponBonus;
        this.signature = signature;
        this.stepCount = stepCount;
        this.planContext = planContext;
        this.domainOpening = domainOpening;
        this.castLevel = owner.serverLevel();
        this.castEpoch = ChargeGate.epoch(owner.getUUID());
    }

    static CastTiming open(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, Skill skill, List<Technique> techniques,
                           List<ChainSegment> segments, CompiledReleaseStep plan, float weaponBonus,
                           String signature, int stepCount) {
        return open(owner, carrier, slot, castSource, skill, techniques, segments, plan, weaponBonus, signature, stepCount, null);
    }

    static CastTiming open(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, Skill skill, List<Technique> techniques,
                           List<ChainSegment> segments, CompiledReleaseStep plan, float weaponBonus,
                           String signature, int stepCount, CastPlanContext planContext) {
        return open(owner, carrier, slot, castSource, skill, techniques, segments, plan, weaponBonus, signature, stepCount,
                planContext, null);
    }

    static CastTiming open(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, Skill skill, List<Technique> techniques,
                           List<ChainSegment> segments, CompiledReleaseStep plan, float weaponBonus,
                           String signature, int stepCount, CastPlanContext planContext, DomainOpening domainOpening) {
        if (plan.members.isEmpty()) {
            owner.sendSystemMessage(Component.literal("§c双重释放后面没有可放的术式，这次没有释放"));
            return CastTiming.rejected();
        }
        String carrierKey = CastCooldown.carrierKey(owner, carrier, slot);
        if (!CastCooldown.acquire(carrierKey)) {
            return CastTiming.rejected();
        }
        CastExecutionSession session = new CastExecutionSession(owner, carrier, slot, castSource, carrierKey, skill, techniques,
                segments, plan, weaponBonus, signature, stepCount, planContext, domainOpening);
        OPEN.put(carrierKey, session);
        if (plan.actionLed) {
            return session.startAction();
        }
        return session.startOrdinary();
    }

    public static void cancelOwned(ServerPlayer owner) {
        BatchCastSession.cancelOwned(owner);
        if (owner == null) {
            return;
        }
        List<CastExecutionSession> living = new ArrayList<>(OPEN.values());
        for (CastExecutionSession session : living) {
            if (owner.getUUID().equals(session.owner.getUUID())) {
                session.cancel();
            }
        }
    }

    public static void cancelAll() {
        BatchCastSession.cancelAll();
        List<CastExecutionSession> living = new ArrayList<>(OPEN.values());
        for (CastExecutionSession session : living) {
            session.cancel();
        }
    }

    private CastTiming startAction() {
        actionReceipt = ManaPaymentReceipt.charge(owner, plan.actionShare, true);
        if (actionReceipt == null) {
            dropUnaccepted();
            return CastTiming.insufficient();
        }
        ChainSegment action = segments.get(plan.actionSegment);
        Technique technique = techniques.get(action.actionIndex());
        if (!SkillSegmentRunner.playAction(owner, technique, techniques, action.actionIndex(), carrier, this, plan.actionInput, planContext)) {
            dropUnaccepted();
            owner.sendSystemMessage(Component.literal("§c动作没有开始，这次没有释放"));
            return CastTiming.rejected();
        }
        accepted = true;
        return CastTiming.deferred(owner.getServer().getTickCount(), proficiencyTechniques(techniques));
    }

    private CastTiming startOrdinary() {
        int nowTick = owner.getServer().getTickCount();
        UUID groupId = UUID.randomUUID();
        ActionContact contact = new ActionContact(owner.getUUID(), nowTick, owner.position(), owner.getLookAngle(), groupId);
        beginGroup(contact);
        if (!accepted) {
            boolean unpaid = reserveFailed;
            dropUnaccepted();
            return unpaid ? CastTiming.insufficient() : CastTiming.rejected();
        }
        return CastTiming.deferred(nowTick, proficiencyTechniques(techniques));
    }

    @Override
    public void onContact(UUID targetId, Vec3 point, Vec3 direction) {
        if (closed || terminalAlready || !plan.actionLed || actionEnded) {
            return;
        }
        Entity found = castLevel.getEntity(targetId);
        if (!(found instanceof LivingEntity living) || !ActionWindow.hostileTo(owner, living)) {
            return;
        }
        if (!processedTargets.add(targetId)) {
            return;
        }
        legalCollision = true;
        Vec3 aimed = direction == null || direction.lengthSqr() < 1.0E-8 ? owner.getLookAngle() : direction.normalize();
        Vec3 anchor = point == null ? living.position() : point;
        beginGroup(new ActionContact(targetId, owner.getServer().getTickCount(), anchor, aimed, UUID.randomUUID()));
    }

    @Override
    public void onEnded() {
        if (actionEnded || terminalAlready) {
            return;
        }
        actionEnded = true;
        tryFinish();
    }

    @Override
    public boolean closed() {
        return closed || terminalAlready;
    }

    private void cancel() {
        if (terminalAlready) {
            return;
        }
        closed = true;
        if (!accepted) {
            dropUnaccepted();
            return;
        }
        actionEnded = true;
        List<Group> pendingGroups = new ArrayList<>(groups);
        for (Group group : pendingGroups) {
            finishGroup(group);
        }
        tryFinish();
        closeHostLedger();
    }

    private boolean beginGroup(ActionContact contact) {
        if (closed || terminalAlready) {
            return false;
        }
        int memberCount = plan.members.size();
        if (memberCount <= 0) {
            return false;
        }
        Group group = new Group(contact, memberCount);
        groups.add(group);
        pending++;
        GroupBudget budget = GroupBudget.reserve(owner, plan.groupBill());
        if (budget == null) {
            reserveFailed = true;
            finishGroup(group);
            return false;
        }
        group.budget = budget;
        group.batch = ReleaseBatch.open(owner, skill);
        String groupToken = contact.groupId() == null ? "none" : contact.groupId().toString();
        String openingToken = domainOpening == null ? "none" : domainOpening.laneToken();
        ExplicitHostOffers.capture(group.batch, skill, techniques, segments, domainOpening, owner,
                "group:" + openingToken + ":" + groupToken);
        ReleaseBatch.unbind();
        boolean firstCommitted = false;
        int anchorTick = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
        try {
            ReleaseOutcome first = deliverMember(group, 0);
            anchorTick = first.tick();
            firstCommitted = first.committed();
            if (!firstCommitted && !first.reason().isEmpty()) {
                owner.sendSystemMessage(Component.literal("§c" + first.reason()));
            }
        } catch (Throwable thrown) {
            DebugLog.log("多重成员执行异常：" + thrown);
            firstCommitted = false;
        }
        if (firstCommitted) {
            accepted = true;
            if (!plan.actionLed) {
                legalCollision = true;
                actionEnded = true;
            }
        }
        group.settleMember(0);
        if (!firstCommitted || group.openMembers() == 0) {
            finishGroup(group);
            return firstCommitted || (plan.actionLed && accepted);
        }
        if (owner.getServer() == null) {
            finishGroup(group);
            return true;
        }
        for (int memberIndex = 1; memberIndex < memberCount; memberIndex++) {
            if (group.closed || terminalAlready) {
                break;
            }
            int scheduledIndex = memberIndex;
            long delayedTick = (long) anchorTick + plan.gapTicks(memberIndex);
            int fireTick = delayedTick > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) delayedTick;
            try {
                ChainScheduler.schedule(owner.getServer(), fireTick, () -> runScheduledMember(group, scheduledIndex));
            } catch (Throwable thrown) {
                DebugLog.log("多重成员排程异常：" + thrown);
                finishGroup(group);
                return true;
            }
        }
        return true;
    }

    private void runScheduledMember(Group group, int memberIndex) {
        if (group.closed || terminalAlready) {
            return;
        }
        try {
            if (closed || !continues()) {
                finishGroup(group);
                return;
            }
            deliverMember(group, memberIndex);
        } catch (Throwable thrown) {
            DebugLog.log("多重成员执行异常：" + thrown);
        } finally {
            if (group.closed) {
                return;
            }
            if (!group.tracksMember(memberIndex)) {
                finishGroup(group);
                return;
            }
            if (group.settleMember(memberIndex) && group.openMembers() == 0) {
                finishGroup(group);
            }
        }
    }

    private ReleaseOutcome deliverMember(Group group, int memberIndex) {
        int nowTick = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
        if (memberIndex < 0 || memberIndex >= plan.members.size()) {
            return ReleaseOutcome.rejected(nowTick, "");
        }
        return commitMember(group, memberIndex);
    }

    private int memberShareAt(int memberIndex) {
        if (memberIndex < 0 || memberIndex >= plan.memberShares.size()) {
            return 0;
        }
        return plan.memberShares.get(memberIndex);
    }

    private void retainPreparedMember(Group group, int memberIndex) {
        int ruleShare = memberIndex == 0 ? Math.max(0, plan.ruleShare) : 0;
        group.retainWholeMember(memberIndex, memberShareAt(memberIndex), ruleShare);
    }

    private static int[] splitMemberShare(int memberShare, int copyCount) {
        int safeCopies = Math.max(1, copyCount);
        int safeShare = Math.max(0, memberShare);
        int baseShare = safeShare / safeCopies;
        int remainder = safeShare % safeCopies;
        int[] copyPrices = new int[safeCopies];
        for (int copyIndex = 0; copyIndex < safeCopies; copyIndex++) {
            copyPrices[copyIndex] = baseShare + (copyIndex < remainder ? 1 : 0);
        }
        return copyPrices;
    }

    private ReleaseOutcome commitMember(Group group, int memberIndex) {
        int nowTick = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
        if (!continues()) {
            return ReleaseOutcome.cancelled(nowTick, "施法已经中断");
        }
        ChainSegment member = plan.members.get(memberIndex);
        Technique technique = techniques.get(member.actionIndex());
        MemberCastView castView = plan.memberView(memberIndex);
        ReleaseInput chainInput = castView != null ? castView.chainInput : plan.memberInput(memberIndex);
        Vec3 point = group.contact.point();
        Vec3 look = group.contact.direction();
        SkillSegmentRunner.bindMemberView(castView);
        try {
        if (technique.isEnchantment()) {
            SkillChainPlan.imprintSelection(owner, skill, List.of(member.actionIndex()), 1);
            retainPreparedMember(group, memberIndex);
            return ReleaseOutcome.committed(nowTick);
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        int copies = castView != null
                ? Math.max(1, castView.copyCount)
                : Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, segments.indexOf(member), planContext));
        boolean memberLock = castView != null
                ? definition != null && definition.acceptsInitialAim() && castView.entityLock
                : SkillChainPlan.lockThisCast(techniques, member.actionIndex(), planContext);
        boolean memberDrop = castView != null
                ? definition != null && definition.acceptsPathOverride() && castView.drop
                : SkillChainPlan.dropThisCast(techniques, segments, member.actionIndex(), planContext);
        EnchantmentSnapshot snapshot = ContactCast.snapshot(skill, member, owner);
        String resolvedClaim = group.contact.groupId().toString();
        BoundReleasePlan boundPlan = domainOpening == null ? null : domainOpening.planFor(member.actionIndex());
        if (boundPlan != null) {
            String boundClaim = domainOpening.claimFor(boundPlan);
            if (boundClaim != null) {
                resolvedClaim = boundClaim;
            }
        }
        final String claimSuffix = resolvedClaim;
        if (definition != null && technique.getBaseDamage() <= 0f && definition.conditionalEffect() != null) {
            HostCast parentCast = hostLedger();
            UUID operationId = ConditionalIntent.operationId(parentCast.tokenId(), memberIndex, 0, group.contact.groupId());
            ManaPaymentReceipt parentReceipt = group.budget == null ? null : group.budget.receipt;
            ReleaseOutcome outcome = SkillSegmentRunner.commitConditional(owner, definition, operationId, snapshot, chainInput, parentReceipt);
            if (outcome.committed()) {
                recordConditionalUse(technique);
                retainPreparedMember(group, memberIndex);
            }
            return outcome;
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
            DomainWave.Held domainHeld = DomainWave.hold(owner, skill, technique, member.enchantIndexes());
            boolean retained;
            try {
                retained = publishReservedCopies(group, memberIndex, copies, () -> DomainWave.openReserved(
                        owner, technique, definition.domain(), copies, snapshot, point,
                        claimSuffix, chainInput, boundPlan, domainHeld));
            } finally {
                DomainWave.release(domainHeld);
            }
            return retained ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
            boolean retained = publishReservedCopies(group, memberIndex, copies, () -> BarrierService.tryPlaceReserved(
                    owner, definition.barrier(), copies, snapshot, point, look, claimSuffix, chainInput));
            return retained ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
            long waveKey = group.contact.groupId().getLeastSignificantBits() ^ member.actionIndex();
            boolean retained = publishReservedCopies(group, memberIndex, 1, () -> {
                boolean placed = GuardStates.activateReserved(owner, definition.guard(), waveKey, chainInput);
                if (placed && snapshot != null) {
                    GuardStates.bindContact(owner, snapshot);
                }
            });
            return retained ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
        }
        ReleaseBatch previous = ReleaseBatch.current();
        ReleaseBatch.bind(group.batch);
        try {
            if (definition != null && definition.submitsHostGroup()) {
                return publishHostMember(group, memberIndex, nowTick, member, technique, definition, copies,
                        chainInput, look, point, snapshot, memberLock, memberDrop);
            }
            ReleaseBatch.Tally tally = copies > 1 ? group.batch.openGroupTally() : null;
            EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
            EnchantBonus bonus = EnchantBonus.resolve(skill, member.enchantIndexes(), technique.getBaseDamage(), 1);
            float damage = member.enchantIndexes().isEmpty()
                    ? SkillSegmentMath.segmentDamage(technique, member, weaponBonus, SkillSegmentMath.segmentMagicDamage(skill, member), null)
                    : bonus.carriedDamage(weaponBonus);
            boolean fieldHost = definition != null && definition.fieldRadius() > 0f;
            boolean carries = !fieldHost && technique.getBaseDamage() > 0f && !member.enchantIndexes().isEmpty();
            boolean anyCommitted = false;
            int commitTick = nowTick;
            UUID publicationRound = UUID.randomUUID();
            int ruleShare = memberIndex == 0 ? Math.max(0, plan.ruleShare) : 0;
            group.armLoosePublication(publicationRound, memberIndex, splitMemberShare(memberShareAt(memberIndex), copies), ruleShare);
            PublicationAttempt.bindLedger(publicationRound, group);
            try {
                for (int copyIndex = 0; copyIndex < copies; copyIndex++) {
                    PublicationAttempt attempt = PublicationAttempt.openLoose(publicationRound, memberIndex, copyIndex);
                    if (attempt == null) {
                        continue;
                    }
                    try {
                        ReleaseOutcome outcome = SkillSegmentRunner.commitNow(owner, technique, techniques, member.actionIndex(), carrier,
                                damage, carries, enchantments,
                                memberLock, memberDrop,
                                tally, copyIndex, copies, group.batch, bonus, memberIndex, look, point, chainInput, planContext);
                        if (attempt.accepted()) {
                            anyCommitted = true;
                            commitTick = outcome.tick();
                        }
                    } catch (Throwable thrown) {
                        DebugLog.log("多重复制执行异常：" + thrown);
                        if (attempt.accepted()) {
                            anyCommitted = true;
                        }
                    } finally {
                        attempt.close();
                    }
                }
            } finally {
                group.clearLoosePublication();
                PublicationAttempt.unbindLedger(publicationRound);
            }
            if (!anyCommitted) {
                return ReleaseOutcome.rejected(nowTick, "没有可提交的释放");
            }
            if (fieldHost && snapshot != null) {
                FieldZones.bindBorn(owner.getUUID(), snapshot, commitTick);
            }
            return ReleaseOutcome.committed(commitTick);
        } finally {
            ReleaseBatch.bind(previous);
        }
        } finally {
            SkillSegmentRunner.clearMemberView();
        }
    }

    private boolean publishReservedCopies(Group group, int memberIndex, int copyCount, ReservedPlacement placement) {
        int safeCopies = Math.max(1, copyCount);
        UUID publicationRound = UUID.randomUUID();
        int ruleShare = memberIndex == 0 ? Math.max(0, plan.ruleShare) : 0;
        group.armLoosePublication(publicationRound, memberIndex,
                splitMemberShare(memberShareAt(memberIndex), safeCopies), ruleShare);
        PublicationAttempt.bindLedger(publicationRound, group);
        PublicationAttempt attempt = PublicationAttempt.openLoose(publicationRound, memberIndex, 0);
        boolean retained = false;
        try {
            if (attempt != null) {
                placement.place();
            }
        } catch (Throwable thrown) {
            DebugLog.log("多重成员发布异常：" + thrown);
        } finally {
            if (attempt != null) {
                attempt.close();
            }
            retained = group.retainedAnyCopy();
            group.clearLoosePublication();
            PublicationAttempt.unbindLedger(publicationRound);
        }
        return retained;
    }

    private ReleaseOutcome publishHostMember(Group group, int memberIndex, int nowTick, ChainSegment member,
                                              Technique technique, ReleaseDefinition definition, int copies,
                                              ReleaseInput chainInput, Vec3 look, Vec3 point, EnchantmentSnapshot snapshot,
                                              boolean memberLock, boolean memberDrop) {
        ReleaseBatch.Tally tally = copies > 1 ? group.batch.openGroupTally() : null;
        HostCast parentCast = hostLedger();
        UUID hostGroup = HostGroupIds.sessionHost(group.contact.groupId(), memberIndex);
        EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
        EnchantBonus bonus = EnchantBonus.resolve(skill, member.enchantIndexes(), technique.getBaseDamage(), 1);
        float damage = member.enchantIndexes().isEmpty()
                ? SkillSegmentMath.segmentDamage(technique, member, weaponBonus, SkillSegmentMath.segmentMagicDamage(skill, member), null)
                : bonus.carriedDamage(weaponBonus);
        boolean fieldHost = definition.fieldRadius() > 0f;
        boolean carries = !fieldHost && technique.getBaseDamage() > 0f && !member.enchantIndexes().isEmpty();
        UUID publicationRound = UUID.randomUUID();
        int ruleShare = memberIndex == 0 ? Math.max(0, plan.ruleShare) : 0;
        group.armLoosePublication(publicationRound, memberIndex,
                new int[] {Math.max(0, memberShareAt(memberIndex))}, ruleShare);
        PublicationAttempt.bindLedger(publicationRound, group);
        PublicationAttempt attempt = PublicationAttempt.openHostGroup(publicationRound, memberIndex);
        ReleaseOutcome outcome = ReleaseOutcome.rejected(nowTick, "");
        try {
            outcome = SkillSegmentRunner.commitHostGroup(owner, technique, techniques, member.actionIndex(), carrier,
                    damage, carries, enchantments,
                    memberLock, memberDrop,
                    tally, copies, group.batch, bonus, memberIndex, look, point, point, look, parentCast, hostGroup, null,
                    chainInput, planContext);
        } catch (Throwable thrown) {
            DebugLog.log("宿主组发布异常：" + thrown);
        } finally {
            if (attempt != null) {
                if (outcome != null && outcome.committed()) {
                    attempt.noteHostCommitted(true);
                }
                attempt.close();
            }
            group.clearLoosePublication();
            PublicationAttempt.unbindLedger(publicationRound);
        }
        boolean retained = group.memberWasRetained(memberIndex);
        if (!retained && attempt == null && outcome != null && outcome.committed()) {
            retainPreparedMember(group, memberIndex);
            retained = true;
        }
        if (!retained) {
            return outcome == null ? ReleaseOutcome.rejected(nowTick, "") : outcome;
        }
        int commitTick = outcome == null ? nowTick : outcome.tick();
        if (fieldHost && snapshot != null) {
            FieldZones.bindBorn(owner.getUUID(), snapshot, commitTick);
        }
        if (outcome != null && outcome.committed()) {
            return outcome;
        }
        return ReleaseOutcome.committed(commitTick);
    }

    private void finishGroup(Group group) {
        if (group.closed) {
            return;
        }
        group.closed = true;
        if (group.budget != null) {
            group.budget.settle(owner);
        }
        pending = Math.max(0, pending - 1);
        tryFinish();
    }

    private void tryFinish() {
        if (!actionEnded || pending != 0 || terminalAlready || !accepted) {
            return;
        }
        terminalAlready = true;
        closed = true;
        int nowTick = owner.getServer().getTickCount();
        int cooldown = legalCollision ? plan.fullCooldown : plan.actionCooldown;
        int readyTick = nowTick + Math.max(cooldown, 1);
        CastCooldown.mark(owner, carrier, slot, readyTick);
        if (stepCount > 0) {
            ChainProgress.advance(owner, carrier, slot, signature, stepCount);
        }
        CastCooldown.release(carrierKey);
        OPEN.remove(carrierKey);
        closeHostLedger();
        DebugLog.log("多重释放结算 碰撞=" + legalCollision + " 冷却=" + cooldown + " 重放=" + readyTick);
    }

    private void dropUnaccepted() {
        if (terminalAlready) {
            return;
        }
        terminalAlready = true;
        closed = true;
        for (Group group : groups) {
            if (!group.closed && group.budget != null) {
                group.budget.settle(owner);
            }
            group.closed = true;
        }
        if (actionReceipt != null) {
            actionReceipt.refund(owner);
        }
        CastCooldown.release(carrierKey);
        OPEN.remove(carrierKey);
        closeHostLedger();
    }

    private HostCast hostLedger() {
        if (hostCast == null) {
            hostCast = HostCast.open(owner.getUUID(), castSource);
        }
        return hostCast;
    }

    private void closeHostLedger() {
        HostCast current = hostCast;
        if (current != null) {
            current.close();
        }
    }

    private boolean continues() {
        return owner.isAlive() && owner.level() == castLevel && ChargeGate.same(owner.getUUID(), castEpoch);
    }

    private void recordConditionalUse(Technique technique) {
        SkillUsePacket.recordTechniqueUse(owner, technique, conditionalUsage);
    }

    private static boolean isConditionalTechnique(Technique technique) {
        if (technique == null || technique.getBaseDamage() > 0f) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.conditionalEffect() != null;
    }

    /** 没有条件术式时原样交回，延后释放的熟练度名单不变。 */
    private static List<Technique> proficiencyTechniques(List<Technique> techniques) {
        boolean conditionalPresent = false;
        for (Technique technique : techniques) {
            if (isConditionalTechnique(technique)) {
                conditionalPresent = true;
                break;
            }
        }
        if (!conditionalPresent) {
            return techniques;
        }
        List<Technique> visible = new ArrayList<>();
        for (Technique technique : techniques) {
            if (!isConditionalTechnique(technique)) {
                visible.add(technique);
            }
        }
        return visible;
    }

    @FunctionalInterface
    private interface ReservedPlacement {
        void place();
    }

    private static final class Group implements PublicationAttempt.Ledger {
        final ActionContact contact;
        private final boolean[] memberSettled;
        private final boolean[] memberRetained;
        private int openMembers;
        GroupBudget budget;
        ReleaseBatch batch;
        boolean closed;
        private UUID publicationRound;
        private int publicationMember = -1;
        private int[] copyPrices = new int[0];
        private boolean[] copyRetained = new boolean[0];
        private int armedRuleShare;
        private boolean ruleRetained;

        Group(ActionContact contact, int memberCount) {
            this.contact = contact;
            int safeCount = Math.max(0, memberCount);
            this.memberSettled = new boolean[safeCount];
            this.memberRetained = new boolean[safeCount];
            this.openMembers = safeCount;
        }

        boolean tracksMember(int memberIndex) {
            return memberIndex >= 0 && memberIndex < memberSettled.length;
        }

        boolean settleMember(int memberIndex) {
            if (!tracksMember(memberIndex) || memberSettled[memberIndex]) {
                return false;
            }
            memberSettled[memberIndex] = true;
            if (openMembers > 0) {
                openMembers--;
            }
            return true;
        }

        int openMembers() {
            return openMembers;
        }

        boolean retainedAnyCopy() {
            for (boolean retained : copyRetained) {
                if (retained) {
                    return true;
                }
            }
            return false;
        }

        boolean memberWasRetained(int memberIndex) {
            return tracksMember(memberIndex) && memberRetained[memberIndex];
        }

        void armLoosePublication(UUID roundId, int memberIndex, int[] prices, int ruleShare) {
            this.publicationRound = roundId;
            this.publicationMember = memberIndex;
            this.copyPrices = prices == null ? new int[0] : prices;
            this.copyRetained = new boolean[this.copyPrices.length];
            this.armedRuleShare = Math.max(0, ruleShare);
        }

        void clearLoosePublication() {
            this.publicationRound = null;
            this.publicationMember = -1;
            this.copyPrices = new int[0];
            this.copyRetained = new boolean[0];
            this.armedRuleShare = 0;
        }

        void retainWholeMember(int memberIndex, int memberShare, int ruleShare) {
            if (!tracksMember(memberIndex) || memberRetained[memberIndex]) {
                return;
            }
            memberRetained[memberIndex] = true;
            if (budget == null || budget.settled) {
                return;
            }
            budget.commit(Math.max(0, memberShare));
            if (memberIndex == 0) {
                retainRule(ruleShare);
            }
        }

        @Override
        public boolean openingAllowed(UUID roundId) {
            return publicationRound != null && publicationRound.equals(roundId)
                    && !closed && budget != null && !budget.settled;
        }

        @Override
        public void attemptOpened(UUID roundId) {
        }

        @Override
        public void retainLoose(UUID roundId, int memberIndex, int copyIndex) {
            if (publicationRound == null || !publicationRound.equals(roundId) || memberIndex != publicationMember) {
                return;
            }
            if (copyIndex < 0 || copyIndex >= copyPrices.length || copyRetained[copyIndex]) {
                return;
            }
            if (budget == null || budget.settled) {
                return;
            }
            copyRetained[copyIndex] = true;
            budget.commit(Math.max(0, copyPrices[copyIndex]));
            if (memberIndex == 0) {
                retainRule(armedRuleShare);
            }
        }

        @Override
        public void retainHost(UUID roundId, int memberIndex) {
            if (publicationRound == null || !publicationRound.equals(roundId) || memberIndex != publicationMember) {
                return;
            }
            retainWholeMember(memberIndex, memberShareSum(copyPrices), armedRuleShare);
        }

        @Override
        public void attemptFinished(UUID roundId) {
        }

        private void retainRule(int ruleShare) {
            if (ruleRetained || budget == null || budget.settled) {
                return;
            }
            ruleRetained = true;
            budget.commit(Math.max(0, ruleShare));
        }

        private static int memberShareSum(int[] prices) {
            int summed = 0;
            if (prices == null) {
                return 0;
            }
            for (int price : prices) {
                summed += Math.max(0, price);
            }
            return summed;
        }
    }

}
