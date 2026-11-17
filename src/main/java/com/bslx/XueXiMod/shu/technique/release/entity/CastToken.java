package com.bslx.XueXiMod.shu.technique.release.entity;

import java.util.UUID;

/** 一次被接受的独立释放。重试沿用同一枚，不在每个回调里重发。 */
public final class CastToken {
    private final UUID tokenId;

    private CastToken(UUID tokenId) {
        this.tokenId = tokenId;
    }

    /** 发一枚新的独立释放标识。 */
    public static CastToken open() {
        return new CastToken(UUID.randomUUID());
    }

    public UUID tokenId() {
        return tokenId;
    }
}
