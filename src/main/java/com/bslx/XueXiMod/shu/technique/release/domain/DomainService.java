package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.network.DomainSyncPacket;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.DomainCastContext;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.ReleaseMotionLedger;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.StillWaterLease;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.VolumeMotionKind;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.VolumeMotionRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * 领域实例的发布和存续服务。同一波先预检再一起发布；到期、主人死亡、下线或跨维就整份关掉，存在期间不扣维护。
 * 球形场地按周期收维护，护体收生成和维护；领域只在未预留时收起手费。
 */
public final class DomainService {
    private static final ArrayList<DomainInstance> ACTIVE = new ArrayList<>();
    private static final HashSet<String> PLACED_THIS_TICK = new HashSet<>();
    private static int placedTick = Integer.MIN_VALUE;
    private static boolean legacySwept;

    private DomainService() {
    }

    /** 首次启动时清掉旧场地形态。重复调用直接返回，不改新的领域实例。 */
    public static void onServerStarted() {
        if (legacySwept) {
            return;
        }
        legacySwept = true;
        FieldZones.retireLegacyMist();
        FieldZones.retireLegacyPressure();
    }

    /** 用基础释放输入开域。缺参、未学会、账单越界或法力不足时返回假且不放实例。 */
    public static boolean tryWave(ServerPlayer player, Technique source, DomainDefinition definition, int copies, EnchantmentSnapshot snapshot) {
        return tryWave(player, source, definition, copies, snapshot, ReleaseInput.BASE);
    }

    /** 用指定释放输入开域。失败退回和基础入口相同，不收取绑定轮次费用。 */
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
                null, null, null);
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
                load, rootCastId, null);
    }

    public static boolean tryWaveLoaded(ServerPlayer player, Technique source, DomainDefinition definition,
                                        int copies, EnchantmentSnapshot snapshot, ReleaseInput input,
                                        @Nullable BoundReleasePlan boundPlan, @Nullable String claimSuffix,
                                        @Nullable ResourceEnchantLoad load, @Nullable UUID rootCastId,
                                        @Nullable DomainCastContext castContext) {
        if (missingBindingSuffix(boundPlan, claimSuffix)) {
            return false;
        }
        return releaseWave(player, source, definition, copies, snapshot, null, false, claimSuffix, input, boundPlan,
                load, rootCastId, castContext);
    }

    /** 费用已经由父会话预留。feet 不为空时整盒平移到接触点；缺参或几何失败直接返回假，不再退回父会话预留。 */
    public static boolean tryWaveReserved(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                          EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix) {
        return tryWaveReserved(player, source, definition, copies, snapshot, feet, claimSuffix, ReleaseInput.BASE);
    }

    /** 父会话已预留费用并用指定输入开域。同一声明重复请求返回真，不重复扣费或放置。 */
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
                null, null, null);
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
                load, rootCastId, null);
    }

    public static boolean tryWaveReservedLoaded(ServerPlayer player, Technique source, DomainDefinition definition,
                                                int copies, EnchantmentSnapshot snapshot, Vec3 feet, String claimSuffix,
                                                ReleaseInput input, @Nullable BoundReleasePlan boundPlan,
                                                @Nullable ResourceEnchantLoad load, @Nullable UUID rootCastId,
                                                @Nullable DomainCastContext castContext) {
        if (missingBindingSuffix(boundPlan, claimSuffix)) {
            return false;
        }
        return releaseWave(player, source, definition, copies, snapshot, feet, true, claimSuffix, input, boundPlan,
                load, rootCastId, castContext);
    }

    private static boolean missingBindingSuffix(@Nullable BoundReleasePlan boundPlan, @Nullable String claimSuffix) {
        return boundPlan != null && (claimSuffix == null || claimSuffix.isEmpty());
    }

    private static boolean releaseWave(ServerPlayer player, Technique source, DomainDefinition definition, int copies,
                                       EnchantmentSnapshot snapshot, Vec3 feet, boolean reserved, String claimSuffix,
                                       ReleaseInput input, @Nullable BoundReleasePlan boundPlan,
                                       @Nullable ResourceEnchantLoad load, @Nullable UUID rootCastId,
                                       @Nullable DomainCastContext castContext) {
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
        if (castContext != null && (definition.motionKind() != VolumeMotionKind.NONE || definition.placesStillWater())) {
            claim = claim + ":flow:" + castContext.declarationSlot() + ":" + castContext.groupId();
        }
        if (PLACED_THIS_TICK.contains(claim)) {
            return true;
        }
        int unitCost = definition.balanceId().isEmpty()
                ? definition.initialCost()
                : SpecialBalance.mana(definition.balanceId(), BalanceKeys.MANA_COST, definition.initialCost());
        Integer totalBill = exactBill(unitCost, count);
        if (totalBill == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c这次的份数或费用超出可表示范围，没有释放"));
            return false;
        }
        int bill = totalBill;
        if (!reserved && !MagicPool.canAfford(player, bill)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c法力不足"));
            return false;
        }
        List<DomainInstance> pending = new ArrayList<>();
        for (int copyOrdinal = 0; copyOrdinal < count; copyOrdinal++) {
            Vec3 targetFeet = feet;
            if (castContext != null) {
                Vec3 spatial = castContext.origin(player);
                if (!castContext.verticalAxis()) {
                    spatial = new Vec3(spatial.x, player.getY(), spatial.z);
                }
                targetFeet = spatial;
                if (castContext.scatter() && count > 1) {
                    DomainGeometry footprint = definition.place(player);
                    double ring = footprint == null ? 3.0D : Math.max(2.0D, footprint.bounds().getXsize() * 0.75D);
                    double angle = copyOrdinal * 2.399963229728653D;
                    targetFeet = new Vec3(spatial.x + Math.cos(angle) * ring, spatial.y, spatial.z + Math.sin(angle) * ring);
                }
            }
            DomainGeometry geometry = placedGeometry(definition.place(player), targetFeet, player.position(), frozen);
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
            domain.bindCarriedLoad(load);
            if (castContext != null) {
                domain.bindRelease(castContext.groupId(), castContext.declarationSlot());
            }
            NetherDomainCycle.attach(domain, load, rootCastId);
            ACTIVE.add(domain);
            if (domain.definition().motionKind() == VolumeMotionKind.VORTEX_HOST) {
                ReleaseMotionLedger.retain(domain);
            }
            if (domain.definition().placesStillWater()) {
                StillWaterLease.fill(domain);
            }
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

    /** 推进全部领域。主人缺失、死亡、跨维或到期就关闭；不扣领域维护。 */
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
        VolumeMotionRuntime.tick(server);
        DomainEnchantPulse.tick(server, now);
    }

    /** 按实例标识查找活动领域。标识为空或找不到时返回空。 */
    public static DomainInstance find(UUID instanceId) {
        if (instanceId == null) {
            return null;
        }
        for (DomainInstance domain : ACTIVE) {
            if (instanceId.equals(domain.instanceId())) {
                return domain;
            }
        }
        return null;
    }

    /** 关闭指定实例并广播一次移除。空实例忽略，重复移除不重复广播。 */
    public static void closeInstance(DomainInstance domain) {
        if (domain == null) {
            return;
        }
        closeAt(ACTIVE.indexOf(domain), domain);
    }

    /** 关闭这个玩家拥有的全部领域。空玩家忽略，不退已收起手费。 */
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

    /** 已配置单价乘份数。溢出或负数账单返回空，调用方在放置和扣款前拒绝。0 仍是 0。 */
    private static Integer exactBill(int unitCost, int copyCount) {
        if (unitCost < 0 || copyCount < 0) {
            return null;
        }
        try {
            return Math.multiplyExact(unitCost, copyCount);
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    /** 按释放输入缩放寿命。声明为空返回 0，输入为空时用基础输入。 */
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

    /** 绕落地盒底面放大。原几何为空返回空，输入为空时用基础倍率。 */
    public static DomainGeometry enlarge(DomainGeometry placed, ReleaseInput input) {
        if (placed == null) {
            return null;
        }
        ReleaseInput frozen = input == null ? ReleaseInput.BASE : input;
        return AxisBoxGeometry.scaledFromGround(placed.bounds(), frozen.sizeMultiplier());
    }

    /** 判断当前刻是否尚未到期。比较规则交给释放输入，不在这里延长寿命。 */
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
