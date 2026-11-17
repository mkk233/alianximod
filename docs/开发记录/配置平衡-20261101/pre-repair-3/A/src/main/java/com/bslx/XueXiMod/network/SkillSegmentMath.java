package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.balance.BalanceDisplay;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffect;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

final class SkillSegmentMath {
    /** 装配链是否含「操纵世界剑实体」类术式（由 ReleaseDefinition 登记，不写死 id）。 */
    static boolean hasSwordManipulation(List<Technique> techniques) {
        for (Technique technique : techniques) {
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.manipulatesSwordEntities()) {
                return true;
            }
        }
        return false;
    }

    static boolean isSelfStatus(Technique technique) {
        if (technique == null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.kind() == ReleaseKind.SELF_STATUS;
    }

    /** 混链被拒绝时的原因。没有拒绝时返回空。 */
    static String mixedChainRefusal(List<Technique> techniques) {
        if (!refusesMixedChain(techniques)) {
            return null;
        }
        for (Technique technique : techniques) {
            if (isSelfStatus(technique)) {
                return "自身状态不能和其他术式装配";
            }
        }
            return "操纵术式不能和其他术式装配";
    }

    static boolean refusesMixedChain(List<Technique> techniques) {
        int effectiveCount = 0;
        boolean refuses = false;
        for (Technique technique : techniques) {
            if (technique == null || isRemovedRule(technique.getId())) continue;
            effectiveCount++;
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            refuses |= definition != null && (definition.manipulatesSwordEntities() || definition.refusesMixedChain());
        }
        return effectiveCount > 1 && refuses;
    }

    static boolean hasPlantedWorldSword(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        return SwordEntity.anyOwnedBy(level, player.getUUID());
    }

    /** 空手可放：链上有操纵类术式，且维度里存在该玩家拥有的存活剑实体。 */
    static boolean allowsEmptyHandSwordCast(ServerPlayer player, Skill skill) {
        return skill != null
                && hasSwordManipulation(skill.getTechniques())
                && hasPlantedWorldSword(player);
    }

    static ChainRole roleOf(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return ChainRole.ATTACK;
        }
        return definition.role();
    }

    static boolean isChainModifier(ChainRole role) {
        return role == ChainRole.SEEK_SPACE
                || role == ChainRole.SEEK_ENTITY
                || role == ChainRole.SEEK_SURFACE
                || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_DROP
                || role == ChainRole.TRAJECTORY_UPWELL
                || role == ChainRole.TRAJECTORY_SCATTER
                || role == ChainRole.RULE_TRIPLE
                || role == ChainRole.RULE_DUAL
                || role == ChainRole.RULE_STAGGER
                || role == ChainRole.RULE_HIT_CONTINUE;
    }

    static boolean isChainModifier(Technique technique) {
        if (technique == null) {
            return false;
        }
        if (isChainModifier(roleOf(technique))) {
            return true;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return false;
        }
        float supplied = definition.chainEnergyScale();
        return Float.isFinite(supplied) && Math.abs(supplied - 1f) > 0.001f;
    }

    static boolean isRemovedRule(String techniqueId) {
        return "rule_interleave".equals(techniqueId) || "rule_damage_tally".equals(techniqueId);
    }

    static int baseCooldownSum(List<ChainSegment> segments, List<Technique> techniques) {
        int sum = 0;
        for (ChainSegment segment : segments) {
            sum += configuredBaseCooldown(techniques.get(segment.actionIndex()));
        }
        return sum;
    }

    /** 配置后的基础冷却。法杖和反应缩放仍由调用方在这之后做一次。 */
    static int configuredBaseCooldown(Technique technique) {
        if (technique == null) {
            return 0;
        }
        return BalanceDisplay.cooldown(technique.getId(), technique.getBaseCooldown());
    }

    static float segmentDamage(Technique action, ChainSegment segment, float weaponBonus, int magicDamage, Double attributeDamageBonus) {
        float base = scaledBaseDamage(action.getBaseDamage(), attributeDamageBonus);
        return action.getBaseDamage() > 0
                ? base + weaponBonus + magicDamage
                : base + weaponBonus;
    }

    static float scaledBaseDamage(float baseDamage, Double attributeDamageBonus) {
        if (attributeDamageBonus == null || baseDamage <= 0) return baseDamage;
        return (float) Math.max(0, baseDamage * (1 + attributeDamageBonus));
    }

    static int scaleCooldown(int ticks, double cooldownReduction) {
        if (ticks <= 0) return ticks;
        return Math.max(1, (int) Math.round(ticks * (1 - cooldownReduction)));
    }

    static ReleaseKind releaseKind(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null ? definition.kind() : null;
    }

    static boolean isTriggerRelease(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null && definition.tag() == ReleaseTag.TRIGGER;
    }

    static boolean manipulatesSwordEntities(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null && definition.manipulatesSwordEntities();
    }

    static EnchantmentResolution resolveSegmentEnchantments(Skill skill, ChainSegment segment) {
        EnchantmentResolution resolution = EnchantmentResolution.empty();
        List<Technique> techniques = skill.getTechniques();
        for (int index : segment.enchantIndexes()) {
            Technique technique = techniques.get(index);
            EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
            if (definition == null) {
                DebugLog.log("附魔未登记：" + technique.getId());
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
            DebugLog.log("附魔 " + technique.getId() + " 原胚=" + skill.getEmbryo(index).getId()
                    + " " + reaction.describe());
            EnchantmentAccumulator current = resolution.effect(definition.createAccumulator().effectId());
            resolution = resolution.with(definition.fold(reaction, current));
        }
        return resolution;
    }

    static int segmentMagicDamage(Skill skill, ChainSegment segment) {
        int total = 0;
        List<Technique> techniques = skill.getTechniques();
        for (int index : segment.enchantIndexes()) {
            Technique technique = techniques.get(index);
            EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
            if (definition == null) {
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
            total += definition.magicDamage(reaction);
        }
        return total;
    }

    static int segmentManaCost(List<Technique> techniques, ChainSegment segment) {
        int cost = quotedMana(techniques.get(segment.actionIndex()));
        return cost + enchantOnlyCost(techniques, segment);
    }

    /** 单价大于 0 的条件段，预扣只留附魔一次。固定价术式仍是整段费用。 */
    static int settledSegmentCost(List<Technique> techniques, ChainSegment segment) {
        if (unitManaCost(techniques.get(segment.actionIndex())) > 0) {
            return enchantOnlyCost(techniques, segment);
        }
        return segmentManaCost(techniques, segment);
    }

    static boolean isConditionalRelease(Technique technique) {
        if (technique == null || technique.getBaseDamage() > 0f) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.conditionalEffect() != null;
    }

    static int unitManaCost(Technique technique) {
        if (technique == null) {
            return 0;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        ConditionalEffect effect = definition == null ? null : definition.conditionalEffect();
        if (effect == null) {
            return 0;
        }
        int unitCost = effect.unitManaCost();
        return unitCost > 0 ? unitCost : 0;
    }

    static int enchantOnlyCost(List<Technique> techniques, ChainSegment segment) {
        int enchantCost = 0;
        for (int enchantIndex : segment.enchantIndexes()) {
            enchantCost += quotedMana(techniques.get(enchantIndex));
        }
        return enchantCost;
    }

    /**
     * 原始单价为 0 仍读取标准 manaCost。默认仍是 0，显式配成正数后沿用原来的这一次报价。
     * 领域、屏障、护环的真实费用由另一单价承担，宿主占位 0 不读，避免和本体费用叠成两笔。
     */
    static int quotedMana(Technique technique) {
        if (technique == null) {
            return 0;
        }
        int rawQuote = manaCostOf(technique);
        if (rawQuote <= 0 && separateBodyQuote(technique)) {
            return 0;
        }
        return BalanceDisplay.mana(technique.getId(), BalanceKeys.MANA_COST, rawQuote);
    }

    /** 定义上另有开域费或生成费，且宿主基础伤害不是正数。不按术式编号区分。 */
    private static boolean separateBodyQuote(Technique technique) {
        if (technique.getBaseDamage() > 0f) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return false;
        }
        return definition.domain() != null || definition.barrier() != null || definition.guard() != null;
    }

    /**
     * 领域、水幕、水环的本体费用。返回 null 表示这段不是延后本体。
     * 领域读 manaCost，水幕和水环读 spawnManaCost，单价先缩放再由调用方乘份数。
     */
    static Integer configuredBodyUnit(Technique technique, ReleaseDefinition definition) {
        if (technique == null || definition == null || technique.getBaseDamage() > 0f) {
            return null;
        }
        if (definition.domain() != null) {
            return BalanceDisplay.mana(technique.getId(), BalanceKeys.MANA_COST, definition.domain().initialCost());
        }
        if (definition.barrier() != null) {
            return BalanceDisplay.mana(technique.getId(), BalanceKeys.SPAWN_MANA_COST, definition.barrier().spawnCost());
        }
        if (definition.guard() != null) {
            return BalanceDisplay.mana(technique.getId(), BalanceKeys.SPAWN_MANA_COST, definition.guard().spawnCost());
        }
        return null;
    }

    /**
     * 无附魔时把法杖调整后的宿主金额缩放一次。
     * 有附魔时宿主加武器、共享附加、潮印各按自己的来源缩放一次，不再把合计再乘一遍。
     */
    static float configuredSegmentDamage(Technique action, ChainSegment segment, float weaponBonus,
                                          int magicDamage, Double attributeDamageBonus, EnchantBonus bonus) {
        if (action == null || segment == null || segment.enchantIndexes().isEmpty() || bonus == null) {
            float rawAmount = segmentDamage(action, segment, weaponBonus, magicDamage, attributeDamageBonus);
            return BalanceDisplay.damage(action == null ? "" : action.getId(), rawAmount);
        }
        float total = BalanceDisplay.damage(action.getId(), bonus.rawHostAttack() + weaponBonus);
        float addedRaw = bonus.rawAddedDamage();
        String addedSource = bonus.addedSourceId();
        if (addedSource != null && !addedSource.isEmpty() && addedRaw != 0f) {
            total += BalanceDisplay.damage(addedSource, addedRaw);
        }
        float tideRaw = bonus.rawTideDamage();
        if (tideRaw != 0f) {
            total += BalanceDisplay.damage("tide_mark", tideRaw);
        }
        return total;
    }

    static int manaCostOf(Technique technique) {
        if (technique == null) {
            return 0;
        }
        int explicitPrice = com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantCatalog
                .explicitManaPrice(technique.getId());
        if (explicitPrice >= 0) {
            return explicitPrice;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition != null && definition.explicitManaCost() >= 0) {
            return definition.explicitManaCost();
        }
        return technique.manaCost();
    }

    /**
     * 两个原始宿主是否属于同一次多重。窗口来自 {@link ChainSteps#hostsShareExpansion}，不再读 {@code pullAfterA}。
     * 多重成员自己的增幅以 {@link MemberCastView} 为准；这条判断仍会被旧的链能量扫描用来跨过释放边界。
     */
    static boolean sharesDualRelease(List<Technique> techniques, int leftIndex, int rightIndex) {
        return ChainSteps.hostsShareExpansion(techniques, leftIndex, rightIndex);
    }
}
