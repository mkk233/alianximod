package com.bslx.XueXiMod.shu.technique.release.contact;

import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainContactBilling;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 宿主活着就按快照接触。每 2 tick 采样一次。
 * 高速掠过用身体当前盒和上一位置盒的扫掠相交，表面容差 0.15 格；
 * 两拍之间整段跳过墙面且扫掠盒不相交时可能漏检，不把体积放成球。
 */
public final class ContactEnchantState {
    public static final double SURFACE_SLOP = 0.15;
    private static final ArrayList<ContactEnchantState> ACTIVE = new ArrayList<>();

    private final ContactCarrier carrier;
    private final EnchantmentSnapshot snapshot;
    private final int revision;
    private boolean open = true;

    private ContactEnchantState(ContactCarrier carrier, EnchantmentSnapshot snapshot) {
        this.carrier = carrier;
        this.snapshot = snapshot;
        this.revision = carrier.revision();
    }

    public static ContactEnchantState open(ContactCarrier carrier, EnchantmentSnapshot snapshot) {
        com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainCycle.consider(carrier, snapshot);
        if (carrier == null || !configuredContact(snapshot)) {
            return null;
        }
        ContactEnchantState state = new ContactEnchantState(carrier, snapshot);
        ACTIVE.add(state);
        return state;
    }

    /**
     * 灼地把同一次载荷和根传进来。接触伤害扫描仍只走上面的旧签名逻辑，这里不第二次 consider。
     * 载荷带幽冥时再 attach；根不在绑定表里时由周期自己记住，不新建施法账。
     */
    public static void open(ContactCarrier carrier, EnchantmentSnapshot snapshot, ResourceEnchantLoad load, UUID rootCastId) {
        open(carrier, snapshot);
        if (load != null && load.gloom()) {
            com.bslx.XueXiMod.shu.technique.release.domain.NetherDomainCycle.attach(carrier, load, rootCastId);
        }
    }

    public void close() {
        open = false;
    }

    public static void tick(MinecraftServer server) {
        long contactTickStart = PerformanceRecorder.begin();
        try {
            if (server == null || (server.getTickCount() & 1) != 0) {
                return;
            }
            int detectTick = server.getTickCount();
            try {
                for (int index = ACTIVE.size() - 1; index >= 0; index--) {
                    ContactEnchantState state = ACTIVE.get(index);
                    if (!state.open || !state.carrier.active() || state.carrier.revision() != state.revision) {
                        state.open = false;
                        ACTIVE.remove(index);
                        continue;
                    }
                    state.scan(server, detectTick);
                }
            } finally {
                DomainContactBilling.settlePending();
            }
        } finally {
            PerformanceRecorder.end(PerfMetric.CONTACT_ENCHANT_TICK, contactTickStart);
        }
    }

    private void scan(MinecraftServer server, int detectTick) {
        ServerLevel level = carrier.hostLevel();
        if (level == null || level.getServer() != server) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(carrier.ownerId());
        if (owner == null) {
            return;
        }
        AABB query = queryBox();
        if (query == null) {
            return;
        }
        if (carrier instanceof DomainInstance) {
            return;
        }
        java.util.HashSet<UUID> touched = new java.util.HashSet<>();
        List<LivingEntity> livingTargets = LivingCandidateQueries.entityQuery(level, query, LivingEntity::isAlive);
        for (LivingEntity target : livingTargets) {
            if (!touched.add(target.getUUID())) {
                continue;
            }
            if (!ContactPolicy.mayContactAffect(carrier.ownerId(), target) || !carrier.touches(target)) {
                continue;
            }
            HitVerdict verdict = TechniqueHits.classifyHit(target, owner, snapshot.contactDamage,
                    TechniqueHits.Landing.RESPECT_INVULNERABILITY, snapshot.tideSuccess());
            if (verdict == null || !verdict.continues()) {
                continue;
            }
            if (!open || !carrier.active() || carrier.revision() != revision || !target.isAlive()) {
                continue;
            }
            if (verdict.grantsSeeds()) {
                FireSeedGrant.deposit(target, owner, snapshot.seedPoints);
            }
            snapshot.applyEffects(target, owner.position());
        }
    }

    private AABB queryBox() {
        ServerLevel level = carrier.hostLevel();
        if (level == null) {
            return null;
        }
        return carrier.queryBox();
    }

    public UUID hostId() {
        return carrier.hostId();
    }

    /**
     * 有限的非负配置接触可以安装。恰好为 0 时还要快照里仍有基础附魔，避免把空快照当成接触。
     * 负数和非有限值不安装。原始加数是否大于 0 由上游快照来源已经核对。
     */
    public static boolean configuredContact(EnchantmentSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (snapshot.distinctBasicTypeCount() <= 0) {
            return false;
        }
        float amount = snapshot.contactDamage;
        if (!Float.isFinite(amount) || amount < 0.0f) {
            return false;
        }
        return true;
    }
}
