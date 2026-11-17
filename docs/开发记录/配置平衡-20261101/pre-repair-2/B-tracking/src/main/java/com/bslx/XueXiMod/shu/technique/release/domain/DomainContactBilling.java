package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * 只给领域实例的接触宿主做事件预留。屏障、场地、护体和普通攻击不进这里。
 * 同一拍收齐后再按主人、领域实例、目标标识排序，逐笔读当时余额。
 */
public final class DomainContactBilling {
    private static final ArrayList<PendingContact> PENDING = new ArrayList<>();
    /** 有限 0 接触在同一次停留里只收一次费。目标离开这次收集后忘掉。 */
    private static final HashMap<UUID, HashSet<UUID>> ZERO_STAY = new HashMap<>();
    private static boolean settling;

    private DomainContactBilling() {
    }

    public static void collect(DomainInstance domain, ServerPlayer owner, EnchantmentSnapshot snapshot,
                               ServerLevel level, AABB query) {
        if (settling || domain == null || owner == null || snapshot == null || level == null || query == null) {
            return;
        }
        if (!domain.active() || !ContactEnchantState.configuredContact(snapshot)) {
            return;
        }
        List<LivingEntity> eligible = new ArrayList<>();
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, query, LivingEntity::isAlive)) {
            if (!ContactPolicy.mayContactAffect(domain.ownerId(), candidate) || !domain.touches(candidate)) {
                continue;
            }
            eligible.add(candidate);
        }
        eligible.sort(DomainContactBilling::compareTargets);
        UUID seenTarget = null;
        for (LivingEntity candidate : eligible) {
            UUID targetId = candidate.getUUID();
            if (targetId.equals(seenTarget)) {
                continue;
            }
            seenTarget = targetId;
            PENDING.add(new PendingContact(domain, owner, candidate, snapshot, domain.revision()));
        }
    }

    public static void settlePending() {
        if (settling) {
            return;
        }
        settling = true;
        List<PendingContact> batch = new ArrayList<>();
        try {
            if (PENDING.isEmpty()) {
                return;
            }
            batch.addAll(PENDING);
            PENDING.clear();
            batch.sort(DomainContactBilling::compareEvents);
            PendingContact previous = null;
            for (PendingContact event : batch) {
                if (previous != null && sameEvent(previous, event)) {
                    continue;
                }
                previous = event;
                apply(event);
            }
        } finally {
            pruneZeroStays(batch);
            PENDING.clear();
            settling = false;
        }
    }

    private static void apply(PendingContact event) {
        if (!eligible(event) || event.snapshot.distinctBasicTypeCount() <= 0) {
            return;
        }
        float amount = event.snapshot.contactDamage;
        if (Float.isFinite(amount) && amount == 0.0f) {
            applyZero(event);
            return;
        }
        int price = contactPrice(event);
        if (price <= 0) {
            HitVerdict verdict = classifyContact(event);
            if (verdict != null && verdict.grantsSeeds()) {
                deliverPayload(event, true);
            }
            return;
        }
        ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(event.owner, price, false);
        if (receipt == null) {
            return;
        }
        boolean committed = false;
        boolean seeded = false;
        try {
            HitVerdict verdict = eligible(event) ? classifyContact(event) : null;
            seeded = verdict != null && verdict.grantsSeeds();
            if (verdict != null && verdict.continues() && seeded) {
                receipt.retainShare(price);
                committed = true;
            }
        } catch (Throwable failure) {
            throw unchecked(failure);
        } finally {
            if (!committed) {
                receipt.refund(event.owner);
            }
        }
        if (committed) {
            deliverPayload(event, seeded);
        }
    }

    /** 有限 0 先看裁决。成立才扣一次并留下；同一次停留里后面的采样只补状态，不再扣。 */
    private static void applyZero(PendingContact event) {
        HitVerdict verdict = classifyContact(event);
        if (verdict == null || !verdict.continues()) {
            forgetZero(event);
            return;
        }
        if (zeroStay(event)) {
            deliverPayload(event, false);
            return;
        }
        int price = contactPrice(event);
        if (price <= 0) {
            deliverPayload(event, false);
            rememberZero(event);
            return;
        }
        ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(event.owner, price, false);
        if (receipt == null) {
            return;
        }
        boolean committed = false;
        try {
            if (eligible(event)) {
                receipt.retainShare(price);
                committed = true;
            }
        } catch (Throwable failure) {
            throw unchecked(failure);
        } finally {
            if (!committed) {
                receipt.refund(event.owner);
            }
        }
        if (!committed) {
            return;
        }
        deliverPayload(event, false);
        rememberZero(event);
    }

    private static HitVerdict classifyContact(PendingContact event) {
        return TechniqueHits.classifyConfirmed(event.target, event.owner, event.snapshot.contactDamage,
                TechniqueHits.Landing.RESPECT_INVULNERABILITY, event.snapshot.tideSuccess());
    }

    /** 报价保持原始单价乘种类。扣费只把单价缩放一次，再乘种类。 */
    private static int contactPrice(PendingContact event) {
        if (event.snapshot == null) {
            return 0;
        }
        int kinds = event.snapshot.distinctBasicTypeCount();
        if (kinds <= 0) {
            return 0;
        }
        String techniqueId = event.domain == null || event.domain.definition() == null
                ? ""
                : event.domain.definition().balanceId();
        int unit = techniqueId.isEmpty()
                ? DomainContactCharge.MANA_PER_BASIC_ENCHANTMENT
                : SpecialBalance.mana(techniqueId, DomainDefinition.CONTACT_MANA_COST,
                DomainContactCharge.MANA_PER_BASIC_ENCHANTMENT);
        if (unit <= 0) {
            return 0;
        }
        long total = (long) kinds * (long) unit;
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    private static RuntimeException unchecked(Throwable failure) {
        if (failure instanceof RuntimeException runtimeFailure) {
            return runtimeFailure;
        }
        if (failure instanceof Error errorFailure) {
            throw errorFailure;
        }
        return new RuntimeException(failure);
    }

    private static boolean eligible(PendingContact event) {
        if (event.domain == null || event.owner == null || event.target == null || event.snapshot == null) {
            return false;
        }
        if (!event.domain.active() || event.domain.revision() != event.revision) {
            return false;
        }
        if (!event.domain.ownerId().equals(event.owner.getUUID())
                || !ContactEnchantState.configuredContact(event.snapshot)) {
            return false;
        }
        return ContactPolicy.mayContactAffect(event.domain.ownerId(), event.target) && event.domain.touches(event.target);
    }

    private static void deliverPayload(PendingContact event, boolean seeded) {
        if (!event.domain.active() || event.domain.revision() != event.revision || !event.target.isAlive()) {
            return;
        }
        if (seeded) {
            FireSeedGrant.deposit(event.target, event.owner, event.snapshot.seedPoints);
        }
        event.snapshot.applyEffects(event.target, event.owner.position());
    }

    private static boolean zeroStay(PendingContact event) {
        HashSet<UUID> targets = ZERO_STAY.get(event.domain.instanceId());
        return targets != null && targets.contains(event.target.getUUID());
    }

    private static void rememberZero(PendingContact event) {
        ZERO_STAY.computeIfAbsent(event.domain.instanceId(), domainId -> new HashSet<>()).add(event.target.getUUID());
    }

    private static void forgetZero(PendingContact event) {
        HashSet<UUID> targets = ZERO_STAY.get(event.domain.instanceId());
        if (targets == null) {
            return;
        }
        targets.remove(event.target.getUUID());
        if (targets.isEmpty()) {
            ZERO_STAY.remove(event.domain.instanceId());
        }
    }

    private static void pruneZeroStays(List<PendingContact> batch) {
        HashMap<UUID, HashSet<UUID>> present = new HashMap<>();
        for (PendingContact event : batch) {
            if (event.domain == null || event.target == null) {
                continue;
            }
            present.computeIfAbsent(event.domain.instanceId(), domainId -> new HashSet<>()).add(event.target.getUUID());
        }
        ArrayList<UUID> domainIds = new ArrayList<>(ZERO_STAY.keySet());
        for (UUID domainId : domainIds) {
            HashSet<UUID> remembered = ZERO_STAY.get(domainId);
            HashSet<UUID> stillInside = present.get(domainId);
            if (remembered == null || stillInside == null) {
                ZERO_STAY.remove(domainId);
                continue;
            }
            remembered.retainAll(stillInside);
            if (remembered.isEmpty()) {
                ZERO_STAY.remove(domainId);
            }
        }
    }

    private static int compareTargets(LivingEntity left, LivingEntity right) {
        return left.getUUID().compareTo(right.getUUID());
    }

    private static int compareEvents(PendingContact left, PendingContact right) {
        int ownerOrder = left.domain.ownerId().compareTo(right.domain.ownerId());
        if (ownerOrder != 0) {
            return ownerOrder;
        }
        int instanceOrder = left.domain.instanceId().compareTo(right.domain.instanceId());
        if (instanceOrder != 0) {
            return instanceOrder;
        }
        return left.target.getUUID().compareTo(right.target.getUUID());
    }

    private static boolean sameEvent(PendingContact left, PendingContact right) {
        return left.domain.instanceId().equals(right.domain.instanceId())
                && left.target.getUUID().equals(right.target.getUUID());
    }

    private static final class PendingContact {
        private final DomainInstance domain;
        private final ServerPlayer owner;
        private final LivingEntity target;
        private final EnchantmentSnapshot snapshot;
        private final int revision;

        private PendingContact(DomainInstance domain, ServerPlayer owner, LivingEntity target,
                               EnchantmentSnapshot snapshot, int revision) {
            this.domain = domain;
            this.owner = owner;
            this.target = target;
            this.snapshot = snapshot;
            this.revision = revision;
        }
    }
}
