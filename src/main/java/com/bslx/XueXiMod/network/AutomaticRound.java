package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundMember;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleases;
import com.bslx.XueXiMod.shu.technique.release.bind.LifetimeToken;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.bind.RoundHandle;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSpawnInheritance;
import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 领域自动轮。松散成功按计划复制份保留，宿主组只认原子回执。
 * 多重成员按同一份编译的 {@code gapTicks} 排队，非多重仍按序号乘 {@link CastExecutionSession#STEP_GAP}。
 * 复制份在同一刻发布。不占用按键冷却，不推进链进度，不读玩家准星或锁定标记。
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
        CompiledReleaseStep compiled = CompiledReleaseStep.compile(frozenSkill, techniques, segments, 0.0);
        PricedQuote quote = priceQuote(plan, techniques, segments, compiled);
        if (quote == null) {
            return new ClosedRound(context.roundId());
        }
        GroupBudget budget = GroupBudget.reserve(owner, quote.totalPrice, false);
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
        OpenRound round = new OpenRound(context.roundId(), owner, budget, hostCast, quote, plan.members().size());
        ReleaseBatch batch = ReleaseBatch.open(owner, frozenSkill);
        ReleaseBatch.unbind();
        try {
            int memberCount = plan.members().size();
            for (int memberOrdinal = 0; memberOrdinal < memberCount; memberOrdinal++) {
                if (round.blocksLaunch()) {
                    for (int skippedOrdinal = memberOrdinal; skippedOrdinal < memberCount; skippedOrdinal++) {
                        round.closeMember(skippedOrdinal);
                    }
                    break;
                }
                BoundMember scheduledMember = plan.members().get(memberOrdinal);
                int launchTick = nowTick + launchGap(compiled, memberOrdinal, scheduledMember.memberOrdinal());
                int scheduledOrdinal = memberOrdinal;
                ChainScheduler.schedule(server, launchTick, () -> launchMemberGuarded(round, plan, context, token,
                        frozenSkill, techniques, segments, batch, hostCast, compiled, scheduledOrdinal));
            }
            return round;
        } catch (RuntimeException failure) {
            round.haltRemainder();
            throw failure;
        }
    }

    private static PricedQuote priceQuote(BoundReleasePlan plan, List<Technique> techniques,
                                          List<ChainSegment> segments, CompiledReleaseStep compiled) {
        int memberCount = plan.members().size();
        if (memberCount <= 0) {
            return null;
        }
        int[] memberPools = new int[memberCount];
        int ruleShare = 0;
        if (compiled != null && compiled.chargeRefusal != null) {
            return null;
        }
        if (compiled != null && compiled.multicast()) {
            return multicastQuote(compiled, memberCount);
        } else if (plan.dual()) {
            if (compiled == null) {
                return null;
            }
            for (int memberIndex = 0; memberIndex < memberCount && memberIndex < compiled.memberShares.size(); memberIndex++) {
                memberPools[memberIndex] = Math.max(0, compiled.memberShares.get(memberIndex));
            }
            ruleShare = Math.max(0, compiled.ruleShare);
        } else {
            Integer pressedQuote = SkillChainPlan.pressCommitCost(techniques, segments);
            if (pressedQuote == null) {
                return null;
            }
            memberPools[0] = pressedQuote;
        }
        int totalPrice = ruleShare;
        for (int poolPrice : memberPools) {
            Integer nextTotal = SkillSegmentMath.exactAdd(totalPrice, poolPrice);
            if (nextTotal == null) {
                return null;
            }
            totalPrice = nextTotal;
        }
        return new PricedQuote(memberPools, ruleShare, totalPrice);
    }

    /** 新多重的份额相加或复制数放不进 int 时整轮拒绝，不加爆成负数后再被当成免费。 */
    private static PricedQuote multicastQuote(CompiledReleaseStep compiled, int memberCount) {
        if (compiled.memberCount() != memberCount || compiled.memberShares.size() != memberCount || compiled.ruleShare < 0) {
            return null;
        }
        int[] memberPools = new int[memberCount];
        long summed = compiled.ruleShare;
        for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
            MemberCastView view = compiled.memberView(memberIndex);
            int share = compiled.memberShares.get(memberIndex);
            if (view == null || view.copyCount < 1 || view.copyCount == Integer.MAX_VALUE || share < 0) {
                return null;
            }
            if (summed > Integer.MAX_VALUE - share) {
                return null;
            }
            memberPools[memberIndex] = share;
            summed += share;
        }
        return new PricedQuote(memberPools, compiled.ruleShare, (int) summed);
    }

    private static int launchGap(CompiledReleaseStep compiled, int queueIndex, int memberOrdinal) {
        if (compiled != null && compiled.multicast()) {
            return Math.max(0, compiled.gapTicks(memberOrdinal));
        }
        return queueIndex * CastExecutionSession.STEP_GAP;
    }

    private static void launchMemberGuarded(OpenRound round, BoundReleasePlan plan, ReleaseExecutionContext context,
                                             LifetimeToken token, Skill frozenSkill, List<Technique> techniques,
                                             List<ChainSegment> segments, ReleaseBatch batch, HostCast hostCast,
                                             CompiledReleaseStep compiled, int memberIndex) {
        try {
            launchMember(round, plan, context, token, frozenSkill, techniques, segments, batch, hostCast, compiled,
                    memberIndex);
        } catch (Throwable failure) {
            round.haltRemainder();
            round.closeMember(memberIndex);
        }
    }

    private static void launchMember(OpenRound round, BoundReleasePlan plan, ReleaseExecutionContext context,
                                     LifetimeToken token, Skill frozenSkill, List<Technique> techniques,
                                     List<ChainSegment> segments, ReleaseBatch batch, HostCast hostCast,
                                     CompiledReleaseStep compiled, int memberIndex) {
        if (round.blocksLaunch()) {
            round.closeMember(memberIndex);
            return;
        }
        ServerPlayer owner = context.owner();
        MinecraftServer server = owner == null ? null : owner.getServer();
        int nowTick = server == null ? 0 : server.getTickCount();
        if (server == null || !token.valid(nowTick)) {
            round.cancel();
            round.closeMember(memberIndex);
            return;
        }
        BoundMember member = memberIndex >= 0 && memberIndex < plan.members().size() ? plan.members().get(memberIndex) : null;
        ReleaseDefinition definition = member == null || member.technique() == null
                ? null
                : ReleaseRegistry.get(member.technique().getId());
        int chargeTicks = definition == null ? 0 : definition.chargeTicks();
        if (chargeTicks > 0 && compiled != null && compiled.multicast()) {
            round.haltRemainder();
            round.closeMember(memberIndex);
            return;
        }
        if (chargeTicks > 0) {
            ChainScheduler.schedule(server, nowTick + chargeTicks, () -> {
                try {
                    if (round.blocksLaunch()) {
                        round.closeMember(memberIndex);
                        return;
                    }
                    int firedTick = server.getTickCount();
                    if (!token.valid(firedTick)) {
                        round.cancel();
                        round.closeMember(memberIndex);
                        return;
                    }
                    WaterMomentum.Hold momentumHold = WaterMomentum.reserve(owner, definition.momentumSpend());
                    SkillSegmentRunner.openChargeBypass();
                    try {
                        WaterMomentum.commit(owner, momentumHold);
                        finishMember(round, plan, context, token, frozenSkill, techniques, segments, batch, hostCast,
                                compiled, memberIndex);
                    } finally {
                        SkillSegmentRunner.closeChargeBypass();
                    }
                } catch (Throwable failure) {
                    round.haltRemainder();
                    round.closeMember(memberIndex);
                }
            });
            return;
        }
        finishMember(round, plan, context, token, frozenSkill, techniques, segments, batch, hostCast, compiled,
                memberIndex);
    }

    private static void finishMember(OpenRound round, BoundReleasePlan plan, ReleaseExecutionContext context,
                                     LifetimeToken token, Skill frozenSkill, List<Technique> techniques,
                                     List<ChainSegment> segments, ReleaseBatch batch, HostCast hostCast,
                                     CompiledReleaseStep compiled, int memberIndex) {
        try {
            spawnMember(round, plan, context, token, frozenSkill, techniques, segments, batch, hostCast, compiled,
                    memberIndex);
        } catch (Throwable failure) {
            round.haltRemainder();
        }
        round.closeMember(memberIndex);
    }

    private static void spawnMember(OpenRound round, BoundReleasePlan plan, ReleaseExecutionContext context,
                                    LifetimeToken token, Skill frozenSkill, List<Technique> techniques,
                                    List<ChainSegment> segments, ReleaseBatch batch, HostCast hostCast,
                                    CompiledReleaseStep compiled, int memberIndex) {
        if (round.blocksLaunch()) {
            return;
        }
        ServerPlayer owner = context.owner();
        int nowTick = owner == null || owner.getServer() == null ? 0 : owner.getServer().getTickCount();
        if (owner == null || !token.valid(nowTick)) {
            round.cancel();
            return;
        }
        if (memberIndex < 0 || memberIndex >= plan.members().size()) {
            return;
        }
        BoundMember member = plan.members().get(memberIndex);
        Technique technique = member.technique();
        if (technique == null || !HeldSwordGate.allows(technique, owner.getMainHandItem())) {
            return;
        }
        Skill castSkill = frozenSkill;
        List<Technique> castTechniques = techniques;
        int hostLocal = member.localIndex();
        List<Integer> enchantLocals = member.enchantLocalIndexes();
        int plannedCopies;
        boolean lockCast;
        boolean dropCast;
        ReleaseInput chainInput;
        if (compiled != null && compiled.multicast()) {
            MemberCastSlice slice = memberSlice(frozenSkill, techniques, member, compiled.memberView(memberIndex));
            if (slice == null) {
                throw new IllegalStateException("多重成员视图和冻结宿主对不上");
            }
            castSkill = slice.skill;
            castTechniques = slice.techniques;
            hostLocal = slice.hostLocal;
            enchantLocals = slice.enchantLocals;
            plannedCopies = slice.plannedCopies;
            lockCast = slice.entityLock;
            dropCast = slice.dropCast;
            chainInput = slice.chainInput;
        } else {
            int segmentIndex = segmentIndexOf(segments, member.localIndex());
            plannedCopies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, segmentIndex));
            lockCast = SkillChainPlan.lockThisCast(techniques, member.localIndex());
            dropCast = SkillChainPlan.dropThisCast(techniques, segments, member.localIndex());
            chainInput = member.chainInput();
        }
        ChainSegment segment = new ChainSegment(hostLocal, enchantLocals);
        EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(castSkill, segment);
        EnchantBonus bonus = EnchantBonus.resolve(castSkill, enchantLocals, technique.getBaseDamage(), 1);
        bonus = DomainSpawnInheritance.apply(context, castSkill, enchantLocals, bonus);
        DomainInstance carriedDomain = DomainService.find(context.domainInstanceId());
        bonus = DomainSpawnInheritance.stackBonus(bonus, technique.getAttribute(),
                DomainSpawnInheritance.matchedByOwn(technique.getAttribute(), castSkill, enchantLocals),
                carriedDomain == null ? null : carriedDomain.carriedEnchantments());
        float damage = SkillSegmentMath.configuredSegmentDamage(technique, segment, context.weaponBonus(),
                SkillSegmentMath.segmentMagicDamage(castSkill, segment), null, bonus);
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        boolean fieldHost = definition != null && definition.fieldRadius() > 0f;
        ResourceEnchantLoad inheritedLoad = bonus.resourceLoad == null ? ResourceEnchantLoad.EMPTY : bonus.resourceLoad;
        boolean carries = !fieldHost && technique.getBaseDamage() > 0f
                && (!enchantLocals.isEmpty() || !inheritedLoad.empty());
        EnchantmentSnapshot snapshot = DomainSpawnInheritance.inheritContact(
                ContactCast.snapshot(castSkill, segment, owner));
        ItemStack weaponCopy = context.weaponCopy();
        Vec3 origin = context.origin();
        Vec3 direction = context.baseDirection();
        BirthLayout.Slot inheritedBirth = inheritedBirth(technique, inheritedLoad, context);
        if (inheritedBirth != null) {
            BirthLayout.install(inheritedBirth);
        }
        ReleaseBatch previousBatch = ReleaseBatch.current();
        ReleaseBatch.bind(batch);
        ReleaseExecutionContext.bind(context);
            try {
                if (member.cooperativeAbsorbed()) {
                    return;
                }
                if (member.cooperativeRecipe() != null) {
                    PublicationAttempt cooperativeAttempt = PublicationAttempt.openLoose(context.roundId(), memberIndex, 0);
                    if (cooperativeAttempt == null) {
                        return;
                    }
                    try {
                        CooperativeCastRouter.launchPrepaid(owner, castSkill, weaponCopy, direction, origin,
                                context.domainInstanceId(), "rule_domain_bind", round.budget);
                    } finally {
                        cooperativeAttempt.close();
                    }
                    return;
                }
                if (definition != null && definition.submitsHostGroup()) {
                if (!token.valid(nowTick)) {
                    round.cancel();
                    return;
                }
                if (hostCast == null || round.blocksLaunch()) {
                    return;
                }
                PublicationAttempt attempt = PublicationAttempt.openHostGroup(context.roundId(), memberIndex);
                if (attempt == null) {
                    return;
                }
                boolean hostCommitted = false;
                try {
                    ReleaseBatch.Tally tally = plannedCopies > 1 && batch != null ? batch.openGroupTally() : null;
                    UUID hostGroup = hostCast.hostId(memberIndex, 0);
                    ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(owner, technique, castTechniques, hostLocal,
                            weaponCopy, damage, carries, enchantments, lockCast, dropCast, tally, plannedCopies, batch, bonus,
                            memberIndex, direction, origin, null, null, hostCast, hostGroup, null, chainInput);
                    hostCommitted = outcome != null && outcome.committed();
                    if (hostCommitted && fieldHost && snapshot != null) {
                        FieldZones.bindBorn(owner.getUUID(), snapshot, outcome.tick());
                    }
                } finally {
                    attempt.noteHostCommitted(hostCommitted);
                    attempt.close();
                }
                return;
            }
            round.preparePrices(memberIndex, plannedCopies);
            int inheritedPrice = DomainSpawnInheritance.automaticEnchantPrice(castSkill, enchantLocals,
                    !enchantLocals.isEmpty());
            if (inheritedPrice > 0) {
                round.addRetainedShare(inheritedPrice);
            }
            ReleaseBatch.Tally tally = plannedCopies > 1 && batch != null ? batch.openGroupTally() : null;
            boolean acceptedCopy = false;
            int acceptTick = nowTick;
            for (int copyIndex = 0; copyIndex < plannedCopies; copyIndex++) {
                int copyTick = owner.getServer() == null ? nowTick : owner.getServer().getTickCount();
                if (!token.valid(copyTick)) {
                    round.cancel();
                    break;
                }
                if (round.blocksLaunch()) {
                    break;
                }
                if (!HeldSwordGate.allows(technique, owner.getMainHandItem())) {
                    break;
                }
                PublicationAttempt attempt = PublicationAttempt.openLoose(context.roundId(), memberIndex, copyIndex);
                if (attempt == null) {
                    break;
                }
                try {
                    SkillSegmentRunner.runSegment(owner, technique, castTechniques, hostLocal, weaponCopy, damage, carries,
                            enchantments, null, lockCast, dropCast, tally, copyIndex, plannedCopies, batch, direction, origin,
                            bonus, memberIndex, null, null, null, chainInput, null);
                } finally {
                    attempt.close();
                }
                if (attempt.accepted()) {
                    acceptedCopy = true;
                    acceptTick = owner.getServer() == null ? copyTick : owner.getServer().getTickCount();
                }
                if (round.blocksLaunch()) {
                    break;
                }
            }
            if (acceptedCopy && fieldHost && snapshot != null) {
                FieldZones.bindBorn(owner.getUUID(), snapshot, acceptTick);
            }
        } finally {
            if (inheritedBirth != null) {
                BirthLayout.clear();
            }
            DomainSpawnInheritance.release();
            ReleaseExecutionContext.unbind();
            ReleaseBatch.bind(previousBatch);
        }
    }

    private static BirthLayout.Slot inheritedBirth(Technique technique, ResourceEnchantLoad inheritedLoad,
                                                      ReleaseExecutionContext context) {
        ResourceEnchantLoad ownLoad = inheritedLoad == null ? ResourceEnchantLoad.EMPTY : inheritedLoad;
        DomainInstance carriedDomain = DomainService.find(context.domainInstanceId());
        boolean carried = carriedDomain != null
                && !DomainSpawnInheritance.inheritable(carriedDomain.carriedEnchantments()).isEmpty();
        if (ownLoad.empty() && !carried) {
            return null;
        }
        UUID rootCastId = DomainSpawnInheritance.automaticRoot(context);
        if (rootCastId == null) {
            return null;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        BirthLayout.Slot slot = new BirthLayout.Slot();
        slot.rootCastId = rootCastId;
        slot.resourceCommit = true;
        slot.resourceLoad = ownLoad;
        slot.bodyScaledBase = definition == null ? 0 : definition.bodyScaledSeedBase();
        return slot;
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

    /**
     * 只放入这一名成员的继承修饰、本地修饰、附魔和宿主。
     * 现有段扫描会读传入列表；短链里没有兄弟宿主，内部修饰不会串到后面的攻击。
     */
    private static MemberCastSlice memberSlice(Skill frozenSkill, List<Technique> techniques, BoundMember member,
                                               MemberCastView view) {
        if (view == null || member == null || view.independentEnchant || view.ordinal != member.memberOrdinal()
                || view.hostIndex != member.localIndex()) {
            return null;
        }
        if (view.hostIndex < 0 || view.hostIndex >= techniques.size() || techniques.get(view.hostIndex) == null) {
            return null;
        }
        if (view.copyCount < 1 || view.copyCount == Integer.MAX_VALUE) {
            return null;
        }
        List<Technique> castTechniques = new ArrayList<>();
        List<ShuAttribute> castEmbryos = new ArrayList<>();
        List<Integer> enchantLocals = new ArrayList<>();
        if (!appendCast(castTechniques, castEmbryos, enchantLocals, frozenSkill, techniques,
                view.inheritedModifierIndexes, view.hostIndex, false)) {
            return null;
        }
        if (!appendCast(castTechniques, castEmbryos, enchantLocals, frozenSkill, techniques,
                view.localModifierIndexes, view.hostIndex, false)) {
            return null;
        }
        if (!appendCast(castTechniques, castEmbryos, enchantLocals, frozenSkill, techniques,
                view.attachedEnchantIndexes, view.hostIndex, true)) {
            return null;
        }
        castTechniques.add(techniques.get(view.hostIndex));
        castEmbryos.add(frozenSkill.getEmbryo(view.hostIndex));
        Skill castSkill = new Skill(frozenSkill.getName(), frozenSkill.getType(), castTechniques, castEmbryos);
        return new MemberCastSlice(castSkill, castTechniques, castTechniques.size() - 1, List.copyOf(enchantLocals),
                view.copyCount, view.chainInput, view.entityLock, view.drop);
    }

    private static boolean appendCast(List<Technique> castTechniques, List<ShuAttribute> castEmbryos,
                                      List<Integer> enchantLocals, Skill frozenSkill, List<Technique> techniques,
                                      List<Integer> sourceIndexes, int hostIndex, boolean enchantSlot) {
        if (sourceIndexes == null) {
            return true;
        }
        for (int slotIndex : sourceIndexes) {
            if (slotIndex < 0 || slotIndex >= techniques.size() || techniques.get(slotIndex) == null) {
                return false;
            }
            if (slotIndex == hostIndex) {
                continue;
            }
            castTechniques.add(techniques.get(slotIndex));
            castEmbryos.add(frozenSkill.getEmbryo(slotIndex));
            if (enchantSlot) {
                enchantLocals.add(castTechniques.size() - 1);
            }
        }
        return true;
    }

    private static int[] splitPool(int poolPrice, int plannedCopies) {
        int copyCount = Math.max(1, plannedCopies);
        int safePool = Math.max(0, poolPrice);
        int basePrice = safePool / copyCount;
        int leftover = safePool % copyCount;
        int[] prices = new int[copyCount];
        for (int copyIndex = 0; copyIndex < copyCount; copyIndex++) {
            prices[copyIndex] = basePrice + (copyIndex < leftover ? 1 : 0);
        }
        return prices;
    }

    private static final class MemberCastSlice {
        private final Skill skill;
        private final List<Technique> techniques;
        private final int hostLocal;
        private final List<Integer> enchantLocals;
        private final int plannedCopies;
        private final ReleaseInput chainInput;
        private final boolean entityLock;
        private final boolean dropCast;

        private MemberCastSlice(Skill skill, List<Technique> techniques, int hostLocal, List<Integer> enchantLocals,
                                int plannedCopies, ReleaseInput chainInput, boolean entityLock, boolean dropCast) {
            this.skill = skill;
            this.techniques = techniques;
            this.hostLocal = hostLocal;
            this.enchantLocals = enchantLocals;
            this.plannedCopies = plannedCopies;
            this.chainInput = chainInput == null ? ReleaseInput.BASE : chainInput;
            this.entityLock = entityLock;
            this.dropCast = dropCast;
        }
    }

    private static final class PricedQuote {
        private final int[] memberPools;
        private final int ruleShare;
        private final int totalPrice;

        private PricedQuote(int[] memberPools, int ruleShare, int totalPrice) {
            this.memberPools = memberPools;
            this.ruleShare = ruleShare;
            this.totalPrice = totalPrice;
        }
    }

    private static final class OpenRound implements RoundHandle, PublicationAttempt.Ledger {
        private final UUID roundIdentity;
        private final ServerPlayer owner;
        private final GroupBudget budget;
        private final HostCast hostCast;
        private final int[] memberPools;
        private final int ruleShare;
        private final int[][] loosePrices;
        private final boolean[][] looseRetained;
        private final boolean[] hostRetained;
        private final boolean[] memberClosed;
        private int membersLeft;
        private int inFlight;
        private boolean cancelRequested;
        private boolean halted;
        private boolean settleStarted;
        private boolean settled;
        private boolean ruleRetained;

        private OpenRound(UUID roundIdentity, ServerPlayer owner, GroupBudget budget, HostCast hostCast,
                          PricedQuote quote, int memberCount) {
            this.roundIdentity = roundIdentity;
            this.owner = owner;
            this.budget = budget;
            this.hostCast = hostCast;
            this.memberPools = quote.memberPools;
            this.ruleShare = quote.ruleShare;
            this.loosePrices = new int[memberPools.length][];
            this.looseRetained = new boolean[memberPools.length][];
            this.hostRetained = new boolean[memberPools.length];
            this.memberClosed = new boolean[Math.max(0, memberCount)];
            this.membersLeft = memberClosed.length;
            PublicationAttempt.bindLedger(roundIdentity, this);
        }

        @Override
        public UUID roundId() {
            return roundIdentity;
        }

        @Override
        public void cancel() {
            if (settled) {
                return;
            }
            cancelRequested = true;
            if (inFlight == 0) {
                settleNow();
            }
        }

        @Override
        public boolean cancelled() {
            return cancelRequested;
        }

        @Override
        public boolean done() {
            return settled;
        }

        private boolean blocksLaunch() {
            return settled || settleStarted || cancelRequested || halted;
        }

        private void haltRemainder() {
            if (settled) {
                return;
            }
            halted = true;
            if (inFlight == 0) {
                settleNow();
            }
        }

        private void closeMember(int memberIndex) {
            if (settled || settleStarted || membersLeft <= 0) {
                return;
            }
            if (memberIndex >= 0 && memberIndex < memberClosed.length) {
                if (memberClosed[memberIndex]) {
                    return;
                }
                memberClosed[memberIndex] = true;
            }
            membersLeft--;
            if (membersLeft == 0 && inFlight == 0) {
                settleNow();
            }
        }

        private void addRetainedShare(int share) {
            if (share > 0) {
                budget.commit(share);
            }
        }

        private void preparePrices(int memberIndex, int plannedCopies) {
            if (memberIndex < 0 || memberIndex >= memberPools.length || loosePrices[memberIndex] != null) {
                return;
            }
            loosePrices[memberIndex] = splitPool(memberPools[memberIndex], plannedCopies);
            looseRetained[memberIndex] = new boolean[loosePrices[memberIndex].length];
        }

        @Override
        public boolean openingAllowed(UUID roundId) {
            return roundIdentity.equals(roundId) && !settled && !settleStarted && !cancelRequested && !halted;
        }

        @Override
        public void attemptOpened(UUID roundId) {
            if (roundIdentity.equals(roundId)) {
                inFlight++;
            }
        }

        @Override
        public void retainLoose(UUID roundId, int memberIndex, int copyIndex) {
            if (!roundIdentity.equals(roundId) || settleStarted || memberIndex < 0 || memberIndex >= loosePrices.length) {
                return;
            }
            int[] prices = loosePrices[memberIndex];
            boolean[] retained = looseRetained[memberIndex];
            if (prices == null || retained == null || copyIndex < 0 || copyIndex >= prices.length || retained[copyIndex]) {
                return;
            }
            retained[copyIndex] = true;
            budget.commit(prices[copyIndex]);
            retainRuleOnce();
        }

        @Override
        public void retainHost(UUID roundId, int memberIndex) {
            if (!roundIdentity.equals(roundId) || settleStarted || memberIndex < 0 || memberIndex >= memberPools.length) {
                return;
            }
            if (hostRetained[memberIndex]) {
                return;
            }
            hostRetained[memberIndex] = true;
            budget.commit(memberPools[memberIndex]);
            retainRuleOnce();
        }

        /** 规则费挂在这一轮第一笔真实成功上，只留一次；谁都没发布就留在未提交份额里退回。 */
        private void retainRuleOnce() {
            if (ruleRetained) {
                return;
            }
            ruleRetained = true;
            budget.commit(ruleShare);
        }

        @Override
        public void attemptFinished(UUID roundId) {
            if (!roundIdentity.equals(roundId)) {
                return;
            }
            if (inFlight > 0) {
                inFlight--;
            }
            if (inFlight == 0 && (cancelRequested || halted || membersLeft == 0)) {
                settleNow();
            }
        }

        private void settleNow() {
            if (settleStarted) {
                return;
            }
            settleStarted = true;
            budget.settle(owner);
            settled = true;
            PublicationAttempt.unbindLedger(roundIdentity);
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
