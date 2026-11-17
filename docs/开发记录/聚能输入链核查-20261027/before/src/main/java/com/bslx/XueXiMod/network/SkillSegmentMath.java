package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
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
                || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_DROP
                || role == ChainRole.TRAJECTORY_SCATTER
                || role == ChainRole.RULE_TRIPLE
                || role == ChainRole.RULE_DUAL;
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
            sum += techniques.get(segment.actionIndex()).getBaseCooldown();
        }
        return sum;
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
        int cost = manaCostOf(techniques.get(segment.actionIndex()));
        for (int index : segment.enchantIndexes()) {
            cost += manaCostOf(techniques.get(index));
        }
        return cost;
    }

    private static int manaCostOf(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition != null && definition.explicitManaCost() >= 0) {
            return definition.explicitManaCost();
        }
        return technique.manaCost();
    }

    static boolean sharesDualRelease(List<Technique> techniques, int leftIndex, int rightIndex) {
        List<Integer> playable = new ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            if (!techniques.get(techniqueIndex).isEnchantment()) {
                playable.add(techniqueIndex);
            }
        }
        for (int playableIndex = 0; playableIndex < playable.size(); playableIndex++) {
            int techniqueIndex = playable.get(playableIndex);
            ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(techniqueIndex).getId());
            if (definition == null || definition.role() != ChainRole.RULE_DUAL) {
                continue;
            }
            int pull = definition.pullAfterA();
            int available = Math.max(0, playable.size() - (playableIndex + 2));
            int follow = pull < 0 ? available : Math.min(pull, available);
            int lastPlayable = playableIndex + 1 + follow;
            boolean leftFound = false;
            boolean rightFound = false;
            for (int member = playableIndex + 1; member <= lastPlayable && member < playable.size(); member++) {
                int memberIndex = playable.get(member);
                if (memberIndex == leftIndex) {
                    leftFound = true;
                }
                if (memberIndex == rightIndex) {
                    rightFound = true;
                }
            }
            if (leftFound && rightFound) {
                return true;
            }
        }
        return false;
    }
}
