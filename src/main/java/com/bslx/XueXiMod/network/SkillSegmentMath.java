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
import com.bslx.XueXiMod.shu.technique.release.ChainCompatibility;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordFlightEffect;
import com.bslx.XueXiMod.shu.technique.sword.SwordEntity;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 链段使用的服务端数值和资格判断。输入来自已重编的技能或释放定义，不读取客户端报价。
 * 费用放不进 int 时返回空，由调用方拒绝；这里不扣费、不排队，也不提示玩家。
 */
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

    /** 只有声明了可执行自身状态事务的定义才走状态提交。种类或窗口角色不能代替这个声明。 */
    static boolean commitsSelfState(Technique technique) {
        if (technique == null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null
                && definition.chainCompatibility() == ChainCompatibility.SELF_STATE
                && definition.selfStatus() != null;
    }

    /** 混链被拒绝时的原因。没有拒绝时返回空。 */
    static String mixedChainRefusal(List<Technique> techniques) {
        if (!refusesMixedChain(techniques)) {
            return null;
        }
        ChainCompatibility exclusive = exclusiveCompatibility(techniques);
        if (exclusive == ChainCompatibility.SELF_STATE) {
            return "自身状态不能和其他术式装配";
        }
        if (exclusive == ChainCompatibility.MANIPULATION) {
            return "操纵术式不能和其他术式装配";
        }
        if (exclusive == ChainCompatibility.SPELL_WINDOW) {
            return "术法窗口不能和其他术式装配";
        }
        return "这些术式不能装配在一起";
    }

    /**
     * 过滤已移除规则后，出现显式排他类型时，其余有效术式必须属于同一类型。
     * 普通组合保持原样，不因自身状态种类、窗口角色或空手标志被当成排他。
     */
    static boolean refusesMixedChain(List<Technique> techniques) {
        if (techniques == null) {
            return false;
        }
        ChainCompatibility required = null;
        for (Technique technique : techniques) {
            if (technique == null || isRemovedRule(technique.getId())) continue;
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            ChainCompatibility compatibility = definition == null
                    ? ChainCompatibility.ORDINARY : definition.chainCompatibility();
            if (compatibility == null || compatibility == ChainCompatibility.ORDINARY) {
                if (required != null) {
                    return true;
                }
                continue;
            }
            if (required == null) {
                required = compatibility;
                continue;
            }
            if (required != compatibility) {
                return true;
            }
        }
        return false;
    }

    /** 这条原始链里第一个显式排他类型。没有时返回空。 */
    private static ChainCompatibility exclusiveCompatibility(List<Technique> techniques) {
        if (techniques == null) {
            return null;
        }
        for (Technique technique : techniques) {
            if (technique == null || isRemovedRule(technique.getId())) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            ChainCompatibility compatibility = definition == null
                    ? ChainCompatibility.ORDINARY : definition.chainCompatibility();
            if (compatibility != null && compatibility != ChainCompatibility.ORDINARY) {
                return compatibility;
            }
        }
        return null;
    }

    /** 当前服务端维度是否有这名玩家拥有的存活剑实体。非服务端维度时没有。 */
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
                && (hasPlantedWorldSword(player)
                || SwordFlightEffect.serverAllowsInventorySource(skill.getTechniques(), player));
    }

    /** 读取术式登记的链角色。没有释放定义时按普通攻击处理。 */
    static ChainRole roleOf(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return ChainRole.ATTACK;
        }
        return definition.role();
    }

    /** 判断角色是否属于索敌、弹道或规则修饰，而不是最终攻击。 */
    static boolean isChainModifier(ChainRole role) {
        return role == ChainRole.SEEK_SPACE
                || role == ChainRole.SEEK_ENTITY
                || role == ChainRole.SEEK_SURFACE
                || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_DROP
                || role == ChainRole.TRAJECTORY_PARALLEL
                || role == ChainRole.TRAJECTORY_UPWELL
                || role == ChainRole.TRAJECTORY_SCATTER
                || role == ChainRole.RULE_TRIPLE
                || role == ChainRole.RULE_DUAL
                || role == ChainRole.RULE_STAGGER
                || role == ChainRole.RULE_HIT_CONTINUE
                || role == ChainRole.RULE_COOPERATIVE;
    }

    /** 事件响应和弹道操纵是可执行宿主，不归入弹道修饰白名单。 */
    static boolean isOperationHost(ChainRole role) {
        return role == ChainRole.EVENT_RESPONSE || role == ChainRole.TRAJECTORY_MANIPULATION;
    }

    /** 按登记角色判断操作宿主。没有术式或释放定义时不是。 */
    static boolean isOperationHost(Technique technique) {
        return technique != null && isOperationHost(roleOf(technique));
    }

    /**
     * 操作宿主的链上报价。御剑的启动法力由效果当场收取，这里记 0，避免和链上预扣叠成两笔。
     * 其余操作宿主仍报标准法力。
     */
    static Integer operationQuote(List<Technique> techniques, ChainSegment segment) {
        Technique host = techniques.get(segment.actionIndex());
        if (host != null && SwordFlightEffect.TECHNIQUE_ID.equals(host.getId())) {
            return enchantOnlyCost(techniques, segment);
        }
        return segmentManaCost(techniques, segment);
    }

    /** 判断链修饰。角色属于修饰，或定义提供了不是 1 的有限链能量倍率时都算。 */
    static boolean isChainModifier(Technique technique) {
        if (technique == null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition != null && (definition.domain() != null || definition.explicitDomainHost())) {
            return false;
        }
        if (isChainModifier(roleOf(technique))) {
            return true;
        }
        if (definition == null) {
            return false;
        }
        float supplied = definition.chainEnergyScale();
        return Float.isFinite(supplied) && Math.abs(supplied - 1f) > 0.001f;
    }

    /** 判断已经从可装配列表移除、只为旧链识别而保留的两条规则编号。 */
    static boolean isRemovedRule(String techniqueId) {
        return "rule_interleave".equals(techniqueId) || "rule_damage_tally".equals(techniqueId);
    }

    /** 合计每个分段宿主配置后的基础冷却，不再做法杖或反应缩放。 */
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

    /** 合成一段伤害。正基础伤害才加入魔法伤害；非正基础伤害只保留缩放后的基础值和武器加成。 */
    static float segmentDamage(Technique action, ChainSegment segment, float weaponBonus, int magicDamage, Double attributeDamageBonus) {
        float base = scaledBaseDamage(action.getBaseDamage(), attributeDamageBonus);
        return action.getBaseDamage() > 0
                ? base + weaponBonus + magicDamage
                : base + weaponBonus;
    }

    /** 按属性伤害加成缩放正基础伤害。没有加成或基础伤害非正时保持原值。 */
    static float scaledBaseDamage(float baseDamage, Double attributeDamageBonus) {
        if (attributeDamageBonus == null || baseDamage <= 0) return baseDamage;
        return (float) Math.max(0, baseDamage * (1 + attributeDamageBonus));
    }

    /** 按冷却缩减缩放 tick。非正输入保持原值，正输入至少保留 1 tick。 */
    static int scaleCooldown(int ticks, double cooldownReduction) {
        if (ticks <= 0) return ticks;
        return Math.max(1, (int) Math.round(ticks * (1 - cooldownReduction)));
    }

    /** 读取释放种类。没有定义时返回空。 */
    static ReleaseKind releaseKind(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null ? definition.kind() : null;
    }

    /** 判断定义标签是否为命中触发。没有定义时不是。 */
    static boolean isTriggerRelease(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null && definition.tag() == ReleaseTag.TRIGGER;
    }

    /** 判断定义是否操纵剑实体。没有定义时不是。 */
    static boolean manipulatesSwordEntities(String techniqueId) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniqueId);
        return definition != null && definition.manipulatesSwordEntities();
    }

    /** 按分段附魔和原胚折成一份附魔结果。未登记附魔只记录并跳过，不中断整段。 */
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

    /** 合计分段中已登记附魔的魔法伤害。未登记附魔跳过。 */
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

    /** 合计宿主和附魔报价。任一部分溢出时返回空。 */
    static Integer segmentManaCost(List<Technique> techniques, ChainSegment segment) {
        Integer enchantCost = enchantOnlyCost(techniques, segment);
        if (enchantCost == null) {
            return null;
        }
        return exactAdd(quotedMana(techniques.get(segment.actionIndex())), enchantCost);
    }

    /** 单价大于 0 的条件段，预扣只留附魔一次。固定价术式仍是整段费用。 */
    static Integer settledSegmentCost(List<Technique> techniques, ChainSegment segment) {
        Technique host = techniques.get(segment.actionIndex());
        ReleaseDefinition definition = host == null ? null : ReleaseRegistry.get(host.getId());
        if (host != null && host.getBaseDamage() <= 0f && definition != null && definition.domain() != null) {
            return quotedMana(host);
        }
        if (unitManaCost(techniques.get(segment.actionIndex())) > 0) {
            return enchantOnlyCost(techniques, segment);
        }
        return segmentManaCost(techniques, segment);
    }

    /** 判断零基础伤害且定义了条件效果的术式。正伤害不是条件释放。 */
    static boolean isConditionalRelease(Technique technique) {
        if (technique == null || technique.getBaseDamage() > 0f) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.conditionalEffect() != null;
    }

    /** 读取条件效果的正单位魔力。没有条件效果或单位费用非正时返回 0。 */
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

    /** 只合计分段附魔报价。任一附魔报价溢出时返回空。 */
    static Integer enchantOnlyCost(List<Technique> techniques, ChainSegment segment) {
        Integer enchantCost = 0;
        for (int enchantIndex : segment.enchantIndexes()) {
            enchantCost = exactAdd(enchantCost, quotedMana(techniques.get(enchantIndex)));
            if (enchantCost == null) {
                return null;
            }
        }
        return enchantCost;
    }

    /** 与批量编译相同：超出 int 或得到负数就拒绝，不把回绕结果交给魔力池。 */
    static Integer exactAdd(int left, int right) {
        try {
            int summed = Math.addExact(left, right);
            if (summed < 0) {
                return null;
            }
            return summed;
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    /** 精确相乘。溢出或结果为负时返回空，不把回绕值交给费用计算。 */
    static Integer exactMultiply(int left, int right) {
        try {
            int product = Math.multiplyExact(left, right);
            if (product < 0) {
                return null;
            }
            return product;
        } catch (ArithmeticException overflow) {
            return null;
        }
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

    /** 读取未缩放的原始魔力报价，依次尝试显式资源价、显式释放价和术式默认价。 */
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
