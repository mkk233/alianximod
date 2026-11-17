package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.contact.ContactPolicy;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 只给领域实例的接触宿主做事件预留。屏障、场地、护体和普通攻击不进这里。
 * 同一拍收齐后再按主人、领域实例、目标标识排序，逐笔读当时余额。
 */
public final class DomainContactBilling {
    private static final ArrayList<PendingContact> PENDING = new ArrayList<>();
    private static boolean settling;

    private DomainContactBilling() {
    }

    public static void collect(DomainInstance domain, ServerPlayer owner, EnchantmentSnapshot snapshot,
                               ServerLevel level, AABB query) {
        if (settling || domain == null || owner == null || snapshot == null || level == null || query == null) {
            return;
        }
        if (!domain.active() || snapshot.contactDamage <= 0f) {
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
        try {
            if (PENDING.isEmpty()) {
                return;
            }
            List<PendingContact> batch = new ArrayList<>(PENDING);
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
            PENDING.clear();
            settling = false;
        }
    }

    private static void apply(PendingContact event) {
        if (!eligible(event) || event.snapshot.distinctBasicTypeCount() <= 0) {
            return;
        }
        int price = DomainContactCharge.quote(event.snapshot);
        if (price <= 0) {
            if (confirmDamage(event)) {
                deliverPayload(event);
            }
            return;
        }
        ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(event.owner, price, false);
        if (receipt == null) {
            return;
        }
        boolean committed = false;
        boolean confirmed = false;
        try {
            confirmed = eligible(event) && confirmDamage(event);
            if (confirmed) {
                receipt.retainShare(price);
                committed = true;
            }
        } finally {
            if (!committed) {
                receipt.refund(event.owner);
            }
        }
        if (confirmed) {
            deliverPayload(event);
        }
    }

    private static boolean eligible(PendingContact event) {
        if (event.domain == null || event.owner == null || event.target == null || event.snapshot == null) {
            return false;
        }
        if (!event.domain.active() || event.domain.revision() != event.revision) {
            return false;
        }
        if (!event.domain.ownerId().equals(event.owner.getUUID()) || event.snapshot.contactDamage <= 0f) {
            return false;
        }
        return ContactPolicy.mayContactAffect(event.domain.ownerId(), event.target) && event.domain.touches(event.target);
    }

    private static boolean confirmDamage(PendingContact event) {
        return TechniqueHits.hurtConfirmed(event.target, event.owner, event.snapshot.contactDamage,
                TechniqueHits.Landing.RESPECT_INVULNERABILITY, event.snapshot.tideSuccess());
    }

    private static void deliverPayload(PendingContact event) {
        if (!event.domain.active() || event.domain.revision() != event.revision || !event.target.isAlive()) {
            return;
        }
        FireSeedGrant.deposit(event.target, event.owner, event.snapshot.seedPoints);
        event.snapshot.applyEffects(event.target, event.owner.position());
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
