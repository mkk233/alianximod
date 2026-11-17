package com.bslx.XueXiMod.debug.performance;

/**
 * 首版固定计数。只累加调用方给出的非负增量；零和负数在记录器内无作用。
 * 计数不得再执行一次业务表达式。
 */
public enum PerfCounter {
    /** 实际执行一次目标类 getEntitiesOfClass 加一。只计底层世界查询，复用命中不加。与 ENTITY_QUERY_CALLS 互斥。 */
    TARGET_QUERY_CALLS,
    /** 实际执行一次实体类 getEntitiesOfClass 加一。只计底层世界查询，复用命中不加。TrackingSupport 原先不计数，接入后归此项，新旧报告口径扩大。 */
    ENTITY_QUERY_CALLS,
    /** 每次目标请求返回、供原调用者消费的过滤后列表 size 累计，包含复用。不是唯一目标，也不是扩大范围的内部扫描量。 */
    TARGET_CANDIDATES,
    /** 每次实体请求返回、供原调用者消费的过滤后列表 size 累计，包含复用。重复访问照实相加，不是唯一目标，也不是扩大范围的内部扫描量。 */
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
    CAST_PLAN_BYPASSES,
    /** 接入候选查询 helper 的一次逻辑请求。同一次请求的 reuse、build、bypass 最多一个终态。 */
    CANDIDATE_QUERY_REQUESTS,
    /** 复用已有候选，且没有实际底层世界查询。 */
    CANDIDATE_QUERY_REUSES,
    /** 真实取得候选并成功放入复用存储。异常或构建放弃不能先记此项。 */
    CANDIDATE_QUERY_BUILDS,
    /** 本次走原 API，例如超限、不安全或未绑定。 */
    CANDIDATE_QUERY_BYPASSES,
    /** 候选空间变化时真正清空已有缓存的次数。没有清空已有缓存则不加。 */
    CANDIDATE_QUERY_INVALIDATIONS,
    /** 纯计算入口的一次逻辑请求。标量每次调用加一，批量每次调用加一，不是元素个数，也不表示跨实体已经合并。 */
    COMPUTE_LOGICAL_REQUESTS,
    /** 本次由 Java 参考实现完成，且没有先尝试 native。与回退计数互斥。 */
    COMPUTE_JAVA_EXECUTIONS,
    /** native 批量结果被接受的次数。 */
    COMPUTE_NATIVE_EXECUTIONS,
    /** 已选择 native，但本次改由 Java 参考实现完成。与 COMPUTE_JAVA_EXECUTIONS 互斥。 */
    COMPUTE_JAVA_FALLBACKS,
    /** 分身索引发布成功后加一。失败不加。只由分身服务调用。 */
    CLONE_SPAWNS,
    /** 分身首次进入结束加一。重复结束不加。只由分身服务调用。 */
    CLONE_ENDS,
    /** 响应从可行动变为已预约时加一。当场拒绝不加。只由分身服务调用。 */
    CLONE_RESPONSE_REQUESTS,
    /** 响应跨过提交线时加一。只由分身服务调用。 */
    CLONE_RESPONSE_COMMITS,
    /** 一条主动或受击请求被拒绝时加一，含排队后取消。同一条只加一次。只由分身服务调用。 */
    CLONE_RESPONSE_REJECTIONS,
    /** 实际执行一次诱敌查询加一。只由分身服务调用。 */
    CLONE_DECOY_PASSES
}
