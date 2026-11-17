package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 一个逻辑副本上的一次生成。演员序号不在这把钥匙里。 */
public record LogicalCopyKey(MemberKey member, int copyOrdinal, int spawnOrdinal) {
    public LogicalCopyKey {
        if (member == null) {
            throw new IllegalArgumentException("逻辑副本缺少成员");
        }
        if (copyOrdinal < 0 || spawnOrdinal < 0) {
            throw new IllegalArgumentException("逻辑副本序号不能为负");
        }
    }
}
