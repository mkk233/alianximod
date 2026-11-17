package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.WeaponEnchant;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupCommit;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class SkillChainPlan {
    static Integer pressCommitCost(List<Technique> techniques, List<ChainSegment> segments) {
        return pressCommitCost(techniques, segments, 0, null);
    }

    static Integer pressCommitCost(List<Technique> techniques, List<ChainSegment> segments, CastPlanContext planContext) {
        return pressCommitCost(techniques, segments, 0, planContext);
    }

    static Integer pressCommitCost(List<Technique> techniques, List<ChainSegment> segments, int startIndex) {
        return pressCommitCost(techniques, segments, startIndex, null);
    }

    /** 合计放不进 int 时返回空。调用方要在扣款和释放前拒绝，不能把空或负数交给魔力池。 */
    static Integer pressCommitCost(List<Technique> techniques, List<ChainSegment> segments, int startIndex,
                               CastPlanContext planContext) {
        Integer cost = 0;
        int segmentIndex = Math.max(0, startIndex);
        while (segmentIndex < segments.size()) {
            ChainSegment segment = segments.get(segmentIndex);
            Technique action = techniques.get(segment.actionIndex());

            if (SkillSegmentMath.isRemovedRule(action.getId())) {
                segmentIndex += 1;
                continue;
            }
            ChainRole role = SkillSegmentMath.roleOf(action);
            if (SkillSegmentMath.isSelfStatus(action) && role != ChainRole.RULE_COOPERATIVE) {
                segmentIndex += 1;
                continue;
            }
            if (role == ChainRole.ACTION) {
                SkillChainPlan.ModifierPlan plan = collectModifiers(null, segments, techniques, segmentIndex + 1, planContext);
                cost = quoteAdd(cost, SkillSegmentMath.segmentManaCost(techniques, segment));
                if (cost == null) {
                    return null;
                }
                if (plan.dualRuleSegment >= 0) {
                    cost = quoteAdd(cost, consumedModifierQuotes(techniques, segments, segmentIndex + 1, plan.resume));
                    cost = quoteAdd(cost, dualSequenceCost(techniques, segments, plan, planContext));
                    if (cost == null) {
                        return null;
                    }
                    segmentIndex = plan.resume;
                } else if (plan.attackSegment >= 0) {
                    cost = quoteAdd(cost, consumedModifierQuotes(techniques, segments, segmentIndex + 1, plan.resume));
                    int attackCopies = pricedCopies(techniques.get(segments.get(plan.attackSegment).actionIndex()), plan.copies);
                    cost = quoteAdd(cost, scaledSegmentCost(techniques, segments.get(plan.attackSegment), attackCopies, plan.volleys));
                    cost = quoteAdd(cost, fieldSpawnExtra(techniques, segments.get(plan.attackSegment).actionIndex(), attackCopies));
                    if (cost == null) {
                        return null;
                    }
                    segmentIndex = plan.resume;
                } else {
                    segmentIndex += 1;
                }
                continue;
            }
            if (SkillSegmentMath.isChainModifier(action)) {
                SkillChainPlan.ModifierPlan plan = collectModifiers(null, segments, techniques, segmentIndex, planContext);
                cost = quoteAdd(cost, consumedModifierQuotes(techniques, segments, segmentIndex, plan.resume));
                if (plan.dualRuleSegment >= 0) {
                    cost = quoteAdd(cost, dualSequenceCost(techniques, segments, plan, planContext));
                } else if (plan.attackSegment >= 0) {
                    int attackCopies = pricedCopies(techniques.get(segments.get(plan.attackSegment).actionIndex()), plan.copies);
                    cost = quoteAdd(cost, scaledSegmentCost(techniques, segments.get(plan.attackSegment), attackCopies, plan.volleys));
                    cost = quoteAdd(cost, fieldSpawnExtra(techniques, segments.get(plan.attackSegment).actionIndex(), attackCopies));
                }
                if (cost == null) {
                    return null;
                }
                segmentIndex = Math.max(plan.resume, segmentIndex + 1);
                continue;
            }

            ChainSegment nextSegment = (segmentIndex + 1 < segments.size()) ? segments.get(segmentIndex + 1) : null;
            boolean chainedToTrigger = nextSegment != null
                    && SkillSegmentMath.releaseKind(action.getId()) == ReleaseKind.ACTION
                    && SkillSegmentMath.releaseKind(techniques.get(nextSegment.actionIndex()).getId()) != ReleaseKind.RULE
                    && SkillSegmentMath.isTriggerRelease(techniques.get(nextSegment.actionIndex()).getId())
                    && !SkillSegmentMath.manipulatesSwordEntities(techniques.get(nextSegment.actionIndex()).getId())
                    && !SkillSegmentMath.isConditionalRelease(techniques.get(nextSegment.actionIndex()));

            if (chainedToTrigger) {
                cost = quoteAdd(cost, SkillSegmentMath.segmentManaCost(techniques, segment));
                if (cost == null) {
                    return null;
                }
                segmentIndex += 2;
                continue;
            }

            cost = quoteAdd(cost, SkillSegmentMath.settledSegmentCost(techniques, segment));
            cost = quoteAdd(cost, fieldSpawnExtra(techniques, segment.actionIndex(),
                    copiesBefore(segments, techniques, segmentIndex, planContext)));
            if (cost == null) {
                return null;
            }
            segmentIndex += 1;
        }
        return cost;
    }

    /** 这次按键吃掉的链上修饰，各自只加一次标准报价，不乘后面的份数。 */
    private static Integer consumedModifierQuotes(List<Technique> techniques, List<ChainSegment> segments,
                                                  int fromIndex, int resumeIndex) {
        Integer quoted = 0;
        int endIndex = Math.min(Math.max(resumeIndex, 0), segments.size());
        for (int modifierIndex = Math.max(0, fromIndex); modifierIndex < endIndex; modifierIndex++) {
            ChainSegment modifierSegment = segments.get(modifierIndex);
            Technique modifierTechnique = techniques.get(modifierSegment.actionIndex());
            if (modifierTechnique == null || SkillSegmentMath.isRemovedRule(modifierTechnique.getId())) {
                continue;
            }
            if (modifierTechnique.getBaseDamage() > 0 && !modifierTechnique.isEnchantment()) {
                continue;
            }
            if (!SkillSegmentMath.isChainModifier(modifierTechnique)) {
                continue;
            }
            quoted = quoteAdd(quoted, SkillSegmentMath.segmentManaCost(techniques, modifierSegment));
            if (quoted == null) {
                return null;
            }
        }
        return quoted;
    }

    private static Integer scaledSegmentCost(List<Technique> techniques, ChainSegment segment, int copies, int volleys) {
        Integer unitCost = SkillSegmentMath.segmentManaCost(techniques, segment);
        if (unitCost == null) {
            return null;
        }
        Integer copiedCost = SkillSegmentMath.exactMultiply(unitCost, copies);
        if (copiedCost == null) {
            return null;
        }
        return SkillSegmentMath.exactMultiply(copiedCost, volleys);
    }

    private static Integer quoteAdd(Integer total, Integer part) {
        if (total == null || part == null) {
            return null;
        }
        return SkillSegmentMath.exactAdd(total, part);
    }

    static SkillChainPlan.ModifierPlan collectModifiers(Skill skill, List<ChainSegment> segments, List<Technique> techniques, int from) {
        return collectModifiers(skill, segments, techniques, from, null);
    }

    static SkillChainPlan.ModifierPlan collectModifiers(Skill skill, List<ChainSegment> segments, List<Technique> techniques, int from,
                                                        CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure()) {
            SkillChainPlan.ModifierPlan cached = planContext.modifierPlan(from, skill != null);
            if (cached != null) {
                return cached;
            }
        }
        SkillChainPlan.ModifierPlan plan = scanModifiers(segments, techniques, from);
        plan.captureChainInputs(skill, segments, techniques);
        return plan;
    }

    static ModifierTemplate captureModifierTemplate(List<ChainSegment> segments, List<Technique> techniques, int from, Skill skill) {
        ModifierPlan scanned = scanModifiers(segments, techniques, from);
        scanned.captureChainInputs(skill, segments, techniques);
        return new ModifierTemplate(scanned);
    }

    private static SkillChainPlan.ModifierPlan scanModifiers(List<ChainSegment> segments, List<Technique> techniques, int from) {
        SkillChainPlan.ModifierPlan plan = new SkillChainPlan.ModifierPlan();
        int segmentIndex = from;
        while (segmentIndex < segments.size()) {
            Technique technique = techniques.get(segments.get(segmentIndex).actionIndex());
            if (SkillSegmentMath.isRemovedRule(technique.getId())) {
                segmentIndex++;
                continue;
            }
            ChainRole role = SkillSegmentMath.roleOf(technique);
            if (technique.getBaseDamage() > 0 && !technique.isEnchantment()) {
                plan.attackSegment = segmentIndex;
                segmentIndex++;
                break;
            }
            if (!SkillSegmentMath.isChainModifier(technique)) {
                break;
            }
            switch (role) {
                case SEEK_SPACE -> plan.spaceLock = true;
                case SEEK_ENTITY -> plan.entityLock = true;
                case TRAJECTORY_DROP -> {
                    ReleaseDefinition pathDefinition = ReleaseRegistry.get(technique.getId());
                    plan.pathOverride = pathDefinition != null ? pathDefinition.trajectory() : null;
                    plan.drop = plan.pathOverride == Trajectory.DROP;
                }
                case TRAJECTORY_TRACKING -> plan.tracking = true;
                case TRAJECTORY_SCATTER -> plan.scatter = true;
                case RULE_TRIPLE -> plan.copies *= 3;
                case RULE_DUAL -> plan.dualRuleSegment = segmentIndex;
                default -> {
                }
            }
            segmentIndex++;
        }
        plan.resume = segmentIndex;
        if (plan.dualRuleSegment >= 0) {
            List<ChainSegment> released = releasedSegments(segments, plan.dualRuleSegment, techniques);
            int lastSegment = plan.dualRuleSegment;
            for (ChainSegment member : released) {
                int memberIndex = segmentIndexOfHost(segments, member.actionIndex());
                if (memberIndex > lastSegment) {
                    lastSegment = memberIndex;
                }
            }
            plan.resume = Math.max(plan.resume, lastSegment + 1);
        }
        return plan;
    }

    static int firePlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                          SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, ManaPaymentReceipt receipt,
                          CastSource castSource) {
        return firePlan(player, skill, techniques, segments, plan, weaponBonus, startTick, receipt, castSource, null);
    }

    static int firePlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                          SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, ManaPaymentReceipt receipt,
                          CastSource castSource, CastPlanContext planContext) {
        if (plan.dualRuleSegment >= 0) {
            return fireDualSequence(player, skill, techniques, segments, plan, weaponBonus, startTick, castSource, planContext);
        }
        Integer attackShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(plan.attackSegment));
        if (attackShare == null) {
            return -1;
        }
        int memberCount = Math.max(0, plan.volleys) * Math.max(0, plan.copies);
        return firePaidPlan(player, skill, techniques, segments, plan, weaponBonus, startTick,
                UncommittedShare.reserve(receipt, attackShare, memberCount), castSource, planContext);
    }

    static int firePaidPlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                             SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, UncommittedShare.Pool shares,
                             CastSource castSource) {
        return firePaidPlan(player, skill, techniques, segments, plan, weaponBonus, startTick, shares, castSource, null);
    }

    static int firePaidPlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                             SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, UncommittedShare.Pool shares,
                             CastSource castSource, CastPlanContext planContext) {
        ChainSegment attackSeg = segments.get(plan.attackSegment);
        Technique attack = techniques.get(attackSeg.actionIndex());
        EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, attackSeg);
        EnchantBonus bonus = EnchantBonus.resolve(skill, attackSeg.enchantIndexes(), attack.getBaseDamage(), 1);
        float damage = SkillSegmentMath.configuredSegmentDamage(attack, attackSeg, weaponBonus,
                SkillSegmentMath.segmentMagicDamage(skill, attackSeg), null, bonus);
        boolean carries = !attackSeg.enchantIndexes().isEmpty();
        ReleaseDefinition definition = ReleaseRegistry.get(attack.getId());
        boolean mutable = definition != null && definition.trajectoryMutable();
        boolean dedicatedAim = definition != null && definition.dedicatedInitialAim();
        ReleaseBatch batch = ReleaseBatch.current();
        ItemStack weapon = player.getMainHandItem();
        ServerLevel castLevel = player.serverLevel();
        int castEpoch = ChargeGate.epoch(player.getUUID());
        int finishedTick = startTick + Math.max(0, plan.volleys - 1) * DUAL_STEP_TICKS;
        if (shares == null || shares.isClosed()) {
            return finishedTick;
        }
        ReleaseInput chainInput = plan.attackInput();
        if (definition != null && definition.submitsHostGroup()) {
            HostCast hostCast = HostCast.open(player.getUUID(), castSource);
            hostCast.hold();
            try {
                for (int volley = 0; volley < plan.volleys; volley++) {
                    int volleyIndex = volley;
                    List<UncommittedShare> hostClaims = new ArrayList<>();
                    boolean completeHost = true;
                    for (int copy = 0; copy < plan.copies; copy++) {
                        UncommittedShare claim = shares.take();
                        if (claim == null) {
                            completeHost = false;
                            break;
                        }
                        hostClaims.add(claim);
                    }
                    if (!completeHost) {
                        for (UncommittedShare claim : hostClaims) {
                            claim.cancel(player);
                        }
                        return finishedTick;
                    }
                    ReleaseBatch.Tally tally = plan.copies > 1 && batch != null ? batch.openGroupTally() : null;
                    int tick = startTick + volleyIndex * DUAL_STEP_TICKS;
                    hostCast.hold();
                    UUID hostGroup = hostCast.hostId(0, volleyIndex);
                    AtomicBoolean once = new AtomicBoolean(false);
                    AtomicBoolean released = new AtomicBoolean(false);
                    Runnable releaseVolley = () -> {
                        if (released.compareAndSet(false, true)) {
                            hostCast.release();
                        }
                    };
                    try {
                        ChainScheduler.schedule(player.getServer(), tick, () -> {
                            if (!once.compareAndSet(false, true)) {
                                return;
                            }
                            try {
                                int nowTick = player.getServer() == null ? tick : player.getServer().getTickCount();
                                if (!castContinues(player, castLevel, castEpoch) || !HeldSwordGate.allows(attack, player.getMainHandItem())) {
                                    HostGroupCommit.cancel(hostCast, hostGroup, nowTick, "施法已经中断");
                                    for (UncommittedShare claim : hostClaims) {
                                        claim.cancel(player);
                                    }
                                    return;
                                }
                                ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(player, attack, techniques, attackSeg.actionIndex(), weapon,
                                        damage, carries, enchantments,
                                        plan.entityLock && (mutable || dedicatedAim), plan.drop && mutable,
                                        tally, plan.copies, batch, bonus, volleyIndex, null, null, null, null, hostCast, hostGroup, null,
                                        chainInput, planContext);
                                if (outcome.committed()) {
                                    for (UncommittedShare claim : hostClaims) {
                                        claim.commit();
                                    }
                                } else {
                                    for (UncommittedShare claim : hostClaims) {
                                        claim.cancel(player);
                                    }
                                }
                            } finally {
                                releaseVolley.run();
                            }
                        });
                    } catch (RuntimeException failure) {
                        releaseVolley.run();
                        throw failure;
                    }
                }
                return finishedTick;
            } finally {
                hostCast.release();
            }
        }
        for (int volley = 0; volley < plan.volleys; volley++) {
            ReleaseBatch.Tally tally = plan.copies > 1 && batch != null ? batch.openGroupTally() : null;
            int tick = startTick + volley * DUAL_STEP_TICKS;
            for (int copy = 0; copy < plan.copies; copy++) {
                int copyIndex = copy;
                int volleyIndex = volley;
                UncommittedShare claim = shares.take();
                if (claim == null) {
                    return finishedTick;
                }
                AtomicBoolean once = new AtomicBoolean(false);
                Runnable runner = SkillSegmentRunner.buildSegmentRunner(player, attack, techniques, attackSeg.actionIndex(), weapon,
                        damage, carries, enchantments, null,
                        plan.entityLock && (mutable || dedicatedAim), plan.drop && mutable,
                        tally, copyIndex, plan.copies, batch,
                        bonus, volleyIndex, castSource, chainInput, planContext);
                ChainScheduler.schedule(player.getServer(), tick, () -> {
                    if (!once.compareAndSet(false, true)) {
                        return;
                    }
                    if (!castContinues(player, castLevel, castEpoch) || !HeldSwordGate.allows(attack, player.getMainHandItem())) {
                        claim.cancel(player);
                        return;
                    }
                    runner.run();
                    claim.commit();
                });
            }
        }
        return finishedTick;
    }

    static int fireDualSequence(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                  SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, CastSource castSource) {
        return fireDualSequence(player, skill, techniques, segments, plan, weaponBonus, startTick, castSource, null);
    }

    static int fireDualSequence(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                  SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, CastSource castSource,
                                  CastPlanContext planContext) {
        return fireDualSequence(player, skill, techniques, segments, plan, weaponBonus, startTick, castSource, planContext, null);
    }

    static int fireDualSequence(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                  SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, CastSource castSource,
                                  CastPlanContext planContext, DomainOpening domainOpening) {
        List<ChainSegment> released = releasedSegments(segments, plan.dualRuleSegment, techniques, planContext);
        MulticastExpansion sequenceExpansion = expansionAtRule(segments, plan.dualRuleSegment, techniques);
        int lastTick = startTick;
        int attackOrdinal = 0;
        ReleaseBatch batch = ReleaseBatch.current();
        ItemStack weapon = player.getMainHandItem();
        boolean dualEnchantImprinted = false;
        int releasedOrdinal = 0;
        for (ChainSegment member : released) {
            ReleaseInput chainInput = memberChainInput(skill, techniques, member.actionIndex(), plan.dualInput(releasedOrdinal), planContext);
            releasedOrdinal++;
            Technique memberTechnique = techniques.get(member.actionIndex());
            ReleaseDefinition memberDefinition = ReleaseRegistry.get(memberTechnique.getId());
            if (memberTechnique.getBaseDamage() <= 0f && memberDefinition != null && memberDefinition.domain() != null) {
                int sequenceTick = startTick + sequenceGap(sequenceExpansion, attackOrdinal);
                int memberCopies = memberCopyCount(segments, techniques, member, planContext);
                EnchantmentSnapshot domainSnapshot = ContactCast.snapshot(skill, member, player);
                BoundReleasePlan boundPlan = domainOpening == null ? null : domainOpening.planFor(member.actionIndex());
                String boundClaim = domainOpening == null ? null : domainOpening.claimFor(boundPlan);
                boolean placedNow = sequenceTick <= player.getServer().getTickCount();
                ServerLevel domainLevel = player.serverLevel();
                int domainEpoch = ChargeGate.epoch(player.getUUID());
                DomainWave.Held domainHeld = DomainWave.hold(player, skill, memberTechnique, member.enchantIndexes());
                UUID domainGroup = domainOpening == null ? UUID.randomUUID() : domainOpening.releaseIdentity();
                com.bslx.XueXiMod.shu.technique.release.domain.motion.DomainCastContext domainContext =
                        SkillSegmentRunner.domainSpace(player, techniques, member.actionIndex(), domainGroup,
                                false, null, null);
                if (placedNow) {
                    boolean domainPlaced;
                    try {
                        domainPlaced = DomainWave.open(player, memberTechnique, memberDefinition.domain(), memberCopies,
                                domainSnapshot, chainInput, boundPlan, boundClaim, domainHeld, domainContext);
                    } finally {
                        DomainWave.release(domainHeld);
                    }
                    if (!domainPlaced) {
                        if (attackOrdinal == 0) {
                            return -1;
                        }
                        continue;
                    }
                }
                if (!placedNow) {
                    ChainScheduler.schedule(player.getServer(), sequenceTick, () -> {
                        try {
                            if (!castContinues(player, domainLevel, domainEpoch)) {
                                return;
                            }
                            DomainWave.open(player, memberTechnique, memberDefinition.domain(), memberCopies,
                                    domainSnapshot, chainInput, boundPlan, boundClaim, domainHeld, domainContext);
                        } finally {
                            DomainWave.release(domainHeld);
                        }
                    });
                }
                lastTick = sequenceTick;
                attackOrdinal++;
                continue;
            }
            if (memberTechnique.getBaseDamage() <= 0f && memberDefinition != null && memberDefinition.barrier() != null) {
                int sequenceTick = startTick + sequenceGap(sequenceExpansion, attackOrdinal);
                int memberCopies = memberCopyCount(segments, techniques, member, planContext);
                EnchantmentSnapshot barrierSnapshot = ContactCast.snapshot(skill, member, player);
                boolean placedNow = sequenceTick <= player.getServer().getTickCount();
                ServerLevel barrierLevel = player.serverLevel();
                int barrierEpoch = ChargeGate.epoch(player.getUUID());
                if (placedNow && !com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService.tryPlace(
                        player, memberDefinition.barrier(), memberCopies, barrierSnapshot, chainInput)) {
                    if (attackOrdinal == 0) {
                        return -1;
                    }
                    continue;
                }
                if (!placedNow) {
                    ChainScheduler.schedule(player.getServer(), sequenceTick, () -> {
                        if (!castContinues(player, barrierLevel, barrierEpoch)) {
                            return;
                        }
                        com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService.tryPlace(
                                player, memberDefinition.barrier(), memberCopies, barrierSnapshot, chainInput);
                    });
                }
                lastTick = sequenceTick;
                attackOrdinal++;
                continue;
            }
            if (memberTechnique.getBaseDamage() <= 0f && memberDefinition != null && memberDefinition.guard() != null) {
                int sequenceTick = startTick + sequenceGap(sequenceExpansion, attackOrdinal);
                long waveKey = ((long) startTick << 8) + member.actionIndex() + attackOrdinal * 32L;
                if (!GuardStates.schedule(player, memberDefinition.guard(), sequenceTick, waveKey, chainInput)) {
                    if (attackOrdinal == 0) {
                        return -1;
                    }
                    continue;
                }
                EnchantmentSnapshot guardSnapshot = ContactCast.snapshot(skill, member, player);
                ServerLevel guardLevel = player.serverLevel();
                int guardEpoch = ChargeGate.epoch(player.getUUID());
                if (guardSnapshot != null) {
                    ChainScheduler.schedule(player.getServer(), sequenceTick, () -> {
                        if (!castContinues(player, guardLevel, guardEpoch)) {
                            return;
                        }
                        GuardStates.bindContact(player, guardSnapshot);
                    });
                }
                lastTick = sequenceTick;
                attackOrdinal++;
                continue;
            }
            if (memberTechnique.isEnchantment()) {
                if (!dualEnchantImprinted) {
                    int memberCopies = memberCopyCount(segments, techniques, member, planContext);
                    java.util.ArrayList<Integer> loose = new java.util.ArrayList<>();
                    for (ChainSegment releasedMember : released) {
                        if (techniques.get(releasedMember.actionIndex()).isEnchantment()) {
                            loose.add(releasedMember.actionIndex());
                        }
                    }
                    imprintSelection(player, skill, loose, Math.max(1, memberCopies));
                    dualEnchantImprinted = true;
                }
                lastTick = startTick + sequenceGap(sequenceExpansion, attackOrdinal);
                attackOrdinal++;
                continue;
            }
            int memberCopies = memberCopyCount(segments, techniques, member, planContext);
            EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
            EnchantBonus bonus = EnchantBonus.resolve(skill, member.enchantIndexes(), memberTechnique.getBaseDamage(), 1);
            float damage = SkillSegmentMath.configuredSegmentDamage(memberTechnique, member, weaponBonus,
                    SkillSegmentMath.segmentMagicDamage(skill, member), null, bonus);
            ReleaseDefinition definition = ReleaseRegistry.get(memberTechnique.getId());
            EnchantmentSnapshot fieldSnapshot = memberTechnique.getBaseDamage() <= 0f
                    && definition != null && definition.fieldRadius() > 0f
                    ? ContactCast.snapshot(skill, member, player) : null;
            boolean carries = fieldSnapshot == null && memberTechnique.getBaseDamage() > 0f && !member.enchantIndexes().isEmpty();
            boolean mutable = definition != null && definition.trajectoryMutable();
            MemberCastView castView = memberViewFor(skill, techniques, member.actionIndex());
            boolean lockCast = castView != null
                    ? lockThisCast(techniques, member.actionIndex(), planContext)
                    : (plan.entityLock && mutable) || lockThisCast(techniques, member.actionIndex(), planContext);
            boolean dropCast = castView != null
                    ? dropThisCast(techniques, segments, member.actionIndex(), planContext)
                    : (plan.drop && mutable) || dropThisCast(techniques, segments, member.actionIndex(), planContext);
            int sequenceTick = startTick + sequenceGap(sequenceExpansion, attackOrdinal);
            lastTick = sequenceTick;
            ReleaseBatch.Tally tally = memberCopies > 1 && batch != null ? batch.openGroupTally() : null;
            ServerLevel castLevel = player.serverLevel();
            for (int copy = 0; copy < memberCopies; copy++) {
                int copyIndex = copy;
                int volleyIndex = attackOrdinal;
                Runnable runner = SkillSegmentRunner.buildSegmentRunner(player, memberTechnique, techniques, member.actionIndex(), weapon,
                        damage, carries, enchantments, null,
                        lockCast, dropCast,
                        tally, copyIndex, memberCopies, batch,
                        bonus, volleyIndex, castSource, chainInput, planContext);
                ChainScheduler.schedule(player.getServer(), sequenceTick, () -> {
                    if (!player.isAlive() || player.level() != castLevel) {
                        return;
                    }
                    runner.run();
                    if (fieldSnapshot != null && copyIndex == 0) {
                        com.bslx.XueXiMod.shu.technique.release.field.FieldZones.bindBorn(
                                player.getUUID(), fieldSnapshot, player.getServer().getTickCount());
                    }
                });
            }
            attackOrdinal++;
        }
        return lastTick;
    }

    static Integer dualSequenceCost(List<Technique> techniques, List<ChainSegment> segments, SkillChainPlan.ModifierPlan plan) {
        return dualSequenceCost(techniques, segments, plan, null);
    }

    static Integer dualSequenceCost(List<Technique> techniques, List<ChainSegment> segments, SkillChainPlan.ModifierPlan plan,
                                CastPlanContext planContext) {
        Integer cost = 0;
        List<ChainSegment> released = releasedSegments(segments, plan.dualRuleSegment, techniques, planContext);
        for (ChainSegment member : released) {
            Technique memberTechnique = techniques.get(member.actionIndex());
            ReleaseDefinition memberDefinition = ReleaseRegistry.get(memberTechnique.getId());
            boolean dynamicUnit = SkillSegmentMath.unitManaCost(memberTechnique) > 0;
            if (memberTechnique.getBaseDamage() <= 0f && memberDefinition != null
                    && (memberDefinition.guard() != null || memberDefinition.barrier() != null || memberDefinition.domain() != null
                    || ConditionalEffects.copiesCoalesce(memberDefinition) || dynamicUnit)) {
                cost = quoteAdd(cost, SkillSegmentMath.settledSegmentCost(techniques, member));
                if (cost == null) {
                    return null;
                }
                continue;
            }
            int memberCopies = memberCopyCount(segments, techniques, member, planContext);
            cost = quoteAdd(cost, scaledSegmentCost(techniques, member, memberCopies, 1));
            if (memberDefinition != null && memberDefinition.fieldRadius() > 0f) {
                cost = quoteAdd(cost, fieldSpawnExtra(techniques, member.actionIndex(), memberCopies));
            }
            if (cost == null) {
                return null;
            }
        }
        return cost;
    }

    private static int pricedCopies(Technique technique, int copies) {
        if (technique != null && technique.getBaseDamage() <= 0f
                && ConditionalEffects.copiesCoalesce(ReleaseRegistry.get(technique.getId()))) {
            return 1;
        }
        return copies;
    }

    private static Integer fieldSpawnExtra(List<Technique> techniques, int actionIndex, int copies) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
        if (definition == null || definition.fieldRadius() <= 0f || definition.fieldSpawnCost() <= 0 || copies <= 0) {
            return 0;
        }
        return SkillSegmentMath.exactMultiply(definition.fieldSpawnCost(), copies);
    }

    static int copiesBefore(List<ChainSegment> segments, List<Technique> techniques, int segmentIndex,
                             CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure()) {
            int cachedCopies = planContext.copiesAt(segmentIndex);
            if (cachedCopies >= 0) {
                return Math.max(1, cachedCopies);
            }
        }
        if (segments != null && techniques != null && segmentIndex >= 0 && segmentIndex < segments.size()) {
            MemberCastView memberView = memberViewFor(null, techniques, segments.get(segmentIndex).actionIndex());
            if (memberView != null) {
                return Math.max(1, memberView.copyCount);
            }
        }
        return copiesBefore(segments, techniques, segmentIndex);
    }

    static int copiesBefore(List<ChainSegment> segments, List<Technique> techniques, int segmentIndex) {
        int copies = 1;
        for (int earlier = segmentIndex - 1; earlier >= 0; earlier--) {
            Technique earlierTechnique = techniques.get(segments.get(earlier).actionIndex());
            if (earlierTechnique.getBaseDamage() > 0 && !earlierTechnique.isEnchantment()) {
                break;
            }
            if (SkillSegmentMath.roleOf(earlierTechnique) == ChainRole.RULE_TRIPLE) {
                copies *= 3;
            }
        }
        return copies;
    }

    static float chainEnergyBefore(List<Technique> techniques, int actionIndex) {
        float gathered = 1f;
        for (int earlier = actionIndex - 1; earlier >= 0; earlier--) {
            Technique earlierTechnique = techniques.get(earlier);
            ReleaseDefinition definition = ReleaseRegistry.get(earlierTechnique.getId());
            if (isReleaseBoundary(earlierTechnique, definition)) {
                if (SkillSegmentMath.sharesDualRelease(techniques, earlier, actionIndex)) {
                    continue;
                }
                break;
            }
            if (definition == null) {
                continue;
            }
            float supplied = definition.chainEnergyScale();
            if (!Float.isFinite(supplied) || supplied <= 0f || Math.abs(supplied - 1f) <= 0.001f) {
                continue;
            }
            gathered *= supplied;
        }
        return gathered;
    }

    /** 一次释放的边界。聚能是规则加攻击职责，不是边界；动作宿主也不是。 */
    static boolean isReleaseBoundary(Technique earlierTechnique, ReleaseDefinition definition) {
        return !earlierTechnique.isEnchantment()
                && definition != null
                && definition.kind() == ReleaseKind.HIT_VOLUME
                && definition.role() == ChainRole.ATTACK;
    }

    /**
     * 计划建立时冻结这一名成员的链输入。只有元素法术的命中体积才扫描链倍率。
     * 空链、非法索引、空成员、剑技、剑术、无属性、动作、规则、通用和附魔都是基准倍率。
     * 倍率恰好为 1 时仍是新的一份输入，不收成单例。
     */
    static ReleaseInput frozenChainInput(Skill skill, List<Technique> techniques, int actionIndex,
                                          CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure()) {
            ReleaseInput cachedInput = planContext.frozenInput(actionIndex);
            if (cachedInput != null) {
                return cachedInput;
            }
        }
        MemberCastView memberView = memberViewFor(skill, techniques, actionIndex);
        if (memberView != null) {
            return memberView.chainInput;
        }
        if (skill == null) {
            return ReleaseInput.BASE;
        }
        return frozenChainInput(skill, techniques, actionIndex);
    }

    static ReleaseInput frozenChainInput(Skill skill, List<Technique> techniques, int actionIndex) {
        MemberCastView memberView = memberViewFor(skill, techniques, actionIndex);
        if (memberView != null) {
            return memberView.chainInput;
        }
        if (!consumesElementMagic(skill, techniques, actionIndex)) {
            return ReleaseInput.BASE;
        }
        float gatheredEnergy = chainEnergyBefore(techniques, actionIndex);
        if (!Float.isFinite(gatheredEnergy) || gatheredEnergy <= 0f) {
            return ReleaseInput.BASE;
        }
        return new ReleaseInput(gatheredEnergy);
    }

    /** 目标成员自己要同时满足这些条件。聚能源的类型和属性不能顶上。 */
    static boolean consumesElementMagic(Skill skill, List<Technique> techniques, int actionIndex) {
        if (skill == null || skill.getType() != SkillType.MAGIC) {
            return false;
        }
        if (techniques == null || actionIndex < 0 || actionIndex >= techniques.size()) {
            return false;
        }
        Technique member = techniques.get(actionIndex);
        if (member == null || member.getType() != TechniqueType.MAGIC || member.isEnchantment()) {
            return false;
        }
        ShuAttribute attribute = member.getAttribute();
        if (attribute == null || attribute == ShuAttribute.NONE) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(member.getId());
        return definition != null && definition.kind() == ReleaseKind.HIT_VOLUME;
    }

    /** 几何能量只含定义自身倍率乘链输入。附魔半径留在 CastFrame.volumeScale。 */
    static float geometryEnergy(float ownEnergy, ReleaseInput chainInput) {
        ReleaseInput frozenInput = chainInput == null ? ReleaseInput.BASE : chainInput;
        float combinedEnergy = ownEnergy * frozenInput.sizeMultiplier();
        if (!Float.isFinite(combinedEnergy) || combinedEnergy <= 0f) {
            return 1f;
        }
        return combinedEnergy;
    }

    static boolean suppliesChainEnergy(Technique technique) {
        if (technique == null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return false;
        }
        float suppliedEnergy = definition.chainEnergyScale();
        return Float.isFinite(suppliedEnergy) && suppliedEnergy > 0f && Math.abs(suppliedEnergy - 1f) > 0.001f;
    }

    static Technique closingAttack(List<Technique> techniques, List<ChainSegment> segments, ModifierPlan plan) {
        return closingAttack(techniques, segments, plan, null);
    }

    static Technique closingAttack(List<Technique> techniques, List<ChainSegment> segments, ModifierPlan plan,
                                   CastPlanContext planContext) {
        if (plan.dualRuleSegment >= 0) {
            Technique closing = null;
            for (ChainSegment member : releasedSegments(segments, plan.dualRuleSegment, techniques, planContext)) {
                Technique memberTechnique = techniques.get(member.actionIndex());
                if (memberTechnique.getBaseDamage() > 0f && !memberTechnique.isEnchantment()) {
                    closing = memberTechnique;
                }
            }
            return closing;
        }
        if (plan.attackSegment < 0) {
            return null;
        }
        return techniques.get(segments.get(plan.attackSegment).actionIndex());
    }

    static void imprintEnchant(ServerPlayer player, Skill skill, List<Technique> techniques, int layers) {
        java.util.ArrayList<Integer> indexes = new java.util.ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            if (techniques.get(techniqueIndex).isEnchantment()) {
                indexes.add(techniqueIndex);
            }
        }
        imprintSelection(player, skill, indexes, layers);
    }

    static void imprintSelection(ServerPlayer player, Skill skill, List<Integer> selected, int layers) {
        java.util.ArrayList<Integer> indexes = new java.util.ArrayList<>();
        List<Technique> techniques = skill.getTechniques();
        for (int techniqueIndex : selected) {
            if (techniqueIndex < 0 || techniqueIndex >= techniques.size() || !techniques.get(techniqueIndex).isEnchantment()) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(techniqueIndex).getId());
            if (definition != null && definition.fieldRadius() > 0f) {
                continue;
            }
            indexes.add(techniqueIndex);
        }
        if (indexes.isEmpty()) {
            return;
        }
        float weaponAttack = (float) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        EnchantBonus bonus = EnchantBonus.resolve(skill, indexes, weaponAttack, Math.max(1, layers));
        WeaponEnchant.imprint(player, player.getMainHandItem(), bonus);
    }

    private static boolean castContinues(ServerPlayer player, ServerLevel castLevel, int castEpoch) {
        return player.isAlive() && player.level() == castLevel && ChargeGate.same(player.getUUID(), castEpoch);
    }

    static int findTripleAttack(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        for (int segmentIndex = ruleIndex + 1; segmentIndex < segments.size(); segmentIndex++) {
            ChainSegment seg = segments.get(segmentIndex);
            Technique action = techniques.get(seg.actionIndex());
            if (SkillSegmentMath.releaseKind(action.getId()) == ReleaseKind.RULE) {
                DebugLog.log("rule_triple_cast 先遇到其它规则术式，停止");
                return -1;
            }
            if (action.getBaseDamage() > 0 && !action.isEnchantment()) {
                return segmentIndex;
            }
        }
        return -1;
    }

    static boolean lockThisCast(List<Technique> techniques, int actionIndex, CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure() && planContext.knowsLock(actionIndex)) {
            return planContext.lockAt(actionIndex);
        }
        MemberCastView memberView = memberViewFor(null, techniques, actionIndex);
        if (memberView != null) {
            return memberView.entityLock;
        }
        return lockThisCast(techniques, actionIndex);
    }

    static boolean lockThisCast(List<Technique> techniques, int actionIndex) {
        MemberCastView memberView = memberViewFor(null, techniques, actionIndex);
        if (memberView != null) {
            return memberView.entityLock;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
        if (definition == null || !definition.acceptsInitialAim()) {
            return false;
        }
        for (int earlierIndex = actionIndex - 1; earlierIndex >= 0; earlierIndex--) {
            ReleaseDefinition earlier = ReleaseRegistry.get(techniques.get(earlierIndex).getId());
            if (earlier == null) {
                continue;
            }
            if (earlier.setsTargetForNextDirectional()) {
                return true;
            }
            if (earlier.acceptsTargetOverride() || earlier.acceptsInitialAim()) {
                if (SkillSegmentMath.sharesDualRelease(techniques, earlierIndex, actionIndex)) {
                    continue;
                }
                return false;
            }
        }
        return false;
    }

    static boolean dropThisCast(List<Technique> techniques, List<ChainSegment> segments, int actionIndex,
                                 CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure() && planContext.knowsDrop(actionIndex)) {
            return planContext.dropAt(actionIndex);
        }
        MemberCastView memberView = memberViewFor(null, techniques, actionIndex);
        if (memberView != null) {
            return memberView.drop;
        }
        return dropThisCast(techniques, segments, actionIndex);
    }

    static boolean dropThisCast(List<Technique> techniques, List<ChainSegment> segments, int actionIndex) {
        MemberCastView memberView = memberViewFor(null, techniques, actionIndex);
        if (memberView != null) {
            return memberView.drop;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
        if (definition == null || !definition.acceptsPathOverride()) {
            return false;
        }
        List<Integer> sameRelease = sameReleaseGroup(segments, actionIndex, techniques);
        for (int earlierIndex = actionIndex - 1; earlierIndex >= 0; earlierIndex--) {
            ReleaseDefinition earlier = ReleaseRegistry.get(techniques.get(earlierIndex).getId());
            if (earlier == null) {
                continue;
            }
            if (earlier.setsDropForNextDirectional()) {
                return true;
            }
            if (earlier.acceptsPathOverride()) {
                if (sameRelease != null && sameRelease.contains(earlierIndex)) {
                    continue;
                }
                return false;
            }
        }
        return false;
    }

    static List<Integer> sameReleaseGroup(List<ChainSegment> segments, int actionIndex, List<Technique> techniques,
                                          CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure() && planContext.knowsGroup(actionIndex)) {
            return planContext.sameGroup(actionIndex);
        }
        return sameReleaseGroup(segments, actionIndex, techniques);
    }

    static List<Integer> sameReleaseGroup(List<ChainSegment> segments, int actionIndex, List<Technique> techniques) {
        for (int ruleIndex = 0; ruleIndex < segments.size(); ruleIndex++) {
            ChainSegment ruleSegment = segments.get(ruleIndex);
            if (SkillSegmentMath.releaseKind(techniques.get(ruleSegment.actionIndex()).getId()) != ReleaseKind.RULE) {
                continue;
            }
            List<ChainSegment> released = releasedSegments(segments, ruleIndex, techniques);
            boolean found = false;
            for (ChainSegment seg : released) {
                if (seg.actionIndex() == actionIndex) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                continue;
            }
            List<Integer> group = new ArrayList<>();
            for (ChainSegment seg : released) {
                group.add(seg.actionIndex());
            }
            return group;
        }
        return null;
    }

    static int ruleConsumedCount(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        int count = 1;
        if (ruleIndex + 1 >= segments.size()) {
            return count;
        }
        count += 1;
        ReleaseDefinition rule = ReleaseRegistry.get(techniques.get(segments.get(ruleIndex).actionIndex()).getId());
        int pullAfterA = rule != null ? rule.pullAfterA() : 0;
        count += pulledCount(pullAfterA, segments, ruleIndex);
        return count;
    }

    static int pulledCount(int pullAfterA, List<ChainSegment> segments, int ruleIndex) {
        int available = Math.max(0, segments.size() - (ruleIndex + 2));
        return pullAfterA < 0 ? available : Math.min(pullAfterA, available);
    }

    static int releasedRoom(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        MulticastExpansion expansion = expansionAtRule(segments, ruleIndex, techniques);
        if (expansion != null) {
            return expansion.releases() ? expansion.orderedMembers().size() : 0;
        }
        if (segments == null || ruleIndex < 0 || ruleIndex + 1 >= segments.size()) {
            return 0;
        }
        ReleaseDefinition rule = ReleaseRegistry.get(techniques.get(segments.get(ruleIndex).actionIndex()).getId());
        int pull = rule != null ? rule.pullAfterA() : 0;
        int room = pull < 0 ? 64 : pull + 1;
        return room < 0 ? 0 : room;
    }

    static List<ChainSegment> releasedSegments(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques,
                                               CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure()) {
            List<ChainSegment> cachedMembers = planContext.releasedMembers(ruleIndex);
            if (cachedMembers != null) {
                return cachedMembers;
            }
        }
        return releasedSegments(segments, ruleIndex, techniques);
    }

    static List<ChainSegment> releasedSegments(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        List<ChainSegment> parsedMembers = parsedReleased(segments, techniques, ruleIndex);
        if (parsedMembers != null) {
            return parsedMembers;
        }
        List<ChainSegment> released = new ArrayList<>();
        if (ruleIndex < 0 || ruleIndex + 1 >= segments.size()) {
            return released;
        }
        ReleaseDefinition rule = ReleaseRegistry.get(techniques.get(segments.get(ruleIndex).actionIndex()).getId());
        int pull = rule != null ? rule.pullAfterA() : 0;
        int room = pull < 0 ? 64 : pull + 1;
        for (int followIndex = ruleIndex + 1; followIndex < segments.size() && released.size() < room; followIndex++) {
            ChainSegment candidate = segments.get(followIndex);
            Technique technique = techniques.get(candidate.actionIndex());
            if (SkillSegmentMath.isRemovedRule(technique.getId()) || SkillSegmentMath.isChainModifier(technique)) {
                continue;
            }
            released.add(candidate);
        }
        return released;
    }

    static final class ModifierTemplate {
        final boolean spaceLock;
        final boolean entityLock;
        final boolean drop;
        final Trajectory pathOverride;
        final boolean tracking;
        final boolean scatter;
        final int copies;
        final int dualRuleSegment;
        final int attackSegment;
        final int resume;
        final List<ReleaseInput> memberInputs;

        private ModifierTemplate(ModifierPlan scanned) {
            this.spaceLock = scanned.spaceLock;
            this.entityLock = scanned.entityLock;
            this.drop = scanned.drop;
            this.pathOverride = scanned.pathOverride;
            this.tracking = scanned.tracking;
            this.scatter = scanned.scatter;
            this.copies = scanned.copies;
            this.dualRuleSegment = scanned.dualRuleSegment;
            this.attackSegment = scanned.attackSegment;
            this.resume = scanned.resume;
            this.memberInputs = List.copyOf(scanned.memberInputs);
        }

        int storedUnits() {
            return memberInputs.size();
        }

        ModifierPlan instantiate(boolean realInputs) {
            ModifierPlan plan = new ModifierPlan();
            plan.spaceLock = spaceLock;
            plan.entityLock = entityLock;
            plan.drop = drop;
            plan.pathOverride = pathOverride;
            plan.tracking = tracking;
            plan.scatter = scatter;
            plan.copies = copies;
            plan.volleys = 1;
            plan.dualRuleSegment = dualRuleSegment;
            plan.attackSegment = attackSegment;
            plan.resume = resume;
            if (!realInputs) {
                List<ReleaseInput> baseInputs = new ArrayList<>(memberInputs.size());
                for (int inputIndex = 0; inputIndex < memberInputs.size(); inputIndex++) {
                    baseInputs.add(ReleaseInput.BASE);
                }
                plan.memberInputs = List.copyOf(baseInputs);
            } else {
                plan.memberInputs = List.copyOf(memberInputs);
            }
            return plan;
        }
    }

    static final class ModifierPlan {
        boolean spaceLock;
        boolean entityLock;
        boolean drop;
        Trajectory pathOverride;
        boolean tracking;
        boolean scatter;
        int copies = 1;
        int volleys = 1;
        int dualRuleSegment = -1;
        int attackSegment = -1;
        int resume;
        List<ReleaseInput> memberInputs = List.of();

        void captureChainInputs(Skill skill, List<ChainSegment> segments, List<Technique> techniques) {
            List<ReleaseInput> frozenInputs = new ArrayList<>();
            if (this.dualRuleSegment >= 0) {
                for (ChainSegment member : releasedSegments(segments, this.dualRuleSegment, techniques)) {
                    frozenInputs.add(frozenChainInput(skill, techniques, member.actionIndex()));
                }
            } else if (this.attackSegment >= 0 && this.attackSegment < segments.size()) {
                frozenInputs.add(frozenChainInput(skill, techniques, segments.get(this.attackSegment).actionIndex()));
            }
            this.memberInputs = List.copyOf(frozenInputs);
        }

        ReleaseInput attackInput() {
            if (this.memberInputs.isEmpty()) {
                return ReleaseInput.BASE;
            }
            return this.memberInputs.get(0);
        }

        ReleaseInput dualInput(int releasedOrdinal) {
            if (releasedOrdinal < 0 || releasedOrdinal >= this.memberInputs.size()) {
                return ReleaseInput.BASE;
            }
            return this.memberInputs.get(releasedOrdinal);
        }
    }

    static final int DUAL_STEP_TICKS = 5;

    static boolean opensMulticast(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        return expansionAtRule(segments, ruleIndex, techniques) != null;
    }

    private static int sequenceGap(MulticastExpansion expansion, int ordinal) {
        if (expansion != null) {
            return expansion.gapTicks(ordinal);
        }
        return Math.max(0, ordinal) * DUAL_STEP_TICKS;
    }

    private static int memberCopyCount(List<ChainSegment> segments, List<Technique> techniques, ChainSegment member,
                                        CastPlanContext planContext) {
        if (member == null) {
            return 1;
        }
        int segmentIndex = segmentIndexOfHost(segments, member.actionIndex());
        if (segmentIndex < 0) {
            segmentIndex = segments == null ? -1 : segments.indexOf(member);
        }
        return Math.max(1, copiesBefore(segments, techniques, segmentIndex, planContext));
    }

    private static ReleaseInput memberChainInput(Skill skill, List<Technique> techniques, int hostIndex,
                                                  ReleaseInput captured, CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure()) {
            ReleaseInput cachedInput = planContext.frozenInput(hostIndex);
            if (cachedInput != null) {
                return cachedInput;
            }
        }
        MemberCastView memberView = memberViewFor(skill, techniques, hostIndex);
        if (memberView != null) {
            return memberView.chainInput;
        }
        return captured == null ? ReleaseInput.BASE : captured;
    }

    private static int segmentIndexOfHost(List<ChainSegment> segments, int hostIndex) {
        if (segments == null) {
            return -1;
        }
        for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
            if (segments.get(segmentIndex).actionIndex() == hostIndex) {
                return segmentIndex;
            }
        }
        return -1;
    }

    private static List<ChainSegment> parsedReleased(List<ChainSegment> segments, List<Technique> techniques, int ruleIndex) {
        MulticastExpansion expansion = expansionAtRule(segments, ruleIndex, techniques);
        if (expansion == null) {
            return null;
        }
        if (!expansion.releases()) {
            return List.of();
        }
        List<ChainSegment> released = new ArrayList<>();
        for (MulticastMember member : expansion.orderedMembers()) {
            released.add(new ChainSegment(member.hostIndex(), member.attachedEnchantIndexes()));
        }
        return released;
    }

    private static MulticastExpansion expansionAtRule(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        if (segments == null || techniques == null || ruleIndex < 0 || ruleIndex >= segments.size()) {
            return null;
        }
        int hostIndex = segments.get(ruleIndex).actionIndex();
        if (hostIndex < 0 || hostIndex >= techniques.size()) {
            return null;
        }
        return enclosingExpansion(techniques, hostIndex);
    }

    /** 规则格若落在某次展开里，用那一次的成员，不再把内层多重单独解析成另一条队列。 */
    private static MulticastExpansion enclosingExpansion(List<Technique> techniques, int hostIndex) {
        int cursor = 0;
        while (cursor < techniques.size()) {
            MulticastExpansion expansion = ChainSteps.expansionFrom(techniques, cursor);
            if (expansion == null || !expansion.applicable()) {
                cursor++;
                continue;
            }
            if (expansion.startIndex() == hostIndex) {
                return expansion;
            }
            if (expansion.consumedIndexes().contains(hostIndex)) {
                ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(hostIndex).getId());
                if (definition != null && definition.multicastSpec() != null) {
                    return expansion;
                }
            }
            int nextStart = expansion.nextIndex();
            cursor = nextStart > cursor ? nextStart : cursor + 1;
        }
        return null;
    }

    private static MemberCastView memberViewFor(Skill skill, List<Technique> techniques, int hostIndex) {
        if (techniques == null || hostIndex < 0 || hostIndex >= techniques.size()) {
            return null;
        }
        int cursor = 0;
        while (cursor < techniques.size()) {
            MulticastExpansion expansion = ChainSteps.expansionFrom(techniques, cursor);
            if (expansion == null || !expansion.applicable()) {
                cursor++;
                continue;
            }
            for (MulticastMember member : expansion.orderedMembers()) {
                if (member.hostIndex() == hostIndex) {
                    return MemberCastView.from(skill, techniques, member);
                }
            }
            int nextStart = expansion.nextIndex();
            cursor = nextStart > cursor ? nextStart : cursor + 1;
        }
        return null;
    }
}
