package com.bslx.XueXiMod.debug.performance;

/**
 * 首版固定耗时分类。全部是 inclusive：可以嵌套，也可以同一项重入。
 * 各项禁止相加后解释成总 CPU 或 tick 总耗时。
 */
public enum PerfMetric {
    /** SkillUsePacket.enqueueWork 里的同步按下处理。 */
    RELEASE_REQUEST,
    /** TechniqueChain.split。 */
    CHAIN_SPLIT,
    /**
     * 实际角色扫描。编译与失配 fallback 的实际扫描都计入；命中后只读标志不计。
     * 口径由原先的执行期 runSegment 回扫扩大到全部实际扫描，新旧报告须按此口径阅读。
     */
    CHAIN_ROLE_SCAN,
    /** ChainScheduler 两处真正的 action.run，不含 schedule 整体。 */
    SCHEDULED_ACTION,
    /** SkillSegmentRunner.runSegment 的服务端执行体，含子项和早退。 */
    SEGMENT_EXECUTE,
    /** 锁定、准星和锚点的实际底层世界查询区间。复用命中不加。射线时间留在此项，不进 BLOCK_CLIP。 */
    TARGET_QUERY,
    /** CastFrame.forAttack、volumePoint、aimFor 的计算体。重叠照实累加。 */
    GEOMETRY,
    /** ContactEnchantState.tick 的服务端处理体。 */
    CONTACT_ENCHANT_TICK,
    /** TechniqueHitEntity.tick 客户端返回后的整个服务端体，含 pendingActivation 早退。 */
    HIT_ENTITY_UPDATE,
    /** SwordEntity.tick 客户端返回后的整个服务端体。 */
    SWORD_UPDATE,
    /** 已有 motion.tick、SwordEntityMotion.flyStep 与 SwordEntity.returnTick。弧带留在命中体更新里。 */
    MOTION,
    /** 已列明的实际底层 getEntitiesOfClass。复用命中不加。只包查询表达式，不包随后的消费循环。TrackingSupport 原先不计入，接入后归此项，新旧报告口径扩大。 */
    ENTITY_QUERY,
    /** SwordEntity.anyOwnedBy / ownedBy 的原扫描体。与空间查询分开。 */
    SWORD_OWNED_SCAN,
    /** 普通 contains、弧带 intersectsSlice、hurtAlong 的原窄判定。 */
    NARROW_PHASE,
    /** 命中体和剑步进的 level.clip。不含实体 AABB.clip。 */
    BLOCK_CLIP,
    /** TechniqueHitSpec.spawnReported 整段调用。 */
    HIT_SPAWN,
    /** PhantomSwordEffect 的原生成执行体。 */
    PHANTOM_SWORD_SPAWN,
    /** TechniqueHitSettle.tryHit 与 settleFlow。settle 内不再重复包同一枚举。 */
    HIT_SETTLE,
    /** ReleaseBatch.Tally.record。 */
    TALLY_RECORD,
    /** ReleaseBatch.Tally.flush。 */
    TALLY_FLUSH,
    /** TechniqueHits.hurt 最内层、hurtConfirmed，以及剑的直接 magic hurt。不含再包一层 hurtRoot。 */
    DAMAGE_APPLY,
    /** TechniqueHitSettle 中 broadcastEntityEvent、playSound、sendParticles 的每个原调用。 */
    EXPLICIT_SEND,
    /** 真实结构编译。只包编译本身；命中读取和旧路径不进此项。 */
    CAST_PLAN_COMPILE,
    /**
     * 候选查询 helper 的包含式耗时。覆盖该次逻辑请求，含 miss 或 bypass 里的实际世界查询。
     * 不可与嵌套的 TARGET_QUERY、ENTITY_QUERY 相加。
     */
    CANDIDATE_QUERY_LOOKUP
}
