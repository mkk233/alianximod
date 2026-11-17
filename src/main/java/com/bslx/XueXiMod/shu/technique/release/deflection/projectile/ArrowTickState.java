package com.bslx.XueXiMod.shu.technique.release.deflection.projectile;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 同一次箭 tick 里，前段实体和本拍 partial 的记号。tick 末尾清掉。 */
public final class ArrowTickState {
    private static final ConcurrentHashMap<UUID, Double> HANDLED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> PARTIAL = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> SUPPRESS = new ConcurrentHashMap<>();

    private ArrowTickState() {
    }

    /** 清掉这支箭上一拍留下的处理记号。 */
    public static void beginTick(UUID arrowId) {
        if (arrowId != null) {
            HANDLED.put(arrowId, -1.0);
        }
    }

    /** 本拍已经处理到的线段参数。 */
    public static double handled(UUID arrowId) {
        if (arrowId == null) {
            return -1.0;
        }
        return HANDLED.getOrDefault(arrowId, -1.0);
    }

    /** 记下本拍已经处理过的最远线段参数。 */
    public static void handleThrough(UUID arrowId, double segmentT) {
        if (arrowId == null || !Double.isFinite(segmentT)) {
            return;
        }
        HANDLED.merge(arrowId, segmentT, Math::max);
    }

    /** 记下这一拍发生了部分卸力。 */
    public static void markPartial(UUID arrowId) {
        if (arrowId != null) {
            PARTIAL.put(arrowId, Boolean.TRUE);
        }
    }

    /** 这一拍是否已经部分卸力。 */
    public static boolean partial(UUID arrowId) {
        return arrowId != null && PARTIAL.containsKey(arrowId);
    }

    /** 准备抑制这一拍随后的一次减伤回势。 */
    public static void armSuppress(UUID arrowId) {
        if (arrowId != null) {
            SUPPRESS.put(arrowId, Boolean.TRUE);
        }
    }

    /** 消费一次减伤抑制。没有标记时为假。 */
    public static boolean consumeSuppress(UUID arrowId) {
        return arrowId != null && SUPPRESS.remove(arrowId) != null;
    }

    /** 服务器拍末清掉全部临时记号。 */
    public static void clear() {
        HANDLED.clear();
        PARTIAL.clear();
        SUPPRESS.clear();
    }
}
