package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.command.SkillEntityCount;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.shu.technique.release.cooperative.ActorAttackAdapterRegistry;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeCapabilityIndex;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeCarrier;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeCastBuild;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeCastRecipe;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeContent;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeRuntime;
import com.bslx.XueXiMod.shu.technique.release.cooperative.ExecutionFamilyPlan;
import com.bslx.XueXiMod.shu.technique.release.cooperative.TechniqueExecutionCapabilities;
import com.bslx.XueXiMod.shu.technique.release.cooperative.action.ObliqueSlashEffect;
import com.bslx.XueXiMod.shu.technique.release.cooperative.action.SpinTimelineDefinition;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorAttackAdapter;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AdapterVolumeSpec;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AttackMemberPlan;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.AttackPreparation;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ChainIndexMapping;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperationOrder;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperationSettings;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTechniqueIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeTexts;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.DomainLink;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyControlToken;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FrozenCarrierCredential;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.HitContinueLink;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.LogicalSpawnIndex;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PlanShareSchedule;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PlanSlotKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.PreparedAttack;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.QuotedCooldown;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGate;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateKind;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.SettlementEnvelope;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ShareRole;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ShareSlot;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.WeaponDisplaySnapshot;
import com.bslx.XueXiMod.shu.technique.release.cooperative.attack.HorizontalSlashActorAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 把玩家链降成一份尚未扣费的执行族。没有本批能力时返回 null，旧路径继续。
 */
public final class CooperativeChainCompiler {
    public static final String BUDGET_KEY = "gui.xueximod.cooperative.budget";
    public static final String CLONE_SLOT_KEY = "gui.xueximod.cooperative.clone_slot";
    public static final String CLONE_MIXED_KEY = "gui.xueximod.cooperative.clone_mixed";
    private static final String DASH_ID = "movement_dash";
    private static final String LEAP_ID = "movement_leap";

    private CooperativeChainCompiler() {
    }

    public static @Nullable CooperativeCastRecipe describe(Skill skill) {
        if (skill == null) {
            return null;
        }
        List<Technique> techniques = skill.getTechniques();
        if (techniques == null || techniques.isEmpty()) {
            return null;
        }
        boolean opens = false;
        boolean domainBind = false;
        for (Technique technique : techniques) {
            if (technique == null) {
                continue;
            }
            if (SkillSegmentMath.roleOf(technique) == ChainRole.RULE_DOMAIN_BIND) {
                domainBind = true;
            }
            if (CooperativeCapabilityIndex.opensFamily(technique.getId())) {
                opens = true;
            }
        }
        if (domainBind || !opens) {
            return null;
        }
        return walk(skill, techniques);
    }

    public static CooperativeCastBuild prepare(ServerPlayer player, Skill skill, ItemStack weapon, int skillSlot,
                                               double cooldownReduction, boolean prepaid,
                                               @Nullable Vec3 aimOverride, @Nullable DomainLink domainLink) {
        CooperativeCastRecipe recipe = describe(skill);
        if (recipe == null) {
            return null;
        }
        if (recipe.refused()) {
            return CooperativeCastBuild.refused(recipe.refusalKey());
        }
        try {
            return freeze(player, skill, weapon, skillSlot, cooldownReduction, prepaid, recipe, aimOverride, domainLink);
        } catch (RuntimeException failure) {
            return CooperativeCastBuild.refused(BUDGET_KEY);
        }
    }

    private static CooperativeCastRecipe walk(Skill skill, List<Technique> techniques) {
        WalkState state = new WalkState();
        int chainIndex = 0;
        while (chainIndex < techniques.size()) {
            int currentIndex = chainIndex;
            chainIndex += 1;
            Technique technique = techniques.get(currentIndex);
            if (technique == null) {
                return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
            }
            MulticastExpansion expansion = MulticastCompileAdapter.expansionOpeningAt(techniques, currentIndex);
            if (expansion != null) {
                int resumeAt = MulticastCompileAdapter.resumeIndex(expansion, currentIndex);
                if (resumeAt < 0) {
                    return CooperativeCastRecipe.refused(MulticastCompileAdapter.boundaryRefusal(expansion));
                }
                String refusalKey = adoptSharedExpansion(skill, techniques, expansion, state);
                if (refusalKey != null) {
                    return CooperativeCastRecipe.refused(refusalKey);
                }
                chainIndex = resumeAt;
                continue;
            }
            String techniqueId = technique.getId();
            TechniqueExecutionCapabilities capabilities = CooperativeCapabilityIndex.find(techniqueId);
            ChainRole role = SkillSegmentMath.roleOf(technique);
            if (technique.isEnchantment()) {
                state.pendingEnchants.add(currentIndex);
                state.mappings.add(new ChainIndexMapping(currentIndex, techniqueId, PlanSlotKind.ENCHANT, -1, false));
                continue;
            }
            if (capabilities.isStandaloneDeployment()) {
                state.sawStandalone = true;
                state.mappings.add(new ChainIndexMapping(currentIndex, techniqueId, PlanSlotKind.OTHER, -1, false));
                continue;
            }
            if (capabilities.isCooperativeRule() || role == ChainRole.RULE_COOPERATIVE) {
                state.cooperativeRules += 1;
                if (state.cooperativeRules > 1) {
                    return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
                }
                state.mappings.add(new ChainIndexMapping(currentIndex, techniqueId, PlanSlotKind.COOPERATIVE_RULE, -1, false));
                continue;
            }
            if (capabilities.isTimedRelease()) {
                if (state.timedIndex >= 0 || state.contactIndex >= 0) {
                    return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
                }
                state.timedIndex = currentIndex;
                state.actionTechniqueId = techniqueId;
                state.mappings.add(new ChainIndexMapping(currentIndex, techniqueId, PlanSlotKind.TIMED_ACTION, -1, false));
                continue;
            }
            if (SkillSegmentMath.isOperationHost(technique)) {
                return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
            }
            if (SkillSegmentMath.isChainModifier(technique)) {
                String noted = noteCooperativeModifier(state, technique, currentIndex, true);
                if (noted != null) {
                    return CooperativeCastRecipe.refused(noted);
                }
                PlanSlotKind slotKind = seekSlot(role);
                state.mappings.add(new ChainIndexMapping(currentIndex, techniqueId, slotKind, -1, false));
                continue;
            }
            if (DASH_ID.equals(techniqueId) && role == ChainRole.ACTION) {
                if (state.timedIndex >= 0 || state.contactIndex >= 0) {
                    return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
                }
                state.contactIndex = currentIndex;
                state.actionTechniqueId = techniqueId;
                state.mappings.add(new ChainIndexMapping(currentIndex, techniqueId, PlanSlotKind.CONTACT_ACTION, -1, false));
                continue;
            }
            if (LEAP_ID.equals(techniqueId)) {
                state.leapIndexes.add(currentIndex);
                state.mappings.add(new ChainIndexMapping(currentIndex, techniqueId, PlanSlotKind.OTHER, -1, false));
                continue;
            }
            if (role == ChainRole.ATTACK || capabilities.isExplicitAttackAdapter()
                    || ActorAttackAdapterRegistry.supports(techniqueId)) {
                if (!ActorAttackAdapterRegistry.supports(techniqueId)) {
                    return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
                }
                state.attacks.add(new CooperativeCastRecipe.AttackSketch(
                        currentIndex, state.memberOrdinal, techniqueId, Math.max(1, state.copyCount), 0,
                        state.staggerPending, state.continueParent, state.pendingEnchants, state.pendingModifiers,
                        state.entityLock, state.upwellPath));
                state.mappings.add(new ChainIndexMapping(
                        currentIndex, techniqueId, PlanSlotKind.IMMEDIATE_ATTACK, state.memberOrdinal, true));
                state.memberOrdinal += 1;
                consumeAttackPending(state);
                continue;
            }
            return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
        }
        if (state.sawStandalone) {
            return CooperativeCastRecipe.refused(CLONE_MIXED_KEY);
        }
        if (state.continueParent >= 0) {
            return CooperativeCastRecipe.refused("gui.xueximod.batch.missing_child");
        }
        if (state.cooperativeRules > 0 && state.attacks.isEmpty()) {
            return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
        }
        if (state.contactIndex >= 0 && state.attacks.isEmpty()) {
            return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
        }
        ReleaseGateKind gateKind = ReleaseGateKind.IMMEDIATE;
        int actionChainIndex = -1;
        if (state.timedIndex >= 0) {
            gateKind = ReleaseGateKind.TIMED;
            actionChainIndex = state.timedIndex;
        } else if (state.contactIndex >= 0) {
            gateKind = ReleaseGateKind.CONTACT;
            actionChainIndex = state.contactIndex;
        }
        if (!state.pendingEnchants.isEmpty() && !state.attacks.isEmpty()) {
            CooperativeCastRecipe.AttackSketch last = state.attacks.get(state.attacks.size() - 1);
            List<Integer> merged = new ArrayList<>(last.enchantIndexes());
            merged.addAll(state.pendingEnchants);
            state.attacks.set(state.attacks.size() - 1, new CooperativeCastRecipe.AttackSketch(
                    last.chainIndex(), last.memberOrdinal(), last.techniqueId(), last.copyCount(), last.delayTicks(),
                    last.stagger(), last.continueParentOrdinal(), merged, last.modifierIndexes(),
                    last.entityLock(), last.upwellPath()));
        }
        return CooperativeCastRecipe.ready(gateKind, actionChainIndex, state.actionTechniqueId, state.cooperativeRules > 0,
                state.spaceLock, state.scatter, state.dropPath, state.attacks, state.mappings, state.leapIndexes);
    }

    /**
     * 共享展开已经读完的区间只进来一次。攻击延迟用成员序号，份数用复制积。
     * 空展开和边界不前进由调用方按各自的键拒绝，不回到固定两段。
     */
    private static String adoptSharedExpansion(Skill skill, List<Technique> techniques, MulticastExpansion expansion,
                                               WalkState state) {
        for (MulticastMember member : expansion.orderedMembers()) {
            String refusalKey = adoptSharedMember(skill, techniques, expansion, state, member);
            if (refusalKey != null) {
                return refusalKey;
            }
        }
        for (int consumedIndex : expansion.consumedIndexes()) {
            ChainIndexMapping mapping = sharedMapping(techniques, state, consumedIndex);
            if (mapping == null) {
                return CooperativeTexts.UNADAPTED_ATTACK_KEY;
            }
            state.mappings.add(mapping);
        }
        return null;
    }

    private static String adoptSharedMember(Skill skill, List<Technique> techniques, MulticastExpansion expansion,
                                            WalkState state, MulticastMember member) {
        int hostIndex = member.hostIndex();
        if (hostIndex < 0 || hostIndex >= techniques.size()) {
            return CooperativeTexts.UNADAPTED_ATTACK_KEY;
        }
        Technique technique = techniques.get(hostIndex);
        if (technique == null) {
            return CooperativeTexts.UNADAPTED_ATTACK_KEY;
        }
        if (member.independentEnchant() || technique.isEnchantment()) {
            state.pendingEnchants.add(hostIndex);
            return null;
        }
        String techniqueId = technique.getId();
        TechniqueExecutionCapabilities capabilities = CooperativeCapabilityIndex.find(techniqueId);
        ChainRole role = SkillSegmentMath.roleOf(technique);
        if (capabilities.isStandaloneDeployment()) {
            state.sawStandalone = true;
            return null;
        }
        if (capabilities.isCooperativeRule() || role == ChainRole.RULE_COOPERATIVE) {
            state.cooperativeRules += 1;
            if (state.cooperativeRules > 1) {
                return CooperativeTexts.UNADAPTED_ATTACK_KEY;
            }
            return null;
        }
        if (capabilities.isTimedRelease()) {
            if (state.timedIndex >= 0 || state.contactIndex >= 0) {
                return CooperativeTexts.UNADAPTED_ATTACK_KEY;
            }
            state.timedIndex = hostIndex;
            state.actionTechniqueId = techniqueId;
            return null;
        }
        if (SkillSegmentMath.isOperationHost(technique)) {
            return CooperativeTexts.UNADAPTED_ATTACK_KEY;
        }
        if (SkillSegmentMath.isChainModifier(technique)) {
            return noteCooperativeModifier(state, technique, hostIndex, true);
        }
        if (DASH_ID.equals(techniqueId) && role == ChainRole.ACTION) {
            if (state.timedIndex >= 0 || state.contactIndex >= 0) {
                return CooperativeTexts.UNADAPTED_ATTACK_KEY;
            }
            state.contactIndex = hostIndex;
            state.actionTechniqueId = techniqueId;
            return null;
        }
        if (LEAP_ID.equals(techniqueId)) {
            state.leapIndexes.add(hostIndex);
            return null;
        }
        if (role == ChainRole.ATTACK || capabilities.isExplicitAttackAdapter()
                || ActorAttackAdapterRegistry.supports(techniqueId)) {
            if (!ActorAttackAdapterRegistry.supports(techniqueId)) {
                return CooperativeTexts.UNADAPTED_ATTACK_KEY;
            }
            String folded = foldMemberAttachments(state, techniques, member);
            if (folded != null) {
                return folded;
            }
            MulticastCompileAdapter.CopyRead copies = MulticastCompileAdapter.memberCopies(skill, techniques, member);
            if (copies.overflow) {
                return BUDGET_KEY;
            }
            int delayTicks = MulticastCompileAdapter.memberDelayTicks(expansion, member.ordinal());
            state.attacks.add(new CooperativeCastRecipe.AttackSketch(
                    hostIndex, state.memberOrdinal, techniqueId, Math.max(1, copies.copyCount), delayTicks,
                    state.staggerPending, state.continueParent, state.pendingEnchants, state.pendingModifiers,
                    state.entityLock, state.upwellPath));
            state.memberOrdinal += 1;
            consumeAttackPending(state);
            return null;
        }
        return CooperativeTexts.UNADAPTED_ATTACK_KEY;
    }

    private static String foldMemberAttachments(WalkState state, List<Technique> techniques, MulticastMember member) {
        String noted = foldModifierIndexes(state, techniques, member.inheritedModifierIndexes());
        if (noted != null) {
            return noted;
        }
        noted = foldModifierIndexes(state, techniques, member.localModifierIndexes());
        if (noted != null) {
            return noted;
        }
        noted = foldModifierIndexes(state, techniques, member.replicationIndexes());
        if (noted != null) {
            return noted;
        }
        for (int enchantIndex : member.attachedEnchantIndexes()) {
            state.pendingEnchants.add(enchantIndex);
        }
        return null;
    }

    private static String foldModifierIndexes(WalkState state, List<Technique> techniques, List<Integer> slotIndexes) {
        for (int slotIndex : slotIndexes) {
            if (slotIndex < 0 || slotIndex >= techniques.size()) {
                continue;
            }
            Technique listed = techniques.get(slotIndex);
            if (listed == null) {
                return CooperativeTexts.UNADAPTED_ATTACK_KEY;
            }
            String noted = noteCooperativeModifier(state, listed, slotIndex, false);
            if (noted != null) {
                return noted;
            }
        }
        return null;
    }

    /**
     * 原赋值点只把复制因子写进份数。共享展开上的附着复制不再赋一次，份数另取复制积。
     * 多重声明只由共享展开读取，这里不再另开两段门槛。
     */
    private static String noteCooperativeModifier(WalkState state, Technique technique, int chainIndex,
                                                  boolean assignReplication) {
        if (state.pendingModifiers.contains(chainIndex)) {
            return null;
        }
        ChainRole role = SkillSegmentMath.roleOf(technique);
        if (role == ChainRole.RULE_TRIPLE) {
            if (assignReplication) {
                int factor = MulticastCompileAdapter.replicationFactor(technique);
                if (factor < 2) {
                    return CooperativeTexts.UNADAPTED_ATTACK_KEY;
                }
                state.copyCount = factor;
            }
        } else if (role == ChainRole.RULE_STAGGER) {
            state.staggerPending = true;
        } else if (role == ChainRole.RULE_HIT_CONTINUE) {
            if (state.attacks.isEmpty()) {
                return "gui.xueximod.batch.missing_parent";
            }
            if (state.continueParent >= 0) {
                return "gui.xueximod.batch.nested";
            }
            state.continueParent = state.attacks.get(state.attacks.size() - 1).chainIndex();
        } else if (role == ChainRole.SEEK_SPACE || role == ChainRole.SEEK_SURFACE) {
            state.spaceLock = true;
        } else if (role == ChainRole.TRAJECTORY_SCATTER) {
            state.scatter = true;
        } else if (role == ChainRole.TRAJECTORY_DROP) {
            state.dropPath = true;
        } else if (role == ChainRole.SEEK_ENTITY) {
            state.entityLock = true;
        } else if (role == ChainRole.TRAJECTORY_UPWELL) {
            state.upwellPath = true;
        }
        state.pendingModifiers.add(chainIndex);
        return null;
    }

    private static void consumeAttackPending(WalkState state) {
        state.pendingEnchants = new ArrayList<>();
        state.pendingModifiers = new ArrayList<>();
        state.copyCount = 1;
        state.staggerPending = false;
        state.continueParent = -1;
        state.entityLock = false;
        state.upwellPath = false;
    }

    private static @Nullable ChainIndexMapping sharedMapping(List<Technique> techniques, WalkState state, int consumedIndex) {
        if (consumedIndex < 0 || consumedIndex >= techniques.size()) {
            return null;
        }
        Technique technique = techniques.get(consumedIndex);
        if (technique == null || technique.getId() == null || technique.getId().isBlank()) {
            return null;
        }
        String techniqueId = technique.getId();
        if (SkillSegmentMath.isRemovedRule(techniqueId) || technique.isEnchantment()) {
            PlanSlotKind slotKind = technique.isEnchantment() ? PlanSlotKind.ENCHANT : PlanSlotKind.OTHER;
            return new ChainIndexMapping(consumedIndex, techniqueId, slotKind, -1, false);
        }
        TechniqueExecutionCapabilities capabilities = CooperativeCapabilityIndex.find(techniqueId);
        ChainRole role = SkillSegmentMath.roleOf(technique);
        if (capabilities.isStandaloneDeployment()) {
            return new ChainIndexMapping(consumedIndex, techniqueId, PlanSlotKind.OTHER, -1, false);
        }
        if (capabilities.isCooperativeRule() || role == ChainRole.RULE_COOPERATIVE) {
            return new ChainIndexMapping(consumedIndex, techniqueId, PlanSlotKind.COOPERATIVE_RULE, -1, false);
        }
        if (capabilities.isTimedRelease()) {
            return new ChainIndexMapping(consumedIndex, techniqueId, PlanSlotKind.TIMED_ACTION, -1, false);
        }
        if (SkillSegmentMath.isOperationHost(technique)) {
            return new ChainIndexMapping(consumedIndex, techniqueId, PlanSlotKind.OTHER, -1, false);
        }
        if (SkillSegmentMath.isChainModifier(technique)) {
            return new ChainIndexMapping(consumedIndex, techniqueId, seekSlot(role), -1, false);
        }
        if (DASH_ID.equals(techniqueId) && role == ChainRole.ACTION) {
            return new ChainIndexMapping(consumedIndex, techniqueId, PlanSlotKind.CONTACT_ACTION, -1, false);
        }
        if (LEAP_ID.equals(techniqueId)) {
            return new ChainIndexMapping(consumedIndex, techniqueId, PlanSlotKind.OTHER, -1, false);
        }
        CooperativeCastRecipe.AttackSketch sketch = sketchAt(state.attacks, consumedIndex);
        if (sketch != null) {
            return new ChainIndexMapping(
                    consumedIndex, techniqueId, PlanSlotKind.IMMEDIATE_ATTACK, sketch.memberOrdinal(), true);
        }
        return new ChainIndexMapping(consumedIndex, techniqueId, PlanSlotKind.OTHER, -1, false);
    }

    private static @Nullable CooperativeCastRecipe.AttackSketch sketchAt(
            List<CooperativeCastRecipe.AttackSketch> attacks, int chainIndex) {
        for (int attackCursor = attacks.size() - 1; attackCursor >= 0; attackCursor--) {
            CooperativeCastRecipe.AttackSketch sketch = attacks.get(attackCursor);
            if (sketch.chainIndex() == chainIndex) {
                return sketch;
            }
        }
        return null;
    }

    private static PlanSlotKind seekSlot(ChainRole role) {
        if (role == ChainRole.SEEK_SPACE || role == ChainRole.SEEK_ENTITY || role == ChainRole.SEEK_SURFACE) {
            return PlanSlotKind.SEEK;
        }
        if (role == ChainRole.TRAJECTORY_DROP || role == ChainRole.TRAJECTORY_SCATTER
                || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_UPWELL) {
            return PlanSlotKind.TRAJECTORY;
        }
        return PlanSlotKind.RULE;
    }

    private static @Nullable CooperativeCastBuild freeze(ServerPlayer player, Skill skill, ItemStack weapon, int skillSlot,
                                                         double cooldownReduction, boolean prepaid,
                                                         CooperativeCastRecipe recipe, @Nullable Vec3 aimOverride,
                                                         @Nullable DomainLink domainLink) {
        List<Technique> techniques = skill.getTechniques();
        int spawnCount = Math.max(1, SkillEntityCount.entityCount());
        long soloEmits = 0L;
        for (CooperativeCastRecipe.AttackSketch attack : recipe.attacks()) {
            soloEmits = Math.addExact(soloEmits, Math.multiplyExact((long) attack.copyCount(), (long) spawnCount));
        }
        if (soloEmits > CooperativeStructureLimits.FAMILY_PLANNED_EMIT_CAP) {
            return CooperativeCastBuild.refused(BUDGET_KEY);
        }
        boolean keepCooperation = recipe.cooperationRequested() && !recipe.attacks().isEmpty();
        CooperativeRuntime.Room room = CooperativeRuntime.service().roomFor(player, soloEmits, keepCooperation);
        if (room == CooperativeRuntime.Room.REFUSE) {
            return CooperativeCastBuild.refused(BUDGET_KEY);
        }
        if (room == CooperativeRuntime.Room.SOLO) {
            keepCooperation = false;
        }
        int actionMana = 0;
        int actionCooldown = 0;
        if (recipe.actionTechniqueId() != null) {
            Technique action = techniques.get(recipe.actionChainIndex());
            actionMana = readMana(action);
            actionCooldown = scaleCooldown(readCooldown(action), cooldownReduction);
        }
        int cooperationMana = 0;
        int cooperationCooldown = 0;
        if (keepCooperation) {
            Technique rule = TechniqueManager.getById(CooperativeTechniqueIds.COOPERATIVE);
            cooperationMana = rule == null ? CooperativeContent.RULE_MANA_COST : readMana(rule);
            cooperationCooldown = scaleCooldown(rule == null ? CooperativeContent.RULE_COOLDOWN_TICKS : readCooldown(rule), cooldownReduction);
        }
        List<Integer> attackMana = new ArrayList<>();
        List<Integer> attackCooldown = new ArrayList<>();
        int attackSum = 0;
        for (CooperativeCastRecipe.AttackSketch attack : recipe.attacks()) {
            Technique technique = techniques.get(attack.chainIndex());
            int mana = readMana(technique);
            attackMana.add(mana);
            attackCooldown.add(scaleCooldown(readCooldown(technique), cooldownReduction));
            attackSum += mana;
        }
        int totalMana = actionMana + attackSum + cooperationMana;
        int soloMana = actionMana + attackSum;
        if (!prepaid) {
            if (keepCooperation && !MagicPool.canAfford(player, totalMana) && MagicPool.canAfford(player, soloMana)) {
                keepCooperation = false;
                cooperationMana = 0;
                cooperationCooldown = 0;
                totalMana = soloMana;
            }
            if (!MagicPool.canAfford(player, totalMana)) {
                return CooperativeCastBuild.unaffordable();
            }
        }
        Aim frozen = freezeAim(player, aimOverride);
        WeaponDisplaySnapshot weaponSnapshot = weapon == null || weapon.isEmpty()
                ? WeaponDisplaySnapshot.EMPTY : WeaponDisplaySnapshot.capture(weapon);
        FrozenCarrierCredential carrier = CooperativeCarrier.freeze(player, skillSlot);
        FamilyControlToken cancelToken = new FamilyControlToken(UUID.randomUUID());
        FamilyControlToken completionToken = new FamilyControlToken(UUID.randomUUID());
        List<AttackMemberPlan> members = new ArrayList<>();
        List<LogicalSpawnIndex> spawns = new ArrayList<>();
        List<ShareSlot> shares = new ArrayList<>();
        List<QuotedCooldown> cooldowns = new ArrayList<>();
        List<ChainIndexMapping> mappings = new ArrayList<>();
        for (ChainIndexMapping mapping : recipe.chainIndexes()) {
            if (!keepCooperation && mapping.slotKind() == PlanSlotKind.COOPERATIVE_RULE) {
                mappings.add(new ChainIndexMapping(mapping.chainIndex(), mapping.techniqueId(), PlanSlotKind.RULE, -1, false));
            } else {
                mappings.add(mapping);
            }
        }
        if (recipe.actionTechniqueId() != null) {
            int actionAmount = actionMana;
            if (prepaid) {
                Integer quotedAction = SkillSegmentMath.segmentManaCost(
                        techniques, new ChainSegment(recipe.actionChainIndex(), List.of()));
                if (quotedAction != null) {
                    actionAmount = quotedAction;
                }
            }
            shares.add(new ShareSlot(ShareRole.ACTION, -1, true, actionAmount));
            cooldowns.add(new QuotedCooldown(recipe.actionTechniqueId(), ShareRole.ACTION, -1, true,
                    prepaid ? 0 : actionCooldown));
        }
        for (int attackCursor = 0; attackCursor < recipe.attacks().size(); attackCursor++) {
            CooperativeCastRecipe.AttackSketch attack = recipe.attacks().get(attackCursor);
            Technique technique = techniques.get(attack.chainIndex());
            AdapterVolumeSpec volume = volumeOf(attack.techniqueId());
            ConfiguredDamage damage = BalanceService.damageContribution(
                    BalanceSide.SERVER, attack.techniqueId(), technique.getBaseDamage());
            ReleaseInput chainInput = SkillChainPlan.frozenChainInput(skill, techniques, attack.chainIndex());
            EnchantmentSnapshot snapshot = EnchantmentSnapshot.from(
                    skill, new ChainSegment(attack.chainIndex(), attack.enchantIndexes()), player);
            HitContinueLink continueLink = continueLink(attack, volume);
            SettlementEnvelope envelope = new SettlementEnvelope(
                    snapshot, chainInput, domainLink, continueLink, carrier, attack.enchantIndexes(), attack.modifierIndexes());
            AttackPreparation preparation = new AttackPreparation(
                    attack.techniqueId(), attack.chainIndex(), damage, volume, weaponSnapshot, frozen.aim(),
                    envelope, cancelToken.tokenId());
            Optional<ActorAttackAdapter> adapter = ActorAttackAdapterRegistry.find(attack.techniqueId());
            if (adapter.isEmpty()) {
                return CooperativeCastBuild.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
            }
            PreparedAttack prepared = adapter.get().prepare(preparation);
            members.add(new AttackMemberPlan(
                    attack.chainIndex(), attack.memberOrdinal(), attack.techniqueId(), prepared, attack.copyCount(),
                    spawnCount, attack.delayTicks(), attack.stagger(), continueLink));
            int settlementOrdinal = 0;
            for (int copyOrdinal = 0; copyOrdinal < attack.copyCount(); copyOrdinal++) {
                for (int spawnOrdinal = 0; spawnOrdinal < spawnCount; spawnOrdinal++) {
                    spawns.add(new LogicalSpawnIndex(attack.memberOrdinal(), copyOrdinal, spawnOrdinal, settlementOrdinal));
                    settlementOrdinal += 1;
                }
            }
            int attackAmount = attackMana.get(attackCursor);
            if (prepaid) {
                Integer quotedAttack = SkillSegmentMath.segmentManaCost(
                        techniques, new ChainSegment(attack.chainIndex(), attack.enchantIndexes()));
                int unitAmount = quotedAttack == null ? attackAmount : quotedAttack;
                try {
                    attackAmount = Math.multiplyExact(unitAmount, Math.max(1, attack.copyCount()));
                } catch (ArithmeticException overflow) {
                    return CooperativeCastBuild.refused(BUDGET_KEY);
                }
            }
            shares.add(new ShareSlot(ShareRole.ATTACK_MEMBER, attack.memberOrdinal(), false, attackAmount));
            cooldowns.add(new QuotedCooldown(attack.techniqueId(), ShareRole.ATTACK_MEMBER, attack.memberOrdinal(), false,
                    prepaid ? 0 : attackCooldown.get(attackCursor)));
        }
        CooperationOrder cooperation = null;
        if (keepCooperation) {
            double selectionRadius = BalanceService.number(BalanceSide.SERVER, CooperativeTechniqueIds.COOPERATIVE, CooperativeContent.SELECTION_RADIUS);
            double moveLimit = BalanceService.number(BalanceSide.SERVER, CooperativeTechniqueIds.COOPERATIVE, CooperativeContent.MOVE_LIMIT);
            double sideOffset = BalanceService.number(BalanceSide.SERVER, CooperativeTechniqueIds.COOPERATIVE, CooperativeContent.SIDE_OFFSET);
            CooperationSettings settings = new CooperationSettings(
                    selectionRadius, moveLimit, sideOffset, CooperativeStructureLimits.PREPARE_TICK_CAP);
            long expiry = player.serverLevel().getServer().getTickCount() + CooperativeStructureLimits.UNCOMMITTED_RESERVATION_TICKS;
            cooperation = new CooperationOrder(settings, expiry);
            shares.add(new ShareSlot(ShareRole.COOPERATION, -1, true, cooperationMana));
            cooldowns.add(new QuotedCooldown(CooperativeTechniqueIds.COOPERATIVE, ShareRole.COOPERATION, -1, true,
                    prepaid ? 0 : cooperationCooldown));
        }
        if (cooldowns.isEmpty()) {
            return CooperativeCastBuild.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
        }
        ReleaseGate gate = gateOf(recipe);
        int familyOrdinal = CooperativeRuntime.service().nextFamilyOrdinal(player.getServer());
        FamilyKey family = new FamilyKey(
                CooperativeRuntime.sessions().sessionOf(player.getServer()),
                player.level().dimension(),
                player.getUUID(),
                UUID.randomUUID(),
                player.getServer().getTickCount(),
                familyOrdinal);
        ExecutionFamilyPlan plan = new ExecutionFamilyPlan(
                family, mappings, carrier, gate, cooperation, members, spawns,
                new PlanShareSchedule(shares, keepCooperation), cooldowns, cancelToken, completionToken,
                frozen.aim(), frozen.yaw(), frozen.fallback());
        int quotedCooldown = 0;
        if (!prepaid && gate.kind() == ReleaseGateKind.IMMEDIATE) {
            for (QuotedCooldown piece : cooldowns) {
                quotedCooldown += piece.cooldownTicks();
            }
        }
        List<Technique> fired = new ArrayList<>();
        if (recipe.actionChainIndex() >= 0) {
            fired.add(techniques.get(recipe.actionChainIndex()));
        }
        for (CooperativeCastRecipe.AttackSketch attack : recipe.attacks()) {
            fired.add(techniques.get(attack.chainIndex()));
        }
        return CooperativeCastBuild.ready(plan, recipe, gate.kind() != ReleaseGateKind.IMMEDIATE, quotedCooldown, fired);
    }

    private static int readMana(Technique technique) {
        return BalanceService.manaCost(BalanceSide.SERVER, technique.getId(), BalanceKeys.MANA_COST, technique.manaCost());
    }

    private static int readCooldown(Technique technique) {
        return BalanceService.cooldownTicks(BalanceSide.SERVER, technique.getId(), technique.getBaseCooldown());
    }

    private static int scaleCooldown(int ticks, double cooldownReduction) {
        if (ticks <= 0) {
            return 0;
        }
        return SkillSegmentMath.scaleCooldown(ticks, cooldownReduction);
    }

    private static AdapterVolumeSpec volumeOf(String techniqueId) {
        if (CooperativeTechniqueIds.HORIZONTAL_SLASH.equals(techniqueId)) {
            return HorizontalSlashActorAdapter.serverVolume();
        }
        return ObliqueSlashEffect.serverVolume();
    }

    private static @Nullable HitContinueLink continueLink(CooperativeCastRecipe.AttackSketch attack, AdapterVolumeSpec volume) {
        if (attack.continueParentOrdinal() < 0) {
            return null;
        }
        return new HitContinueLink(
                attack.continueParentOrdinal(), attack.chainIndex(), attack.copyCount(), attack.entityLock(),
                false, attack.upwellPath(), attack.stagger(), volume.lifetimeTicks(), Math.max(volume.boxDepth(), 1.0D),
                UUID.randomUUID());
    }

    private static ReleaseGate gateOf(CooperativeCastRecipe recipe) {
        if (recipe.gateKind() == ReleaseGateKind.TIMED) {
            return new ReleaseGate(ReleaseGateKind.TIMED, SpinTimelineDefinition.serverTimeline());
        }
        if (recipe.gateKind() == ReleaseGateKind.CONTACT) {
            return new ReleaseGate(ReleaseGateKind.CONTACT, null);
        }
        return ObliqueSlashEffect.immediateGate();
    }

    private static Aim freezeAim(ServerPlayer player, @Nullable Vec3 aimOverride) {
        Vec3 look = aimOverride == null ? player.getLookAngle() : aimOverride;
        float yaw = player.getYRot();
        double horizontal = look.x * look.x + look.z * look.z;
        if (horizontal < 1.0E-8D) {
            double radians = Math.toRadians(yaw);
            return new Aim(new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians)), yaw, true);
        }
        return new Aim(look.normalize(), yaw, false);
    }

    private record Aim(Vec3 aim, float yaw, boolean fallback) {
    }

    private static final class WalkState {
        private final List<ChainIndexMapping> mappings = new ArrayList<>();
        private final List<CooperativeCastRecipe.AttackSketch> attacks = new ArrayList<>();
        private final List<Integer> leapIndexes = new ArrayList<>();
        private List<Integer> pendingEnchants = new ArrayList<>();
        private List<Integer> pendingModifiers = new ArrayList<>();
        private boolean spaceLock;
        private boolean scatter;
        private boolean dropPath;
        private boolean entityLock;
        private boolean upwellPath;
        private boolean staggerPending;
        private int copyCount = 1;
        private int continueParent = -1;
        private int timedIndex = -1;
        private int contactIndex = -1;
        private String actionTechniqueId;
        private int cooperativeRules;
        private boolean sawStandalone;
        private int memberOrdinal;
    }
}
