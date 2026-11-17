package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
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

    private EnchantmentSnapshot(UUID casterId, float contactDamage, int burnSeconds, int controlTicks,
                                boolean extinguish, float knockback, int tideMarks, int seedPoints,
                                List<String> typeIds) {
        this.casterId = casterId;
        this.contactDamage = contactDamage;
        this.burnSeconds = burnSeconds;
        this.controlTicks = controlTicks;
        this.extinguish = extinguish;
        this.knockback = knockback;
        this.tideMarks = Math.max(0, tideMarks);
        this.seedPoints = Math.max(0, seedPoints);
        this.basicTypeIds = typeIds == null ? List.of() : List.copyOf(typeIds);
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
            EnchantmentDefinition definition = EnchantmentRegistry.get(techniques.get(index).getId());
            if (definition == null) {
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
            total += definition.magicDamage(reaction);
        }
        return total;
    }

    public static EnchantmentSnapshot from(Skill skill, ChainSegment segment, ServerPlayer caster) {
        if (skill == null || segment == null || segment.enchantIndexes().isEmpty() || caster == null) {
            return null;
        }
        Read loaded = read(skill, segment.enchantIndexes());
        List<String> typeIds = distinctBasicTypeIds(skill, segment.enchantIndexes());
        return new EnchantmentSnapshot(caster.getUUID(), loaded.damage, loaded.burnSeconds, loaded.controlTicks,
                loaded.extinguish, loaded.knockback, loaded.tideMarks, loaded.seedPoints, typeIds);
    }

    /** 与伤害累加分开走一遍。只收集解析成功的附魔标识，同一标识只留一个。 */
    private static List<String> distinctBasicTypeIds(Skill skill, List<Integer> indexes) {
        if (skill == null || indexes == null) {
            return List.of();
        }
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
            if (typeId == null || typeId.isBlank()) {
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
            } else if ("wood".equals(effectId)) {
                int ticks = Math.max(1, Math.round(40f * reaction.durationMultiplier()));
                read.controlTicks = Math.max(read.controlTicks, ticks);
            } else if ("water".equals(effectId)) {
                read.tideMarks += 1;
            } else if ("earth".equals(effectId)) {
                read.knockback = Math.max(read.knockback, 0.35f);
            }
        }
        if (hasFire) {
            read.burnSeconds = Math.max(1, Math.round(5f * fireDuration));
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
        if (burnSeconds > 0) {
            target.setSecondsOnFire(burnSeconds);
        }
        if (extinguish) {
            target.clearFire();
        }
        if (controlTicks > 0) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, controlTicks, 4));
        }
        if (knockback > 0f && from != null) {
            Vec3 delta = target.position().subtract(from);
            target.knockback(knockback, delta.x, delta.z);
        }
    }
}
