package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActorAttackAdapter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 无状态攻击适配登记。这里没有服务器队列，也还没有斜斩或横劈实例。
 * 重复登记同一个实例允许再次调用；换一个实例则拒绝。
 */
public final class ActorAttackAdapterRegistry {
    private static final ConcurrentMap<String, ActorAttackAdapter> ADAPTERS = new ConcurrentHashMap<>();

    private ActorAttackAdapterRegistry() {
    }

    public static void register(String techniqueId, ActorAttackAdapter adapter) {
        if (techniqueId == null || techniqueId.isBlank() || adapter == null) {
            throw new IllegalArgumentException("攻击适配器登记不完整");
        }
        ActorAttackAdapter previous = ADAPTERS.putIfAbsent(techniqueId, adapter);
        if (previous != null && previous != adapter) {
            throw new IllegalStateException("攻击适配器重复登记: " + techniqueId);
        }
    }

    public static Optional<ActorAttackAdapter> find(String techniqueId) {
        if (techniqueId == null || techniqueId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ADAPTERS.get(techniqueId));
    }

    public static boolean supports(String techniqueId) {
        return techniqueId != null && ADAPTERS.containsKey(techniqueId);
    }
}
