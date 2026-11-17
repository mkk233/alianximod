package com.bslx.XueXiMod.shu.technique.release.conditional;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonPressure;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonRuntime;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonCommitResult;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonReservation;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonService;
import com.bslx.XueXiMod.shu.technique.release.statusquery.RelationPolicy;
import com.bslx.XueXiMod.shu.technique.release.statusquery.ResourceView;
import com.bslx.XueXiMod.shu.technique.release.statusquery.StatusCandidate;
import com.bslx.XueXiMod.shu.technique.release.statusquery.StatusQuery;
import com.bslx.XueXiMod.shu.technique.release.statusquery.StatusScan;
import com.bslx.XueXiMod.shu.technique.release.statusquery.StatusScanRequest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 水牢按施术者周围的潮湿敌对目标逐个准备和提交。
 * 牢的状态、禁锢和结算仍在牢服务里。这里不刷新已有牢，也不在快照外补目标。
 */
public final class WaterPrisonEffect implements ConditionalEffect {
    public static final WaterPrisonEffect INSTANCE = new WaterPrisonEffect();
    public static final double CAPTURE_RADIUS = 12.0D;
    public static final int SUCCESS_MANA = 40;
    public static final ResourceLocation WETNESS = new ResourceLocation(XueXiMod.MOD_ID, "wetness");
    public static final String TECHNIQUE_ID = "water_prison";

    private WaterPrisonEffect() {
    }

    @Override
    public int unitManaCost() {
        return SUCCESS_MANA;
    }

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.ticks(
                SpecialBalance.ticks(
                        SpecialBalance.ticks(
                                SpecialBalance.blocks(SpecialBalance.begin(TECHNIQUE_ID),
                                        BalanceKeys.SCAN_RANGE, CAPTURE_RADIUS, "水牢扫描半径"),
                                BalanceKeys.PRISON_DURATION_TICKS, PrisonRuntime.tierOneTicks(), "水牢一层时长"),
                        BalanceKeys.DURATION_TICKS, PrisonRuntime.tierTwoTicks(), "水牢二层时长"),
                BalanceKeys.PRESSURE_INTERVAL_TICKS, PrisonPressure.INTERVAL_TICKS, "水牢压迫间隔"));
    }

    @Override
    public boolean copiesCoalesce() {
        return true;
    }

    @Override
    public boolean supportsContact() {
        return true;
    }

    @Override
    public ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact, ReleaseInput input) {
        return this.attempt(caster, operationId, contact, input, null);
    }

    @Override
    public ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact,
                                     ReleaseInput input, ManaPaymentReceipt receipt) {
        ReleaseInput frozenInput = input == null ? ReleaseInput.BASE : input;
        if (caster == null || operationId == null) {
            return ConditionalResult.rejected("没有可禁锢的潮湿目标");
        }
        if (receipt == null) {
            return ConditionalResult.rejected("法力不足");
        }
        List<StatusCandidate> found = StatusScan.collect(caster, new StatusScanRequest(
                SpecialBalance.number(TECHNIQUE_ID, BalanceKeys.SCAN_RANGE, CAPTURE_RADIUS),
                StatusQuery.and(StatusQuery.resourceAtLeast(WETNESS, 1), StatusQuery.effectActive(WETNESS)),
                RelationPolicy.hostileCapture(),
                true,
                true));
        if (found == null || found.isEmpty()) {
            return ConditionalResult.rejected("没有可禁锢的潮湿目标");
        }
        List<StatusCandidate> ordered = new ArrayList<>(found);
        ordered.removeIf(candidate -> candidate == null || candidate.subjectId() == null);
        if (ordered.isEmpty()) {
            return ConditionalResult.rejected("没有可禁锢的潮湿目标");
        }
        ordered.sort((leftCandidate, rightCandidate) -> TargetPayment.captureOrder(
                wetTier(leftCandidate), leftCandidate.closestDistanceSquared(), leftCandidate.subjectId(),
                wetTier(rightCandidate), rightCandidate.closestDistanceSquared(), rightCandidate.subjectId()));
        int unitCost = SpecialBalance.mana(TECHNIQUE_ID, BalanceKeys.MANA_COST, this.unitManaCost());
        int successCount = 0;
        boolean paymentShort = false;
        String lastReason = "";
        Set<UUID> engaged = new HashSet<>();
        for (StatusCandidate candidate : ordered) {
            if (candidate == null || candidate.subjectId() == null) {
                continue;
            }
            LivingEntity subject = candidate.subject();
            if (subject == null || !subject.isAlive() || subject.isSpectator()) {
                continue;
            }
            UUID targetOperation = ConditionalIntent.targetOperationId(operationId, candidate.subjectId());
            if (targetOperation == null || !engaged.add(targetOperation)) {
                continue;
            }
            if (PrisonService.reservationOpen(targetOperation)) {
                continue;
            }
            if (!MagicPool.canAfford(caster, unitCost)) {
                paymentShort = true;
                break;
            }
            PrisonReservation reservation = PrisonService.prepare(caster, subject, targetOperation, frozenInput);
            if (reservation == null) {
                continue;
            }
            boolean paidThisTarget = false;
            boolean keptThisTarget = false;
            try {
                if (!reservation.acquired()) {
                    lastReason = reasonOf(reservation.reason());
                    continue;
                }
                if (!MagicPool.spendInto(receipt, caster, unitCost)) {
                    paymentShort = true;
                    break;
                }
                paidThisTarget = true;
                PrisonCommitResult committed = reservation.commit(contact, frozenInput);
                if (committed != null && committed.committed()) {
                    keptThisTarget = true;
                    receipt.retainShare(unitCost);
                    successCount++;
                } else {
                    receipt.refundShare(caster, unitCost);
                    paidThisTarget = false;
                    lastReason = reasonOf(committed == null ? "" : committed.reason());
                }
            } catch (RuntimeException failure) {
                if (paidThisTarget && !keptThisTarget) {
                    receipt.refundShare(caster, unitCost);
                }
                DebugLog.log("群体水牢单目标失败：" + failure);
                lastReason = "水牢没有成立";
            } finally {
                reservation.close();
            }
        }
        if (successCount > 0) {
            MagicPool.showBalance(caster);
            return ConditionalResult.committed();
        }
        if (paymentShort) {
            return ConditionalResult.rejected("法力不足");
        }
        if (lastReason.isEmpty()) {
            return ConditionalResult.rejected("水牢没有成立");
        }
        return ConditionalResult.rejected(lastReason);
    }

    private static int wetTier(StatusCandidate candidate) {
        if (candidate == null || candidate.resources() == null) {
            return 0;
        }
        ResourceView view = candidate.resources().get(WETNESS);
        if (view == null || view.value() <= 0) {
            return 0;
        }
        return view.tier();
    }

    private static String reasonOf(String reason) {
        return reason == null ? "" : reason;
    }
}
