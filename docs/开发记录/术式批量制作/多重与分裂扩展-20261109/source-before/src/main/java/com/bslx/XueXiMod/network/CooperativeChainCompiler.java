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
        return walk(techniques);
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

    private static CooperativeCastRecipe walk(List<Technique> techniques) {
        List<ChainIndexMapping> mappings = new ArrayList<>();
        List<CooperativeCastRecipe.AttackSketch> attacks = new ArrayList<>();
        List<Integer> leapIndexes = new ArrayList<>();
        List<Integer> pendingEnchants = new ArrayList<>();
        List<Integer> pendingModifiers = new ArrayList<>();
        boolean spaceLock = false;
        boolean scatter = false;
        boolean dropPath = false;
        boolean entityLock = false;
        boolean upwellPath = false;
        boolean staggerPending = false;
        boolean dualPending = false;
        int dualAttacks = 0;
        int copyCount = 1;
        int continueParent = -1;
        int timedIndex = -1;
        int contactIndex = -1;
        String actionTechniqueId = null;
        int cooperativeRules = 0;
        boolean sawStandalone = false;
        int memberOrdinal = 0;
        for (int chainIndex = 0; chainIndex < techniques.size(); chainIndex++) {
            Technique technique = techniques.get(chainIndex);
            if (technique == null) {
                return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
            }
            String techniqueId = technique.getId();
            TechniqueExecutionCapabilities capabilities = CooperativeCapabilityIndex.find(techniqueId);
            ChainRole role = SkillSegmentMath.roleOf(technique);
            if (technique.isEnchantment()) {
                pendingEnchants.add(chainIndex);
                mappings.add(new ChainIndexMapping(chainIndex, techniqueId, PlanSlotKind.ENCHANT, -1, false));
                continue;
            }
            if (capabilities.isStandaloneDeployment()) {
                sawStandalone = true;
                mappings.add(new ChainIndexMapping(chainIndex, techniqueId, PlanSlotKind.OTHER, -1, false));
                continue;
            }
            if (capabilities.isCooperativeRule() || role == ChainRole.RULE_COOPERATIVE) {
                cooperativeRules += 1;
                if (cooperativeRules > 1) {
                    return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
                }
                mappings.add(new ChainIndexMapping(chainIndex, techniqueId, PlanSlotKind.COOPERATIVE_RULE, -1, false));
                continue;
            }
            if (capabilities.isTimedRelease()) {
                if (timedIndex >= 0 || contactIndex >= 0) {
                    return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
                }
                timedIndex = chainIndex;
                actionTechniqueId = techniqueId;
                mappings.add(new ChainIndexMapping(chainIndex, techniqueId, PlanSlotKind.TIMED_ACTION, -1, false));
                continue;
            }
            if (SkillSegmentMath.isChainModifier(technique)) {
                if (role == ChainRole.RULE_TRIPLE) {
                    copyCount = 3;
                } else if (role == ChainRole.RULE_DUAL) {
                    dualPending = true;
                } else if (role == ChainRole.RULE_STAGGER) {
                    staggerPending = true;
                } else if (role == ChainRole.RULE_HIT_CONTINUE) {
                    if (attacks.isEmpty()) {
                        return CooperativeCastRecipe.refused("gui.xueximod.batch.missing_parent");
                    }
                    continueParent = attacks.get(attacks.size() - 1).chainIndex();
                } else if (role == ChainRole.SEEK_SPACE || role == ChainRole.SEEK_SURFACE) {
                    spaceLock = true;
                } else if (role == ChainRole.TRAJECTORY_SCATTER) {
                    scatter = true;
                } else if (role == ChainRole.TRAJECTORY_DROP) {
                    dropPath = true;
                } else if (role == ChainRole.SEEK_ENTITY) {
                    entityLock = true;
                } else if (role == ChainRole.TRAJECTORY_UPWELL) {
                    upwellPath = true;
                }
                pendingModifiers.add(chainIndex);
                PlanSlotKind slotKind = seekSlot(role);
                mappings.add(new ChainIndexMapping(chainIndex, techniqueId, slotKind, -1, false));
                continue;
            }
            if (DASH_ID.equals(techniqueId) && role == ChainRole.ACTION) {
                if (timedIndex >= 0 || contactIndex >= 0) {
                    return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
                }
                contactIndex = chainIndex;
                actionTechniqueId = techniqueId;
                mappings.add(new ChainIndexMapping(chainIndex, techniqueId, PlanSlotKind.CONTACT_ACTION, -1, false));
                continue;
            }
            if (LEAP_ID.equals(techniqueId)) {
                leapIndexes.add(chainIndex);
                mappings.add(new ChainIndexMapping(chainIndex, techniqueId, PlanSlotKind.OTHER, -1, false));
                continue;
            }
            if (role == ChainRole.ATTACK || capabilities.isExplicitAttackAdapter()
                    || ActorAttackAdapterRegistry.supports(techniqueId)) {
                if (!ActorAttackAdapterRegistry.supports(techniqueId)) {
                    return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
                }
                int delayTicks = 0;
                if (dualPending) {
                    dualAttacks += 1;
                    if (dualAttacks == 2) {
                        delayTicks = BatchReleasePlan.MEMBER_GAP;
                    }
                }
                attacks.add(new CooperativeCastRecipe.AttackSketch(
                        chainIndex, memberOrdinal, techniqueId, Math.max(1, copyCount), delayTicks, staggerPending,
                        continueParent, pendingEnchants, pendingModifiers, entityLock, upwellPath));
                mappings.add(new ChainIndexMapping(chainIndex, techniqueId, PlanSlotKind.IMMEDIATE_ATTACK, memberOrdinal, true));
                memberOrdinal += 1;
                pendingEnchants = new ArrayList<>();
                pendingModifiers = new ArrayList<>();
                copyCount = 1;
                staggerPending = false;
                continueParent = -1;
                entityLock = false;
                upwellPath = false;
                continue;
            }
            return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
        }
        if (sawStandalone) {
            return CooperativeCastRecipe.refused(CLONE_MIXED_KEY);
        }
        if (dualPending && dualAttacks != 2) {
            return CooperativeCastRecipe.refused("gui.xueximod.batch.dual_incomplete");
        }
        if (continueParent >= 0) {
            return CooperativeCastRecipe.refused("gui.xueximod.batch.missing_child");
        }
        if (cooperativeRules > 0 && attacks.isEmpty()) {
            return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
        }
        if (contactIndex >= 0 && attacks.isEmpty()) {
            return CooperativeCastRecipe.refused(CooperativeTexts.UNADAPTED_ATTACK_KEY);
        }
        ReleaseGateKind gateKind = ReleaseGateKind.IMMEDIATE;
        int actionChainIndex = -1;
        if (timedIndex >= 0) {
            gateKind = ReleaseGateKind.TIMED;
            actionChainIndex = timedIndex;
        } else if (contactIndex >= 0) {
            gateKind = ReleaseGateKind.CONTACT;
            actionChainIndex = contactIndex;
        }
        if (!pendingEnchants.isEmpty() && !attacks.isEmpty()) {
            CooperativeCastRecipe.AttackSketch last = attacks.get(attacks.size() - 1);
            List<Integer> merged = new ArrayList<>(last.enchantIndexes());
            merged.addAll(pendingEnchants);
            attacks.set(attacks.size() - 1, new CooperativeCastRecipe.AttackSketch(
                    last.chainIndex(), last.memberOrdinal(), last.techniqueId(), last.copyCount(), last.delayTicks(),
                    last.stagger(), last.continueParentOrdinal(), merged, last.modifierIndexes(),
                    last.entityLock(), last.upwellPath()));
        }
        return CooperativeCastRecipe.ready(gateKind, actionChainIndex, actionTechniqueId, cooperativeRules > 0,
                spaceLock, scatter, dropPath, attacks, mappings, leapIndexes);
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
}
