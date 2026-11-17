package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.ActionWindow;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
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
 * 双重释放的父会话。执行锁和正式冷却分开。游标和冷却只在终结时各做一次。
 */
public final class CastExecutionSession implements ActionWindow.Sink {
    static final int STEP_GAP = 5;

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
                                 float weaponBonus, String signature, int stepCount) {
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
        this.castLevel = owner.serverLevel();
        this.castEpoch = ChargeGate.epoch(owner.getUUID());
    }

    static CastTiming open(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, Skill skill, List<Technique> techniques,
                           List<ChainSegment> segments, CompiledReleaseStep plan, float weaponBonus,
                           String signature, int stepCount) {
        if (plan.members.isEmpty()) {
            owner.sendSystemMessage(Component.literal("§c双重释放后面没有可放的术式，这次没有释放"));
            return CastTiming.rejected();
        }
        String carrierKey = CastCooldown.carrierKey(owner, carrier, slot);
        if (!CastCooldown.acquire(carrierKey)) {
            return CastTiming.rejected();
        }
        CastExecutionSession session = new CastExecutionSession(owner, carrier, slot, castSource, carrierKey, skill, techniques,
                segments, plan, weaponBonus, signature, stepCount);
        OPEN.put(carrierKey, session);
        if (plan.actionLed) {
            return session.startAction();
        }
        return session.startOrdinary();
    }

    public static void cancelOwned(ServerPlayer owner) {
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
        if (!SkillSegmentRunner.playAction(owner, technique, techniques, action.actionIndex(), carrier, this)) {
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
        Group group = new Group(contact);
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
        ReleaseBatch.unbind();
        ReleaseOutcome first = commitMember(group, 0);
        if (!first.committed()) {
            if (!first.reason().isEmpty()) {
                owner.sendSystemMessage(Component.literal("§c" + first.reason()));
            }
            finishGroup(group);
            return plan.actionLed && accepted;
        }
        budget.commit(plan.ruleShare + plan.memberShares.get(0));
        accepted = true;
        if (!plan.actionLed) {
            legalCollision = true;
            actionEnded = true;
        }
        if (plan.members.size() < 2) {
            finishGroup(group);
            return true;
        }
        int secondTick = first.tick() + STEP_GAP;
        ChainScheduler.schedule(owner.getServer(), secondTick, () -> runSecond(group));
        return true;
    }

    private void runSecond(Group group) {
        if (group.closed || terminalAlready) {
            return;
        }
        if (closed || !continues()) {
            finishGroup(group);
            return;
        }
        ReleaseOutcome second = commitMember(group, 1);
        if (second.committed() && group.budget != null) {
            group.budget.commit(plan.memberShares.get(1));
        }
        finishGroup(group);
    }

    private ReleaseOutcome commitMember(Group group, int memberIndex) {
        int nowTick = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
        if (!continues()) {
            return ReleaseOutcome.cancelled(nowTick, "施法已经中断");
        }
        ChainSegment member = plan.members.get(memberIndex);
        Technique technique = techniques.get(member.actionIndex());
        Vec3 point = group.contact.point();
        Vec3 look = group.contact.direction();
        if (technique.isEnchantment()) {
            SkillChainPlan.imprintSelection(owner, skill, List.of(member.actionIndex()), 1);
            return ReleaseOutcome.committed(nowTick);
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        int copies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, segments.indexOf(member)));
        EnchantmentSnapshot snapshot = ContactCast.snapshot(skill, member, owner);
        String claimSuffix = group.contact.groupId().toString();
        if (definition != null && technique.getBaseDamage() <= 0f && definition.conditionalEffect() != null) {
            HostCast parentCast = hostLedger();
            UUID operationId = ConditionalIntent.operationId(parentCast.tokenId(), memberIndex, 0, group.contact.groupId());
            ReleaseOutcome outcome = SkillSegmentRunner.commitConditional(owner, definition, operationId, snapshot);
            if (outcome.committed()) {
                recordConditionalUse(technique);
            }
            return outcome;
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
            boolean placed = DomainService.tryWaveReserved(owner, technique, definition.domain(), copies, snapshot, point, claimSuffix);
            return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
            boolean placed = BarrierService.tryPlaceReserved(owner, definition.barrier(), copies, snapshot, point, look, claimSuffix);
            return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
            long waveKey = group.contact.groupId().getLeastSignificantBits() ^ member.actionIndex();
            boolean placed = GuardStates.activateReserved(owner, definition.guard(), waveKey);
            if (placed && snapshot != null) {
                GuardStates.bindContact(owner, snapshot);
            }
            return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
        }
        ReleaseBatch previous = ReleaseBatch.current();
        ReleaseBatch.bind(group.batch);
        try {
            if (definition != null && definition.submitsHostGroup()) {
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
                ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(owner, technique, techniques, member.actionIndex(), carrier,
                        damage, carries, enchantments,
                        SkillChainPlan.lockThisCast(techniques, member.actionIndex()),
                        SkillChainPlan.dropThisCast(techniques, segments, member.actionIndex()),
                        tally, copies, group.batch, bonus, memberIndex, look, point, point, look, parentCast, hostGroup, null);
                if (outcome.committed() && fieldHost && snapshot != null) {
                    FieldZones.bindBorn(owner.getUUID(), snapshot, outcome.tick());
                }
                return outcome;
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
            for (int copyIndex = 0; copyIndex < copies; copyIndex++) {
                ReleaseOutcome outcome = SkillSegmentRunner.commitNow(owner, technique, techniques, member.actionIndex(), carrier,
                        damage, carries, enchantments,
                        SkillChainPlan.lockThisCast(techniques, member.actionIndex()),
                        SkillChainPlan.dropThisCast(techniques, segments, member.actionIndex()),
                        tally, copyIndex, copies, group.batch, bonus, memberIndex, look, point);
                if (outcome.committed()) {
                    anyCommitted = true;
                    commitTick = outcome.tick();
                }
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
        DebugLog.log("双重释放结算 碰撞=" + legalCollision + " 冷却=" + cooldown + " 重放=" + readyTick);
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

    private static final class Group {
        final ActionContact contact;
        GroupBudget budget;
        ReleaseBatch batch;
        boolean closed;

        Group(ActionContact contact) {
            this.contact = contact;
        }
    }

    private static final class GroupBudget {
        final ManaPaymentReceipt receipt;
        int openShare;
        boolean settled;

        GroupBudget(ManaPaymentReceipt receipt, int openShare) {
            this.receipt = receipt;
            this.openShare = openShare;
        }

        static GroupBudget reserve(ServerPlayer owner, int cost) {
            ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(owner, Math.max(0, cost), true);
            if (receipt == null) {
                return null;
            }
            return new GroupBudget(receipt, Math.max(0, cost));
        }

        void commit(int share) {
            if (settled || share <= 0) {
                return;
            }
            int keeping = Math.min(share, openShare);
            openShare -= keeping;
            if (keeping > 0 && receipt != null) {
                receipt.retainShare(keeping);
            }
        }

        void settle(ServerPlayer owner) {
            if (settled) {
                return;
            }
            settled = true;
            receipt.refundShare(owner, openShare);
            openShare = 0;
        }
    }
}
