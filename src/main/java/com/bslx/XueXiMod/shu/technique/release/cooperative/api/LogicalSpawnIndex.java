package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 逻辑副本和生成序号到密集结算序号的映射。两个演员写同一个 settlementOrdinal。
 * 禁止用副本号加生成号乘 1024 代替这份显式映射。
 */
public record LogicalSpawnIndex(int memberOrdinal, int copyOrdinal, int spawnOrdinal, int settlementOrdinal) {
    public LogicalSpawnIndex {
        if (memberOrdinal < 0 || copyOrdinal < 0 || spawnOrdinal < 0 || settlementOrdinal < 0) {
            throw new IllegalArgumentException("逻辑生成索引不能为负");
        }
    }
}
