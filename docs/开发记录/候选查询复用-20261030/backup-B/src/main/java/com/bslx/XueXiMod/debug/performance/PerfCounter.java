package com.bslx.XueXiMod.debug.performance;

/**
 * 首版固定计数。只累加调用方给出的非负增量；零和负数在记录器内无作用。
 * 计数不得再执行一次业务表达式。
 */
public enum PerfCounter {
    /** 实际执行一次目标类 getEntitiesOfClass 加一。缓存命中不加。与实体查询次数互斥。 */
    TARGET_QUERY_CALLS,
    /** 实际执行一次实体类 getEntitiesOfClass 加一。缓存命中不加。 */
    ENTITY_QUERY_CALLS,
    /** 目标查询返回列表的 size 累计，含谓词过滤后的结果，不是索引内部扫描量。 */
    TARGET_CANDIDATES,
    /** 实体查询返回列表的 size 累计。重复访问照实相加，不是唯一目标数。 */
    ENTITY_CANDIDATES,
    /** 每次 owned 扫描加一。 */
    SWORD_OWNED_SCAN_CALLS,
    /** owned 扫描实际访问的实体个数。anyOwnedBy 早退只计已访问的实体。 */
    SWORD_OWNED_SCANNED,
    /** 攻击体服务端 tick 入口次数，含休眠和等待激活。不是活跃实体总数。 */
    HIT_ENTITY_UPDATES,
    /** 剑服务端 tick 入口次数，含休眠和等待激活。不是活跃实体总数。 */
    SWORD_UPDATES,
    /** spawnReported 原循环计划次数，与现有 requested 口径一致，只累加一次。 */
    HIT_SPAWN_REQUESTED,
    /** 成功 new TechniqueHitEntity 后加一。 */
    HIT_SPAWN_CONSTRUCTED,
    /** addFreshEntity 返回 true 后加一。不得为了计数再调用一次该方法。 */
    HIT_SPAWN_ACCEPTED,
    /** 仅 PhantomSwordEffect 的计划次数。 */
    SWORD_SPAWN_REQUESTED,
    /** 仅 PhantomSwordEffect 成功构造后加一。 */
    SWORD_SPAWN_CONSTRUCTED,
    /** 仅 PhantomSwordEffect 被世界接受后加一。 */
    SWORD_SPAWN_ACCEPTED,
    /** 实际窄判定次数。保留原短路，不为收集结果改变求值。 */
    NARROW_PHASE_TESTS,
    /** 上述窄判定通过的次数。 */
    NARROW_PHASE_PASSED,
    /** 已插桩的 target.hurt 调用次数。不是 tryHit、Tally.record 或扣血量。 */
    DAMAGE_ATTEMPTS,
    /** target.hurt 原布尔返回 true 的次数。 */
    DAMAGE_ACCEPTED,
    /** 显式事件、声音、粒子调用次数。不是数据包数或字节数。 */
    EXPLICIT_SEND_CALLS,
    /** 结构计划查找命中已有条目。一次 lookup 只记这一个终态。 */
    CAST_PLAN_HITS,
    /** 合格且可以入缓存，但没有条目。一次 lookup 只记这一个终态。成功编译入缓存另计 CAST_PLAN_COMPILES。 */
    CAST_PLAN_MISSES,
    /** 成功完整编译并放入缓存的次数。与本次 miss 分开累加，不代替 miss。 */
    CAST_PLAN_COMPILES,
    /** 无资格、过长、超预算、未绑定或线程不符。预算超限只记此项，不先记 miss。 */
    CAST_PLAN_BYPASSES
}
