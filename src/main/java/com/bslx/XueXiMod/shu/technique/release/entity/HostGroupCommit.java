package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.animation.runtime.ActionCommit;
import com.bslx.XueXiMod.animation.runtime.ActionRuntime;
import com.bslx.XueXiMod.animation.runtime.CommitPermit;
import com.bslx.XueXiMod.shu.technique.release.bind.PublicationAttempt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** 同一个宿主组只终结一次。回执记在父释放上，重复调用直接交回已经记下的那张。 */
public final class HostGroupCommit {
    private HostGroupCommit() {
    }

    /** 这个宿主组已经记下的回执。没有父释放时为空。 */
    public static HostGroupReceipt find(HostCast hostCast, UUID groupId) {
        if (hostCast == null) {
            return null;
        }
        return hostCast.existing(groupId);
    }

    /** 已有回执则交回原件，否则记下拒绝。 */
    public static HostGroupReceipt reject(HostCast hostCast, UUID groupId, int tick, String reason) {
        if (hostCast == null) {
            return HostGroupReceipt.rejected(groupId, tick, reason);
        }
        HostGroupReceipt prior = hostCast.existing(groupId);
        if (prior != null) {
            return prior;
        }
        return hostCast.remember(groupId, HostGroupReceipt.rejected(groupId, tick, reason));
    }

    /** 已有回执则交回原件，否则记下取消。 */
    public static HostGroupReceipt cancel(HostCast hostCast, UUID groupId, int tick, String reason) {
        if (hostCast == null) {
            return HostGroupReceipt.cancelled(groupId, tick, reason);
        }
        HostGroupReceipt prior = hostCast.existing(groupId);
        if (prior != null) {
            return prior;
        }
        return hostCast.remember(groupId, HostGroupReceipt.cancelled(groupId, tick, reason));
    }

    /**
     * 外层准备一次。{@code body} 里的复制份读同一个上下文。
     * 全部必需生成成功才提交并通知一次；失败、重复和父释放结束后的迟到调用都不生成。
     */
    public static HostGroupReceipt commitOnce(HostCast hostCast, ServerPlayer player, UUID groupId, int requiredSpawns,
                                               int spawnCount, HostGroupListener listener, Runnable body) {
        int nowTick = player != null && player.getServer() != null ? player.getServer().getTickCount() : 0;
        if (hostCast == null) {
            return HostGroupReceipt.rejected(groupId, nowTick, "宿主组没有父释放");
        }
        HostGroupReceipt blocked = hostCast.priorOrBlock(groupId, nowTick);
        if (blocked != null) {
            return blocked;
        }
        if (player == null || !player.isAlive()) {
            return cancel(hostCast, groupId, nowTick, "施法者已经离开");
        }
        HostGroupContext context = HostGroupContext.open(hostCast, groupId, Math.max(0, requiredSpawns), Math.max(1, spawnCount), listener);
        HostGroupContext.bind(context);
        try {
            if (body != null) {
                body.run();
            }
        } finally {
            HostGroupContext.unbind(context);
            HostBeatHooks.beforeActivation(player, context);
            boolean ownerLeft = !player.isAlive();
            HostGroupReceipt sealed = context.seal(player, nowTick, ownerLeft);
            hostCast.remember(groupId, sealed);
            HostGroupReceipt stored = hostCast.existing(groupId);
            HostGroupListener userListener = context.listener();
            if (stored != null && stored.committed()) {
                PublicationAttempt.keepOpenHostShare();
                stored.notifyCommit(receipt -> {
                    ResourceLocation candidate = context.actionCandidate();
                    if (candidate != null) {
                        CommitPermit permit = stored.commitPermit(player.getUUID());
                        if (permit != null) {
                            try {
                                ActionRuntime.notifyAttackGroup(player, permit, ActionCommit.group(receipt.groupId(), candidate));
                            } catch (RuntimeException ignored) {
                                // 视觉失败不回滚已经提交的攻击组
                            }
                        }
                    }
                    if (userListener != null) {
                        userListener.onCommitted(receipt);
                    }
                    HostBeatHooks.onCommitted(player, context, receipt);
                });
            } else {
                HostBeatHooks.onAbandoned(context);
            }
        }
        return hostCast.existing(groupId);
    }
}
