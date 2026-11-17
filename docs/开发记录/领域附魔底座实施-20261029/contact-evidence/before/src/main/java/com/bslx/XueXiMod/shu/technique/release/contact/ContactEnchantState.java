package com.bslx.XueXiMod.shu.technique.release.contact;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import com.bslx.XueXiMod.shu.technique.release.TechniqueHits;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
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
        if (carrier == null || snapshot == null || snapshot.contactDamage <= 0f) {
            return null;
        }
        ContactEnchantState state = new ContactEnchantState(carrier, snapshot);
        ACTIVE.add(state);
        return state;
    }

    public void close() {
        open = false;
    }

    public static void tick(MinecraftServer server) {
        if (server == null || (server.getTickCount() & 1) != 0) {
            return;
        }
        int detectTick = server.getTickCount();
        for (int index = ACTIVE.size() - 1; index >= 0; index--) {
            ContactEnchantState state = ACTIVE.get(index);
            if (!state.open || !state.carrier.active() || state.carrier.revision() != state.revision) {
                state.open = false;
                ACTIVE.remove(index);
                continue;
            }
            state.scan(server, detectTick);
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
        java.util.HashSet<UUID> touched = new java.util.HashSet<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, query, LivingEntity::isAlive)) {
            if (!touched.add(target.getUUID())) {
                continue;
            }
            if (!ContactPolicy.mayContactAffect(carrier.ownerId(), target) || !carrier.touches(target)) {
                continue;
            }
            if (!TechniqueHits.hurt(target, owner, snapshot.contactDamage, TechniqueHits.Landing.RESPECT_INVULNERABILITY,
                    snapshot.tideSuccess())) {
                continue;
            }
            if (!open || !carrier.active() || carrier.revision() != revision || !target.isAlive()) {
                continue;
            }
            FireSeedGrant.deposit(target, owner, snapshot.seedPoints);
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
}
