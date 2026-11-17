package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.animation.runtime.CommitPermit;

import java.util.UUID;

/** 一个宿主组的一次终结回执。提交监听只在成功时发一次。 */
public final class HostGroupReceipt {
    public enum Kind {
        COMMITTED,
        REJECTED,
        CANCELLED
    }

    private final UUID groupId;
    private final Kind kind;
    private final int tick;
    private final String reason;
    private boolean notified;
    private CommitPermit permit;

    private HostGroupReceipt(UUID groupId, Kind kind, int tick, String reason) {
        this.groupId = groupId;
        this.kind = kind;
        this.tick = tick;
        this.reason = reason == null ? "" : reason;
    }

    public static HostGroupReceipt committed(UUID groupId, int tick) {
        return new HostGroupReceipt(groupId, Kind.COMMITTED, tick, "");
    }

    public static HostGroupReceipt rejected(UUID groupId, int tick, String reason) {
        return new HostGroupReceipt(groupId, Kind.REJECTED, tick, reason);
    }

    public static HostGroupReceipt cancelled(UUID groupId, int tick, String reason) {
        return new HostGroupReceipt(groupId, Kind.CANCELLED, tick, reason);
    }

    public UUID groupId() {
        return groupId;
    }

    public Kind kind() {
        return kind;
    }

    public int tick() {
        return tick;
    }

    public String reason() {
        return reason;
    }

    public boolean committed() {
        return kind == Kind.COMMITTED;
    }

    /** 只有成功回执会通知，并且同一个回执只通知一次。 */
    public boolean notifyCommit(HostGroupListener listener) {
        if (kind != Kind.COMMITTED || notified || listener == null) {
            return false;
        }
        notified = true;
        listener.onCommitted(this);
        return true;
    }

    /**
     * 只有成功回执才发播放许可，并且同一张回执永远交回同一份。
     * 构造许可不表示已经攻击过；调用点必须是这次成功回调。
     */
    public CommitPermit commitPermit(UUID ownerId) {
        if (kind != Kind.COMMITTED || ownerId == null || groupId == null) {
            return null;
        }
        synchronized (this) {
            if (permit == null) {
                permit = new CommitPermit(ownerId, groupId);
            }
            return permit;
        }
    }
}
