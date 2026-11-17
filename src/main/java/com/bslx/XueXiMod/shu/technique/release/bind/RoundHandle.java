package com.bslx.XueXiMod.shu.technique.release.bind;

import java.util.UUID;

/**
 * 一轮自动释放。成功、失败、缺魔和取消都可以结束。
 * 取消对已结束的轮次没有副作用，并且只结清一次未提交份额。
 */
public interface RoundHandle {
    UUID roundId();

    /** 结束还没完成的轮次。已经结束时不再结清。 */
    void cancel();

    /** 这一轮是不是被取消结束的。 */
    boolean cancelled();

    /** 成功、失败、缺魔或取消都算已经结束。 */
    boolean done();
}
