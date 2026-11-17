package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.network.DomainSyncPacket;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** 同一波先预检再一起发布。到期、主人死亡、下线或跨维就整份关掉。存在期间不扣维护。 */
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
        return tryWave(player, source, definition, copies, snapshot, ReleaseInput.BASE);
    }

    public static boolean tryWave(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                  EnchantmentSnapshot snapshot, ReleaseInput input) {
        return tryWave(player, source, definition, copies, snapshot, input, null, null);
    }

    /**
     * 带冻结绑定计划的开域。计划非空时必须有声明后缀，避免不同绑定在同一刻被去重合并。
     * 这里仍只收领域起手费，不收后续每轮子计划的费用。
     */
    public static boolean tryWave(ServerPlayer player, Technique source, DomainDefinition definition,
                                  int copies, EnchantmentSnapshot snapshot, ReleaseInput input,
                                  @Nullable BoundReleasePlan boundPlan, @Nullable String claimSuffix) {
        if (missingBindingSuffix(boundPlan, claimSuffix)) {
            return false;
        }
        return releaseWave(player, source, definition, copies, snapshot, null, false, claimSuffix, input, boundPlan,
                null, null);
    }

    /**
     * 带上这一发的附魔载荷和根施法标识。旧入口不传这两项，零接触伤的纯幽冥要走这里。
     */
    public static boolean tryWaveLoaded(ServerPlayer player, Technique source, DomainDefinition definition,
                                        int copies, EnchantmentSnapshot snapshot, ReleaseInput input,
                                        @Nullable BoundReleasePlan boundPlan, @Nullable String claimSuffix,
                                        @Nullable ResourceEnchantLoad load, @Nullable UUID rootCastId) {
        if (missingBindingSuffix(boundPlan, claimSuffix)) {
            return false;
        }
        return releaseWave(player, source, definition, copies, snapshot, null, false, claimSuffix, input, boundPlan,
                load, rootCastId);
    }

    /** 费用已经由父会话预留。feet 不为空时整盒平移到接触点，不再跟施法者脚下。 */
    public static boolean tryWaveReserved(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                          EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix) {
        return tryWaveReserved(player, source, definition, copies, snapshot, feet, claimSuffix, ReleaseInput.BASE);
    }

    public static boolean tryWaveReserved(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                          EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix, ReleaseInput input) {
        return tryWaveReserved(player, source, definition, copies, snapshot, feet, claimSuffix, input, null);
    }

    /**
     * 费用已经由父会话预留。有冻结计划时必须带同一声明后缀。
     * 周期释放的费用不在这次开域里预扣。
     */
    public static boolean tryWaveReserved(ServerPlayer player, Technique source, DomainDefinition definition,
                                          int copies, EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix,
                                          ReleaseInput input, @Nullable BoundReleasePlan boundPlan) {
        if (missingBindingSuffix(boundPlan, claimSuffix)) {
            return false;
        }
        return releaseWave(player, source, definition, copies, snapshot, feet, true, claimSuffix, input, boundPlan,
                null, null);
    }

    /**
     * 费用已由父会话预留时的幽冥入口。根标识只在绑定表里已经存在时才会被持有。
     */
    public static boolean tryWaveReservedLoaded(ServerPlayer player, Technique source, DomainDefinition definition,
                                                int copies, EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix,
                                                ReleaseInput input, @Nullable BoundReleasePlan boundPlan,
                                                @Nullable ResourceEnchantLoad load, @Nullable UUID rootCastId) {
        if (missingBindingSuffix(boundPlan, claimSuffix)) {
            return false;
        }
        return releaseWave(player, source, definition, copies, snapshot, feet, true, claimSuffix, input, boundPlan,
                load, rootCastId);
    }

    private static boolean missingBindingSuffix(@Nullable BoundReleasePlan boundPlan, @Nullable String claimSuffix) {
        return boundPlan != null && (claimSuffix == null || claimSuffix.isEmpty());
    }

    private static boolean releaseWave(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                       EnchantmentSnapshot snapshot, Vec3 feet, boolean reserved, String claimSuffix,
                                       ReleaseInput input, @Nullable BoundReleasePlan boundPlan,
                                       @Nullable ResourceEnchantLoad load, @Nullable UUID rootCastId) {
        if (player == null || source == null || definition == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        int scaledLife = frozenLife(definition, frozen);
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
            DomainGeometry geometry = placedGeometry(definition.place(player), feet, player.position(), frozen);
            if (geometry == null) {
                discard(pending);
                return false;
            }
            pending.add(new DomainInstance(player.getUUID(), level, geometry, definition, now, scaledLife, boundPlan));
        }
        if (!reserved) {
            ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(player, bill, true);
            if (receipt == null) {
                discard(pending);
                return false;
            }
        }
        int placedOrdinal = 0;
        for (DomainInstance domain : pending) {
            domain.bindContact(snapshot);
            NetherDomainCycle.attach(domain, load, rootCastId);
            ACTIVE.add(domain);
            if (reserved) {
                PublicationAttempt.reportPlacedCopy(placedOrdinal);
            }
            placedOrdinal++;
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
        int index = ACTIVE.size();
        while (index > 0) {
            index--;
            if (index >= ACTIVE.size()) {
                index = ACTIVE.size();
                continue;
            }
            DomainInstance domain = ACTIVE.get(index);
            int sizeBefore = ACTIVE.size();
            if (!domain.active()) {
                ACTIVE.remove(index);
            } else {
                ServerPlayer owner = server.getPlayerList().getPlayer(domain.ownerId());
                if (owner == null || !owner.isAlive() || owner.level() != domain.hostLevel() || !withinLife(now, domain.expiresAt())) {
                    closeAt(index, domain);
                }
            }
            if (ACTIVE.size() < sizeBefore) {
                index = ACTIVE.size();
            }
        }
        DomainPulses.fire(ACTIVE, now);
        List<DomainInstance> activeSnapshot = List.copyOf(ACTIVE);
        DomainBindingRuntime.tick(server, now, activeSnapshot);
        NetherDomainCycle.tick(server);
    }

    public static void closeOwned(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int index = ACTIVE.size();
        while (index > 0) {
            index--;
            if (index >= ACTIVE.size()) {
                index = ACTIVE.size();
                continue;
            }
            DomainInstance domain = ACTIVE.get(index);
            if (!player.getUUID().equals(domain.ownerId())) {
                continue;
            }
            int sizeBefore = ACTIVE.size();
            closeAt(index, domain);
            if (ACTIVE.size() < sizeBefore) {
                index = ACTIVE.size();
            }
        }
    }

    public static List<DomainInstance> active() {
        return ACTIVE;
    }

    public static int frozenLife(DomainDefinition definition, ReleaseInput input) {
        if (definition == null) {
            return 0;
        }
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        return frozen.scaleDuration(definition.lifetimeTicks());
    }

    /** 先按接触点平移，再绕落地锚点放大。feet 为空时锚点就是原放置盒的地面中心。 */
    public static DomainGeometry placedGeometry(DomainGeometry placed, Vec3 feet, Vec3 playerFeet, ReleaseInput input) {
        DomainGeometry shifted = placed;
        if (placed != null && feet != null && playerFeet != null) {
            Vec3 delta = feet.subtract(playerFeet);
            shifted = new AxisBoxGeometry(placed.bounds().move(delta.x, delta.y, delta.z));
        }
        return enlarge(shifted, input);
    }

    public static DomainGeometry enlarge(DomainGeometry placed, ReleaseInput input) {
        if (placed == null) {
            return null;
        }
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        return AxisBoxGeometry.scaledFromGround(placed.bounds(), frozen.sizeMultiplier());
    }

    public static boolean withinLife(int now, int expiresAt) {
        return !ReleaseInput.hasExpired(now, expiresAt);
    }

    private static void closeAt(int index, DomainInstance domain) {
        try {
            domain.close();
        } finally {
            if (index >= 0 && index < ACTIVE.size() && ACTIVE.get(index) == domain) {
                ACTIVE.remove(index);
            } else {
                ACTIVE.remove(domain);
            }
            if (domain.markRemovalBroadcast()) {
                DomainSyncPacket.broadcast(domain, true);
            }
        }
    }

    private static void discard(List<DomainInstance> pending) {
        for (DomainInstance domain : pending) {
            domain.close();
        }
    }
}
