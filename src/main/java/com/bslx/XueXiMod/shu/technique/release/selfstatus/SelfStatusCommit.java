package com.bslx.XueXiMod.shu.technique.release.selfstatus;

import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * 自身状态一次提交。先预检再二次校验，通过后才扣魔力，然后安装。
 * 预检或二次校验失败不收费。安装失败或安装抛错则退回这笔魔力。
 * 安装已经成功后，视觉失败只记日志，不退款，也不把这次提交改成失败。
 * 首次预检明确为当前状态不适用时，在扣款前返回，不进入复核。
 * 冷却由调用方只在 {@link Result#COMMITTED} 之后标记。
 * {@link #playerText()} 只在本次调用的线程上、返回之后立刻读取。
 */
public final class SelfStatusCommit {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String UNBOUND_TEXT = "§c自身状态没有就绪";
    public static final String INSUFFICIENT_TEXT = "§c法力不足";
    private static final ThreadLocal<String> PLAYER_TEXT = new ThreadLocal<>();

    public enum Result {
        COMMITTED,
        REJECTED,
        INSUFFICIENT,
        NOT_APPLICABLE
    }

    private SelfStatusCommit() {
    }

    /** 读取本次调用留给玩家的提示。成功提交后为空提示。 */
    public static String playerText() {
        String shown = PLAYER_TEXT.get();
        if (shown == null || shown.isBlank()) {
            return "§c自身状态没有改变";
        }
        return shown;
    }

    /** 预检、复核、扣款、安装。安装前失败不收费，安装失败退款，视觉失败不退款。 */
    public static Result attempt(ServerPlayer player, ReleaseDefinition definition) {
        PLAYER_TEXT.remove();
        if (player == null || definition == null || definition.kind() != ReleaseKind.SELF_STATUS) {
            return reject(UNBOUND_TEXT);
        }
        SelfStatusCast status = definition.selfStatus();
        if (status == null) {
            return reject(UNBOUND_TEXT);
        }
        StatusPreparation preparation = status.prepare(player);
        if (preparation != null && preparation.notApplicable()) {
            return notApplicable(preparation.rejectionText());
        }
        boolean previewReady = preparation != null && preparation.ready() && preparation.change() != null;
        PreparedStatusChange prepared = previewReady ? preparation.change() : null;
        String recheckText = null;
        boolean recheckReady = false;
        if (previewReady) {
            recheckText = prepared.recheck(player);
            recheckReady = recheckText == null;
        }
        int rawCost = definition.explicitManaCost();
        if (rawCost < 0) {
            rawCost = 0;
        }
        int cost = SpecialBalance.mana(definition.id(), BalanceKeys.MANA_COST, rawCost);
        boolean affordable = previewReady && recheckReady && MagicPool.canAfford(player, cost);
        ManaPaymentReceipt receipt = null;
        if (affordable && cost > 0) {
            receipt = ManaPaymentReceipt.charge(player, cost, false);
        }
        boolean paid = affordable && (cost <= 0 || receipt != null);
        // 第四个参数在安装前固定为 false。此时返回的 REFUND 只表示还没安装，这一步不退款。
        SelfStatusOrder.Step gateStep = SelfStatusOrder.decide(previewReady, recheckReady, paid, false);
        if (gateStep == SelfStatusOrder.Step.PREVIEW_REJECT) {
            String rejectionText = preparation == null ? UNBOUND_TEXT : preparation.rejectionText();
            return reject(rejectionText);
        }
        if (gateStep == SelfStatusOrder.Step.RECHECK_REJECT) {
            return reject(recheckText);
        }
        if (!paid) {
            PLAYER_TEXT.set(INSUFFICIENT_TEXT);
            return Result.INSUFFICIENT;
        }
        boolean installed;
        try {
            installed = prepared.commit(player);
        } catch (RuntimeException failure) {
            if (receipt != null) {
                receipt.refund(player);
            }
            throw failure;
        }
        if (SelfStatusOrder.decide(true, true, true, installed) == SelfStatusOrder.Step.REFUND) {
            LOGGER.error("self status install failed after mana charge for {}", player.getUUID());
            if (receipt != null) {
                receipt.refund(player);
            }
            return reject(prepared.failureText());
        }
        if (cost > 0) {
            MagicPool.showBalance(player);
        }
        try {
            prepared.afterCommit(player);
        } catch (RuntimeException visualFailure) {
            LOGGER.error("self status visual failed after commit for {}", player.getUUID(), visualFailure);
        }
        PLAYER_TEXT.remove();
        return Result.COMMITTED;
    }

    private static Result reject(String playerText) {
        String shown = playerText == null || playerText.isBlank() ? "§c自身状态没有改变" : playerText;
        PLAYER_TEXT.set(shown);
        return Result.REJECTED;
    }

    private static Result notApplicable(String playerText) {
        String shown = playerText == null || playerText.isBlank() ? "§c自身状态没有改变" : playerText;
        PLAYER_TEXT.set(shown);
        return Result.NOT_APPLICABLE;
    }
}
