package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundMember;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleases;
import com.bslx.XueXiMod.shu.technique.release.bind.LifetimeToken;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.bind.RoundHandle;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 领域自动轮。复用现有报价、持剑门、复制份数和宿主组提交。
 * 不占用按键冷却，不推进链进度，不读玩家准星或锁定标记。
 */
final class AutomaticRound implements BoundReleases.Executor {
    private static final AutomaticRound INSTANCE = new AutomaticRound();

    private AutomaticRound() {
    }

    static AutomaticRound executor() {
        return INSTANCE;
    }

    @Override
    public RoundHandle executeAutomatic(BoundReleasePlan plan, ReleaseExecutionContext context, LifetimeToken token) {
        if (plan == null || context == null || token == null) {
            throw new IllegalArgumentException("自动释放缺少计划、上下文或令牌");
        }
        ServerPlayer owner = context.owner();
        MinecraftServer server = owner == null ? null : owner.getServer();
        int nowTick = server == null ? 0 : server.getTickCount();
        if (owner == null || server == null || plan.members().isEmpty() || !token.valid(nowTick)) {
            return new ClosedRound(context.roundId());
        }
        Skill frozenSkill = plan.frozenSkill();
        List<Technique> techniques = frozenSkill.getTechniques();
        List<ChainSegment> segments = TechniqueChain.split(frozenSkill);
        int[] shares = quoteShares(plan, frozenSkill, techniques, segments);
        if (shares == null) {
            return new ClosedRound(context.roundId());
        }
        int roundCost = 0;
        for (int share : shares) {
            roundCost += Math.max(0, share);
        }
        GroupBudget budget = GroupBudget.reserve(owner, roundCost, false);
        if (budget == null) {
            return new ClosedRound(context.roundId());
        }
        boolean needsHost = false;
        for (BoundMember member : plan.members()) {
            ReleaseDefinition definition = member.technique() == null ? null : ReleaseRegistry.get(member.technique().getId());
            if (definition != null && definition.submitsHostGroup()) {
                needsHost = true;
                break;
            }
        }
        HostCast hostCast = needsHost ? HostCast.open(owner.getUUID()) : null;
        OpenRound round = new OpenRound(context.roundId(), owner, budget, hostCast);
        ReleaseBatch batch = ReleaseBatch.open(owner, frozenSkill);
        ReleaseBatch.unbind();
        AtomicInteger remaining = new AtomicInteger(plan.members().size());
        try {
            if (plan.members().size() >= 2) {
                int secondTick = nowTick + CastExecutionSession.STEP_GAP;
                ChainScheduler.schedule(server, secondTick, () -> launchMember(round, plan, context, token, frozenSkill,
                        techniques, segments, batch, hostCast, shares, 1, remaining));
            }
            launchMember(round, plan, context, token, frozenSkill, techniques, segments, batch, hostCast, shares, 0, remaining);
            return round;
        } catch (RuntimeException failure) {
            round.finish();
            throw failure;
        }
    }

    private static int[] quoteShares(BoundReleasePlan plan, Skill frozenSkill, List<Technique> techniques,
                                     List<ChainSegment> segments) {
        int memberCount = Math.max(1, plan.members().size());
        int[] shares = new int[memberCount];
        if (plan.dual()) {
            CompiledReleaseStep compiled = CompiledReleaseStep.compile(frozenSkill, techniques, segments, 0.0);
            if (compiled == null) {
                return null;
            }
            for (int memberIndex = 0; memberIndex < shares.length && memberIndex < compiled.memberShares.size(); memberIndex++) {
                shares[memberIndex] = Math.max(0, compiled.memberShares.get(memberIndex));
            }
            if (shares.length > 0) {
                shares[0] += Math.max(0, compiled.ruleShare);
            }
            return shares;
        }
        shares[0] = Math.max(0, SkillChainPlan.pressCommitCost(techniques, segments));
        return shares;
    }

    private static void launchMember(OpenRound round, BoundReleasePlan plan, ReleaseExecutionContext context,
                                     LifetimeToken token, Skill frozenSkill, List<Technique> techniques,
                                     List<ChainSegment> segments, ReleaseBatch batch, HostCast hostCast,
                                     int[] shares, int memberIndex, AtomicInteger remaining) {
        if (round.done() || round.cancelled()) {
            return;
        }
        ServerPlayer owner = context.owner();
        MinecraftServer server = owner.getServer();
        int nowTick = server == null ? 0 : server.getTickCount();
        if (server == null || token == null || !token.valid(nowTick)) {
            completeMember(round, remaining);
            return;
        }
        BoundMember member = memberIndex >= 0 && memberIndex < plan.members().size() ? plan.members().get(memberIndex) : null;
        ReleaseDefinition definition = member == null || member.technique() == null
                ? null
                : ReleaseRegistry.get(member.technique().getId());
        int chargeTicks = definition == null ? 0 : definition.chargeTicks();
        if (chargeTicks > 0) {
            ChainScheduler.schedule(server, nowTick + chargeTicks, () -> {
                if (round.done() || round.cancelled()) {
                    return;
                }
                int firedTick = server.getTickCount();
                if (!token.valid(firedTick)) {
                    completeMember(round, remaining);
                    return;
                }
                WaterMomentum.Hold momentumHold = WaterMomentum.reserve(owner, definition.momentumSpend());
                SkillSegmentRunner.openChargeBypass();
                try {
                    WaterMomentum.commit(owner, momentumHold);
                    boolean spawned = spawnMember(round, plan, context, token, frozenSkill, techniques, segments,
                            batch, hostCast, memberIndex);
                    if (spawned) {
                        round.budget.commit(shareAt(shares, memberIndex));
                    }
                } catch (RuntimeException failure) {
                    round.finish();
                    throw failure;
                } finally {
                    SkillSegmentRunner.closeChargeBypass();
                    if (!round.done()) {
                        completeMember(round, remaining);
                    }
                }
            });
            return;
        }
        try {
            boolean spawned = spawnMember(round, plan, context, token, frozenSkill, techniques, segments,
                    batch, hostCast, memberIndex);
            if (spawned && !round.done()) {
                round.budget.commit(shareAt(shares, memberIndex));
            }
        } catch (RuntimeException failure) {
            round.finish();
            throw failure;
        }
        if (!round.done()) {
            completeMember(round, remaining);
        }
    }

    private static void completeMember(OpenRound round, AtomicInteger remaining) {
        if (round.done()) {
            return;
        }
        if (remaining.decrementAndGet() <= 0) {
            round.finish();
        }
    }

    private static int shareAt(int[] shares, int memberIndex) {
        if (shares == null || memberIndex < 0 || memberIndex >= shares.length) {
            return 0;
        }
        return shares[memberIndex];
    }

    private static boolean spawnMember(OpenRound round, BoundReleasePlan plan, ReleaseExecutionContext context,
                                       LifetimeToken token, Skill frozenSkill, List<Technique> techniques,
                                       List<ChainSegment> segments, ReleaseBatch batch, HostCast hostCast,
                                       int memberIndex) {
        if (round.done() || round.cancelled()) {
            return false;
        }
        ServerPlayer owner = context.owner();
        int nowTick = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
        if (token == null || !token.valid(nowTick)) {
            return false;
        }
        if (memberIndex < 0 || memberIndex >= plan.members().size()) {
            return false;
        }
        BoundMember member = plan.members().get(memberIndex);
        Technique technique = member.technique();
        if (technique == null || !HeldSwordGate.allows(technique, owner.getMainHandItem())) {
            return false;
        }
        int segmentIndex = segmentIndexOf(segments, member.localIndex());
        int copies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, segmentIndex));
        ChainSegment segment = new ChainSegment(member.localIndex(), member.enchantLocalIndexes());
        EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(frozenSkill, segment);
        EnchantBonus bonus = EnchantBonus.resolve(frozenSkill, member.enchantLocalIndexes(), technique.getBaseDamage(), 1);
        float damage = member.enchantLocalIndexes().isEmpty()
                ? SkillSegmentMath.segmentDamage(technique, segment, context.weaponBonus(),
                SkillSegmentMath.segmentMagicDamage(frozenSkill, segment), null)
                : bonus.carriedDamage(context.weaponBonus());
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        boolean fieldHost = definition != null && definition.fieldRadius() > 0f;
        boolean carries = !fieldHost && technique.getBaseDamage() > 0f && !member.enchantLocalIndexes().isEmpty();
        boolean lockCast = SkillChainPlan.lockThisCast(techniques, member.localIndex());
        boolean dropCast = SkillChainPlan.dropThisCast(techniques, segments, member.localIndex());
        ReleaseInput chainInput = member.chainInput();
        EnchantmentSnapshot snapshot = ContactCast.snapshot(frozenSkill, segment, owner);
        ItemStack weaponCopy = context.weaponCopy();
        Vec3 origin = context.origin();
        Vec3 direction = context.baseDirection();
        ReleaseBatch previousBatch = ReleaseBatch.current();
        ReleaseBatch.bind(batch);
        ReleaseExecutionContext.bind(context);
        try {
            if (definition != null && definition.submitsHostGroup()) {
                if (hostCast == null || round.done() || !token.valid(nowTick)) {
                    return false;
                }
                ReleaseBatch.Tally tally = copies > 1 && batch != null ? batch.openGroupTally() : null;
                UUID hostGroup = hostCast.hostId(memberIndex, 0);
                ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(owner, technique, techniques, member.localIndex(),
                        weaponCopy, damage, carries, enchantments, lockCast, dropCast, tally, copies, batch, bonus,
                        memberIndex, direction, origin, null, null, hostCast, hostGroup, null, chainInput);
                if (outcome.committed() && fieldHost && snapshot != null) {
                    FieldZones.bindBorn(owner.getUUID(), snapshot, outcome.tick());
                }
                return outcome.committed();
            }
            ReleaseBatch.Tally tally = copies > 1 && batch != null ? batch.openGroupTally() : null;
            boolean anyCommitted = false;
            int commitTick = nowTick;
            for (int copyIndex = 0; copyIndex < copies; copyIndex++) {
                int copyTick = owner.getServer() == null ? nowTick : owner.getServer().getTickCount();
                if (round.done() || round.cancelled() || !token.valid(copyTick)) {
                    break;
                }
                if (!HeldSwordGate.allows(technique, owner.getMainHandItem())) {
                    break;
                }
                SkillSegmentRunner.clearAutomaticSpawn();
                SkillSegmentRunner.runSegment(owner, technique, techniques, member.localIndex(), weaponCopy, damage, carries,
                        enchantments, null, lockCast, dropCast, tally, copyIndex, copies, batch, direction, origin,
                        bonus, memberIndex, null, null, null, chainInput, null);
                if (SkillSegmentRunner.consumeAutomaticSpawn()) {
                    anyCommitted = true;
                    commitTick = owner.getServer() == null ? copyTick : owner.getServer().getTickCount();
                }
            }
            if (!anyCommitted) {
                return false;
            }
            if (fieldHost && snapshot != null) {
                FieldZones.bindBorn(owner.getUUID(), snapshot, commitTick);
            }
            return true;
        } finally {
            ReleaseExecutionContext.unbind();
            ReleaseBatch.bind(previousBatch);
        }
    }

    private static int segmentIndexOf(List<ChainSegment> segments, int localIndex) {
        if (segments == null) {
            return 0;
        }
        for (int candidate = 0; candidate < segments.size(); candidate++) {
            if (segments.get(candidate).actionIndex() == localIndex) {
                return candidate;
            }
        }
        return 0;
    }

    private static final class OpenRound implements RoundHandle {
        private final UUID roundIdentity;
        private final ServerPlayer owner;
        private final GroupBudget budget;
        private final HostCast hostCast;
        private final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean(false);
        private volatile boolean cancelled;

        private OpenRound(UUID roundIdentity, ServerPlayer owner, GroupBudget budget, HostCast hostCast) {
            this.roundIdentity = roundIdentity;
            this.owner = owner;
            this.budget = budget;
            this.hostCast = hostCast;
        }

        @Override
        public UUID roundId() {
            return roundIdentity;
        }

        @Override
        public void cancel() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            cancelled = true;
            settleBudget();
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public boolean done() {
            return finished.get();
        }

        private void finish() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            settleBudget();
        }

        private void settleBudget() {
            if (budget != null) {
                budget.settle(owner);
            }
            if (hostCast != null) {
                hostCast.close();
            }
        }
    }

    private static final class ClosedRound implements RoundHandle {
        private final UUID roundIdentity;

        private ClosedRound(UUID roundIdentity) {
            this.roundIdentity = roundIdentity == null ? UUID.randomUUID() : roundIdentity;
        }

        @Override
        public UUID roundId() {
            return roundIdentity;
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean cancelled() {
            return false;
        }

        @Override
        public boolean done() {
            return true;
        }
    }
}
