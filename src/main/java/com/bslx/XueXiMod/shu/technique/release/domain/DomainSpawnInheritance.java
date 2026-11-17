package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.balance.BalanceDisplay;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantCatalog;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantSlice;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceHitCommit;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthLayout;
import com.bslx.XueXiMod.shu.technique.release.batch.RootCastBindings;
import com.bslx.XueXiMod.shu.technique.release.bind.ReleaseExecutionContext;
import com.bslx.XueXiMod.shu.technique.release.fire.ResourceEffectKeys;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 领域承载的普通额外附魔，在领域内新生成的攻击出生时并入那一发。
 * 旧五行效果和新资源效果共用这一条，不按具体术式标识补丁。
 * 幽冥本体留在领域产种，不进入攻击载荷；领域周期已付过的附魔不再向攻击重复收费。
 */
public final class DomainSpawnInheritance {
    private static final ThreadLocal<Inheritance> HELD = new ThreadLocal<>();

    private DomainSpawnInheritance() {
    }

    public static EnchantBonus apply(ServerPlayer caster, Skill castSkill, List<Integer> ownIndexes,
                                     EnchantBonus ownBonus) {
        ReleaseExecutionContext current = ReleaseExecutionContext.current();
        if (current != null && current.domainInstanceId() != null) {
            return apply(current, castSkill, ownIndexes, ownBonus);
        }
        return apply(carried(ownedDomain(caster)), castSkill, ownIndexes, ownBonus);
    }

    public static EnchantBonus apply(ReleaseExecutionContext context, Skill castSkill,
                                     List<Integer> ownIndexes, EnchantBonus ownBonus) {
        return apply(carried(context), castSkill, ownIndexes, ownBonus);
    }

    /** 领域快照直接供共享入口使用。多个所属领域重叠时调用方不猜测。 */
    public static EnchantBonus apply(EnchantmentSnapshot carried, Skill castSkill, List<Integer> ownIndexes,
                                     EnchantBonus ownBonus) {
        if (carried == null) {
            return ownBonus;
        }
        List<String> inherited = inheritable(carried);
        if (inherited.isEmpty()) {
            return ownBonus;
        }
        EnchantBonus merged = merge(ownBonus, carried, inherited);
        HELD.set(new Inheritance(carried, List.copyOf(inherited)));
        return merged;
    }

    /** 把领域快照中的旧效果和资源载荷各并入一次。自身已有的同名附魔保留。 */
    public static EnchantBonus merge(EnchantBonus ownBonus, EnchantmentSnapshot carried,
                                     List<String> inheritedTypeIds) {
        EnchantBonus resolved = ownBonus == null ? EnchantBonus.NONE : ownBonus;
        if (carried == null || inheritedTypeIds == null || inheritedTypeIds.isEmpty()) {
            return resolved;
        }
        EnchantBonus inherited = EnchantBonus.resolve(synthetic(inheritedTypeIds, carried), identity(inheritedTypeIds.size()),
                0f, 1);
        ResourceEnchantLoad combined = combine(resolved.resourceLoad, inheritedTypeIds);
        float added = resolved.addedDamage;
        String addedSource = resolved.addedSourceId;
        if (added <= 0f && inherited.addedDamage > 0f) {
            added = inherited.addedDamage;
            addedSource = inherited.addedSourceId;
        }
        return new EnchantBonus(resolved.attackDamage, added,
                resolved.burnSeconds + inherited.burnSeconds,
                resolved.radiusMultiplier * inherited.radiusMultiplier,
                resolved.cooldownMultiplier, Math.max(resolved.controlTicks, inherited.controlTicks),
                resolved.extinguish || inherited.extinguish, Math.max(resolved.knockback, inherited.knockback),
                resolved.speedMultiplier * inherited.speedMultiplier, resolved.tideMarks + inherited.tideMarks,
                resolved.seedPoints + inherited.seedPoints, combined, addedSource);
    }

    /** 领域附魔和攻击自带附魔各留一次。同标识不因重复扫描合并掉其中一份。 */
    public static ResourceEnchantLoad combine(ResourceEnchantLoad ownLoad, List<String> inheritedTypeIds) {
        ResourceEnchantLoad own = ownLoad == null ? ResourceEnchantLoad.EMPTY : ownLoad;
        if (inheritedTypeIds == null || inheritedTypeIds.isEmpty()) {
            return own;
        }
        int mana = own.manaPrice();
        int damage = own.directDamage();
        int seeds = own.scaledSeedBase();
        boolean kindle = own.kindle();
        boolean detonate = own.detonate();
        boolean trail = own.trail();
        boolean gloom = own.gloom();
        boolean broken = own.breakGround();
        List<ResourceEnchantSlice> slices = new ArrayList<>(own.slices());
        for (String typeId : inheritedTypeIds) {
            var entry = ResourceEnchantCatalog.find(typeId);
            if (entry == null) {
                continue;
            }
            ResourceEnchantSlice slice = entry.slice();
            slices.add(slice);
            seeds = add(seeds, configuredSeed(typeId, slice));
            kindle = kindle || slice.hasKindle();
            detonate = detonate || slice.hasDetonate();
            trail = trail || slice.hasTrail();
            gloom = gloom || slice.hasGloom();
            broken = broken || slice.hasBreakGround();
        }
        return ResourceEnchantLoad.of(mana, damage, seeds, kindle, detonate, trail, gloom, broken, slices);
    }


    /** 这一发生成结束时放下暂存标识，避免下一发读到上一发的领域附魔。 */
    public static void release() {
        HELD.remove();
    }

    /** 领域接触快照并入新攻击。留焰和崩地仍只留在资源载荷，等自身轨迹或落点触发。 */
    public static EnchantmentSnapshot inheritContact(EnchantmentSnapshot own) {
        Inheritance held = HELD.get();
        if (held == null) {
            return own;
        }
        if (own == null) {
            return contactOnly(held.carried, held.typeIds);
        }
        return own.inherit(held.carried, held.typeIds);
    }

    /**
     * 把领域继承写进已有出生槽。槽上原来的自身附魔保留，同标识再追加一次。
     * 没有槽、没有继承，或这一发还没有根账时不改，也不在这里新建账。
     */
    public static void install(BirthLayout.Slot slot, ReleaseExecutionContext context, Technique attack) {
        List<String> inherited = heldTypes();
        if (slot == null || inherited.isEmpty() || slot.rootCastId == null) {
            return;
        }
        if (RootCastBindings.lookup(slot.rootCastId) == null) {
            return;
        }
        ResourceEnchantLoad own = slot.resourceLoad == null ? ResourceEnchantLoad.EMPTY : slot.resourceLoad;
        ResourceEnchantLoad combined = combine(own, inherited);
        if (combined == own) {
            return;
        }
        slot.resourceCommit = true;
        slot.resourceLoad = combined;
        if (slot.bodyScaledBase == 0 && attack != null) {
            ReleaseDefinition definition = ReleaseRegistry.get(attack.getId());
            slot.bodyScaledBase = definition == null ? 0 : definition.bodyScaledSeedBase();
        }
    }

    /** 领域自身不另收继承附魔。自动轮只补这一发生成时还没计入的自身附魔价。 */
    public static int automaticEnchantPrice(Skill skill, List<Integer> ownIndexes, boolean alreadyPriced) {
        if (alreadyPriced || heldTypes().isEmpty()) {
            return 0;
        }
        ResourceEnchantLoad own = ResourceEnchantCatalog.collect(skill, ownIndexes);
        return Math.max(0, own.manaPrice());
    }

    /** 领域内这一轮共用一本账。账已存在就沿用，不覆盖开域时的幽冥账。 */
    public static UUID automaticRoot(ReleaseExecutionContext context) {
        if (context == null || context.domainInstanceId() == null || context.roundId() == null || context.owner() == null) {
            return null;
        }
        return RootCastBindings.open(context.roundId(), context.owner().getUUID()) == null ? null : context.roundId();
    }


    /**
     * 施法者当前只属于一个自己的活动领域时返回它。
     * 多个领域同时包含施法者时没有既有归属规则，返回空，不挑选。
     */
    /**
     * 新攻击出生时的共享继承入口。自动上下文、唯一所属领域都走这里；没有领域时原样返回。
     * 同属性加成仍只收一次，调用后立即放下暂存，避免影响下一次出生。
     */
    public static EnchantBonus finish(ServerPlayer caster, Technique attack, Skill skill, List<Integer> indexes,
                                      EnchantBonus ownBonus) {
        ReleaseExecutionContext current = ReleaseExecutionContext.current();
        EnchantmentSnapshot carried = current != null && current.domainInstanceId() != null
                ? carried(current) : carried(ownedDomain(caster));
        if (carried == null) {
            return ownBonus;
        }
        EnchantBonus inherited = apply(carried, skill, indexes, ownBonus);
        ShuAttribute attribute = attack == null ? null : attack.getAttribute();
        try {
            return stackBonus(inherited, attribute, matchedByOwn(attribute, skill, indexes), carried);
        } finally {
            release();
        }
    }

    private static EnchantmentSnapshot carried(DomainInstance domain) {
        return domain == null ? null : domain.carriedEnchantments();
    }

    public static DomainInstance ownedDomain(ServerPlayer caster) {
        if (caster == null || !caster.isAlive()) {
            return null;
        }
        DomainInstance matched = null;
        for (DomainInstance domain : DomainService.active()) {
            if (domain == null || !domain.active() || !caster.getUUID().equals(domain.ownerId())
                    || domain.hostLevel() != caster.serverLevel() || !domain.touches(caster)) {
                continue;
            }
            if (matched != null) {
                return null;
            }
            matched = domain;
        }
        return matched;
    }

    /** 攻击原胚与任一真实附魔同属性时，伤害和释放冷却各按 1.30、0.70 收一次。 */
    public static float stackDamage(float baseDamage, ShuAttribute attack, boolean alreadyMatched,
                                    EnchantmentSnapshot carried) {
        if (baseDamage <= 0f || alreadyMatched || !matches(attack, carried)) {
            return baseDamage;
        }
        return baseDamage * ElementReaction.of(embryoOf(attack, carried), attack).damageMultiplier();
    }

    public static int stackCooldown(int configuredTicks, ShuAttribute attack, boolean ownMatched,
                                    EnchantmentSnapshot carried) {
        if (configuredTicks <= 0 || attack == null || attack == ShuAttribute.NONE) {
            return configuredTicks;
        }
        boolean carriedMatched = matches(attack, carried);
        if (!ownMatched && !carriedMatched) {
            return configuredTicks;
        }
        ShuAttribute embryo = ownMatched ? attack : embryoOf(attack, carried);
        int cooled = (int) Math.round(configuredTicks * ElementReaction.of(embryo, attack).cooldownMultiplier());
        return Math.max(1, cooled);
    }


    /** 伤害只加成宿主底伤和同属性附魔的原始附加。潮印、武器和资源点数不进这个乘法。 */
    public static EnchantBonus stackBonus(EnchantBonus bonus, ShuAttribute attack, boolean alreadyMatched,
                                          EnchantmentSnapshot carried) {
        EnchantBonus resolved = bonus == null ? EnchantBonus.NONE : bonus;
        if (alreadyMatched || !matches(attack, carried)) {
            return resolved;
        }
        float multiplier = ElementReaction.of(embryoOf(attack, carried), attack).damageMultiplier();
        float attackDamage = resolved.attackDamage * multiplier;
        float addedDamage = resolved.addedDamage > 0f ? resolved.addedDamage * multiplier : resolved.addedDamage;
        return new EnchantBonus(attackDamage, addedDamage, resolved.burnSeconds, resolved.radiusMultiplier,
                resolved.cooldownMultiplier, resolved.controlTicks, resolved.extinguish, resolved.knockback,
                resolved.speedMultiplier, resolved.tideMarks, resolved.seedPoints, resolved.resourceLoad,
                resolved.addedSourceId);
    }

    public static List<String> inheritable(EnchantmentSnapshot snapshot) {
        List<String> accepted = new ArrayList<>();
        if (snapshot == null) {
            return accepted;
        }
        for (String typeId : snapshot.basicTypeIds()) {
            if (transferable(typeId)) {
                accepted.add(typeId);
            }
        }
        return accepted;
    }

    /**
     * 普通附魔快照加上领域术式自己声明的攻击载荷。
     * 没有声明攻击载荷的领域仍是空快照；声明的载荷不进入领域周期收费。
     */
    public static EnchantmentSnapshot offer(EnchantmentSnapshot carried, DomainDefinition definition) {
        String payloadId = payloadId(definition);
        if (payloadId.isEmpty() || !transferable(payloadId)) {
            return carried;
        }
        EnchantmentSnapshot base = carried == null ? EnchantmentSnapshot.bare(null) : carried;
        if (base.basicTypeIds().contains(payloadId)) {
            return base;
        }
        return base.inherit(EnchantmentSnapshot.marked(base.casterId, payloadId), List.of(payloadId));
    }

    /** 原版近战已经确认落地后，并入创建者当前唯一领域的攻击载荷。 */
    public static void grantVanilla(ServerPlayer attacker, LivingEntity target, FireCastContext cast,
                                    boolean accepted) {
        if (!accepted || attacker == null || target == null || cast == null) {
            return;
        }
        if (!cast.claimEffect(target.getUUID(), "domain-owner-attack")) {
            return;
        }
        EnchantmentSnapshot carried = carried(ownedDomain(attacker));
        List<String> inherited = inheritable(carried);
        if (inherited.isEmpty()) {
            return;
        }
        EnchantmentSnapshot applied = EnchantmentSnapshot.bare(attacker.getUUID()).inherit(carried, inherited);
        if (applied.seedPoints > 0) {
            FireSeedGrant.deposit(target, attacker, applied.seedPoints);
        }
        applied.applyEffects(target, attacker.position());
        ResourceEnchantLoad load = combine(ResourceEnchantLoad.EMPTY, inherited);
        if (!load.empty()) {
            ResourceHitCommit.commit(target, cast, attacker, load, 0, 0, true);
        }
    }

    /** 领域周期可重复施加的附魔。轨迹和落点效果留到攻击自己的触发条件。 */
    public static boolean repeatable(String typeId) {
        if (!transferable(typeId)) {
            return false;
        }
        ReleaseDefinition release = ReleaseRegistry.get(typeId);
        if (release != null && release.tag() == ReleaseTag.SUSTAINED) {
            return false;
        }
        ResourceEnchantSlice slice = sliceOf(typeId);
        return slice == null || (!slice.hasTrail() && !slice.hasBreakGround() && !slice.hasGloom());
    }

    public static boolean transferable(String typeId) {
        if (typeId == null) {
            return false;
        }
        EnchantmentDefinition definition = EnchantmentRegistry.get(typeId);
        if (definition == null) {
            return false;
        }
        ReleaseDefinition release = ReleaseRegistry.get(typeId);
        if (release == null || release.domain() == null) {
            return true;
        }
        String payloadId = definition.attackPayloadId();
        return payloadId != null && payloadId.equals(typeId);
    }

    private static String payloadId(DomainDefinition definition) {
        if (definition == null || definition.balanceId() == null || definition.balanceId().isBlank()) {
            return "";
        }
        EnchantmentDefinition enchantment = EnchantmentRegistry.get(definition.balanceId());
        if (enchantment == null || enchantment.attackPayloadId() == null) {
            return "";
        }
        return enchantment.attackPayloadId();
    }

    public static int displayedAttackCooldown(Technique attack, boolean alreadyMatched,
                                              EnchantmentSnapshot carried) {
        if (attack == null) {
            return 0;
        }
        int configured = BalanceDisplay.cooldown(attack.getId(), attack.getBaseCooldown());
        return stackCooldown(configured, attack.getAttribute(), alreadyMatched, carried);
    }

    /** 自动轮报价前使用。领域没带可继承附魔时，冷却和原来完全一致。 */
    public static int automaticCooldown(Technique attack, Skill skill, List<Integer> indexes,
                                        EnchantmentSnapshot carried) {
        return displayedAttackCooldown(attack, matchedByOwn(attack == null ? null : attack.getAttribute(), skill, indexes),
                carried);
    }

    /** 自身附魔已经按原胚同属性折过时，继承来的同属性不再折第二次。 */
    public static boolean matchedByOwn(ShuAttribute attack, Skill skill, List<Integer> indexes) {
        if (attack == null || attack == ShuAttribute.NONE || skill == null || indexes == null) {
            return false;
        }
        List<Technique> techniques = skill.getTechniques();
        for (int index : indexes) {
            if (index < 0 || index >= techniques.size() || techniques.get(index) == null) {
                continue;
            }
            EnchantmentDefinition definition = EnchantmentRegistry.get(techniques.get(index).getId());
            if (definition != null && definition.element() == attack) {
                return true;
            }
        }
        return false;
    }

    private static EnchantmentSnapshot carried(ReleaseExecutionContext context) {
        if (context == null || context.domainInstanceId() == null) {
            return null;
        }
        DomainInstance domain = DomainService.find(context.domainInstanceId());
        return domain == null ? null : domain.carriedEnchantments();
    }

    private static boolean matches(ShuAttribute attack, EnchantmentSnapshot carried) {
        return embryoOf(attack, carried) != null;
    }

    /** 沿用原胚对术式的既有反应。快照没有对应原胚时，退回术式自身属性。 */
    private static ShuAttribute embryoOf(ShuAttribute attack, EnchantmentSnapshot carried) {
        if (attack == null || attack == ShuAttribute.NONE || carried == null) {
            return null;
        }
        List<String> inherited = inheritable(carried);
        List<ShuAttribute> embryos = carried.sourceEmbryos();
        List<String> stored = carried.basicTypeIds();
        for (String typeId : inherited) {
            int index = stored.indexOf(typeId);
            ShuAttribute embryo = index >= 0 && index < embryos.size() ? embryos.get(index) : attack;
            ElementReaction reaction = ElementReaction.of(embryo, attack);
            if (reaction.kind() == ElementReaction.Kind.SAME) {
                return embryo;
            }
        }
        return null;
    }

    private static List<String> heldTypes() {
        Inheritance held = HELD.get();
        return held == null ? List.of() : held.typeIds;
    }

    private static EnchantmentSnapshot contactOnly(EnchantmentSnapshot carried, List<String> inherited) {
        if (carried == null || inherited == null || inherited.isEmpty()) {
            return null;
        }
        EnchantmentSnapshot empty = EnchantmentSnapshot.bare(carried.casterId);
        EnchantmentSnapshot merged = empty.inherit(carried, inherited);
        return merged == empty ? null : merged;
    }

    private static Skill synthetic(List<String> typeIds, EnchantmentSnapshot carried) {
        ArrayList<Technique> techniques = new ArrayList<>();
        ArrayList<ShuAttribute> embryos = new ArrayList<>();
        List<String> stored = carried.basicTypeIds();
        List<ShuAttribute> storedEmbryos = carried.sourceEmbryos();
        for (String typeId : typeIds) {
            EnchantmentDefinition definition = EnchantmentRegistry.get(typeId);
            if (definition == null) {
                continue;
            }
            techniques.add(new Technique(typeId, typeId, TechniqueType.MAGIC,
                    0f, 0, 0, "", 0, definition.element(), 1f, true));
            int storedIndex = stored.indexOf(typeId);
            ShuAttribute embryo = storedIndex >= 0 && storedIndex < storedEmbryos.size()
                    ? storedEmbryos.get(storedIndex) : ShuAttribute.NONE;
            embryos.add(embryo == null ? ShuAttribute.NONE : embryo);
        }
        return new Skill("domain-inheritance", SkillType.MAGIC, techniques, embryos);
    }

    private static List<Integer> identity(int size) {
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            indexes.add(index);
        }
        return indexes;
    }

    private static ResourceEnchantSlice sliceOf(String typeId) {
        var definition = ResourceEnchantCatalog.find(typeId);
        return definition == null ? null : definition.slice();
    }

    private static final class Inheritance {
        private final EnchantmentSnapshot carried;
        private final List<String> typeIds;

        private Inheritance(EnchantmentSnapshot carried, List<String> typeIds) {
            this.carried = carried;
            this.typeIds = typeIds;
        }
    }

    private static int configuredSeed(String typeId, ResourceEnchantSlice slice) {
        if (ResourceEffectKeys.SOW.equals(typeId) || ResourceEffectKeys.GLOOM.equals(typeId)) {
            return ResourceEnchantCatalog.configuredSeed(typeId, slice.scaledSeedBase());
        }
        return Math.max(0, slice.scaledSeedBase());
    }

    private static int add(int current, int extra) {
        long summed = (long) current + Math.max(0, extra);
        return summed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) summed;
    }
}
