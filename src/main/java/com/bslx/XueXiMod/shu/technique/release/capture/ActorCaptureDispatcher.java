package com.bslx.XueXiMod.shu.technique.release.capture;

import com.bslx.XueXiMod.item.StaffItem;
import com.bslx.XueXiMod.item.StaffSkills;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.balance.BalanceException;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.technique.ChainStructureEpoch;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadAdapter;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.ActorPayloadRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.CaptureQuote;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.CaptureQuoteContext;
import com.bslx.XueXiMod.shu.technique.release.clone.capture.PayloadQuoteFault;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CapturedAttackSnapshot;
import com.bslx.XueXiMod.shu.technique.release.clone.core.ClonePositionPolicy;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneResults;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneService;
import com.bslx.XueXiMod.shu.technique.release.clone.core.PreparedCapture;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneSpawnAuthority;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 捕获部署的唯一入口。报价交给载荷适配器，付款和发布交给分身服务，这里不复制支付事务。
 */
public final class ActorCaptureDispatcher {
    private static final double BODY_HEALTH = 1.0D;
    private static final int RESIDENCE_UPPER = 1_200_000;
    private static final double DECOY_UPPER = 16.0D;

    private ActorCaptureDispatcher() {
    }

    public static CaptureDispatchResult tryExecute(ServerPlayer owner, Skill skill,
                                                    CapturedStructurePlan plan, CapturedInput input) {
        if (owner == null || input == null || skill == null || plan == null) {
            return refused(CaptureRefusal.CAPTURE_OWNER_UNAVAILABLE, ActorCaptureTexts.OWNER);
        }
        if (owner.getServer() == null || !owner.getServer().isSameThread()) {
            return refused(CaptureRefusal.CAPTURE_OWNER_UNAVAILABLE, ActorCaptureTexts.OWNER);
        }
        if (CaptureAcceptanceLedger.alreadyAccepted(owner.getUUID(), owner.getServer(), input.serverGameTick())) {
            return refused(CaptureRefusal.CAPTURE_TICK_LIMIT, ActorCaptureTexts.TICK_LIMIT);
        }
        if (!owner.isAlive() || owner.isRemoved() || owner.hasDisconnected()) {
            return refused(CaptureRefusal.CAPTURE_OWNER_UNAVAILABLE, ActorCaptureTexts.OWNER);
        }
        ItemStack hand = owner.getMainHandItem();
        boolean liveStaff = StaffItem.holds(hand) && input.slotIndex() == 0;
        if (!liveStaff || !input.staffCarrier()) {
            return refused(CaptureRefusal.CAPTURE_REQUIRES_STAFF, ActorCaptureTexts.STAFF_ONLY);
        }
        if (StaffSkills.rawEntryFault(hand) != null || !StaffSkills.matchesSkill(hand, skill)) {
            String rawFault = StaffSkills.rawEntryFault(hand);
            return refused(CaptureRefusal.CAPTURE_COMPLEX_CHAIN,
                    rawFault == null ? ActorCaptureTexts.CHAIN_MISMATCH : rawFault);
        }
        ActorCaptureInspection freshInspection = ActorCaptureCompiler.inspect(skill);
        if (freshInspection.status() != ActorCaptureStatus.PLAN || freshInspection.plan() == null
                || !samePlan(freshInspection.plan(), plan)
                || plan.structureEpoch() != ChainStructureEpoch.current()) {
            return refused(CaptureRefusal.CAPTURE_ADAPTER_CHANGED, ActorCaptureTexts.ADAPTER);
        }
        CaptureCarrierCheck carrier = ActorCaptureCarrierGate.inspect(skill, true, false);
        if (carrier.decision() != CaptureCarrierDecision.READY || carrier.plan() == null) {
            CaptureRefusal refusal = carrier.refusal() == null
                    ? CaptureRefusal.CAPTURE_COMPLEX_CHAIN : carrier.refusal();
            String playerText = carrier.playerText().isBlank() ? ActorCaptureTexts.STRUCTURE : carrier.playerText();
            return refused(refusal, playerText);
        }
        int payloadIndex = plan.payloadOriginalIndexes().get(0);
        Technique host = skill.getTechniques().get(plan.captureOriginalIndex());
        Technique payload = skill.getTechniques().get(payloadIndex);
        ReleaseDefinition hostDefinition = ReleaseRegistry.get(host.getId());
        ActorPayloadAdapter adapter = ActorPayloadRegistry.find(payload.getId());
        if (hostDefinition == null || adapter == null || adapter.version() != plan.adapterVersion()) {
            return refused(CaptureRefusal.CAPTURE_ADAPTER_CHANGED, ActorCaptureTexts.ADAPTER);
        }
        if (!finite(input.feetPosition()) || !finite(input.eyePosition()) || !ClonePositionPolicy.unit(input.lookUnit())) {
            return refused(CaptureRefusal.CAPTURE_GEOMETRY_INVALID, ActorCaptureTexts.GEOMETRY);
        }
        CaptureQuote quote = adapter.freezeQuote(new CaptureQuoteContext(
                payload,
                input.weaponCopy(),
                input.weaponBonus(),
                input.attributeDamageBonus(),
                plan.embryoCopy(),
                plan.structureEpoch()));
        if (quote == null || !quote.accepted()) {
            return refused(mapFault(quote == null ? null : quote.fault()),
                    quote == null || quote.playerText().isBlank()
                            ? ActorCaptureTexts.STRUCTURE : quote.playerText());
        }
        if (quote.adapterVersion() != adapter.version() || quote.configuredDamage() == null) {
            return refused(CaptureRefusal.CAPTURE_ADAPTER_CHANGED, ActorCaptureTexts.ADAPTER);
        }
        int spawnMana;
        int spawnCooldown;
        int residenceTicks;
        double decoyRadius;
        try {
            spawnMana = BalanceService.manaCost(BalanceSide.SERVER, host.getId(), BalanceKeys.MANA_COST,
                    hostDefinition.explicitManaCost());
            spawnCooldown = BalanceService.cooldownTicks(BalanceSide.SERVER, host.getId(), host.getBaseCooldown());
            residenceTicks = BalanceService.intValue(BalanceSide.SERVER, host.getId(), BalanceKeys.LIFETIME_TICKS);
            decoyRadius = BalanceService.number(BalanceSide.SERVER, host.getId(), CloneBalance.DECOY_KEY);
        } catch (BalanceException unused) {
            return refused(CaptureRefusal.CAPTURE_INVALID_QUOTE, ActorCaptureTexts.QUOTE_UNAVAILABLE);
        }
        Integer totalMana = narrowNonNegative(spawnMana, quote.configuredMana());
        if (totalMana == null) {
            return refused(CaptureRefusal.CAPTURE_INVALID_QUOTE, ActorCaptureTexts.QUOTE_OVERFLOW);
        }
        Integer unscaledCooldown = narrowNonNegative(spawnCooldown, quote.configuredCooldownTicks());
        if (unscaledCooldown == null) {
            return refused(CaptureRefusal.CAPTURE_INVALID_QUOTE, ActorCaptureTexts.COOLDOWN_OVERFLOW);
        }
        boolean immediate = input.ownerShift();
        if (immediate) {
            decoyRadius = 0.0D;
        }
        if (residenceTicks < 0 || residenceTicks > RESIDENCE_UPPER
                || !Double.isFinite(decoyRadius) || decoyRadius < 0.0D || decoyRadius > DECOY_UPPER) {
            return refused(CaptureRefusal.CAPTURE_INVALID_QUOTE, ActorCaptureTexts.QUOTE_UNAVAILABLE);
        }
        PreparedCapture prepared;
        try {
            prepared = new PreparedCapture(
                    CloneSpawnAuthority.PLAYER_CAPTURED_CAST,
                    owner.getUUID(),
                    owner.level().dimension(),
                    immediate,
                    input.feetPosition(),
                    input.eyePosition(),
                    input.lookUnit(),
                    totalMana,
                    residenceTicks,
                    decoyRadius,
                    BODY_HEALTH,
                    new CapturedAttackSnapshot(
                            payload.getId(),
                            adapter.version(),
                            plan.structureEpoch(),
                            owner.getUUID(),
                            UUID.randomUUID(),
                            quote.configuredDamage(),
                            quote.depthBlocks(),
                            quote.heightBlocks(),
                            quote.widthBlocks(),
                            quote.lifetimeTicks(),
                            quote.configuredMana(),
                            quote.configuredCooldownTicks()),
                    plan.structureEpoch());
        } catch (IllegalArgumentException unused) {
            return refused(CaptureRefusal.CAPTURE_GEOMETRY_INVALID, ActorCaptureTexts.GEOMETRY);
        }
        CloneResults.SpawnResult spawned = CloneService.spawnCaptured(owner, prepared);
        if (spawned == null || !spawned.accepted()) {
            return mapSpawn(spawned);
        }
        CaptureAcceptanceLedger.record(owner.getUUID(), owner.getServer(), input.serverGameTick());
        return new CaptureDispatchResult(true, false, null, "", host, unscaledCooldown, spawned.handle());
    }

    private static boolean samePlan(CapturedStructurePlan fresh, CapturedStructurePlan offered) {
        return fresh.captureOriginalIndex() == offered.captureOriginalIndex()
                && fresh.nextOriginalIndex() == offered.nextOriginalIndex()
                && fresh.adapterVersion() == offered.adapterVersion()
                && fresh.structureEpoch() == offered.structureEpoch()
                && fresh.payloadTechniqueId().equals(offered.payloadTechniqueId())
                && fresh.adapterKey().equals(offered.adapterKey())
                && fresh.payloadOriginalIndexes().equals(offered.payloadOriginalIndexes())
                && fresh.consumedOriginalIndexes().equals(offered.consumedOriginalIndexes())
                && fresh.chargedOriginalIndexes().equals(offered.chargedOriginalIndexes());
    }

    private static CaptureDispatchResult mapSpawn(CloneResults.SpawnResult spawned) {
        CloneResults.RejectReason reason = spawned == null ? null : spawned.reason();
        if (reason == CloneResults.RejectReason.INSUFFICIENT_MANA) {
            return new CaptureDispatchResult(false, true, null, "法力不足", null, 0, null);
        }
        if (reason == CloneResults.RejectReason.CAP_REACHED) {
            return refused(CaptureRefusal.CAPTURE_CAP_REACHED, ActorCaptureTexts.CAPACITY);
        }
        if (reason == CloneResults.RejectReason.OWNER_UNAVAILABLE) {
            return refused(CaptureRefusal.CAPTURE_OWNER_UNAVAILABLE, ActorCaptureTexts.OWNER);
        }
        if (reason == CloneResults.RejectReason.INVALID_GEOMETRY) {
            return refused(CaptureRefusal.CAPTURE_GEOMETRY_INVALID, ActorCaptureTexts.GEOMETRY);
        }
        if (reason == CloneResults.RejectReason.INVALID_QUOTE) {
            return refused(CaptureRefusal.CAPTURE_INVALID_QUOTE, ActorCaptureTexts.QUOTE_OVERFLOW);
        }
        return refused(CaptureRefusal.CAPTURE_OWNER_UNAVAILABLE, ActorCaptureTexts.OWNER);
    }

    private static CaptureRefusal mapFault(PayloadQuoteFault fault) {
        if (fault == PayloadQuoteFault.GEOMETRY) {
            return CaptureRefusal.CAPTURE_GEOMETRY_INVALID;
        }
        if (fault == PayloadQuoteFault.INVALID_QUOTE) {
            return CaptureRefusal.CAPTURE_INVALID_QUOTE;
        }
        if (fault == PayloadQuoteFault.ADAPTER_MISMATCH) {
            return CaptureRefusal.CAPTURE_ADAPTER_CHANGED;
        }
        return CaptureRefusal.CAPTURE_UNSUPPORTED_PAYLOAD;
    }

    private static Integer narrowNonNegative(int leftAmount, int rightAmount) {
        long combined = (long) leftAmount + (long) rightAmount;
        if (leftAmount < 0 || rightAmount < 0 || combined < 0L || combined > Integer.MAX_VALUE) {
            return null;
        }
        return Integer.valueOf((int) combined);
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static CaptureDispatchResult refused(CaptureRefusal refusal, String playerText) {
        return new CaptureDispatchResult(false, false, refusal, playerText, null, 0, null);
    }
}
