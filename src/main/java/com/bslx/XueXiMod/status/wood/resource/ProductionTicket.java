package com.bslx.XueXiMod.status.wood.resource;

import java.util.UUID;

/**
 * 服务签发的单次生产票据。没有公开构造器。
 * 只有同一次 begin 交回的对象能发布一次；旧编号、另一只对象或存档都不会重新获得预算。
 */
public final class ProductionTicket {
    private final UUID token;
    private final UUID ownerId;
    private final ResourceOrigin origin;
    private final long serial;
    private final int issuedTick;

    private ProductionTicket(UUID token, UUID ownerId, ResourceOrigin origin, long serial, int issuedTick) {
        this.token = token;
        this.ownerId = ownerId;
        this.origin = origin;
        this.serial = serial;
        this.issuedTick = issuedTick;
    }

    static ProductionTicket issue(UUID token, UUID ownerId, ResourceOrigin origin, long serial, int issuedTick) {
        return new ProductionTicket(token, ownerId, origin, serial, issuedTick);
    }

    UUID token() {
        return token;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public ResourceOrigin origin() {
        return origin;
    }

    /** 持久单调批次号。发布失败也会消耗，不能再发给下一次 begin。 */
    public long serial() {
        return serial;
    }

    /** 签发时的服务器 tick。跨 tick 再发布会失败并作废。 */
    public int issuedTick() {
        return issuedTick;
    }
}
