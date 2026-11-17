package com.bslx.XueXiMod.animation.runtime;

import java.util.UUID;

import com.bslx.XueXiMod.network.ActionSyncS2CPacket;

/**
 * 客户端播放准入。生产路径和回归都调用这里，不引用客户端类型。
 * 连接用调用方传入的身份比较；世界是否更换看实例，不看维度名字。
 */
public final class ActionPlaybackGate {
    private ActionPlaybackGate() {
    }

    public static boolean sameLink(Object liveLink, Object packetLink) {
        return liveLink != null && packetLink != null && liveLink == packetLink;
    }

    public static boolean sameLevel(Object boundLevel, Object liveLevel) {
        return boundLevel != null && boundLevel == liveLevel;
    }

    public static boolean replaceWorld(Object boundLevel, Object liveLevel, Object boundLink, Object liveLink) {
        if (liveLevel == null || liveLink == null) {
            return boundLevel != null || boundLink != null;
        }
        return liveLevel != boundLevel || liveLink != boundLink;
    }

    public static boolean entityApplies(UUID viewOwner, int viewEntityId, UUID liveOwner, int liveEntityId, boolean entityPresent) {
        return entityPresent && viewOwner != null && viewOwner.equals(liveOwner) && viewEntityId == liveEntityId;
    }

    public static boolean admit(
            boolean hasView,
            long viewEpoch,
            long viewSequence,
            int viewRevision,
            boolean viewTerminal,
            boolean viewCancelled,
            long packetEpoch,
            long packetSequence,
            int packetRevision,
            int packetKind) {
        if (hasView && packetEpoch < viewEpoch) {
            return false;
        }
        if (hasView && packetEpoch == viewEpoch && packetSequence < viewSequence) {
            return false;
        }
        boolean sameLife = hasView && packetEpoch == viewEpoch && packetSequence == viewSequence;
        if (sameLife && packetKind == ActionSyncS2CPacket.START) {
            return false;
        }
        if (sameLife && viewCancelled) {
            return false;
        }
        if (sameLife && viewTerminal && packetKind != ActionSyncS2CPacket.CANCEL) {
            return false;
        }
        if (sameLife && packetRevision < viewRevision) {
            return false;
        }
        return !(sameLife && packetRevision == viewRevision && packetKind != ActionSyncS2CPacket.CANCEL);
    }
}
