package com.bslx.XueXiMod.shu.technique.release.multicast;

/**
 * 一条被忽略的原始装配下标，以及原因。下标属于 {@code Skill.getTechniques()}。
 */
public record MulticastIgnore(int slotIndex, MulticastIgnoreReason reason) {
    public MulticastIgnore {
        if (reason == null) {
            throw new IllegalArgumentException("忽略原因不能为空");
        }
    }
}
