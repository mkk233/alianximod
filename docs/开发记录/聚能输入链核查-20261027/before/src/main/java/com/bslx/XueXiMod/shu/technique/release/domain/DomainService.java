package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.network.DomainSyncPacket;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** 同一波先预检再一起发布。维护失败或到期就整份关掉。 */
public final class DomainService {
    private static final ArrayList<DomainInstance> ACTIVE = new ArrayList<>();
    private static final HashSet<String> PLACED_THIS_TICK = new HashSet<>();
    private static int placedTick = Integer.MIN_VALUE;
    private static boolean legacySwept;

    private DomainService() {
    }

    public static void onServerStarted() {
        if (legacySwept) {
            return;
        }
        legacySwept = true;
        FieldZones.retireLegacyMist();
        FieldZones.retireLegacyPressure();
    }

    public static boolean tryWave(ServerPlayer player, Technique source, DomainDefinition definition, int copies, EnchantmentSnapshot snapshot) {
        return releaseWave(player, source, definition, copies, snapshot, null, false, null);
    }

    /** 费用已经由父会话预留。feet 不为空时整盒平移到接触点，不再跟施法者脚下。 */
    public static boolean tryWaveReserved(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                          EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix) {
        return releaseWave(player, source, definition, copies, snapshot, feet, true, claimSuffix);
    }

    private static boolean releaseWave(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                       EnchantmentSnapshot snapshot, Vec3 feet, boolean reserved, String claimSuffix) {
        if (player == null || source == null || definition == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!PlayerShuData.get(player).isUnlocked(source)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c还没有学会释放" + source.getName()));
            return false;
        }
        int count = Math.max(1, copies);
        int now = level.getServer().getTickCount();
        if (placedTick != now) {
            PLACED_THIS_TICK.clear();
            placedTick = now;
        }
        String kind = definition.nativePolicy() == null ? "plain" : definition.nativePolicy().effectKey();
        String claim = player.getUUID() + ":" + kind + ":" + definition.initialCost() + ":" + definition.lifetimeTicks();
        if (claimSuffix != null) {
            claim = claim + ":" + claimSuffix;
        }
        if (PLACED_THIS_TICK.contains(claim)) {
            return true;
        }
        int bill = definition.initialCost() * count;
        if (!reserved && !MagicPool.canAfford(player, bill)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c魔力不足"));
            return false;
        }
        List<DomainInstance> pending = new ArrayList<>();
        for (int copy = 0; copy < count; copy++) {
            DomainGeometry geometry = definition.place(player);
            if (geometry != null && feet != null) {
                Vec3 delta = feet.subtract(player.position());
                geometry = new AxisBoxGeometry(geometry.bounds().move(delta.x, delta.y, delta.z));
            }
            if (geometry == null) {
                discard(pending);
                return false;
            }
            pending.add(new DomainInstance(player.getUUID(), level, geometry, definition, now));
        }
        if (!reserved) {
            ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(player, bill, true);
            if (receipt == null) {
                discard(pending);
                return false;
            }
        }
        for (DomainInstance domain : pending) {
            domain.bindContact(snapshot);
            ACTIVE.add(domain);
            DomainSyncPacket.broadcast(domain, false);
        }
        if (!reserved && bill > 0) {
            MagicPool.showBalance(player);
        }
        PLACED_THIS_TICK.add(claim);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        int now = server.getTickCount();
        for (int index = ACTIVE.size() - 1; index >= 0; index--) {
            DomainInstance domain = ACTIVE.get(index);
            if (!domain.active()) {
                ACTIVE.remove(index);
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(domain.ownerId());
            if (owner == null || !owner.isAlive() || owner.level() != domain.hostLevel() || now >= domain.expiresAt()) {
                closeAt(index, domain);
                continue;
            }
            if (now >= domain.nextUpkeepAt()) {
                if (!MagicPool.trySpend(owner, domain.definition().upkeepCost(), false)) {
                    closeAt(index, domain);
                    continue;
                }
                domain.setNextUpkeepAt(now + domain.definition().upkeepPeriod());
            }
        }
        DomainPulses.fire(ACTIVE, now);
    }

    public static void closeOwned(ServerPlayer player) {
        if (player == null) {
            return;
        }
        for (int index = ACTIVE.size() - 1; index >= 0; index--) {
            DomainInstance domain = ACTIVE.get(index);
            if (player.getUUID().equals(domain.ownerId())) {
                closeAt(index, domain);
            }
        }
    }

    public static List<DomainInstance> active() {
        return ACTIVE;
    }

    private static void closeAt(int index, DomainInstance domain) {
        domain.close();
        ACTIVE.remove(index);
        DomainSyncPacket.broadcast(domain, true);
    }

    private static void discard(List<DomainInstance> pending) {
        for (DomainInstance domain : pending) {
            domain.close();
        }
    }
}
