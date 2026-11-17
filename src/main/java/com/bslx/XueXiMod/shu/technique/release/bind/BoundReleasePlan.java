package com.bslx.XueXiMod.shu.technique.release.bind;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.util.List;

/** 开域时冻住的子计划。不含可变技能列表、槽位或按键游标。 */
public final class BoundReleasePlan {
    private final int originalDomainIndex;
    private final int originalBindIndex;
    private final List<BoundMember> members;
    private final List<Technique> frozenTechniques;
    private final List<ShuAttribute> frozenEmbryos;
    private final boolean dual;
    private final SkillType skillType;
    private final String skillName;
    private final List<Integer> frozenOriginalIndexes;
    private final int nextOriginalIndex;
    private final List<Integer> consumedOriginalIndexes;
    private final List<Integer> chargedOriginalIndexes;

    private BoundReleasePlan(int originalDomainIndex, int originalBindIndex, List<BoundMember> members,
                             List<Technique> frozenTechniques, List<ShuAttribute> frozenEmbryos,
                             boolean dual, SkillType skillType, String skillName,
                             List<Integer> frozenOriginalIndexes, int nextOriginalIndex,
                             List<Integer> consumedOriginalIndexes, List<Integer> chargedOriginalIndexes) {
        this.originalDomainIndex = originalDomainIndex;
        this.originalBindIndex = originalBindIndex;
        this.members = members;
        this.frozenTechniques = frozenTechniques;
        this.frozenEmbryos = frozenEmbryos;
        this.dual = dual;
        this.skillType = skillType;
        this.skillName = skillName;
        this.frozenOriginalIndexes = frozenOriginalIndexes;
        this.nextOriginalIndex = nextOriginalIndex;
        this.consumedOriginalIndexes = consumedOriginalIndexes;
        this.chargedOriginalIndexes = chargedOriginalIndexes;
    }

    /** 旧工厂没有展开游标和计费名单。 */
    public static BoundReleasePlan create(int originalDomainIndex, int originalBindIndex, List<BoundMember> members,
                                          List<Technique> frozenTechniques, List<ShuAttribute> frozenEmbryos,
                                          boolean dual, SkillType skillType, String skillName) {
        return create(originalDomainIndex, originalBindIndex, members, frozenTechniques, frozenEmbryos,
                dual, skillType, skillName, List.of(), -1, List.of(), List.of());
    }

    /** 复制成员和名单。技能类型空着时按术法。 */
    public static BoundReleasePlan create(int originalDomainIndex, int originalBindIndex, List<BoundMember> members,
                                          List<Technique> frozenTechniques, List<ShuAttribute> frozenEmbryos,
                                          boolean dual, SkillType skillType, String skillName,
                                          List<Integer> frozenOriginalIndexes, int nextOriginalIndex,
                                          List<Integer> consumedOriginalIndexes, List<Integer> chargedOriginalIndexes) {
        return new BoundReleasePlan(originalDomainIndex, originalBindIndex,
                members == null ? List.of() : List.copyOf(members),
                frozenTechniques == null ? List.of() : List.copyOf(frozenTechniques),
                frozenEmbryos == null ? List.of() : List.copyOf(frozenEmbryos),
                dual, skillType == null ? SkillType.MAGIC : skillType,
                skillName == null ? "" : skillName,
                frozenOriginalIndexes == null ? List.of() : List.copyOf(frozenOriginalIndexes),
                nextOriginalIndex,
                consumedOriginalIndexes == null ? List.of() : List.copyOf(consumedOriginalIndexes),
                chargedOriginalIndexes == null ? List.of() : List.copyOf(chargedOriginalIndexes));
    }

    public int originalDomainIndex() {
        return originalDomainIndex;
    }

    public int originalBindIndex() {
        return originalBindIndex;
    }

    public List<BoundMember> members() {
        return members;
    }

    public List<Technique> frozenTechniques() {
        return frozenTechniques;
    }

    public List<ShuAttribute> frozenEmbryos() {
        return frozenEmbryos;
    }

    public boolean dual() {
        return dual;
    }

    public SkillType skillType() {
        return skillType;
    }

    /** 只用冻结序列重建技能，供现有报价和附魔解析复用。 */
    public Skill frozenSkill() {
        return new Skill(skillName, skillType, frozenTechniques, frozenEmbryos);
    }

    /** 与 {@link #frozenTechniques()} 对齐的原始装配下标。 */
    public List<Integer> frozenOriginalIndexes() {
        return frozenOriginalIndexes;
    }

    /**
     * 这次展开没有读到的下一原始格。父链从这里继续。
     * -1 表示旧工厂没有这份游标。
     */
    public int nextOriginalIndex() {
        return nextOriginalIndex;
    }

    /** 收进冻结序列的原始格，不含领域术式和绑定标记。 */
    public List<Integer> consumedOriginalIndexes() {
        return consumedOriginalIndexes;
    }

    /**
     * 展开给出的计费原始格，并且确实进了冻结序列。
     * 空列表表示这次捕获没有展开账单。{@link #dual()} 仍只给现有自动轮报价选择旧的双重编译路径。
     */
    public List<Integer> chargedOriginalIndexes() {
        return chargedOriginalIndexes;
    }
}
