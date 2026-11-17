package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantCatalog;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantSlice;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceHitCommit;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
import com.bslx.XueXiMod.shu.technique.release.fire.ResourceEffectKeys;
import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 领域额外附魔的整域节拍。创建后第 10、20……刻各收一次当前配置费用，再对范围内合格目标施加一次。
 * 裸领域和只有宿主自带效果的领域不进入这里。缺魔跳过本拍，领域继续存活。
 */
public final class DomainEnchantPulse {
    public static final int PERIOD_TICKS = 10;

    private DomainEnchantPulse() {
    }

    public static void tick(MinecraftServer server, int now) {
        if (server == null) {
            return;
        }
        List<DomainInstance> domains = DomainService.active();
        for (int index = 0; index < domains.size(); index++) {
            DomainInstance domain = domains.get(index);
            if (!due(domain, now)) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(domain.ownerId());
            if (owner == null || !owner.isAlive() || owner.level() != domain.hostLevel()) {
                continue;
            }
            EnchantmentSnapshot snapshot = domain.contactSnapshot();
            if (snapshot == null) {
                continue;
            }
            ResourceEnchantLoad extra = extraLoad(snapshot);
            int price = price(snapshot);
            if (price > 0 && !MagicPool.canAfford(owner, price)) {
                continue;
            }
            List<LivingEntity> targets = eligible(domain, owner);
            ManaPaymentReceipt receipt = price <= 0 ? null : ManaPaymentReceipt.charge(owner, price, false);
            if (price > 0 && receipt == null) {
                continue;
            }
            try {
                apply(domain, owner, snapshot, extra, targets, now);
                if (receipt != null) {
                    receipt.retainShare(price);
                }
            } catch (RuntimeException failure) {
                if (receipt != null) {
                    receipt.refund(owner);
                }
                throw failure;
            }
        }
    }

    /** 创建后满 10 的倍数，且领域仍未到期。第 0 刻只收开域费。 */
    public static boolean due(DomainInstance domain, int now) {
        if (domain == null || !domain.active()) {
            return false;
        }
        int age = now - domain.startedAt();
        return age > 0 && age % PERIOD_TICKS == 0 && DomainService.withinLife(now, domain.expiresAt());
    }

    /** 链上可施加的额外附魔各收一次当前报价。宿主自带效果、留焰和崩地不在这份周期费里。 */
    public static int price(EnchantmentSnapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }
        int total = 0;
        for (String typeId : snapshot.basicTypeIds()) {
            if (!repeatable(typeId)) {
                continue;
            }
            int quoted = quoted(typeId);
            if (quoted <= 0) {
                continue;
            }
            long summed = (long) total + quoted;
            total = summed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) summed;
        }
        return total;
    }

    /** 每一拍单独一本账。同一拍内目标和效果仍去重，下一拍不沿用上一拍。 */
    private static UUID pulseCast(DomainInstance domain, int now) {
        long mixed = domain.instanceId().getMostSignificantBits()
                ^ ((long) now << 32)
                ^ domain.instanceId().getLeastSignificantBits();
        return new UUID(domain.instanceId().getMostSignificantBits(), mixed);
    }
    private static int quoted(String typeId) {
        EnchantmentDefinition definition = EnchantmentRegistry.get(typeId);
        if (definition == null) {
            return 0;
        }
        int explicit = ResourceEnchantCatalog.explicitManaPrice(typeId);
        int raw = explicit >= 0 ? explicit : Math.max(0, definition.magicDamage(
                ElementReaction.of(ShuAttribute.NONE, definition.element())));
        if (!BalanceRegistry.isSealed() || !BalanceRegistry.isDeclared(typeId, BalanceKeys.MANA_COST)) {
            return Math.max(0, raw);
        }
        return Math.max(0, BalanceService.manaCost(typeId, BalanceKeys.MANA_COST, raw));
    }

    /** 领域能对范围内实体重复施加的附魔。留焰要载体轨迹，崩地要落点，幽冥是宿主自己的产种。 */
    private static boolean repeatable(String typeId) {
        return DomainSpawnInheritance.repeatable(typeId);
    }

    private static List<LivingEntity> eligible(DomainInstance domain, ServerPlayer owner) {
        AABB query = domain.queryBox();
        if (domain.hostLevel() == null || query == null) {
            return List.of();
        }
        List<LivingEntity> accepted = new ArrayList<>();
        for (LivingEntity candidate : domain.hostLevel().getEntitiesOfClass(LivingEntity.class, query, LivingEntity::isAlive)) {
            if (!ContactPolicy.mayContactAffect(domain.ownerId(), candidate) || !domain.touches(candidate)) {
                continue;
            }
            if (!DamagePermission.allows(owner, candidate)) {
                continue;
            }
            accepted.add(candidate);
        }
        accepted.sort(Comparator.comparing(LivingEntity::getUUID));
        return accepted;
    }

    private static void apply(DomainInstance domain, ServerPlayer owner, EnchantmentSnapshot snapshot,
                              ResourceEnchantLoad extra, List<LivingEntity> targets, int now) {
        FireCastContext cast = new FireCastContext(pulseCast(domain, now), owner.getUUID());
        UUID seen = null;
        for (LivingEntity target : targets) {
            UUID targetId = target.getUUID();
            if (targetId.equals(seen) || !domain.active() || !target.isAlive()) {
                continue;
            }
            seen = targetId;
            HitVerdict verdict = TechniqueHits.classifyConfirmed(target, owner, snapshot.contactDamage,
                    TechniqueHits.Landing.RESPECT_INVULNERABILITY, snapshot.tideSuccess());
            if (verdict == null || !verdict.continues()) {
                continue;
            }
            if (verdict.grantsSeeds()) {
                FireSeedGrant.deposit(target, owner, snapshot.seedPoints);
            }
            snapshot.applyEffects(target, owner.position());
            if (!extra.empty()) {
                ResourceHitCommit.commit(target, cast, owner,
                        extra, 0, 0, true);
            }
        }
    }

    /** 只留下领域能对实体重复施加的资源效果。留焰和崩地需要载体轨迹或落点。 */
    private static ResourceEnchantLoad extraLoad(EnchantmentSnapshot snapshot) {
        if (snapshot == null) {
            return ResourceEnchantLoad.EMPTY;
        }
        int mana = 0;
        int seeds = 0;
        boolean kindle = false;
        boolean detonate = false;
        List<ResourceEnchantSlice> slices = new ArrayList<>();
        for (String typeId : snapshot.basicTypeIds()) {
            if (!repeatable(typeId)) {
                continue;
            }
            var definition = ResourceEnchantCatalog.find(typeId);
            if (definition == null || slices.stream().anyMatch(slice -> typeId.equals(slice.enchantmentId()))) {
                continue;
            }
            ResourceEnchantSlice slice = definition.slice();
            slices.add(slice);
            mana += Math.max(0, slice.manaPrice());
            seeds += Math.max(0, slice.scaledSeedBase());
            kindle = kindle || slice.hasKindle();
            detonate = detonate || slice.hasDetonate();
        }
        if (slices.isEmpty()) {
            return ResourceEnchantLoad.EMPTY;
        }
        return ResourceEnchantLoad.of(mana, 0, seeds, kindle, detonate, slices);
    }
}
