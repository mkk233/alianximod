package com.bslx.XueXiMod.shu.technique.release.selfstatus;

import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.fire.FireVisualRelay;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceChange;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceMath;
import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import com.bslx.XueXiMod.visual.fire.FireVisualLayout;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * 自身状态一次提交。先预检炉心和魔力，扣款成功后才改炉心。
 * 炉心没改成则退回这笔魔力。失败不进冷却。冷却由调用方只在提交成功后标记。
 * 成功才发一次视觉提示。失败、死亡和被动结束不发蒸汽。
 */
public final class SelfStatusCommit {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Result {
        COMMITTED,
        REJECTED,
        INSUFFICIENT
    }

    private SelfStatusCommit() {
    }

    public static Result attempt(ServerPlayer player, ReleaseDefinition definition) {
        SelfStatusCast status = definition == null ? null : definition.selfStatus();
        if (player == null || status == null || definition.kind() != ReleaseKind.SELF_STATUS) {
            return Result.REJECTED;
        }
        FurnaceMath.Action furnaceAction = status.action();
        if (furnaceAction == null || furnaceAction == FurnaceMath.Action.END) {
            return Result.REJECTED;
        }
        int cost = definition.explicitManaCost();
        if (cost < 0) {
            cost = 0;
        }
        FurnaceChange preview = FurnaceService.preview(player, furnaceAction);
        boolean previewReady = preview != null && preview.succeeded();
        boolean affordable = previewReady && MagicPool.canAfford(player, cost);
        ManaPaymentReceipt receipt = null;
        if (affordable && cost > 0) {
            receipt = ManaPaymentReceipt.charge(player, cost, false);
        }
        boolean paid = previewReady && affordable && (cost <= 0 || receipt != null);
        if (SelfStatusOrder.decide(previewReady, paid, false) == SelfStatusOrder.Step.PREVIEW_REJECT) {
            return Result.REJECTED;
        }
        if (!paid) {
            return Result.INSUFFICIENT;
        }
        FurnaceChange committed;
        try {
            committed = commit(player, furnaceAction);
        } catch (RuntimeException failure) {
            if (receipt != null) {
                receipt.refund(player);
            }
            throw failure;
        }
        boolean furnaceReady = committed != null && committed.succeeded();
        if (SelfStatusOrder.decide(true, true, furnaceReady) == SelfStatusOrder.Step.REFUND) {
            LOGGER.error("self status furnace commit failed after mana charge for {}", player.getUUID());
            if (receipt != null) {
                receipt.refund(player);
            }
            return Result.REJECTED;
        }
        if (cost > 0) {
            MagicPool.showBalance(player);
        }
        notice(player, committed.outcome());
        return Result.COMMITTED;
    }

    private static FurnaceChange commit(ServerPlayer player, FurnaceMath.Action furnaceAction) {
        return switch (furnaceAction) {
            case ENTER_NORMAL -> FurnaceService.enterNormal(player);
            case ENTER_OVERDRIVE -> FurnaceService.enterOverdrive(player);
            case COOL -> FurnaceService.cool(player);
            case END -> new FurnaceChange(FurnaceMath.Outcome.REJECT_BLOCKED);
        };
    }

    /** 只在这次提交成功后通知。升级不重播升温，冷却不由死亡或被动移除触发。 */
    private static void notice(ServerPlayer player, FurnaceMath.Outcome outcome) {
        int cue = switch (outcome) {
            case ENTERED_NORMAL -> FireVisualLayout.HEAT_LOOP;
            case ENTERED_OVERDRIVE, UPGRADED -> FireVisualLayout.OVERDRIVE_LOOP;
            case COOLED -> FireVisualLayout.COOL_ONCE;
            default -> 0;
        };
        if (cue != 0) {
            FireVisualRelay.furnace(player, cue);
        }
    }
}
