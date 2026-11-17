package com.bslx.XueXiMod.animation.runtime;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一次已提交攻击组的播放许可。只保存主人和组的编号，以及是否已经消费。
 * 构造本身不证明发生过攻击；已提交回执的桥负责创建并缓存这一份。
 */
public final class CommitPermit {
    private final UUID ownerId;
    private final UUID groupId;
    private final AtomicBoolean consumed = new AtomicBoolean(false);

    public CommitPermit(UUID ownerId, UUID groupId) {
        if (ownerId == null || groupId == null) {
            throw new IllegalArgumentException("commit permit is missing");
        }
        this.ownerId = ownerId;
        this.groupId = groupId;
    }

    public UUID ownerId() {
        return this.ownerId;
    }

    public UUID groupId() {
        return this.groupId;
    }

    public boolean consumed() {
        return this.consumed.get();
    }

    public boolean consume() {
        return this.consumed.compareAndSet(false, true);
    }
}
