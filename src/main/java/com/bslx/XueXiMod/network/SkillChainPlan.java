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
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSpawnInheritance;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.WeaponEnchant;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.status.wood.vine.VineMarkActions;
import com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupCommit;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.shu.technique.release.multicast.ReplicationSpec;
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

/**
 * 服务端重编后的链计划。它只使用已校验的技能、分段和服务端姿态，不信任客户端传来的费用、份数或目标。
 * 报价放不进 int 时返回空，由调用方拒绝；开火方法负责排队和份额，施法中断时静默取消未执行部分。
 * 实际扣费、宿主提交和效果执行见被调用方。
 */
final class SkillChainPlan {
    /** 从链首估算一次按键的预提交费用。费用语义见四参数入口。 */
    static Integer pressCommitCost(List<Technique> techniques, List<ChainSegment> segments) {
        return pressCommitCost(techniques, segments, 0, null);
    }

    /** 使用已有计划上下文，从链首估算预提交费用。 */
    static Integer pressCommitCost(List<Technique> techniques, List<ChainSegment> segments, CastPlanContext planContext) {
        return pressCommitCost(techniques, segments, 0, planContext);
    }

    /** 从指定分段开始估算剩余预提交费用，不读取计划缓存。 */
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
            if (SkillSegmentMath.isOperationHost(role)) {
                Integer operationQuote = SkillSegmentMath.operationQuote(techniques, segment);
                cost = quoteAdd(cost, operationQuote);
                if (cost == null) {
                    return null;
                }
                segmentIndex += 1;
                continue;
            }
            if (SkillSegmentMath.commitsSelfState(action) && role != ChainRole.RULE_COOPERATIVE) {
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

    /**
     * 合计指定区间里被这次按键吃掉的链上修饰。各自只加一次标准报价，不乘后面的份数；
     * 正伤害宿主不重复计入，溢出返回空。
     */
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

    /** 把一段单价先乘份数再乘波次。任一步放不进 int 或出现负数时返回空。 */
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

    /** 合并两段报价。任一为空就保持空，避免溢出后继续计算。 */
    private static Integer quoteAdd(Integer total, Integer part) {
        if (total == null || part == null) {
            return null;
        }
        return SkillSegmentMath.exactAdd(total, part);
    }

    /** 从指定分段向后收集一份新的修饰计划，并冻结其中的链输入。 */
    static SkillChainPlan.ModifierPlan collectModifiers(Skill skill, List<ChainSegment> segments, List<Technique> techniques, int from) {
        return collectModifiers(skill, segments, techniques, from, null);
    }

    /** 优先复用计划上下文里的结构；没有缓存时现场扫描并冻结链输入。不改变原链。 */
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

    /** 扫描一次修饰结构并保存为可重复实例化的模板，同时冻结当时的链输入。 */
    static ModifierTemplate captureModifierTemplate(List<ChainSegment> segments, List<Technique> techniques, int from, Skill skill) {
        ModifierPlan scanned = scanModifiers(segments, techniques, from);
        scanned.captureChainInputs(skill, segments, techniques);
        return new ModifierTemplate(scanned);
    }

    /**
     * 向后扫描锁定、弹道、复制和双重规则，直到遇到攻击或非修饰。
     * 双重规则会把恢复位置延到它释放出的最后一名成员之后。
     */
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
            if (SkillSegmentMath.isOperationHost(role)) {
                break;
            }
            if (!SkillSegmentMath.isChainModifier(technique)) {
                break;
            }
            switch (role) {
                case SEEK_SPACE -> plan.spaceLock = true;
                case SEEK_ENTITY -> plan.entityLock = true;
                case TRAJECTORY_DROP, TRAJECTORY_PARALLEL -> {
                    ReleaseDefinition pathDefinition = ReleaseRegistry.get(technique.getId());
                    plan.pathOverride = pathDefinition != null ? pathDefinition.trajectory() : null;
                    plan.drop = plan.pathOverride == Trajectory.DROP;
                }
                case TRAJECTORY_TRACKING -> plan.tracking = true;
                case TRAJECTORY_SCATTER -> plan.scatter = true;
                case RULE_TRIPLE -> plan.copies = replicatedCopies(plan.copies, technique);
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

    /** 按修饰计划开火，不使用计划缓存。费用份额和完成时刻见完整入口。 */
    static int firePlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                          SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, ManaPaymentReceipt receipt,
                          CastSource castSource) {
        return firePlan(player, skill, techniques, segments, plan, weaponBonus, startTick, receipt, castSource, null);
    }

    /**
     * 把一份修饰计划排进调度。双重规则转交双重队列；普通攻击先按份数和波次预留份额。
     * 单价放不进 int 时返回失败。实际执行、提交和退款见被调用方。
     */
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

    /** 使用调用方已经预留好的份额开火，不读取计划缓存。 */
    static int firePaidPlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                             SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, UncommittedShare.Pool shares,
                             CastSource castSource) {
        return firePaidPlan(player, skill, techniques, segments, plan, weaponBonus, startTick, shares, castSource, null);
    }

    /**
     * 为每波、每份排一次攻击。宿主组先凑齐该波份额再整组提交，普通攻击逐份提交。
     * 份额已关闭时不新增排队；施法中断、持剑失效或份额不足时取消未用份额并静默停在已排时刻。
     */
    static int firePaidPlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                             SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, UncommittedShare.Pool shares,
                             CastSource castSource, CastPlanContext planContext) {
        return firePaidPlan(player, skill, techniques, segments, plan, weaponBonus, startTick, shares,
                castSource, planContext, null);
    }

    static int firePaidPlan(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                             SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, UncommittedShare.Pool shares,
                             CastSource castSource, CastPlanContext planContext, VineMarkActions.Action frozenSword) {
        ChainSegment attackSeg = segments.get(plan.attackSegment);
        Technique attack = techniques.get(attackSeg.actionIndex());
        EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, attackSeg);
        EnchantBonus bonus = DomainSpawnInheritance.finish(player, attack, skill, attackSeg.enchantIndexes(),
                EnchantBonus.resolve(skill, attackSeg.enchantIndexes(), attack.getBaseDamage(), 1));
        float damage = SkillSegmentMath.configuredSegmentDamage(attack, attackSeg, weaponBonus,
                SkillSegmentMath.segmentMagicDamage(skill, attackSeg), null, bonus);
        boolean carries = !attackSeg.enchantIndexes().isEmpty()
                || (bonus.resourceLoad != null && !bonus.resourceLoad.empty());
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
            boolean vineSpectrum = frozenSword == null ? VineMarkActions.swordSpectrum() : frozenSword.spectrumEligible();
            VineMarkActions.Action vineAction = frozenSword != null ? frozenSword
                    : VineMarkActions.swordArmed(player)
                    ? new VineMarkActions.Action(player.getUUID(), hostCast.hostId(0, 0),
                    VineMarkActions.Source.SWORD, vineSpectrum)
                    : null;
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
                    UUID vineActionId = vineAction == null ? hostGroup : vineAction.actionId();
                    if (vineAction != null) {
                        VineMarkRuntime.rememberAction(vineActionId, player, vineSpectrum);
                        VineMarkRuntime.stampTally(tally, vineActionId, vineSpectrum);
                    }
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
                            VineMarkActions.Action previousAction = VineMarkActions.current();
                            if (vineAction != null) {
                                VineMarkActions.open(new VineMarkActions.Action(
                                        vineAction.casterId(), vineActionId, vineAction.source(), vineSpectrum));
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
                                VineMarkActions.open(previousAction);
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
            VineMarkActions.Action paidSword = frozenSword != null ? frozenSword
                    : VineMarkActions.frozenSword(player, tick, volley);
            boolean vineSpectrum = paidSword != null && paidSword.spectrumEligible();
            UUID vineActionId = paidSword == null ? null : paidSword.actionId();
            if (vineActionId != null) {
                VineMarkRuntime.rememberAction(vineActionId, player, vineSpectrum);
                VineMarkRuntime.stampTally(tally, vineActionId, vineSpectrum);
            }
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
                    VineMarkActions.Action previousAction = VineMarkActions.current();
                    if (vineActionId != null) {
                        VineMarkActions.open(new VineMarkActions.Action(
                                player.getUUID(), vineActionId, VineMarkActions.Source.SWORD, vineSpectrum));
                    }
                    try {
                        if (!castContinues(player, castLevel, castEpoch) || !HeldSwordGate.allows(attack, player.getMainHandItem())) {
                            claim.cancel(player);
                            return;
                        }
                        runner.run();
                        claim.commit();
                    } finally {
                        VineMarkActions.open(previousAction);
                    }
                });
            }
        }
        return finishedTick;
    }

    /** 按双重规则释放成员队列，不使用计划缓存或领域开口。 */
    static int fireDualSequence(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                  SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, CastSource castSource) {
        return fireDualSequence(player, skill, techniques, segments, plan, weaponBonus, startTick, castSource, null);
    }

    /** 按双重规则释放成员队列，并可复用计划上下文。 */
    static int fireDualSequence(ServerPlayer player, Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                  SkillChainPlan.ModifierPlan plan, float weaponBonus, int startTick, CastSource castSource,
                                  CastPlanContext planContext) {
        return fireDualSequence(player, skill, techniques, segments, plan, weaponBonus, startTick, castSource, planContext, null);
    }

    /**
     * 依次安排双重规则放出的领域、屏障、护环、附魔和攻击。
     * 第一名成员当前放不下时返回失败；后续成员放不下时跳过。延迟成员在施法中断后静默返回。
     */
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
        Integer dualRoseShare = null;
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
                    if (loose.stream().anyMatch(index -> index >= 0 && index < techniques.size()
                            && "vine_mark".equals(techniques.get(index).getId()))) {
                        com.bslx.XueXiMod.player.ManaPaymentReceipt receipt =
                                com.bslx.XueXiMod.player.MagicPool.lastReceipt(player.getUUID());
                        if (dualRoseShare == null) {
                            dualRoseShare = dualSequenceCost(techniques, segments, plan, planContext);
                        }
                        if (receipt != null && dualRoseShare != null && dualRoseShare > 0) {
                            com.bslx.XueXiMod.status.wood.vine.VineMarkSpectrum.armPending(player,
                                    receipt == null ? null : receipt.receiptId());
                            receipt.retainShare(dualRoseShare);
                            VineMarkRuntime.afterShareCommitted(receipt);
                        }
                    }
                }
                lastTick = startTick + sequenceGap(sequenceExpansion, attackOrdinal);
                attackOrdinal++;
                continue;
            }
            int memberCopies = memberCopyCount(segments, techniques, member, planContext);
            EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
            EnchantBonus bonus = DomainSpawnInheritance.finish(player, memberTechnique, skill, member.enchantIndexes(),
                    EnchantBonus.resolve(skill, member.enchantIndexes(), memberTechnique.getBaseDamage(), 1));
            float damage = SkillSegmentMath.configuredSegmentDamage(memberTechnique, member, weaponBonus,
                    SkillSegmentMath.segmentMagicDamage(skill, member), null, bonus);
            ReleaseDefinition definition = ReleaseRegistry.get(memberTechnique.getId());
            EnchantmentSnapshot fieldSnapshot = memberTechnique.getBaseDamage() <= 0f
                    && definition != null && definition.fieldRadius() > 0f
                    ? ContactCast.snapshot(skill, member, player) : null;
            boolean carries = fieldSnapshot == null && memberTechnique.getBaseDamage() > 0f
                    && (!member.enchantIndexes().isEmpty() || (bonus.resourceLoad != null && !bonus.resourceLoad.empty()));
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
            VineMarkActions.Action dualSword = VineMarkActions.frozenSword(player, sequenceTick, attackOrdinal);
            boolean vineSpectrum = dualSword != null && dualSword.spectrumEligible();
            UUID vineActionId = dualSword == null ? null : dualSword.actionId();
            if (vineActionId != null) {
                VineMarkRuntime.rememberAction(vineActionId, player, vineSpectrum);
                VineMarkRuntime.stampTally(tally, vineActionId, vineSpectrum);
            }
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
                    VineMarkActions.Action previousAction = VineMarkActions.current();
                    if (vineActionId != null) {
                        VineMarkActions.open(new VineMarkActions.Action(
                                player.getUUID(), vineActionId, VineMarkActions.Source.SWORD, vineSpectrum));
                    }
                    try {
                    runner.run();
                    if (fieldSnapshot != null && copyIndex == 0) {
                        com.bslx.XueXiMod.shu.technique.release.field.FieldZones.bindBorn(
                                player.getUUID(), fieldSnapshot, player.getServer().getTickCount());
                    }
                    } finally {
                        VineMarkActions.open(previousAction);
                    }
                });
            }
            attackOrdinal++;
        }
        return lastTick;
    }

    /** 估算双重队列的费用，不读取计划缓存。 */
    static Integer dualSequenceCost(List<Technique> techniques, List<ChainSegment> segments, SkillChainPlan.ModifierPlan plan) {
        return dualSequenceCost(techniques, segments, plan, null);
    }

    /**
     * 估算双重队列费用。领域、屏障、护环、会合并份数或按单位计价的成员只算结算价；
     * 其余成员乘自身份数。场地另加生成费用，溢出返回空。
     */
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

    /** 条件效果声明合并份数时只按一份计价，否则保留传入份数。 */
    private static int pricedCopies(Technique technique, int copies) {
        if (technique != null && technique.getBaseDamage() <= 0f
                && ConditionalEffects.copiesCoalesce(ReleaseRegistry.get(technique.getId()))) {
            return 1;
        }
        return copies;
    }

    /** 计算场地的额外生成费用。没有正费用或份数时返回 0，乘法溢出返回空。 */
    private static Integer fieldSpawnExtra(List<Technique> techniques, int actionIndex, int copies) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(actionIndex).getId());
        if (definition == null || definition.fieldRadius() <= 0f || definition.fieldSpawnCost() <= 0 || copies <= 0) {
            return 0;
        }
        return SkillSegmentMath.exactMultiply(definition.fieldSpawnCost(), copies);
    }

    /** 取该分段生效的复制份数，优先用计划缓存或多重成员视图。结果至少为 1。 */
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

    /** 向前连乘尚未越过攻击边界的复制规则。没有复制时返回 1。 */
    static int copiesBefore(List<ChainSegment> segments, List<Technique> techniques, int segmentIndex) {
        int copies = 1;
        for (int earlier = segmentIndex - 1; earlier >= 0; earlier--) {
            Technique earlierTechnique = techniques.get(segments.get(earlier).actionIndex());
            if (SkillSegmentMath.isOperationHost(earlierTechnique)
                    || earlierTechnique.getBaseDamage() > 0 && !earlierTechnique.isEnchantment()) {
                break;
            }
            if (SkillSegmentMath.roleOf(earlierTechnique) == ChainRole.RULE_TRIPLE) {
                copies = replicatedCopies(copies, earlierTechnique);
            }
        }
        return copies;
    }

    /**
     * 已声明复制时按因子连乘。定义或声明缺失则这一格不复制，避免把未声明的三重角色写成固定 3。
     * 已声明的一分为三仍是因子 3。溢出仍按 int 回绕，与原来的乘 3 相同。
     */
    private static int replicatedCopies(int copies, Technique technique) {
        ReleaseDefinition definition = technique == null ? null : ReleaseRegistry.get(technique.getId());
        ReplicationSpec replication = definition == null ? null : definition.replication();
        if (replication == null) {
            return copies;
        }
        return copies * replication.factor();
    }

    /** 从宿主向前累计链能量倍率，跨过一次释放边界就停止。没有有效倍率时保持 1。 */
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

    /** 取这份计划最后一名正伤害攻击，不读取计划缓存。 */
    static Technique closingAttack(List<Technique> techniques, List<ChainSegment> segments, ModifierPlan plan) {
        return closingAttack(techniques, segments, plan, null);
    }

    /** 双重队列取最后一名正伤害成员；普通计划取唯一攻击。没有攻击时返回空。 */
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

    /** 选出技能中的全部附魔并按层数印到主手。场地附魔的排除见被调用方。 */
    static void imprintEnchant(ServerPlayer player, Skill skill, List<Technique> techniques, int layers) {
        java.util.ArrayList<Integer> indexes = new java.util.ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            if (techniques.get(techniqueIndex).isEnchantment()) {
                indexes.add(techniqueIndex);
            }
        }
        imprintSelection(player, skill, indexes, layers);
    }

    /** 过滤出可印记的附魔并写入主手。下标越界、非附魔或场地附魔被跳过；结果为空时不改武器。 */
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

    /** 延迟开火前确认施法者仍存活、仍在原维度且蓄力代次未变。 */
    private static boolean castContinues(ServerPlayer player, ServerLevel castLevel, int castEpoch) {
        return player.isAlive() && player.level() == castLevel && ChargeGate.same(player.getUUID(), castEpoch);
    }

    /** 从复制规则向后找第一段正伤害攻击。先遇到其他规则时停止并返回失败。 */
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

    /** 判断这次攻击是否接受前方锁定。可复用缓存或多重成员视图时直接取那里的结果。 */
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

    /**
     * 向前寻找会给下一次定向释放设目标的术式。宿主不接受初始瞄准时直接否决；
     * 遇到另一名会消费目标或瞄准的宿主即停止，同一次多重成员除外。
     */
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

    /** 判断这次攻击是否改走垂落。可复用缓存或多重成员视图时直接取那里的结果。 */
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

    /**
     * 向前寻找会把下一次定向释放改为垂落的术式。宿主不接受路径覆盖时直接否决；
     * 同一次释放中的其他可覆盖宿主不截断搜索。
     */
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

    /** 查找与该宿主属于同一规则释放的成员下标，可先复用计划缓存。 */
    static List<Integer> sameReleaseGroup(List<ChainSegment> segments, int actionIndex, List<Technique> techniques,
                                          CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure() && planContext.knowsGroup(actionIndex)) {
            return planContext.sameGroup(actionIndex);
        }
        return sameReleaseGroup(segments, actionIndex, techniques);
    }

    /** 在规则分段中查找包含该宿主的释放组。找不到时返回空。 */
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

    /** 计算规则自身、紧随一段以及声明继续拉取的分段数量。 */
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

    /** 计算规则声明还能向后拉取多少段。负值表示拉到链尾，否则不超过剩余段数。 */
    static int pulledCount(int pullAfterA, List<ChainSegment> segments, int ruleIndex) {
        int available = Math.max(0, segments.size() - (ruleIndex + 2));
        return pullAfterA < 0 ? available : Math.min(pullAfterA, available);
    }

    /** 计算规则可放出的成员容量。多重展开优先；没有可放成员时返回 0。 */
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

    /** 取规则实际放出的成员，结构可复用时直接返回缓存。 */
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

    /** 取规则放出的成员。多重展开优先；否则跳过修饰并按拉取容量收集宿主。 */
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

        /** 模板保存的冻结链输入数量。 */
        int storedUnits() {
            return memberInputs.size();
        }

        /** 复制成新的修饰计划。不要求真实输入时，全部链输入改回基准值。 */
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

        /** 为双重成员或唯一攻击冻结链输入。没有攻击时清空输入。 */
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

        /** 取普通攻击保存的第一份链输入。没有输入时返回基准值。 */
        ReleaseInput attackInput() {
            if (this.memberInputs.isEmpty()) {
                return ReleaseInput.BASE;
            }
            return this.memberInputs.get(0);
        }

        /** 按双重队列序号取链输入。越界时返回基准值。 */
        ReleaseInput dualInput(int releasedOrdinal) {
            if (releasedOrdinal < 0 || releasedOrdinal >= this.memberInputs.size()) {
                return ReleaseInput.BASE;
            }
            return this.memberInputs.get(releasedOrdinal);
        }
    }

    static final int DUAL_STEP_TICKS = 5;

    /** 判断该规则分段是否位于一次多重展开中。 */
    static boolean opensMulticast(List<ChainSegment> segments, int ruleIndex, List<Technique> techniques) {
        return expansionAtRule(segments, ruleIndex, techniques) != null;
    }

    /** 取双重队列第几名成员的延迟。没有多重展开时按固定波次间隔计算。 */
    private static int sequenceGap(MulticastExpansion expansion, int ordinal) {
        if (expansion != null) {
            return expansion.gapTicks(ordinal);
        }
        return Math.max(0, ordinal) * DUAL_STEP_TICKS;
    }

    /** 取双重成员的复制份数。成员为空时返回 1。 */
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

    /** 选择双重成员的链输入，依次使用计划缓存、多重视图和调用方捕获值。 */
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

    /** 按宿主原始下标查找分段位置。找不到时返回失败。 */
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

    /** 把多重展开的有序成员转成分段。该规则不在展开中时返回空，展开不释放时返回空列表。 */
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

    /** 定位规则分段所属的多重展开。下标越界时返回空。 */
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

    /** 在整条链的多重展开里查找宿主对应的成员视图。没有匹配时返回空。 */
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
