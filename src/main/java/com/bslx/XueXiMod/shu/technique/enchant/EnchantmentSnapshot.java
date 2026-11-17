package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueIds;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.RawDamage;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * 释放当时解析好的附魔。接触伤害只累加 magicDamage，不再加武器伤害或 addedDamage。
 * 效果参数在这里拆出来，之后不再读主手。
 */
public final class EnchantmentSnapshot {
    public final UUID casterId;
    public final float contactDamage;
    public final int burnSeconds;
    public final int controlTicks;
    public final boolean extinguish;
    public final float knockback;
    public final int tideMarks;
    /** 点火附魔的固定火种。接触伤害被接受时挂上，不随时长倍率变化。 */
    public final int seedPoints;
    /** 冻结时的不同基础附魔标识。重复条目只留一个，不参与接触伤害累加。 */
    private final List<String> basicTypeIds;
    private final List<ShuAttribute> sourceEmbryos;

    private EnchantmentSnapshot(UUID casterId, float contactDamage, int burnSeconds, int controlTicks,
                                boolean extinguish, float knockback, int tideMarks, int seedPoints,
                                List<String> typeIds, List<ShuAttribute> embryos) {
        this.casterId = casterId;
        this.contactDamage = contactDamage;
        this.burnSeconds = burnSeconds;
        this.controlTicks = controlTicks;
        this.extinguish = extinguish;
        this.knockback = knockback;
        this.tideMarks = Math.max(0, tideMarks);
        this.seedPoints = Math.max(0, seedPoints);
        this.basicTypeIds = typeIds == null ? List.of() : List.copyOf(typeIds);
        this.sourceEmbryos = alignEmbryos(this.basicTypeIds, embryos);
    }

    public static EnchantmentSnapshot bare(UUID casterId) {
        return new EnchantmentSnapshot(casterId, 0f, 0, 0, false, 0f, 0, 0, List.of(), List.of());
    }

    /** 只标记一个附魔标识，效果仍由后续 inherit 按登记定义读取。 */
    public static EnchantmentSnapshot marked(UUID casterId, String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return bare(casterId);
        }
        return new EnchantmentSnapshot(casterId, 0f, 0, 0, false, 0f, 0, 0,
                List.of(typeId), List.of(ShuAttribute.NONE));
    }

    public List<ShuAttribute> sourceEmbryos() {
        return sourceEmbryos;
    }

    /** 冻结快照里不同基础附魔的个数。同一标识重复出现只算一种。 */
    public int distinctBasicTypeCount() {
        return basicTypeIds.size();
    }

    /** 按标识排序的只读列表。伤害和效果仍按原快照计算，不读这份名单。 */
    public List<String> basicTypeIds() {
        return basicTypeIds;
    }

    public TideMarkSuccess tideSuccess() {
        return TideMarkSuccess.of(this.tideMarks, this.casterId);
    }

    /**
     * 把领域快照里可继承的附魔合并进这一发。
     * 同一标识在自身和领域各留一次；没有可继承条目时原样返回。
     */
    public EnchantmentSnapshot inherit(EnchantmentSnapshot carried, List<String> inheritedTypeIds) {
        if (carried == null || inheritedTypeIds == null || inheritedTypeIds.isEmpty()) {
            return this;
        }
        List<String> accepted = new ArrayList<>();
        List<ShuAttribute> acceptedEmbryos = new ArrayList<>();
        List<String> stored = carried.basicTypeIds;
        List<ShuAttribute> storedEmbryos = carried.sourceEmbryos;
        for (String typeId : inheritedTypeIds) {
            if (typeId == null || typeId.isBlank()) {
                continue;
            }
            int storedIndex = stored.indexOf(typeId);
            if (storedIndex < 0) {
                continue;
            }
            accepted.add(typeId);
            ShuAttribute embryo = storedIndex < storedEmbryos.size() ? storedEmbryos.get(storedIndex) : ShuAttribute.NONE;
            acceptedEmbryos.add(embryo == null ? ShuAttribute.NONE : embryo);
        }
        if (accepted.isEmpty()) {
            return this;
        }
        Read read = readTypes(accepted, acceptedEmbryos);
        return new EnchantmentSnapshot(this.casterId, this.contactDamage + read.damage, this.burnSeconds + read.burnSeconds,
                Math.max(this.controlTicks, read.controlTicks), this.extinguish || read.extinguish,
                Math.max(this.knockback, read.knockback), this.tideMarks + read.tideMarks,
                this.seedPoints + read.seedPoints, concat(this.basicTypeIds, accepted),
                concat(this.sourceEmbryos, acceptedEmbryos));
    }

    public static float damageOf(Skill skill, List<Integer> indexes) {
        float total = 0f;
        if (skill == null || indexes == null) {
            return 0f;
        }
        List<Technique> techniques = skill.getTechniques();
        for (int index : indexes) {
            if (index < 0 || index >= techniques.size()) {
                continue;
            }
            Technique technique = techniques.get(index);
            if (technique == null) {
                continue;
            }
            EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
            if (definition == null) {
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
            int rawDamage = definition.magicDamage(reaction);
            if (rawDamage <= 0) {
                continue;
            }
            String techniqueId = canonicalTechniqueId(technique.getId());
            if (techniqueId.isEmpty()) {
                continue;
            }
            total += BalanceService.damageContribution(techniqueId, RawDamage.of(rawDamage)).amount();
        }
        return total;
    }

    /** 逐槽 magicDamage 原值合计。不读配置，供接触附魔在缩放前判断是否有接触伤害。 */
    public static float rawDamageOf(Skill skill, List<Integer> indexes) {
        if (skill == null || indexes == null) {
            return 0.0f;
        }
        float total = 0.0f;
        List<Technique> techniques = skill.getTechniques();
        for (int slotIndex : indexes) {
            if (slotIndex < 0 || slotIndex >= techniques.size()) {
                continue;
            }
            Technique technique = techniques.get(slotIndex);
            if (technique == null) {
                continue;
            }
            EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
            if (definition == null) {
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(slotIndex), definition.element());
            total += definition.magicDamage(reaction);
        }
        return total;
    }

    private static String canonicalTechniqueId(String techniqueId) {
        if (techniqueId == null || techniqueId.isBlank()) {
            return "";
        }
        String canonicalId = TechniqueIds.canonicalId(techniqueId);
        return canonicalId == null || canonicalId.isBlank() ? techniqueId : canonicalId;
    }

    public static EnchantmentSnapshot from(Skill skill, ChainSegment segment, ServerPlayer caster) {
        if (skill == null || segment == null || segment.enchantIndexes().isEmpty() || caster == null) {
            return null;
        }
        Read loaded = read(skill, segment.enchantIndexes());
        List<String> typeIds = distinctBasicTypeIds(skill, segment.enchantIndexes());
        return new EnchantmentSnapshot(caster.getUUID(), loaded.damage, loaded.burnSeconds, loaded.controlTicks,
                loaded.extinguish, loaded.knockback, loaded.tideMarks, loaded.seedPoints, typeIds,
                sourceEmbryos(skill, segment.enchantIndexes(), typeIds));
    }


    private static List<ShuAttribute> alignEmbryos(List<String> typeIds, List<ShuAttribute> embryos) {
        List<ShuAttribute> aligned = new ArrayList<>(typeIds.size());
        for (int index = 0; index < typeIds.size(); index++) {
            ShuAttribute embryo = embryos != null && index < embryos.size() ? embryos.get(index) : null;
            aligned.add(embryo == null ? ShuAttribute.NONE : embryo);
        }
        return List.copyOf(aligned);
    }

    /** 同一附魔标识只留第一次出现的原胚，顺序和去重后的标识一致。 */
    private static List<ShuAttribute> sourceEmbryos(Skill skill, List<Integer> indexes, List<String> typeIds) {
        Map<String, ShuAttribute> found = new LinkedHashMap<>();
        List<Technique> techniques = skill.getTechniques();
        for (int index : indexes) {
            if (index < 0 || index >= techniques.size() || techniques.get(index) == null) {
                continue;
            }
            EnchantmentDefinition definition = EnchantmentRegistry.get(techniques.get(index).getId());
            if (definition == null || definition.id() == null || definition.id().isBlank()) {
                continue;
            }
            found.putIfAbsent(definition.id(), skill.getEmbryo(index));
        }
        List<ShuAttribute> aligned = new ArrayList<>(typeIds.size());
        for (String typeId : typeIds) {
            ShuAttribute embryo = found.get(typeId);
            aligned.add(embryo == null ? ShuAttribute.NONE : embryo);
        }
        return aligned;
    }

    private static <T> List<T> concat(List<T> current, List<T> extra) {
        List<T> combined = new ArrayList<>(current.size() + extra.size());
        combined.addAll(current);
        combined.addAll(extra);
        return List.copyOf(combined);
    }

    /** 与伤害累加分开走一遍。只收集解析成功的附魔标识，同一标识只留一个。 */
    private static List<String> distinctBasicTypeIds(Skill skill, List<Integer> indexes) {
        TreeSet<String> typeIds = new TreeSet<>();
        List<Technique> techniques = skill.getTechniques();
        for (int index : indexes) {
            if (index < 0 || index >= techniques.size()) {
                continue;
            }
            Technique technique = techniques.get(index);
            if (technique == null || technique.getId() == null) {
                continue;
            }
            EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
            if (definition == null) {
                continue;
            }
            String typeId = definition.id();
            if (typeId == null || typeId.isBlank()) {
                typeId = technique.getId();
            }
            if (typeId.isBlank()) {
                continue;
            }
            typeIds.add(typeId);
        }
        return List.copyOf(typeIds);
    }

    /** 接触快照和检查共用。潮印只计数，不在这里灭火，也不按效果 id 去重。 */
    static Read read(Skill skill, List<Integer> indexes) {
        Read read = new Read();
        read.damage = damageOf(skill, indexes);
        if (skill == null || indexes == null) {
            return read;
        }
        float fireDuration = 1f;
        boolean hasFire = false;
        List<Technique> techniques = skill.getTechniques();
        for (int index : indexes) {
            if (index < 0 || index >= techniques.size()) {
                continue;
            }
            EnchantmentDefinition definition = EnchantmentRegistry.get(techniques.get(index).getId());
            if (definition == null || definition.createAccumulator() == null) {
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
            String effectId = definition.createAccumulator().effectId();
            if ("fire".equals(effectId)) {
                hasFire = true;
                if (reaction.kind() == ElementReaction.Kind.GENERATE) {
                    fireDuration *= reaction.durationMultiplier();
                }
            } else if ("water".equals(effectId)) {
                read.tideMarks += 1;
            } else if ("earth".equals(effectId)) {
                read.knockback = Math.max(read.knockback, 0.35f);
            }
        }
        if (hasFire) {
            read.burnSeconds = SpecialBalance.burnSeconds(fireDuration);
            read.seedPoints = 2;
        }
        return read;
    }

    /** 按已登记的效果键计算一次快照效果。资源附魔没有旧五行键时不产生旧效果。 */
    private static Read readTypes(List<String> typeIds, List<ShuAttribute> embryos) {
        Read read = new Read();
        float fireDuration = 1f;
        boolean hasFire = false;
        for (int index = 0; index < typeIds.size(); index++) {
            String typeId = typeIds.get(index);
            EnchantmentDefinition definition = EnchantmentRegistry.get(typeId);
            if (definition == null || definition.createAccumulator() == null) {
                continue;
            }
            ShuAttribute embryo = embryos.get(index);
            ElementReaction reaction = ElementReaction.of(embryo, definition.element());
            int rawDamage = definition.magicDamage(reaction);
            if (rawDamage > 0) {
                read.damage += BalanceService.damageContribution(canonicalTechniqueId(typeId),
                        RawDamage.of(rawDamage)).amount();
            }
            String effectId = definition.createAccumulator().effectId();
            if ("fire".equals(effectId)) {
                hasFire = true;
                if (reaction.kind() == ElementReaction.Kind.GENERATE) {
                    fireDuration *= reaction.durationMultiplier();
                }
            } else if ("water".equals(effectId)) {
                read.tideMarks += 1;
            } else if ("earth".equals(effectId)) {
                read.knockback = Math.max(read.knockback, 0.35f);
            }
        }
        if (hasFire) {
            read.burnSeconds = SpecialBalance.burnSeconds(fireDuration);
            read.seedPoints = 2;
        }
        return read;
    }

    static final class Read {
        float damage;
        int burnSeconds;
        int controlTicks;
        boolean extinguish;
        float knockback;
        int tideMarks;
        int seedPoints;
    }

    public void applyEffects(LivingEntity target, Vec3 from) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (HarmPolicy.denies(this.casterId, target)) {
            return;
        }
        if (burnSeconds > 0) {
            target.setSecondsOnFire(burnSeconds);
        }
        if (extinguish) {
            target.clearFire();
        }
        if (controlTicks > 0) {
            com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime.suppressLegacyControl(this.casterId, target);
        }
        if (knockback > 0f && from != null) {
            Vec3 delta = target.position().subtract(from);
            target.knockback(knockback, delta.x, delta.z);
        }
    }
}
