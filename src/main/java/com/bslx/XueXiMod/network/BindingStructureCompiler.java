package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.bind.BindingLayout;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastParser;
import com.bslx.XueXiMod.shu.technique.release.multicast.ReplicationSpec;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundMember;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.StructureFault;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把原始装配链收成投影技能和各领域的冻结子计划。输入是服务端已取得的技能，不读取客户端结果。
 * 空链直接就绪；绑定结构无效时返回失败和第一条故障，不修改原技能。不按术式标识特判。
 */
final class BindingStructureCompiler implements BindingLayout.Compiler {
    private static final BindingStructureCompiler INSTANCE = new BindingStructureCompiler();
    private static final String BESIDE_DOMAIN = "领域绑定必须紧接在领域术式之后";
    private static final String GAP_BEFORE_MARK = "领域绑定必须紧接领域术式，中间不能夹附魔或其他术式";
    private static final String MISSING_ATTACK = "领域绑定后面没有可绑定的攻击";
    private static final String MISSING_SECOND = "双重释放后面缺少第二个攻击";
    private static final String REMOVED_RULE = "链里有已移除的术式，请重新装配";

    private BindingStructureCompiler() {
    }

    /** 返回进程内唯一编译器。不创建状态，也不校验技能。 */
    static BindingStructureCompiler compiler() {
        return INSTANCE;
    }

    @Override
    /**
     * 扫描全部领域绑定标记，成功后去掉被捕获的段落并保留原始下标。
     * 任一标记无法收成子计划时立即失败；无标记时投影与原链一致。
     */
    public BindingLayout compile(Skill rawSkill) {
        if (rawSkill == null) {
            return BindingLayout.failed(new Skill("", SkillType.MAGIC, List.of()), List.of(new StructureFault(-1, MISSING_ATTACK)));
        }
        List<Technique> techniques = rawSkill.getTechniques();
        if (techniques == null || techniques.isEmpty()) {
            return BindingLayout.ready(rawSkill, new int[0], Map.of());
        }
        boolean[] captured = new boolean[techniques.size()];
        Map<Integer, BoundReleasePlan> plans = new LinkedHashMap<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            if (captured[techniqueIndex] || !bindMarker(techniques.get(techniqueIndex))) {
                continue;
            }
            StructureFault fault = captureMark(rawSkill, techniques, techniqueIndex, captured, plans);
            if (fault != null) {
                return BindingLayout.failed(rawSkill, List.of(fault));
            }
        }
        List<Technique> keptTechniques = new ArrayList<>();
        List<ShuAttribute> keptEmbryos = new ArrayList<>();
        int[] originalIndexes = new int[techniques.size() - countCaptured(captured)];
        int projectedIndex = 0;
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            if (captured[techniqueIndex]) {
                continue;
            }
            keptTechniques.add(techniques.get(techniqueIndex));
            keptEmbryos.add(rawSkill.getEmbryo(techniqueIndex));
            originalIndexes[projectedIndex] = techniqueIndex;
            projectedIndex++;
        }
        Skill projected = new Skill(rawSkill.getName(), rawSkill.getType(), keptTechniques, keptEmbryos);
        return BindingLayout.ready(projected, originalIndexes, plans);
    }

    private static int countCaptured(boolean[] captured) {
        int capturedCount = 0;
        for (boolean taken : captured) {
            if (taken) {
                capturedCount++;
            }
        }
        return capturedCount;
    }

    /**
     * 从一处紧邻领域宿主的绑定标记向后捕获攻击或多重展开。
     * 位置、缺口、缺失攻击或越界都返回故障且不改计划；成功时标记捕获范围并放入该领域的冻结计划。
     */
    private static StructureFault captureMark(Skill rawSkill, List<Technique> techniques, int bindIndex,
                                              boolean[] captured, Map<Integer, BoundReleasePlan> plans) {
        if (bindIndex <= 0 || !domainHost(techniques.get(bindIndex - 1))) {
            return new StructureFault(bindIndex, gapMessage(techniques, bindIndex));
        }
        int domainIndex = bindIndex - 1;
        if (plans.containsKey(domainIndex)) {
            return new StructureFault(bindIndex, GAP_BEFORE_MARK);
        }
        if (bindIndex + 1 < techniques.size()) {
            int multicastRoot = MulticastParser.openingIndex(techniques, bindIndex + 1);
            if (multicastRoot >= 0) {
                MulticastExpansion expansion = MulticastParser.parse(techniques, multicastRoot);
                if (expansion == null || !expansion.applicable()) {
                    return new StructureFault(multicastRoot, MISSING_ATTACK);
                }
                if (expansion.orderedMembers() == null || expansion.orderedMembers().isEmpty()) {
                    return new StructureFault(multicastRoot, CompiledReleaseStep.EMPTY_MULTICAST_REFUSAL);
                }
                if (!expansion.releases()) {
                    return new StructureFault(multicastRoot, MISSING_ATTACK);
                }
                return captureExpansion(rawSkill, techniques, bindIndex, domainIndex, multicastRoot, expansion,
                        captured, plans);
            }
        }
        int scanIndex = bindIndex + 1;
        int firstDual = -1;
        int pullAfter = 1;
        while (scanIndex < techniques.size()) {
            Technique current = techniques.get(scanIndex);
            if (SkillSegmentMath.isRemovedRule(current.getId())) {
                return new StructureFault(scanIndex, REMOVED_RULE);
            }
            if (SkillSegmentMath.isOperationHost(current)) {
                return new StructureFault(scanIndex, blockedMessage(current));
            }
            if (current.isEnchantment() || SkillSegmentMath.isChainModifier(current)) {
                if (domainHost(current)) {
                    break;
                }
                if (SkillSegmentMath.roleOf(current) == ChainRole.RULE_DUAL && firstDual < 0) {
                    firstDual = scanIndex;
                    pullAfter = pullOf(current);
                }
                scanIndex++;
                continue;
            }
            break;
        }
        List<Integer> hostIndexes = new ArrayList<>();
        int endInclusive;
        if (firstDual < 0) {
            if (scanIndex >= techniques.size()) {
                return new StructureFault(bindIndex, MISSING_ATTACK);
            }
            Technique stopped = techniques.get(scanIndex);
            if (!qualifiedAttack(stopped)) {
                return new StructureFault(scanIndex, blockedMessage(stopped));
            }
            hostIndexes.add(scanIndex);
            endInclusive = scanIndex;
        } else {
            int room = pullAfter + 1;
            if (room > 2) {
                room = 2;
            }
            int followIndex = firstDual + 1;
            while (followIndex < techniques.size() && hostIndexes.size() < room) {
                Technique current = techniques.get(followIndex);
                if (SkillSegmentMath.isRemovedRule(current.getId())) {
                    return new StructureFault(followIndex, REMOVED_RULE);
                }
                if (SkillSegmentMath.isOperationHost(current)) {
                    return new StructureFault(followIndex, blockedMessage(current));
                }
                if (current.isEnchantment() || SkillSegmentMath.isChainModifier(current)) {
                    if (domainHost(current)) {
                        return new StructureFault(followIndex, blockedMessage(current));
                    }
                    followIndex++;
                    continue;
                }
                hostIndexes.add(followIndex);
                followIndex++;
            }
            if (hostIndexes.size() < 2) {
                return new StructureFault(firstDual, MISSING_SECOND);
            }
            for (int hostIndex : hostIndexes) {
                if (!qualifiedAttack(techniques.get(hostIndex))) {
                    return new StructureFault(hostIndex, MISSING_SECOND);
                }
            }
            endInclusive = hostIndexes.get(hostIndexes.size() - 1);
        }
        for (int spanIndex = bindIndex + 1; spanIndex <= endInclusive; spanIndex++) {
            if (domainHost(techniques.get(spanIndex))) {
                return new StructureFault(spanIndex, blockedMessage(techniques.get(spanIndex)));
            }
        }
        for (int spanIndex = bindIndex; spanIndex <= endInclusive; spanIndex++) {
            captured[spanIndex] = true;
        }
        List<Technique> frozenTechniques = new ArrayList<>();
        List<ShuAttribute> frozenEmbryos = new ArrayList<>();
        for (int spanIndex = bindIndex + 1; spanIndex <= endInclusive; spanIndex++) {
            frozenTechniques.add(techniques.get(spanIndex));
            frozenEmbryos.add(rawSkill.getEmbryo(spanIndex));
        }
        Skill frozenSkill = new Skill(rawSkill.getName(), rawSkill.getType(), frozenTechniques, frozenEmbryos);
        List<ChainSegment> segments = TechniqueChain.split(frozenSkill);
        List<Technique> frozenList = frozenSkill.getTechniques();
        int frozenStart = bindIndex + 1;
        List<BoundMember> members = new ArrayList<>();
        for (int hostIndex : hostIndexes) {
            int localIndex = hostIndex - frozenStart;
            List<Integer> enchantLocalIndexes = enchantIndexes(segments, localIndex);
            ReleaseInput chainInput = SkillChainPlan.frozenChainInput(frozenSkill, frozenList, localIndex);
            members.add(BoundMember.create(localIndex, hostIndex, techniques.get(hostIndex), enchantLocalIndexes, chainInput));
        }
        members = CooperativeCastRouter.stampDomain(frozenSkill, members);
        List<Integer> frozenOriginals = new ArrayList<>();
        for (int spanIndex = frozenStart; spanIndex <= endInclusive; spanIndex++) {
            frozenOriginals.add(spanIndex);
        }
        plans.put(domainIndex, BoundReleasePlan.create(domainIndex, bindIndex, members, frozenTechniques, frozenEmbryos,
                firstDual >= 0, rawSkill.getType(), rawSkill.getName(), frozenOriginals, endInclusive + 1,
                frozenOriginals, List.of()));
        return null;
    }

    /**
     * 捕获绑定标记后的一次多重展开。成员、附魔或重编译对不上时返回故障；
     * 成功后冻结展开消耗的原始段落，并记录复制份数和蓄力下标。
     */
    private static StructureFault captureExpansion(Skill rawSkill, List<Technique> techniques, int bindIndex,
                                                   int domainIndex, int multicastRoot, MulticastExpansion expansion,
                                                   boolean[] captured, Map<Integer, BoundReleasePlan> plans) {
        for (MulticastMember expansionMember : expansion.orderedMembers()) {
            if (expansionMember == null) {
                return new StructureFault(multicastRoot, MISSING_ATTACK);
            }
            int hostIndex = expansionMember.hostIndex();
            Technique host = hostIndex >= 0 && hostIndex < techniques.size() ? techniques.get(hostIndex) : null;
            if (expansionMember.independentEnchant() || !qualifiedAttack(host)) {
                return new StructureFault(hostIndex, blockedMessage(host));
            }
        }
        int nextOriginal = expansion.nextIndex();
        List<Integer> frozenOriginals = new ArrayList<>();
        List<Integer> consumed = expansion.consumedIndexes();
        if (consumed != null) {
            for (Integer rawIndex : consumed) {
                if (rawIndex == null || rawIndex <= bindIndex || rawIndex >= nextOriginal || rawIndex >= techniques.size()) {
                    continue;
                }
                if (frozenOriginals.contains(rawIndex)) {
                    continue;
                }
                Technique occupied = techniques.get(rawIndex);
                if (domainHost(occupied)) {
                    return new StructureFault(rawIndex, blockedMessage(occupied));
                }
                frozenOriginals.add(rawIndex);
            }
        }
        if (frozenOriginals.isEmpty()) {
            return new StructureFault(multicastRoot, MISSING_ATTACK);
        }
        Map<Integer, Integer> localByOriginal = new LinkedHashMap<>();
        for (int frozenOffset = 0; frozenOffset < frozenOriginals.size(); frozenOffset++) {
            localByOriginal.put(frozenOriginals.get(frozenOffset), frozenOffset);
        }
        List<BoundMember> members = new ArrayList<>();
        for (MulticastMember expansionMember : expansion.orderedMembers()) {
            int hostIndex = expansionMember.hostIndex();
            Integer localBoxed = localByOriginal.get(hostIndex);
            if (localBoxed == null) {
                return new StructureFault(hostIndex, MISSING_ATTACK);
            }
            List<Integer> enchantLocals = localsFor(localByOriginal, expansionMember.attachedEnchantIndexes());
            if (enchantLocals == null) {
                return new StructureFault(hostIndex, MISSING_ATTACK);
            }
            List<Integer> inheritedRaw = indexesInside(localByOriginal, expansionMember.inheritedModifierIndexes());
            List<Integer> localRaw = indexesInside(localByOriginal, expansionMember.localModifierIndexes());
            List<Integer> replicationRaw = indexesInside(localByOriginal, expansionMember.replicationIndexes());
            List<Integer> attachedRaw = indexesInside(localByOriginal, expansionMember.attachedEnchantIndexes());
            Technique host = techniques.get(hostIndex);
            ReleaseInput chainInput = energyInput(rawSkill, techniques, inheritedRaw, localRaw, host);
            int copyCount = replicationCount(techniques, replicationRaw);
            members.add(BoundMember.create(localBoxed, hostIndex, host, enchantLocals, chainInput,
                    expansionMember.ordinal(), inheritedRaw, localRaw, replicationRaw, attachedRaw, copyCount));
        }
        List<Technique> frozenTechniques = new ArrayList<>();
        List<ShuAttribute> frozenEmbryos = new ArrayList<>();
        for (int rawIndex : frozenOriginals) {
            frozenTechniques.add(techniques.get(rawIndex));
            frozenEmbryos.add(rawSkill.getEmbryo(rawIndex));
        }
        Skill frozenSkill = new Skill(rawSkill.getName(), rawSkill.getType(), frozenTechniques, frozenEmbryos);
        List<ChainSegment> frozenSegments = TechniqueChain.split(frozenSkill);
        CompiledReleaseStep compiled = CompiledReleaseStep.compile(frozenSkill, frozenTechniques, frozenSegments, 0.0);
        if (compiled != null && compiled.chargeRefusal != null) {
            return new StructureFault(multicastRoot, compiled.chargeRefusal);
        }
        if (compiled == null || !compiled.multicast() || compiled.memberCount() != members.size()) {
            return new StructureFault(multicastRoot, MISSING_ATTACK);
        }
        captured[bindIndex] = true;
        for (int rawIndex : frozenOriginals) {
            captured[rawIndex] = true;
        }
        List<Integer> chargedOriginals = indexesInside(localByOriginal, expansion.chargedIndexes());
        members = CooperativeCastRouter.stampDomain(frozenSkill, members);
        plans.put(domainIndex, BoundReleasePlan.create(domainIndex, bindIndex, members, frozenTechniques, frozenEmbryos,
                true, rawSkill.getType(), rawSkill.getName(), frozenOriginals, nextOriginal, frozenOriginals,
                chargedOriginals));
        return null;
    }

    /** 按继承修饰、本地修饰和宿主的顺序重组一条能量链，再冻结宿主位置的链输入。倍率规则见被调用方。 */
    private static ReleaseInput energyInput(Skill rawSkill, List<Technique> techniques, List<Integer> inheritedRaw,
                                            List<Integer> localRaw, Technique host) {
        List<Technique> energyTechniques = new ArrayList<>();
        appendRaw(energyTechniques, techniques, inheritedRaw);
        appendRaw(energyTechniques, techniques, localRaw);
        energyTechniques.add(host);
        int hostLocal = energyTechniques.size() - 1;
        Skill energySkill = new Skill(rawSkill.getName(), rawSkill.getType(), energyTechniques);
        return SkillChainPlan.frozenChainInput(energySkill, energyTechniques, hostLocal);
    }

    /** 把仍在原链范围内的非空术式追加到能量链。空下标列表直接跳过。 */
    private static void appendRaw(List<Technique> energyTechniques, List<Technique> techniques, List<Integer> rawIndexes) {
        if (rawIndexes == null) {
            return;
        }
        for (int rawIndex : rawIndexes) {
            if (rawIndex < 0 || rawIndex >= techniques.size()) {
                continue;
            }
            Technique chosen = techniques.get(rawIndex);
            if (chosen != null) {
                energyTechniques.add(chosen);
            }
        }
    }

    /** 把原始下标逐个映射成冻结链下标。任一空缺都返回空，表示这组附魔不能进入计划。 */
    private static List<Integer> localsFor(Map<Integer, Integer> localByOriginal, List<Integer> rawIndexes) {
        if (rawIndexes == null || rawIndexes.isEmpty()) {
            return List.of();
        }
        List<Integer> localIndexes = new ArrayList<>();
        for (Integer rawIndex : rawIndexes) {
            if (rawIndex == null) {
                return null;
            }
            Integer localIndex = localByOriginal.get(rawIndex);
            if (localIndex == null) {
                return null;
            }
            localIndexes.add(localIndex);
        }
        return List.copyOf(localIndexes);
    }

    /** 只保留能在冻结链中找到的原始下标，缺失项跳过而不使整组失败。 */
    private static List<Integer> indexesInside(Map<Integer, Integer> localByOriginal, List<Integer> rawIndexes) {
        if (rawIndexes == null || rawIndexes.isEmpty()) {
            return List.of();
        }
        List<Integer> kept = new ArrayList<>();
        for (Integer rawIndex : rawIndexes) {
            if (rawIndex != null && localByOriginal.containsKey(rawIndex)) {
                kept.add(rawIndex);
            }
        }
        return List.copyOf(kept);
    }

    /** 连乘复制声明里不小于 2 的因子。没有可用声明时保持 1；结果超出 int 时停在最大值。 */
    private static int replicationCount(List<Technique> techniques, List<Integer> replicationIndexes) {
        int gathered = 1;
        if (replicationIndexes == null) {
            return gathered;
        }
        for (int replicationSlot : replicationIndexes) {
            if (replicationSlot < 0 || replicationSlot >= techniques.size()) {
                continue;
            }
            Technique replicationTechnique = techniques.get(replicationSlot);
            ReleaseDefinition definition = replicationTechnique == null ? null : ReleaseRegistry.get(replicationTechnique.getId());
            ReplicationSpec spec = definition == null ? null : definition.replication();
            if (spec == null || spec.factor() < 2) {
                continue;
            }
            long widened = (long) gathered * spec.factor();
            if (widened > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            gathered = (int) widened;
        }
        return gathered;
    }

    /** 从冻结分段中取宿主的附魔下标。分段缺失或没有对应宿主时返回空列表。 */
    private static List<Integer> enchantIndexes(List<ChainSegment> segments, int localIndex) {
        if (segments == null) {
            return List.of();
        }
        for (ChainSegment segment : segments) {
            if (segment.actionIndex() == localIndex) {
                return segment.enchantIndexes();
            }
        }
        return List.of();
    }

    /** 区分绑定标记前方是夹了其他术式，还是根本没有紧邻的领域宿主。 */
    private static String gapMessage(List<Technique> techniques, int bindIndex) {
        int walked = bindIndex - 1;
        boolean gap = false;
        boolean foundDomain = false;
        while (walked >= 0) {
            Technique earlier = techniques.get(walked);
            if (bindMarker(earlier)) {
                break;
            }
            if (domainHost(earlier)) {
                foundDomain = true;
                break;
            }
            gap = true;
            walked--;
        }
        if (foundDomain && gap) {
            return GAP_BEFORE_MARK;
        }
        return BESIDE_DOMAIN;
    }

    /** 用被挡住的术式名称说明绑定不能继续向后寻找攻击。 */
    private static String blockedMessage(Technique technique) {
        String shownName = technique == null || technique.getName() == null ? "" : technique.getName();
        return "领域绑定不能越过「" + shownName + "」继续寻找攻击";
    }

    /** 读取双重规则向后拉取的数量。没有定义时是 0，负值按 64 处理。 */
    private static int pullOf(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        int pull = definition == null ? 0 : definition.pullAfterA();
        return pull < 0 ? 64 : pull;
    }

    /** 非附魔且释放角色为领域绑定标记时返回真。没有定义不是标记。 */
    private static boolean bindMarker(Technique technique) {
        if (technique == null || (technique.isEnchantment() && !domainHost(technique))) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.role() == ChainRole.RULE_DOMAIN_BIND;
    }

    /** 登记了领域，或显式声明领域宿主。附魔标记不取消这个身份。 */
    private static boolean domainHost(Technique technique) {
        if (technique == null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && (definition.domain() != null || definition.explicitDomainHost());
    }

    /**
     * 判断可被领域绑定捕获的正伤害攻击。领域宿主、领域、护环、屏障、条件效果和自身状态都不合格。
     */
    private static boolean qualifiedAttack(Technique technique) {
        if (technique == null || technique.isEnchantment() || technique.getBaseDamage() <= 0f) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null || definition.role() != ChainRole.ATTACK) {
            return false;
        }
        if (definition.explicitDomainHost()) {
            return false;
        }
        if (definition.domain() != null || definition.guard() != null || definition.barrier() != null) {
            return false;
        }
        return definition.conditionalEffect() == null && definition.kind() != ReleaseKind.SELF_STATUS;
    }
}
