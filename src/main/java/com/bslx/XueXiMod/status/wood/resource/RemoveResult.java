package com.bslx.XueXiMod.status.wood.resource;

/** 主动丢弃结果。成功也不发岁华。 */
public record RemoveResult(boolean accepted, WoodReject reject) {
    public static RemoveResult rejected(WoodReject reject) {
        return new RemoveResult(false, reject);
    }

    public static RemoveResult success() {
        return new RemoveResult(true, WoodReject.NONE);
    }
}
