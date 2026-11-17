package com.bslx.XueXiMod.shu.technique.release.entity;

import com.bslx.XueXiMod.shu.technique.release.water.CastSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一次父释放或父会话持有的宿主回执。
 * 静态表只登记还没结束的父释放，关闭、登出和停服时摘掉。回执本身不进那张表。
 */
public final class HostCast {
    private static final ConcurrentHashMap<UUID, HostCast> OPEN = new ConcurrentHashMap<>();

    private final CastToken token;
    private final UUID ownerId;
    private final CastSource source;
    private final ConcurrentHashMap<UUID, HostGroupReceipt> settled = new ConcurrentHashMap<>();
    private final AtomicBoolean ended = new AtomicBoolean(false);
    private final AtomicInteger holders = new AtomicInteger();

    private HostCast(CastToken token, UUID ownerId, CastSource source) {
        this.token = token;
        this.ownerId = ownerId;
        this.source = source;
    }

    /** 没有载体来源的父释放。 */
    public static HostCast open(UUID ownerId) {
        return open(ownerId, null);
    }

    /** 打开父释放并放进未结束表。 */
    public static HostCast open(UUID ownerId, CastSource source) {
        HostCast cast = new HostCast(CastToken.open(), ownerId, source);
        OPEN.put(cast.token.tokenId(), cast);
        return cast;
    }

    public CastSource source() {
        return source;
    }

    /** 关掉这个主人还开着的父释放。 */
    public static void closeOwned(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        List<HostCast> living = new ArrayList<>(OPEN.values());
        for (HostCast cast : living) {
            if (ownerId.equals(cast.ownerId)) {
                cast.close();
            }
        }
    }

    /** 关掉全部未结束的父释放。 */
    public static void closeAll() {
        List<HostCast> living = new ArrayList<>(OPEN.values());
        for (HostCast cast : living) {
            cast.close();
        }
    }

    public UUID tokenId() {
        return token.tokenId();
    }

    /** 这个成员和波次的宿主号。复制份不进编号。 */
    public UUID hostId(int memberIndex, int volley) {
        return HostGroupIds.castHost(token.tokenId(), memberIndex, volley);
    }

    /** 延迟回调开始时多记一次持有。 */
    public void hold() {
        holders.incrementAndGet();
    }

    /** 持有归零后关闭父释放。 */
    public void release() {
        int remaining = holders.decrementAndGet();
        if (remaining == 0) {
            close();
        }
    }

    /** 从静态表摘掉。重复关闭无效。 */
    public void close() {
        if (!ended.compareAndSet(false, true)) {
            return;
        }
        OPEN.remove(token.tokenId(), this);
    }

    public boolean closed() {
        return ended.get();
    }

    public HostGroupReceipt existing(UUID groupId) {
        if (groupId == null) {
            return null;
        }
        return settled.get(groupId);
    }

    /**
     * 已经有回执，或父释放已经结束时，返回那张终态回执，调用方不得再生成。
     * 还可以生成时返回空。
     */
    public HostGroupReceipt priorOrBlock(UUID groupId, int tick) {
        if (groupId == null) {
            return HostGroupReceipt.cancelled(null, tick, "宿主组没有编号");
        }
        HostGroupReceipt prior = existing(groupId);
        if (prior != null) {
            return prior;
        }
        if (ended.get()) {
            return remember(groupId, HostGroupReceipt.cancelled(groupId, tick, "宿主组已经结束"));
        }
        return null;
    }

    public HostGroupReceipt remember(UUID groupId, HostGroupReceipt receipt) {
        if (groupId == null || receipt == null) {
            return receipt;
        }
        settled.putIfAbsent(groupId, receipt);
        return settled.get(groupId);
    }
}
