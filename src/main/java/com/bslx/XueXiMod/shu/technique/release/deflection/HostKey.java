package com.bslx.XueXiMod.shu.technique.release.deflection;

import java.util.UUID;

/**
 * 稳定宿主身份。生成号用来换代，不用队列视觉序号，也不用三分复制号。
 */
public record HostKey(UUID hostId, long generation) implements Comparable<HostKey> {
    public HostKey {
        if (hostId == null) {
            throw new IllegalArgumentException("宿主编号不能空");
        }
    }

    @Override
    public int compareTo(HostKey other) {
        int identityOrder = this.hostId.compareTo(other.hostId);
        if (identityOrder != 0) {
            return identityOrder;
        }
        return Long.compare(this.generation, other.generation);
    }
}
